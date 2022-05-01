package de.wagner_wedtlenstedt.evccwearosclient.viewmodel

import androidx.lifecycle.*
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccRepository
import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EvccViewModel: ViewModel(){

    private var evccRepository: EvccRepository?=null
    private var evccModelLiveData : LiveData<EvccStateModel>?=null

    init {
        evccRepository = EvccRepository()
        evccModelLiveData = MutableLiveData()
    }

    fun getEvccLiveData() = evccRepository?.getEvccLiveData()

    fun startEvccStateUpdateRoutine(){
        viewModelScope.launch {
            while(true){
                evccRepository?.fetchEvccState()
                delay(5000)
            }
        }
    }
}