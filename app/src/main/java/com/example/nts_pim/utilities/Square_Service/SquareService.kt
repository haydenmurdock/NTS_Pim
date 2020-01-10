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
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.SetupComplete
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.view_walker.ViewWalker
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
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
    private var timeout: CountDownTimer? = null
    private var removeCardTimer: CountDownTimer? = null
    private var closeSquareForSoundCheckTimer: CountDownTimer? = null
    private var mSuccessPlayed = false
    private var userHasRemovedCard = false
    private var successfulSoundHasBeenPlayed = false


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
        OTHER_STATE
    }

    fun enableTransactionMode(enable: Boolean) {
        mTransactionMode = enable
    }

    /* The Application class calls Application.registerActivityLifecycleCallbacks and creates
       a new Application.ActivityLifecycleCallbacks object. This catches all onResume, onPause and
       etc. calls to all activities, including the square activities.
       The onActivityResumed() method calls this method
     */

    @SuppressLint("ClickableViewAccessibility")
    fun processActivityResume(activity: Activity) {
        fullScreenMode(activity)
        Log.d(TAG, activity.localClassName)
        val rootView = activity.findViewById<View>(android.R.id.content)

        // Figure our it this onResume call is for the square activity
        val name = activity.localClassName
        if (name == "com.squareup.ui.main.ApiMainActivity") {
            val setupCompleteObject = ModelPreferences(activity.applicationContext)
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

            if (!VehicleTripArrayHolder.squareHasBeenSetUp){
                viewGroup!!.visibility = View.INVISIBLE
                closeSquareForSoundCheck()
                //I tried view.gone but it didn't change anything.
            } else {
                turnUpVolume()
                stateMachine(viewGroup!!, squareActivity!!)
            }
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
                pressCancelButtons()
            }else if (meterState == MeterEnum.METER_TIME_OFF.state){
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
                pressCancelButtons()
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
                        }
                        val meterState = VehicleTripArrayHolder.getMeterState().value
                        if (meterState == MeterEnum.METER_ON.state){
                            stopTimeout()
                            pressCancelButtons()
                        } else {
                            VehicleTripArrayHolder.squareTransactionHasStarted()
                        }
                    }
                }
                SqUIState.DECLINE_STATE -> {
                }

                SqUIState.SIGNATURE_STATE -> {
                }

                SqUIState.RECEIPT_STATE -> {
                }

                SqUIState.SUCCESSFUL_PAYMENT -> {
                }

                else ->{

                }
            }
            // Entering newState
            when (newState) {
                SqUIState.SWIPE_STATE -> {
                    insertCardView = View.inflate(activity, R.layout.insert_card, viewGroup)
                    if (insertCardView != null){
                        val backButton = activity.findViewById<ImageView>(R.id.insert_card_back_btn)
                        if(backButton != null){
                            backButton.setOnClickListener{
                                stopTimeout()
                                pressCancelButtons()
                            }
                        }
                        val imageView = activity.findViewById<ImageView>(R.id.insert_card_imageView)
                        Glide.with(activity.applicationContext)
                            .load(R.raw.insert_swipe_card).into(imageView)
                    }
                    val tripTotal = VehicleTripArrayHolder.getAmountForSquarDisplay().toDouble()
                    val decimalFormatter = DecimalFormat("####00.00")
                    val tripTotalFormatted = decimalFormatter.format(tripTotal)
                    val tripTotalTextView = activity.findViewById<TextView>(R.id.insert_card_pay_price_textView)
                    if(tripTotalTextView != null){
                        tripTotalTextView.text = "$$tripTotalFormatted"
                    }
                    turnUpVolume()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.SUCCESSFUL_PAYMENT -> {
                    turnUpVolume()
                    stopTimeout()
                }
               SqUIState.DECLINE_STATE -> {
                    stopTimeout()
                    startTimeout()
                     playCardDeclinedSound()
                }
                SqUIState.SIGNATURE_STATE -> {
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.REMOVE_CARD_STATE -> {
                    removeCardView = View.inflate(activity,R.layout.activity_remove_card,viewGroup)
                    if (insertCardView != null) {
                        val imageView = activity.findViewById<ImageView>(R.id.remove_card_ImageView)
                        Glide.with(activity.applicationContext)
                            .load(R.raw.remove_card).into(imageView)
                    }
                    turnUpVolume()
                    playPaymentSuccessfulSound()
                    startRemoveCardTimer()
                    stopTimeout()
                }

                SqUIState.NO_INTERNET_STATE -> {
                    startTimeout()
                }

                SqUIState.RECEIPT_STATE -> {
                    if (removeCardView == null){
                        removeCardView = View.inflate(activity,R.layout.activity_remove_card,viewGroup)
                    }
                    pressNoReceipt()
                    userHasRemovedCard = true
                    stopRemoveCardTimer()
                    stopTimeout()
                    removeCardTimer?.cancel()
                    VehicleTripArrayHolder.squareTransactionChange()
                }
                SqUIState.INSERT_TRY_AGAIN_STATE -> {
                    turnUpVolume()
                    playCardDeclinedSound()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.CHIP_ERROR_STATE -> {
                    turnUpVolume()
                    playCardDeclinedSound()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.PAYMENT_CANCELED_STATE -> {
                    turnUpVolume()
                    stopTimeout()
                    startTimeout()
                }
                SqUIState.AUTHORIZING -> {
                }
                SqUIState.INIT_STATE ->{
                }
                SqUIState.OTHER_STATE -> {
                }
            }
            state = newState
        }
    }
    // Swipe screen timeout and crude animation
    private fun startTimeout() {
        timeout = object : CountDownTimer(30000, 1000) {
            //We are running for 30 seconds
            override fun onTick(millisUntilFinished: Long) {
              if (state == SqUIState.OTHER_STATE) {
                  timeout?.cancel()
                }
            }
            override fun onFinish() {
                pressCancelButtons()
//                VehicleTripArrayHolder.squareHasTimedOut()
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
            override fun onTick(millisUntilFinished: Long) {
            }

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

    private fun playPaymentSuccessfulSound(){
        if (!successfulSoundHasBeenPlayed){
            val mediaPlayer = MediaPlayer.create(squareActivity?.applicationContext, R.raw.transaction_complete)
            mediaPlayer.setOnCompletionListener {
                    mediaPlayer.release()
                }
            mediaPlayer.start()
            successfulSoundHasBeenPlayed = true
        }
    }

    private fun playCardDeclinedSound(){
        val mediaPlayer = MediaPlayer.create(squareActivity?.applicationContext, R.raw.card_denied_sound)
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
        mediaPlayer.start()
    }
//    View 2131298335 (7f09081f) marin.widgets.MarinGlyphView Vis:0 ClickableEnabled
//    View 2131298684 (7f09097c) class com.squareup.marin.widgets.MarinGlyphView Vis:0 ClickableEnabled Focusable
//    View 2131298678 (7f090976) class com.squareup.marin.widgets.MarinGlyphView Vis:0 ClickableEnabled Focusable
//    View 2131297991 (7f0906c7) class android.widget.RelativeLayout Vis:0 ClickableEnabled Focusable
//    View 2131298212 (7f0907a4) class com.squareup.marketfont.MarketButton Vis:0 Clickable Enabled Focusable  BUTTON: 'Cancel Payment'
    // This function "cancels" the square operation. How to cancel depends on where we are on the square activities
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
            stopTimeout()
            cancelButton5.performClick()
        } else if (cancelButton4 != null){
            stopTimeout()
            cancelButton4?.performClick()
        } else {
            if (cancelButton1 != null)
                stopTimeout()
                cancelButton1?.performClick()
            if (cancelButton2 != null)
                stopRemoveCardTimer()
                cancelButton2?.performClick()
            if (cancelButton3 != null)
                stopTimeout()
                cancelButton3?.performClick()
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
        Log.d("Square", "Receipt Timeout =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-")
        if (squareActivity == null || squareActivity!!.isFinishing || squareActivity!!.isDestroyed) {
            // Activity not there...don't press buttons
            Log.i(TAG, "Timeout Ignored ***************")
            return
        }

        val noReceipt = getButton(squareActivity!!, com.squareup.sdk.reader.api.R.id.no_receipt_button, "NoReceipt")
        Log.e(TAG, "noReceipt ************ = " + noReceipt!!)
            noReceipt.performClick()
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
        } else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.buyer_actionbar_call_to_action, "Please remove card.")) {
            println("Remove card state added")
            return SqUIState.REMOVE_CARD_STATE
        }else if (testResourceText(view, com.squareup.sdk.reader.api.R.id.buyer_actionbar_call_to_action, "How do you want to receive your digital receipts?")) {
            println("Square receipt state added")
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
        }else{
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
