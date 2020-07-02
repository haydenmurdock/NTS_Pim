package com.example.nts_pim.utilities.Square_Service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.MotionEvent
import android.widget.TextView
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.View.OnLayoutChangeListener
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.SetupComplete
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.view_walker.ViewWalker
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.ReaderStatusEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import java.text.DecimalFormat


class SquareService : OnLayoutChangeListener,
    View.OnTouchListener, View.OnClickListener{

    private val vw = ViewWalker()
    private var state = SqUIState.INIT_STATE
    private var squareActivity: Activity? = null
    private var viewGroup:ViewGroup? = null
    private var mTransactionMode = false
    private var removeCardView: View? = null
    private var insertCardView: View? = null
    private var cardReaderCheckView:View? = null
    private var timeout: CountDownTimer? = null
    private var removeCardTimer: CountDownTimer? = null
    private var closeSquareForSoundCheckTimer: CountDownTimer? = null
    private var timerForRemoveCardScreen: CountDownTimer? = null
    private var readerCheckTimer:CountDownTimer? = null
    private var setupCompleteObject: SetupComplete? = null
    private var mSuccessPlayed = false
    private var userHasRemovedCard = false
    private var tag = "Square"
    private var navController: NavController? = null


    private enum class SqUIState {
        INIT_STATE,
        SWIPE_STATE,
        DECLINE_STATE,
        SIGNATURE_STATE,
        RECEIPT_STATE,
        INSERT_TRY_AGAIN_STATE,
        REMOVE_CARD_STATE,
        NO_INTERNET_STATE,
        CHIP_ERROR_STATE,
        PAYMENT_CANCELED_STATE,
        AUTHORIZING,
        SUCCESSFUL_PAYMENT,
        CARD_READER_LIST,
        UNSUCCESSFUL_CARD_PAIRED,
        OTHER_STATE
    }

    fun enableTransactionMode(enable: Boolean) {
        mTransactionMode = enable
    }

    @SuppressLint("ClickableViewAccessibility")
    fun processActivityResume(activity: Activity) {
        fullScreenMode(activity)
        Log.d(TAG, activity.localClassName)
        val rootView = activity.findViewById<View>(android.R.id.content)

        // Figure our it this onResume call is for the square activity
        val name = activity.localClassName
        if (name == "com.squareup.ui.main.ApiMainActivity") {
             setupCompleteObject = ModelPreferences(activity.applicationContext)
                .getObject(SharedPrefEnum.SETUP_COMPLETE.key, SetupComplete::class.java)
            if (setupCompleteObject?.status == false || setupCompleteObject == null){
                return
            }
            squareActivity = activity
            viewGroup = squareActivity!!.findViewById<View>(android.R.id.content) as ViewGroup
            viewGroup?.addOnLayoutChangeListener(this)
            viewGroup?.setOnTouchListener(this)// Call this method each time the activity layout changes
            state = SqUIState.INIT_STATE
            mSuccessPlayed = true
            navController = MainActivity.navigationController
            turnUpVolume()
            stateMachine(viewGroup!!, squareActivity!!)
        }
    }
    // Os, for every layout change of the square activity we will look at what is on the screen to figure out what square is doing
    override fun onLayoutChange(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (VehicleTripArrayHolder.getMeterState().value != null){
            val meterState = VehicleTripArrayHolder.getMeterState().value
            if(meterState == MeterEnum.METER_ON.state){
                Log.i("Square", "Meter was on internal. pressed cancel buttons")
                pressCancelButtonForSquareCheck()
            }else {
                stateMachine(viewGroup!!, squareActivity!!)
            }
        } else {
            return
        }
    }

    // Figure out which square screen and then react to it
    // This code may need to be revised for each square SDK update.
    private fun stateMachine(view: View, activity: Activity) {
        //Each state machine switch we are going to check to see if the meter is on
        if (VehicleTripArrayHolder.getMeterState().value != null){
            val meterState = VehicleTripArrayHolder.getMeterState().value
            if(meterState == MeterEnum.METER_ON.state){
                pressCancelButtonForSquareCheck()
            }
        } else {
            return
        }
        val viewGroup = squareActivity!!.findViewById<View>(android.R.id.content) as ViewGroup
        val newState = computeState(view)
        vw.walk(view)
        if (state != newState || state == SqUIState.OTHER_STATE) {
            // Leaving state
            when (state) {
                SqUIState.INIT_STATE -> {
                }
                SqUIState.REMOVE_CARD_STATE -> {
                }
                SqUIState.NO_INTERNET_STATE -> {
                }
                SqUIState.SWIPE_STATE -> {
                    // we are leaving swipe screen to new screen so we remove image
                    if(viewGroup.isVisible){
                        val linearLayout = view.findViewById<View>(R.id.linearLayout5)
                        viewGroup.removeView(linearLayout)
                        if (insertCardView != null){
                            viewGroup.removeView(insertCardView)
                            VehicleTripArrayHolder.squareTransactionHasStarted()
                        } else {
                            pressCancelButtonForSquareCheck()
                        }
                    } else {
                        pressCancelButtonForSquareCheck()
                    }
                }
                SqUIState.DECLINE_STATE -> {
                }

                SqUIState.SIGNATURE_STATE -> {
                }

                SqUIState.RECEIPT_STATE -> {
                    if (viewGroup.isVisible) {
                        if (removeCardView != null) {
                            viewGroup.removeView(removeCardView)
                            Log.i(tag, "removed removeCardView from viewGroup")
                        }
                    }
                }
                SqUIState.SUCCESSFUL_PAYMENT -> {
                }
                else -> {

                }
            }
            // Entering newState
            when (newState) {
                SqUIState.SWIPE_STATE -> {
                    Log.i(tag, "Square is in swipe state")
                    turnUpVolume()
                    SoundHelper.turnOffSound(activity.applicationContext)
                    val navController = MainActivity.navigationController
                    if(navController.currentDestination?.id == R.id.tipScreenFragment ||
                        navController.currentDestination?.id == R.id.vehicle_settings_detail_fragment){
                        insertCardView = View.inflate(activity, R.layout.insert_card, viewGroup)
                    }

                    if (insertCardView != null){
                        val backButton = activity.findViewById<ImageView>(R.id.insert_card_back_btn)
                         backButton?.setOnClickListener{
                            stopTimeout()
                            pressCancelButtonForSquareCheck()
                        }
                        val imageView = activity.findViewById<ImageView>(R.id.insert_card_imageView)
                        Glide.with(activity.applicationContext)
                            .load(R.raw.insert_swipe_card).into(imageView)
                    }
                    val tripTotal = VehicleTripArrayHolder.getAmountForSquareDisplay().toDouble()
                    val decimalFormatter = DecimalFormat("####00.00")
                    val decimalFormatterUnderTen = DecimalFormat("###0.00")
                    var tripTotalFormatted = ""
                    if(tripTotal < 10){
                        tripTotalFormatted = decimalFormatterUnderTen.format(tripTotal)
                    } else{
                        tripTotalFormatted = decimalFormatter.format(tripTotal)
                    }
                    val tripTotalTextView = activity.findViewById<TextView>(R.id.insert_card_pay_price_textView)
                    if(tripTotalTextView != null){
                        tripTotalTextView.text = "$$tripTotalFormatted"
                    } else {
                        pressCancelButtonForSquareCheck()
                    }
                    stopTimeout()
                    startTimeout()

                }
                SqUIState.SUCCESSFUL_PAYMENT -> {
                    Log.i(tag, "Square is in successful payment state")
                    turnUpVolume()
                    stopTimeout()
                    addRemoveCardScreen(activity)
                }
               SqUIState.DECLINE_STATE -> {
                    Log.i(tag, "Square is in decline state")
                    stopTimeout()
                    startTimeout()
                    playCardDeclinedSound()
                }
                SqUIState.SIGNATURE_STATE -> {
                    Log.i(tag, "Square is in signature state")
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.REMOVE_CARD_STATE -> {
                    Log.i(tag, "Square is remove card state")

                    if (removeCardView == null) {
                        // if the view did not inflate we will inflate it now
                        removeCardView = View.inflate(activity,R.layout.activity_remove_card,viewGroup)
                        val imageView = activity.findViewById<ImageView>(R.id.remove_card_ImageView)
                        Glide.with(activity.applicationContext)
                            .load(R.raw.remove_card).into(imageView)
                        Log.i(tag, "Remove card view was null so Remove_card_state tried to inflate it")
                    }
                    turnUpVolume()
                    startRemoveCardTimer()
                    stopTimeout()
                }

                SqUIState.NO_INTERNET_STATE -> {
                    Log.i(tag, "Square is in no internet state")
                    Log.i(tag,  "Reader is not connected since internet is not connected")
                    stopTimeout()
                    startTimeout()
                }

                SqUIState.RECEIPT_STATE -> {
                    Log.i(tag, "Square is in Receipt State")
                    pressNoReceipt()
                    userHasRemovedCard = true
                    // this will turn off the sound
                    stopRemoveCardTimer()
                    stopTimeout()
                    removeCardTimer?.cancel()
                    VehicleTripArrayHolder.squareTransactionChange()
                }
                SqUIState.INSERT_TRY_AGAIN_STATE -> {
                    Log.i(tag, "Square is in Insert try again state")
                    turnUpVolume()
                    playCardDeclinedSound()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.CHIP_ERROR_STATE -> {
                    Log.i(tag, "Square is in Chip Error State")
                    turnUpVolume()
                    playCardDeclinedSound()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.PAYMENT_CANCELED_STATE -> {
                    Log.i(tag, "Square is in Payment Canceled State")
                    turnUpVolume()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.AUTHORIZING -> {
                    Log.i(tag, "Square is in Authorized State")
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.CARD_READER_LIST -> {
                    Log.i(tag, "Square is in CARD_READER_LIST state")
                    if(!VehicleTripArrayHolder.cardReaderStatusHasBeenChecked) {
                        stopTimeout()
                        // This is where we check the reader status bar to find if we too reauthorize square for a failed reader status
                        cardReaderCheckView = View.inflate(activity,R.layout.card_reader_check_screen, viewGroup)
                        SoundHelper.turnOffSound(activity.applicationContext)
                        Log.i(tag, "Reader has not been check. Inflating view over reader list")
                        val newViewGroup =
                            squareActivity?.findViewById<TextView>(com.squareup.sdk.reader.api.R.id.reader_message_bar_current_text_view)
                        val squareReaderState = newViewGroup?.text
                        Log.i(tag, "Reader Message: $squareReaderState")
                        LoggerHelper.writeToLog("$tag, Reader Message: $squareReaderState")
                        if (squareReaderState!!.contains("Press Button on Reader to Connect – Learn More") ) {
                            LoggerHelper.writeToLog("$tag, Square service is on card reader list in unavailable status")
                            Log.i(
                                tag,
                                "Reader is not connected and is showing unavailable status and check again"
                            )
                            VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.UNAVAILABLE.status)
                            stopReaderCheckTimeout()
                            startReaderCheckTimeout()
                        }
                        if (squareReaderState.contains("Establishing Secure Connection")) {
                            LoggerHelper.writeToLog("$tag, Square service is on card reader list in establishing connection status")
                            Log.i(
                                tag,
                                "Reader is connected and is trying to establish secure connection"
                            )
                            //Begin Connection timer....
                            stopReaderCheckTimeout()
                            startReaderCheckTimeout()

                        }
                        if (squareReaderState.contains("Reader Not Ready")) {
                            //The card list showed the reader as not ready so we need to start the checking process.
                            LoggerHelper.writeToLog("$tag, Square service is on Reader Not Ready- aka- failed status")
                            Log.i(tag, "Reader has failed to connect")
                            VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.FAILED.status)
                            Log.i(tag, "Removed Square Card Reader View")
                            stopReaderCheckTimeout()
                            startReaderCheckTimeout()
                        }

                        if (squareReaderState.contains("Reader Ready") ) {
                            //The card list showed the reader as connected so no need to start the checking process.
                            LoggerHelper.writeToLog("$tag, Square service is on card reader list in Reader Ready status")
                            Log.i(tag, "Reader is connected and ready")
                            stopReaderCheckTimeout()
                            VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.CONNECTED.status)
                            VehicleTripArrayHolder.squareHasBeenSetUp = true
                            removeSquareReaderView()
                        }
                    }
                }
                SqUIState.UNSUCCESSFUL_CARD_PAIRED -> {
                    Log.i(tag, "Square is in UNSUCCESSFUL card paired state")
                }
                SqUIState.INIT_STATE ->{
                }
                SqUIState.OTHER_STATE -> {
                    stopTimeout()
                    startTimeout()
                }
            }
            state = newState
        }
    }
    // Swipe screen timeout and crude animation
    private fun startReaderCheckTimeout(){
        //This is where we check the reader during the process of connecting. If the reader connects during the check we close down the checking process and move on.
        if(readerCheckTimer == null){
            readerCheckTimer = object :CountDownTimer(60000, 2500){
                override fun onTick(millisUntilFinished: Long) {
                    val newViewGroup =
                        squareActivity?.findViewById<TextView>(com.squareup.sdk.reader.api.R.id.reader_message_bar_current_text_view)
                    val squareReaderState = newViewGroup?.text
                    Log.i(tag, "Reader Check Timer: squareReaderState: $squareReaderState")
                    if (squareReaderState == null){
                        Log.i(tag, "Reader checked via readerCheckTimer. Text view was null but timer was still going")
                        LoggerHelper.writeToLog("$tag, Reader checked via readerCheckTimer. Text view was null but timer is still going. Will check again in 2.5 seconds.")
                        stopReaderCheckTimeout()
                        return
                    }
                    if (squareReaderState.contains("Reader Ready")) {
                        Log.i(tag, "Reader checked via readerCheckTimer. Reader is Connected")
                        LoggerHelper.writeToLog("$tag, Reader checked via readerCheckTimer. Reader is Connected")
                        VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.CONNECTED.status)
                        onFinish()
                    }
                }
                override fun onFinish() {
                    val newViewGroup =
                        squareActivity?.findViewById<TextView>(com.squareup.sdk.reader.api.R.id.reader_message_bar_current_text_view)
                    val squareReaderState = newViewGroup?.text
                    if (squareReaderState == null){
                        Log.i(tag, "Reader checked via readerCheckTimer. Text view was null but timer was still going")
                        LoggerHelper.writeToLog("$tag, Reader checked via readerCheckTimer. Timer has finished and there is no text view to read.")
                        return
                    }
                    if(squareReaderState.contains("Reader Not Ready")){
                        Log.i(tag, "Reader checked via readerCheckTimer. Reader has failed")
                        LoggerHelper.writeToLog("$tag, Reader checked via readerCheckTimer. Timer has finished. Reader has failed")
                        VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.FAILED.status)
                        VehicleTripArrayHolder.needToReAuthorizeSquare()
                        removeSquareReaderView()
                    }
                    if(squareReaderState.contains("Establishing Secure Connection")){
                        Log.i(tag, "Reader checked via readerCheckTimer. Reader has failed")
                        LoggerHelper.writeToLog("$tag, Reader checked via readerCheckTimer. Timer has finished. Reader is still establishing connection after 60 seconds. Reader Failed")
                        VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.FAILED.status)
                        VehicleTripArrayHolder.needToReAuthorizeSquare()
                        removeSquareReaderView()
                    }
                    if(squareReaderState.contains("Press Button on Reader to Connect – Learn More")){
                        VehicleTripArrayHolder.updateReaderStatus(ReaderStatusEnum.UNAVAILABLE.status)
                        removeSquareReaderView()
                    } else {
                        Log.i(tag, "Reader check timer finished, but the square square was unknown. reader state == $squareReaderState")
                        LoggerHelper.writeToLog("$tag Reader check timer finished, but the square square was unknown. reader state == $squareReaderState")
                        removeSquareReaderView()
                    }
                }
            }.start()
        } else {
            Log.i(tag, "start Reader timer is already running")
        }
    }
    private fun stopReaderCheckTimeout(){
        if (readerCheckTimer != null) {
            readerCheckTimer!!.cancel()
            Log.i("Square", "Reader Check timer was cancelled")
            readerCheckTimer = null
        }
    }
    private fun removeSquareReaderView(){
        if(cardReaderCheckView != null){
            VehicleTripArrayHolder.readerStatusHasBeenChecked()
            viewGroup?.removeView(cardReaderCheckView).run {
                Log.i(tag, "Removed Square Card Reader View")
                LoggerHelper.writeToLog("$tag, Removed Square Reader Check View and activity.finished() was called")
            }
            squareActivity!!.finish().run {
                VehicleTripArrayHolder.numberOfReaderChecks += 1
            }
        }
    }
    private fun startTimeout() {
        timeout = object : CountDownTimer(30000, 1000) {
            //We are running for 30 seconds
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                pressCancelButtons()
            }
        }.start()
    }

    private fun stopTimeout() {
        if (timeout != null) {
            timeout!!.cancel()
            Log.i("Square", "Square Time out canceled")
            timeout = null
        }
    }
    // "Remove your card" Timer
    private fun startRemoveCardTimer() {
        removeCardTimer = object : CountDownTimer(1000, 1000) {
            // 1 seconds
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (!userHasRemovedCard){
                    playCardRemoverSound()
                    startRemoveCardTimer()
                }
            }
        }.start()
    }

    private fun stopRemoveCardTimer() {
        if (removeCardTimer != null) {
            removeCardTimer!!.cancel()
            removeCardTimer = null
        }
    }

    private fun closeSquareForSoundCheck(){
        closeSquareForSoundCheckTimer = object : CountDownTimer(20000, 1000){
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                timeout?.cancel()
                pressCancelButtonForSquareCheck()
            }
        }.start()
    }

    private fun playCardRemoverSound(){
        val applicationContext = squareActivity?.applicationContext
        SoundHelper.turnOnSound(applicationContext!!)
          val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.transaction_complete)
           mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
        mediaPlayer.start()
    }


    private fun playCardDeclinedSound(){
        val mediaPlayer = MediaPlayer.create(squareActivity?.applicationContext, R.raw.card_denied_sound)
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
        mediaPlayer.start()
    }
    private fun addRemoveCardScreen(activity: Activity){
       timerForRemoveCardScreen = object : CountDownTimer(1250, 1250){
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                if (removeCardView == null) {
                    removeCardView = View.inflate(activity, R.layout.activity_remove_card, viewGroup)
                    val imageView = activity.findViewById<ImageView>(R.id.remove_card_ImageView)
                    Glide.with(activity.applicationContext)
                        .load(R.raw.remove_card).into(imageView)
                    Log.i(tag, "remove card view was inflated via timer")
                }
            }
        }.start()
    }

   fun pressCancelButtons() {
        Log.i(
            "Square",
            "Timeout =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=--=-=-=-=-=-=-="
        )
        if (squareActivity == null || squareActivity!!.isFinishing || squareActivity!!.isDestroyed) {
            // Activity not there...don't press buttons
            Log.i("Square", "Timeout Ignored ***************")
            return
        }

        val noReceipt = getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.no_receipt_button, "NoReceipt")
        //myLog.e(TAG,"noReceipt ************ = " + noReceipt);
        if (noReceipt != null) {
            noReceipt.performClick()
            return
        }

        val cancelButton1 =
            getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.select_payment_up_button, "cancelButton1")
        val cancelButton2 = getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.tutorial_bar_cancel, "cancelButton2")
        val cancelButton3 = getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.tutorial2_cancel, "cancelButton3")
        val cancelButton4 =
            getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.reader_warning_bottom_default_button, "cancelButton4")
        val cancelButton5 = getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.cancel_button, "cancelButton5")

        if(cancelButton5 != null) {
            cancelButton5.performClick()
            VehicleTripArrayHolder.squareHasTimedOut()
        }
        if (cancelButton4 != null) {
            cancelButton4.performClick()
            VehicleTripArrayHolder.squareHasTimedOut()
        }

        if(cancelButton1 != null) {
        cancelButton1.performClick()
        VehicleTripArrayHolder.squareHasTimedOut()
        }
        if(cancelButton2 != null) {
            cancelButton2.performClick()
            VehicleTripArrayHolder.squareHasTimedOut()
        }
    if (cancelButton3 != null) {
        cancelButton3.performClick()
        VehicleTripArrayHolder.squareHasTimedOut()
         }
    }

    private fun pressCancelButtonForSquareCheck() {
        val cancelButton1 =
            getButton(
                squareActivity!!,
                com.squareup.sdk.reader.api.R.id.select_payment_up_button,
                "cancelButton1"
            )
        val cancelButton2 = getButton(
            squareActivity!!,
            com.squareup.sdk.reader.api.R.id.tutorial_bar_cancel,
            "cancelButton2"
        )
        val cancelButton3 = getButton(
            squareActivity!!,
            com.squareup.sdk.reader.api.R.id.tutorial2_cancel,
            "cancelButton3"
        )
        val cancelButton4 =
            getButton(
                squareActivity!!,
                com.squareup.sdk.reader.api.R.id.reader_warning_bottom_default_button,
                "cancelButton4"
            )
        val cancelButton5 = getButton(
            squareActivity!!,
            com.squareup.sdk.reader.api.R.id.cancel_button,
            "cancelButton5"
        )

        if (cancelButton5 != null) {
            cancelButton5.performClick()
        } else if (cancelButton4 != null)
            cancelButton4.performClick()
        else {
            if (cancelButton1 != null)
                cancelButton1.performClick()
            if (cancelButton2 != null)
                cancelButton2.performClick()
            if (cancelButton3 != null)
                cancelButton3.performClick()

            if (squareActivity != null){
                VehicleTripArrayHolder.squareHasBeenSetUp = true
                SoundHelper.turnOnSound(squareActivity!!.applicationContext)
                squareActivity!!.finish()
            }
        }
    }

    // This was an experiment in automatically pressing "no receipt", but I am not using it
    private fun pressNoReceipt() {
        Log.i(tag, "Receipt Timeout =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-")
        if (squareActivity == null || squareActivity!!.isFinishing || squareActivity!!.isDestroyed) {
            // Activity not there...don't press buttons
            Log.i(TAG, "Timeout Ignored ***************")
            return
        }
        val noThanksViewGroup = squareActivity!!.findViewById<View>(com.squareup.sdk.reader.api.R.id.secondary_action_container) as ViewGroup
        if(noThanksViewGroup.childCount == 1){
            (noThanksViewGroup[0] as? Button)?.performClick()
            Log.i(tag, "No thanks button was pressed")
        }

    }

    private fun getButton(activity: Activity, id: Int, name: String): View? {
        var v = activity.findViewById(id) as View?

        if (v != null) {
            if (true) {
                // Debug code
                var s = name + " " + v.getVisibility() + " "
                if (v.isEnabled()) s += "EN "
                if (v.isFocusable()) s += "FO "
                if (v.isClickable()) s += "Clk "
                if (v.isContextClickable()) s += "CtxClk "
                if (v.isShown()) s += "Shown "
                Log.e(TAG, s)
            }

            if (!v.isShown || !v.isEnabled || !v.isClickable)
                v = null
        }

        return v
    }

    // This is the code that looks at what is on the screen and determine which square "state" or screen we are on
    private fun computeState(view: View): SqUIState {
        if (testResourceText(view, com.squareup.sdk.reader.api.R.id.swipe_insert_tap_prompt, "Insert or Tap to Pay")) {
            return SqUIState.SWIPE_STATE
        }else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.swipe_insert_tap_prompt, "Tap, Insert, or Swipe to Pay")) {
            return SqUIState.SWIPE_STATE
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.swipe_insert_tap_prompt, "Swipe to Pay")) {
            return SqUIState.SWIPE_STATE
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.please_sign_here, "Please Sign Here")) {
            return SqUIState.SIGNATURE_STATE
        }else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.noho_buyer_action_bar_left_button, "New Sale")) {
            val newViewGroup = squareActivity?.findViewById<View>(com.squareup.sdk.reader.api.R.id.noho_buyer_action_container_call_to_action) as? ViewGroup
            if(newViewGroup?.childCount == 1) {
                val childView = newViewGroup[0] as TextView
                if(childView.text.contains("Please")) {
                    Log.i(tag, "compute state is going to return remove card state since please remove card is on screen")
                    return SqUIState.REMOVE_CARD_STATE
                } else {
                    Log.i(tag, "compute state is going to return receipt state since please remove card is not on screen")
                    return SqUIState.RECEIPT_STATE
                }
            }
            Log.i(tag, "compute state is going to return receipt state since since there was no child for action container")
            return SqUIState.RECEIPT_STATE
        } else if(testResourceText(view, com.squareup.sdk.reader.api.R.id.glyph_title, "Approved")){
            return SqUIState.SUCCESSFUL_PAYMENT
        }  else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.glyph_message_title, "Please Insert Card")) {
            return SqUIState.INSERT_TRY_AGAIN_STATE
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.glyph_message_title, "Declined")) {
            return SqUIState.DECLINE_STATE
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.glyph_message_title, "Please Try Inserting Again")) {
            return SqUIState.CHIP_ERROR_STATE
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.glyph_message_title, "Payment Canceled")) {
            return SqUIState.PAYMENT_CANCELED_STATE
        } else if (testResourceText(view,R.id.reader_message_bar_current_text_view,"Restore Internet to Take Payments")) {
                return SqUIState.NO_INTERNET_STATE
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.glyph_message_title, "Authorizing")) {
            return SqUIState.AUTHORIZING
        } else if(testResourceText(view, com.squareup.sdk.reader.api.R.id.up_text, "Card Readers")) {
            return SqUIState.CARD_READER_LIST
            }else {
                return SqUIState.OTHER_STATE
             }
        }

    private fun testResourceText(view: View, id: Int, text: String): Boolean {
        try {
            val tv = view.findViewById(id) as TextView
            //myLog.d(TAG,"secondary_button = " + tv.getText().toString());
            return text == tv.text.toString()
        } catch (e: Exception) {
            Log.i("textResourceText", "Text resource error $e")
        }

        return false
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        //myLog.d(TAG,"--------------------- onTouch:"+view.getClass() + " " +event.toString());
        if (event.action == MotionEvent.ACTION_DOWN) {
            stopTimeout()
        }
        return true
    }

    override fun onClick(view: View) {
    }

    // Makes the square activities "full screen mode"
    private fun fullScreenMode(activity: Activity) {
                val decorView = activity.window.decorView
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

    }

    private fun turnUpVolume(){
        val audioManager = squareActivity?.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
        val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        val musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (systemVolume != maxVolume || musicVolume != maxVolume){
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVolume, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        }
    }
}
