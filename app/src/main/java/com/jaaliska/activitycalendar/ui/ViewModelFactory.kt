package com.jaaliska.activitycalendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Builds a factory that creates a ViewModel with dependencies taken from the app container. */
inline fun <reified VM : ViewModel> viewModelFactoryOf(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = viewModelFactory { initializer { create() } }
