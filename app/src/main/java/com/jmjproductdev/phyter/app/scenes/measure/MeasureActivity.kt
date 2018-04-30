package com.jmjproductdev.phyter.app.scenes.measure

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.inputmethod.EditorInfo
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.android.activity.PhyterActivity
import com.jmjproductdev.phyter.android.activity.hideKeyboard
import com.jmjproductdev.phyter.core.instrument.MeasurementData
import kotlinx.android.synthetic.main.activity_measure.*
import timber.log.Timber
import java.text.DecimalFormat

class MeasureActivity : PhyterActivity(), MeasureView {


  override var deviceName: String
    get() = deviceNameText.text?.toString() ?: ""
    set(value) {
      runOnUiThread { deviceNameText.text = value }
    }
  override var salinity: Float
    get() = salinityField.text?.toString()?.toFloat() ?: 35.0F
    set(value) {
      runOnUiThread {
        val df = DecimalFormat()
        df.minimumFractionDigits = 1
        df.maximumFractionDigits = 3
        salinityField.text.clear()
        salinityField.text.append(df.format(value))
      }
    }
  override var salinityFieldEnabled: Boolean
    get() = salinityField.isEnabled
    set(value) {
      runOnUiThread { salinityField.isEnabled = value }
    }
  override var actionButtonText: String
    get() = actionButton.text?.toString() ?: ""
    set(value) {
      runOnUiThread { actionButton.text = value }
    }
  override var actionButtonEnabled: Boolean
    get() = actionButton.isEnabled
    set(value) {
      runOnUiThread { actionButton.isEnabled = value }
    }

  private lateinit var presenter: MeasurePresenter
  private var errorDialog: AlertDialog? = null
  private var disconnectingDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.i("created")
    setContentView(R.layout.activity_measure)
    setupViews()
    presenter = MeasurePresenter()
    presenter.onCreate(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.i("destroyed")
    presenter.onDestroy()
  }

  override fun onBackPressed() {
    Timber.i("back pressed")
    presenter.onBackPressed()
  }

  override fun showSnackbar(msg: String) {
    runOnUiThread {
      Timber.i("showing snackbar '$msg'")
      Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun dismiss() {
    runOnUiThread {
      Timber.i("finishing")
      finish()
    }
  }

  override fun addMeasurement(measurementData: MeasurementData) {

  }

  override fun presentErrorDialog(msg: String) {
    runOnUiThread {
      Timber.i("presenting error dialog with '$msg'")
      errorDialog?.dismiss()
      errorDialog = AlertDialog.Builder(this).let {
        it.setTitle("Error")
        it.setMessage(msg)
        it.setCancelable(false)
        it.setNeutralButton("OK") { dialog, _ -> dialog.dismiss() }
        it.show()
      }
    }
  }

  override fun presentDisconnectingDialog(msg: String) {
    runOnUiThread {
      disconnectingDialog?.dismiss()
      disconnectingDialog = AlertDialog.Builder(this).let {
        it.setTitle("Please wait")
        it.setMessage(msg)
        it.setCancelable(false)
        it.show()
      }
    }
  }

  override fun dismissDisconnectingDialog() {
    runOnUiThread { disconnectingDialog?.dismiss() }
  }

  private fun setupViews() {
    with(salinityField) {
      setOnEditorActionListener { _, actionId, _ -> onSalinityFieldAction(actionId) }
    }
    with(actionButton) {
      setOnClickListener { presenter.onActionButtonClicked() }
    }
    with(historyRecycler) {
      layoutManager = LinearLayoutManager(this@MeasureActivity)
    }
  }

  private fun onSalinityFieldAction(actionId: Int): Boolean {
    return if (actionId == EditorInfo.IME_ACTION_DONE) {
      Timber.i("salinity entered as $salinity")
      salinityField.hideKeyboard()
      presenter.onSalinityEntered()
      true
    } else {
      false
    }
  }

}
