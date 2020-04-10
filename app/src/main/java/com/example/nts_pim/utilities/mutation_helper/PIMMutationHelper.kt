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
        val updatePimStatusInput = UpdateVehTripStatusInput.builder()?.vehicleId(vehicleId)?.pimStatus(pimStatusUpdate)?.build()

        appSyncClient.mutate(UpdateVehTripStatusMutation.builder().parameters(updatePimStatusInput).build())
            ?.enqueue(mutationCallbackOnPIM)
       // We are setting the internal PIM Status
        VehicleTripArrayHolder.updateInternalPIMStatus(pimStatusUpdate)
    }

    private val mutationCallbackOnPIM = object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
            Log.i("Results", "PIM Status Updated ${response.data()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the pimStatus: $e")
        }
    }

    fun updateReaderStatus(vehicleId: String, readerStatus: String, appSyncClient: AWSAppSyncClient){
        val updatePimStatusInput = UpdateVehTripStatusInput.builder()?.vehicleId(vehicleId)?.readerStatus(readerStatus)?.build()

        appSyncClient.mutate(UpdateVehTripStatusMutation.builder().parameters(updatePimStatusInput).build())
            ?.enqueue(mutationCallbackOnReaderUpdate)
    }

    private val mutationCallbackOnReaderUpdate = object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
            Log.i("Results", "ReaderStatus Updated ${response.data()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the pimStatus: $e")
        }
    }

    fun updatePaymentType(vehicleId: String, paymentType: String, appSyncClient: AWSAppSyncClient, tripId: String){
        val updatePaymentTypeInput = PimPaymentMadeInput.builder().vehicleId(vehicleId).tripId(tripId).paymentType(paymentType).build()

        appSyncClient.mutate(PimPaymentMadeMutation.builder().parameters(updatePaymentTypeInput).build())
            ?.enqueue(mutationCallbackPaymentType)

    }

    private val mutationCallbackPaymentType = object : GraphQLCall.Callback<PimPaymentMadeMutation.Data>() {
        override fun onResponse(response: Response<PimPaymentMadeMutation.Data>) {
            Log.i("Results", "Meter Table Updated ${response.data()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the MeterTable: $e")
        }
    }

    fun updateTripStatus(vehicleId: String, tripUpdate: String, appSyncClient: AWSAppSyncClient, tripId: String){
        val updateStatusInput = UpdateVehTripStatusInput.builder()?.vehicleId(vehicleId)?.tripStatus(tripUpdate)?.tripId(tripId)?.build()

        appSyncClient.mutate(UpdateVehTripStatusMutation.builder().parameters(updateStatusInput).build())
            ?.enqueue(mutationCallbackOnTripStatus)
    }

    private val mutationCallbackOnTripStatus = object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
            Log.i("Results", "vehicle Status Updated to ${response.data()}")

        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the vehicle Status: $e")
        }
    }

    fun updatePaymentDetails(transactionId: String, tripNumber: Int, vehicleId: String, appSyncClient: AWSAppSyncClient, paymentMethod: String, tripId: String){
        Log.i("Payment AWS", "Trying to send the following to Payment AWS. TransactionId: $transactionId, tripNumber: $tripNumber, vehicleId: $vehicleId, paymentMethod: $paymentMethod, tripID: $tripId")
        val updatePaymentInput = SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber).vehicleId(vehicleId).paymentMethod(paymentMethod).tripId(tripId).build()

        appSyncClient.mutate(SavePaymentDetailsMutation.builder().parameters(updatePaymentInput).build())?.enqueue(
            mutationCallbackPaymentDetails)
    }
    private val mutationCallbackPaymentDetails = object : GraphQLCall.Callback<SavePaymentDetailsMutation.Data>() {
        override fun onResponse(response: Response<SavePaymentDetailsMutation.Data>) {
            Log.i("Payment AWS", "payment details have been updated to ${response.data()?.savePaymentDetails().toString()}")

        }

        override fun onFailure(e: ApolloException) {
            Log.e("Payment AWS", "There was an issue updating payment api: $e")
        }
    }

    fun updatePimSettings(blueToothAddress: String?, appVersion: String?, appSyncClient: AWSAppSyncClient, deviceId: String){
        val updatePimSettings = UpdatePIMSettingsInput
            .builder()
            .deviceId(deviceId)
            .appVersion(appVersion)
            .btAddress(blueToothAddress)
            .build()
        appSyncClient.mutate(UpdatePimSettingsMutation.builder().parameters(updatePimSettings).build()).enqueue(pimSettingsCallback)
    }
    private val pimSettingsCallback = object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>() {
        override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
            if (response.hasErrors()){
                Log.e("PIM Settings", "There was an issue updating payment settings: ${response.errors()[0]}")
            }

            Log.i("Response", "response: ${response.data()?.updatePIMSettings().toString()}")
        }
        override fun onFailure(e: ApolloException) {
            Log.i("Response", "response: $e")
        }
    }

    fun updateTabletPhoneNumber(phoneNumber: String, appSyncClient: AWSAppSyncClient, vehicleId: String){

    }

}