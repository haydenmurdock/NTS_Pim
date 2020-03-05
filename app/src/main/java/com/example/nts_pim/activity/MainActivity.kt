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
import com.amazonaws.amplify.generated.graphql.*
import com.example.nts_pim.NetworkReceiver
import com.example.nts_pim.utilities.logging_service.LoggerHelper



open class MainActivity : AppCompatActivity(), CoroutineScope, KodeinAware {
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
    private var subscriptionWatcherDoPimPayment: AppSyncSubscriptionCall<OnDoPimPaymentSubscription.Data>? =
        null
    private var subscriptionWatcherUpdateVehTripStatus: AppSyncSubscriptionCall<OnUpdateVehTripStatusSubscription.Data>? =
        null
    private var loggingTimer: CountDownTimer? = null
    private var vehicleSubscriptionTimer: CountDownTimer? = null
    private val logFragment = "Background Activity"
    private var mNetworkReceiver: NetworkReceiver? = null
    private var watchingTripId = ""

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
        viewModel.watchSetUpComplete().observe(this, Observer { successfulSetup ->
            if (successfulSetup) {
                mSuccessfulSetup = successfulSetup
                vehicleId = viewModel.getVehicleID()
                internetConnection = isOnline(this)
                if (internetConnection && vehicleSubscriptionComplete) {
//                    startOnStatusUpdateSubscription(vehicleId)
                    subscribeToUpdateVehTripStatus(vehicleId)
                    LoggerHelper.writeToLog(this.applicationContext, "$logFragment, vehicle setup complete, started subscription to vehicle")
                    Log.i("Results", "Tried to subscribe to $vehicleId because setup is complete")
                } else {
                    recheckInternetConnection(this)
                }
            }
        })
        callbackViewModel.getTripHasEnded().observe(this, Observer { tripEnded ->
            if (tripEnded) {
                meterStateQueryComplete = false
                LoggerHelper.writeToLog(this.applicationContext, "$logFragment, Trip ended and meterStateQueryComplete is set to false")
            }
        })
        forceSpeaker()
        setUpBluetooth()
        checkNavBar()
        registerNetworkReceiver()
        callbackViewModel.getReSyncStatus().observe(this, Observer { reSync ->
            if (reSync) {
                resync = reSync
                vehicleId = viewModel.getVehicleID()
                internetConnection = isOnline(this)
                if (!internetConnection) {
                    recheckInternetConnection(this)
                }
                val currentTrip = ModelPreferences(applicationContext)
                    .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
                lastTrip = currentTrip
                if (!vehicleSubscriptionComplete) {
                    Log.i("Results", "Vehicle subscription was started from re-sync")
//                    startOnStatusUpdateSubscription(vehicleId)
                    subscribeToUpdateVehTripStatus(vehicleId)

                }
                if (currentTrip != null && currentTrip.tripID != "" && internetConnection) {
                    Log.i("Results", "Trip Id was updated on Main Activity from re-sync")
                    tripId = currentTrip.tripID
                }
            }
        })

        callbackViewModel.hasNewTripStarted().observe(this, Observer { hasTripStarted ->
            if (hasTripStarted) {
                val currentTripId = callbackViewModel.getTripId()
                val navController = findNavController(this, R.id.nav_host_fragment)
                callbackViewModel.clearAllTripValues()
                if (navController.currentDestination?.id != R.id.welcome_fragment ||
                    navController.currentDestination?.id == R.id.taxi_number_fragment && !resync) {
                    Log.i(
                        "TripStart",
                        "This needs to work now. Old trip id: $tripId, new trip id: $currentTripId"
                    )
                    LoggerHelper.writeToLog(this.applicationContext, "${logFragment}, New trip was started by the driver while the pim trip was not finished")
                    getMeterOwedQuery(currentTripId)
                    navController.navigate(R.id.action_global_taxi_number_fragment)
                } else {
                    Log.i(
                        "TripStart",
                        "current Nav destination is ${navController.currentDestination.toString()}"
                    )
                    LoggerHelper.writeToLog(this.applicationContext, "${logFragment}, Driver tried to start new trip, but the Pim was on Welcome/taxi number screen")
                }
            }
        })
        if (loggingTimer == null) {
            startTimerToSendLogsToAWS(vehicleId, this@MainActivity)
        }

