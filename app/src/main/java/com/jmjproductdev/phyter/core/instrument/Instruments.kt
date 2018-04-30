package com.jmjproductdev.phyter.core.instrument

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Data model for Phyter measurements.
 */
class MeasurementData(
    var ph: Float = 0f,
    var temp: Float = 0f,
    var a578: Float = 0f,
    var a434: Float = 0f,
    var dark: Float = 0f
) {

  override fun toString(): String {
    return "MeasurementData(ph=$ph, temp=$temp, a578=$a578, a434=$a434, dark=$dark)"
  }
}

/**
 * Represents a PhyterApp measurement tool.
 */
interface Phyter {
  /**
   * Name of this instrument (usually the bluetooth name)
   */
  val name: String
  /**
   * Bluetooth address of this instrument.
   */
  val address: String
  /**
   * Bluetooth RSSI of the instrument. See [here][https://en.wikipedia.org/wiki/Received_signal_strength_indication]
   * for more information about RSSI.
   */
  val rssi: Short
  /**
   * Instrument connection state.
   */
  val connected: Boolean
  /**
   * Emits the current salinity value, and any subsequent changes.
   */
  val salinity: Observable<Float>

  /**
   * Connect to this instrument.
   * @return [Completable] that completes upon successful connection.
   */
  fun connect(): Completable

  /**
   * Disconnect this instrument.
   * @return [Completable] that completes upon successful disconnection.
   */
  fun disconnect(): Completable

  /**
   * Set the salinity of the sample to be measured.
   * @return [Completable] that completes once the instrument has completed the operation. The [salinity] property
   * will also emit the new value upon completion.
   */
  fun setSalinity(salinity: Float): Completable

  /**
   * Perform background measurements.
   * @return [Completable] that completes once the instrument has completed the operation.
   */
  fun background(): Completable

  /**
   * Perform a measurement.
   * @return [Single] that emits the measurement results once the instrument has completed the operation.
   */
  fun measure(): Single<MeasurementData>

}


/**
 * Bluetooth scanner for PhyterApp instruments.
 */
interface InstrumentScanner {
  /**
   * Scan state.
   */
  val isScanning: Boolean

  /**
   * Starts a BLE scan for nearby PhyterApp instruments.
   * @return [Observable] that emits discovered [Phyter]s
   */
  fun scan(): Observable<Phyter>

}