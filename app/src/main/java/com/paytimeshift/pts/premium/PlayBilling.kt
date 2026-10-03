package com.paytimeshift.pts.premium

import android.app.Activity
import com.android.billingclient.api.*

/** Only server verification grants access. Pending and suspended purchases never unlock Premium. */
class PlayBilling(activity: Activity,private val uid: ()->String?,private val verify: (String)->Unit,private val message: (String)->Unit,private val price: (String?)->Unit) {
    private val host=activity
    private var product: ProductDetails?=null
    private var offer: ProductDetails.SubscriptionOfferDetails?=null
    private val client=BillingClient.newBuilder(activity).setListener {result,purchases->
        when(result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach {p->
                if("pts_premium" in p.products && p.purchaseState==Purchase.PurchaseState.PURCHASED && !p.isSuspended) verify(p.purchaseToken)
                else if(p.purchaseState==Purchase.PurchaseState.PENDING) message("Payment is pending. Premium activates after payment is confirmed.")
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            else -> message("Google Play purchase failed. Please try again.")
        }
    }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).enableAutoServiceReconnection().build()
    fun start() {client.startConnection(object: BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {if(result.responseCode==BillingClient.BillingResponseCode.OK) {loadProduct();restore()} else price(null)}
        override fun onBillingServiceDisconnected() {price(null)}
    })}
    private fun loadProduct() {
        val params=QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId("pts_premium").setProductType(BillingClient.ProductType.SUBS).build())).build()
        client.queryProductDetailsAsync(params) {result,response->
            if(result.responseCode==BillingClient.BillingResponseCode.OK) {
                product=response.productDetailsList.firstOrNull {it.productId=="pts_premium"}
                offer=product?.subscriptionOfferDetails?.firstOrNull {it.basePlanId=="annual" && it.offerId==null}
                price(offer?.pricingPhases?.pricingPhaseList?.lastOrNull {it.billingPeriod=="P1Y"}?.formattedPrice)
            } else price(null)
        }
    }
    fun buy() {
        val user=uid();val p=product;val o=offer
        if(user==null) {message("Sign in before subscribing.");return}
        if(p==null || o==null || !client.isReady) {message("Subscription is not available in Google Play yet. Please try again later.");return}
        val params=BillingFlowParams.newBuilder().setObfuscatedAccountId(accountHash(user)).setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(p).setOfferToken(o.offerToken).build())).build()
        val result=client.launchBillingFlow(host,params)
        if(result.responseCode==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) restore()
        else if(result.responseCode!=BillingClient.BillingResponseCode.OK) message("Google Play purchase failed. Please try again.")
    }
    fun restore() {if(uid()==null) return
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) {result,purchases->
            if(result.responseCode==BillingClient.BillingResponseCode.OK) purchases.filter {"pts_premium" in it.products && it.purchaseState==Purchase.PurchaseState.PURCHASED && !it.isSuspended}.forEach {verify(it.purchaseToken)}
        }
    }
    fun close() {client.endConnection()}
}
