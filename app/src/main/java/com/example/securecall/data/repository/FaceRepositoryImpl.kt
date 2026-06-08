package com.example.securecall.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.securecall.domain.repository.DetectedOrientation
import com.example.securecall.domain.repository.FaceFrameInfo
import com.example.securecall.domain.repository.FaceRepository
import com.example.securecall.domain.repository.HeadPose
import com.example.securecall.domain.repository.RegistrationProgress
import com.example.securecall.domain.repository.RegistrationStep
import com.example.securecall.domain.repository.UserRepository
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor

class FaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : FaceRepository {

    private val threshold = 0.9862f
    private val authFramesNeeded = 10
    private val regFramesPerStep = 10
    private val orientationThreshold = 0.12f
    private val minDetectionScore = 0.85f
    private val minBoundingBoxArea = 0.04f
    private val maxCenterOffset = 0.35f
    private val minEyeDistance = 40f

    private val authCandidates = mutableListOf<ScoredEmbedding>()
    private val regCandidates = RegistrationStep.entries.associateWith { mutableListOf<ScoredEmbedding>() }
    private var storedEmbedding: FloatArray? = null

    private val dstLandmarks = arrayOf(
        floatArrayOf(30.2946f, 51.6963f),
        floatArrayOf(65.5318f, 51.5014f),
        floatArrayOf(48.0252f, 71.7366f),
        floatArrayOf(33.5493f, 92.3655f),
        floatArrayOf(62.7299f, 92.2041f)
    )

    private val faceDetector: FaceDetector by lazy {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("blaze_face_short_range.tflite")
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        FaceDetector.createFromOptions(context, options)
    }

    private val model: Module by lazy {
        Module.load(assetFilePath("mobilefacenet.pt"))
    }

    override suspend fun authenticate(imageProxy: ImageProxy): AuthResult {
        return try {
            val preprocessResult = preprocessImage(imageProxy) ?: return AuthResult.NoFace
            val outputTensor = model.forward(IValue.from(preprocessResult.tensor)).toTensor()
            val embedding = normalize(outputTensor.dataAsFloatArray)

            if (!isFrameQualityValid(preprocessResult)) return AuthResult.Collecting

            authCandidates.add(
                ScoredEmbedding(
                    embedding = embedding,
                    detectionScore = preprocessResult.landmarkResult.detectionScore,
                    eyeDistance = preprocessResult.frameInfo.eyeDistance
                )
            )

            Log.d("FACE_REG", "[AUTH] frames=${authCandidates.size}/$authFramesNeeded")
            if (authCandidates.size < authFramesNeeded) return AuthResult.Collecting

            val bestEmbedding = authCandidates.maxByOrNull { it.quality }!!.embedding
            authCandidates.clear()

            if (storedEmbedding == null) {
                storedEmbedding = userRepository.getEmbedding().getOrNull()
            }
            val reference = storedEmbedding ?: return AuthResult.Error("no_reference")
            val similarity = cosineSimilarity(bestEmbedding, normalize(reference))
            val match = similarity > threshold

            Log.d("FACE_REG", "[AUTH] similarity=$similarity threshold=$threshold match=$match")
            if (match) AuthResult.Match else AuthResult.NoMatch
        } catch (e: Exception) {
            Log.e("FACE_REG", "[AUTH] Error: ${e.message}", e)
            AuthResult.Error(e.message ?: "unknown")
        } finally {
            imageProxy.close()
        }
    }

    override suspend fun register(imageProxy: ImageProxy): RegisterResult {
        return try {
            val preprocessResult = preprocessImage(imageProxy) ?: return RegisterResult.NoFace
            val outputTensor = model.forward(IValue.from(preprocessResult.tensor)).toTensor()
            val embedding = normalize(outputTensor.dataAsFloatArray)

            if (!isFrameQualityValid(preprocessResult)) return RegisterResult.CollectingFrames

            val progress = getRegistrationProgress()
            val currentStep = progress.currentStep
            val expectedOrientation = currentStep.toOrientation()
            if (preprocessResult.frameInfo.orientation != expectedOrientation) {
                return RegisterResult.CollectingFrames
            }

            regCandidates.getValue(currentStep).add(
                ScoredEmbedding(
                    embedding = embedding,
                    detectionScore = preprocessResult.landmarkResult.detectionScore,
                    eyeDistance = preprocessResult.frameInfo.eyeDistance
                )
            )

            val framesInStep = regCandidates.getValue(currentStep).size
            Log.d("FACE_REG", "[REG] step=$currentStep frames=$framesInStep/$regFramesPerStep")

            if (framesInStep < regFramesPerStep) return RegisterResult.CollectingFrames

            val allStepsDone = RegistrationStep.entries.all {
                regCandidates.getValue(it).size >= regFramesPerStep
            }
            if (!allStepsDone) {
                return RegisterResult.StepCompleted(
                    step = currentStep,
                    framesCollected = framesInStep
                )
            }

            val finalEmbedding = buildFinalRegistrationEmbedding()
                ?: return RegisterResult.Error("empty_buffer")
            userRepository.saveEmbedding(finalEmbedding)
                .onFailure { return RegisterResult.Error(it.message ?: "save_failed") }

            resetRegistrationBuffers()
            Log.d("FACE_REG", "[REG] Registration complete")
            RegisterResult.Saved
        } catch (e: Exception) {
            Log.e("FACE_REG", "[REG] Error: ${e.message}", e)
            RegisterResult.Error(e.message ?: "unknown")
        } finally {
            imageProxy.close()
        }
    }

