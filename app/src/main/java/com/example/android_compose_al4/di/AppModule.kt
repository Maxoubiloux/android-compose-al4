package com.example.android_compose_al4.di

import android.content.Context
import com.example.android_compose_al4.BankApplication
import com.example.android_compose_al4.data.api.ApiService
import com.example.android_compose_al4.data.api.RetrofitInstance
import com.example.android_compose_al4.data.repository.BankRepository
import com.example.android_compose_al4.data.repository.BankRepositoryImpl
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
    fun provideApplicationContext(@ApplicationContext context: Context): BankApplication {
        return context as BankApplication
    }
    
    @Provides
    @Singleton
    fun provideRetrofitInstance(application: BankApplication): RetrofitInstance {
        return RetrofitInstance.getInstance(application)
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofitInstance: RetrofitInstance): ApiService {
        return retrofitInstance.apiService
    }
    
    @Provides
    @Singleton
    fun provideBankRepository(
        apiService: ApiService,
        @ApplicationContext context: Context
    ): BankRepository {
        return BankRepositoryImpl(apiService, context)
    }
}
