package com.notjuststudio.bashim.helper

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.R

class BillingHelper(val context: Context) {

    companion object {
        private const val ORDER_ID = "pie_donate"
    }

    private val billingClient: BillingClient


    init {
        billingClient = BillingClient.newBuilder(context).setListener(BillingListener()).build()
    }

    fun purchasePie(activity: Activity, onDone: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    //billing
                    purchaseDialog(activity, onDone)
                }
            }

            override fun onBillingServiceDisconnected() {
//                billingClient.startConnection(this)
            }

        })
    }

    private fun purchaseDialog(activity: Activity, onDone: () -> Unit) {
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(SkuDetails("""{
                        "productId": "${ORDER_ID}",
                        "type": "${BillingClient.SkuType.INAPP}"
                    }"""))
                .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        if (responseCode != BillingClient.BillingResponse.OK) {
            Log.i("Billing", "Billing error: responseCode = $responseCode")
        } else {
            Log.i("Billing", "Billing success?: responseCode = $responseCode")
        }
        onDone()
    }

    private inner class  BillingListener : PurchasesUpdatedListener {

        override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
            if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                for (purchase in purchases) {
                    Log.i("Billing", "Success! Id: ${purchase.orderId}")
                    billingClient.consumeAsync(purchase.purchaseToken) {
                       _,_ ->
                       Log.i("Billing", "Consumed")
                    }
                }
            } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else if (responseCode == BillingClient.BillingResponse.ITEM_ALREADY_OWNED) {
                    billingClient.consumeAsync(purchases?.find { it.sku == ORDER_ID }?.purchaseToken) {
                        _,_ ->
                        Log.i("Billing", "Consumed")
                    }
            } else {
                // Handle any other error codes.
            }

        }

    }

}