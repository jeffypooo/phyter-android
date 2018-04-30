package com.jmjproductdev.phyter.realm.measurements

import com.jmjproductdev.phyter.core.data.measurements.ChangeSet
import com.jmjproductdev.phyter.core.data.measurements.ChangeSetState
import com.jmjproductdev.phyter.core.data.measurements.MeasurementRecord
import com.jmjproductdev.phyter.core.data.measurements.MeasurementRepository
import io.reactivex.Observable
import io.realm.*
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.util.*

private fun OrderedCollectionChangeSet.State.asChangeSetState(): ChangeSetState {
  return when (this) {
    OrderedCollectionChangeSet.State.INITIAL -> ChangeSetState.INITIAL
    OrderedCollectionChangeSet.State.UPDATE  -> ChangeSetState.UPDATE
    OrderedCollectionChangeSet.State.ERROR   -> ChangeSetState.ERROR
  }
}

class RealmMeasurementRepository() : MeasurementRepository {


  companion object {
    private const val FIELD_MAC_ADDR = "instrumentMacAddress"
    private const val FIELD_TIMESTAMP = "timestamp"
  }

  private val realm: Realm by lazy { Realm.getDefaultInstance() }

  override fun create(): MeasurementRecord {
    return realm.createObject<RealmMeasurementRecord>()
  }

  override fun measurements(macAddress: String): Observable<ChangeSet<MeasurementRecord>> {
    return Observable.defer<ChangeSet<MeasurementRecord>> {
      val realmObs = findAll(macAddress).asChangesetObservable()
      return@defer realmObs.map {
        val items = it.collection.toList().map { it as MeasurementRecord }
        val set = ChangeSet(items = items)
        it.changeset?.apply {
          set.state = this.state.asChangeSetState()
          set.deletions = this.deletions.toList()
          set.changes = this.changes.toList()
          set.insertions = this.insertions.toList()
        }
        return@map set
      }
    }
  }

  override fun delete(record: MeasurementRecord): Boolean {
    if (record is RealmMeasurementRecord) {
      realm.executeTransaction { record.deleteFromRealm() }
      return true
    }
    val match = findByTime(record.timestamp) ?: return false
    realm.executeTransaction { match.deleteFromRealm() }
    return true
  }

  fun dispose() {
    realm.close()
  }

  private fun findAll(address: String): RealmResults<RealmMeasurementRecord> = records()
      .equalTo(FIELD_MAC_ADDR, address)
      .findAll()
      .sort(FIELD_TIMESTAMP, Sort.DESCENDING)

  private fun findByTime(time: Date): RealmMeasurementRecord? = records().equalTo(FIELD_TIMESTAMP, time).findFirst()

  private fun records(): RealmQuery<RealmMeasurementRecord> = realm.where()
}