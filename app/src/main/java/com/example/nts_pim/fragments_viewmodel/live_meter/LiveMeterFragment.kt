package com.example.nts_pim.fragments_viewmodel.live_meter

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlinx.android.synthetic.main.live_meter_screen.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import androidx.lifecycle.Observer
import com.amazonaws.amplify.generated.graphql.GetStatusQuery
import com.amazonaws.amplify.generated.graphql.GetTripQuery
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.robinhood.ticker.TickerUtils
import java.text.DecimalFormat
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.*
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LiveMeterFragment: ScopedFragment(), KodeinAware {

    //Kodein and ViewModel
    override val kodein by closestKodein()
    private val viewModelFactory: LiveMeterViewModelFactory by instance()
    private lateinit var viewModel: LiveMeterViewModel
    private lateinit var callbackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    // Local Variables
    private var vehicleId = ""
    private var tripID = ""
    var meterState = ""
    private var meterValue = ""
    private val decimalFormatter = DecimalFormat("####00.00")
    private var initialMeterValueSet = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.live_meter_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callbackFactory = InjectorUtiles.provideCallBackModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(LiveMeterViewModel::class.java)
        callbackViewModel =
            ViewModelProviders.of(this, callbackFactory).get(CallBackViewModel::class.java)
        vehicleId = viewModel.getVehicleID()
        tripID = callbackViewModel.getTripId()
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentTrip = ModelPreferences(context!!)
            .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
        updateTickerUI()
        checkCurrentTrip()
        updatePIMStatus()
        updateMeterFromTripReview()
        if(tripID != ""){
            getMeterOwedQuery(tripID)
        } else if(currentTrip != null && currentTrip.tripID != ""){
            getMeterOwedQuery(currentTrip.tripID)
        } else {
            getTripId(vehicleId)
        }
       callbackViewModel.getMeterOwed().observe(this, Observer {
            if (meterState == MeterEnum.METER_ON.state) {
                val df = decimalFormatter.format(it)
                meterValue = df.toString()
                tickerView.setText(meterValue, true)
                if (tickerView.alpha != 1.0f) {
                    tickerView.animate().alpha(1.0f).setDuration(2500).start()
                    live_meter_dollar_sign.animate().alpha(1.0f).setDuration(2500)
                        .start()
                }

            }
        })
        callbackViewModel.getMeterState().observe(this, Observer {AWSMeterState ->
            meterState = AWSMeterState
            if (meterState == MeterEnum.METER_TIME_OFF.state){
                if(audioManager.isMicrophoneMute){
                    playTimeOffSound()
                }
                toTripReview()
            }
        })
        callbackViewModel.getTripStatus().observe(this, Observer {tripStatus ->
            //This is for account trips that have already been paid.
            if(tripStatus == VehicleStatusEnum.Trip_Closed.status
                || tripStatus == VehicleStatusEnum.TRIP_END.status){
                toEmailText()
            }
        })


        live_meter_next_screen_button.setOnClickListener {
            toTripReview()
        }
    }

    private fun updateTickerUI() {
        tickerView.setCharacterLists(TickerUtils.provideNumberList())
    }
    private fun toTripReview() {
        if(meterValue != ""){
            val priceAsFloat = meterValue.toFloat()
            val action = LiveMeterFragmentDirections.toTripReviewFragment(priceAsFloat).setMeterOwedPrice(priceAsFloat)
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if(navController.currentDestination?.id == R.id.live_meter_fragment){
                navController.navigate(action)
            }
        }
    }

    private fun toEmailText(){
        if(meterValue != ""){
            val priceAsFloat = meterValue.toFloat()
            val paymentType = PaymentTypeEnum.CARD.paymentType
            val action = LiveMeterFragmentDirections.toEmailorTextFromLiveMeter(priceAsFloat, paymentType).setPaymentType(paymentType).setTripTotal(priceAsFloat)
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if(navController.currentDestination?.id == R.id.live_meter_fragment){
                navController.navigate(action)
            }
        }
    }

    private fun updateMeterFromTripReview(){
        val args = arguments?.getFloat("meterTotal")
        val checkAgainst = 00.00.toFloat()
        if(args != null && args != checkAgainst){
            val formattedArgs = decimalFormatter.format(args)
            val tripTotalToString = formattedArgs.toString()
            live_meter_dollar_sign.visibility = View.VISIBLE
            initialMeterValueSet = true
            tickerView.setText(tripTotalToString, true)
        }
    }
    private fun checkCurrentTrip(){
        if (resources.getBoolean(R.bool.isSquareBuildOn)){
            meterValue = "1.00"
            tickerView.setText("$$meterValue", true)
            if(tickerView.alpha.equals(0.0f)){
                tickerView.animate().alpha(1.0f).setDuration(2500).start()
            }
        } else {
            live_meter_next_screen_button.isEnabled = false
            live_meter_next_screen_button.isVisible = false
        }
    }

    private fun updatePIMStatus() = launch{
        PIMMutationHelper.updatePIMStatus(vehicleId,
            PIMStatusEnum.METER_SCREEN.status,
            mAWSAppSyncClient!!)
    }

    private fun getMeterOwedQuery(tripId: String) = launch(Dispatchers.IO){
            if (mAWSAppSyncClient == null) {
                mAWSAppSyncClient = ClientFactory.getInstance(context!!)
            }
           mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
                ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                ?.enqueue(getTripQueryCallBack)
        }
        private var getTripQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>(){
            override fun onResponse(response: Response<GetTripQuery.Data>) {
                if (response.data()?.trip != null) {
                    val initialMeterValue = response.data()!!.trip.owedPrice()
                    val startingMeterValue = response.data()!!.trip.meterState()

                    if (startingMeterValue != null) {
                    meterState = startingMeterValue
                    }

                    if (meterState == MeterEnum.METER_TIME_OFF.state &&
                            initialMeterValue!! != 0.toDouble()){
                        toTripReview()
                    }
                    val df = decimalFormatter.format(initialMeterValue)
                    meterValue = df.toString()
                    reSyncComplete()
                    runOnUiThread {
                        initialMeterValueSet = true
                        if (tickerView != null) {
                            tickerView.setText(meterValue, true)
                            if (tickerView.alpha.equals(0.0f)) {
                                tickerView.animate().alpha(1.0f).setDuration(2500).start()
                                live_meter_dollar_sign.animate().alpha(1.0f).setDuration(2500)
                                    .start()
                            }
                        }
                    }
                }
            }
            override fun onFailure(e: ApolloException) {
                Log.e("ERROR", e.toString())
            }
        }

    private fun getTripId(vehicleId: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context!!)
        }
        mAWSAppSyncClient?.query(GetStatusQuery.builder().vehicleId(vehicleId).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(vehicleIdCallBack)
    }
    private var vehicleIdCallBack = object: GraphQLCall.Callback<GetStatusQuery.Data>(){
        override fun onResponse(response: Response<GetStatusQuery.Data>) {
            if (response.data() != null) {
                if (response.data()!!.status.tripId() != null || response.data()!!.status.tripId() != "") {
                    val tripId = response.data()!!.status.tripId()
                    if(tripId != null){
                        tripID = tripId
                        launch(Dispatchers.Main.immediate) {
                            callbackViewModel.addTripId(tripId,context!!)
                        }
                        launch(Dispatchers.IO) {
                            getMeterOwedQuery(tripID)
                        }
                    }
                }
            }
         }
        override fun onFailure(e: ApolloException) {
            Log.e("ERROR", e.toString())
        }
    }

    private fun reSyncComplete() = launch(Dispatchers.Main.immediate) {
        callbackViewModel.updateCurrentTrip(true, tripID, context!!)
        callbackViewModel.reSyncComplete()
    }

    private fun playTimeOffSound(){
        SoundHelper.turnOnSound(context!!)
        val mediaPlayer = MediaPlayer.create(context, R.raw.time_off_test_sound)
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
       mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        callbackViewModel.getMeterOwed().removeObservers(this)
        callbackViewModel.getMeterState().removeObservers(this)
        callbackViewModel.getTripStatus().removeObservers(this)
        Log.i("Observer", "Live Meter observer removed.")
    }
}

