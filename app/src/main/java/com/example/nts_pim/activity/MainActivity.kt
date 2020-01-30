package com.example.nts_pim.activity

import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Context
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.amazonaws.amplify.generated.graphql.GetTripQuery
import com.amazonaws.amplify.generated.graphql.OnStatusUpdateSubscription
import com.amazonaws.amplify.generated.graphql.OnTripUpdateSubscription
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.UnlockScreenLock
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import kotlin.coroutines.CoroutineContext
import androidx.navigation.Navigation.findNavController


class MainActivity : AppCompatActivity(), CoroutineScope, KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance()
    private lateinit var viewModel: VehicleSetupViewModel
    private var vehicleId = ""
    private var vehicleSubscriptionComplete = false
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
    private lateinit var callbackViewModel: CallBackViewModel
    private var internetConnection = false
    private var resync = false
    private var meterStateQueryComplete = false
    private var mSuccessfulSetup = false
    private var tripId = ""
    private var lastTrip: CurrentTrip? = null
    private var subscriptionWatcherVehicle: AppSyncSubscriptionCall<OnStatusUpdateSubscription.Data>? = null
    private var subscriptionWatcherTrip: AppSyncSubscriptionCall<OnTripUpdateSubscription.Data>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //This is for screen wake
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        UnlockScreenLock()
        mJob = Job()
       findNavController(this, R.id.nav_host_fragment)
        mAWSAppSyncClient = ClientFactory.getInstance(applicationContext)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callbackViewModel = ViewModelProviders.of(this, factory)
            .get(CallBackViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        val intentFilter = IntentFilter()
        viewModel.watchSetUpComplete().observe(this, Observer {successfulSetup ->
            if (successfulSetup){
                mSuccessfulSetup = successfulSetup
                vehicleId = viewModel.getVehicleID()
                internetConnection = isOnline(this)
                if (internetConnection && vehicleSubscriptionComplete){
                    startOnStatusUpdateSubscription(vehicleId)
                    Log.i("Results","Tried to subscribe to vehicle because setup is complete")
                } else{
                    recheckInternetConnection(this)
                }
            }
        })
        callbackViewModel.getTripHasEnded().observe(this, Observer {tripEnded ->
            if(tripEnded){
                meterStateQueryComplete = false
            }
        })
        forceSpeaker()
        setUpBluetooth()
        checkNavBar()
        callbackViewModel.getReSyncStatus().observe(this, Observer { reSync ->
            if (reSync){
                resync = reSync
                vehicleId = viewModel.getVehicleID()
                internetConnection = isOnline(this)
                if (!internetConnection){
                    recheckInternetConnection(this)
                }
                val currentTrip = ModelPreferences(applicationContext)
                    .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
                lastTrip = currentTrip
                if(!vehicleSubscriptionComplete){
                    startOnStatusUpdateSubscription(vehicleId)
                    Log.i("Results","Vehicle subscription was started from resync")
                }
                if (currentTrip != null && currentTrip.tripID != "" && internetConnection){
                    Log.i("Results","Trip Id was updated on Main Activity from REsync")
                    tripId = currentTrip.tripID
                }
            }
        })

        callbackViewModel.hasNewTripStarted().observe(this, Observer{hasTripStarted ->
            if(hasTripStarted){
                val currentTripId = callbackViewModel.getTripId()
                val navController = findNavController(this, R.id.nav_host_fragment)
                callbackViewModel.clearAllTripValues()
                if (navController.currentDestination?.id != R.id.welcome_fragment && !resync){
                    Log.i("TripStart", "This needs to work now. Old trip id: $tripId, new trip id: $currentTripId")
                        getMeterOwedQuery(currentTripId)
                        navController.navigate(R.id.action_global_taxi_number_fragment)
                } else {
                    Log.i("TripStart", "current Nav destination is ${navController.currentDestination.toString()}")
                }
            }})
    }
    //App Sync subscription to vehicleTable
    private fun startOnStatusUpdateSubscription(vehicleID: String) = launch(Dispatchers.IO) {
        if(!vehicleSubscriptionComplete){
            vehicleSubscriptionComplete = true
            val subscription = OnStatusUpdateSubscription.builder().vehicleId(vehicleID).build()
            subscriptionWatcherVehicle = mAWSAppSyncClient?.subscribe(subscription)
            subscriptionWatcherVehicle?.execute(vehicleStatusCallback)
            Log.i("Results", "Watching $vehicleID for information")
            Log.i("SubscriptionWatcher", "Subscription watcher started for $vehicleID")
        }
    }
    //App Sync Callback for vehicle subscription
    private var vehicleStatusCallback = object : AppSyncSubscriptionCall.Callback<OnStatusUpdateSubscription.Data> {
        override fun onResponse(response: Response<OnStatusUpdateSubscription.Data>) {
            Log.i("Results", "Successful subscription callback for vehicle status - ${response.data()}")
            val tripStatus = response.data()?.onStatusUpdate()?.tripStatus() as String
            val awsTripId = response.data()?.onStatusUpdate()?.tripId()
            val pimStatus = response.data()?.onStatusUpdate()?.pimStatus() as String
            if(pimStatus == "_"){
                // sends back requested current pim status
                sendPIMStatus()
            }
            if (!tripStatus.isNullOrBlank()) {
                insertTripStatus(tripStatus)
            }
            if(!awsTripId.isNullOrBlank()) {
                insertTripId(awsTripId)
                if(awsTripId != tripId){
                    startSubscriptionTripUpdate(awsTripId)
                    tripId = awsTripId
                }
            }
        }
            override fun onFailure(e: ApolloException) {
            Log.i("Error", "Error in callback for tripStatusUpdate: $e.")
        }
        override fun onCompleted() {
            Log.i("Results", "Subscription completed")
        }
    }
    //Coroutine to insert Trip Status
    private fun insertTripStatus(string: String) = launch {
        callbackViewModel.addTripStatus(string)
    }
    //Coroutine to insert Meter State
    private fun insertMeterState(string: String) = launch {
        callbackViewModel.addMeterState(string)
    }
    // App Sync subscription for update On Trip
    private fun startSubscriptionTripUpdate(tripId: String) {
        val subscription = OnTripUpdateSubscription.builder().tripId(tripId).build()
        subscriptionWatcherTrip = mAWSAppSyncClient?.subscribe(subscription)
        subscriptionWatcherTrip?.execute(tripUpdateCallback)
        Log.i("Results", "Watching $tripId for information")
    }

    // App Sync CallBack for trip Update
    private var tripUpdateCallback = object : AppSyncSubscriptionCall.Callback<OnTripUpdateSubscription.Data> {
        override fun onResponse(response: Response<OnTripUpdateSubscription.Data>) {
            Log.i("TripSubscriptionCallBack", "Successful subscription callback for Trip Update - ${response.data()}")
            val tripNumber = response.data()?.onTripUpdate()?.tripNbr()
            val meterState = response.data()?.onTripUpdate()?.meterState()
            val owedPriceForMeter = response.data()?.onTripUpdate()?.owedPrice()
            val transactionId = response.data()?.onTripUpdate()?.pimTransId()
            val pimPaymentAmount = response.data()?.onTripUpdate()?.pimPayAmt()
            val pimPaidAmount = response.data()?.onTripUpdate()?.pimPaidAmt()
            val pimNoReceipt = response.data()?.onTripUpdate()?.pimNoReceipt()

            if (tripNumber != null){
                insertTripNumber(tripNumber)
                //we get the transactionId when we get the trip number.
                if (!transactionId.isNullOrBlank()){
                    insertTransactionID(transactionId)
                }
            }

            if (meterState != null) {
               insertMeterState(meterState)
            }
            if (owedPriceForMeter != null && owedPriceForMeter != 0.0){
                insertMeterValue(owedPriceForMeter)
            }
            if(pimPaymentAmount != null){
                insertPimPayAmount(pimPaymentAmount)
            }
            if(pimPaidAmount != null){
                insertPimPaidAmount(pimPaidAmount)
            }
            if(pimNoReceipt != null
                && pimNoReceipt.trim() == "Y"){
                insertPimNoReceipt(true)
            }
        }
        override fun onFailure(e: ApolloException) {
            Log.i("Error", "Error in callback for tripUpdate: $e.")
        }
        override fun onCompleted() {
            Log.i("Results", "Subscription completed")
        }
    }

    private fun getMeterOwedQuery(tripId: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(this@MainActivity.applicationContext)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(getTripQueryCallBack)
    }

    private var getTripQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (response.data() != null &&
                !response.hasErrors()
            ) {
                val meterOwed = response.data()?.trip?.owedPrice()
                val meterValue = response.data()?.trip?.meterState()
                val tripId = response.data()!!.trip.tripId()
                if(tripId != null){
                    startSubscriptionTripUpdate(tripId)
                }
                Log.i("PleaseWait", "meterOwed from Meter query = $meterOwed")
                Log.i("PLeaseWait", "meterValue from Meter query = $meterValue")
                if (meterOwed != null
                    && meterOwed > 0
                ) {
                    insertMeterValue(meterOwed)
                }
                if (!meterValue.isNullOrBlank()) {
                    insertMeterState(meterValue)
                }
            }
        }
        override fun onFailure(e: ApolloException) {
        }
    }
    //Coroutine to insert Meter Value
    private fun insertMeterValue(double: Double) = launch {
        callbackViewModel.addMeterValue(double)
    }
    private fun insertTripId(tripId: String) = launch {
        callbackViewModel.addTripId(tripId, applicationContext)
    }
    private fun insertTripNumber(tripNumber: Int) = launch {
        callbackViewModel.addTripNumber(tripNumber)
    }
    private fun insertTransactionID(transactionId: String) = launch {
        callbackViewModel.setTransactionId(transactionId)
    }
    private fun insertPimPayAmount(pimPayAmount:Double) = launch{
        callbackViewModel.setPimPayAmount(pimPayAmount)
    }

    private fun insertPimPaidAmount(pimPaidAmount: Double) = launch {
        callbackViewModel.setPimPaidAmount(pimPaidAmount)
    }
    private fun insertPimNoReceipt(boolean: Boolean) = launch{
        callbackViewModel.pimDoesNotNeedToDoReceipt(boolean)
    }

    private fun sendPIMStatus() = launch{
        val pimStatus = VehicleTripArrayHolder.getInternalPIMStatus()
        if(pimStatus != ""){
            PIMMutationHelper.updatePIMStatus(vehicleId, pimStatus, mAWSAppSyncClient!!)
        } else {
            PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.ERROR_UPDATING.status, mAWSAppSyncClient!!)
        }
    }

    override fun onWindowFocusChanged(hasFocus:Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && mSuccessfulSetup) {
           ViewHelper.hideSystemUI(this)
            }
        }
    private fun forceSpeaker() {
        try {
//          playTestSound()
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, e.toString())
        }
    }
    //we might have to move this for the square call function
    private fun playTestSound(){
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = (AudioManager.STREAM_MUSIC)
        SoundHelper.turnOnSound(this)
        val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.startup)
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                build()
            }).build()
        }
        val request = audioManager.requestAudioFocus(focusRequest)
        when(request) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
               mediaPlayer.start()
             }
        }
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.stop()
            it.release()
        }
    }
    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun recheckInternetConnection(context: Context){
        object: CountDownTimer(5000, 1000){
        override fun onTick(millisUntilFinished: Long) {
            internetConnection = isOnline(context)
        }
        override fun onFinish() {
            if(!internetConnection){
                recheckInternetConnection(this@MainActivity)
                // this is for a resync of trip
            } else if (resync) {
                val currentTrip = ModelPreferences(applicationContext)
                    .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
                startOnStatusUpdateSubscription(vehicleId)
                if (currentTrip != null && currentTrip.tripID != "" && internetConnection){
                    startSubscriptionTripUpdate(currentTrip.tripID)
                    resync = false
                }
            } else {
                // start subscription since the internet is connected.
                startOnStatusUpdateSubscription(vehicleId)
            }
         }
        }.start()
    }
    private fun checkNavBar(){
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener((object: View.OnSystemUiVisibilityChangeListener{
            override fun onSystemUiVisibilityChange(visibility: Int) {
                if(mSuccessfulSetup){
                    ViewHelper.hideSystemUI(this@MainActivity)
                }
            }
        }))
    }
    override fun onBackPressed() {
        Log.i("Back Button", "Back button was pressed")
    }

    private fun setUpBluetooth(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
        }
    }

    override fun onDestroy() {
        Log.i("SubscriptionWatcher", "Subscription watcher canceled for $vehicleId")
        subscriptionWatcherVehicle?.cancel()
        viewModel.isSquareAuthorized().removeObservers(this)
        callbackViewModel.getTripHasEnded().removeObservers(this)
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if(mSuccessfulSetup){
            ViewHelper.hideSystemUI(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if(mSuccessfulSetup){
            ViewHelper.hideSystemUI(this)
        }
    }
}

