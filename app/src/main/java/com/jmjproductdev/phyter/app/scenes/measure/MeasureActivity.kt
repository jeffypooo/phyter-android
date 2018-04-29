package com.jmjproductdev.phyter.app.scenes.measure

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.inputmethod.EditorInfo
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.android.activity.PhyterActivity
import com.jmjproductdev.phyter.android.activity.hideKeyboard
import kotlinx.android.synthetic.main.activity_measure.*
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

  override var actionButtonText: String
    get() = actionButton.text?.toString() ?: ""
    set(value) {
      runOnUiThread { actionButton.text = value }
    }

  private lateinit var presenter: MeasurePresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_measure)
    setupViews()
    presenter = MeasurePresenter()
    presenter.onCreate(this)
  }

  override fun showSnackbar(msg: String) {
    runOnUiThread { Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT).show() }
  }

  override fun dismiss() {
    runOnUiThread { dismiss() }
  }

  private fun setupViews() {
    with(salinityField) {
      setOnEditorActionListener { _, actionId, _ -> onSalinityFieldAction(actionId) }
    }
    with(actionButton) {
      setOnClickListener { presenter.onActionButtonClicked() }
    }
  }

  private fun onSalinityFieldAction(actionId: Int): Boolean {
    return if (actionId == EditorInfo.IME_ACTION_DONE) {
      salinityField.hideKeyboard()
      presenter.onSalinityEntered()
      true
    } else {
      false
    }
  }

}
