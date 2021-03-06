package com.jon.common.repositories.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jon.common.repositories.IStatusRepository
import com.jon.common.service.ServiceState
import javax.inject.Inject

class StatusRepository @Inject constructor() : IStatusRepository {
    private val lock = Any()
    private val currentStatus = MutableLiveData<ServiceState>().apply { value = ServiceState.STOPPED }

    override fun getStatus(): LiveData<ServiceState> {
        synchronized(lock) {
            return currentStatus
        }
    }

    override fun postStatus(state: ServiceState) {
        synchronized(lock) {
            currentStatus.postValue(state)
        }
    }
}
