package com.jmjproductdev.phyter.core.data.measurements

import java.util.*

interface Geolocation {
  var latitude: Double
  var longitude: Double
  var altitude: Double
  var horizontalAccuracy: Double
  var verticalAccuracy: Double
  var timestamp: Date
}

interface MeasurementRecord {

  var instrumentMacAddress: String
  var timestamp: Date
  var salinity: Float
  var pH: Float
  var temperature: Float
  var dark: Float
  var a578: Float
  var a434: Float

}

