package com.jmjproductdev.phyter.app.scenes.measure

import com.jmjproductdev.phyter.MockitoTest
import com.jmjproductdev.phyter.android.bluetooth.Devices
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.instrument.MeasurementData
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

private fun emptyMeasurement() = MeasurementData(0f, 0f, 0f, 0f, 0f)

@Suppress("RemoveRedundantBackticks")
class MeasurePresenterTest : MockitoTest() {

  companion object {
    const val MOCK_PHYTER_NAME = "FooPhyter"
    const val MOCK_PHYTER_INITIAL_SAL = 35.0F
  }

  @Mock lateinit var mockPhyter: Phyter
  @Mock lateinit var mockView: MeasureView

  /* stubs */
  lateinit var salinityStub: BehaviorSubject<Float>

  lateinit var presenter: MeasurePresenter

  @Before
  fun setup() {
    whenever(mockPhyter.name).thenReturn(MOCK_PHYTER_NAME)
    salinityStub = BehaviorSubject.createDefault(MOCK_PHYTER_INITIAL_SAL)
    whenever(mockPhyter.salinity).thenReturn(salinityStub)
    Devices.shared().activeDevice = mockPhyter
    presenter = MeasurePresenter()
  }

  @Test
  fun `on creation, the device name is set`() {
    presenter.onCreate(mockView)
    verify(mockView).deviceName = MOCK_PHYTER_NAME
  }

  @Test
  fun `on creation, the salinity is set from the device`() {
    presenter.onCreate(mockView)
    verify(mockView).salinity = MOCK_PHYTER_INITIAL_SAL
  }

  @Test
  fun `background is invoked for first action button click, then measure for second click`() {
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    whenever(mockPhyter.measure()).thenReturn(Single.just(emptyMeasurement()))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    with(mockPhyter) {
      verify(this).background()
      verify(this).measure()
    }
  }

  @Test
  fun `action button is disabled while background is running`() {
    val backgroundStub = CompletableSubject.create()
    whenever(mockPhyter.background()).thenReturn(backgroundStub)
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    verify(mockView).actionButtonEnabled = false
    backgroundStub.onComplete()
    verify(mockView).actionButtonEnabled = true
  }

  @Test
  fun `action button is disabled while measure is running`() {
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    val measureStub = SingleSubject.create<MeasurementData>()
    whenever(mockPhyter.measure()).thenReturn(measureStub)
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    verify(mockView, times(2)).actionButtonEnabled = false
    measureStub.onSuccess(emptyMeasurement())
    verify(mockView, times(2)).actionButtonEnabled = true
  }

  @Test
  fun `action button text is changed for each step`() {
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    whenever(mockPhyter.measure()).thenReturn(Single.just(emptyMeasurement()))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    with(mockView) {
      verify(this).actionButtonText = "Measure pH"
      verify(this).actionButtonText = "Measure Background"
    }
  }

  @Test
  fun `salinity field is disabled until measurement is finished`() {
    val backgroundStub = CompletableSubject.create()
    whenever(mockPhyter.background()).thenReturn(backgroundStub)
    val measureStub = SingleSubject.create<MeasurementData>()
    whenever(mockPhyter.measure()).thenReturn(measureStub)
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    verify(mockView).salinityFieldEnabled = false
    backgroundStub.onComplete()
    verify(mockView, never()).salinityFieldEnabled = true
    presenter.onActionButtonClicked()
    verify(mockView, never()).salinityFieldEnabled = true
    measureStub.onSuccess(emptyMeasurement())
    verify(mockView).salinityFieldEnabled = true
  }

  @Test
  fun `measurements are added to view upon completion`() {
    val measureStub = SingleSubject.create<MeasurementData>()
    with(mockPhyter) {
      whenever(background()).thenReturn(Completable.complete())
      whenever(measure()).thenReturn(measureStub)
    }
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    val measurement = MeasurementData(ph = 7f)
    measureStub.onSuccess(measurement)
    verify(mockView).addMeasurement(measurement)
  }

  @Test
  fun `action button is re-enabled and an error is shown if background fails`() {
    whenever(mockPhyter.background()).thenReturn(Completable.error(Exception("foo")))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    with(mockView) {
      verify(this).actionButtonEnabled = false
      verify(this).actionButtonEnabled = true
      verify(this).presentErrorDialog("Something went wrong while measuring the background.")
    }
  }

