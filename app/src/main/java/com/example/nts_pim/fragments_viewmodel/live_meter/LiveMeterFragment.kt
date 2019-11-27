package com.example.nts_pim.fragments_viewmodel.live_meter

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
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
import com.amazonaws.amplify.generated.graphql.GetTripQuery
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.utilities.enums.MeterEnum
import com.robinhood.ticker.TickerUtils
import java.text.DecimalFormat
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
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
    private var airportFee:Double? = null
    private var tollFee:Double? = null
    private val decimalFormatter = DecimalFormat("####00.00")
    private var slidePopUpDownTimer: CountDownTimer? = null

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
        updateTickerUI()
        checkSquareDev()
        callbackViewModel.getMeterOwed().observe(this, Observer {
            if (meterState == MeterEnum.METER_ON.state) {
                var df = decimalFormatter.format(it)
                meterValue = df.toString()
                tickerView.setText(meterValue, true)
            }
        })
        callbackViewModel.getMeterState().observe(this, Observer {awsMeterState ->
            meterState = awsMeterState
            if (meterState == MeterEnum.METER_TIME_OFF.state)
                playTimeOffSound()
            toTripReview()
        })
        callbackViewModel.getTripStatus().observe(this, Observer {
            //This is for account trips that have already been paid.
        })
        updateMeterFromTripReview()
        PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.METER_SCREEN.status,mAWSAppSyncClient!!)
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

    private fun updateMeterFromTripReview(){
        val args = arguments?.getFloat("meterTotal")
        val checkAgainst = 0.0.toFloat()
//        We have this turned off for initial build just in case it causes problems in the live version.
//        val popupView = view?.findViewById<View>(R.id.live_meter_popup_scrollView) as ScrollView
//        val duration = 500
        if(args != null && args != checkAgainst){
//            ViewHelper.viewSlideUp(popupView, duration)
//            slidePopUpDownTimer(popupView)
            val formattedArgs = decimalFormatter.format(args)
            val tripTotalToString = formattedArgs.toString()
            live_meter_dollar_sign.visibility = View.VISIBLE
            tickerView.setText(tripTotalToString, true)
        }
    }
    private fun checkSquareDev(){
        if (resources.getBoolean(R.bool.isSquareBuildOn)){
            meterValue = "1.00"
            tickerView.setText("$$meterValue", true)
            if(tickerView.alpha.equals(0.0f)){
                tickerView.animate().alpha(1.0f).setDuration(2500).start()
            }
        } else {
            live_meter_next_screen_button.isEnabled = false
            live_meter_next_screen_button.isVisible = false
            val currentTrip = ModelPreferences(context!!)
                .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
            if(currentTrip != null && currentTrip.tripID != ""){
                getMeterOwedQuery(currentTrip.tripID)
            }
        }
    }

    private fun getMeterOwedQuery(tripId: String) {
            if (mAWSAppSyncClient == null) {
                mAWSAppSyncClient = ClientFactory.getInstance(context!!)
            }
           mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
                ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                ?.enqueue(getTripQueryCallBack)
        }
        private var getTripQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>(){
            override fun onResponse(response: Response<GetTripQuery.Data>) {
                if (response.data()!!.trip.owedPrice() != null) {
                    val initialMeterValue = response.data()!!.trip.owedPrice()
                    //These are values for when the meter screen will have a trip breakdown and I think are causing a memory leak issue
//                     airportFee = response.data()?.trip?.airportFee()
//                     tollFee = response.data()?.trip?.toll()
                     meterState = response.data()!!.trip.meterState() as String
                        val df = decimalFormatter.format(initialMeterValue)
                        meterValue = df.toString()
                        tripID = response.data()!!.trip.tripId() as String
                        reSyncComplete()
                        if (meterState == MeterEnum.METER_TIME_OFF.state &&
                                initialMeterValue != 0.0){
                            //we are still on an active trip, but trip's meter has been set to Time_off
                            toTripReview()
                        } else {
                            //We are still on an active trip, but meter is still ON
                            runOnUiThread {
                                if(tickerView != null){
                                    tickerView.setText(meterValue, true)
                                    if(tickerView.alpha.equals(0.0f)){
                                        tickerView.animate().alpha(1.0f).setDuration(2500).start()
                                    }
                                    if(live_meter_dollar_sign.alpha.equals(0.0f)){
                                        live_meter_dollar_sign.animate().alpha(1.0f).setDuration(2500).start()
                                    }
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
//        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        callbackViewModel.getMeterOwed().removeObservers(this)
        Log.i("Observer", "Meter owed observer removed")

        callbackViewModel.getMeterState().removeObservers(this)
        Log.i("Observer", "Meter state observer removed")
    }
}

