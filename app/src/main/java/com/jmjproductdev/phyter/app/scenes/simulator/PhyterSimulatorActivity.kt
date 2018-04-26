package com.jmjproductdev.phyter.app.scenes.simulator

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.Editable
import android.text.TextWatcher
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.android.activity.PhyterActivity
import com.jmjproductdev.phyter.android.bluetooth.AndroidBLEManager
import kotlinx.android.synthetic.main.activity_phyter_simulator.*
import timber.log.Timber

class PhyterSimulatorActivity : PhyterActivity(), SimulatorView {

  override var nameFieldText: String
    get() = nameField.text.toString()
    set(value) {
      runOnUiThread {
        nameField.text.clear()
        nameField.text.append(value)
      }
    }
  override var addressFieldText: String
    get() = addressField.text.toString()
    set(value) {
      runOnUiThread {
        addressField.text.apply {
          clear()
          append(value)
        }
      }
    }
  override var configurationControlsEnabled: Boolean
    get() = nameField.isEnabled && addressField.isEnabled && randomizeButton.isEnabled
    set(value) {
      runOnUiThread {
        nameField.isEnabled = value
        addressField.isEnabled = value
        randomizeButton.isEnabled = value
      }
    }
  override var controlButtonText: String
    get() = startButton.text.toString()
    set(value) {
      startButton.text = value
    }

  private val addressFieldWatcher = object : TextWatcher {
    override fun afterTextChanged(s: Editable) {
      Timber.i("address field changed: '$s'")
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
  }

  private lateinit var bleManager: AndroidBLEManager
  private lateinit var presenter: SimulatorPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_phyter_simulator)
    setupViews()
    bleManager = AndroidBLEManager(this)
    presenter = SimulatorPresenter(bleManager).apply { onCreate(this@PhyterSimulatorActivity) }
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.onDestroy()
    with(addressField) {
      removeTextChangedListener(addressFieldWatcher)
    }
  }


  private fun setupViews() {
    /* address field */
    with(addressField) {
      addTextChangedListener(addressFieldWatcher)
    }
    /* address randomize button */
    with(randomizeButton) {
      setOnClickListener { presenter.onRandomizeButtonClick() }
    }
    /* start button */
    with(startButton) {
      setOnClickListener { presenter.onActionButtonClick() }
    }
  }

  override fun showSnackbar(msg: String) {
    runOnUiThread { Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT).show() }
  }

  override fun dismiss() {
    runOnUiThread { finish() }
  }
}
