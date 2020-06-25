package com.example.nts_pim.fragments_viewmodel.receipt_information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import android.content.Context
import android.net.ConnectivityManager
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.SavePaymentDetailsMutation
import com.amazonaws.amplify.generated.graphql.UpdateTripMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.simple_email_helper.EmailHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.receipt_information_email.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import type.SavePaymentDetailsInput
import type.UpdateTripInput
import java.util.*


class ReceiptInformationEmailFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: ReceiptInformationViewModelFactory by instance<ReceiptInformationViewModelFactory>()
    private lateinit var mJob: Job
    private lateinit var viewModel: ReceiptInformationViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var vehicleId = ""
    private var tripId = ""
    private var tripTotal = 0.0
    private var tripNumber = 0
    private var paymentType = ""
    private var transactionId = ""
    private var email = ""
    private var inactiveScreenTimer: CountDownTimer? = null
    private val logFragment = "Email Receipt"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.receipt_information_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mJob = Job()
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ReceiptInformationViewModel::class.java)
        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        vehicleId = viewModel.getVehicleID()
        transactionId = callBackViewModel.getTransactionId()
        val greyColor = ContextCompat.getColor(context!!,R.color.grey)
        showSoftKeyboard()
        email_editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int) {
            }
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int) {
                if (s.contains("@") && s.contains(".")) {
                    enableSendEmailBtn()
                }
                if(inactiveScreenTimer != null){
                    inactiveScreenTimer?.cancel()
                    Log.i("Email Receipt", "inactivity time was canceled and started")
                    inactiveScreenTimer?.start()
                }
            }
        })
        getTripDetails()
        startInactivityTimeout()
        email_editText.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
                LoggerHelper.writeToLog("$logFragment, edit text does not have focus. Closing soft keyboard")
                closeSoftKeyboard()
            }
        }
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                            inactiveScreenTimer?.cancel()
                            inactiveScreenTimer?.start()
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })
        send_email_btn_receipt.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    send_email_btn_receipt.setTextColor(greyColor)
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
        no_receipt_btn_receipt_email.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                   no_receipt_btn_receipt_email.setTextColor(greyColor)
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

        send_email_btn_receipt.setOnClickListener {
            callBackViewModel.setTransactionId(transactionId)
            email = email_editText.text.toString().trim()
            if(paymentType == "CASH" ||
                    paymentType == "cash"){
                updatePaymentDetails(transactionId, tripNumber,vehicleId, mAWSAppSyncClient!!,paymentType,tripId)
            } else {
                sendEmail(email)
            }
        }

        back_btn_email_receipt.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, back button hit")
            backToEmailOrText()
        }
        no_receipt_btn_receipt_email.setOnClickListener {
            LoggerHelper.writeToLog("$logFragment, no receipt button hit")
            toThankYou()
        }
    }
    private fun getTripDetails(){
        tripId = callBackViewModel.getTripId()
        tripNumber = callBackViewModel.getTripNumber()
        val tripPriceArgs = arguments?.getFloat("tripTotal")
        val paymentTypeArgs = arguments?.getString("paymentType")
        val previousEmail = arguments?.getString("previousEmail")
        if (tripPriceArgs != null && paymentTypeArgs != null) {
            tripTotal = tripPriceArgs.toDouble()
            paymentType = paymentTypeArgs
        }
        if(paymentType == "CASH"){
            transactionId = UUID.randomUUID().toString()
        }
        if(!previousEmail.isNullOrBlank()){
            email_editText.setText(previousEmail)
            email_editText.setSelection(previousEmail.length)
        }
    }
    private fun showSoftKeyboard() {
        val imm =
            activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(email_editText, InputMethodManager.SHOW_IMPLICIT)
        email_editText.requestFocus()
        ViewHelper.hideSystemUI(activity!!)
        LoggerHelper.writeToLog("$logFragment, Showing Keyboard")
    }
    private fun closeSoftKeyboard(){
        val imm =
            activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        ViewHelper.hideSystemUI(this.activity!!)
    }

    private fun enableSendEmailBtn(){
        if(send_email_btn_receipt != null){
            send_email_btn_receipt.isEnabled = true
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        LoggerHelper.writeToLog("$logFragment, Checked internet Connection. Connection ${networkInfo.isConnected}")
        return networkInfo != null && networkInfo.isConnected

    }
//    private fun updatePaymentDetailsApi() = launch(Dispatchers.IO) {
//        PIMMutationHelper.updatePaymentDetails(transactionId, tripNumber, vehicleId, mAWSAppSyncClient!!,paymentType, tripId)
//    }
    private fun sendEmail(email: String){
        if(vehicleId.isNotEmpty()&&
            tripId.isNotEmpty()&&
            paymentType.isNotEmpty()&&
            tripTotal > 0){
            updateCustomerEmail(email)
        } else {
            Log.i("Email Receipt", "Did not update cust email because one of the following was empty or zero. vehicle ID: $vehicleId, tripId: $tripId paymentType: $paymentType, tripTotal: $tripTotal")
        }
        TripDetails.receiptSentTo = email
        toConfirmation()
    }

    private fun updateCustomerEmail(email:String) = launch(Dispatchers.IO) {
       updateCustomerEmail(vehicleId, email,mAWSAppSyncClient!!,tripId)
    }

    private fun updateCustomerEmail(vehicleId: String, custEmail: String, appSyncClient: AWSAppSyncClient, tripId: String){
        LoggerHelper.writeToLog("$logFragment, entered email: $custEmail")
        val updatePaymentTypeInput = UpdateTripInput.builder().vehicleId(vehicleId).tripId(tripId).custEmail(custEmail).build()
        if(isOnline(context!!)){
            appSyncClient.mutate(UpdateTripMutation.builder().parameters(updatePaymentTypeInput).build())
                ?.enqueue(mutationCustomerEmailCallback )
        } else {
            Log.i("Email Receipt", "Not connected to internet")
        }
    }

    private val mutationCustomerEmailCallback = object : GraphQLCall.Callback<UpdateTripMutation.Data>() {
        override fun onResponse(response: Response<UpdateTripMutation.Data>) {
            Log.i("Email Receipt", "Meter Table Updated Customer Email}")
            val tripId = callBackViewModel.getTripId()
            val transactionId = callBackViewModel.getTransactionId()

            if (response.hasErrors()) {
                Log.i("Email Receipt", "Response from Aws had errors so did not send email")
                return
            }
            if (!response.hasErrors()) {
                if (response.data() != null)
                 {
                    launch(Dispatchers.IO) {
                        Log.i("Email Receipt", "updated custEmail successfully. Step 2: complete")
                        LoggerHelper.writeToLog("$logFragment, update custEmail successfully. Step 2: complete")
                        EmailHelper.sendEmail(tripId, paymentType, transactionId)
                    }
                }
            }
        }
        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the MeterTable: $e")
        }
    }

    private fun updatePaymentDetails(transactionId: String, tripNumber: Int, vehicleId: String, appSyncClient: AWSAppSyncClient, paymentMethod: String, tripId: String){
        Log.i("Email Receipts", "Trying to send the following to Payment AWS. TransactionId: $transactionId, tripNumber: $tripNumber, vehicleId: $vehicleId, paymentMethod: $paymentMethod, tripID: $tripId")
        val updatePaymentInput = SavePaymentDetailsInput.builder().paymentId(transactionId).tripNbr(tripNumber).vehicleId(vehicleId).paymentMethod(paymentMethod).tripId(tripId).build()

        appSyncClient.mutate(SavePaymentDetailsMutation.builder().parameters(updatePaymentInput).build())
        ?.enqueue(mutationCallbackPaymentDetails)
    }
    private val mutationCallbackPaymentDetails = object : GraphQLCall.Callback<SavePaymentDetailsMutation.Data>() {
        override fun onResponse(response: Response<SavePaymentDetailsMutation.Data>) {

            if(!response.hasErrors()){
                Log.i(
                    "Email Receipts",
                    "update payment Api successfully. Step 1: Complete")
                LoggerHelper.writeToLog("$logFragment, update payment Api successfully. Step 1: Complete")
                launch(Dispatchers.IO) {
                    sendEmail(email)
                }
            }else{
                Log.i(
                    "Email Receipts",
                    "update payment Api Unsuccessfully. Step 1: Incomplete")
                LoggerHelper.writeToLog("$logFragment, update payment unsuccessfully. Step 1: Incomplete")
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Email Receipts", "There was an issue updating payment api: $e")
            LoggerHelper.writeToLog("$logFragment, update payment unsuccessfully. Step 1: Incomplete. There was an issue updating payment api: $e")
        }
    }

    private fun startInactivityTimeout(){
        inactiveScreenTimer = object: CountDownTimer(60000, 1000) {
            // this is set to 1 min and will finish if a new trip is started.
            override fun onTick(millisUntilFinished: Long) {

            }
            override fun onFinish() {
                if (!resources.getBoolean(R.bool.isSquareBuildOn) &&
                        no_receipt_btn_receipt_email != null) {
                    LoggerHelper.writeToLog("$logFragment, Inactivity Timer finished")
                    no_receipt_btn_receipt_email.performClick()
                }
            }
        }.start()
    }
    //Navigation
    private fun toConfirmation() = launch(Dispatchers.Main.immediate){
        val action = ReceiptInformationEmailFragmentDirections
            .actionReceiptInformationEmailFragmentToConfirmationFragment(email_editText.text.toString(),tripTotal.toFloat(),"Email")
            .setEmailOrPhoneNumber(email_editText.text.toString())
            .setTripTotal(tripTotal.toFloat())
            .setReceiptType("Email")
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.receiptInformationEmailFragment){
            navController.navigate(action)
        }
    }

    private fun toThankYou(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.receiptInformationEmailFragment){
            navController.navigate(R.id.action_receiptInformationEmailFragment_to_interaction_complete_fragment)
        }
    }


    private fun backToEmailOrText(){
        val action = ReceiptInformationEmailFragmentDirections
            .actionReceiptInformationEmailFragmentToEmailOrTextFragment(
                tripTotal.toFloat(),paymentType)
            .setPaymentType(paymentType)
            .setTripTotal(tripTotal.toFloat())
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.receiptInformationEmailFragment){
            navController.navigate(action)
        }
    }


    override fun onDestroy() {
        inactiveScreenTimer?.cancel()
        Log.i("Email Receipt", "inactivity timer was canceled")
        closeSoftKeyboard()
        super.onDestroy()
    }
}