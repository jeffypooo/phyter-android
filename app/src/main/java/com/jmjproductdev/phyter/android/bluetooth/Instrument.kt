package com.jmjproductdev.phyter.android.bluetooth

import android.bluetooth.*
import android.content.Context
import com.jmjproductdev.phyter.core.bluetooth.clientConfigUUID
import com.jmjproductdev.phyter.core.bluetooth.phyterServiceUUID
import com.jmjproductdev.phyter.core.bluetooth.phyterSppUUID
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.instrument.PhyterMeasurement
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Single
import timber.log.Timber

private fun emptyMeasurement() = PhyterMeasurement(0F, 0F, 0F, 0F, 0F)

class PhyterError(msg: String) : Exception(msg)

class BlePhyter(private val context: Context, private val bluetoothDevice: BluetoothDevice, override val rssi: Short) : Phyter {

  companion object {
    const val NAME_UNKNOWN = "Unknown"
  }

  override val name: String
    get() = bluetoothDevice.name ?: NAME_UNKNOWN
  override val address: String
    get() = bluetoothDevice.address ?: ""
  override val connected: Boolean
    get() = TODO("not implemented")
  override var salinity: Float
    get() = actualSalinity
    set(value) {
      actualSalinity = value
    }

  private var gatt: BluetoothGatt? = null
  private var connectEmitter: CompletableEmitter? = null

  private val gattCallback = object : BluetoothGattCallback() {

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
      when (status) {
        BluetoothGatt.GATT_SUCCESS -> Timber.v("successfully discovered services")
        else                       -> Timber.w("unknown service discovery status $status")
      }
      if (!completeConnectionIfServiceFound(gatt))
        connectError(PhyterError("failed to find spp service"))
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
      when (newState) {
        BluetoothProfile.STATE_DISCONNECTED -> {
          Timber.v("gatt disconnected (status ${status.toString(16)})")
          connectError(PhyterError("failed to connect"))
        }
        BluetoothProfile.STATE_CONNECTED    -> {
          Timber.v("gatt connected. (status ${status.toString(16)})")
          if (completeConnectionIfServiceFound(gatt)) return
          if (!gatt.discoverServices())
            connectError(PhyterError("failed to start service discovery"))
        }
      }
    }
  }

  private var actualSalinity: Float = 35.0F

  override fun connect(): Completable {
    return disconnect().andThen(
        Completable.create {
          connectEmitter = it
          bluetoothDevice.connectGatt(context, false, gattCallback).connect()
        }
    )
  }

  override fun disconnect(): Completable {
    return Completable.defer {
      gatt?.apply {
        Timber.v("disconnecting gatt")
        disconnect()
        close()
      }
      gatt = null
      Completable.complete()
    }
  }

  override fun background(): Completable {
    Timber.w("background(): stub!")
    return Completable.complete()
  }

  override fun measure(): Single<PhyterMeasurement> {
    Timber.w("measure(): stub!")
    return Single.just(emptyMeasurement())
  }

  override fun toString(): String {
    return "BlePhyter{name: $name, address: $address, rssi: $rssi, salinity: $actualSalinity}"
  }

  private fun completeConnectionIfServiceFound(gatt: BluetoothGatt): Boolean {
    if (gatt.services.isEmpty()) return false
    Timber.v("inspecting services: ${gatt.services.joinToString { it.uuid.toString() }}")
    gatt.services.firstOrNull { it.uuid == phyterServiceUUID }?.apply {
      configureGatt(gatt, this)
      connectComplete()
      return true
    }
    return false
  }

  private fun configureGatt(gatt: BluetoothGatt, service: BluetoothGattService) {
    this.gatt = gatt
    Timber.v("requesting high priority connection")
    if (!gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
      Timber.w("error requesting high priority connection")
    }
    Timber.v("configuring spp")
    service.getCharacteristic(phyterSppUUID)?.apply {
      Timber.v("enabling local spp characteristic notifications")
      gatt.setCharacteristicNotification(this, true)
      getDescriptor(clientConfigUUID)?.apply {
        Timber.v("enabling remote spp characteristic notifications")
        value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(this)
      }
    }


  }

  private fun connectComplete() {
    connectEmitter?.onComplete()
    connectEmitter = null
  }

  private fun connectError(err: Throwable) {
    connectEmitter?.onError(err)
    connectEmitter = null
  }
}

class Devices private constructor() {

  private object Singleton {
    val instance = Devices()
  }

  companion object {
    fun shared(): Devices {
      return Singleton.instance
    }
  }

  var activeDevice: Phyter? = null

}