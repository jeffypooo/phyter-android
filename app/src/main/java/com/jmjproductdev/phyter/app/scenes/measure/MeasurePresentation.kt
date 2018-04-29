package com.jmjproductdev.phyter.app.scenes.measure

import com.jmjproductdev.phyter.android.bluetooth.Devices
import com.jmjproductdev.phyter.app.common.presentation.PhyterPresenter
import com.jmjproductdev.phyter.app.common.presentation.PhyterView
import com.jmjproductdev.phyter.core.instrument.Phyter

interface MeasureView : PhyterView {
  var deviceName: String
  var salinity: Float
  var actionButtonText: String
}

class MeasurePresenter : PhyterPresenter<MeasureView>() {

  private val device: Phyter? get() = Devices.shared().activeDevice

  override fun onCreate(view: MeasureView) {
    super.onCreate(view)
    updateDeviceName()
  }

  fun onSalinityEntered() {
    underContruction()
  }

  fun onActionButtonClicked() {
    underContruction()
  }

  private fun underContruction() {
    view?.showSnackbar("under construction!")
  }

  private fun updateDeviceName() {
    device?.apply {
      view?.deviceName = name
    }
  }

}