package com.jmjproductdev.phyter.util

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun Disposable.disposedBy(composite: CompositeDisposable) {
  composite.add(this)
}