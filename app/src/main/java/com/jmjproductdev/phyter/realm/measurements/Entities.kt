package com.jmjproductdev.phyter.realm.measurements

import com.jmjproductdev.phyter.core.data.measurements.MeasurementRecord
import io.realm.RealmObject
import java.util.*

class RealmMeasurementRecord(
    override var instrumentMacAddress: String = "",
    override var timestamp: Date = Date(),
    override var salinity: Float = 0f,
    override var pH: Float = 0f,
    override var temperature: Float = 0f,
    override var dark: Float = 0f,
    override var a578: Float = 0f,
    override var a434: Float = 0f
) : RealmObject(), MeasurementRecord {


}