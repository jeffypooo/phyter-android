package com.jmjproductdev.phyter.android.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Intent
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.jmjproductdev.phyter.core.bluetooth.BLEPeripheral
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.instrument.PhyterMeasurement
import com.jmjproductdev.phyter.core.instrument.PhyterScanner
import com.jmjproductdev.phyter.util.illegalArgs
import com.jmjproductdev.phyter.util.npe
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit

private fun emptyMeasurement() = PhyterMeasurement(0F, 0F, 0F, 0F, 0F)

private fun enableIntent() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

class BluetoothPhyter(val bluetoothDevice: BluetoothDevice, override val rssi: Short) : Phyter {

  companion object {
    const val NAME_UNKNOWN = "Unknown"
  }

  override val name: String
    get() = bluetoothDevice.name ?: NAME_UNKNOWN
  override val address: String
    get() = bluetoothDevice.address ?: ""
  override val connected: Boolean
    get() = TODO("not implemented")
  override var salinity: Float
    get() = actualSalinity
    set(value) {
      actualSalinity = value
    }

  private var actualSalinity: Float = 35.0F

  override fun connect(): Completable {
    Timber.w("connect(): stub!")
    return Completable.complete()
  }

  override fun disconnect(): Completable {
    Timber.w("disconnect(): stub!")
    return Completable.complete()
  }

  override fun background(): Completable {
    Timber.w("background(): stub!")
    return Completable.complete()
  }

  override fun measure(): Single<PhyterMeasurement> {
    Timber.w("measure(): stub!")
    return Single.just(emptyMeasurement())
  }

  override fun toString(): String {
    return "BluetoothPhyter{name: $name, address: $address, rssi: $rssi, salinity: $actualSalinity}"
  }
}

class AndroidBLEManager(activity: Activity) : BLEManager {
  companion object {
    const val REQ_CODE_ENABLE_BLUETOOTH = 777
    private const val TAG = "AndroidBLEManager"
  }

  override val enabled: Boolean
    get() = BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false

  override val scanner: PhyterScanner?
    get() = scannerInstance

  private val activityRef: WeakReference<Activity> = WeakReference(activity)
  private val activity: Activity?
    get() = activityRef.get()

  private val scannerInstance: ActivityPhyterScanner by lazy { ActivityPhyterScanner() }
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

  override fun createPeripheral(serviceUuid: UUID): BLEPeripheral? {
    TODO("not implemented")
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

class ScanError(msg: String) : Exception(msg)

class ActivityPhyterScanner : PhyterScanner, ScanCallback() {

  companion object {
    const val SCAN_DURATION_MS = 10000L
  }

  private val leScanner: BluetoothLeScanner?
    get() = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

  override val isScanning: Boolean
    get() = scanInProgress

  private val scanSettings: ScanSettings by lazy {
    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
  }
  private val scanFilters: List<ScanFilter> by lazy {
//    val uuid = UUID(0L, 0xFFE0L)
//    val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(uuid)).build()
//    listOf(filter)
    listOf(ScanFilter.Builder().build())
  }
  private val timers = CompositeDisposable()
  private var scanSubject: PublishSubject<Phyter>? = null
  private var scanInProgress = false


  @Synchronized
  override fun scan(): Observable<Phyter> {
    if (scanInProgress) return scanSubject!!
    leScanner?.run {
      return Observable.defer {
        scanSubject = PublishSubject.create()
        startScan(scanFilters, scanSettings, this@ActivityPhyterScanner)
            .also { scanInProgress = true }
            .also { Timber.d("BLE scan started") }
        startScanTimer()
        return@defer scanSubject!!.doOnDispose { scanSubjectDisposed() }
      }

    }
    return Observable.error(npe("BluetoothLeScanner was null"))
  }

  @Synchronized
  override fun onScanFailed(errorCode: Int) {
    Timber.e("BLE scan failed with code $errorCode")
    scanSubject?.onError(ScanError("scan failed with code $errorCode"))
    scanSubject = null
  }

  @Synchronized
  override fun onScanResult(callbackType: Int, result: ScanResult?) {
    val btDev = result?.device ?: return
    val rssi = result.rssi.toShort()
    val phyter = BluetoothPhyter(btDev, rssi)
    scanSubject?.onNext(phyter)
  }

  @Synchronized
  private fun scanSubjectDisposed() {
    Timber.d("scan observer disposed")
    completeScan()
  }

  @Synchronized
  private fun completeScan() {
    leScanner?.run {
      Timber.d("stopping BLE scan")
      stopScan(this@ActivityPhyterScanner)
      scanSubject?.onComplete()
      scanSubject = null
      scanInProgress = false
    }
  }

  @Synchronized
  private fun startScanTimer() {
    Observable.timer(SCAN_DURATION_MS, TimeUnit.MILLISECONDS)
        .flatMapCompletable { Completable.complete() }
        .doOnComplete { Timber.v("scan timer expired") }
        .subscribe({ completeScan() })
        .also { timers.add(it) }
        .also { Timber.v("scan timer started") }
  }
}

