package com.jmjproductdev.phyter.app.scenes.launch

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.common.android.PhyterActivity

class LaunchActivity : PhyterActivity() {


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launch)
    if (!bluetoothEnabled()) {
      requestBluetoothEnable()
    } else {
      startBleScan(period = 5000)
    }
  }
}
