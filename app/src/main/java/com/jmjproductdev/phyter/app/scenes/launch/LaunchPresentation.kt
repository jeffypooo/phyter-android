package com.jmjproductdev.phyter.app.scenes.launch

import com.jmjproductdev.phyter.app.common.presentation.PhyterPresenter
import com.jmjproductdev.phyter.app.common.presentation.PhyterView
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.permissions.Permission
import com.jmjproductdev.phyter.core.permissions.PermissionsManager
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

enum class LaunchMenuOption {
  LAUNCH_SIM
}

interface LaunchView : PhyterView {
  var refresing: Boolean
  fun add(device: Phyter)
  fun update(device: Phyter)
  fun presentSimulatorView()
}


class LaunchPresenter(val permissionsManager: PermissionsManager, val bleManager: BLEManager) : PhyterPresenter<LaunchView>() {

  private val permissionsSubs = CompositeDisposable()
  private val bluetoothSubs = CompositeDisposable()
  private val instruments = mutableMapOf<String, Phyter>()


  override fun onCreate(view: LaunchView) {
    super.onCreate(view)
    checkPermissionsAndScan()
  }

  fun onRefresh() {
    Timber.d("user manually refreshed")
    checkBluetoothAndScan()
  }

  fun onMenuOptionSelected(option: LaunchMenuOption) {
    permissionsSubs.clear()
    bluetoothSubs.clear()
    Timber.d("presenting simulator view")
    view?.presentSimulatorView()
  }

  private fun checkPermissionsAndScan() {
    if (!permissionsManager.granted(Permission.ACCESS_FINE_LOCATION)) {
      requestPermissionsAndScan()
    } else {
      checkBluetoothAndScan()
    }
  }

  private fun requestPermissionsAndScan() {
    permissionsManager.request(Permission.ACCESS_FINE_LOCATION)
        .doOnNext { Timber.d("permission result: $it") }
        .subscribe(
            {
              if (it.first == Permission.ACCESS_FINE_LOCATION && it.second) {
                checkBluetoothAndScan()
              }
            },
            {},
            {}
        )
        .also { permissionsSubs.add(it) }
        .also { Timber.d("requested fine location permission") }
  }

  private fun checkBluetoothAndScan() {
    if (!bleManager.enabled) {
      enableBluetoothAndScan()
    } else {
      doScan()
    }
  }

  private fun enableBluetoothAndScan() {
    bleManager.requestEnable()
        .doOnSuccess { Timber.d("user ${if (it) "enabled" else "declined to enable"} bluetooth") }
        .subscribe(
            { if (it) doScan() },
            { Timber.e("failed to request bluetooth enable: $it") }
        )
        .also { bluetoothSubs.add(it) }
        .also { Timber.d("requesting to enable bluetooth") }

  }

  private fun doScan() {
    val scanner = bleManager.scanner ?: return
    scanner.scan()
        .subscribe(
            { deviceFound(it) },
            {
              Timber.e("failed to scan: $it")
              view?.refresing = false
            },
            { view?.refresing = false }
        )
        .also { Timber.d("scan started") }
        .also { bluetoothSubs.add(it) }
        .also { view?.refresing = true }
  }

  private fun deviceFound(device: Phyter) {
    if (instruments.containsKey(device.address)) {
      val prev = instruments[device.address]!!
      instruments[device.address] = device
      if (prev.name != device.name || prev.rssi != device.rssi){
        Timber.d("updating $device")
        view?.update(device)
      }
    } else {
      Timber.d("found $device")
      instruments[device.address] = device
      view?.add(device)
    }

  }

}