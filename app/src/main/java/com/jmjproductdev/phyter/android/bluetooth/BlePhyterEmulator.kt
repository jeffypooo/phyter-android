package com.jmjproductdev.phyter.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.jmjproductdev.phyter.core.bluetooth.BLEPeripheral
import com.jmjproductdev.phyter.core.bluetooth.clientConfigUUID
import com.jmjproductdev.phyter.core.bluetooth.phyterServiceUUID
import com.jmjproductdev.phyter.core.bluetooth.phyterSppUUID
import com.jmjproductdev.phyter.core.instrument.*
import io.reactivex.Observable
import timber.log.Timber
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

fun makeClientConfigDescriptor(): BluetoothGattDescriptor {
  val perms = BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
  return BluetoothGattDescriptor(clientConfigUUID, perms)
}

fun makeSppCharacteristic(): BluetoothGattCharacteristic {
  val props = BluetoothGattCharacteristic.PROPERTY_READ or
      BluetoothGattCharacteristic.PROPERTY_WRITE or
      BluetoothGattCharacteristic.PROPERTY_NOTIFY
  val perms = BluetoothGattCharacteristic.PERMISSION_READ or
      BluetoothGattCharacteristic.PERMISSION_WRITE
  return BluetoothGattCharacteristic(phyterSppUUID, props, perms)
}

