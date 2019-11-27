package com.example.nts_pim.fragments_viewmodel.trip_review



import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.enums.PaymentTypeEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.Square_Service.SquareService
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




class TripReviewFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: TripReviewViewModelFactory by instance()
    private lateinit var viewModel: TripReviewViewModel
    private lateinit var callbackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null

    var vehicleId = ""
    var tripID = ""
    var tripTotal = 0.0
    var cardInfo = ""

    private val decimalFormatter = DecimalFormat("####00.00")
    private val tripTotalDF = DecimalFormat("####00.00")
    private val tripTotalDFUnderTen = DecimalFormat("###0.00")
    private var removeWaitScreenTimer: CountDownTimer? = null
    private var inactiveScreenTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
         return inflater.inflate(R.layout.trip_review_screen,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = InjectorUtiles.provideCallBackModelFactory()

        callbackViewModel = ViewModelProviders.of(this, factory)
            .get(CallBackViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(TripReviewViewModel::class.java)

        mAWSAppSyncClient = ClientFactory.getInstance(context)

        vehicleId = viewModel.getVehicleId()
        tripID = callbackViewModel.getTripId()

        PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.PAYMENT_SCREEN.status, mAWSAppSyncClient!!)
        cash_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    cash_textView.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                    dollar_sign_imageView.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_currency_icon_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    cash_btn.isEnabled = false
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })

        debit_credit_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    credit_textView.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                    credit_card_imageView.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_credit_card_grey
                    ))
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

        updateTripInfo()
        //To Tip Screen
        debit_credit_btn.setOnClickListener {
            ViewHelper.disableButton(debit_credit_btn)
            // checks that trip total was updated before sending to square
            if (tripTotal > 1.00){
                PIMMutationHelper.updatePaymentType(vehicleId,
                    PaymentTypeEnum.CARD.paymentType,
                    mAWSAppSyncClient!!,
                    tripID)
                toTipScreen() }
            else {
                Toast.makeText(context,
                    "A card payment requires the trip to be over $1.00",
                    Toast.LENGTH_LONG).show()
                debit_credit_btn.isEnabled = true
            }
        }
        //To the Email or Text Screen

        cash_btn.setOnClickListener {
            launch{
                val action = TripReviewFragmentDirections.toEmailOrText(tripTotal.toFloat(), "CASH")
                    .setTripTotal(tripTotal.toFloat())
                val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                if (navController.currentDestination?.id == (R.id.trip_review_fragment)) {
                    navController.navigate(action)
                }
            }.invokeOnCompletion {
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
        }

        callbackViewModel.getIsTransactionComplete().observe(this, Observer {
            if(it){
                PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.SQUARE_PAYMENT_COMPLETE.status, mAWSAppSyncClient!!)
                val tripTotalFloat = tripTotal.toFloat()
                val actionComplete = TripReviewFragmentDirections.toEmailOrText(tripTotalFloat,"CARD").setTripTotal(tripTotalFloat)
                val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                navController.navigate(actionComplete)
            }
        })

        callbackViewModel.getMeterState().observe(this, Observer {
            val meterState = it
            if (meterState == MeterEnum.METER_ON.state) {
                SquareService().pressCancelButtons()
                val tripTotalFloat = tripTotal.toFloat()
                val action = TripReviewFragmentDirections.backToLiveMeter(tripTotalFloat).setMeterTotal(tripTotalFloat)
                val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                navController.navigate(action)
            }
        })
    }

    private fun updateTripInfo() {
        if (resources.getBoolean(R.bool.isDevModeOn)){
                val dollar = 1
                val formattedArgs = decimalFormatter.format(dollar)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                trip_total_for_tip_text_view.text = "$$tripTotalToString" }
        else {
                val args = arguments?.getFloat("meterOwedPrice")
                if(args != null){
                    if(args < 10){
                        val formattedArgs = tripTotalDFUnderTen.format(args)
                        tripTotal = formattedArgs.toDouble()
                        val tripTotalToString = formattedArgs.toString()
                        trip_total_for_tip_text_view.text = "$$tripTotalToString"
                    } else {
                        val formattedArgs = decimalFormatter.format(args)
                        tripTotal = formattedArgs.toDouble()
                        val tripTotalToString = formattedArgs.toString()
                        trip_total_for_tip_text_view.text = "$$tripTotalToString"
                    }
                }

        }
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
       //If the microphone is muted, the square connection is still taking place.
        if (!audioManager.isMicrophoneMute){
            debit_credit_btn.visibility = View.INVISIBLE
            cash_btn.visibility = View.INVISIBLE
            credit_card_imageView.visibility = View.INVISIBLE
            dollar_sign_imageView.visibility = View.INVISIBLE
            credit_textView.visibility = View.INVISIBLE
            cash_textView.visibility = View.INVISIBLE
            pleaseWaitScrollView.visibility = View.VISIBLE
            if (progressBar3 != null){
                progressBar3.animate()
            }
            startRemoveWaitScreenTimer()
        }
    }
    private fun startRemoveWaitScreenTimer(){
         removeWaitScreenTimer = object : CountDownTimer(30000, 1000){
             //we will show this screen for 20 seconds just in case something makes square take a while to come up
             val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            override fun onTick(millisUntilFinished: Long) {
                 if(audioManager.isMicrophoneMute){
                     onFinish()
                     removeWaitScreenTimer?.cancel()
                 }
            }

            override fun onFinish() {
                changeBackToRegularUI(debit_credit_btn)
                changeBackToRegularUI(cash_btn)
                changeBackToRegularUI(credit_card_imageView)
                changeBackToRegularUI(dollar_sign_imageView)
                changeBackToRegularUI(credit_textView)
                changeBackToRegularUI(cash_textView)
                if (pleaseWaitScrollView != null){
                    pleaseWaitScrollView.visibility = View.INVISIBLE
                }
                if (progressBar3 != null){
                    progressBar3.visibility = View.GONE
                    progressBar3.clearAnimation()
                }
            }
        }.start()
    }
    private fun toTipScreen() {
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if(navController.currentDestination?.id == (R.id.trip_review_fragment)){
            val action = TripReviewFragmentDirections.toTipScreen(tripTotal.toFloat(),0.toFloat()).setTipScreenTripTotal(tripTotal.toFloat())
            navController.navigate(action)
        }
    }


    private fun startInactivityTimer(){
       inactiveScreenTimer = object: CountDownTimer(60000, 1000) {
            //1 min inactivity timer
            override fun onTick(millisUntilFinished: Long) {
                val hasNewTripStarted = callbackViewModel.hasNewTripStarted().value
                if(hasNewTripStarted!!){
                    inactiveScreenTimer?.onFinish()
                }
            }

            override fun onFinish() {
                if (!resources.getBoolean(R.bool.isSquareBuildOn)){
                    val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                    if(navController.currentDestination?.id == (R.id.trip_review_fragment)){
                        PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.PAYMENT_ERROR.status, mAWSAppSyncClient!!)
                        navController.navigate(R.id.activityTimeOutToInteractionComplete)
                    }
                }
            }
        }.start()
    }
    private fun changeBackToRegularUI(view: View?){
        if (view != null){
            view.visibility = View.VISIBLE
        }
    }
    override fun onPause() {
        super.onPause()
        inactiveScreenTimer?.cancel()
        callbackViewModel.getTripStatus().removeObservers(this)
    }

    override fun onStop() {
        super.onStop()
        inactiveScreenTimer?.cancel()
        callbackViewModel.getTripStatus().removeObservers(this)
    }

    override fun onResume() {
        super.onResume()
        callbackViewModel.getTripStatus().observe(this, Observer {tripStatus ->
            if(tripStatus == VehicleStatusEnum.TRIP_END.status||
                tripStatus == VehicleStatusEnum.Trip_Closed.status)
            {
                startInactivityTimer()
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        callbackViewModel.getIsTransactionComplete().removeObservers(this)
        Log.i("Observer", "is transaction complete observer removed")
        callbackViewModel.getMeterState().removeObservers(this)
        Log.i("Observer", "Meter state observer removed")
        callbackViewModel.getTripStatus().removeObservers(this)

    }
}
