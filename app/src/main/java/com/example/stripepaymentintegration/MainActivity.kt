package com.example.stripepaymentintegration

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.example.stripepaymentintegration.service.StripeApiService
import com.example.stripepaymentintegration.util.Constant
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.stripe.android.*
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.CustomerSource
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.CardInputWidget
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var paymentIntentClientSecret: String
    private lateinit var stripe: Stripe

    private val stripeApiService = StripeApiService()
    private val disposable = CompositeDisposable()

    private var paymentSession: PaymentSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stripe = Stripe(applicationContext, Constant.PK)
        //startCheckout()
        PaymentConfiguration.init(applicationContext, Constant.PK)
        CustomerSession.initCustomerSession(this, ExampleEphemeralKeyProvider("cus_IL9S1cQ3jNctfD"))
        paymentSession = PaymentSession(this, paymentSessionConfig())
        paymentSession?.init(createPaymentSessionListener())





        getPaymentIntentCS()

        val payButton: Button = findViewById(R.id.payButton)
        payButton.setOnClickListener {
            val cardInputWidget =
                findViewById<CardInputWidget>(R.id.cardInputWidget)
            cardInputWidget.paymentMethodCreateParams?.let { params ->
                val confirmParams = ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(params, paymentIntentClientSecret)
                stripe.confirmPayment(this, confirmParams)
            }
        }
    }

    private fun paymentSessionConfig(): PaymentSessionConfig {
        return PaymentSessionConfig.Builder()
            .setShippingInfoRequired(false)
            .setShippingMethodsRequired(false)
            .setPaymentMethodTypes(
                listOf(PaymentMethod.Type.Card)
            )
            .setShouldShowGooglePay(true)
            .build()
    }

    private fun createPaymentSessionListener(): PaymentSession.PaymentSessionListener {
        return object : PaymentSession.PaymentSessionListener {
            override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                if (isCommunicating) {
                    // update UI to indicate that network communication is in progress
                } else {
                    // update UI to indicate that network communication has completed
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {}

            // Called whenever the PaymentSession's data changes,
            // e.g. when the user selects a new `PaymentMethod` or enters shipping info.
            override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                if (data.useGooglePay) {
                    // customer intends to pay with Google Pay
                } else {
                    data.paymentMethod?.let { paymentMethod ->
                        // Display information about the selected payment method
                    }
                }

                // Update your UI here with other data
                if (data.isPaymentReadyToCharge) {
                    // Use the data to complete your charge - see below.
                }
            }
        }
    }

    private fun getPaymentIntentCS(){
        val mediaType = "application/json; charset=utf-8".toMediaType()

        var payMap = HashMap<String,Any>()
        payMap.put("currency","usd")
        var itemMap  = HashMap<String,Any>()
        itemMap.put("id","photo_subscription")
        itemMap.put("amount","25000")
        var itemList = ArrayList<Map<String,Any>>()
        itemList.add(itemMap)
        payMap.put("items",itemList)
        var json = Gson().toJson(payMap)
        val body = json.toRequestBody(mediaType)


        disposable.add(
            stripeApiService.getData(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<ResponseBody>(){
                    override fun onSuccess(t: ResponseBody) {
                        val responseData = t
                        val responseJson =
                            responseData?.let {
                                JSONObject(it.string())
                            } ?: JSONObject()

                        paymentIntentClientSecret = responseJson.getString("clientSecret")
                    }

                    override fun onError(e: Throwable) {
                        displayAlert(this@MainActivity, "Failed to load page2", "Error: $e")
                    }
                })
        )
    }

    private fun displayAlert(
        activity: Activity,
        title: String,
        message: String,
        restartDemo: Boolean = false
    ) {
        runOnUiThread {
            val builder = AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
            builder.setPositiveButton("Ok", null)
            builder.create().show()
        }
    }

    fun pay(clientSecret: String) {
        stripe = Stripe(this, PaymentConfiguration.getInstance(this).publishableKey)
        stripe?.confirmPayment(
            this,
            ConfirmPaymentIntentParams.create(clientSecret)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        paymentSession?.handlePaymentData(requestCode, resultCode, data ?: Intent())
        val weakActivity = WeakReference<Activity>(this)
        // Handle the result of stripe.confirmPayment
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                val paymentIntent = result.intent
                val status = paymentIntent.status
                if (status == StripeIntent.Status.Succeeded) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    weakActivity.get()?.let { activity ->
                        displayAlert(
                            activity,
                            "Payment succeeded",
                            gson.toJson(paymentIntent)
                        )
                    }
                } else if (status == StripeIntent.Status.RequiresPaymentMethod) {
                    weakActivity.get()?.let { activity ->
                        displayAlert(
                            activity,
                            "Payment failed",
                            paymentIntent.lastPaymentError?.message.orEmpty()
                        )
                    }
                }
            }
            override fun onError(e: Exception) {
                weakActivity.get()?.let { activity ->
                    displayAlert(
                        activity,
                        "Payment failed2",
                        e.toString()
                    )
                }
            }
        })
    }
}