fun makeSppService(): BluetoothGattService {
  return BluetoothGattService(phyterServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
}

class BlePhyterEmulator(private val context: Context, override val serviceUUID: UUID) : BLEPeripheral {

  override var advertising: Boolean by Delegates.observable(false) { _, _, _ -> advertisingChanged() }
  override var name: String
    get() = BluetoothAdapter.getDefaultAdapter()?.name ?: ""
    set(value) {
      BluetoothAdapter.getDefaultAdapter()?.name = value
    }

  private val advertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
      Timber.v("advertising started with $settingsInEffect")
    }

    override fun onStartFailure(errorCode: Int) {
      val failMsg = "failed to start advertising"
      when (errorCode) {
        ADVERTISE_FAILED_ALREADY_STARTED      -> Timber.e("$failMsg, already started")
        ADVERTISE_FAILED_DATA_TOO_LARGE       -> Timber.e("$failMsg, data is too large")
        ADVERTISE_FAILED_FEATURE_UNSUPPORTED  -> Timber.e("$failMsg, feature unsupported")
        ADVERTISE_FAILED_INTERNAL_ERROR       -> Timber.e("$failMsg, internal error")
        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Timber.e("$failMsg, too many advertisers")
        else                                  -> Timber.e("$failMsg, unknown error $errorCode")
      }
    }
  }

  private val advertiser: BluetoothLeAdvertiser? get() = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser
  private val advertiseSettings by lazy {
    AdvertiseSettings.Builder().let {
      it.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
      it.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
      it.setConnectable(true)
      it.setTimeout(0)
      it.build()
    }
  }
  private val advertiseData by lazy {
    AdvertiseData.Builder().let {
      it.setIncludeDeviceName(true)
      it.addServiceUuid(ParcelUuid(phyterServiceUUID))
      it.build()
    }
  }

  private val gattServerCallback = object : BluetoothGattServerCallback() {
    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
      when (newState) {
        BluetoothProfile.STATE_DISCONNECTED -> {
          Timber.v("$device disconnected (status $status)")
//          advertising = true
        }
        BluetoothProfile.STATE_CONNECTED    -> {
          Timber.v("$device connected (status $status)")
          connectedDevice = device
//          advertising = false
        }
      }
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor?
    ) {
      Timber.v("desc read req: $device, $requestId, $offset, $descriptor")
    }

    override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
      Timber.v("notification sent to $device, status $status")
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray
    ) {
      if (characteristic.uuid == phyterSppUUID)
        receiveSpp(device, requestId, value, responseNeeded)
      else
        Timber.w("write req: unknown characteristic")
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice,
        requestId: Int,
        descriptor: BluetoothGattDescriptor,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray
    ) {
      if (descriptor.uuid == clientConfigUUID)
        writeSppDesc(device, requestId, value, responseNeeded)
      else
        Timber.w("desc write req: unknown descriptor")
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
      Timber.v("MTU changed for $device: $mtu")
    }

    override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
      Timber.v("PHY update for $device - RX/TX = $rxPhy/$txPhy, status = $status")
    }

    override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
      Timber.v("execute write for $device - $requestId, $execute")
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
      Timber.v("read request from $device - $requestId, $offset, ${characteristic.uuid}")
    }

    override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
      Timber.v("PHY read for $device - TX/RX = $txPhy/$rxPhy, status = $status")
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService) {
      Timber.v("service added ${service.uuid}, status = $status")
    }
  }

  private val btManager: BluetoothManager get() = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val server = btManager.openGattServer(context, gattServerCallback).apply {
    val characteristic = makeSppCharacteristic()
    characteristic.addDescriptor(makeClientConfigDescriptor())
    val service = makeSppService()
    service.addCharacteristic(characteristic)
    addService(service)
  }
  private val sppService: BluetoothGattService get() = server.getService(phyterServiceUUID)
  private val sppCharacteristic: BluetoothGattCharacteristic get() = sppService.getCharacteristic(phyterSppUUID)
  private var connectedDevice: BluetoothDevice? = null

  private val commandParserDelegate = object : CommandParser.Delegate {
    override fun onSalinityCommand(salinity: Float) {
      Timber.d("salinity command received: $salinity")
      sppCharacteristic.value = salinityResponse(salinity)
      connectedDevice?.let { server.notifyCharacteristicChanged(it, sppCharacteristic, false) }
    }

    override fun onBackgroundCommand() {
      Timber.d("background command received")
      sppCharacteristic.value = backgroundResponse()
      connectedDevice?.let { server.notifyCharacteristicChanged(it, sppCharacteristic, false) }
    }

    @SuppressLint("CheckResult")
    override fun onMeasureCommand() {
      Timber.d("measure command received, sending first response")
      val rand = SecureRandom()
      sppCharacteristic.value = measureResponseOne(rand.nextFloat(), rand.nextFloat())
      connectedDevice?.let { server.notifyCharacteristicChanged(it, sppCharacteristic, false) }
      Observable.timer(1, TimeUnit.SECONDS).subscribe(
          {
            Timber.d("sending second measure response")
            sppCharacteristic.value = measureResponseTwo(rand.nextFloat(), rand.nextFloat(), rand.nextFloat())
            connectedDevice?.let { server.notifyCharacteristicChanged(it, sppCharacteristic, false) }
          }
      )
    }
  }
  private val commandParser = CommandParser().apply { delegate = commandParserDelegate }


  override fun dispose() {
    advertising = false
    server.close()
  }

  private fun advertisingChanged() {
    advertiser?.apply {
      if (advertising) {
        startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
      } else {
        stopAdvertising(advertiseCallback)
        Timber.v("advertising stopped")
      }
    }
  }

  private fun receiveSpp(
      device: BluetoothDevice,
      requestId: Int,
      value: ByteArray,
      respond: Boolean
  ) {
    Timber.v("SPP RX: ${value.joinToString { String.format("%02X", it) }}")
    if (respond) {
      Timber.v("sending gatt response for spp write")
      server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
    }
    commandParser.parse(value)
  }

  private fun writeSppDesc(
      device: BluetoothDevice,
      requestId: Int,
      value: ByteArray,
      respond: Boolean
  ) {
    sppCharacteristic.getDescriptor(clientConfigUUID)?.apply {
      Timber.v("writing spp characteristic config descriptor with ${value.joinToString { it.toString(16) }}")
      this.value = value
      if (respond) {
        Timber.v("sending gatt response for config descriptor write")
        server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
      }
    }

  }

}
