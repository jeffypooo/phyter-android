package com.jmjproductdev.phyter.core.instrument

import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Float.toByteArray(order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteArray {
  return ByteBuffer.allocate(4).let {
    it.order(order)
    it.putFloat(this)
    it.array()
  }
}

fun float(fromBytes: List<Byte>, order: ByteOrder = ByteOrder.LITTLE_ENDIAN): Float? {
  if (fromBytes.size != 4) return null
  return ByteBuffer.allocate(4).let {
    val arr = fromBytes.toByteArray()
    it.order(order)
    it.put(arr, 0, arr.size)
    it.rewind()
    it.float
  }
}

enum class PhyterCommand(private val id: Int) {
  SET_SALINITY(0x1),
  BACKGROUND(0x2),
  MEASURE(0x3),
  LED_INTENSITY_CHECK(0x4);

  companion object {
    fun from(byteValue: Byte): PhyterCommand? = PhyterCommand.values().firstOrNull { it.commandId == byteValue }
  }

  val commandId: Byte get() = id.toByte()

}

enum class PhyterResponse(private val id: Int) {
  SET_SALINITY(0x81),
  BACKGROUND(0x82),
  MEASURE_1(0x83),
  MEASURE_2(0x84),
  LED_INTENSITY_CHECK(0x85),
  ERROR(0xFF);

  companion object {
    fun from(byteValue: Byte): PhyterResponse? = PhyterResponse.values().firstOrNull { it.responseId == byteValue }
  }

  val responseId: Byte get() = id.toByte()
}

class ResponseParser {

  interface Delegate {
    fun onSalinityResponse(salinity: Float) {}
    fun onBackgroundResponse() {}
    fun onMeasureResponse(measurementData: MeasurementData) {}
    fun onLedIntensityCheckResponse() {}
  }

  var delegate: Delegate? = null
  private var curMeasurementData: MeasurementData? = null

  fun parse(bytes: ByteArray) {
    if (bytes.isEmpty()) return
    val buf = bytes.toList()
    val resp = PhyterResponse.from(buf.first()) ?: return
    when (resp) {
      PhyterResponse.SET_SALINITY -> handleSalinity(buf)
      PhyterResponse.BACKGROUND   -> handleBackground()
      PhyterResponse.MEASURE_1    -> handleMeasureOne(buf)
      PhyterResponse.MEASURE_2    -> handleMeasureTwo(buf)
      else                        -> Timber.w("parse: not implemented for $resp")
    }
  }

  private fun handleSalinity(buf: List<Byte>) {
    if (buf.size != 5) return
    val salBytes = buf.subList(1, 4)
    float(fromBytes = salBytes)?.let {
      delegate?.onSalinityResponse(it)
    }
  }

  private fun handleBackground() {
    delegate?.onBackgroundResponse()
  }

  private fun handleMeasureOne(buf: List<Byte>) {
    if (buf.size != 9) return
    curMeasurementData = MeasurementData().apply {
      ph = float(buf.subList(1, 5)) ?: 0f
      temp = float(buf.subList(5, 9)) ?: 0f
    }
  }

  private fun handleMeasureTwo(buf: List<Byte>) {
    if (buf.size != 13) return
    curMeasurementData?.apply {
      a578 = float(buf.subList(1, 5)) ?: 0f
      a434 = float(buf.subList(5, 9)) ?: 0f
      dark = float(buf.takeLast(4)) ?: 0f
      delegate?.onMeasureResponse(this)
    }
    curMeasurementData = null
  }

}

class CommandParser {

  interface Delegate {
    fun onSalinityCommand(salinity: Float) {}
    fun onBackgroundCommand() {}
    fun onMeasureCommand() {}
    fun onLedIntensityCheckCommand() {}
  }

  var delegate: Delegate? = null

  fun parse(bytes: ByteArray) {
    if (bytes.isEmpty()) return
    val buf = bytes.toList()
    val cmd = PhyterCommand.from(buf.first()) ?: return
    Timber.v("parsing command: $cmd")
    when (cmd) {
      PhyterCommand.SET_SALINITY -> handleSalinity(buf)
      PhyterCommand.BACKGROUND   -> handleBackground()
      PhyterCommand.MEASURE      -> handleMeasure()
      else                       -> Timber.w("parse: not implemented for $cmd")
    }
  }

  private fun handleSalinity(buf: List<Byte>) {
    if (buf.size != 5) return
    val salBytes = buf.subList(1, 5)
    float(salBytes)?.apply {
      delegate?.onSalinityCommand(this)
    }
  }

  private fun handleBackground() = delegate?.onBackgroundCommand()

  private fun handleMeasure() = delegate?.onMeasureCommand()

}

private fun makeCommandBuffer(cmd: PhyterCommand): MutableList<Byte> = mutableListOf(cmd.commandId)

private fun makeResponseBuffer(resp: PhyterResponse): MutableList<Byte> = mutableListOf(resp.responseId)

fun salinityCommand(salinity: Float): ByteArray = makeCommandBuffer(PhyterCommand.SET_SALINITY).let {
  it.addAll(salinity.toByteArray(ByteOrder.LITTLE_ENDIAN).toList())
  it.toByteArray()
}

fun salinityResponse(salinity: Float): ByteArray = makeResponseBuffer(PhyterResponse.SET_SALINITY).let {
  it.addAll(salinity.toByteArray().toList())
  it.toByteArray()
}

fun backgroundCommand(): ByteArray = makeCommandBuffer(PhyterCommand.BACKGROUND).toByteArray()

fun backgroundResponse(): ByteArray = makeResponseBuffer(PhyterResponse.BACKGROUND).toByteArray()

fun measureCommand(): ByteArray = makeCommandBuffer(PhyterCommand.MEASURE).toByteArray()

fun measureResponseOne(pH: Float, temp: Float): ByteArray = makeResponseBuffer(PhyterResponse.MEASURE_1).let {
  it.addAll(pH.toByteArray().toList())
  it.addAll(temp.toByteArray().toList())
  it.toByteArray()
}

fun measureResponseTwo(a578: Float, a434: Float, dark: Float) = makeResponseBuffer(PhyterResponse.MEASURE_2).let {
  it.addAll(a578.toByteArray().toList())
  it.addAll(a434.toByteArray().toList())
  it.addAll(dark.toByteArray().toList())
  it.toByteArray()
}