        callbackViewModel.getIsPimOnline().observe(this, Observer { onlineStatus ->
            if(!onlineStatus){
                vehicleSubscriptionComplete = false
            }
            if(onlineStatus && !vehicleSubscriptionComplete && mSuccessfulSetup){
                callbackViewModel.reSyncTrip()
                watchingTripId = ""
                getMeterOwedQuery(tripId)
  //              startSubscriptionTripUpdate(tripId)
                subscribeToUpdateVehTripStatus(vehicleId)
                watchingTripId = ""
            }
        })
    }

    //Coroutine to insert Trip Status
    private fun insertTripStatus(string: String) = launch {
        callbackViewModel.addTripStatus(string)
    }

    //Coroutine to insert Meter State
    private fun insertMeterState(string: String) = launch {
        callbackViewModel.addMeterState(string)
    }
    private fun subscribeToUpdateVehTripStatus(vehicleId: String){
       if(!vehicleSubscriptionComplete && isOnline(applicationContext)) {
            vehicleSubscriptionComplete = true
            val subscription =
                OnUpdateVehTripStatusSubscription.builder().vehicleId(vehicleId).build()
            if (subscriptionWatcherUpdateVehTripStatus == null) {
                subscriptionWatcherUpdateVehTripStatus = mAWSAppSyncClient?.subscribe(subscription)
                subscriptionWatcherUpdateVehTripStatus?.execute(subscribeToUpdateVehCallback)
            } else {
                subscriptionWatcherUpdateVehTripStatus?.cancel()
                subscriptionWatcherUpdateVehTripStatus = mAWSAppSyncClient?.subscribe(subscription)
                subscriptionWatcherUpdateVehTripStatus?.execute(subscribeToUpdateVehCallback)
            }
        }
    }
    private var subscribeToUpdateVehCallback = object: AppSyncSubscriptionCall.Callback<OnUpdateVehTripStatusSubscription.Data>{
        override fun onResponse(response: Response<OnUpdateVehTripStatusSubscription.Data>) {
            val pimStatus = response.data()?.onUpdateVehTripStatus()?.pimStatus()
            val tripStatus = response.data()?.onUpdateVehTripStatus()?.tripStatus()
            val awsTripId = response.data()?.onUpdateVehTripStatus()?.tripId()


            if (pimStatus == "_") {
                // sends back requested current pim status
                sendPIMStatus()
            }
            if (!tripStatus.isNullOrBlank()) {
                insertTripStatus(tripStatus)
            }
            if (!awsTripId.isNullOrBlank()) {
                insertTripId(awsTripId)
               subscribeToDoPIMPayment(awsTripId)
                tripId = awsTripId
            }

        }

        override fun onFailure(e: ApolloException) {
            vehicleSubscriptionComplete = false
            subscriptionWatcherUpdateVehTripStatus?.cancel()
            subscriptionWatcherUpdateVehTripStatus = null

        }

        override fun onCompleted() {

        }
    }

    private fun subscribeToDoPIMPayment(tripId: String){
        if (tripId != watchingTripId && tripId != ""){
            val subscription = OnDoPimPaymentSubscription.builder().tripId(tripId).build()
            if(subscriptionWatcherDoPimPayment == null) {
                subscriptionWatcherDoPimPayment = mAWSAppSyncClient?.subscribe(subscription)
                subscriptionWatcherDoPimPayment?.execute(doPimPaymentCallback)
            } else {
                subscriptionWatcherDoPimPayment?.cancel()
                subscriptionWatcherDoPimPayment = mAWSAppSyncClient?.subscribe(subscription)
                subscriptionWatcherDoPimPayment?.execute(doPimPaymentCallback)
            }
        }
    }
    private var doPimPaymentCallback = object: AppSyncSubscriptionCall.Callback<OnDoPimPaymentSubscription.Data>{
        override fun onResponse(response: Response<OnDoPimPaymentSubscription.Data>) {
            val meterState = response.data()?.onDoPimPayment()?.meterState()
            val pimNoReceipt = response.data()?.onDoPimPayment()?.pimNoReceipt()
            val pimPaymentAmount = response.data()?.onDoPimPayment()?.pimPayAmt()
            val dropoffLocation = response.data()?.onDoPimPayment()?.dropoffLocation()
            val tripEndTime = response.data()?.onDoPimPayment()?.tripEndTime()
            val owedPriceForMeter = response.data()?.onDoPimPayment()?.owedPrice()
            val tripNumber = response.data()?.onDoPimPayment()?.tripNbr()
            val transactionId = response.data()?.onDoPimPayment()?.pimTransId()
            val pimPaidAmount = response.data()?.onDoPimPayment()?.pimPaidAmt()
            val driverId = response.data()?.onDoPimPayment()?.driverId()

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
            if(driverId != null){
                insertDriverId(driverId)
            }
        }

        override fun onFailure(e: ApolloException) {
            subscriptionWatcherDoPimPayment?.cancel()
            subscriptionWatcherDoPimPayment = null
        }
        override fun onCompleted() {

        }
    }

    private fun getMeterOwedQuery(tripId: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(this@MainActivity.applicationContext)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_FIRST)
            ?.enqueue(getTripQueryCallBack)
    }
    private var getTripQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (response.data() != null &&
                !response.hasErrors()
            ) {
                val meterOwed = response.data()?.trip?.owedPrice()
                val meterValue = response.data()?.trip?.meterState()
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

    private fun insertDriverId(driverId: Int)= launch{
        callbackViewModel.setDriverId(driverId)
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
           // playTestSound()
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, e.toString())
        }
    }
    private fun startTimerToSendLogsToAWS(vehicleId: String, context: Context){
        Log.i("LOGGER", "Log Timer Started")
            loggingTimer = object: CountDownTimer(180000, 30000){
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished/1000
                Log.i("LOGGER", "Log Timer: $seconds until logs are sent to AWS")
            }
            override fun onFinish() {
                Log.i("LOGGER", "Log Timer: onFinish")
                launch(Dispatchers.IO) {
                    LoggerHelper.sendLogToAWS(vehicleId, context)
                }
                startTimerToSendLogsToAWS(vehicleId, context)
            }
        }.start()
    }
    private fun stopLogTimer(){
        if(loggingTimer != null){
            Log.i("LOGGER", "Log Timer Stopped")
            loggingTimer!!.cancel()
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
                LoggerHelper.writeToLog(this@MainActivity, "${logFragment}, pim played start up sound")
               mediaPlayer.start()
             }
        }
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }
    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun registerNetworkReceiver(){
        //This is for the Internet Receiver.
        mNetworkReceiver = NetworkReceiver()
        registerReceiver(mNetworkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }
    private fun recheckInternetConnection(context: Context){
        object: CountDownTimer(5000, 1000){
        override fun onTick(millisUntilFinished: Long) {
            internetConnection = isOnline(context)
        }
        override fun onFinish() {
            if(!internetConnection){
                LoggerHelper.writeToLog(applicationContext, "$logFragment, recheck internet connection timer finished. internet was not connected. retrying in 5 seconds")
                recheckInternetConnection(this@MainActivity)
                // this is for a resync of trip
            } else if (resync) {
                val currentTrip = ModelPreferences(applicationContext)
                    .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
                LoggerHelper.writeToLog(applicationContext, "$logFragment, recheck internet connection timer finished. Internet is connected. Trying to start subscription on ${vehicleId} due to resync.")
  //              startOnStatusUpdateSubscription(vehicleId)
                subscribeToUpdateVehTripStatus(vehicleId)
                if (currentTrip != null && currentTrip.tripID != "" && internetConnection){
                    LoggerHelper.writeToLog(applicationContext, "$logFragment, recheck internet connection timer finished.Internet is connected. Trying to start subscription on ${currentTrip.tripID} due to resync.")
                    //
//                    startSubscriptionTripUpdate(currentTrip.tripID)
                    resync = false
                }
            } else {
                // start subscription since the internet is connected.
                LoggerHelper.writeToLog(applicationContext, "$logFragment, recheck internet connection timer finished. Internet is connected. Trying to start subscription on ${vehicleId}.")
  //              startOnStatusUpdateSubscription(vehicleId)
                subscribeToUpdateVehTripStatus(vehicleId)
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
        LoggerHelper.writeToLog(this@MainActivity, "${logFragment}, back button on nav bar pressed")
        Log.i("Back Button", "Back button was pressed")
    }

    private fun setUpBluetooth(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            LoggerHelper.writeToLog(this@MainActivity, "${logFragment}, bluetooth was off, turned on programmatically")
            mBluetoothAdapter.enable()
        } else {
            LoggerHelper.writeToLog(this@MainActivity, "${logFragment}, bluetooth was on during start up")
        }
    }

    override fun onDestroy() {
        Log.i("SubscriptionWatcher", "Subscription watcher canceled for $vehicleId")
        subscriptionWatcherUpdateVehTripStatus?.cancel()
        LoggerHelper.writeToLog(this@MainActivity, "${logFragment}, Subscription watcher canceled for $vehicleId, onDestroy hit")
        viewModel.isSquareAuthorized().removeObservers(this)
        callbackViewModel.getTripHasEnded().removeObservers(this)
        callbackViewModel.getIsPimOnline().removeObservers(this)
        unregisterReceiver(mNetworkReceiver)
        LoggerHelper.writeToLog(this, "$logFragment, MainActivity onDestroy hit")
        stopLogTimer()
        vehicleSubscriptionTimer?.cancel()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if(mSuccessfulSetup){
            ViewHelper.hideSystemUI(this)
        }
        LoggerHelper.writeToLog(this, "$logFragment, MainActivity onPause hit")
    }

    override fun onResume() {
        super.onResume()
        if(mSuccessfulSetup){
            ViewHelper.hideSystemUI(this)
        }
    }
}

