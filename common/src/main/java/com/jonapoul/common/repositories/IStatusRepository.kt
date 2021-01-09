package com.jonapoul.common.repositories

import androidx.lifecycle.LiveData
import com.jonapoul.common.service.ServiceState

interface IStatusRepository {
    fun getStatus(): LiveData<ServiceState>
    fun postStatus(state: ServiceState)
}
