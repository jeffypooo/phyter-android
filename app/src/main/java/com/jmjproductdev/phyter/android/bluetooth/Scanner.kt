package com.jmjproductdev.phyter.android.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.jmjproductdev.phyter.core.bluetooth.phyterServiceUUID
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.instrument.InstrumentScanner
import com.jmjproductdev.phyter.util.npe
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ScanError(msg: String) : Exception(msg)
class BleInstrumentScanner(val context: Context) : InstrumentScanner, ScanCallback() {

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
    listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(phyterServiceUUID)).build())
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
        startScan(scanFilters, scanSettings, this@BleInstrumentScanner)
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
    val phyter = BlePhyter(context, btDev, rssi)
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
      stopScan(this@BleInstrumentScanner)
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