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
import com.example.securecall.domain.repository.FaceRepository
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import org.pytorch.IValue
import org.pytorch.Tensor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.sqrt
import com.example.securecall.domain.repository.UserRepository
import com.google.mediapipe.framework.image.BitmapImageBuilder
import org.pytorch.Module
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class FaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : FaceRepository {

    private final val threshold = 0.968f
    private val embeddingsBuffer = mutableListOf<FloatArray>()
    private final val MAX_FRAMES = 10

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
        val path = assetFilePath("mobilefacenet.pt")
        Module.load(path)
    }

    private var storedEmbedding: FloatArray? = null

    override suspend fun getEmbedding(imageProxy: ImageProxy): FaceResult {
        return try {

            val tensor = preprocessImage(imageProxy)
                ?: return FaceResult.NoFace

            val outputTensor = model.forward(IValue.from(tensor)).toTensor()
            Log.d("EMBED", outputTensor.dataAsFloatArray.joinToString(","))
            val embedding = normalize(outputTensor.dataAsFloatArray)
            Log.d("EMBED-N", embedding.joinToString(","))

            FaceResult.Success(embedding)

        } catch (e: Exception) {
            e.printStackTrace()
            FaceResult.Error
        } finally {
            imageProxy.close()
        }
    }

    override fun addEmbedding(embedding: FloatArray) {
        embeddingsBuffer.add(embedding)
    }

    override fun hasEnoughFrames(): Boolean {
        return embeddingsBuffer.size >= MAX_FRAMES
    }

    override fun getAveragedEmbedding(): FloatArray? {
        if (embeddingsBuffer.isEmpty()) return null

        val size = embeddingsBuffer[0].size
        val avg = FloatArray(size)

        // 🔹 Media
        for (emb in embeddingsBuffer) {
            for (i in emb.indices) {
                avg[i] += emb[i]
            }
        }

        for (i in avg.indices) {
            avg[i] /= embeddingsBuffer.size
        }

        return normalize(avg)
    }

    override fun resetEmbeddings() {
        embeddingsBuffer.clear()
    }

    override suspend fun compareEmbedding(
        embedding: FloatArray
    ): Boolean {

        loadEmbedding()
        val stored = storedEmbedding ?: return false
        val storedEmbedding = normalize(stored)
        Log.d("DEBUG_A", embedding.take(5).toString())
        Log.d("DEBUG_B", storedEmbedding.take(5).toString())
        val similarity = cosineSimilarity(embedding, storedEmbedding)
        return similarity > threshold
    }

    suspend fun loadEmbedding() {
        storedEmbedding = userRepository.getEmbedding().getOrNull()
    }

    private fun preprocessImage(imageProxy: ImageProxy): Tensor? {
        val bitmap = imageProxyToBitmap(imageProxy)

        val srcLandmarks = detectFaceLandmarks(bitmap) ?: return null

        val aligned = alignFace(bitmap, srcLandmarks, dstLandmarks)

        val input = bitmapToFloatArray(aligned)

        return Tensor.fromBlob(input, longArrayOf(1, 3, 112, 96))
    }

    private fun normalize(embedding: FloatArray): FloatArray {
        var norm = 0f
        for (v in embedding) {
            norm += v * v
        }
        norm = sqrt(norm)
        return embedding.map { it / norm }.toFloatArray()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val dot = a.indices.sumOf { (a[it] * b[it]).toDouble() }.toFloat()
        val normA = sqrt(a.sumOf { (it * it).toDouble() }).toFloat()
        val normB = sqrt(b.sumOf { (it * it).toDouble() }).toFloat()
        Log.d("Threshold", "Dot: $dot, NormA: $normA, NormB: $normB, Result: ${dot / (normA * normB)}")

        return dot / (normA * normB)
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
        yuvImage.compressToJpeg(
            Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )

        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        return rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Y
        yBuffer.get(nv21, 0, ySize)

        // VU (NV21 = V + U)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun detectFaceLandmarks(bitmap: Bitmap): Array<FloatArray>? {

        val mpImage = BitmapImageBuilder(bitmap).build()

        val result = faceDetector.detect(mpImage)

        val detection = result.detections().firstOrNull() ?: return null

        val keypointsOptional = detection.keypoints()

        if (!keypointsOptional.isPresent) return null

        val keypoints = keypointsOptional.get()
        // MediaPipe da puntos normalizados (0–1)
        val width = bitmap.width
        val height = bitmap.height

        val rightEye = keypoints[0]
        val leftEye = keypoints[1]
        val nose = keypoints[2]
        val leftMouth = floatArrayOf(
            keypoints[3].x() * width - 10,
            keypoints[3].y() * height
        )

        val rightMouth = floatArrayOf(
            keypoints[3].x() * width + 10,
            keypoints[3].y() * height
        )

        return arrayOf(
            floatArrayOf(leftEye.x() * width, leftEye.y() * height),
            floatArrayOf(rightEye.x() * width, rightEye.y() * height),
            floatArrayOf(nose.x() * width, nose.y() * height),
            leftMouth,
            rightMouth
        )
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
        val w = bitmap.width
        val h = bitmap.height

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val floatArray = FloatArray(3 * w * h)

        for (i in pixels.indices) {
            val px = pixels[i]

            val r = ((px shr 16 and 0xFF) - 127.5f) / 128f
            val g = ((px shr 8 and 0xFF) - 127.5f) / 128f
            val b = ((px and 0xFF) - 127.5f) / 128f

            val size = w * h

            floatArray[i] = r
            floatArray[i + size] = g
            floatArray[i + 2 * size] = b
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

        val mValues = floatArrayOf(
            scale * cos, -scale * sin, dstMean.x - scale * (cos * srcMean.x - sin * srcMean.y),
            scale * sin,  scale * cos, dstMean.y - scale * (sin * srcMean.x + cos * srcMean.y),
            0f, 0f, 1f
        )

        matrix.setValues(mValues)

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

sealed class FaceResult {
    data class Success(val embedding: FloatArray) : FaceResult()
    data object NoFace : FaceResult()
    data object Error : FaceResult()
}



