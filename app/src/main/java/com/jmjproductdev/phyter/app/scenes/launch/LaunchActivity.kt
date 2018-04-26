package com.jmjproductdev.phyter.app.scenes.launch

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.android.activity.PhyterActivity
import com.jmjproductdev.phyter.android.appcompat.getColorCompat
import com.jmjproductdev.phyter.android.bluetooth.AndroidBLEManager
import com.jmjproductdev.phyter.android.permissions.ActivityPermissionsManager
import com.jmjproductdev.phyter.app.common.android.adapter.PhytersAdapter
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import com.mikepenz.itemanimators.AlphaInAnimator
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import kotlinx.android.synthetic.main.activity_launch.*
import timber.log.Timber


class LaunchActivity : PhyterActivity(), LaunchView {

  override var refresing: Boolean
    get() = swipeLayout.isRefreshing
    set(value) {
      runOnUiThread {
        swipeLayout.isRefreshing = value
        instrumentsAdapter.footerText = if (value) "Scanning..." else "Pull down to refresh."
      }
    }

  private val instrumentsAdapter: PhytersAdapter
    get() = instrumentList.adapter as PhytersAdapter


  private lateinit var permissionsManager: ActivityPermissionsManager
  private lateinit var bleManager: AndroidBLEManager
  private lateinit var presenter: LaunchPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launch)
    setupViews()
    setupPresenter()
    presenter.onCreate(this)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    bleManager.onActivityResult(requestCode, resultCode, data)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_launch, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    item?.let { Timber.i("menu item selected: '$item'") }
    return true
  }

  override fun showSnackbar(msg: String) {
    runOnUiThread {
      Timber.i("showing snackbar: '$msg'")
      Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun dismiss() {
    runOnUiThread {
      Timber.i("finishing")
      finish()
    }
  }

  override fun add(device: Phyter) {
    runOnUiThread {
      Timber.i("adding '${device.name}'")
      instrumentsAdapter.add(device)
    }
  }

  override fun update(device: Phyter) {
    runOnUiThread {
      Timber.i("updating '${device.name}'")
      instrumentsAdapter.refresh(device)
    }
  }

  private fun setupViews() {
    /* toolbar */
    with(toolbar) {
      setSupportActionBar(this)
    }
    /* swipe layout */
    with(swipeLayout) {
      /* progress indicator colors */
      setProgressBackgroundColorSchemeColor(getColorCompat(R.color.colorAccent))
      setColorSchemeColors(getColorCompat(R.color.colorDivider))
      /* listener */
      setOnRefreshListener { presenter.onRefresh() }
    }
    /* instrument recycler view */
    with(instrumentList) {
      layoutManager = LinearLayoutManager(this@LaunchActivity)

      adapter = PhytersAdapter(this@LaunchActivity).apply {
        onItemClickListener = { device, position -> Timber.i("clicked: $device @ $position") }
      }

    }
  }

  private fun setupPresenter() {
    permissionsManager = ActivityPermissionsManager(this)
    bleManager = AndroidBLEManager(this)
    presenter = LaunchPresenter(permissionsManager, bleManager)
  }
}
