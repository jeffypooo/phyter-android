package com.jmjproductdev.phyter.android.bluetooth

import android.bluetooth.*
import android.content.Context
import com.jmjproductdev.phyter.core.bluetooth.clientConfigUUID
import com.jmjproductdev.phyter.core.bluetooth.phyterServiceUUID
import com.jmjproductdev.phyter.core.bluetooth.phyterSppUUID
import com.jmjproductdev.phyter.core.instrument.*
import io.reactivex.*
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

private fun emptyMeasurement() = MeasurementData(0F, 0F, 0F, 0F, 0F)

val BluetoothGattService.isPhyterSppService: Boolean get() = uuid == phyterServiceUUID
val BluetoothGattCharacteristic.isPhyterSpp: Boolean get() = uuid == phyterSppUUID

class PhyterError(msg: String) : Exception(msg)

private fun notConnected(): PhyterError = PhyterError("not connected")

class BlePhyter(private val context: Context, private val bluetoothDevice: BluetoothDevice, override val rssi: Short) : Phyter {

  companion object {
    const val NAME_UNKNOWN = "Unknown"
  }

  override val name: String get() = bluetoothDevice.name ?: NAME_UNKNOWN
  override val address: String get() = bluetoothDevice.address ?: ""
  override val connected: Boolean get() = gatt != null
  override val salinity: Observable<Float> get() = salinitySubject

  private var gatt: BluetoothGatt? = null
  private val sppService get() = gatt?.getService(phyterServiceUUID)
  private val sppCharacteristic get() = sppService?.getCharacteristic(phyterSppUUID)
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
          Timber.v("gatt disconnected (status $status)")
          this@BlePhyter.gatt = null
          connectError(PhyterError("failed to connect"))
        }
        BluetoothProfile.STATE_CONNECTED    -> {
          Timber.v("gatt connected. (status $status)")
          if (completeConnectionIfServiceFound(gatt)) return
          if (!gatt.discoverServices())
            connectError(PhyterError("failed to start service discovery"))
        }
      }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
      Timber.v("onDescriptorWrite($gatt, $descriptor, $status)")
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
      Timber.v("onDescriptorRead($gatt, $descriptor, $status)")
    }


    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
      if (!characteristic.isPhyterSpp) return
      handleReceive()
    }
  }

  private var connectEmitter: CompletableEmitter? = null
  private var setSalinityEmitter: CompletableEmitter? = null
  private var backgroundEmitter: CompletableEmitter? = null
  private var measureEmitter: SingleEmitter<MeasurementData>? = null
  private val salinitySubject = BehaviorSubject.createDefault(35.0F)

  private val responseParserDelegate = object : ResponseParser.Delegate {
    override fun onSalinityResponse(salinity: Float) {
      Timber.d("salinity response: $salinity")
      setSalinityEmitter?.onComplete()
      setSalinityEmitter = null
      salinitySubject.onNext(salinity)
    }

    override fun onBackgroundResponse() {
      super.onBackgroundResponse()
      Timber.d("background response")
      backgroundEmitter?.onComplete()
      backgroundEmitter = null
    }

    override fun onMeasureResponse(measurementData: MeasurementData) {
      Timber.d("measure response: $measurementData")
      measureEmitter?.onSuccess(measurementData)
      measureEmitter = null
    }
  }
  private val responseParser = ResponseParser().apply { delegate = responseParserDelegate }

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

  override fun setSalinity(salinity: Float): Completable {
    return Completable.defer { doSetSalinity(salinity) }
  }

  override fun background(): Completable {
    return Completable.defer { doBackground() }
  }

  override fun measure(): Single<MeasurementData> {
    return Single.defer { doMeasure() }
  }

  override fun toString(): String {
    return "BlePhyter{name: $name, address: $address, rssi: $rssi, salinity: ${salinitySubject.value}}"
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
      getDescriptor(clientConfigUUID)?.apply {
        Timber.v("enabling remote spp characteristic notifications")
        value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(this)
      }
      Timber.v("enabling local spp characteristic notifications")
      gatt.setCharacteristicNotification(this, true)
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

  private fun doSetSalinity(salinity: Float): Completable {
    return Completable.create {
      if (gatt == null) {
        Timber.e("cannot set salinity, disconnected")
        it.onError(PhyterError("not connected"))
        return@create
      }
      setSalinityEmitter = it
      val spp = gatt!!.getService(phyterServiceUUID).getCharacteristic(phyterSppUUID)
      spp.value = salinityCommand(salinity)
      gatt!!.writeCharacteristic(spp)
      Timber.v("wrote salinity command")
    }
  }

  private fun doBackground(): Completable {
    return Completable.create {
      if (gatt == null) {
        it.onError(notConnected())
        return@create
      }
      backgroundEmitter = it
      val spp = gatt!!.getService(phyterServiceUUID).getCharacteristic(phyterSppUUID)
      spp.value = backgroundCommand()
      gatt!!.writeCharacteristic(spp)
      Timber.v("wrote background command")
    }
  }

  private fun doMeasure(): Single<MeasurementData> {
    return Single.create {
      if (gatt == null) {
        it.onError(notConnected())
        return@create
      }
      measureEmitter = it
      val spp = gatt!!.getService(phyterServiceUUID).getCharacteristic(phyterSppUUID)
      spp.value = measureCommand()
      gatt!!.writeCharacteristic(spp)
      Timber.v("wrote background command")
    }
  }

  private fun handleReceive() {
    sppCharacteristic?.let {
      Timber.v("parsing ${it.value.size} bytes")
      responseParser.parse(it.value)
    }
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