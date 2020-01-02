package com.example.nts_pim.fragments_viewmodel.receipt_information

import android.content.Context
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
import com.amazonaws.amplify.generated.graphql.UpdateTripMutation
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import type.UpdateTripInput
import java.util.*


class ReceiptInformationTextFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: EmailOrTextViewModelFactory by instance()
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
    private var countryListIsShowing = false
    private var internationalNumber = false
    private var inactiveScreenTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.receipt_information_text, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mJob = Job()
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()


        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(EmailOrTextViewModel::class.java)

        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        keyboardViewModel = ViewModelProviders.of(this, keyboardFactory)
            .get(SettingsKeyboardViewModel::class.java)
        transactionId = callBackViewModel.getTransactionId()
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
                    s.length > 9) {
                    enableSendTextBtn()
                }

                if (s != null &&
                    s.length < 9){
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

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
                        context!!,
                        R.drawable.ic_backspace_arrow_grey))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    text_receipt_screen_backspace_btn.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_backspace_arrow_white))
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        }))
        country_code_editText.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    text_editText.performClick()
                    countryListIsShowing = !countryListIsShowing
                    when(countryListIsShowing){
                        true -> listView.visibility = View.VISIBLE
                        false -> listView.visibility = View.GONE
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
        listView.onItemClickListener = object : OnItemClickListener {

            override fun onItemClick(
                parent: AdapterView<*>, view: View,
                position: Int, id: Long
            ) {
                // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Country
                val countryCode = itemValue.callingCodes.first().trim()
                country_code_editText.setText(countryCode)
                listView.visibility = View.GONE
            }
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
            toThankYou()
        }
        send_text_btn_receipt.setOnClickListener {
            launch {
                updatePaymentDetailsApi().invokeOnCompletion {
                    sendTextReceipt()
                }
            }

        }
        back_btn_text_receipt.setOnClickListener {
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
            val firstPart = previousPhoneNumber.substring(1, 4)
            val middlePart = previousPhoneNumber.substring(4,7)
            val lastPart = previousPhoneNumber.substring(7, previousPhoneNumber.lastIndex + 1)
            val newFullNumber = firstPart + "-" + middlePart + "-" + lastPart
            enteredPhoneNumber = newFullNumber
            text_editText.setText(newFullNumber)
            text_editText.setSelection(newFullNumber.length)
            enableSendTextBtn()
            text_receipt_screen_backspace_btn.isEnabled = true
            keyboardViewModel.keyboardIsGoingForward()
        }
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
        button.setTextColor((ContextCompat.getColor(context!!, R.color.grey)))
    }
    private fun setTextToWhite(button: Button){
        button.setTextColor((ContextCompat.getColor(context!!, R.color.whiteTextColor)))
    }
    private fun addNumberInEditText(enteredNumber: String){
        enteredPhoneNumber += enteredNumber
        text_editText.setText(enteredPhoneNumber)
    }
    private fun deleteLastNumberInEditText(){
        val updatedPhoneNumber = enteredPhoneNumber.dropLast(1)
        enteredPhoneNumber = updatedPhoneNumber
        text_editText.setText(enteredPhoneNumber)
        text_receipt_screen_backspace_btn.isEnabled = true
    }
    private fun closeSoftKeyboard(){
        val imm =
            activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        ViewHelper.hideSystemUI(this.activity!!)
    }
    private fun startInactivityTimeout(){
        inactiveScreenTimer = object: CountDownTimer(60000, 1000) {
            // this is set to 1 min and will finish if a new trip is started.
            override fun onTick(millisUntilFinished: Long) {
                val hasNewTripStarted = callBackViewModel.hasNewTripStarted().value
                if(hasNewTripStarted!!){
   //                 inactiveScreenTimer?.onFinish()
                }
            }
            override fun onFinish() {
                if (!resources.getBoolean(R.bool.isSquareBuildOn) &&
                    no_receipt_btn_text != null) {
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
            if (internationalNumber){
                //formatting of international numbers if it works.
//                combinedNumber = countryCode + "." + phoneNumber
                combinedNumber = countryCode + phoneNumber
            } else {
                combinedNumber = countryCode + phoneNumber
            }
            val trimmedPhoneNumber = combinedNumber.replace("-", "")
            updateCustomerPhoneNumber(trimmedPhoneNumber)
            toConfirmation()
    }
    private fun updatePaymentDetailsApi() = launch(Dispatchers.IO) {
        PIMMutationHelper.updatePaymentDetails(transactionId, tripNumber, vehicleId, mAWSAppSyncClient!!)
    }
    private fun updateCustomerPhoneNumber(phoneNumber:String) = launch(Dispatchers.IO) {
        updateCustomerPhoneNumber(vehicleId, phoneNumber,mAWSAppSyncClient!!,tripId)
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
                                }
                            } catch (e: Error){
                                println("$e")
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
    private fun updateCustomerPhoneNumber(vehicleId: String, custPhoneNumber: String, appSyncClient: AWSAppSyncClient, tripId: String){
        val updatePaymentTypeInput = UpdateTripInput.builder().vehicleId(vehicleId).tripId(tripId).custPhoneNbr(custPhoneNumber).build()
        appSyncClient.mutate(UpdateTripMutation.builder().parameters(updatePaymentTypeInput).build())
            ?.enqueue(mutationCustomerPhoneNumberCallback )
    }

    private val mutationCustomerPhoneNumberCallback = object : GraphQLCall.Callback<UpdateTripMutation.Data>() {
        override fun onResponse(response: com.apollographql.apollo.api.Response<UpdateTripMutation.Data>) {
            Log.i("Results", "Meter Table Updated ${response.data()}")
            if(!response.hasErrors()){
                launch(Dispatchers.IO) {
                        SmsHelper.sendSMS(tripId, paymentType, transactionId)
                }
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the MeterTable: $e")
        }
    }


    //Navigation
    private fun toConfirmation() = launch(Dispatchers.Main){
        val action = ReceiptInformationTextFragmentDirections
            .actionReceiptInformationTextFragmentToConfirmationFragment(text_editText.text.toString(),tripTotal.toFloat(),"Text")
            .setEmailOrPhoneNumber(text_editText.text.toString())
            .setTripTotal(tripTotal.toFloat())
            .setReceiptType("Text")
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(action)
        }

    }

    private fun toThankYou(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(interactionCompleteFragment)
        }
    }
    private fun backToEmailOrText(){
        val action = ReceiptInformationTextFragmentDirections.actionReceiptInformationTextFragment2ToEmailOrTextFragment(tripTotal.toFloat(),paymentType).setTripTotal(tripTotal.toFloat()).setPaymentType(paymentType)
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(action)
        }
    }
}