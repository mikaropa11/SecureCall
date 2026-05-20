package com.example.securecall.di

import android.content.Context
import androidx.room.Room
import com.example.securecall.data.local.dao.CallDao
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.local.dao.MessageDao
import com.example.securecall.data.local.dao.UserDao
import com.example.securecall.data.local.db.AppDatabase
import com.example.securecall.data.repository.AuthenticationRepositoryImpl
import com.example.securecall.data.repository.CallRepositoryImpl
import com.example.securecall.data.repository.ChatRepositoryImpl
import com.example.securecall.data.repository.FaceRepositoryImpl
import com.example.securecall.data.repository.MessageRepositoryImpl
import com.example.securecall.data.repository.UserRepositoryImpl
import com.example.securecall.domain.repository.AuthenticationRepository
import com.example.securecall.domain.repository.CallRepository
import com.example.securecall.domain.repository.ChatRepository
import com.example.securecall.domain.repository.FaceRepository
import com.example.securecall.domain.repository.MessageRepository
import com.example.securecall.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthenticationRepository(firebaseAuth: FirebaseAuth):
            AuthenticationRepository = AuthenticationRepositoryImpl(firebaseAuth)

    @Provides
    @Singleton
    fun provideFaceRepository(@ApplicationContext context: Context,
                              userRepository: UserRepository):
            FaceRepository = FaceRepositoryImpl(context, userRepository)

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore, firebaseAuth: FirebaseAuth, userDao: UserDao):
            UserRepository = UserRepositoryImpl(firestore, firebaseAuth, userDao)

    @Provides
    @Singleton
    fun provideChatRepository(chatDao: ChatDao, firestore: FirebaseFirestore, auth: FirebaseAuth):
            ChatRepository = ChatRepositoryImpl(chatDao, firestore, auth)

    @Provides
    @Singleton
    fun provideMessageRepository(messageDao: MessageDao, firestore: FirebaseFirestore, chatDao: ChatDao):
            MessageRepository = MessageRepositoryImpl(messageDao, firestore, chatDao)

    @Provides
    @Singleton
    fun provideCallRepository(callDao: CallDao): CallRepository = CallRepositoryImpl(callDao)

}