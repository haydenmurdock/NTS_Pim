package com.example.nts_pim.utilities.mutation_helper

import android.annotation.SuppressLint
import android.util.Log
import com.amazonaws.amplify.generated.graphql.*
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.AdInfoHolder
import com.example.nts_pim.data.repository.SetupHolder
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.DeviceID
import com.example.nts_pim.data.repository.model_objects.PimError
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import type.*
import java.text.SimpleDateFormat
import java.util.*

object PIMMutationHelper {

    private var mAppSyncClient: AWSAppSyncClient? = null
    private var overHeatedTimeStamp: String? = null
    var stopPimSetup = false
    var pimError: PimError? = null

    init {
        mAppSyncClient = ClientFactory.getInstance(PimApplication.pimContext)
    }

    fun updatePIMStatus(
        vehicleId: String,
        pimStatusUpdate: String,
        appSyncClient: AWSAppSyncClient
    ) {
        val updatePimStatusInput =
            UpdateVehTripStatusInput.builder()?.vehicleId(vehicleId)?.pimStatus(pimStatusUpdate)
                ?.build()

        appSyncClient.mutate(
            UpdateVehTripStatusMutation.builder().parameters(updatePimStatusInput).build()
        ).enqueue(
            mutationCallbackOnPIMStatusUpdate
        )
        // We are setting the internal PIM Status
        VehicleTripArrayHolder.updateInternalPIMStatus(pimStatusUpdate)
        LoggerHelper.writeToLog("Pim Mutation Helper: sent pim status: $pimStatusUpdate to aws", LogEnums.TRIP_STATUS.tag)
    }

