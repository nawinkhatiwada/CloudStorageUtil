package com.evolveasia.cloudutils

import androidx.multidex.MultiDexApplication
import com.facebook.stetho.Stetho

/**
 * Created by rakeeb on 9/26/17.
 */
class DemoApplication: MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}