package com.example.stripepaymentintegration.service

import com.example.stripepaymentintegration.util.Constant
import io.reactivex.Single

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface StripeAPI {

    @POST(Constant.CREATE_PAYMENT_INTENT)
    fun createPaymentIntent(@Body params: RequestBody): Single<ResponseBody>

    @POST(Constant.CREATE_EPHEMERAL_KEY)
    fun createEphemeralKey(@Body params: RequestBody): Single<ResponseBody>
}