    override fun getRegistrationProgressPublic(): RegistrationProgress = getRegistrationProgress()

    private fun getRegistrationProgress(): RegistrationProgress {
        val frontal = regCandidates.getValue(RegistrationStep.FRONTAL).size >= regFramesPerStep
        val left = regCandidates.getValue(RegistrationStep.LEFT).size >= regFramesPerStep
        val right = regCandidates.getValue(RegistrationStep.RIGHT).size >= regFramesPerStep
        val currentStep = when {
            !frontal -> RegistrationStep.FRONTAL
            !left -> RegistrationStep.LEFT
            else -> RegistrationStep.RIGHT
        }

        return RegistrationProgress(
            currentStep = currentStep,
            framesInStep = regCandidates.getValue(currentStep).size.coerceAtMost(regFramesPerStep),
            framesNeeded = regFramesPerStep,
            frontalCompleted = frontal,
            leftCompleted = left,
            rightCompleted = right,
            isComplete = frontal && left && right
        )
    }

    private fun buildFinalRegistrationEmbedding(): FloatArray? {
        val bestPerStep = RegistrationStep.entries.mapNotNull { step ->
            regCandidates.getValue(step).maxByOrNull { it.quality }?.embedding
        }
        if (bestPerStep.isEmpty()) return null

        val size = bestPerStep[0].size
        val avg = FloatArray(size)
        for (embedding in bestPerStep) {
            for (i in embedding.indices) {
                avg[i] += embedding[i]
            }
        }
        for (i in avg.indices) {
            avg[i] /= bestPerStep.size
        }

        return normalize(avg)
    }

    private fun resetRegistrationBuffers() {
        regCandidates.values.forEach { it.clear() }
    }

    private fun RegistrationStep.toOrientation(): DetectedOrientation {
        return when (this) {
            RegistrationStep.FRONTAL -> DetectedOrientation.FRONTAL
            RegistrationStep.LEFT -> DetectedOrientation.LEFT
            RegistrationStep.RIGHT -> DetectedOrientation.RIGHT
        }
    }

    private fun isFrameQualityValid(result: PreprocessResult): Boolean {
        val info = result.frameInfo
        val lm = result.landmarkResult
        val scoreOk = lm.detectionScore >= minDetectionScore
        val bboxOk = lm.boundingBoxArea >= minBoundingBoxArea
        val centeredOk = kotlin.math.abs(lm.faceCenterX - 0.5f) <= maxCenterOffset &&
            kotlin.math.abs(lm.faceCenterY - 0.5f) <= maxCenterOffset
        val eyeOk = info.eyeDistance >= minEyeDistance
        val ok = scoreOk && bboxOk && centeredOk && eyeOk

        Log.d(
            "FACE_REG",
            "[QUALITY] score=${lm.detectionScore} bbox=${lm.boundingBoxArea} eye=${info.eyeDistance} ok=$ok"
        )
        return ok
    }

    private data class ScoredEmbedding(
        val embedding: FloatArray,
        val detectionScore: Float,
        val eyeDistance: Float
    ) {
        val quality: Float
            get() = detectionScore * 0.7f + (eyeDistance / 200f).coerceAtMost(1f) * 0.3f
    }

    private data class PreprocessResult(
        val tensor: Tensor,
        val landmarkResult: LandmarkResult,
        val frameInfo: FaceFrameInfo
    )

    private data class LandmarkResult(
        val landmarks: Array<FloatArray>,
        val detectionScore: Float,
        val boundingBoxArea: Float,
        val faceCenterX: Float,
        val faceCenterY: Float
    )

