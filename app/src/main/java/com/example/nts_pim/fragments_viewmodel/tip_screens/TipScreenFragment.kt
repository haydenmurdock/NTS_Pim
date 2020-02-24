package com.example.nts_pim.fragments_viewmodel.tip_screens

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.UpdateTripMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.live_meter.LiveMeterViewModel
import com.example.nts_pim.fragments_viewmodel.live_meter.LiveMeterViewModelFactory
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.checkout.*
import com.squareup.sdk.reader.core.CallbackReference
import com.squareup.sdk.reader.core.Result
import com.squareup.sdk.reader.core.ResultError
import kotlinx.android.synthetic.main.tip_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import type.UpdateTripInput
import java.text.DecimalFormat
import java.util.*


class TipScreenFragment: ScopedFragment(),KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: LiveMeterViewModelFactory by instance()
    private var tripTotal = 00.00
    private var tripTotalReset = 00.00
    private var amountForSquare = 00.00
    private val tripTotalDF = DecimalFormat("####00.00")
    private val tripTotalDFUnderTen = DecimalFormat("###0.00")
    private var tripTotalOption1 = 00.00
    private var tripTotalOption2 = 00.00
    private var tripTotalOption3 = 00.00
    private var tripTotalOption4 = 00.00
    private var fifteenPercent = 00.00
    private var twentyPercent = 00.00
    private var twentyFivePercent = 00.00
    private var thirtyPercent = 00.00
    private var tipAmountPassedToSquare = 00.00
    private var tripTotalBackFromSquare = 00.00
    private var tipPercentPicked = 00.00
    private lateinit var callbackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var checkoutCallbackRef: CallbackReference? = null
    private lateinit var viewModel: LiveMeterViewModel
    private var vehicleId = ""
    private var tripId = ""
    private var tripNumber = 0
    var cardInfo = ""
    private var transactionDate: Date? = null
    private var transactionId = ""
    private var paymentSentForSquare = false
    private val logFragment = "Tip Screen"
    private var driverId: Int? = null


    val screenTimeOutTimer = object: CountDownTimer(30000, 1000) {
        // this is set to 30 seconds.
        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.tip_screen,container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgsFromTripReview()

        updateUI()
        tripTotalReset = tripTotal
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val factory = InjectorUtiles.provideCallBackModelFactory()

        callbackViewModel = ViewModelProviders.of(this, factory)
            .get(CallBackViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(LiveMeterViewModel::class.java)

        vehicleId = viewModel.getVehicleID()
        tripId = callbackViewModel.getTripId()
        tripNumber = callbackViewModel.getTripNumber()
        driverId = callbackViewModel.getDriverId()
        val checkoutManager = ReaderSdk.checkoutManager()
        checkoutCallbackRef = checkoutManager.addCheckoutActivityCallback(this::onCheckoutResult)
        getArgsFromCustomTipScreen()
        PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.TIP_SCREEN.status, mAWSAppSyncClient!!)

        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        screenTimeOutTimer.cancel()
                        screenTimeOutTimer.start()
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })
        fifteen_percent_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGreyForButtonPress(fifteen_percent_tip_amount_text_view)
                    setTextToGreyForButtonPress(fifteen_percent_text_view)
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

        twenty_percent_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGreyForButtonPress(twenty_percent_tip_amount_text_view)
                    setTextToGreyForButtonPress(twenty_percent_text_view)
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

        twenty_five_percent_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGreyForButtonPress(twenty_five_percent_tip_amount_text_view)
                    setTextToGreyForButtonPress(twenty_five_percent_text_view)
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

        thirty_percent_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGreyForButtonPress(thirty_percent_tip_amount_text_view)
                    setTextToGreyForButtonPress(thirty_percent_text_view)
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

        customTipAmountBtn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    customTipAmountBtn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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

        no_tip_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    no_tip_btn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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

        closeTipScreenBtn.setOnClickListener {
            LoggerHelper.writeToLog(context!!, "$logFragment, Back button hit")
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if(navController.currentDestination?.id == (R.id.tipScreenFragment)){
                val action = TipScreenFragmentDirections.backToTripReview(tripTotal.toFloat()).setMeterOwedPrice(tripTotal.toFloat())
                navController.navigate(action)
            }

        }
        fifteen_percent_btn.setOnClickListener {
                amountForSquare = tripTotalOption1
                squareCheckout(amountForSquare)
                updateTripTotalTextField(tripTotalOption1)
                tipAmountPassedToSquare = fifteenPercent
                callbackViewModel.setTipAmount(tipAmountPassedToSquare)
                if(tripTotal < 10.00){
                    tipPercentPicked = 0.0
                } else {
                    tipPercentPicked = 00.15
                }
                lowerAlpha()
        }
        twenty_percent_btn.setOnClickListener {
                amountForSquare = tripTotalOption2
                squareCheckout(amountForSquare)
                updateTripTotalTextField(amountForSquare)
                tipAmountPassedToSquare = twentyPercent
                callbackViewModel.setTipAmount(tipAmountPassedToSquare)
                 if(tripTotal < 10.00){
                     tipPercentPicked = 0.0
                 } else {
                     tipPercentPicked = 00.20
                 }
                lowerAlpha()
        }
        twenty_five_percent_btn.setOnClickListener {
                amountForSquare= tripTotalOption3
                squareCheckout(amountForSquare)
                updateTripTotalTextField(amountForSquare)
                tipAmountPassedToSquare = twentyFivePercent
                callbackViewModel.setTipAmount(tipAmountPassedToSquare)
                 if(tripTotal < 10.00){
                    tipPercentPicked = 0.0
                } else {
                     tipPercentPicked = 00.25
                }

                lowerAlpha()

        }
        thirty_percent_btn.setOnClickListener {
            amountForSquare = tripTotalOption4
            squareCheckout(amountForSquare)
            updateTripTotalTextField(amountForSquare)
            tipAmountPassedToSquare = thirtyPercent
            callbackViewModel.setTipAmount(tipAmountPassedToSquare)
            if(tripTotal < 10.00){
                tipPercentPicked = 0.0
            } else {
                tipPercentPicked = 00.30
            }
            lowerAlpha()
        }
        customTipAmountBtn.setOnClickListener {
            customTipAmountBtn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
            toCustomTip()
        }
        no_tip_btn.setOnClickListener {
            squareCheckout(tripTotal)
            lowerAlpha()
        }

        callbackViewModel.getMeterState().observe(this, Observer { meterState ->
            if (meterState == MeterEnum.METER_ON.state){
                Log.i("Trip Review", "The meter is back on so going to the trip Review screen")
               backToTripReview()
            }
        })
        callbackViewModel.getIsTransactionComplete().observe(this, Observer {transactionIsComplete ->
            if(transactionIsComplete){
                Log.i("Trip Review", "The transaction is complete so going to email or text screen")
               toEmailOrText()
            }
        })
    }
    private fun updateUI() {
        if(tripTotal < 10.00){
            if (fifteen_percent_text_view != null &&
                    fifteen_percent_tip_amount_text_view != null){
                fifteen_percent_text_view.visibility = View.GONE
                fifteen_percent_tip_amount_text_view.text = "$ 1"
                fifteenPercent = 01.00
                tripTotalOption1 = tripTotal + 1

                fifteen_percent_tip_amount_text_view.setLayoutParams(
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            if(twenty_percent_text_view != null &&
                    twenty_percent_tip_amount_text_view != null){
                twenty_percent_text_view.visibility = View.GONE
                twenty_percent_tip_amount_text_view.text = "$ 2"
                twentyPercent = 02.00
                tripTotalOption2 = tripTotal + 2

                twenty_percent_tip_amount_text_view.setLayoutParams(
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }

            if(twenty_five_percent_text_view != null &&
                    twenty_five_percent_tip_amount_text_view != null){
                twenty_five_percent_text_view.visibility = View.GONE
                twenty_five_percent_tip_amount_text_view.text = "$ 3"
                twentyFivePercent = 03.00
                tripTotalOption3 = tripTotal + 3

                twenty_five_percent_tip_amount_text_view.setLayoutParams(
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            if (thirty_percent_text_view != null &&
                    thirty_percent_tip_amount_text_view != null){
                thirty_percent_text_view.visibility = View.GONE
                thirty_percent_tip_amount_text_view.text = "$ 4"
                thirtyPercent = 04.00
                tripTotalOption4 = tripTotal + 4

                thirty_percent_tip_amount_text_view.setLayoutParams(
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }

         }else {
            //This logic is for either 00.00 or 0.00 on the tipPercent amount
            fifteenPercent = tripTotal * 0.15
            tripTotalOption1 = fifteenPercent + tripTotal
            if (fifteenPercent < 10.00){
               val fifteenPercentFormatted = tripTotalDFUnderTen.format(fifteenPercent)
              if (fifteen_percent_text_view != null){
                  fifteen_percent_text_view.text = "$$fifteenPercentFormatted"
              }
            }else {
                val fifteenPercentFormatted = tripTotalDF.format(fifteenPercent)
                if (fifteen_percent_text_view != null){
                    fifteen_percent_text_view.text = "$$fifteenPercentFormatted"
                }
            }
            twentyPercent = tripTotal * 0.20
            tripTotalOption2 = twentyPercent + tripTotal
            if (twentyPercent < 10.00){
                val twentyPercentFormatted = tripTotalDFUnderTen.format(twentyPercent)
                if(twenty_percent_text_view != null){
                    twenty_percent_text_view.text = "$$twentyPercentFormatted"
                }
            } else {
                val twentyPercentFormatted = tripTotalDF.format(twentyPercent)
                if(twenty_percent_text_view != null){
                    twenty_percent_text_view.text = "$$twentyPercentFormatted"
                }
            }
            twentyFivePercent = tripTotal * 0.25
            tripTotalOption3 = twentyFivePercent + tripTotal
            if(twentyFivePercent < 10.00){
                val twentyFivePercentFormatted = tripTotalDFUnderTen.format(twentyFivePercent)
                if(twenty_five_percent_text_view != null){
                    twenty_five_percent_text_view.text = "$$twentyFivePercentFormatted"
                }
            } else {
                val twentyFivePercentFormatted = tripTotalDF.format(twentyFivePercent)
                if(twenty_five_percent_text_view != null){
                    twenty_five_percent_text_view.text = "$$twentyFivePercentFormatted"
                }
            }

            thirtyPercent = tripTotal * 0.30
            tripTotalOption4 = thirtyPercent + tripTotal
            if (thirtyPercent < 10.00){
                val thirtyPercentFormatted = tripTotalDFUnderTen.format(thirtyPercent)
                if(thirty_percent_text_view != null) {
                    thirty_percent_text_view.text = "$$thirtyPercentFormatted"
                }
            }else {
                val thirtyPercentFormatted = tripTotalDF.format(thirtyPercent)
                if(thirty_percent_text_view != null){
                    thirty_percent_text_view.text = "$$thirtyPercentFormatted"
                }
            }
        }
    }
    private fun getArgsFromTripReview(){
        val args = arguments?.getFloat("tipScreenTripTotal")
        if(args != null){
            if (args < 10.00){
                val formattedArgs = tripTotalDFUnderTen.format(args)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                tip_screen_trip_total_textView.text = "$$tripTotalToString"
            }else {
                val formattedArgs = tripTotalDF.format(args)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                tip_screen_trip_total_textView.text = "$$tripTotalToString"
            }
        }
    }
    private fun getArgsFromCustomTipScreen(){
        val hasCustomTipBeingPicked = arguments?.getBoolean("doneButtonTouchedOnCustomTipScreen")
        val tripTotalBeforeTip = arguments?.getFloat("tipScreenTripTotal")
        val tipAmount = arguments?.getFloat("tipChosenFromCustomTipScreen")
        val hasCustomerPickedPercentage = arguments?.getBoolean("percentagePickedForCustomTip")
        val amountForSquareArgs = tripTotalBeforeTip!! + tipAmount!!.toFloat()
        if (tripTotalBeforeTip != null &&
            tipAmount != null) {
            if (amountForSquare < 10.00) {
                tripTotal = tripTotalBeforeTip.toDouble()
                amountForSquare = amountForSquareArgs.toDouble()
                tipAmountPassedToSquare = tipAmount.toDouble()
                val formattedArgs = tripTotalDFUnderTen.format(amountForSquare)
                val tripTotalToString = formattedArgs.toString()
                tip_screen_trip_total_textView.text = "$$tripTotalToString"
            } else {
                tripTotal = tripTotalBeforeTip.toDouble()
                amountForSquare = amountForSquareArgs.toDouble()
                tipAmountPassedToSquare = tipAmount.toDouble()
                val formattedArgs = tripTotalDF.format(amountForSquare)
                val tripTotalToString = formattedArgs.toString()
                tip_screen_trip_total_textView.text = "$$tripTotalToString"
            }
            if(hasCustomTipBeingPicked!!) {
                tipAmountPassedToSquare = tipAmount.toDouble()
                if(hasCustomerPickedPercentage != null && hasCustomerPickedPercentage){
                    tipPercentPicked = tipAmountPassedToSquare/amountForSquare
                }
                squareCheckout(amountForSquare)
                lowerAlpha()
            }
        }
    }
    private fun squareCheckout(checkOutAmount: Double) = launch {
        //Function for square
        LoggerHelper.writeToLog(context!!, "$logFragment,  $checkOutAmount send to start square checkout")
        callbackViewModel.setAmountForSquareDisplay(checkOutAmount)
        val p = checkOutAmount * 100.00
        val checkOutTotal = Math.round(p)
        val amountMoney = Money(checkOutTotal, CurrencyCode.current())
        val parametersBuilder = CheckoutParameters.newBuilder(amountMoney)
        parametersBuilder.skipReceipt(false)
        // if trip number is 0 we use the last 8 of trip id
        if (tripNumber != 0){
            parametersBuilder.note("[$tripNumber] [$vehicleId] [$driverId]")
        } else {
            parametersBuilder.note("[${tripId.substring(8..tripId.length)}] [$vehicleId] [$driverId]")
        }
        val checkoutManager = ReaderSdk.checkoutManager()
        checkoutManager.startCheckoutActivity(context!!, parametersBuilder.build())
        PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.STARTED_SQUARE_PAYMENT.status, mAWSAppSyncClient!!)
    }
    private fun lowerAlpha() {
        val alpha = 0.5f
        val isClickable = false
        //Changes the alpha
        fifteen_percent_frameLayout.alpha = alpha
        twenty_percent_frameLayout.alpha = alpha
        twenty_five_percent_frameLayout.alpha = alpha
        thirty_percent_frameLayout.alpha = alpha
        customTipAmountBtn.alpha = alpha
        no_tip_btn.alpha = alpha
        give_tip_textView.alpha = alpha
        tip_screen_trip_total_textView.alpha = alpha
        //Shows/animates the activity indicator
        square_activity_indicator.animate()
        square_activity_indicator.visibility = View.VISIBLE
        //disables buttons
        fifteen_percent_btn.isEnabled = isClickable
        twenty_percent_btn.isEnabled = isClickable
        twenty_five_percent_btn.isEnabled = isClickable
        thirty_percent_btn.isEnabled = isClickable
        customTipAmountBtn.isEnabled = isClickable
        no_tip_btn.isEnabled = isClickable
        screenTimeOutTimer.cancel()
        screenTimeOutTimer.start()
        LoggerHelper.writeToLog(context!!, "$logFragment,  Lowered Alpha for Square payment")
    }
    private fun raiseAlphaUI(){
        val alpha = 1.0f
        val isClickable = true
        screenTimeOutTimer.cancel()
        //Changes the alpha
        if(fifteen_percent_frameLayout != null){
            fifteen_percent_frameLayout.alpha = alpha
        }
        if (twenty_percent_frameLayout != null){
            twenty_percent_frameLayout.alpha = alpha
        }
        if(twenty_five_percent_frameLayout != null){
            twenty_five_percent_frameLayout.alpha = alpha
        }
        if(thirty_percent_frameLayout != null){
            thirty_percent_frameLayout.alpha = alpha
        }
        if (customTipAmountBtn != null){
            customTipAmountBtn.alpha = alpha
            customTipAmountBtn.isEnabled = isClickable
        }
        if(no_tip_btn != null){
            no_tip_btn.alpha = alpha
            no_tip_btn.isEnabled = isClickable
        }

        if(give_tip_textView != null){
            give_tip_textView.alpha = alpha
        }
        if (tip_screen_trip_total_textView != null){
            tip_screen_trip_total_textView.alpha = alpha
        }
        //hides the activity indicator
        if (square_activity_indicator != null){
            square_activity_indicator.visibility = View.INVISIBLE
        }
        //enables buttons and sets text views back to white
        if(fifteen_percent_btn != null){
            fifteen_percent_btn.isEnabled = isClickable
            setTextBackToWhiteForUIReset(fifteen_percent_tip_amount_text_view)
            setTextBackToWhiteForUIReset(fifteen_percent_text_view)
        }
        if (twenty_percent_btn != null){
            twenty_percent_btn.isEnabled = isClickable
            setTextBackToWhiteForUIReset(twenty_percent_tip_amount_text_view)
            setTextBackToWhiteForUIReset(twenty_percent_text_view)
        }
        if (twenty_five_percent_btn != null){
            twenty_five_percent_btn.isEnabled = isClickable
            setTextBackToWhiteForUIReset(twenty_five_percent_tip_amount_text_view)
            setTextBackToWhiteForUIReset(twenty_five_percent_text_view)
        }
        if (thirty_percent_btn != null){
            thirty_percent_btn.isEnabled = isClickable
            setTextBackToWhiteForUIReset(thirty_percent_tip_amount_text_view)
            setTextBackToWhiteForUIReset(thirty_percent_text_view)
        }

        if(no_tip_btn != null){
            no_tip_btn.setTextColor(ContextCompat.getColor(context!!, R.color.whiteTextColor))
        }
        callbackViewModel.setTipAmount(0.0)
        screenTimeOutTimer.cancel()
        screenTimeOutTimer.start()
        LoggerHelper.writeToLog(context!!, "$logFragment,  Raised Alpha after Square payment")
    }
    private fun onCheckoutResult(result: Result<CheckoutResult, ResultError<CheckoutErrorCode>>) {
        SoundHelper.turnOnSound(context!!)
        if (result.isSuccess) {
            LoggerHelper.writeToLog(context!!, "$logFragment,  Square payment result: Success")
            val checkoutResult = result.successValue
            showCheckoutResult(checkoutResult)
            ViewHelper.hideSystemUI(activity!!)
        } else {
            ViewHelper.hideSystemUI(activity!!)
            val error = result.error
            raiseAlphaUI()
            resetScreen()
            if (error.code ==  CheckoutErrorCode.SDK_NOT_AUTHORIZED) {
               Toast.makeText(
                    context,
                    "SDK not authorized",
                    Toast.LENGTH_SHORT
                ).show()
                PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.PAYMENT_ERROR.status, mAWSAppSyncClient!!)
                LoggerHelper.writeToLog(context!!, "$logFragment,  SDK not authorized for square transaction")
            }
            if (error.code == CheckoutErrorCode.CANCELED) {
                val toast = Toast.makeText(context,
                    "Checkout canceled",
                    Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.TOP, 0,0)
                toast.show()
                PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.CANCELED_SQUARE_PAYMENT.status, mAWSAppSyncClient!!)
                LoggerHelper.writeToLog(context!!, "$logFragment,  square payment canceled")
            }
            if (error.code ==  CheckoutErrorCode.USAGE_ERROR) {
                Toast.makeText(context,
                    "Usage ERROR: Checkout needs to be $1.00 or more",
                    Toast.LENGTH_SHORT
                ).show()
                PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.PAYMENT_ERROR.status, mAWSAppSyncClient!!)
                LoggerHelper.writeToLog(context!!, "$logFragment,  Usage Error from square sdk")
            }
        }
    }
    private fun showCheckoutResult(checkoutResult: CheckoutResult) {
        val tenders = checkoutResult.tenders
        transactionDate = checkoutResult.createdAt
        val updatedTransactionDate = ViewHelper.formatDateUtcIso(transactionDate)
        for (i in tenders){
            transactionId = i.tenderId
            callbackViewModel.setTransactionId(transactionId)
            val cardName = i.cardDetails.card.brand.name
            cardInfo = cardName + " " + i.cardDetails.card.lastFourDigits
            tripTotalBackFromSquare = i.totalMoney.amount.toDouble()
        }
        LoggerHelper.writeToLog(context!!, "$logFragment,  transaction id: $transactionId, cardInfo: $cardInfo, trip total back from Square, $tripTotalBackFromSquare")

        if (cardInfo != "") {
           // updateLocalTripDetails()
        }
        if(cardInfo != "" ) {
            val tipAmountToSendToAWS = callbackViewModel.getTipAmount()
            updateTransactionInfo(
                tipAmountToSendToAWS,
                cardInfo,
                tipPercentPicked,
                amountForSquare,
                updatedTransactionDate,
                transactionId)
        }

        if(cardInfo != "") {
            updatePaymentDetail(
                transactionId,
                tripNumber,
                vehicleId,
                mAWSAppSyncClient!!,
                "card",
                tripId)
        }
    }
    private fun updatePaymentDetail(transactionId: String, tripNumber: Int, vehicleId: String, awsAppSyncClient: AWSAppSyncClient, paymentType: String, tripId: String) = launch(Dispatchers.IO){
        LoggerHelper.writeToLog(context!!, "$logFragment,  Updated Payment Detail Api. transaction id: $transactionId, trip number: $tripNumber, payment type, $paymentType, trip id: $tripId")
        PIMMutationHelper.updatePaymentDetails(transactionId, tripNumber, vehicleId, awsAppSyncClient, paymentType, tripId)
    }

    private fun resetScreen() = launch(Dispatchers.Main.immediate){
        tripTotal = tripTotalReset
        updateTripTotalTextField(tripTotal)
        tipAmountPassedToSquare = 00.00
        LoggerHelper.writeToLog(context!!, "$logFragment, reset screen function")
        updateUI()
    }
    private fun setTextToGreyForButtonPress(textView: TextView){
        if (textView.isVisible){
            textView.setTextColor((ContextCompat.getColor(context!!, R.color.grey)))
        }
    }
    private fun setTextBackToWhiteForUIReset(textView: TextView){
        if(textView.isVisible){
            textView.setTextColor(ContextCompat.getColor(context!!, R.color.whiteTextColor))
        }
    }
    private fun updateTripTotalTextField(tripTotalEntered: Double){
        if (tripTotalEntered < 10.00){
            val formattedArgs = tripTotalDFUnderTen.format(tripTotalEntered)
            tripTotal = formattedArgs.toDouble()
            val tripTotalToString = formattedArgs.toString()
            if (tip_screen_trip_total_textView != null){
                tip_screen_trip_total_textView.text = "$$tripTotalToString"
            }
        }else {
            val formattedArgs = tripTotalDF.format(tripTotalEntered)
            tripTotal = formattedArgs.toDouble()
            val tripTotalToString = formattedArgs.toString()
            if (tip_screen_trip_total_textView != null){
                tip_screen_trip_total_textView.text = "$$tripTotalToString"
            }
        }
    }
    private fun updateTransactionInfo(tipAmount: Double, cardInfo: String, tipPercent: Double, paidAmount: Double, transactionDate: String, transactionId: String) = launch(Dispatchers.IO) {
    if(!paymentSentForSquare){
        paymentSentForSquare = true
        val updateTripInput = UpdateTripInput.builder()
            .vehicleId(vehicleId)
            .tripId(tripId)
            .tipAmt(tipAmount)
            .cardInfo(cardInfo)
            .tipPercent(tipPercent)
            .pimPaidAmt(paidAmount)
            .pimTransDate(transactionDate)
            .pimTransId(transactionId)
            .paymentType("card")
            .build()

        mAWSAppSyncClient?.mutate(UpdateTripMutation.builder().parameters(updateTripInput).build())
            ?.enqueue(transactionInfoCallback)

        }
    }

    private val transactionInfoCallback = object : GraphQLCall.Callback<UpdateTripMutation.Data>() {
        override fun onResponse(response: Response<UpdateTripMutation.Data>) {
            Log.i("Results", "Trip Transaction was set for card and tip amount in AppSync")
            LoggerHelper.writeToLog(context!!, "$logFragment,  Updated aws meter trips table. transaction id:" +
                    " ${response.data()?.updateTrip()?.pimTransId()}, tipAmt: ${response.data()?.updateTrip()?.tipAmt()}" +
                    "payment type, ${response.data()?.updateTrip()?.paymentType()}" +
                    ", TransactionDate: ${response.data()?.updateTrip()?.pimTransDate()}" +
                    ", Card Info: ${response.data()?.updateTrip()?.cardInfo()}")
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", e.toString())
        }
    }

    private fun toCustomTip(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if(navController.currentDestination?.id == (R.id.tipScreenFragment)){
            val action = TipScreenFragmentDirections.ToCustomTipScreen(tripTotal.toFloat()).setTripTotalFromTipScreen(tripTotal.toFloat())
            navController.navigate(action)
        }
    }

    private  fun toEmailOrText(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if(navController.currentDestination?.id == (R.id.tipScreenFragment)){
            val action = TipScreenFragmentDirections.tipFragmentToEmailorTextFragment(amountForSquare.toFloat(), "CARD")
                .setPaymentType("CARD").setTripTotal(amountForSquare.toFloat())
            navController.navigate(action)
        }
    }

    private fun backToTripReview(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if(navController.currentDestination?.id == (R.id.tipScreenFragment)){
            val action = TipScreenFragmentDirections.backToTripReview(tripTotal.toFloat()).setMeterOwedPrice(tripTotal.toFloat())
            navController.navigate(action)
        }
    }
    override fun onPause() {
        super.onPause()
        screenTimeOutTimer.cancel()
        ViewHelper.hideSystemUI(activity!!)
        callbackViewModel.hasSquareTimedOut().observe(this, Observer { hasSquareTimedOut ->
            if (hasSquareTimedOut) {
                val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                if (navController.currentDestination?.id == (R.id.tipScreenFragment)) {
                    val action = TipScreenFragmentDirections.backToTripReview(tripTotal.toFloat())
                        .setMeterOwedPrice(tripTotal.toFloat())
                    navController.navigate(action)
                }
            }
        })
    }
    override fun onResume() {
        super.onResume()
        screenTimeOutTimer.start()
    }
    override fun onDestroy() {
        super.onDestroy()
        callbackViewModel.getIsTransactionComplete().removeObservers(this)
        callbackViewModel.hasSquareTimedOut().removeObservers(this)
        checkoutCallbackRef?.clear()
    }

}