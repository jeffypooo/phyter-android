package com.jmjproductdev.phyter.app

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.jmjproductdev.phyter.BuildConfig
import com.jmjproductdev.phyter.R
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class PhyterApp : Application() {

  override fun onCreate() {
    super.onCreate()
    initLog()
    initFabric()
    initFonts()
  }

  private fun initLog() {
    Timber.plant(Timber.DebugTree())
  }

  private fun initFabric() {
    if (BuildConfig.DEBUG) return
    Fabric.with(this, Crashlytics(), Answers())
  }

  private fun initFonts() {
    CalligraphyConfig.initDefault(
        CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/Avenir.ttc")
            .setFontAttrId(R.attr.fontPath)
            .build()
    )
  }


}