    private fun preprocessImage(imageProxy: ImageProxy): PreprocessResult? {
        val bitmap = imageProxyToBitmap(imageProxy)
        val landmarkResult = detectFaceLandmarks(bitmap) ?: return null
        if (landmarkResult.boundingBoxArea < minBoundingBoxArea) return null

        val dx = kotlin.math.abs(landmarkResult.faceCenterX - 0.5f)
        val dy = kotlin.math.abs(landmarkResult.faceCenterY - 0.5f)
        if (dx > maxCenterOffset || dy > maxCenterOffset) return null

        val frameInfo = buildFrameInfo(landmarkResult, bitmap.width, bitmap.height)
        val aligned = alignFace(bitmap, landmarkResult.landmarks, dstLandmarks)
        val input = bitmapToFloatArray(aligned)

        return PreprocessResult(
            tensor = Tensor.fromBlob(input, longArrayOf(1, 3, 112, 96)),
            landmarkResult = landmarkResult,
            frameInfo = frameInfo
        )
    }

    private fun detectFaceLandmarks(bitmap: Bitmap): LandmarkResult? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = faceDetector.detect(mpImage)
        val detection = result.detections()
            .filter { (it.categories().firstOrNull()?.score() ?: 0f) >= minDetectionScore }
            .maxByOrNull { it.categories().firstOrNull()?.score() ?: 0f }
            ?: return null
        val score = detection.categories().firstOrNull()?.score() ?: 0f
        val boundingBox = detection.boundingBox()
        val bboxArea = (boundingBox.width() / bitmap.width) * (boundingBox.height() / bitmap.height)
        val centerX = (boundingBox.left + boundingBox.width() / 2f) / bitmap.width
        val centerY = (boundingBox.top + boundingBox.height() / 2f) / bitmap.height
        val keypointsOptional = detection.keypoints()
        if (!keypointsOptional.isPresent) return null

        val keypoints = keypointsOptional.get()
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val mouthCenter = keypoints[3]
        val mouthOffsetX = width * 0.05f
        val landmarks = arrayOf(
            floatArrayOf(keypoints[1].x() * width, keypoints[1].y() * height),
            floatArrayOf(keypoints[0].x() * width, keypoints[0].y() * height),
            floatArrayOf(keypoints[2].x() * width, keypoints[2].y() * height),
            floatArrayOf(mouthCenter.x() * width - mouthOffsetX, mouthCenter.y() * height),
            floatArrayOf(mouthCenter.x() * width + mouthOffsetX, mouthCenter.y() * height)
        )

