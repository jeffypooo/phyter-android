package com.jmjproductdev.phyter

import android.util.Log
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private val Int.logLevelString: String
  get() {
    return when (this) {
      Log.ASSERT  -> "ASSERT/"
      Log.VERBOSE -> "V/"
      Log.DEBUG   -> "D/"
      Log.INFO    -> "I/"
      Log.WARN    -> "W/"
      Log.ERROR   -> "E/"
      else        -> "?/"
    }
  }


class UnitTestTree : Timber.DebugTree() {

  private val dateFormat: DateFormat by lazy { SimpleDateFormat("HH:mm:SSS") }

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    println("${dateFormat.format(Date())} | ${priority.logLevelString}$tag: $message")
    t?.run { this.printStackTrace(System.err) }
  }
}