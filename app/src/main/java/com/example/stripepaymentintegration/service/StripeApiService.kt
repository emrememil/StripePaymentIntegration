package com.example.stripepaymentintegration.service

import com.example.stripepaymentintegration.util.Constant
import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class StripeApiService {

    private val api = Retrofit.Builder()
        .baseUrl(Constant.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(StripeAPI::class.java)

    fun getData(body: RequestBody) : Single<ResponseBody> {
        return api.createPaymentIntent(body)
    }

    fun getEphermalKey(body: RequestBody ) : Single<ResponseBody> {
        return api.createEphemeralKey(body)
    }

}