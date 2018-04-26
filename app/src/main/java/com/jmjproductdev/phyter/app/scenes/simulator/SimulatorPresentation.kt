package com.jmjproductdev.phyter.app.scenes.simulator

import com.jmjproductdev.phyter.app.common.presentation.PhyterPresenter
import com.jmjproductdev.phyter.app.common.presentation.PhyterView
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import java.security.SecureRandom
import java.util.*

interface SimulatorView : PhyterView {
  var nameFieldText: String
  var addressFieldText: String
  var configurationControlsEnabled: Boolean
  var controlButtonText: String
}

private val ByteArray.stringLikeBdAddress: String
  get() = joinToString(separator = ":") { String.format("%02X", it) }

class SimulatorPresenter(val bleManager: BLEManager) : PhyterPresenter<SimulatorView>() {

  private val peripheral by lazy {
    bleManager.createPeripheral(UUID(0L, 0xFFE0L))
  }

  override fun onCreate(view: SimulatorView) {
    super.onCreate(view)
    randomizeName()
    randomizeAddress()
  }

  fun onRandomizeButtonClick() {

  }

  fun onActionButtonClick() {
    val foo = peripheral
  }

  private fun randomizeName() {
    view?.nameFieldText = SecureRandom().let { "RandomPhyter${it.nextInt(Short.MAX_VALUE.toInt()).toShort()}" }
  }

  private fun randomizeAddress() {
    val bytes = ByteArray(size = 6)
    SecureRandom().apply { nextBytes(bytes) }
    view?.addressFieldText =  bytes.stringLikeBdAddress
  }



}