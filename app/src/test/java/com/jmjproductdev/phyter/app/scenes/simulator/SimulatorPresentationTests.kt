package com.jmjproductdev.phyter.app.scenes.simulator

import com.jmjproductdev.phyter.MockitoTest
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.*

class SimulatorPresenterTest: MockitoTest() {

  @Mock lateinit var mockBLEManager: BLEManager
  @Mock lateinit var mockView: SimulatorView

  lateinit var presenter: SimulatorPresenter

  @Before
  fun setup() {
    presenter = SimulatorPresenter(mockBLEManager)
  }

  @Test
  fun onCreate_setsRandomName() {
    presenter.onCreate(mockView)
    argumentCaptor<String>().apply {
      verify(mockView).nameFieldText = capture()
      assertThat(lastValue, startsWith("RandomPhyter"))
    }
  }

  @Test
  fun onCreate_setsRandomAddress() {
    presenter.onCreate(mockView)
    argumentCaptor<String>().apply {
      verify(mockView).addressFieldText = capture()
      assertThat(lastValue.length, equalTo(17))
    }
  }

  @Test
  fun onControlButtonClick_createsPeripheral() {
    val expUuid = UUID(0L, 0xFFE0L)
    driveOnCreate()
    presenter.onActionButtonClick()
    verify(mockBLEManager).createPeripheral(eq(expUuid))
  }

  private fun driveOnCreate() {
    presenter.onCreate(mockView)
    argumentCaptor<String>().apply {
      verify(mockView).nameFieldText = capture()
      assertThat(lastValue, startsWith("RandomPhyter"))
      verify(mockView).addressFieldText = capture()
      assertThat(lastValue.length, equalTo(17))
    }
  }

}