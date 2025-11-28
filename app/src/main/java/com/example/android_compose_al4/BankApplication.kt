package com.example.android_compose_al4

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BankApplication : Application() {
    
    init {
        instance = this
    }
    
    companion object {
        private var instance: BankApplication? = null
        

    }
}
