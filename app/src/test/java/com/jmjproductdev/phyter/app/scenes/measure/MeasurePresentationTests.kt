package com.jmjproductdev.phyter.app.scenes.measure

import com.jmjproductdev.phyter.MockitoTest
import com.jmjproductdev.phyter.android.bluetooth.Devices
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class MeasurePresenterTest : MockitoTest() {

  companion object {
    const val MOCK_PHYTER_NAME = "FooPhyter"
  }

  @Mock lateinit var mockPhyter: Phyter
  @Mock lateinit var mockView: MeasureView

  lateinit var presenter: MeasurePresenter

  @Before
  fun setup() {
    whenever(mockPhyter.name).thenReturn(MOCK_PHYTER_NAME)
    Devices.shared().activeDevice = mockPhyter
    presenter = MeasurePresenter()
  }

  @Test
  fun onCreate_setsDeviceName() {
    presenter.onCreate(mockView)
    verify(mockView).deviceName = MOCK_PHYTER_NAME
  }

}