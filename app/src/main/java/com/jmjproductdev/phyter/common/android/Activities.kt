package com.jmjproductdev.phyter.common.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

abstract class PhyterActivity : AppCompatActivity() {

  companion object {
    const val REQ_CODE_BT_ENABLE = 10
    private const val TAG = "PhyterActivity"
  }

  protected val bluetoothAdapter: BluetoothAdapter
    get() = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
  protected val leScanner: BluetoothLeScanner
    get() = bluetoothAdapter.bluetoothLeScanner

  /**
   * Injects Calligraphy into the base context by default (for custom fonts).
   */
  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
  }

  protected fun bluetoothEnabled(): Boolean {
    return bluetoothAdapter.isEnabled
  }

  protected fun requestBluetoothEnable() {
    if (bluetoothEnabled()) return
    Log.d(TAG, "requesting to enable bluetooth...")
    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(enableIntent, REQ_CODE_BT_ENABLE)
  }

  protected fun startBleScan(period: Long) {
    if (!bluetoothEnabled()) {
      Log.w(TAG, "startBleScan: bluetooth is disabled")
      return
    }
    leScanner.startScan(PhyterScanCallback())
  }
}

class PhyterScanCallback : ScanCallback() {

  companion object {
    const val TAG = "PhyterScanCallback"
  }

  var failCb: (Int) -> Unit = {
    Log.e(TAG, "error during scan: $it")
  }
  var resultCb: (Int, ScanResult?) -> Unit = { type, res ->
    Log.d(TAG, "scan result: type = $type, res = $res")
  }
  var batchResultsCb: (MutableList<ScanResult>?) -> Unit = {
    Log.d(TAG, "batch scan results: $it")
  }

  override fun onScanFailed(errorCode: Int) {
    super.onScanFailed(errorCode)
    failCb(errorCode)
  }

  override fun onScanResult(callbackType: Int, result: ScanResult?) {
    super.onScanResult(callbackType, result)
    resultCb(callbackType, result)
  }

  override fun onBatchScanResults(results: MutableList<ScanResult>?) {
    super.onBatchScanResults(results)
    batchResultsCb(results)
  }
}