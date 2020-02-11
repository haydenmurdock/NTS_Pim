package com.example.nts_pim.data.repository

import android.content.Context
import android.util.Log
import com.amazonaws.amplify.generated.graphql.OnTripUpdateSubscription
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.utilities.logging_service.LoggerHelper

object  SubscriptionWatcher {
    // This is where we will update/check/cancel tripSubscriptions
    private var subscriptionWatcherTrip: AppSyncSubscriptionCall<OnTripUpdateSubscription.Data>? =
        null
    var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var mTripCallBack:AppSyncSubscriptionCall.Callback<OnTripUpdateSubscription.Data>? = null

    internal fun updateSubscriptionWatcher(tripId: String, context: Context, tripCallBack: AppSyncSubscriptionCall.Callback<OnTripUpdateSubscription.Data>?): AppSyncSubscriptionCall<OnTripUpdateSubscription.Data>?{
        if (tripCallBack != null){
            Log.i("LOGGER", "callback is now set")
            mTripCallBack = tripCallBack
        }
       val subscriptionBuilder = OnTripUpdateSubscription.builder().tripId(tripId).build()
        if(mAWSAppSyncClient == null){
            mAWSAppSyncClient = ClientFactory.getInstance(context)
        }
        if(subscriptionWatcherTrip == null){
            Log.i("LOGGER", "watcher was null, Updated to subscription builder")
            subscriptionWatcherTrip = mAWSAppSyncClient?.subscribe(subscriptionBuilder)
            if (mTripCallBack != null){
                subscriptionWatcherTrip?.execute(mTripCallBack!!)
            }

        } else {
            Log.i("LOGGER", "watcher cancelled, and updated")
            subscriptionWatcherTrip!!.cancel()
            subscriptionWatcherTrip = mAWSAppSyncClient?.subscribe(subscriptionBuilder)
            if (mTripCallBack != null){
                subscriptionWatcherTrip?.execute(mTripCallBack!!)
            }
        }
        return subscriptionWatcherTrip
    }
}