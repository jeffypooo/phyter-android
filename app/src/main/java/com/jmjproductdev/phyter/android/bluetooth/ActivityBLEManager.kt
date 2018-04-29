package com.jmjproductdev.phyter.android.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.jmjproductdev.phyter.core.bluetooth.BLEPeripheral
import com.jmjproductdev.phyter.util.illegalArgs
import com.jmjproductdev.phyter.util.npe
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

private fun enableIntent() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
class ActivityBLEManager(activity: Activity) : BLEManager {
  companion object {
    const val REQ_CODE_ENABLE_BLUETOOTH = 777
    private const val TAG = "ActivityBLEManager"
  }

  override val enabled: Boolean
    get() = BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false

  override val scanner: com.jmjproductdev.phyter.core.instrument.PhyterScanner?
    get() = scannerInstance

  private val activityRef: WeakReference<Activity> = WeakReference(activity)
  private val activity: Activity?
    get() = activityRef.get()

  private val scannerInstance: PhyterScanner by lazy { PhyterScanner(activity) }
  private var enableSubject: SingleSubject<Boolean>? = null

  @Synchronized
  override fun requestEnable(): Single<Boolean> {
    if (enabled) return Single.just(true)
    activity?.run {
      return Single.defer {
        enableSubject = SingleSubject.create()
        with(enableIntent()) { startActivityForResult(this, REQ_CODE_ENABLE_BLUETOOTH) }
        return@defer enableSubject
      }
    }

    return Single.error(npe("activity is null"))
  }

  override fun createPeripheral(serviceUuid: UUID, name: String): BLEPeripheral? {
    activity?.apply { return PhyterPeripheral(this, serviceUuid).apply { this.name = name } }
    return null
  }

  @Synchronized
  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode != REQ_CODE_ENABLE_BLUETOOTH) return
    when (resultCode) {
      Activity.RESULT_OK       -> {
        Timber.d("user enabled bluetooth")
        enableSubject?.onSuccess(true)
      }
      Activity.RESULT_CANCELED -> {
        Timber.w("user declined to enable bluetooth")
        enableSubject?.onSuccess(false)
      }
      else                     -> {
        Timber.e("onActivityResult: unknown result code $resultCode")
        enableSubject?.onError(illegalArgs("unknown result code passed to onActivityResult: $resultCode"))
      }
    }
    enableSubject = null
  }

}