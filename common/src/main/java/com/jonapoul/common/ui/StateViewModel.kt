package com.jonapoul.common.ui

import androidx.lifecycle.ViewModel
import com.jonapoul.common.service.ServiceState

class StateViewModel : ViewModel() {
    var activityIsBuilt = false
    var currentState: ServiceState = ServiceState.STOPPED
    var hasBeenCreatedAlready = false
}
