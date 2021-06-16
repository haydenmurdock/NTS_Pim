package com.example.nts_pim.fragments_viewmodel.receipt_information

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Adapter
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.Country
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.email_or_text.EmailOrTextViewModel
import com.example.nts_pim.fragments_viewmodel.email_or_text.EmailOrTextViewModelFactory
import com.example.nts_pim.fragments_viewmodel.receipt_information.adapter.CountryCodeAdapter
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.sms_helper.SmsHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.google.gson.Gson
import kotlinx.android.synthetic.main.receipt_information_text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.lang.Error
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.amplify.generated.graphql.SavePaymentDetailsMutation
import com.amazonaws.amplify.generated.graphql.UpdateTripMutation
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import type.SavePaymentDetailsInput
import type.UpdateTripInput
import java.util.*


class ReceiptInformationTextFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: EmailOrTextViewModelFactory by instance<EmailOrTextViewModelFactory>()
    private lateinit var mJob: Job
    private lateinit var viewModel: EmailOrTextViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private val currentFragment = (R.id.receiptInformationTextFragment)
    private val interactionCompleteFragment = (R.id.action_receiptInformationTextFragment_to_interaction_complete_fragment)
    private var adapter: Adapter? = null
    private val url = "https://restcountries.eu/rest/v2/all?fields=name;callingCodes"
    private var countryArray = mutableListOf<Country>()

    var vehicleId = ""
    var tripId = ""
    var paymentType = ""
    private var tripNumber = 0
    private var tripTotal = 00.00
    private var enteredPhoneNumber = ""
    private var transactionId = ""
    private var updatedPhoneNumber = ""
    private var countryListIsShowing = false
    private var internationalNumber = false
    private var inactiveScreenTimer: CountDownTimer? = null
    private val logFragment = "Text Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.receipt_information_text, container, false)

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mJob = Job()
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(EmailOrTextViewModel::class.java)
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        keyboardViewModel = ViewModelProvider(this, keyboardFactory)
            .get(SettingsKeyboardViewModel::class.java)
        transactionId = callBackViewModel.getTransactionId()
        val tripIdForPayment = VehicleTripArrayHolder.getTripIdForPayment()
        tripId = if(tripIdForPayment != ""){
            tripIdForPayment
        } else {
            callBackViewModel.getTripId()
        }
        getCountryWithName()
        getTripDetails()
        startInactivityTimeout()
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        closeSoftKeyboard()
                        inactiveScreenTimer?.cancel()
                        inactiveScreenTimer?.start()
                    }
                    MotionEvent.ACTION_BUTTON_RELEASE ->{
                        closeSoftKeyboard()
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })
        text_editText.setOnClickListener {
            closeSoftKeyboard()
        }
        text_editText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null &&
                    s.length > 11) {
                    enableSendTextBtn()
                }

                if (s != null &&
                    s.length < 11){
                    disableSendTextBtn()
                }
                // length is 7 and going forward.
                val isKeyboardGoingForward = keyboardViewModel.isKeyboardGoingForward()

                if (s?.length == 3 &&
                    isKeyboardGoingForward){
                    enteredPhoneNumber += "-"
                    text_editText.setText(enteredPhoneNumber)
                }

                if (s?.length == 7 &&
                    isKeyboardGoingForward) {
                    enteredPhoneNumber += "-"
                    text_editText.setText(enteredPhoneNumber)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val isKeyboardGoingForward = keyboardViewModel.isKeyboardGoingForward()
                if (enteredPhoneNumber.endsWith("-") && !isKeyboardGoingForward){
                     deleteLastNumberInEditText()
                    }
                text_editText.setSelection(enteredPhoneNumber.length)
            }
        })
        no_receipt_btn_text.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    text_editText.performClick()
                    setTextToGrey(no_receipt_btn_text)
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
        //On touchListeners
        text_receipt_screen_one_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_one_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_one_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_two_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_two_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_two_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_three_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_three_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_three_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_four_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_four_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_four_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_five_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_five_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_five_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_six_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_six_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_six_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_seven_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_seven_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_seven_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_eight_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_eight_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_eight_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_nine_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_nine_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_nine_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_zero_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(text_receipt_screen_zero_btn)
                    keyboardViewModel.keyboardIsGoingForward()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(text_receipt_screen_zero_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        text_receipt_screen_backspace_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    keyboardViewModel.keyboardIsGoingBackward()
                    text_receipt_screen_backspace_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_backspace_arrow_grey))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    text_receipt_screen_backspace_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_backspace_arrow_white))
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))

        send_text_btn_receipt.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if(send_text_btn_receipt.isEnabled){
                        setTextToGrey(send_text_btn_receipt)
                    }
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
        listView.onItemClickListener =
            OnItemClickListener { parent, view, position, id -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Country
                val countryCode = itemValue.callingCodes.first().trim()
                country_code_editText.setText(countryCode)
                listView.visibility = View.GONE
            }

        //on clickListeners phoneKeyboard
        text_receipt_screen_one_btn.setOnClickListener {
            val buttonValue = "1"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_two_btn.setOnClickListener {
            val buttonValue = "2"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_three_btn.setOnClickListener {
            val buttonValue = "3"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_four_btn.setOnClickListener {
            val buttonValue = "4"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_five_btn.setOnClickListener {
            val buttonValue = "5"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_six_btn.setOnClickListener {
            val buttonValue = "6"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_seven_btn.setOnClickListener {
            val buttonValue = "7"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_eight_btn.setOnClickListener {
            val buttonValue = "8"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_nine_btn.setOnClickListener {
            val buttonValue = "9"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_zero_btn.setOnClickListener {
            val buttonValue = "0"
            addNumberInEditText(buttonValue)
        }
        text_receipt_screen_backspace_btn.setOnClickListener {
            text_receipt_screen_backspace_btn.isEnabled = false
            if (enteredPhoneNumber.isNotEmpty()) {
                deleteLastNumberInEditText()
            }
            text_receipt_screen_backspace_btn.isEnabled = true
        }
        //on clickListeners for other buttons
        no_receipt_btn_text.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, no receipt hit", LogEnums.BUTTON_PRESS.tag)
            toThankYou()
        }
        send_text_btn_receipt.setOnClickListener {
            if(paymentType == "cash" ||
                    paymentType == "CASH"){
                updatePaymentDetails(transactionId, tripNumber, vehicleId,mAWSAppSyncClient!!, paymentType, tripId)
            } else {
                sendTextReceipt()
            }
        }
        back_btn_text_receipt.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, back button hit", LogEnums.BUTTON_PRESS.tag)
            backToEmailOrText()
        }
    }
    private fun getTripDetails(){
        vehicleId = viewModel.getVehicleID()
        tripId = callBackViewModel.getTripId()
        tripNumber = callBackViewModel.getTripNumber()
        val tripPriceArgs = arguments?.getFloat("tripTotal")
        val paymentTypeArgs = arguments?.getString("paymentType")
        val previousPhoneNumber = arguments?.getString("previousPhoneNumber")
        if (tripPriceArgs != null && paymentTypeArgs != null) {
            tripTotal = tripPriceArgs.toDouble()
            paymentType = paymentTypeArgs
        }
        if(paymentType == "CASH"){
            transactionId = UUID.randomUUID().toString()
        }
        if(!previousPhoneNumber.isNullOrBlank()){
            val autoFillPhoneNumber = formatPreviousPhoneNumber(previousPhoneNumber)
            if(autoFillPhoneNumber.length == 10){
                val firstPart = autoFillPhoneNumber.substring(0, 3)
                val middlePart = autoFillPhoneNumber.substring(3, 6)
                val lastPart = autoFillPhoneNumber.substring(6, 10)
                val newFullNumber = "$firstPart-$middlePart-$lastPart"
                enteredPhoneNumber = newFullNumber
                text_editText.setText(newFullNumber)
                text_editText.setSelection(newFullNumber.length)
                enableSendTextBtn()
                text_receipt_screen_backspace_btn.isEnabled = true
                keyboardViewModel.keyboardIsGoingForward()
            }
        }
    }
    private fun formatPreviousPhoneNumber(phoneNumber: String): String {
       val updatedNumber = if(phoneNumber[0].toString() == "1"){
            phoneNumber.removePrefix("1")
        } else {
            phoneNumber
        }
        val onlyNumbers = updatedNumber.digitsOnly()
        if(onlyNumbers.length == 10){
            return onlyNumbers
        }
        val phoneNumberLength = onlyNumbers.length
        return onlyNumbers.removeRange(10, phoneNumberLength)
    }
    private fun String.digitsOnly(): String{
        val regex = Regex("[^0-9]")
        return regex.replace(this, "")
    }
    private fun enableSendTextBtn(){
        if(send_text_btn_receipt != null){
            send_text_btn_receipt.isEnabled = true
            }
        }
    private fun disableSendTextBtn(){
        if(send_text_btn_receipt != null){
            send_text_btn_receipt.isEnabled = false
        }
    }
    private fun setTextToGrey(button: Button){
        button.setTextColor((ContextCompat.getColor(requireContext(), R.color.grey)))
    }
    private fun setTextToWhite(button: Button){
        button.setTextColor((ContextCompat.getColor(requireContext(), R.color.whiteTextColor)))
    }
    private fun addNumberInEditText(enteredNumber: String){
        enteredPhoneNumber += enteredNumber
        text_editText.setText(enteredPhoneNumber)
    }
    private fun deleteLastNumberInEditText(){
        if(enteredPhoneNumber.last().toString() == "-"){
            updatedPhoneNumber = enteredPhoneNumber.dropLast(2)
        } else {
            updatedPhoneNumber = enteredPhoneNumber.dropLast(1)
        }
        enteredPhoneNumber = updatedPhoneNumber
        text_editText.setText(enteredPhoneNumber)
        text_receipt_screen_backspace_btn.isEnabled = true
    }
    private fun closeSoftKeyboard(){
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        ViewHelper.hideSystemUI(this.requireActivity())
    }
    @SuppressLint("MissingPermission")
    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        LoggerHelper.writeToLog("$logFragment, Checked internet Connection. Connection ${networkInfo.isConnected}", LogEnums.INTERNET_CONNECTION.tag)
        return networkInfo != null && networkInfo.isConnected
    }
    private fun startInactivityTimeout(){
        inactiveScreenTimer = object: CountDownTimer(60000, 1000) {
            // this is set to 1 min and will finish if a new trip is started.
            override fun onTick(millisUntilFinished: Long) {

            }
            override fun onFinish() {
                if (!resources.getBoolean(R.bool.isSquareBuildOn) &&
                    no_receipt_btn_text != null) {
                    LoggerHelper.writeToLog("$logFragment, Inactivity Timer finished", null)
                    no_receipt_btn_text.performClick()
                }
            }
        }.start()
    }
    private fun sendTextReceipt(){
            callBackViewModel.setTransactionId(transactionId)
            val phoneNumber = text_editText.text.toString()
            val countryCode = country_code_editText.text.toString()
            var combinedNumber = ""
            if (countryCode != "1"){
                   internationalNumber = true
            }
             combinedNumber = if(internationalNumber){
            //formatting of international numbers if it works.
            //combinedNumber = countryCode + "." + phoneNumber
            countryCode + phoneNumber
        } else {
            countryCode + phoneNumber
        }
            val trimmedPhoneNumber = combinedNumber.replace("-", "").trim()

            updateCustomerPhoneNumber(trimmedPhoneNumber)
            toConfirmation()
    }

    private fun updateCustomerPhoneNumber(phoneNumber:String) = launch(Dispatchers.IO) {
        if(isOnline(requireContext())){
            updateCustomerPhoneNumber(vehicleId, phoneNumber,mAWSAppSyncClient!!,tripId)
        } else {
            LoggerHelper.writeToLog("Internet not connect, could not update custPhone number on AWS", LogEnums.RECEIPT.tag)
        }
    }
    //Makes sure phone is correctly formatted.
    private fun getCountryWithName() = launch(Dispatchers.IO){
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread((Runnable {
                        run {
                            try {
                                val gson = Gson()
                                val countryList: List<Country> = gson.fromJson(response.body?.string(), Array<Country>::class.java).toList()
                                countryArray = countryList as MutableList<Country>
                                countryList[1].callingCodes
                                adapter = CountryCodeAdapter(context!!, countryArray)
                                if(listView != null){
                                    listView.adapter = adapter as CountryCodeAdapter
                                } else {
                                    Log.i("Text Receipt", "List View was null so List view not updated with phone numbers")
                                }
                            } catch (e: Error){
                                Log.i("Text Receipt", "Error: for getting international number list. Error:$e")
                            }
                        }
                    }))
                }
                if(!response.isSuccessful){
                   listView.isEnabled = false
                }
            }
        })
    }

    private fun updatePaymentDetails(transactionId: String, tripNumber: Int, vehicleId: String, appSyncClient: AWSAppSyncClient, paymentMethod: String, tripId: String){
        Log.i("Payment AWS", "Trying to send the following to Payment AWS. TransactionId: $transactionId, tripNumber: $tripNumber, vehicleId: $vehicleId, paymentMethod: $paymentMethod, tripID: $tripId")
        val updatePaymentInput = SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber).vehicleId(vehicleId).paymentMethod(paymentMethod).tripId(tripId).build()

        appSyncClient.mutate(SavePaymentDetailsMutation.builder().parameters(updatePaymentInput).build())?.enqueue(mutationCallbackPaymentDetails)
    }
    private val mutationCallbackPaymentDetails = object : GraphQLCall.Callback<SavePaymentDetailsMutation.Data>() {
        override fun onResponse(response: com.apollographql.apollo.api.Response<SavePaymentDetailsMutation.Data>) {
            if(!response.hasErrors()){
               launch(Dispatchers.IO) {
                   LoggerHelper.writeToLog("Payment details have been updated successful. Step 1 Complete", LogEnums.RECEIPT.tag)
                   sendTextReceipt()
               }
            }

        }

        override fun onFailure(e: ApolloException) {
        }
    }
    private fun updateCustomerPhoneNumber(vehicleId: String, custPhoneNumber: String, appSyncClient: AWSAppSyncClient, tripId: String){
        LoggerHelper.writeToLog(" Sending Text to phone number: $custPhoneNumber", LogEnums.RECEIPT.tag)
        val updatePaymentTypeInput = UpdateTripInput.builder().vehicleId(vehicleId).tripId(tripId).custPhoneNbr(custPhoneNumber).build()
        appSyncClient.mutate(UpdateTripMutation.builder().parameters(updatePaymentTypeInput).build())
            ?.enqueue(mutationCustomerPhoneNumberCallback )
    }

    private val mutationCustomerPhoneNumberCallback = object : GraphQLCall.Callback<UpdateTripMutation.Data>() {
        override fun onResponse(response: com.apollographql.apollo.api.Response<UpdateTripMutation.Data>) {
            LoggerHelper.writeToLog("entered phone number response: ${response.data()}", LogEnums.RECEIPT.tag)
            val tripId = callBackViewModel.getTripId()
            val transactionId = callBackViewModel.getTransactionId()
            val custPhoneNumber = response.data()?.updateTrip()?.custPhoneNbr()

            LoggerHelper.writeToLog("Transaction id: $transactionId. tripId: $tripId", LogEnums.RECEIPT.tag)

            if(response.hasErrors()){
                LoggerHelper.writeToLog("Response from aws had errors so did not send text message ${response.errors().get(0).message()}", LogEnums.RECEIPT.tag)
                return
            }
            if(!response.hasErrors()){
                if(response.data() != null){
                    launch(Dispatchers.IO) {
                        LoggerHelper.writeToLog("Updated customer phone number successfully. Step 2 Complete", LogEnums.RECEIPT.tag)
                        SmsHelper.sendSMS(tripId, paymentType, transactionId, custPhoneNumber)
                    }
                }
            }
          }
        override fun onFailure(e: ApolloException) {
        }
    }


    //Navigation
    private fun toConfirmation() = launch(Dispatchers.Main){
        val action = ReceiptInformationTextFragmentDirections
            .actionReceiptInformationTextFragmentToConfirmationFragment(text_editText.text.toString(),tripTotal.toFloat(),"Text")
            .setEmailOrPhoneNumber(text_editText.text.toString())
            .setTripTotal(tripTotal.toFloat())
            .setReceiptType("Text")
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(action)
        }

    }

    private fun toThankYou(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(interactionCompleteFragment)
        }
    }
    private fun backToEmailOrText(){
        val action = ReceiptInformationTextFragmentDirections.actionReceiptInformationTextFragment2ToEmailOrTextFragment(tripTotal.toFloat(),paymentType).setTripTotal(tripTotal.toFloat()).setPaymentType(paymentType)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(action)
        }
    }

    override fun onDestroy() {
        inactiveScreenTimer?.cancel()
        Log.i("Text Receipt", "Inactivity timer canceled")
        super.onDestroy()
    }
}