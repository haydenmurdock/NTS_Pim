package com.example.nts_pim.fragments_viewmodel.live_meter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.example.nts_pim.data.repository.SubscriptionWatcher
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.*
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit


class LiveMeterFragment: ScopedFragment(), KodeinAware, OnMapReadyCallback {
    //Kodein and ViewModel
    override val kodein by closestKodein()
    private val viewModelFactory: LiveMeterViewModelFactory by instance()
    private lateinit var viewModel: LiveMeterViewModel
    private lateinit var callbackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    // Local Variables
    private var vehicleId = ""
    private var tripId = ""
    var meterState = ""
    private var meterValue = ""
    private val decimalFormatter = DecimalFormat("####00.00")
    private var initialMeterValueSet = false
    private var currentFragment:Fragment? = null
    private var audioManager: AudioManager? = null
    private var requestMeterStateValueTimer:CountDownTimer? = null
    private val visible = View.VISIBLE
    private var currentTrip:CurrentTrip? = null
    private val logFragment = "Live Meter"
    private var timeOffSoundPlayed = false

    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var locationUpdateState = false
    private var dropOffLocationLatLong: LatLng? = null


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        // 3
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.live_meter_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentFragment = this
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callbackFactory = InjectorUtiles.provideCallBackModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(LiveMeterViewModel::class.java)
        callbackViewModel =
            ViewModelProviders.of(this, callbackFactory).get(CallBackViewModel::class.java)
        vehicleId = viewModel.getVehicleID()
         audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
         currentTrip = ModelPreferences(context!!)
            .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
        tripId = callbackViewModel.getTripId()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.activity!!)
        dropOffLocationLatLong = callbackViewModel.getTripDropOffLatLong()
        if(tripId == "" &&
                currentTrip != null){
            tripId = currentTrip!!.tripID
            getMeterOwedQuery(tripId)
            LoggerHelper.writeToLog(context!!, "$logFragment: had to query Meter Owed for $tripId")
        }
        updateTickerUI()
        checkCurrentTrip()
        updatePIMStatus()
        updateMeterFromTripReview()
        if(activity?.applicationContext?.resources!!.getBoolean(R.bool.isDevModeOn)){
            mapView = view.findViewById(R.id.map_view)
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this)
        }
        meterValue = "2.85"
        meterState = callbackViewModel.getMeterState().value.toString()
        if(meterState == "off"){
            callbackViewModel.addMeterState("ON")
        }

        if(meterState == MeterEnum.METER_TIME_OFF.state){
            if(audioManager!!.isMicrophoneMute && !timeOffSoundPlayed){
                playTimeOffSound()
            }
            toTripReview()
        }

        callbackViewModel.getMeterOwed().observe(this@LiveMeterFragment, Observer {meterOwedValue ->
            if (meterOwedValue > 0) {
                val df = decimalFormatter.format(meterOwedValue)
                meterValue = df.toString()
                tickerView.setText(meterValue, true)
                LoggerHelper.writeToLog(context!!, "$logFragment: Meter UI is displaying $meterValue")
                if (!live_meter_dollar_sign.isVisible &&
                    live_meter_dollar_sign != null
                ) {
                    live_meter_dollar_sign.visibility = visible
                }
                if (!tickerView.isVisible && tickerView != null) {
                    tickerView.visibility = visible
                }
            }
        })
        callbackViewModel.getMeterState().observe(this@LiveMeterFragment, Observer {AWSMeterState ->
            meterState = AWSMeterState
            if (meterState == MeterEnum.METER_TIME_OFF.state){
                Log.i("Live Meter", "Aws Meter state is Time_Off")
                if(audioManager!!.isMicrophoneMute && !timeOffSoundPlayed){
                    playTimeOffSound()
                }
                toTripReview()
            }
        })
        callbackViewModel.getTripStatus().observe(this@LiveMeterFragment, Observer {tripStatus ->
            //This is for account trips that have already been paid.
            if(tripStatus == VehicleStatusEnum.Trip_Closed.status
                || tripStatus == VehicleStatusEnum.TRIP_END.status){
                Log.i("Live Meter", "Trip Status: $tripStatus, pushed to email/text screen")
                toEmailText()
            }
        })
        refresh_button.setOnClickListener {
            if(refresh_progress_bar != null){
                refresh_button.setImageDrawable(null)
                refresh_button.isEnabled = false
                refresh_progress_bar.isVisible = true
                refresh_progress_bar.animate()
                getTripIdQuery(vehicleId)
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                Log.i("LOGGER", "on location results hit")
                lastLocation = p0.lastLocation
                val lastLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
               mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 12.0f))
            }
        }
        if(activity?.applicationContext?.resources!!.getBoolean(R.bool.isDevModeOn)){
                createLocationRequest()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null) {
            mMap = googleMap
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(false)
        mMap.setMyLocationEnabled(true)
        mMap.uiSettings.isZoomControlsEnabled = true
        val currentLocation = getLocation()
        mMap.addMarker(MarkerOptions().position(currentLocation).title("Current Location"))

         // ... get a map.
        // Add a thin red line from London to New York.
// Polyline line = map.addPolyline(new PolylineOptions()
//     .add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
//     .width(5)
//     .color(Color.RED));

        if(dropOffLocationLatLong != null){
            mMap.addMarker(MarkerOptions().position(dropOffLocationLatLong!!).title("Drop Off Location"))
            val polylineOptions = PolylineOptions().add(currentLocation, dropOffLocationLatLong).width(5.0f).color(
                Color.BLUE)
            mMap.addPolyline(polylineOptions)
        } else {
            dropOffLocationLatLong = LatLng(37.095169, -113.575974)
            mMap.addMarker(MarkerOptions().position(dropOffLocationLatLong!!).title("Testing Off Location"))
            val polylineOptions = PolylineOptions().add(currentLocation, dropOffLocationLatLong).width(5.0f).color(
                Color.BLUE)
            mMap.addPolyline(polylineOptions)
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12.0f))

    }

    private fun getLocation():LatLng {
        val locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProvider: String = LocationManager.NETWORK_PROVIDER
        if (ContextCompat.checkSelfPermission(
                this.activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                    this.activity!!,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastLocation = locationManager.getLastKnownLocation(locationProvider)
            if(lastLocation != null){
                Log.i("Narbuck", "CURRENT LOCATION WAS FOUND AND IS ${lastLocation.latitude}, ${lastLocation.longitude}")
                return LatLng(lastLocation.latitude, lastLocation.longitude)
            }
        }
        Log.i("Narbuck", "CURRENT LOCATION NOT FOUND")
        return LatLng(0.0,0.0)
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }


    private fun createLocationRequest() {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this.activity!!)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this.activity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun updateTickerUI() {
        if(tickerView != null){
            tickerView.setCharacterLists(TickerUtils.provideNumberList())
        }
    }
    private fun toTripReview()=launch(Dispatchers.Main.immediate) {
        if(meterValue != ""){
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if(navController.currentDestination?.id == R.id.live_meter_fragment){
                if(currentTrip != null){
                    callbackViewModel.updateCurrentTrip(true, tripId, "TIME_OFF", context!!)
                }
                val priceAsFloat = meterValue.toFloat()
                val action = LiveMeterFragmentDirections.toTripReviewFragment(priceAsFloat).setMeterOwedPrice(priceAsFloat)
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
            Log.i("Live Meter", "The Data is coming from Trip Review")
            val formattedArgs = decimalFormatter.format(args)
            val tripTotalToString = formattedArgs.toString()
            if(live_meter_dollar_sign != null){
                live_meter_dollar_sign.visibility = View.VISIBLE
            }
            initialMeterValueSet = true
            if (tickerView != null){
                tickerView.setText(tripTotalToString, true)
                LoggerHelper.writeToLog(context!!, "$logFragment: Meter UI is displaying $tripTotalToString")
            }
        }
    }
    private fun checkCurrentTrip(){
        if (resources.getBoolean(R.bool.isSquareBuildOn)){
            meterValue = "1.00"
            tickerView.setText("$$meterValue", true)
            if(tickerView.alpha.equals(0.0f)){
                tickerView.animate().alpha(1.0f).setDuration(2500).start()
            }
        }
    }

    private fun updatePIMStatus() = launch(Dispatchers.IO){
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
            if (response.data()?.trip != null && this@LiveMeterFragment.isAdded) {
                val startingMeterValue = response.data()!!.trip.owedPrice()
                val startingMeterState = response.data()!!.trip.meterState()
                if (startingMeterState != null) {
                        Log.i("Live Meter", "Meter is $meterState and is now $startingMeterState from meter query")
                    meterState = startingMeterState
                }
                if(startingMeterValue != null){
                    val df = decimalFormatter.format(startingMeterValue)
                    meterValue = df.toString()

                    runOnUiThread {
                        if (tickerView != null) {
                            LoggerHelper.writeToLog(context!!, "$logFragment: Meter UI is displaying $meterValue from trip Query")
                            tickerView.setText(meterValue, true)
                            if(!live_meter_dollar_sign.isVisible){
                                live_meter_dollar_sign.visibility = visible
                                 }
                            if (!tickerView.isVisible) {
                                tickerView.visibility = visible
                                 }
                            }
                        if (refresh_progress_bar != null){
                            refresh_progress_bar.isVisible = false
                            refresh_button.setImageDrawable(activity?.resources?.getDrawable(R.drawable.ic_refresh_arrows))
                            refresh_button.isEnabled = true
                        }
                    }
                }
                if (startingMeterState == MeterEnum.METER_TIME_OFF.state &&
                    startingMeterValue!! != 0.toDouble()){
                    Log.i("Live Meter", "Meter is $meterState and going to Trip Review")
                    toTripReview()
                }
                initialMeterValueSet = true
                reSyncComplete()
            }
        }
        override fun onFailure(e: ApolloException) {
                Log.e("ERROR", e.toString())
            }
        }
    private fun getTripIdQuery(vehicleId: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context!!)
        }
        mAWSAppSyncClient?.query(GetStatusQuery.builder().vehicleId(vehicleId).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(getTripIdCallBack)
    }

    private var getTripIdCallBack = object: GraphQLCall.Callback<GetStatusQuery.Data>(){
        override fun onResponse(response: Response<GetStatusQuery.Data>) {
            if (response.data()?.status?.tripId() != null && this@LiveMeterFragment.isAdded) {
                val awsTripId = response.data()?.status?.tripId() ?: ""
                if(!awsTripId.isNullOrBlank()) {
                    tripId = awsTripId
                    Log.i("LOGGER", "TRIP ID CALLBACK, TRIP ID $tripId. getting meter query. restarting subscription watcher")
                    SubscriptionWatcher.updateSubscriptionWatcher(tripId, activity!!, null)
                    getMeterOwedQuery(tripId)
                    }
                }
            }
        override fun onFailure(e: ApolloException) {
            Log.e("ERROR", e.toString())
        }
    }

    private fun reSyncComplete() = launch(Dispatchers.Main.immediate) {
        callbackViewModel.updateCurrentTrip(true, tripId, meterState, context!!)
        callbackViewModel.reSyncComplete()
        LoggerHelper.writeToLog(context!!, "$logFragment: Resync Complete")
    }
    private fun requestMeterStateValue(){
          requestMeterStateValueTimer = object:CountDownTimer(10000,1000){
            override fun onTick(millisUntilFinished: Long) {
            }
            override fun onFinish() {

            }
        }.start()
    }

    private fun playTimeOffSound(){
        SoundHelper.turnOnSound(context!!)
        val mediaPlayer = MediaPlayer.create(context, R.raw.time_off_test_sound)
        mediaPlayer.setOnCompletionListener {
            LoggerHelper.writeToLog(context!!, "$logFragment: Time Off Sound Played")
            mediaPlayer.release()
        }
        mediaPlayer.start()
        timeOffSoundPlayed = true
    }

    override fun onResume() {
        mapView.onResume()
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
        requestMeterStateValue()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        requestMeterStateValueTimer?.cancel()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        callbackViewModel.getMeterOwed().removeObservers(this)
        callbackViewModel.getMeterState().removeObservers(this)
        callbackViewModel.getTripStatus().removeObservers(this)
        requestMeterStateValueTimer?.cancel()
        Log.i("Observer", "Live Meter observer removed.")
        super.onDestroy()
    }

    override fun onLowMemory() {
        mapView.onLowMemory()
        super.onLowMemory()
    }

}

