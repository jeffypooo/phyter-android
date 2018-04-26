package com.jmjproductdev.phyter.app.common.presentation

interface PhyterView {
  fun showSnackbar(msg: String)
  fun dismiss()
}

abstract class PhyterPresenter<T : PhyterView> {

  protected var view: T? = null

  open fun onCreate(view: T) {
    this.view = view
  }

  open fun onStart() {

  }

  open fun onResume() {

  }

  open fun onPause() {

  }

  open fun onStop() {

  }

  open fun onDestroy() {
    this.view = null
  }

}