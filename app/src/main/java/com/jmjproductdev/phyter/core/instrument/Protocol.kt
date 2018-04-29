package com.jmjproductdev.phyter.core.instrument

enum class PhyterCommand(private val id: Int) {
  SET_SALINITY(0x1),
  BACKGROUND(0x2),
  MEASURE(0x3),
  LED_INTENSITY_CHECK(0x4);

  val commandId: Byte get() = id.toByte()
}

enum class PhyterResponse(private val id: Int) {
  SET_SALINITY(0x81),
  BACKGROUND(0x82),
  MEASURE_1(0x83),
  MEASURE_2(0x84),
  LED_INTENSITY_CHECK(0x85),
  ERROR(0xFF);

  val responseId: Byte get() = id.toByte()
}


class ResponseParser {

  interface Delegate {
    fun onSalinityResponse(salinity: Float) {}
    fun onBackgroundResponse() {}
    fun onMeasureResponse(measurement: PhyterMeasurement) {}
    fun onLedIntensityCheckResponse() {}
  }

  fun parse(bytes: ByteArray) {
    TODO("implement parse()")
  }

}

class CommandParser {

  interface Delegate {
    fun onSalinityCommand(salinity: Float) {}
    fun onBackgroundCommand() {}
    fun onMeasureCommand() {}
    fun onLedIntensityCheckCommand() {}
  }

  fun parse(bytes: ByteArray) {
    TODO("implement parse()")
  }

}