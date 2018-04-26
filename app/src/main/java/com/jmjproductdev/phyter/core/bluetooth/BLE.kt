package com.jmjproductdev.phyter.core.bluetooth

import com.jmjproductdev.phyter.core.instrument.PhyterScanner
import io.reactivex.Single

interface BLEManager {
  /**
   * Bluetooth state.
   */
  val enabled: Boolean
  /**
   * PhyterScanner instance. May be null if bluetooth is disabled or not available.
   */
  val scanner: PhyterScanner?

  /**
   * Request to enable the bluetooth adapter.
   * @return [Single] that emits the result of the enable request.
   */
  fun requestEnable(): Single<Boolean>

}