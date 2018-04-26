package com.jmjproductdev.phyter.app.scenes.launch

import com.jmjproductdev.phyter.MockitoTest
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.instrument.PhyterScanner
import com.jmjproductdev.phyter.core.permissions.Permission
import com.jmjproductdev.phyter.core.permissions.PermissionsManager
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class LaunchPresenterTest : MockitoTest() {

  @Mock lateinit var mockPermManager: PermissionsManager
  @Mock lateinit var mockBLEManager: BLEManager
  @Mock lateinit var mockScanner: PhyterScanner
  @Mock lateinit var mockView: LaunchView

  /* stub subjects */
  lateinit var permissionSubject: PublishSubject<Pair<Permission, Boolean>>
  lateinit var scanSubject: PublishSubject<Phyter>

  /* class under test */
  lateinit var presenter: LaunchPresenter

  @Before
  fun setup() {
    /* permissions manager defaults */
    permissionSubject = PublishSubject.create()
    with(mockPermManager) {
      whenever(granted(Permission.ACCESS_FINE_LOCATION)).thenReturn(true)
      whenever(request(any())).thenReturn(permissionSubject)
    }
    /* ble manager defaults */
    with(mockBLEManager) {
      whenever(enabled).thenReturn(true)
      whenever(scanner).thenReturn(mockScanner)
    }
    /* scanner defaults */
    scanSubject = PublishSubject.create()
    with(mockScanner) {
      whenever(scan()).thenReturn(scanSubject)
    }
    presenter = LaunchPresenter(mockPermManager, mockBLEManager)
  }

  @Test
  fun onCreate_checksFineLocationPermission() {
    presenter.onCreate(mockView)
    verify(mockPermManager).granted(Permission.ACCESS_FINE_LOCATION)
  }

  @Test
  fun onCreate_checksIfBluetoothIsEnabled() {
    presenter.onCreate(mockView)
    verify(mockBLEManager).enabled
  }

  @Test
  fun onCreate_startsScanningAndShowsRefreshing() {
    whenever(mockScanner.scan()).thenReturn(Observable.never())
    presenter.onCreate(mockView)
    verify(mockScanner).scan()
    verify(mockView).refresing = true
  }

  @Test
  fun onCreate_requestFineLocationPermission() {
    whenever(mockPermManager.granted(Permission.ACCESS_FINE_LOCATION)).thenReturn(false)
    presenter.onCreate(mockView)
    verify(mockPermManager).request(Permission.ACCESS_FINE_LOCATION)
  }

  @Test
  fun onCreate_checksBluetoothAfterPermissionGranted() {
    whenever(mockPermManager.granted(Permission.ACCESS_FINE_LOCATION)).thenReturn(false)
    presenter.onCreate(mockView)
    verify(mockBLEManager, never()).enabled
    permissionSubject.onNext(Pair(Permission.ACCESS_FINE_LOCATION, true))
    verify(mockBLEManager).enabled
  }

  @Test
  fun onCreate_requestsToEnableBluetooth() {
    val enableSubject = SingleSubject.create<Boolean>()
    with(mockBLEManager) {
      whenever(enabled).thenReturn(false)
      whenever(requestEnable()).thenReturn(enableSubject)
    }
    presenter.onCreate(mockView)
    verify(mockBLEManager).requestEnable()
    assertThat(enableSubject.hasObservers(), equalTo(true))
  }

  @Test
  fun onCreate_startsScanningAfterEnablingBluetooth() {
    val enableSubject = SingleSubject.create<Boolean>()
    with(mockBLEManager) {
      whenever(enabled).thenReturn(false)
      whenever(requestEnable()).thenReturn(enableSubject)
    }
    val scanSubject = PublishSubject.create<Phyter>()
    with(mockScanner) {
      whenever(scan()).thenReturn(scanSubject)
    }
    presenter.onCreate(mockView)
    with(mockBLEManager) {
      verify(this, never()).scanner
      enableSubject.onSuccess(true)
      verify(this, times(1)).scanner
    }
    with(mockScanner) {
      verify(this).scan()
      assertThat(scanSubject.hasObservers(), equalTo(true))
    }
  }

  @Test
  fun onRefresh_startsScanAndShowsRefreshing() {
    driveOnCreate()
    presenter.onRefresh()
    verify(mockScanner, times(2)).scan()
    verify(mockView, times(2)).refresing = true
  }

  @Test
  fun scanning_showsDevices() {
    driveOnCreate(completeInitialScan = false)
    val dev = mock<Phyter> {}
    scanSubject.onNext(dev)
    verify(mockView).add(dev)
  }

  @Test
  fun scanning_doesNotShowDuplicates() {
    driveOnCreate(completeInitialScan = false)
    val dev = mock<Phyter> {
      on { name } doReturn "foo"
      on { address } doReturn "00:11:22:33:44:55"
      on { rssi } doReturn -65
    }
    scanSubject.run {
      onNext(dev)
      onNext(dev)
    }
    verify(mockView, times(1)).add(dev)
  }

  @Test
  fun scanning_updatesPreviousDeviceWithDifferentName() {
    driveOnCreate(completeInitialScan = false)
    val dev = mock<Phyter> {
      on { name } doReturn listOf("foo", "foo bar")
      on { address } doReturn "00:11:22:33:44:55"
    }
    scanSubject.run {
      onNext(dev)
      onNext(dev)
    }
    verify(mockView, times(1)).add(dev)
    verify(mockView, times(1)).update(dev)
  }

  @Test
  fun scanning_updatesPreviousDeviceWithDifferentRSSI() {
    driveOnCreate(completeInitialScan = false)
    val dev = mock<Phyter> {
      on { name } doReturn "foo"
      on { address } doReturn "00:11:22:33:44:55"
      on { rssi } doReturn listOf<Short>(-55, -60)
    }
    scanSubject.run {
      onNext(dev)
      onNext(dev)
    }
    verify(mockView, times(1)).add(dev)
    verify(mockView, times(1)).update(dev)
  }

  private fun driveOnCreate(completeInitialScan: Boolean = true) {
    presenter.onCreate(mockView)
    with(mockBLEManager) {
      verify(this).enabled
      verify(this).scanner
    }
    with(mockScanner) {
      verify(this).scan()
      assertThat(scanSubject.hasObservers(), equalTo(true))
    }
    if (!completeInitialScan) return
    scanSubject.onComplete()
    scanSubject = PublishSubject.create() // need to recreate here so it can be used again if needed
    verify(mockView).refresing = false
  }


}