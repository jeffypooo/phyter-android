package com.jmjproductdev.phyter.app.scenes.simulator

import com.jmjproductdev.phyter.MockitoTest
import com.jmjproductdev.phyter.core.bluetooth.BLEManager
import com.jmjproductdev.phyter.core.bluetooth.BLEPeripheral
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class SimulatorPresenterTest : MockitoTest() {

  @Mock lateinit var mockBLEManager: BLEManager
  @Mock lateinit var mockPeripheral: BLEPeripheral
  @Mock lateinit var mockView: SimulatorView

  lateinit var presenter: SimulatorPresenter

  @Before
  fun setup() {
    whenever(mockBLEManager.createPeripheral(any(), any())).thenReturn(mockPeripheral)
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
  fun onControlButtonClick_createsPeripheralAndStartsAdvertising() {
    whenever(mockView.nameFieldText).thenReturn("foo")
    val expUuid = UUID(0L, 0xFFE0L)
    driveOnCreate()
    presenter.onControlButtonClick()
    verify(mockBLEManager).createPeripheral(eq(expUuid), eq("foo"))
    verify(mockPeripheral).advertising = true
  }

  @Test
  fun onControlButtonClick_stopsAdvertisingPeripheral() {
    driveOnCreate()
    presenter.onControlButtonClick()
    verify(mockPeripheral).advertising = true
    presenter.onControlButtonClick()
    verify(mockPeripheral).advertising = false
  }

  @Test
  fun onControlButtonClick_changesButtonText() {
    driveOnCreate()
    presenter.onControlButtonClick()
    verify(mockView).controlButtonText = "Stop"
    presenter.onControlButtonClick()
    verify(mockView).controlButtonText = "Start"
  }

  @Test
  fun onControlButtonClick_enablesAndDisablesControls() {
    driveOnCreate()
    presenter.onControlButtonClick()
    verify(mockView).configurationControlsEnabled = false
    presenter.onControlButtonClick()
    verify(mockView).configurationControlsEnabled = true
  }

  private fun driveOnCreate() {
    presenter.onCreate(mockView)
    argumentCaptor<String>().apply {
      verify(mockView).nameFieldText = capture()
      assertThat(lastValue, startsWith("RandomPhyter"))
    }
  }

}