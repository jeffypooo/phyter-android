package com.jmjproductdev.phyter.app.scenes.measure

import com.jmjproductdev.phyter.android.bluetooth.Devices
import com.jmjproductdev.phyter.app.common.presentation.PhyterPresenter
import com.jmjproductdev.phyter.app.common.presentation.PhyterView
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.jmjproductdev.phyter.core.instrument.MeasurementData
import com.jmjproductdev.phyter.util.disposedBy
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

interface MeasureView : PhyterView {
  var deviceName: String
  var salinity: Float
  var salinityFieldEnabled: Boolean
  var actionButtonText: String
  var actionButtonEnabled: Boolean
  fun addMeasurement(measurementData: MeasurementData)
  fun presentErrorDialog(msg: String)
  fun presentDisconnectingDialog(msg: String)
  fun dismissDisconnectingDialog()

}

class MeasurePresenter : PhyterPresenter<MeasureView>() {

  private enum class ActionState {
    BACKGROUND, MEASURE;

    val buttonText: String
      get() {
        return when (this) {
          BACKGROUND -> "Measure Background"
          MEASURE    -> "Measure pH"
        }
      }
  }

  companion object {
    const val ERR_MSG_BACKGROUND = "Something went wrong while measuring the background."
    const val ERR_MSG_MEASURE = "Something went wrong while measuring the pH."
  }

  private val device: Phyter? get() = Devices.shared().activeDevice
  private val deviceCalls = CompositeDisposable()
  private var actionState = ActionState.BACKGROUND

  override fun onCreate(view: MeasureView) {
    super.onCreate(view)
    bindSalinityToView()
    updateDeviceName()
  }

  override fun onDestroy() {
    super.onDestroy()
    deviceCalls.clear()
  }

  fun onSalinityEntered() {
    val salinity = view?.salinity ?: return
    device?.let {
      it.setSalinity(salinity)
          .subscribe(
              { Timber.d("salinity set successfully") },
              { Timber.e("failed to set salinity: $it") }
          )
          .disposedBy(deviceCalls)
    }
  }

  fun onActionButtonClicked() {
    when (actionState) {
      ActionState.BACKGROUND -> measureBackground()
      ActionState.MEASURE    -> measurePh()
    }
  }

  fun onBackPressed() {
    doDisconnect()
  }

  private fun underContruction() {
    view?.showSnackbar("under construction!")
  }

  private fun bindSalinityToView() {
    val salinity = device?.salinity ?: return
    salinity.subscribe(
        { view?.salinity = it.also { Timber.d("salinity set to $it") } },
        { Timber.e(it, "salinity error") }
    ).also { deviceCalls.add(it) }
  }

  private fun updateDeviceName() {
    device?.apply {
      view?.deviceName = name
    }
  }

  private fun measureBackground() {
    val bg = device?.background() ?: return
    Timber.d("performing background")
    view?.apply {
      actionButtonEnabled = false
      salinityFieldEnabled = false
    }
    bg.subscribe({ backgroundComplete() }, { backgroundFailed(it) }).disposedBy(deviceCalls)
  }

  private fun backgroundComplete() {
    Timber.d("background complete")
    view?.actionButtonEnabled = true
    moveToNextActionState()
  }

  private fun backgroundFailed(err: Throwable) {
    Timber.e(err, "background failed")
    view?.apply {
      actionButtonEnabled = true
      salinityFieldEnabled = true
      presentErrorDialog(ERR_MSG_BACKGROUND)
    }

  }

  private fun measurePh() {
    val measure = device?.measure() ?: return
    Timber.d("performing pH measurement")
    view?.actionButtonEnabled = false
    measure.subscribe({ measureComplete(it) }, { measureFailed(it) }).disposedBy(deviceCalls)
  }

  private fun measureComplete(measurementData: MeasurementData) {
    view?.apply {
      actionButtonEnabled = true
      salinityFieldEnabled = true
      addMeasurement(measurementData)
    }
    moveToNextActionState()
  }

  private fun measureFailed(err: Throwable) {
    Timber.d(err, "measurement failed")
    view?.apply {
      actionButtonEnabled = true
      salinityFieldEnabled = true
      presentErrorDialog(ERR_MSG_MEASURE)
    }
    setActionState(ActionState.BACKGROUND)
  }

  private fun moveToNextActionState() {
    when (actionState) {
      ActionState.BACKGROUND -> setActionState(ActionState.MEASURE)
      ActionState.MEASURE    -> setActionState(ActionState.BACKGROUND)
    }
  }

  private fun setActionState(state: ActionState) {
    this.actionState = state
    view?.actionButtonText = actionState.buttonText
  }

  private fun doDisconnect() {
    deviceCalls.clear()
    Timber.d("disconnecting device")
    device?.let {
      view?.presentDisconnectingDialog("Disconnecting from ${it.name}...")
      it.disconnect().subscribe({ disconnectComplete() }, { disconnectFailed(it) }).disposedBy(deviceCalls)
    }
  }

  private fun disconnectComplete() {
    Timber.d("disconnect complete")
    view?.apply {
      dismissDisconnectingDialog()
      dismiss()
    }
  }

  private fun disconnectFailed(err: Throwable) {
    Timber.e(err, "disconnect failed")
    view?.apply {
      dismissDisconnectingDialog()
      dismiss()
    }
  }

}