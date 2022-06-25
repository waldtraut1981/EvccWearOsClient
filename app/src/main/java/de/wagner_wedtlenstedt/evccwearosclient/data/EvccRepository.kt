package de.wagner_wedtlenstedt.evccwearosclient.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.wagner_wedtlenstedt.evccwearosclient.network.ApiClient
import de.wagner_wedtlenstedt.evccwearosclient.network.ApiInterface
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EvccRepository {

    private var apiInterface: ApiInterface?=null
    private val data = MutableLiveData<EvccStateModel>()

    init {
        apiInterface = ApiClient.getApiClient().create(ApiInterface::class.java)
    }

    fun fetchEvccState() {
        apiInterface?.fetchEvccState()?.enqueue(object : Callback<EvccStateModel> {

            override fun onFailure(call: Call<EvccStateModel>, t: Throwable) {
                data.value = null
            }

            override fun onResponse(
                call: Call<EvccStateModel>,
                response: Response<EvccStateModel>
            ) {
                val res = response.body()
                if (response.code() == 200 && res!=null){
                    data.value = res
                }else{
                    data.value = null
                }
            }
        })
    }

    fun getEvccLiveData() = data
}