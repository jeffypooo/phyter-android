package com.jmjproductdev.phyter.core.instrument

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Data model for PhyterApp measurements.
 */
class PhyterMeasurement(
    val ph: Float,
    val temp: Float,
    val a578: Float,
    val a434: Float,
    val dark: Float
)

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
   * The salinity of the sample to be measured.
   */
  var salinity: Float

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
   * Perform background measurements.
   * @return [Completable] that completes once the instrument has completed the operation.
   */
  fun background(): Completable

  /**
   * Perform a measurement.
   * @return [Single] that emits the measurement results once the instrument has completed the operation.
   */
  fun measure(): Single<PhyterMeasurement>

}


/**
 * Bluetooth scanner for PhyterApp instruments.
 */
interface PhyterScanner {
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