        return LandmarkResult(
            landmarks = landmarks,
            detectionScore = score,
            boundingBoxArea = bboxArea,
            faceCenterX = centerX,
            faceCenterY = centerY
        )
    }

    private fun buildFrameInfo(
        landmarkResult: LandmarkResult,
        width: Int,
        height: Int
    ): FaceFrameInfo {
        val landmarks = landmarkResult.landmarks
        val leftEye = landmarks[0]
        val rightEye = landmarks[1]
        val nose = landmarks[2]
        val eyeCenterX = (leftEye[0] + rightEye[0]) / 2f
        val eyeDistance = kotlin.math.abs(rightEye[0] - leftEye[0]).coerceAtLeast(1f)
        val noseOffsetX = (nose[0] - eyeCenterX) / eyeDistance
        val orientation = when {
            kotlin.math.abs(noseOffsetX) <= orientationThreshold -> DetectedOrientation.FRONTAL
            noseOffsetX < -orientationThreshold -> DetectedOrientation.LEFT
            noseOffsetX > orientationThreshold -> DetectedOrientation.RIGHT
            else -> DetectedOrientation.UNKNOWN
        }
        val faceCenterX = landmarks.map { it[0] }.average().toFloat()
        val faceCenterY = landmarks.map { it[1] }.average().toFloat()
        val centerOffsetX = (faceCenterX - width / 2f) / width
        val centerOffsetY = (faceCenterY - height / 2f) / height

        Log.d(
            "FACE_REG",
            "[LANDMARKS] leftEye=(${leftEye[0]},${leftEye[1]}) rightEye=(${rightEye[0]},${rightEye[1]}) " +
                "nose=(${nose[0]},${nose[1]}) score=${landmarkResult.detectionScore}"
        )
        Log.d("FACE_REG", "[ORIENTATION] noseOffsetX=$noseOffsetX orientation=$orientation")

        return FaceFrameInfo(
            headPose = HeadPose(yaw = noseOffsetX),
            orientation = orientation,
            detectionScore = landmarkResult.detectionScore,
            eyeDistance = eyeDistance,
            faceCenterOffsetX = centerOffsetX,
            faceCenterOffsetY = centerOffsetY
        )
    }

    private fun normalize(embedding: FloatArray): FloatArray {
        var norm = 0f
        for (value in embedding) {
            norm += value * value
        }
        norm = sqrt(norm)
        if (norm == 0f) return embedding.copyOf()
        return embedding.map { it / norm }.toFloatArray()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val dot = a.indices.sumOf { (a[it] * b[it]).toDouble() }.toFloat()
        val normA = sqrt(a.sumOf { (it * it).toDouble() }).toFloat()
        val normB = sqrt(b.sumOf { (it * it).toDouble() }).toFloat()
        val denominator = normA * normB
        if (denominator == 0f) return 0f
        return dot / denominator
    }

    private fun assetFilePath(assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath

        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }

        return file.absolutePath
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val nv21 = yuv420ToNv21(imageProxy)
        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        return rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val nv21 = ByteArray(width * height * 3 / 2)

        for (row in 0 until height) {
            yBuffer.position(row * yPlane.rowStride)
            yBuffer.get(nv21, row * width, width)
        }

        var offset = width * height
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                vBuffer.position(vIndex)
                uBuffer.position(uIndex)
                nv21[offset++] = vBuffer.get()
                nv21[offset++] = uBuffer.get()
            }
        }

        return nv21
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun alignFace(
        bitmap: Bitmap,
        src: Array<FloatArray>,
        dst: Array<FloatArray>
    ): Bitmap {
        val srcPts = src.map { PointF(it[0], it[1]) }
        val dstPts = dst.map { PointF(it[0], it[1]) }
        val matrix = estimateSimilarityTransform(srcPts, dstPts)

        return warpBitmap(bitmap, matrix)
    }

    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val floatArray = FloatArray(3 * width * height)
        val size = width * height

        for (i in pixels.indices) {
            val pixel = pixels[i]
            floatArray[i] = ((pixel shr 16 and 0xFF) - 127.5f) / 128f
            floatArray[i + size] = ((pixel shr 8 and 0xFF) - 127.5f) / 128f
            floatArray[i + 2 * size] = ((pixel and 0xFF) - 127.5f) / 128f
        }

        return floatArray
    }

    private fun estimateSimilarityTransform(
        src: List<PointF>,
        dst: List<PointF>
    ): Matrix {
        require(src.size == dst.size) { "Landmarks size mismatch" }

        val srcMean = PointF(0f, 0f)
        val dstMean = PointF(0f, 0f)
        for (i in src.indices) {
            srcMean.x += src[i].x
            srcMean.y += src[i].y
            dstMean.x += dst[i].x
            dstMean.y += dst[i].y
        }

        srcMean.x /= src.size
        srcMean.y /= src.size
        dstMean.x /= dst.size
        dstMean.y /= dst.size

        var num = 0f
        var den = 0f
        var srcVar = 0f
        for (i in src.indices) {
            val sx = src[i].x - srcMean.x
            val sy = src[i].y - srcMean.y
            val dx = dst[i].x - dstMean.x
            val dy = dst[i].y - dstMean.y
            num += sx * dx + sy * dy
            den += sx * dy - sy * dx
            srcVar += sx * sx + sy * sy
        }

        val scale = sqrt((num * num + den * den) / srcVar)
        val theta = atan2(den, num)
        val cos = cos(theta)
        val sin = sin(theta)
        val matrix = Matrix()
        val values = floatArrayOf(
            scale * cos, -scale * sin, dstMean.x - scale * (cos * srcMean.x - sin * srcMean.y),
            scale * sin, scale * cos, dstMean.y - scale * (sin * srcMean.x + cos * srcMean.y),
            0f, 0f, 1f
        )
        matrix.setValues(values)

        return matrix
    }

    private fun warpBitmap(
        bitmap: Bitmap,
        matrix: Matrix
    ): Bitmap {
        val output = Bitmap.createBitmap(96, 112, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }
}

sealed class RegisterResult {
    data object CollectingFrames : RegisterResult()
    data class StepCompleted(
        val step: RegistrationStep,
        val framesCollected: Int
    ) : RegisterResult()
    data object Saved : RegisterResult()
    data object NoFace : RegisterResult()
    data class Error(val cause: String) : RegisterResult()
}

sealed class AuthResult {
    data object Collecting : AuthResult()
    data object Match : AuthResult()
    data object NoMatch : AuthResult()
    data object NoFace : AuthResult()
    data class Error(val cause: String) : AuthResult()
}
