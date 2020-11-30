package com.example.nts_pim.fragments_viewmodel.trip_review


import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.Square_Service.SquareService
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.enums.*
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.please_wait.*
import kotlinx.android.synthetic.main.trip_review_screen.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.text.DecimalFormat
import java.util.*

class CashOrCardFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: TripReviewViewModelFactory by instance<TripReviewViewModelFactory>()
    private lateinit var viewModel: TripReviewViewModel
    private lateinit var callbackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    var vehicleId = ""
    var tripID = ""
    var tripTotal = 0.0
    var cardInfo = ""
    private val decimalFormatter = DecimalFormat("####00.00")
    private val tripTotalDFUnderTen = DecimalFormat("###0.00")
    private var removeWaitScreenTimer: CountDownTimer? = null
    private var inactiveScreenTimer: CountDownTimer? = null
    private var pimPayAmount: Double? = null
    private var pimPaidAmount: Double = 00.00
    private val currentFragmentId = R.id.trip_review_fragment
    private var doesPimNeedToTakePayment = true
    private var textToSpeech: TextToSpeech? = null
    private var currentTrip:CurrentTrip? = null
    private val logFragment = "Cash or Card"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.trip_review_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = InjectorUtiles.provideCallBackModelFactory()

        callbackViewModel = ViewModelProvider(this, factory)
            .get(CallBackViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(TripReviewViewModel::class.java)

        mAWSAppSyncClient = ClientFactory.getInstance(context)
        vehicleId = viewModel.getVehicleId()
        tripID = callbackViewModel.getTripId()
        pimPaidAmount = callbackViewModel.getPimPaidAmount()

        //we check this value for updating the Trip Review
        pimPayAmount = callbackViewModel.getPimPayAmount()
        tripTotal = pimPayAmount!!.toDouble()
        currentTrip = ModelPreferences(requireContext())
            .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
        Log.i("Trip Review","Pim Pay amount at the start of page was $pimPayAmount")
        updatePimStatus()
        updateTripInfo()
        textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                val language = textToSpeech?.setLanguage(Locale.US)
                if (language == TextToSpeech.LANG_MISSING_DATA
                    || language == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("TTS", "The Language is not supported!")
                } else {
                    Log.i("TTS", "Language Supported.")
                    val formattedNumber = decimalFormatter.format(pimPayAmount!!)
                    playTripTotalAmount(formattedNumber)
                }
                Log.i("TTS", "Initialization success.")
            }
        })

        checkIfPIMNeedsToTakePayment(pimPayAmount!!, pimPaidAmount)
        val pimPaidAmountToString = pimPaidAmount.toString()
        playTripTotalAmount(pimPaidAmountToString)
        cash_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    cash_textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                    dollar_sign_imageView.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_currency_icon_grey
                        )
                    )
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        debit_credit_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    credit_textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                    credit_card_imageView.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_credit_card_grey
                        )
                    )
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))

        //To Tip Screen
        debit_credit_btn.setOnClickListener {
            ViewHelper.disableButton(debit_credit_btn)
            // checks that trip total was updated before sending to square
            if (tripTotal > 1.00) {
                VehicleTripArrayHolder.paymentTypeSelected = "card"
                toTipScreen()
                LoggerHelper.writeToLog("$logFragment: Customer Picked Card")
            } else {
                showLessThanDollarToast()
            }
        }
        //To the Email or Text Screen
        cash_btn.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment: Customer Picked Cash")
            VehicleTripArrayHolder.paymentTypeSelected = "cash"
            launch {
                toEmailOrTextWithPayment()
            }.invokeOnCompletion {
                updateAWSWithCashButtonSelection()
            }
        }

        callbackViewModel.getIsTransactionComplete()
            .observe(this.viewLifecycleOwner, Observer { transactionIsComplete ->
                if (transactionIsComplete) {
                    LoggerHelper.writeToLog("$logFragment: Square Transaction Complete: Going to Email Or Text")
                    toEmailOrTextForSquareTransactionComplete()
                }
            })

        callbackViewModel.getMeterState().observe(this.viewLifecycleOwner, Observer { meterState ->
            if (meterState == MeterEnum.METER_ON.state) {
                LoggerHelper.writeToLog("$logFragment: Meter State Change: $meterState. Going Back to Live Meter")
                backToLiveMeter()
            }
        })
        callbackViewModel.getPimPaySubscriptionAmount().observe(this.viewLifecycleOwner, Observer {
            if(pimPayAmount != it){
                pimPayAmount = it
                updateTripInfo()
                if(textToSpeech!= null){
                    if(textToSpeech!!.isSpeaking){
                        textToSpeech!!.stop()
                        val formattedNumber = decimalFormatter.format(pimPayAmount!!)
                        playTripTotalAmount(formattedNumber)
                        Log.i("Trip Review", "TTS was stopped and started again with new pim pay amount")
                    } else {
                        val formattedNumber = decimalFormatter.format(pimPayAmount!!)
                        playTripTotalAmount(formattedNumber)
                        Log.i("Trip Review", "TTS was started with new pim pay amount")
                    }
                }
            }
        })
        sendPimStatusBluetooth()
    }
    private fun sendPimStatusBluetooth(){
        VehicleTripArrayHolder.updateInternalPIMStatus(PIMStatusEnum.PAYMENT_SCREEN.status)
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val dataObject = NTSPimPacket.PimStatusObj()
            val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
            Log.i("Bluetooth", "status request packet to be sent == $statusObj")
            (activity as MainActivity).sendBluetoothPacket(statusObj)
        }
    }

    private fun updateTripInfo() {
            if(pimPayAmount == null){
                return
            }
            if (pimPayAmount!! < 10) {
                val formattedArgs = tripTotalDFUnderTen.format(pimPayAmount!!)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                if (trip_total_for_tip_text_view != null){
                    trip_total_for_tip_text_view.text = "$$tripTotalToString"
                }
            } else {
                val formattedArgs = decimalFormatter.format(pimPayAmount!!)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                if (trip_total_for_tip_text_view != null){
                    trip_total_for_tip_text_view.text = "$$tripTotalToString"
                }
            }
        val args = arguments?.getFloat("meterOwedPrice")?.toDouble()
        if (args != null) {
            Log.i("TripReview", "The meterValue passed along was$args")
        }
        LoggerHelper.writeToLog("$logFragment,  Customer is seeing $tripTotal")
    }

    private fun checkIfPIMNeedsToTakePayment(enteredPimPayAmount: Double, enteredPimPaidAmount: Double) {
        //We don't need to take a payment if the pimPayAmount is zero or enteredPimPaidAmount is greater than zero
        if (enteredPimPayAmount == 0.toDouble() ||
            enteredPimPayAmount == 00.00 ||
            enteredPimPaidAmount > 0.toDouble()) {
            Log.i("Trip Review: Trip didn't need payment","Pim Pay amount: $enteredPimPayAmount, Pim paid Amount: $enteredPimPaidAmount")
            LoggerHelper.writeToLog("$logFragment, Trip Review: Trip didn't need payment - Pim Pay amount: $enteredPimPayAmount, Pim paid Amount: $enteredPimPaidAmount")
            doesPimNeedToTakePayment = false
            pimPayAmount = enteredPimPaidAmount
            toEmailOrTextWithoutPayment()
        }
    }
    private fun showLessThanDollarToast() {
        Toast.makeText(
            context,
            "A card payment requires the trip to be over $1.00",
            Toast.LENGTH_LONG
        ).show()
        debit_credit_btn.isEnabled = true
    }

    private fun updatePimStatus() {
        PIMMutationHelper.updatePIMStatus(
            vehicleId,
            PIMStatusEnum.PAYMENT_SCREEN.status,
            mAWSAppSyncClient!!
        )
    }
    private fun playTripTotalAmount(messageToSpeak: String) {
        if (messageToSpeak != "0.0"){
            Log.i("TTS", "play Trip Total amount")
            textToSpeech?.setSpeechRate(0.8.toFloat())
            textToSpeech!!.speak(
                "Amount Owed $$messageToSpeak",
                TextToSpeech.QUEUE_FLUSH,
                null,null
            )
            LoggerHelper.writeToLog("$logFragment,  Pim Read $messageToSpeak to customer")
        } else {
            LoggerHelper.writeToLog("$logFragment,  Pim did not  read $messageToSpeak to customer, since it was still set to zero internally")
        }
    }
    private fun updateAWSWithCashButtonSelection() {
        val coroutineTwo =
            launch(CoroutineName("PIM Status Enum type Co-routine") + Dispatchers.Default) {
                PIMMutationHelper.updatePIMStatus(
                    vehicleId,
                    PIMStatusEnum.CASH_PAYMENT.status,
                    mAWSAppSyncClient!!
                )
            }
        val coroutineOne = launch(CoroutineName("Payment type Co-routine") + Dispatchers.Default) {
            PIMMutationHelper.updatePaymentType(
                vehicleId,
                PaymentTypeEnum.CASH.paymentType,
                mAWSAppSyncClient!!,
                tripID
            )
        }
        coroutineOne.start()
        coroutineTwo.start()
    }
    //Navigation
    private fun toEmailOrTextForSquareTransactionComplete() {
        PIMMutationHelper.updatePIMStatus(
            vehicleId,
            PIMStatusEnum.SQUARE_PAYMENT_COMPLETE.status,
            mAWSAppSyncClient!!
        )
        sendPimStatusBluetooth()
        val tripTotalFloat = tripTotal.toFloat()
        val actionComplete = CashOrCardFragmentDirections.toEmailOrText(tripTotalFloat, "CARD")
            .setTripTotal(tripTotalFloat)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        navController.navigate(actionComplete)
    }

    private fun toTipScreen() {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            val action = CashOrCardFragmentDirections.toTipScreen(tripTotal.toFloat(), 0.toFloat())
                .setTipScreenTripTotal(tripTotal.toFloat())
            navController.navigate(action)
        }
    }

    private fun toEmailOrTextWithPayment() {
        val action = CashOrCardFragmentDirections.toEmailOrText(tripTotal.toFloat(), "CASH")
            .setTripTotal(tripTotal.toFloat())
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            navController.navigate(action)
        }
    }

    private fun toEmailOrTextWithoutPayment() {
        val action = CashOrCardFragmentDirections.toEmailOrText(tripTotal.toFloat(), "NONE")
            .setTripTotal(tripTotal.toFloat())
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            navController.navigate(action)
        }
    }

    private fun toInterActionComplete() {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            PIMMutationHelper.updatePIMStatus(
                vehicleId,
                PIMStatusEnum.PAYMENT_ERROR.status,
                mAWSAppSyncClient!!
            )
            navController.navigate(R.id.activityTimeOutToInteractionComplete)
        }
    }

    private fun backToLiveMeter() {
        SquareService().pressCancelButtons()
        val tripTotalFloat = tripTotal.toFloat()
        val action = CashOrCardFragmentDirections.backToLiveMeter(tripTotalFloat)
            .setMeterTotal(tripTotalFloat)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            callbackViewModel.updateCurrentTrip(true, tripID,"ON", requireContext())
            navController.navigate(action)
        }
    }
    private fun startInactivityTimer() {
        inactiveScreenTimer = object : CountDownTimer(60000, 1000) {
            //1 min inactivity timer
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                toInterActionComplete()
            }
        }.start()
    }


    override fun onPause() {
        super.onPause()
        inactiveScreenTimer?.cancel()
        callbackViewModel.getTripStatus().removeObservers(this)
        textToSpeech?.shutdown()
    }

    override fun onStop() {
        super.onStop()
        inactiveScreenTimer?.cancel()
        callbackViewModel.getTripStatus().removeObservers(this)
    }

    override fun onResume() {
        super.onResume()
        callbackViewModel.getTripStatus().observe(this, Observer { tripStatus ->
            if (tripStatus == VehicleStatusEnum.TRIP_END.status ||
                tripStatus == VehicleStatusEnum.Trip_Closed.status
            ) {
                startInactivityTimer()
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        if(this::callbackViewModel.isInitialized){
            callbackViewModel.getIsTransactionComplete().removeObservers(this)
            Log.i("Observer", "is transaction complete observer removed")
            callbackViewModel.getMeterState().removeObservers(this)
            Log.i("Observer", "Meter state observer removed")
            callbackViewModel.getTripStatus().removeObservers(this)
            callbackViewModel.getPimPaySubscriptionAmount().removeObservers(this)
        }
        if (textToSpeech != null) {
            textToSpeech?.shutdown()
        }
        inactiveScreenTimer?.cancel()
        removeWaitScreenTimer?.cancel()
    }
}
