package com.jmjproductdev.phyter.app

import android.app.Application
import com.jmjproductdev.phyter.R
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class PhyterApp : Application() {

  override fun onCreate() {
    super.onCreate()
    initLog()
    initFonts()
  }

  private fun initLog() {
    Timber.plant(Timber.DebugTree())
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