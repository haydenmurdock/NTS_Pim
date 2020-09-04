package com.example.nts_pim.fragments_viewmodel.confirmation

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.interaction_complete.InteractionCompleteViewModel
import com.example.nts_pim.fragments_viewmodel.interaction_complete.InteractionCompleteViewModelFactory
import com.example.nts_pim.utilities.driver_receipt.DriverReceiptHelper
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import kotlinx.android.synthetic.main.confirmation_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.*

class ConfirmationFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: InteractionCompleteViewModelFactory by instance()
    private var vehicleId = ""
    private var tripId = ""
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var callbackViewModel: CallBackViewModel
    private lateinit var viewModel: InteractionCompleteViewModel
    private val currentFragmentId = R.id.confirmationFragment
    private val logFragment = "Confirmation fragment"
    private var tripStatus:String? = ""

    private val restartAppTimer = object: CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if(confirmation_type_textView == null){
                cancel()
            }
        }
        override fun onFinish() {
            callbackViewModel.clearAllTripValues()
            restartApp()
        }
    }

    var tripTotal:Float = 0.0.toFloat()
    var tipAmount:Double = 0.0

    private val decimalFormatter = DecimalFormat("####00.00")
    private val tripTotalDFUnderTen = DecimalFormat("###0.00")
    private var transactionType: String? = null
    private var transactionId:String? = null

    private val visible = View.VISIBLE
    private val gone = View.GONE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.confirmation_screen, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callbackViewModel = ViewModelProviders.of(this,factory).get(CallBackViewModel::class.java)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(InteractionCompleteViewModel::class.java)
        val tripIdForPayment = VehicleTripArrayHolder.getTripIdForPayment()
        tripId = if(tripIdForPayment != ""){
            tripIdForPayment
        } else {
            callbackViewModel.getTripId()
        }
        vehicleId = viewModel.getVehicleID()
        tripStatus = callbackViewModel.getTripStatus().value
        transactionType = VehicleTripArrayHolder.paymentTypeSelected
        transactionId = callbackViewModel.getTransactionId()
        if(transactionId == ""){
            transactionId = UUID.randomUUID().toString()
        }
        getTripDetails()
        checkIfTransactionIsComplete()
        sendDriverReceipt()
        runEndTripMutation()
        setInternalCurrentTripStatus()
        changeEndTripInternalBool()
        restartAppTimer.start()
    }

    private fun getTripDetails(){
            val messageTypeArgs = arguments?.getString("emailOrPhoneNumber")
            val tripTotalArgs = arguments?.getFloat("tripTotal")
            val receiptTypeArgs = arguments?.getString("receiptType")
            tipAmount = callbackViewModel.getTipAmount()
            if(messageTypeArgs != null &&
                tripTotalArgs != null &&
                    receiptTypeArgs != null){
                if (receiptTypeArgs == "Text"){
                    if (phoneNumberGroup != null){
                        phoneNumberGroup.visibility = visible
                    }
                    val (areaCode, firstThree, lastFour) = maskPhoneNumber(messageTypeArgs)
                    areaCode_textView.text = areaCode
                    middle_three_phone_number_textView.text = " $firstThree"
                    if(last_four_phone_number_textView != null){
                        last_four_phone_number_textView.visibility = View.VISIBLE
                        last_four_phone_number_textView.text = " - $lastFour"
                    }

                }

                if(receiptTypeArgs == "Email"){
                    if (phoneNumberGroup != null){
                        phoneNumberGroup.visibility = gone
                    }
                    email_detail_textView.visibility =  visible
                    email_detail_textView.text = maskEmailType(messageTypeArgs)
                }
                tripTotal = tripTotalArgs
                formatUIForTripTotal(tripTotalArgs)
                if (receiptTypeArgs == "Text"){
                    confirmation_type_textView.text = "$receiptTypeArgs message sent. You're all done!"
                } else {
                    confirmation_type_textView.text = "$receiptTypeArgs sent. You're all done!"
                }

            }
    }
    private fun formatUIForTripTotal(tripTotal: Float){
        // WIll need to fix this logic
        val originalTripTotal = tripTotal - tipAmount
        if (tripTotal < 10.00){
            val formattedTripTotal = tripTotalDFUnderTen.format(tripTotal)
            val formattedTipAmount = tripTotalDFUnderTen.format(tipAmount)
            val formattedOriginalTipAmount = tripTotalDFUnderTen.format(originalTripTotal)
            if(tipAmount > 0){
                tripTotal_textView.text = "$$formattedTripTotal($formattedOriginalTipAmount + $formattedTipAmount)"
            } else {
                tripTotal_textView.text = "$$formattedTripTotal"
            }

        } else {
            if (tipAmount > 0){
                val formattedTripTotal = decimalFormatter.format(tripTotal)
                val formattedOriginalTipAmount = decimalFormatter.format(originalTripTotal)
                val formattedTipAmount = decimalFormatter.format(tipAmount)
                tripTotal_textView.text = "$$formattedTripTotal($formattedOriginalTipAmount + $formattedTipAmount)"
            } else {
                val formattedTripTotal = decimalFormatter.format(tripTotal)
                tripTotal_textView.text = "$$formattedTripTotal"
            }

        }
    }

    private fun sendDriverReceipt() = launch(Dispatchers.IO){
        transactionType?.let { transactionId?.let { it1 ->
            DriverReceiptHelper.sendReceipt(tripId, it,
                it1
            )
        } }
    }

    private fun maskEmailType(message: String):String{
        //we are going to update email/text string to UI that only has certain characters showing
//            if(last_four_phone_number_textView != null){
//                last_four_phone_number_textView.visibility = (View.INVISIBLE)
//            }
//            val atSign = "@".toRegex()
//
//            val countToAtSignRange = atSign.find(message, 2)?.range
//            val countToAtSign= countToAtSignRange?.first
//
//            val firstPartOfEmail = message.substring(0, countToAtSign!! + 1)
//            val firstPartFormatted = firstPartOfEmail.replace("(?<=.{2}).(?=.*@)".toRegex(), "*")
//            val emailEndPiece = message.substringAfter("@")
//            val middlePartOfEmail = emailEndPiece.substringBefore(".")
//            val middlePartFormatted = middlePartOfEmail.replace("(?<=.{2}).(?=.*)".toRegex(),"*")
//
//            val lastPartOfEmail = message.substringAfterLast(".")

           return message
    }

    private fun maskPhoneNumber(phoneNumber: String): Triple<String, String, String>{
        val trimmedPhoneNumber = phoneNumber.replace("-", "")
        val lastFour = trimmedPhoneNumber.substring(trimmedPhoneNumber.length - 4)
        val updatedNumber = trimmedPhoneNumber.dropLast(4)
        val middleThree = updatedNumber.substring(updatedNumber.length - 3)
        val areaCode = trimmedPhoneNumber.dropLast(7)
        return Triple(areaCode,middleThree,lastFour)
    }
    private fun changeEndTripInternalBool(){
        val time = LocalDateTime.now()
        TripDetails.tripEndTime = time
        callbackViewModel.tripHasEnded()
    }
    private fun restartApp() {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            navController.navigate(R.id.action_confirmationFragment_to_welcome_fragment)
        }
    }

    private fun checkIfTransactionIsComplete(){
        val isTransactionComplete = callbackViewModel.getIsTransactionComplete().value
        if(isTransactionComplete != null){
            if(isTransactionComplete) {
                callbackViewModel.squareChangeTransaction()
            }
        }
    }
    private fun runEndTripMutation() = launch {
            if (tripStatus != null &&
                tripStatus == VehicleStatusEnum.TRIP_PICKED_UP.status &&
                isOnline(requireContext())){
                //Internet is connected
                Log.i("LOGGER", "trip status was still picked up. Sent end trip status")
                PIMMutationHelper.updateTripStatus(vehicleId, VehicleStatusEnum.TRIP_END.status, mAWSAppSyncClient!!, tripId)
             }
            if(tripStatus != null && tripStatus
                == VehicleStatusEnum.TRIP_PICKED_UP.status &&
                !isOnline(requireContext())){
                //Internet is connected not connected so we will change the internal trip status
                callbackViewModel.addTripStatus("End")
                Log.i("LOGGER", "trip status was still picked up. Internet was not connected so changed internal value to End")
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
        LoggerHelper.writeToLog("$logFragment, internal trip status changed. Trip Active ${currentTrip?.isActive}")
        TripDetails.textToSpeechActivated = false

    }

    override fun onDestroy() {
        super.onDestroy()
        restartAppTimer.cancel()
        if(this::callbackViewModel.isInitialized){
           callbackViewModel.clearAllTripValues()
        }
    }
}
