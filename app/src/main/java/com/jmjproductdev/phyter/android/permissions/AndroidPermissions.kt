package com.jmjproductdev.phyter.android.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import com.jmjproductdev.phyter.android.appcompat.checkSelfPermissionsCompat
import com.jmjproductdev.phyter.android.appcompat.requestPermissionsCompat
import com.jmjproductdev.phyter.core.permissions.Permission
import com.jmjproductdev.phyter.core.permissions.PermissionsManager
import com.jmjproductdev.phyter.util.npe
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference

private val Permission.androidString: String
  get() {
    return when (this) {
      Permission.ACCESS_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
      Permission.ACCESS_COARSE_LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
    }
  }

private fun permission(forAndroidString: String): Permission? {
  return Permission.values().find { it.androidString == forAndroidString }
}


class ActivityPermissionsManager(activity: Activity) : PermissionsManager {

  companion object {
    const val REQ_CODE_PERM_MNG = 777
  }

  private val activityRef = WeakReference(activity)
  private val activity: Activity?
    get() = activityRef.get()

  private var requestSubject: PublishSubject<Pair<Permission, Boolean>>? = null

  override fun granted(permission: Permission): Boolean {
    return activity?.checkSelfPermissionsCompat(permission.androidString) ?: false
  }

  override fun request(vararg permissions: Permission): Observable<Pair<Permission, Boolean>> {
    activity?.apply {
      requestSubject = PublishSubject.create()
      requestPermissionsCompat(permissions.map { it.androidString }, REQ_CODE_PERM_MNG)
      return requestSubject!!
    }
    return Observable.error(npe("activity is null"))
  }

  fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    if (requestCode != REQ_CODE_PERM_MNG) return
    for (i in 0 until permissions.size) {
      val permEnum = permission(forAndroidString = permissions[i]) ?: continue
      val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
      requestSubject?.onNext(Pair(permEnum, granted))
    }
    requestSubject?.onComplete()
    requestSubject = null
  }

}