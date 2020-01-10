package com.example.nts_pim.utilities.mutation_helper

import android.util.Log
import com.amazonaws.amplify.generated.graphql.*
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import type.*

object PIMMutationHelper {

    fun updatePIMStatus(vehicleId: String, pimStatusUpdate: String, appSyncClient: AWSAppSyncClient){
        val updatePimStatusInput = UpdatePimStatusInput.builder()?.vehicleId(vehicleId)?.pimStatus(pimStatusUpdate)?.build()

        appSyncClient.mutate(UpdatePimStatusMutation.builder().parameters(updatePimStatusInput).build())
            ?.enqueue(mutationCallbackOnPIM)
       // We are setting the internal PIM Status
        VehicleTripArrayHolder.updateInternalPIMStatus(pimStatusUpdate)
    }

    private val mutationCallbackOnPIM = object : GraphQLCall.Callback<UpdatePimStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdatePimStatusMutation.Data>) {
            Log.i("Results", "PIM Status Updated ${response.data()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the pimStatus: $e")
        }
    }

    fun updatePaymentType(vehicleId: String, paymentType: String, appSyncClient: AWSAppSyncClient, tripId: String){
        val updatePaymentTypeInput = UpdateTripInput.builder().vehicleId(vehicleId).tripId(tripId).paymentType(paymentType).build()

        appSyncClient.mutate(UpdateTripMutation.builder().parameters(updatePaymentTypeInput).build())
            ?.enqueue(mutationCallbackPaymentType)

    }

    private val mutationCallbackPaymentType = object : GraphQLCall.Callback<UpdateTripMutation.Data>() {
        override fun onResponse(response: Response<UpdateTripMutation.Data>) {
            Log.i("Results", "Meter Table Updated ${response.data()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the MeterTable: $e")
        }
    }

    fun updateTripStatus(vehicleId: String, tripUpdate: String, appSyncClient: AWSAppSyncClient, tripId: String){
        val updateStatusInput = UpdateTripStatusInput.builder()?.vehicleId(vehicleId)?.tripStatus(tripUpdate)?.tripId(tripId)?.build()

        appSyncClient.mutate(UpdateTripStatusMutation.builder().parameters(updateStatusInput).build())
            ?.enqueue(mutationCallbackOnTripStatus)
    }

    private val mutationCallbackOnTripStatus = object : GraphQLCall.Callback<UpdateTripStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdateTripStatusMutation.Data>) {
            Log.i("Results", "vehicle Status Updated to ${response.data()}")

        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the vehicle Status: $e")
        }
    }

    fun updatePaymentDetails(transactionId: String, tripNumber: Int, vehicleId: String, appSyncClient: AWSAppSyncClient, paymentMethod: String, tripId: String){
        val updatePaymentInput = SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber).vehicleId(vehicleId).paymentMethod(paymentMethod).tripId(tripId).build()

        appSyncClient.mutate(SavePaymentDetailsMutation.builder().parameters(updatePaymentInput).build())?.enqueue(
            mutationCallbackPaymentDetails)
    }
    private val mutationCallbackPaymentDetails = object : GraphQLCall.Callback<SavePaymentDetailsMutation.Data>() {
        override fun onResponse(response: Response<SavePaymentDetailsMutation.Data>) {
            Log.i("Payment AWS", "payment details have been updated to ${response.data()}")

        }

        override fun onFailure(e: ApolloException) {
            Log.e("Payment AWS", "There was an issue updating payment api: $e")
        }
    }


}