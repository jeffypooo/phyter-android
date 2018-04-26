package com.jmjproductdev.phyter

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import timber.log.Timber

@RunWith(MockitoJUnitRunner::class)
abstract class MockitoTest {

  companion object {
    @BeforeClass
    @JvmStatic
    fun classSetup() {
      Timber.plant(UnitTestTree())
    }

    @AfterClass
    @JvmStatic
    fun classTeardown() {
      Timber.uprootAll()
    }
  }

}