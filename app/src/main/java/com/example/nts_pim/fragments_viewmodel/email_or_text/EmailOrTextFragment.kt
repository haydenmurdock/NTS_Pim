package com.example.nts_pim.fragments_viewmodel.email_or_text

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.simple_email_helper.EmailHelper
import com.example.nts_pim.utilities.sms_helper.SmsHelper
import kotlinx.android.synthetic.main.email_or_text_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import android.view.inputmethod.EditorInfo
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
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.international_phone_number.CountryPhoneNumber
import com.example.nts_pim.utilities.keyboards.PhoneKeyboard
import com.example.nts_pim.utilities.keyboards.QwertyKeyboard
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.text.DecimalFormat
import java.text.NumberFormat


class EmailOrTextFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: EmailOrTextViewModelFactory by instance()
    private lateinit var mJob: Job
    private lateinit var viewModel: EmailOrTextViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    override val coroutineContext: CoroutineContext

        get() = mJob + Dispatchers.Main
    //Local Variables
    var vehicleId = ""
    var tripId = ""
    var paymentType = ""
    private var tripNumber = 0
    private var tripTotal = 00.00
    private var isQwertyKeyboardUp = false
    private var isPhoneKeyboardUp = false
    private var animatePhoneKeyboardDown = false
    private var animateQwertyKeyboardDown = false
    private val disableAlpha = 0.5f
    private val normalAlpha = 1.0f
    private lateinit var handler:Handler
    private var tripHasEnded = false
    private var phoneNumberAdd = true

    private val decimalFormatter = DecimalFormat("####0.00")

    val screenTimeOutTimer = object: CountDownTimer(60000, 1000) {
        // this is set to 1 min
        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
            if (!resources.getBoolean(R.bool.isSquareBuildOn)) {
                val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                if (navController.currentDestination?.id == (R.id.email_or_text_fragment)) {
                    keyboardViewModel.bothKeyboardsDown()
                    navController.navigate(R.id.toInteractionComplete)
                }
            }
        }
    }

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
        val keyBoardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()

        keyboardViewModel = ViewModelProviders.of(this, keyBoardFactory)
            .get(SettingsKeyboardViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(EmailOrTextViewModel::class.java)

        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)

        vehicleId = viewModel.getVehicleID()
        tripId = callBackViewModel.getTripId()
        tripNumber = callBackViewModel.getTripNumber()
        handler = Handler()
        checkAWSEmailOrPhoneNumber(tripId)
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (tripHasEnded){
                            screenTimeOutTimer.cancel()
                            screenTimeOutTimer.start()
                        }
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })
        val response = CountryPhoneNumber.getCountryWithName("all")
        println(response)
        text_message_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    text_message_btn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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
        })
        email_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    email_btn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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
        })
        no_receipt_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    no_receipt_btn.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
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
        })
        setUpUI()
        updateKeyboard()
        callBackViewModel.getTripStatus().observe(this, Observer {tripStatus ->
            if(tripStatus == VehicleStatusEnum.TRIP_END.status ||
                tripStatus == VehicleStatusEnum.Trip_Closed.status){
                screenTimeOutTimer.start()
                tripHasEnded = true
            }
        })


        callBackViewModel.getIsTransactionComplete().observe(this, Observer {
            val isTransactionComplete = it
            if (isTransactionComplete) {
                email_or_text_back_btn.isVisible = false

            }
        })

       keyboardViewModel.isPhoneKeyboardUp().observe(this, Observer {
            if (it){
                //Slide phone keyboard up
                 handler = Handler()
                val runnable = kotlinx.coroutines.Runnable {
                    no_receipt_btn.isVisible = false
                    email_editText.isEnabled = false
                    email_editText.alpha = disableAlpha
                }
                handler.postDelayed(runnable,200)
                ViewHelper.viewSlideUp(phoneKeyboardScrollView, 500)
            }
           if(!it && !isPhoneKeyboardUp && animatePhoneKeyboardDown){
                //slide phone keyboard down
                handler = Handler()
               val runnable = kotlinx.coroutines.Runnable {
                   no_receipt_btn.isVisible = true
                   email_editText.isEnabled = true
                   email_editText.alpha = normalAlpha
               }
               handler.postDelayed(runnable,200)
               ViewHelper.viewSlideDown(phoneKeyboardScrollView, 500)
               animatePhoneKeyboardDown = false
           }
        })
        keyboardViewModel.isQwertyKeyboardUp().observe(this, Observer {
            if (it){
                // Slide QwertyKeyBoardUp
                    handler = Handler()
                val runnable = kotlinx.coroutines.Runnable {
                    no_receipt_btn.isVisible = false
                    phone_number_editText.isEnabled = false
                    phone_number_editText.alpha = disableAlpha
                }
                    handler.postDelayed(runnable,200)
                ViewHelper.viewSlideUp(qwertyKeyboardScrollView, 500)
            }
            if(!it && !isQwertyKeyboardUp && animateQwertyKeyboardDown){
                //Slide QwertyKeyboardDown
                 handler = Handler()
                val runnable = kotlinx.coroutines.Runnable {
                    no_receipt_btn.isVisible = true
                    phone_number_editText.isEnabled = true
                    phone_number_editText.alpha = normalAlpha
                }
                handler.postDelayed(runnable,200)
                ViewHelper.viewSlideDown(qwertyKeyboardScrollView, 500)
                animateQwertyKeyboardDown = false
            }
        })
        keyboardViewModel.isEnterBtnPressed().observe(this, Observer {
            if(it == "qwerty"){
                animateQwertyKeyboardDown = true
                isQwertyKeyboardUp = false
            }
            if(it == "phone"){
                animatePhoneKeyboardDown = true
                isPhoneKeyboardUp = false
            }
        })

        email_or_text_back_btn.setOnClickListener {
            val action = EmailOrTextFragmentDirections.EmailOrTextBackToTripReview(tripTotal.toFloat()).setMeterOwedPrice(tripTotal.toFloat())
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == (R.id.email_or_text_fragment)){
                keyboardViewModel.bothKeyboardsDown()
                navController.navigate(action)
            }
        }
        phone_number_editText.setOnClickListener {
            isPhoneKeyboardUp = !isPhoneKeyboardUp
            if (!isPhoneKeyboardUp){
                changeKeyboardWatcher("down")
            }else if(isPhoneKeyboardUp) {
                changeKeyboardWatcher("phoneKeyboard")
                animatePhoneKeyboardDown = true
            }
        }

        email_editText.setOnClickListener{
            isQwertyKeyboardUp = !isQwertyKeyboardUp
            if(!isQwertyKeyboardUp){
                //qwerty keyboard is down
               changeKeyboardWatcher("down")
            } else if(isQwertyKeyboardUp) {
                //qwerty keyboard is up
                changeKeyboardWatcher("qwertyKeyboard")
                animateQwertyKeyboardDown = true
            }
        }
        //Looks for 10 numbers in phone number

        val phoneNumberFormatter = object : PhoneNumberFormattingTextWatcher(){
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length != 0) {
                    phone_number_editText.textAlignment = (View.TEXT_ALIGNMENT_TEXT_START)
                }
                screenTimeOutTimer.cancel()
                screenTimeOutTimer.start()
                if (s?.length == 12 || s?.length == 13 && s.startsWith("1")) {
                    enableConfirmBtn(2)
                } else {
                    disableConfirmBtn(2)
                }
                // length is 7 and going forward.
                val isKeyboardGoingForward = keyboardViewModel.isKeyboardGoingForward()
                if(s?.length == 7 && isKeyboardGoingForward){
                    phone_number_editText.text.insert(s.length, "-")
                }
                super.onTextChanged(s, start, before, count)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                println("")
                super.beforeTextChanged(s, start, count, after)
            }

            override fun afterTextChanged(s: Editable?) {
                println("")
                super.afterTextChanged(s)
            }
        }
        phone_number_editText.addTextChangedListener(phoneNumberFormatter)
        //Looks for both a @ and . in email address
        email_editText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int) {
            }
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) { screenTimeOutTimer.cancel()
                screenTimeOutTimer.start()
                if (s.contains("@") && s.contains(".")) {
                    enableConfirmBtn(1)
                } else {
                    disableConfirmBtn(1)
                }

            }
        })

        //Email Button
        email_btn.setOnClickListener {
            isQwertyKeyboardUp = false
            isPhoneKeyboardUp = false
            val recipientEmail = email_editText.text.toString()
            Thread {
                EmailHelper.sendEmail(recipientEmail, tripNumber, tripId, tripTotal, vehicleId ,paymentType)
            }.start()
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == (R.id.email_or_text_fragment)){
            navController.navigate(R.id.toInteractionComplete)
            }
        }

        //Text Message Button
        text_message_btn.setOnClickListener {
            isQwertyKeyboardUp = false
            isPhoneKeyboardUp = false
            val phoneNumber = phone_number_editText.text.toString()
            val trimmedPhoneNumber = phoneNumber.replace("\\s".toRegex(), "")
            val newTrimmedPhoneNumber = checkPhoneString(trimmedPhoneNumber)
            val isPhoneNumberCorrect = doubleCheckPhoneNumber(newTrimmedPhoneNumber)
            if(isPhoneNumberCorrect)
               Thread{
                   SmsHelper.sendSMS(newTrimmedPhoneNumber, tripNumber, tripId, tripTotal, vehicleId, paymentType)
               }.start()
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == (R.id.email_or_text_fragment)){
                navController.navigate(R.id.toInteractionComplete)
            }
        }

        //No Receipt Button
        no_receipt_btn.setOnClickListener {
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == (R.id.email_or_text_fragment)){
                navController.navigate(R.id.toInteractionComplete)
            }
        }
    }

    // sets up price on trip total with arguments from trip_review_fragment/square + set the correct sound state for music
    private fun setUpUI() {
        val tripPriceArgs = arguments?.getFloat("tripTotal")
        var paymentTypeArgs = arguments?.getString("paymentType")
        if (tripPriceArgs != null && paymentTypeArgs != null) {
            tripTotal = tripPriceArgs.toDouble()
            paymentType = paymentTypeArgs
            val df = decimalFormatter.format(tripTotal)
            val tripTotalAsString = df.toString()
            amount_text_View.text = "$$tripTotalAsString"
        }
        SoundHelper.turnOnSound(context!!)
    }
    //Enables confirm button
    private fun enableConfirmBtn(tag: Int) {
        if (tag == 1) {
            email_btn.isClickable = true
            email_btn.isEnabled = true
            email_btn.setTextColor(ContextCompat.getColor(context!!, R.color.whiteTextColor))
        } else {
            text_message_btn.isClickable = true
            text_message_btn.isEnabled = true
            text_message_btn.setTextColor(ContextCompat.getColor(context!!, R.color.whiteTextColor))
        }
    }
    //Disables confirm Button
    private fun disableConfirmBtn(tag: Int) {
        if (tag == 1) {
            email_btn.isEnabled = false
            email_btn.setTextColor(ContextCompat.getColor(context!!, R.color.buttonTextPressed))
        } else {
            text_message_btn.isEnabled = false
            text_message_btn.setTextColor(ContextCompat.getColor(context!!, R.color.buttonTextPressed))
        }
    }
    //Makes sure phone is correctly formatted.
    private fun doubleCheckPhoneNumber(string: String): Boolean {
        if (string.length == 10 || string.length == 11 && string.startsWith("1"))
            if (string.matches("[0-9]+".toRegex()))
                return true
        return false
    }

    //makes sure phoneString is correct format with or without 1 in the front
    private fun checkPhoneString(phoneNumber: String): String{
        if(phoneNumber.length == 10){
            return phoneNumber
        }
        if(phoneNumber.length == 11){
            return phoneNumber.removePrefix("1")
        }
        return ""
    }
    // attaches keyboards to edit text fields

    private fun updateKeyboard() {
        phone_number_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        phone_number_editText.setTextIsSelectable(false)

        email_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        email_editText.setTextIsSelectable(false)

        val phoneKeyboard = phoneKeyboard as PhoneKeyboard
        val ic = phone_number_editText.onCreateInputConnection(EditorInfo())
        phoneKeyboard.setInputConnection(ic)

        val qwertyKeyboard = qwertyKeyboard as QwertyKeyboard
        val ic1 = email_editText.onCreateInputConnection(EditorInfo())
        qwertyKeyboard.setInputConnection(ic1)
    }


    private fun changeKeyboardWatcher(keyboardName: String)= launch{
        if(keyboardName == "phoneKeyboard"){
            keyboardViewModel.phoneKeyboardIsUp()
        } else if(keyboardName == "qwertyKeyboard"){
            keyboardViewModel.qwertyKeyboardisUp()
        } else {

            keyboardViewModel.bothKeyboardsDown()
        }
    }

    private fun checkAWSEmailOrPhoneNumber(tripID: String) = launch(Dispatchers.IO){
            if (mAWSAppSyncClient == null) {
                mAWSAppSyncClient = ClientFactory.getInstance(context!!)
            }
            mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripID).build())
                ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                ?.enqueue(emailOrPhoneNumberQueryCallBack)
        }
        private var emailOrPhoneNumberQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>(){
            override fun onResponse(response: Response<GetTripQuery.Data>) {
                if (response.data()!!.trip.owedPrice() != null) {
                   val email = response.data()?.trip?.custEmail().toString()
                    val phoneNumber = response.data()?.trip?.custPhoneNbr().toString()
                    if (email != ""){
                        launch(Dispatchers.Main.immediate){
                            email_editText.setText(email.trim())
                            email_editText.setSelection(email.length)
                        }
                    }
                     if (phoneNumber != ""){
                         launch(Dispatchers.Main.immediate) {
                             val phoneNumberFormat = PhoneNumberUtils.formatNumber(phoneNumber).trim()
                             phone_number_editText.setText(phoneNumberFormat)
                             phone_number_editText.setSelection(phoneNumberFormat.length)
                         }
                     }
                  }
            }
            override fun onFailure(e: ApolloException) {
                Log.e("ERROR", e.toString())
            }
    }
    override fun onPause() {
        super.onPause()
        screenTimeOutTimer.cancel()
        ViewHelper.hideSystemUI(activity!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardViewModel.isQwertyKeyboardUp().removeObservers(this)
        keyboardViewModel.isPhoneKeyboardUp().removeObservers(this)
        callBackViewModel.getTripStatus().removeObservers(this)
        callBackViewModel.getIsTransactionComplete().removeObservers(this)
    }

}




