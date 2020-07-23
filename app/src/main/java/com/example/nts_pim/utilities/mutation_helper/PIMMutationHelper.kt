package com.example.nts_pim.utilities.mutation_helper

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.amazonaws.amplify.generated.graphql.*
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.DeviceID
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import type.*
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.util.*

object PIMMutationHelper {

   private var mAppSyncClient: AWSAppSyncClient? =  null
    private var overHeatedTimeStamp: String? = null

    init {
        mAppSyncClient = ClientFactory.getInstance(PimApplication.pimContext)
    }

    fun updatePIMStatus(vehicleId: String, pimStatusUpdate: String, appSyncClient: AWSAppSyncClient){
        val updatePimStatusInput = UpdateVehTripStatusInput.builder()?.vehicleId(vehicleId)?.pimStatus(pimStatusUpdate)?.build()

        appSyncClient.mutate(UpdateVehTripStatusMutation.builder().parameters(updatePimStatusInput).build()).enqueue(
            mutationCallbackOnPIMStatusUpdate)
       // We are setting the internal PIM Status
        VehicleTripArrayHolder.updateInternalPIMStatus(pimStatusUpdate)
        LoggerHelper.writeToLog("Pim Mutation Helper: sent pim status: $pimStatusUpdate to aws")
    }

    private val mutationCallbackOnPIMStatusUpdate = object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
            Log.i("Results", "PIM Status Updated ${response.data()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the pimStatus: $e")
        }
    }

    fun updateReaderStatus(vehicleId: String, readerStatus: String, appSyncClient: AWSAppSyncClient){
        val formattedObject = getCurrentDateFormattedDateUtcIso()
        val updatePimStatusInput = UpdateVehTripStatusInput.builder()
            ?.vehicleId(vehicleId)
            ?.readerStatus(readerStatus)
            ?.readerStatusTimeStamp(formattedObject)
            ?.build()

        appSyncClient.mutate(UpdateVehTripStatusMutation.builder().parameters(updatePimStatusInput).build())
            ?.enqueue(mutationCallbackOnReaderUpdate)
    }
    private fun getCurrentDateFormattedDateUtcIso(): String? {
        val cal = Calendar.getInstance() ?: return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
         sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
         return sdf.format(cal.getTime())
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

        appSyncClient.mutate(PimPaymentMadeMutation.builder().parameters(updatePaymentTypeInput).build()).enqueue(
            mutationCallbackPaymentType)
        LoggerHelper.writeToLog("Pim Mutation Helper: updated payment status: payment type: $paymentType for trip id: $tripId to aws")

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

        appSyncClient.mutate(UpdateVehTripStatusMutation.builder().parameters(updateStatusInput).build()).enqueue(
            mutationCallbackOnTripStatus)
        LoggerHelper.writeToLog("Pim Mutation Helper: updated trip status: tripStatus: $tripUpdate for trip id: $tripId to aws")
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
        LoggerHelper.writeToLog("Pim Mutation Helper: updatePaymentDetails: TransactionId: $transactionId, tripNumber: $tripNumber, vehicleId: $vehicleId, paymentMethod: $paymentMethod, tripID: $tripId")
        val updatePaymentInput = SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber).vehicleId(vehicleId).paymentMethod(paymentMethod).tripId(tripId).build()

        appSyncClient.mutate(SavePaymentDetailsMutation.builder().parameters(updatePaymentInput).build()).enqueue(
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

    @SuppressLint("MissingPermission")
    fun updatePimSettings(blueToothAddress: String?, appVersion: String?, phoneNumber: String?, appSyncClient: AWSAppSyncClient, deviceId: String){
        val tabletImei: String
        tabletImei = if(deviceId == ""){
            val telephonyManager = PimApplication.instance.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.imei
        } else {
            deviceId
        }
        val updatePimSettings = UpdatePIMSettingsInput
            .builder()
            .deviceId(tabletImei)
            .phoneNbr(phoneNumber)
            .appVersion(appVersion)
            .btAddress(blueToothAddress)
            .build()
        appSyncClient.mutate(UpdatePimSettingsMutation.builder().parameters(updatePimSettings).build())?.enqueue(
            pimSettingsCallback)
        LoggerHelper.writeToLog("Pim Mutation Helper: update pim settings: blueToothAddress: $blueToothAddress: AppVersion: $appVersion phoneNumber: $phoneNumber: deviceId:$deviceId to aws")
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

    fun updateDeviceId(deviceId: String, appSyncClient: AWSAppSyncClient, vehicleId: String){
        val input = UpdateDeviceIdToIMEIInput.builder()
            .deviceId(deviceId)
            .vehicleId(vehicleId)
            .build()
        appSyncClient.mutate(UpdateDeviceIdToImeiMutation.builder().parameters(input).build())?.enqueue(updateDeviceIdToIMEICallback)
    }
    private val updateDeviceIdToIMEICallback =  object : GraphQLCall.Callback<UpdateDeviceIdToImeiMutation.Data>() {
        override fun onResponse(response: Response<UpdateDeviceIdToImeiMutation.Data>) {
            if (!response.hasErrors()) {
                Log.i(
                    "VehicleSetup",
                    "Updated device Id from blank to ${response.data().toString()}"
                )
            } else {
                Log.i(
                    "VehicleSetup",
                    "Response for updating for device ID has errors. Error: ${response.errors()[0].message()}"
                )
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.i("VehicleSetup", "Error: $e")
        }
    }
    fun sendPIMStartTime(deviceId: String, appSyncClient: AWSAppSyncClient){
        val currentTime = getCurrentDateFormattedDateUtcIso()
        val input = UpdatePIMSettingsInput.builder().deviceId(deviceId).pimStartedTimeStamp(currentTime).build()
        appSyncClient.mutate(UpdatePimSettingsMutation.builder().parameters(input).build())?.enqueue(
            updatePIMStartTimeCallBack)
    }

    private val updatePIMStartTimeCallBack = object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>(){
        override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
            Log.i("PIMStartTime", "PIM Start up response: ${response.data()}")
           if(!response.hasErrors()){
               Log.i("PIMStartTime", "PIM Start up response successful")
           }
        }

        override fun onFailure(e: ApolloException) {
           Log.i("PIMStartTime", "error $e")
        }
    }

    fun sendPIMOverheatTime(){
        val currentTime = getCurrentDateFormattedDateUtcIso()
        val deviceId = ModelPreferences(PimApplication.pimContext).getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)?.number
        if(deviceId != null){
            val input = UpdatePIMSettingsInput.builder().deviceId(deviceId).overheatedTimeStamp(currentTime).build()
            mAppSyncClient?.mutate(UpdatePimSettingsMutation.builder().parameters(input).build())?.enqueue(
                updatePIMOverheatTime)
        }
    }

    private val updatePIMOverheatTime = object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>(){
        override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
            Log.i("PIMOverheat", "PIM over heat: ${response.data()}")
            if(!response.hasErrors()){
                Log.i("PIMOverheat", "PIM overheat response successful")
                overHeatedTimeStamp = response.data()?.updatePIMSettings()?.overheatedTimeStamp()
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.i("PimOverHeat", "error $e")
        }
    }
}