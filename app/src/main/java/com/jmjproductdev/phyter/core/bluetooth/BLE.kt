package com.jmjproductdev.phyter.core.bluetooth

import com.jmjproductdev.phyter.core.instrument.InstrumentScanner
import io.reactivex.Single
import java.util.*

interface BLEPeripheral {
  val serviceUUID: UUID
  var advertising: Boolean
  var name: String
  fun dispose()
}

interface BLEManager {
  /**
   * Bluetooth state.
   */
  val enabled: Boolean
  /**
   * InstrumentScanner instance. May be null if bluetooth is disabled or not available.
   */
  val scanner: InstrumentScanner?

  /**
   * Request to enable the bluetooth adapter.
   * @return [Single] that emits the result of the enable request.
   */
  fun requestEnable(): Single<Boolean>

  fun createPeripheral(serviceUuid: UUID, name: String): BLEPeripheral?

}

val clientConfigUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
val phyterServiceUUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
val phyterSppUUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
