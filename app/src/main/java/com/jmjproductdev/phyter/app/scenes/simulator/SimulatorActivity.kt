package com.jmjproductdev.phyter.app.scenes.simulator

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.Editable
import android.text.TextWatcher
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.android.activity.PhyterActivity
import com.jmjproductdev.phyter.android.bluetooth.ActivityBLEManager
import kotlinx.android.synthetic.main.activity_phyter_simulator.*
import timber.log.Timber

class SimulatorActivity : PhyterActivity(), SimulatorView {

  override var nameFieldText: String
    get() = nameField.text.toString()
    set(value) {
      runOnUiThread {
        nameField.text.clear()
        nameField.text.append(value)
      }
    }
  override var configurationControlsEnabled: Boolean
    get() = nameField.isEnabled
    set(value) {
      runOnUiThread { nameField.isEnabled = value }
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

  private lateinit var bleManager: ActivityBLEManager
  private lateinit var presenter: SimulatorPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_phyter_simulator)
    setupViews()
    bleManager = ActivityBLEManager(this)
    presenter = SimulatorPresenter(bleManager).apply { onCreate(this@SimulatorActivity) }
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.onDestroy()
  }


  private fun setupViews() {
    /* start button */
    with(startButton) {
      setOnClickListener { presenter.onControlButtonClick() }
    }
  }

  override fun showSnackbar(msg: String) {
    runOnUiThread { Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT).show() }
  }

  override fun dismiss() {
    runOnUiThread { finish() }
  }
}
