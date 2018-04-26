package com.jmjproductdev.phyter.android.appcompat

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.annotation.ColorRes
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


fun Context.getColorCompat(@ColorRes resId: Int) = ContextCompat.getColor(this, resId)

fun Activity.checkSelfPermissionsCompat(perm: String): Boolean {
  return ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
}

fun Activity.requestPermissionsCompat(perms: List<String>, requestCode: Int = 0) {
  return ActivityCompat.requestPermissions(this, perms.toTypedArray(), requestCode)
}