    private val mutationCallbackOnPIMStatusUpdate =
        object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
            override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
                Log.i("Results", "PIM Status Updated ${response.data()}")

            }

            override fun onFailure(e: ApolloException) {
                Log.e("Error", "There was an issue updating the pimStatus: $e")

            }
        }

    fun updateReaderStatus(
        vehicleId: String,
        readerStatus: String,
        appSyncClient: AWSAppSyncClient
    ) {

        val formattedObject = getCurrentDateFormattedDateUtcIso()
        val updatePimStatusInput = UpdateVehTripStatusInput.builder()
            ?.vehicleId(vehicleId)
            ?.readerStatus(readerStatus)
            ?.readerStatusTimeStamp(formattedObject)
            ?.build()

        appSyncClient.mutate(
            UpdateVehTripStatusMutation.builder().parameters(updatePimStatusInput).build()
        )
            ?.enqueue(mutationCallbackOnReaderUpdate)
    }


    private val mutationCallbackOnReaderUpdate =
        object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
            override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
                Log.i("Results", "ReaderStatus Updated ${response.data()}")
                if(response.data()?.updateVehTripStatus()?.error() == null){
                    SetupHolder.updatedAWSWithReaderStatus()
                }
                if(response.data()?.updateVehTripStatus()?.error() != null){
                    SetupHolder.didNotUpdateAWSWithReaderStatus(response.data()?.updateVehTripStatus()?.error())
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("Error", "There was an issue updating the pimStatus: $e")
                SetupHolder.didNotUpdateAWSWithReaderStatus("Issue updating reader status. onFailure ${e.message}")
            }
        }

    fun updatePaymentType(
        vehicleId: String,
        paymentType: String,
        appSyncClient: AWSAppSyncClient,
        tripId: String
    ) {
        val updatePaymentTypeInput =
            PimPaymentMadeInput.builder().vehicleId(vehicleId).tripId(tripId)
                .paymentType(paymentType).build()

        appSyncClient.mutate(
            PimPaymentMadeMutation.builder().parameters(updatePaymentTypeInput).build()
        ).enqueue(
            mutationCallbackPaymentType
        )
        LoggerHelper.writeToLog("Pim Mutation Helper: updated payment status: payment type: $paymentType for trip id: $tripId to aws", LogEnums.BLUETOOTH.tag)

    }

    private val mutationCallbackPaymentType =
        object : GraphQLCall.Callback<PimPaymentMadeMutation.Data>() {
            override fun onResponse(response: Response<PimPaymentMadeMutation.Data>) {
                Log.i("Results", "Meter Table Updated ${response.data()}")
            }

            override fun onFailure(e: ApolloException) {
                Log.e("Error", "There was an issue updating the MeterTable: $e")
            }
        }

    fun updateTripStatus(
        vehicleId: String,
        tripUpdate: String,
        appSyncClient: AWSAppSyncClient,
        tripId: String
    ) {
        val updateStatusInput =
            UpdateVehTripStatusInput.builder()?.vehicleId(vehicleId)?.tripStatus(tripUpdate)
                ?.tripId(tripId)?.build()

        appSyncClient.mutate(
            UpdateVehTripStatusMutation.builder().parameters(updateStatusInput).build()
        ).enqueue(
            mutationCallbackOnTripStatus
        )
        LoggerHelper.writeToLog("Pim Mutation Helper: updated trip status: tripStatus: $tripUpdate for trip id: $tripId to aws", LogEnums.BLUETOOTH.tag)
    }

    private val mutationCallbackOnTripStatus =
        object : GraphQLCall.Callback<UpdateVehTripStatusMutation.Data>() {
            override fun onResponse(response: Response<UpdateVehTripStatusMutation.Data>) {
                Log.i("Results", "vehicle Status Updated to ${response.data()}")

            }

            override fun onFailure(e: ApolloException) {
                Log.e("Error", "There was an issue updating the vehicle Status: $e")
            }
        }

    fun pimPaymentSquareMutation(vehicleId: String, tripId: String, tipAmt: Double, cardInfo: String, tipPercent: Double, paidAmount: Double, transactionDate: String, transactionId: String){
    LoggerHelper.writeToLog("pim payment builder structure. VehicleId: $vehicleId, TripId: $tripId, tipAmt: $tipAmt, cardInfo: $cardInfo, tipPercent: $tipPercent, pimPaidAmt: $paidAmount, PimTransactionDate: $transactionDate, pimTransId: $transactionId, paymentType: card", LogEnums.PAYMENT.tag)
    val pimPaymentInput = PimPaymentMadeInput.builder()
        .vehicleId(vehicleId)
        .tripId(tripId)
        .tipAmt(tipAmt)
        .cardInfo(cardInfo)
        .tipPercent(tipPercent)
        .pimPaidAmt(paidAmount)
        .pimTransDate(transactionDate)
        .pimTransId(transactionId)
        .paymentType("card")
        .build()

    mAppSyncClient?.mutate(PimPaymentMadeMutation.builder().parameters(pimPaymentInput).build())
    ?.enqueue(pimPaymentMadeCallback)
}
    private val pimPaymentMadeCallback = object : GraphQLCall.Callback<PimPaymentMadeMutation.Data>() {
        override fun onResponse(response: Response<PimPaymentMadeMutation.Data>) {
            LoggerHelper.writeToLog("pim Payment Made - Response data: ${response.data()}", LogEnums.PAYMENT.tag)
            if(!response.hasErrors()){
                LoggerHelper.writeToLog("Pim Payment Made: No errors in response: response package: ${response.data()}", LogEnums.PAYMENT.tag)
            }
            if(response.hasErrors()){
                LoggerHelper.writeToLog("Pim Payment Made: There was an error in the response. ${response.errors()}}", LogEnums.PAYMENT.tag)
            }
        }

        override fun onFailure(e: ApolloException) {
            LoggerHelper.writeToLog("pimPaymentMade error. $e", LogEnums.PAYMENT.tag)
        }
    }

    fun updatePaymentDetails(
        transactionId: String,
        tripNumber: Int,
        vehicleId: String,
        appSyncClient: AWSAppSyncClient,
        paymentMethod: String,
        tripId: String
    ) {
        LoggerHelper.writeToLog("Pim Mutation Helper: updatePaymentDetails: TransactionId: $transactionId, tripNumber: $tripNumber, vehicleId: $vehicleId, paymentMethod: $paymentMethod, tripID: $tripId", LogEnums.BLUETOOTH.tag)
        val updatePaymentInput = if(paymentMethod == "CASH" || paymentMethod == "cash"){
            SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber)
                .vehicleId(vehicleId).paymentMethod(paymentMethod).paymentSource(null).tripId(tripId).build()
        } else {
            SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber)
                .vehicleId(vehicleId).paymentMethod(paymentMethod).paymentSource("PIM").tripId(tripId).build()
        }
        appSyncClient.mutate(
            SavePaymentDetailsMutation.builder().parameters(updatePaymentInput).build()
        ).enqueue(
            mutationCallbackPaymentDetails
        )
    }

    private val mutationCallbackPaymentDetails =
        object : GraphQLCall.Callback<SavePaymentDetailsMutation.Data>() {
            override fun onResponse(response: Response<SavePaymentDetailsMutation.Data>) {
                Log.i(
                    "Payment AWS",
                    "payment details have been updated to ${response.data()?.savePaymentDetails()
                        .toString()}"
                )

            }

            override fun onFailure(e: ApolloException) {
                Log.e("Payment AWS", "There was an issue updating payment api: $e")
            }
        }

    @SuppressLint("MissingPermission")
   fun updatePimSettings(
        blueToothAddress: String?,
        appVersion: String?,
        phoneNumber: String?,
        appSyncClient: AWSAppSyncClient,
        deviceId: String
    ) {
        val newDeviceId = DeviceIdCheck.getDeviceId() ?: ""
        if (newDeviceId != "") {
            val updatePimSettings = UpdatePIMSettingsInput
                .builder()
                .deviceId(newDeviceId)
                .phoneNbr(phoneNumber)
                .appVersion(appVersion)
                .btAddress(blueToothAddress)
                .build()
            appSyncClient.mutate(
                UpdatePimSettingsMutation.builder().parameters(updatePimSettings).build()
            )?.enqueue(pimSettingsCallback)
            LoggerHelper.writeToLog("Pim Mutation Helper: update pim settings: blueToothAddress: $blueToothAddress: AppVersion: $appVersion phoneNumber: $phoneNumber: deviceId:$deviceId to aws", LogEnums.TRIP_STATUS.tag)
        }
    }

    private val pimSettingsCallback =
        object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>(){
            override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
                if (response.data()?.updatePIMSettings()?.error() != null) {
                    when(response.data()?.updatePIMSettings()?.errorCode()){
                        "1016" -> {
                            val errorMessage = response.data()?.updatePIMSettings()!!.error()
                            stopPimSetup = true
                            pimError = PimError(errorMessage)
                            LoggerHelper.writeToLog("Failed to update pim settings with phone number. Error: $errorMessage", LogEnums.TRIP_STATUS.tag)
                        }
                    }
                }
                LoggerHelper.writeToLog("response: ${response.data()?.updatePIMSettings().toString()}", LogEnums.TRIP_STATUS.tag)
            }

            override fun onFailure(e: ApolloException) {
                Log.i("Response", "response: $e")
            }
        }



    fun updateDeviceId(deviceId: String, appSyncClient: AWSAppSyncClient, vehicleId: String) {
        val input = UpdateDeviceIdPIMInput.builder()
            .deviceId(deviceId)
            .vehicleId(vehicleId)
            .build()
        appSyncClient.mutate(UpdateDeviceIdPimMutation.builder().parameters(input).build())
            ?.enqueue(updateDeviceIdToIMEICallback)
    }


    private val updateDeviceIdToIMEICallback =
        object : GraphQLCall.Callback<UpdateDeviceIdPimMutation.Data>() {
            override fun onResponse(response: Response<UpdateDeviceIdPimMutation.Data>) {
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

    fun sendPIMStartTime(deviceId: String, appSyncClient: AWSAppSyncClient) {
        val currentTime = getCurrentDateFormattedDateUtcIso()
        val input =
            UpdatePIMSettingsInput.builder().deviceId(deviceId).pimStartedTimeStamp(currentTime)
                .build()
        appSyncClient.mutate(UpdatePimSettingsMutation.builder().parameters(input).build())
            ?.enqueue(
                updatePIMStartTimeCallBack
            )
    }

    private val updatePIMStartTimeCallBack =
        object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>() {
            override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
                Log.i("PIMStartTime", "PIM Start up response: ${response.data()}")
                if (!response.hasErrors()) {
                    Log.i("PIMStartTime", "PIM Start up response successful")
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.i("PIMStartTime", "error $e")
            }
        }

    fun sendPIMOverheatTime(currentTime: String) {
        val deviceId = ModelPreferences(PimApplication.pimContext).getObject(
            SharedPrefEnum.DEVICE_ID.key,
            DeviceID::class.java
        )?.number
        if (deviceId != null) {
            val input =
                UpdatePIMSettingsInput.builder().deviceId(deviceId).overheatedTimeStamp(currentTime)
                    .build()
            mAppSyncClient?.mutate(UpdatePimSettingsMutation.builder().parameters(input).build())
                ?.enqueue(
                    updatePIMOverheatTime
                )
        }
    }

    private val updatePIMOverheatTime =
        object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>() {
            override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
                Log.i("PIMOverheat", "PIM over heat: ${response.data()}")
                if (!response.hasErrors()) {
                    Log.i("PIMOverheat", "PIM overheat response successful")
                    overHeatedTimeStamp =
                        response.data()?.updatePIMSettings()?.overheatedTimeStamp()
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.i("PimOverHeat", "error $e")
            }
        }

    internal fun getCurrentDateFormattedDateUtcIso(): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        return sdf.format(Date().time)
    }

    fun getAdInformation(vehicleId: String) {
        val query = GetPimAdvertisementQuery.builder().vehicleId(vehicleId).build()
        mAppSyncClient?.query(query)?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(adQueryCallBack)
    }

    private var adQueryCallBack = object : GraphQLCall.Callback<GetPimAdvertisementQuery.Data>() {
        override fun onResponse(response: Response<GetPimAdvertisementQuery.Data>) {
            Log.i("Ad", "Ad Query response:${response.data()}")
            LoggerHelper.writeToLog("Ad Query response: ${response.data()}", LogEnums.AD_INFO.tag)
          val adUrl =  response.data()?.pimAdvertisement?.advertisement()
          val adType = response.data()?.pimAdvertisement?.adContentType()
          val adDuration = response.data()?.pimAdvertisement?.adDuration()
            val errorCode = response.data()?.pimAdvertisement?.errorCode()
            if(errorCode != null){
                Log.i("Ad", "errorCode $errorCode")
            }
            AdInfoHolder.setAddInformation(adDuration, adType, adUrl)
        }
        override fun onFailure(e: ApolloException) {

        }
    }
    fun unpairPim(vehicleId: String, appSyncClient: AWSAppSyncClient){
        LoggerHelper.writeToLog("Sending request to unpair previous vehicle $vehicleId", LogEnums.TRIP_STATUS.tag)
        val unpairPIMInput = UnpairPIMInput.builder().vehicleId(vehicleId).build()
        appSyncClient.mutate(UnpairPimMutation.builder().parameters(unpairPIMInput).build()).enqueue(
            mutationCallbackUnpairPim)
    }
    private val mutationCallbackUnpairPim = object : GraphQLCall.Callback<UnpairPimMutation.Data>() {
        override fun onResponse(response: Response<UnpairPimMutation.Data>) {
            if(response.hasErrors()){
            LoggerHelper.writeToLog("Error with unPairing previous vehicle tied to phone number.", LogEnums.TRIP_STATUS.tag)
            }
            if(response.data()?.unpairPIM()?.paired() == false){
                LoggerHelper.writeToLog("Successfully unpaired previous pim", LogEnums.TRIP_STATUS.tag)
            } else {
                LoggerHelper.writeToLog("Unsuccessfully unpaired previous pim", LogEnums.TRIP_STATUS.tag)
            }

        }

        override fun onFailure(e: ApolloException) {

        }
    }
}