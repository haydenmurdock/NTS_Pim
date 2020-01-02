package com.example.nts_pim.fragments_viewmodel.receipt_information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import android.content.Context
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.UpdateTripMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.simple_email_helper.EmailHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.receipt_information_email.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import type.UpdateTripInput
import java.util.*


class ReceiptInformationEmailFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: ReceiptInformationViewModelFactory by instance()
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
            }
        })
        getTripDetails()
        startInactivityTimeout()
        email_editText.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
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
            launch {
                updatePaymentDetailsApi().invokeOnCompletion {
                    sendEmail(email)
                }
            }
        }
        back_btn_email_receipt.setOnClickListener {
            backToEmailOrText()
        }
        no_receipt_btn_receipt_email.setOnClickListener {
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
    private fun updatePaymentDetailsApi() = launch(Dispatchers.IO) {
        PIMMutationHelper.updatePaymentDetails(transactionId, tripNumber, vehicleId, mAWSAppSyncClient!!)
    }
    private fun sendEmail(email: String){
        if(vehicleId.isNotEmpty()&&
            tripId.isNotEmpty()&&
            paymentType.isNotEmpty()&&
            tripTotal > 0){
            updateCustomerEmail(email)
        }
        TripDetails.receiptSentTo = email
        toConfirmation()
    }

    private fun updateCustomerEmail(email:String) = launch(Dispatchers.IO) {
       updateCustomerEmail(vehicleId, email,mAWSAppSyncClient!!,tripId)
    }

    private fun updateCustomerEmail(vehicleId: String, custEmail: String, appSyncClient: AWSAppSyncClient, tripId: String){
        val updatePaymentTypeInput = UpdateTripInput.builder().vehicleId(vehicleId).tripId(tripId).custEmail(custEmail).build()

        appSyncClient.mutate(UpdateTripMutation.builder().parameters(updatePaymentTypeInput).build())
            ?.enqueue(mutationCustomerEmailCallback )

    }

    private val mutationCustomerEmailCallback = object : GraphQLCall.Callback<UpdateTripMutation.Data>() {
        override fun onResponse(response: Response<UpdateTripMutation.Data>) {
            Log.i("Results", "Meter Table Updated ${response.data()}")
            if(!response.hasErrors()){
                launch(Dispatchers.IO){
                    EmailHelper.sendEmail(tripId, paymentType, transactionId)
                }
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the MeterTable: $e")
        }
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
                if (!resources.getBoolean(R.bool.isSquareBuildOn) &&
                        no_receipt_btn_receipt_email != null) {
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
        super.onDestroy()
        closeSoftKeyboard()
    }
}