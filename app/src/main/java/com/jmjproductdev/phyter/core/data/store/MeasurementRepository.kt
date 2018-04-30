package com.jmjproductdev.phyter.core.data.store

import io.reactivex.Observable

interface MeasurementRepository {
  fun measurements(forInstrumentMacAddress: String): Observable<List<MeasurementRecord>>
  fun delete(measurementRecord: MeasurementRecord): Boolean
}