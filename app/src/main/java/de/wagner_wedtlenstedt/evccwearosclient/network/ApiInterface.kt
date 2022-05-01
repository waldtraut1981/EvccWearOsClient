package de.wagner_wedtlenstedt.evccwearosclient.network

import de.wagner_wedtlenstedt.evccwearosclient.data.EvccStateModel
import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {
    @GET("state")
    fun fetchEvccState(): Call<EvccStateModel>
}