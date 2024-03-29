package com.example.nts_pim.fragments_viewmodel.interaction_complete

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.R
import kotlinx.android.synthetic.main.interaction_complete_screen.*
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.driver_receipt.DriverReceiptHelper
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import kotlinx.coroutines.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.time.LocalDateTime
import java.util.*


class InteractionCompleteFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: InteractionCompleteViewModelFactory by instance<InteractionCompleteViewModelFactory>()
    private var vehicleId = ""
    private var tripId = ""
    private var tripNumber = 0
    private var transactionId = ""
    private var transactionType = ""
    private var paymentMethod: String? = null
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var callbackViewModel: CallBackViewModel
    private lateinit var viewModel: InteractionCompleteViewModel
    private val logFragment = "Thank you screen"
    private var tripStatus:String? = ""
    private val payByApp = "PAY_BY_APP"

    private val restartAppTimer = object: CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if(thank_you_textView == null){
                cancel()
            }
        }
        override fun onFinish() {
            callbackViewModel.clearAllTripValues()
            LoggerHelper.writeToLog("$logFragment, restart Timer Finished", null)
            restartApp()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.interaction_complete_screen,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callbackViewModel = ViewModelProvider(this,factory).get(CallBackViewModel::class.java)
        viewModel = ViewModelProvider(this, viewModelFactory).get(InteractionCompleteViewModel::class.java)
        val tripIdForPayment = VehicleTripArrayHolder.getTripIdForPayment()
        tripId = if(tripIdForPayment != ""){
            tripIdForPayment
        } else {
            callbackViewModel.getTripId()
        }
        TripDetails.insertTripIdIntoCompleted(tripId)
        vehicleId = viewModel.getVehicleID()
        tripNumber = callbackViewModel.getTripNumber()
        transactionId = callbackViewModel.getTransactionId()
        transactionType = VehicleTripArrayHolder.paymentTypeSelected
        tripStatus = callbackViewModel.getTripStatus().value
        paymentMethod = VehicleTripArrayHolder.getPaymentMethod()

        if(transactionId == ""){
            transactionId = UUID.randomUUID().toString()
        }
        if(paymentMethod != payByApp){
            LoggerHelper.writeToLog("$logFragment, $paymentMethod was set for payment method. Sent request to backend to send receipt with none as sendMethod", LogEnums.RECEIPT.tag)
            sendDriverReceipt()
        } else {
            LoggerHelper.writeToLog("$logFragment, $paymentMethod was set for payment method. No receipt sent", LogEnums.RECEIPT.tag)
        }

        runEndTripMutation()
        thank_you_textView.isVisible = false
        checkIfTransactionIsComplete()
        thank_you_textView.isVisible = true
        setInternalCurrentTripStatus()
        changeEndTripInternalBool()
        updatePaymentDetailsAPI()
        restartAppTimer.start()
        callbackViewModel.setPaymentMethod("none")
    }

    private fun updatePaymentDetailsAPI() = launch(Dispatchers.IO){
            // An empty string means that transaction Id has not been from a square payment so they hit cash and did not send a receipt so we need to update
            PIMMutationHelper.updatePaymentDetails(transactionId,tripNumber,vehicleId,mAWSAppSyncClient!!, transactionType, tripId)
        }

    private fun sendDriverReceipt() = launch(Dispatchers.IO){
        DriverReceiptHelper.sendReceipt(tripId,transactionType, transactionId)
    }

    private fun changeEndTripInternalBool(){
        val time = LocalDateTime.now()
        TripDetails.tripEndTime = time
        callbackViewModel.tripHasEnded()
    }
    private fun restartApp() {
            val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            if (navController.currentDestination?.id == R.id.interaction_complete_fragment) {
                navController.navigate(R.id.toRestartNewTrip)
            }
    }
    private fun checkIfTransactionIsComplete(){
        val isTransactionComplete = callbackViewModel.getIsTransactionComplete().value
            if(isTransactionComplete != null)
                if(isTransactionComplete) {
                    callbackViewModel.squareChangeTransaction()
                    LoggerHelper.writeToLog("Square value of isTransactionComplete was true and now is $isTransactionComplete", LogEnums.TRIP_STATUS.tag)
                }
    }
    private fun runEndTripMutation() = launch {
        if (tripStatus != null &&
            tripStatus == VehicleStatusEnum.TRIP_PICKED_UP.status &&
            isOnline(requireContext())){
            PIMMutationHelper.updateTripStatus(vehicleId, VehicleStatusEnum.TRIP_END.status, mAWSAppSyncClient!!, tripId)
        }
        if(tripStatus != null && tripStatus
            == VehicleStatusEnum.TRIP_PICKED_UP.status &&
            !isOnline(requireContext())){
            //Internet is not connected so we will change the internal trip status
            callbackViewModel.addTripStatus("End")
        }
    }

    @SuppressLint("MissingPermission")
    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun setInternalCurrentTripStatus(){
        val currentTrip = ModelPreferences(requireContext()).getObject(
            SharedPrefEnum.CURRENT_TRIP.key,
            CurrentTrip::class.java)
        currentTrip?.isActive = false
        ModelPreferences(requireContext()).putObject(SharedPrefEnum.CURRENT_TRIP.key, currentTrip)
        TripDetails.textToSpeechActivated = false
        LoggerHelper.writeToLog("$logFragment, Internal trip status changed. Trip Active ${currentTrip?.isActive}", LogEnums.TRIP_STATUS.tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        restartAppTimer.cancel()
        if(this::callbackViewModel.isInitialized){
            callbackViewModel.clearAllTripValues()
        }
    }
}