  @Test
  fun `action button is re-enabled and an error is shown if measurement fails`() {
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    whenever(mockPhyter.measure()).thenReturn(Single.error(Exception("foo")))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    with(mockView) {
      verify(this, times(2)).actionButtonEnabled = false
      verify(this, times(2)).actionButtonEnabled = true
      verify(this).presentErrorDialog("Something went wrong while measuring the pH.")
    }
  }

  @Test
  fun `action button is reset to background if measurement fails`() {
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    whenever(mockPhyter.measure()).thenReturn(Single.error(Exception("foo")))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    with(mockView) {
      verify(this).actionButtonText = "Measure Background"
    }
  }

  @Test
  fun `salinity field is re-enabled if background fails`() {
    whenever(mockPhyter.background()).thenReturn(Completable.error(Exception("foo")))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    with(mockView) {
      verify(this).salinityFieldEnabled = false
      verify(this).salinityFieldEnabled = true
    }
  }

  @Test
  fun `salinity field is re-enabled if measurement fails`() {
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    whenever(mockPhyter.measure()).thenReturn(Single.error(Exception("foo")))
    driveOnCreate()
    presenter.onActionButtonClicked() // background
    presenter.onActionButtonClicked() // measure
    with(mockView) {
      verify(this).salinityFieldEnabled = false
      verify(this).salinityFieldEnabled = true
    }
  }

  @Test
  fun `on back press, device is disconnected`() {
    val disconnectStub = CompletableSubject.create()
    whenever(mockPhyter.disconnect()).thenReturn(disconnectStub)
    driveOnCreate()
    presenter.onBackPressed()
    verify(mockPhyter).disconnect()
    assertThat(disconnectStub.hasObservers(), equalTo(true))
  }

  @Test
  fun `on back press, dialog is shown while disconnection is running`() {
    val disconnectStub = CompletableSubject.create()
    whenever(mockPhyter.disconnect()).thenReturn(disconnectStub)
    driveOnCreate()
    presenter.onBackPressed()
    with(mockView) {
      verify(this).presentDisconnectingDialog("Disconnecting from $MOCK_PHYTER_NAME...")
      disconnectStub.onComplete()
      verify(this).dismissDisconnectingDialog()
    }
  }

  @Test
  fun `on back press, view is dismissed after disconnection`() {
    whenever(mockPhyter.disconnect()).thenReturn(Completable.complete())
    driveOnCreate()
    presenter.onBackPressed()
    verify(mockView).dismiss()
  }

  @Test
  fun `on back press, view is dismissed if error occurs during disconnection`() {
    whenever(mockPhyter.disconnect()).thenReturn(Completable.error(Exception("foo")))
    driveOnCreate()
    presenter.onBackPressed()
    verify(mockView).dismiss()
  }

  @Test
  fun `on destruction, background observers are disposed`() {
    driveOnCreate()
    val backgroundStub = CompletableSubject.create()
    whenever(mockPhyter.background()).thenReturn(backgroundStub)
    presenter.onActionButtonClicked()
    assertThat(backgroundStub.hasObservers(), equalTo(true))
    presenter.onDestroy()
    assertThat(backgroundStub.hasObservers(), equalTo(false))
  }

  @Test
  fun `on destruction, measure observers are disposed`() {
    driveOnCreate()
    whenever(mockPhyter.background()).thenReturn(Completable.complete())
    val measureStub = SingleSubject.create<MeasurementData>()
    whenever(mockPhyter.measure()).thenReturn(measureStub)
    presenter.onActionButtonClicked()
    presenter.onActionButtonClicked()
    assertThat(measureStub.hasObservers(), equalTo(true))
    presenter.onDestroy()
    assertThat(measureStub.hasObservers(), equalTo(false))
  }

  @Test
  fun `on destruction, salinity observers are disposed`() {
    driveOnCreate()
    assertThat(salinityStub.hasObservers(), equalTo(true))
    presenter.onDestroy()
    assertThat(salinityStub.hasObservers(), equalTo(false))
  }

  private fun driveOnCreate() {
    presenter.onCreate(mockView)
    with(mockView) {
      verify(this).deviceName = MOCK_PHYTER_NAME
      verify(this).salinity = MOCK_PHYTER_INITIAL_SAL
    }
  }

}