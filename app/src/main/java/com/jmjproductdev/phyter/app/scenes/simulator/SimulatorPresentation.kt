package com.jmjproductdev.phyter.app.scenes.simulator

import com.jmjproductdev.phyter.app.common.presentation.PhyterPresenter
import com.jmjproductdev.phyter.app.common.presentation.PhyterView
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.jmjproductdev.phyter.core.bluetooth.BLEPeripheral
import com.jmjproductdev.phyter.core.bluetooth.phyterServiceUUID
import timber.log.Timber
import java.security.SecureRandom

interface SimulatorView : PhyterView {
  var nameFieldText: String
  var configurationControlsEnabled: Boolean
  var controlButtonText: String
}

private val ByteArray.stringLikeBdAddress: String
  get() = joinToString(separator = ":") { String.format("%02X", it) }


class SimulatorPresenter(private val bleManager: BLEManager) : PhyterPresenter<SimulatorView>() {

  companion object {
    private const val CONTROL_BUTTON_START = "Start"
    private const val CONTROL_BUTTON_STOP = "Stop"
  }

  private var peripheral: BLEPeripheral? = null
  private val peripheralActive: Boolean get() = peripheral != null

  override fun onCreate(view: SimulatorView) {
    super.onCreate(view)
    randomizeName()
  }

  fun onControlButtonClick() {
    if (peripheralActive) {
      peripheral?.dispose()
      peripheral = null
      view?.controlButtonText = CONTROL_BUTTON_START
      view?.configurationControlsEnabled = true
      Timber.d("peripheral stopped")
      return
    }
    bleManager.createPeripheral(phyterServiceUUID, view?.nameFieldText ?: "")?.apply {
      advertising = true
      peripheral = this
      view?.controlButtonText = CONTROL_BUTTON_STOP
      view?.configurationControlsEnabled = false
      Timber.d("peripheral running")
    }
  }

  private fun randomizeName() {
    view?.nameFieldText = SecureRandom()
        .let { "RandomPhyter${it.nextInt(Short.MAX_VALUE.toInt()).toShort()}" }
        .also { Timber.d("randomized name: $it") }
  }


}