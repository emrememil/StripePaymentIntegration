package com.example.stripepaymentintegration

import android.util.Log
import androidx.annotation.Size
import com.example.stripepaymentintegration.service.StripeApiService
import com.google.gson.Gson
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.IOException

class ExampleEphemeralKeyProvider(val customerID : String) : EphemeralKeyProvider {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val stripeApiService = StripeApiService()


    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {

        val mediaType = "application/json; charset=utf-8".toMediaType()

        var map = HashMap<String,Any>()
        map.put("customer",customerID)
        map.put("apiVersion","2020-08-27")
        var json = Gson().toJson(map)
        val body = json.toRequestBody(mediaType)

        compositeDisposable.add(
            stripeApiService.getEphermalKey(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<ResponseBody>(){
                    override fun onSuccess(t: ResponseBody) {
                        Log.e("EphemeralKey t","-"+t)
                        val ephemeralKeyJson = t.string()
                        Log.e("EphemeralKey json","-"+ephemeralKeyJson)
                        keyUpdateListener.onKeyUpdate(ephemeralKeyJson)
                    }
                    override fun onError(e: Throwable) {
                        keyUpdateListener
                            .onKeyUpdateFailure(0, e.message ?: "")
                        Log.e("EphemeralKey Error","-"+e.toString())
                    }

                }))
        Log.e("Neresi","createEphemeralKey bitti")


    }
}