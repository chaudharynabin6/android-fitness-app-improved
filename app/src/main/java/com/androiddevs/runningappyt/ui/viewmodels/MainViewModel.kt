package com.androiddevs.runningappyt.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.androiddevs.runningappyt.repositories.MainRepository
import javax.inject.Inject

class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {
}