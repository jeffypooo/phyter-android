package com.jmjproductdev.phyter.android.activity

import android.Manifest
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

class Permissions {
  companion object {
    const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
  }
}


fun View.hideKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(windowToken, 0)
}

abstract class PhyterActivity : AppCompatActivity() {

  /**
   * Injects Calligraphy into the base context by default (for custom fonts).
   */
  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
  }

}

