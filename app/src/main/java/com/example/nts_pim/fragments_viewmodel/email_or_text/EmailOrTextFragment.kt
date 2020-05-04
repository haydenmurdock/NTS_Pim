package com.example.nts_pim.fragments_viewmodel.email_or_text

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlinx.android.synthetic.main.email_or_text_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.amazonaws.amplify.generated.graphql.GetTripQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.international_phone_number.CountryPhoneNumber
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.text.DecimalFormat
import java.util.*


class EmailOrTextFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: EmailOrTextViewModelFactory by instance()
    private lateinit var mJob: Job
    private lateinit var viewModel: EmailOrTextViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
    //Local Variables
    var vehicleId = ""
    var tripId = ""
    var paymentType = ""
    private var tripNumber = 0
    private var tripTotal = 00.00
    private var tripHasEnded = false
    private var previousEmail = ""
    private var previousPhoneNumber = ""
    private var pimNoReceipt = false
    private val decimalFormatter = DecimalFormat("####0.00")
    private var inactiveScreenTimer: CountDownTimer? = null
    private val currentFragmentId = R.id.email_or_text_fragment
    private val logFragment = "Email or Text"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.email_or_text_screen, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mJob = Job()
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(EmailOrTextViewModel::class.java)

        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)

        vehicleId = viewModel.getVehicleID()
        tripId = callBackViewModel.getTripId()
        tripNumber = callBackViewModel.getTripNumber()
        pimNoReceipt = callBackViewModel.getPimNoReceipt()
        receiptCheck(pimNoReceipt)
        checkCustomerDetailsAWS(tripId)
        CountryPhoneNumber.getCountryWithName("all")
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (tripHasEnded ){
                            inactiveScreenTimer?.cancel()
                            inactiveScreenTimer?.start()
                        }
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })
        email_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    email_icon_imageView.setImageDrawable(ContextCompat.getDrawable(
                        context!!, R.drawable.ic_email_icon_grey))
                    email_textView.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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
        text_message_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    text_message_icon_imageView.setImageDrawable(ContextCompat.getDrawable(
                        context!!, R.drawable.ic_text_message_icon_grey))
                    text_textView.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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
        no_receipt_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    no_receipt_btn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                    no_receipt_btn.setBackgroundColor(ContextCompat.getColor(
                        context!!,
                        R.color.squareBtnBackgroundPressed))
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
        setUpUI()
        startInactivityTimeout()
        pimStatusUpdate()
        callBackViewModel.getTripStatus().observe(this, Observer {tripStatus ->
            if(tripStatus == VehicleStatusEnum.TRIP_END.status ||
                tripStatus == VehicleStatusEnum.Trip_Closed.status){
                inactiveScreenTimer
                tripHasEnded = true
            }
        })
        callBackViewModel.getIsTransactionComplete().observe(this, Observer {
            val isTransactionComplete = it
            if (isTransactionComplete) {
                email_or_text_back_btn.isVisible = false
            }
        })
        email_or_text_back_btn.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, Back button hit")
           backToCreditOrCash()
        }
        //Email Button
        email_btn.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, Email button hit")
           toEmailReceipt()
        }
        //Text Message Button
        text_message_btn.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, Text Message button hit")
            toTextReceipt()
        }
        //No Receipt Button
        no_receipt_btn.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, No receipt button hit")
           toThankYou()
        }
    }

    // sets up price on trip total with arguments from trip_review_fragment/square + set the correct sound state for music
    private fun setUpUI() {
        val tripPriceArgs = arguments?.getFloat("tripTotal")
        val paymentTypeArgs = arguments?.getString("paymentType")
        val meterOwedPrice = callBackViewModel.getMeterOwed().value
        if (tripPriceArgs != null && paymentTypeArgs != null) {
            tripTotal = tripPriceArgs.toDouble()
            if (tripTotal == 0.0 ||
                tripTotal == 0.toDouble()){
                if(meterOwedPrice != null){
                    //The pimPayAmount is 0 so we will use the meter owed for receipt.
                    tripTotal = meterOwedPrice
                }
            }
            paymentType = paymentTypeArgs
            val df = decimalFormatter.format(tripTotal)
            val tripTotalAsString = df.toString()
            amount_text_View.text = "$$tripTotalAsString"
        }

        if(paymentTypeArgs == "CASH"){
            val transactionId = UUID.randomUUID().toString()
            callBackViewModel.setTransactionId(transactionId)
            updatePaymentDetail(transactionId,tripNumber,vehicleId, mAWSAppSyncClient!!,"cash", tripId)
        }
        SoundHelper.turnOnSound(context!!)
    }

    private fun updatePaymentDetail(transactionId: String, tripNumber: Int, vehicleId: String, awsAppSyncClient: AWSAppSyncClient, paymentType: String, tripID: String) = launch(Dispatchers.IO){
        LoggerHelper.writeToLog("$logFragment, Cash selected so updated Payment API, $transactionId, $tripNumber, $paymentType")
        PIMMutationHelper.updatePaymentDetails(transactionId, tripNumber, vehicleId, awsAppSyncClient, paymentType, tripID)
    }

    private fun pimStatusUpdate() = launch(Dispatchers.IO){
        PIMMutationHelper.updatePIMStatus(
            vehicleId,
            PIMStatusEnum.RECEIPT_SCREEN.status,
            mAWSAppSyncClient!!
        )
    }

    private fun startInactivityTimeout(){
      inactiveScreenTimer = object: CountDownTimer(60000, 1000) {
            // this is set to 1 min and will finish if a new trip is started.
            override fun onTick(millisUntilFinished: Long) {
                val hasNewTripStarted = callBackViewModel.hasNewTripStarted().value
                if(hasNewTripStarted!!){
//                    inactiveScreenTimer?.onFinish()
                }
            }
            override fun onFinish() {
                if (!resources.getBoolean(R.bool.isSquareBuildOn)) {
                    LoggerHelper.writeToLog("$logFragment, Inactivity Timer finished")
                    toThankYou()
                }
            }
        }.start()
    }

    //We are going to check to see if they need a receipt and if a email/phoneNumber is on file for them
    private fun checkCustomerDetailsAWS(tripID: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context!!)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripID).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(emailOrPhoneNumberQueryCallBack)
    }
    private var emailOrPhoneNumberQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (!response.data()?.trip?.custEmail().isNullOrEmpty() ||
                !response.data()?.trip?.custEmail().isNullOrBlank()) {
                val previousEmailAWS = response.data()?.trip?.custEmail() as String
                previousEmail = previousEmailAWS
                LoggerHelper.writeToLog("$logFragment, checked AWS for custEmail. $previousEmail")
            }

            if(!response.data()?.trip?.custPhoneNbr().isNullOrEmpty() ||
                !response.data()?.trip?.custPhoneNbr().isNullOrBlank()) {
                val previousPhoneNumberAWS = response.data()?.trip?.custPhoneNbr() as String
                previousPhoneNumber = previousPhoneNumberAWS
                LoggerHelper.writeToLog("$logFragment, checked AWS for custEmail. $previousPhoneNumber")
            }
         }
        override fun onFailure(e: ApolloException) {

        }
    }
    private fun receiptCheck(pimNoReceipt: Boolean){
        //True if the customer does not need a receipt
        if(pimNoReceipt){
            LoggerHelper.writeToLog("$logFragment, PIM does not need to take receipt")
            toThankYou()
        }
    }
    //Navigation
    private fun backToCreditOrCash(){
        val action = EmailOrTextFragmentDirections.EmailOrTextBackToTripReview(tripTotal.toFloat()).setMeterOwedPrice(tripTotal.toFloat())
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == (currentFragmentId)){
            navController.navigate(action)
        }
    }

    private fun toEmailReceipt()= launch(Dispatchers.Main.immediate){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        val action = EmailOrTextFragmentDirections.actionEmailOrTextFragmentToReceiptInformationEmailFragment(paymentType,tripTotal.toFloat(),previousEmail)
            .setPaymentType(paymentType)
            .setTripTotal(tripTotal.toFloat())
            .setPreviousEmail(previousEmail)
        if (navController.currentDestination?.id == (currentFragmentId)){
            navController.navigate(action)
        }
    }

    private fun toTextReceipt(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        val action = EmailOrTextFragmentDirections.actionEmailOrTextFragmentToReceiptInformationTextFragment2(
            paymentType,tripTotal.toFloat(),previousPhoneNumber)
            .setPaymentType(paymentType)
            .setTripTotal(tripTotal.toFloat())
            .setPreviousPhoneNumber(previousPhoneNumber)
        if (navController.currentDestination?.id == (currentFragmentId)){
            navController.navigate(action)
        }
    }

    private fun toThankYou(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == (currentFragmentId)){
            navController.navigate(R.id.toInteractionComplete)
        }
    }

    override fun onPause() {
        super.onPause()
        inactiveScreenTimer?.cancel()
        ViewHelper.hideSystemUI(activity!!)
    }

    override fun onDestroy() {
        callBackViewModel.getTripStatus().removeObservers(this)
        callBackViewModel.getIsTransactionComplete().removeObservers(this)
        super.onDestroy()
    }
}




