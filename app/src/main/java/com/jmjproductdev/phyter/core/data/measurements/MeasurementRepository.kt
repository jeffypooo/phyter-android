package com.jmjproductdev.phyter.core.data.measurements

import io.reactivex.Observable

enum class ChangeSetState {
  INITIAL, UPDATE, ERROR
}

class ChangeSet<T>(
    var state: ChangeSetState = ChangeSetState.INITIAL,
    var items: List<T> = mutableListOf(),
    var deletions: List<Int> = mutableListOf(),
    var changes: List<Int> = mutableListOf(),
    var insertions: List<Int> = mutableListOf()
)


interface MeasurementRepository {
  fun create(): MeasurementRecord
  fun measurements(macAddress: String): Observable<ChangeSet<MeasurementRecord>>
  fun delete(record: MeasurementRecord): Boolean
}