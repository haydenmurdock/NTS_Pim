package com.example.nts_pim.fragments_viewmodel.welcome

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetTripQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.keyboards.PhoneKeyboard
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.power_cycle.PowerAccessibilityService
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.checkout.CheckoutParameters
import com.squareup.sdk.reader.checkout.CurrencyCode
import com.squareup.sdk.reader.checkout.Money
import kotlinx.android.synthetic.main.welcome_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.timerTask


class WelcomeFragment : ScopedFragment(), KodeinAware {

    // Kodein and ViewModel/Factory
    override val kodein by closestKodein()

    private val viewModelFactory: WelcomeViewModelFactory by instance()
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private lateinit var viewModel: WelcomeViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var vehicleId = ""
    private var cabNumber = ""
    private var buttonCount = 0
    private var isPasswordEntered = false
    private var meterState = ""
    private val password = "1234"
    private val fullBrightness = 255
    private val dimBrightness = 10
    private var isOnActiveTrip = false
    private val currentFragmentId = R.id.welcome_fragment

    private val batteryCheckTimer = object : CountDownTimer(1800000, 1000) {
        //Every 30 minutes  we are doing a battery check.
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            if (!resources.getBoolean(R.bool.isSquareBuildOn)){
                batteryStatusCheck()
            }
        }
    }
    private val dimScreenTimer = object : CountDownTimer(120000, 1000) {
        //after a status has changed to End we run a 2 min timer to dimScreen
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            if (!resources.getBoolean(R.bool.isSquareBuildOn)){
                changeScreenBrightness(dimBrightness)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.welcome_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAWSAppSyncClient = ClientFactory.getInstance(context)

        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()

        val keyboardViewModelFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
        ViewHelper.hideSystemUI(activity!!)

        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(WelcomeViewModel::class.java)

        keyboardViewModel = ViewModelProviders.of(this, keyboardViewModelFactory)
            .get(SettingsKeyboardViewModel::class.java)
        // checks for animation and navigates to next Screen
        setUpKeyboard()
        checkSquareMode()
        ViewHelper.checkForNotifications(activity!!)
        if (checkToSeeIfOnTrip().first != null) {
            isOnActiveTrip = checkToSeeIfOnTrip().first!!
        }
        viewModel.isSetupComplete()
        vehicleId = viewModel.getVehicleId()
        updateVehicleInfoUI()
        updatePimStatus()
        batteryCheckTimer.start()
        dimScreenTimer.start()
        if (!VehicleTripArrayHolder.squareHasBeenSetUp) {
            startSquareFlow()
        }
        //This is encase you have to restart the app during a trip or a trip
        keyboardViewModel.isPhoneKeyboardUp().observe(this, androidx.lifecycle.Observer {
            if (it) {
                isPasswordEntered = true
            }
            if (!it && isPasswordEntered) {
                checkPassword()
                isPasswordEntered = false
            }
        })
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dimScreenTimer.cancel()
                        changeScreenBrightness(fullBrightness)
                        dimScreenTimer.start()
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })
        open_vehicle_settings_button.setOnClickListener {
            dimScreenTimer.cancel()
            changeScreenBrightness(fullBrightness)
            dimScreenTimer.start()
            buttonCount += 1
            if (buttonCount > 2) {
                showToastMessage("$buttonCount", 1000)
            }
            if (buttonCount == 5) {
                open_vehicle_settings_button.animate().alpha(1.00f).setDuration(500)
                keyboardViewModel.phoneKeyboardIsUp()
                ViewHelper.viewSlideUp(password_scroll_view, 500)
            }
            if (buttonCount >= 6) {
                if (buttonCount % 2 == 0) {
                    open_vehicle_settings_button.animate().alpha(0.0f).setDuration(500)
                    ViewHelper.viewSlideDown(password_scroll_view, 500)
                    keyboardViewModel.bothKeyboardsDown()
                } else {
                    open_vehicle_settings_button.animate().alpha(1.00f).setDuration(500)
                    keyboardViewModel.phoneKeyboardIsUp()
                    ViewHelper.viewSlideUp(password_scroll_view, 500)
                }
            }
        }

        callBackViewModel.getMeterState().observe(this, androidx.lifecycle.Observer {
            val meterState = it
            if (meterState == MeterEnum.METER_ON.state) {
                changeScreenBrightness(fullBrightness)
                Log.i("Welcome Screen","Meter State subscription changed to ON and Leaving Welcome Screen")
                checkAnimation()
            }
        })

        callBackViewModel.hasNewTripStarted().observe(this, androidx.lifecycle.Observer { tripStarted ->
            if(tripStarted){
                val tripId = callBackViewModel.getTripId()
                getMeterStatusQuery(tripId)
            }

        })
        tripIsCurrentlyRunning(isOnActiveTrip)
        welcome_screen_next_screen_button.setOnClickListener {
            toLiveMeterScreen()
        }
    }
    private fun checkAnimation() {
        val animationIsOn = resources.getBoolean(R.bool.animationIsOn)
        if (animationIsOn) {
            if(welcome_text_view != null){
                welcome_text_view.animate().alpha(0.0f).setDuration(2500).withEndAction{

                    thank_you_text_view.animate().alpha(1f).setDuration(2500).withEndAction {

                        thank_you_text_view.animate().alpha(0.0f).setDuration(2500)
                            .withEndAction{
                                toTaxiNumber()
                            }
                    }
                }
            } else {
                toNextScreen()
            }
        }
}
    private fun tripIsCurrentlyRunning(isTripActive: Boolean){
        if (!isTripActive){
            return
        }
        val tripIdMeterQuery = checkToSeeIfOnTrip().second
        changeScreenBrightness(fullBrightness)
        getMeterStatusQueryForTripSync(tripIdMeterQuery)
    }
    private fun updateUI(companyName: String) {
        thank_you_text_view.text = "Thank you for choosing $companyName"
        password_editText.isClickable = false
        password_editText.isFocusable = false
    }

    private fun toNextScreen() {
        Timer().schedule(timerTask {
            toTaxiNumber()
        }, 5000)
    }
    private fun markPimStartTime(){
        val time = LocalDateTime.now()
        TripDetails.tripStartTime = time
    }
    private fun checkPassword() {
        if (password_editText.text.toString() == password) {
            changeScreenBrightness(255)
            Navigation.findNavController(view!!).navigate(R.id.toVehicleSettingsDetail)
        }
    }

    private fun showToastMessage(text: String, duration: Int) {
        val toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT)
        toast.show()
        val handler = Handler()
        handler.postDelayed( { toast.cancel() }, duration.toLong())
    }

    private fun setUpKeyboard() {
        password_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        password_editText.setTextIsSelectable(false)

        val phoneKeyboard = passwordPhoneKeyboard as PhoneKeyboard

        val ic = password_editText.onCreateInputConnection(EditorInfo())
        phoneKeyboard.setInputConnection(ic)
    }

    private fun batteryStatusCheck() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let({ ifilter ->
            context?.registerReceiver(null, ifilter)
        })
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
        val batteryPowerPermission = callBackViewModel.batteryPowerStatePermission()
        if (!isCharging && !batteryPowerPermission) {
            //We change the audio to avoid a static sound when turing off
            val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = (AudioManager.MODE_NORMAL)
            val intent = Intent(activity, PowerAccessibilityService::class.java)
            intent.action = "com.claren.tablet_control.shutdown"
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            activity!!.startService(intent)
        }

        if (isCharging) {
            batteryCheckTimer.cancel()
            batteryCheckTimer.start()
        }
    }

    private fun changeScreenBrightness(screenBrightness: Int) {
        Settings.System.putInt(
            context?.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )  //this will set the manual mode (set the automatic mode off)
        Settings.System.putInt(
            context?.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            screenBrightness
        )  //this will set the brightness to maximum (255)

        //refreshes the screen
        val br =
            Settings.System.getInt(context?.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        val lp = activity?.window?.attributes
        lp?.screenBrightness = br.toFloat() / 255
        activity?.window?.attributes = lp
    }

    private fun checkSquareMode(){
        if (resources.getBoolean(R.bool.isSquareBuildOn)){
            changeScreenBrightness(fullBrightness)
        } else {
            welcome_screen_next_screen_button.isVisible = false
            welcome_screen_next_screen_button.isEnabled = false
        }
    }

    private fun checkToSeeIfOnTrip(): Pair<Boolean?, String>{
        if (!resources.getBoolean(R.bool.isDevModeOn)){
            val currentTrip = ModelPreferences(context!!)
                .getObject(SharedPrefEnum.CURRENT_TRIP.key,
                    CurrentTrip::class.java)
            if (currentTrip == null){
                return Pair(false, "")
            }
            if (currentTrip.isActive!!){
                    callBackViewModel.reSyncTrip()
                    return Pair(currentTrip.isActive, currentTrip.tripID)
            }
        }
        return Pair(false, "")
    }

    private fun getMeterStatusQueryForTripSync(tripId: String) {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(getTripMeterQueryCallBack)
    }

    private var getTripMeterQueryCallBack = object : GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (response.data() != null) {
               meterState = response.data()?.trip?.meterState().toString()
                Log.i("Welcome Screen","Meter State Query is for trip Sync worked and Leaving Welcome Screen")
                if (meterState == MeterEnum.METER_ON.state || meterState == MeterEnum.METER_TIME_OFF.state) {
                    toLiveMeterScreen()
                }
            }
        }
        override fun onFailure(e: ApolloException) {
            println("Failure")
        }
    }

    private fun getMeterStatusQuery(tripId: String) {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(getTripMeterQuery)
    }

    private var getTripMeterQuery = object : GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (response.data() != null) {
               val meterState = response.data()?.trip?.meterState().toString()
                if (meterState == MeterEnum.METER_ON.state) {
                    Log.i("Welcome Screen","Meter State Query is ON and Leaving Welcome Screen")
                    launch(Dispatchers.Main.immediate) {
                        changeScreenBrightness(fullBrightness)
                        checkAnimation()
                    }
                }
            }
        }
        override fun onFailure(e: ApolloException) {
            println("Failure")
        }
    }

    private fun startSquareFlow(){
            SoundHelper.turnOffSound(context!!)
            val p = 100.00
            val checkOutTotal = p.toLong()
            val amountMoney = Money(checkOutTotal, CurrencyCode.current())
            val parametersBuilder = CheckoutParameters.newBuilder(amountMoney)
            parametersBuilder.skipReceipt(false)
            val checkoutManager = ReaderSdk.checkoutManager()
            checkoutManager.startCheckoutActivity(context!!, parametersBuilder.build())
    }

    private fun updateVehicleInfoUI(){
        val vehicleSettings = viewModel.getvehicleSettings()
        if (vehicleSettings != null) {
            val companyName = vehicleSettings.companyName
            cabNumber = vehicleSettings.cabNumber
            updateUI(companyName)
        }
    }

    private fun updatePimStatus(){
        PIMMutationHelper.updatePIMStatus(
            vehicleId,
            PIMStatusEnum.START_SCREEN.status,
            mAWSAppSyncClient!!
        )
    }

    // Navigation
    private fun toLiveMeterScreen() = launch(Dispatchers.Main.immediate) {
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.welcomeFragmentToLiveMeterSceen)
        }
    }
    private fun toTaxiNumber() {
        markPimStartTime()
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.toTaxiNumber)
        }
    }

    override fun onStop() {
        super.onStop()
        keyboardViewModel.bothKeyboardsDown()
        batteryCheckTimer.cancel()
        dimScreenTimer.cancel()
    }

    override fun onPause() {
        super.onPause()
        ViewHelper.hideSystemUI(activity!!)
        batteryCheckTimer.cancel()
        dimScreenTimer.cancel()
    }

    override fun onResume() {
        super.onResume()
        ViewHelper.hideSystemUI(activity!!)
        vehicleId = viewModel.getVehicleId()
        batteryCheckTimer.cancel()
        batteryCheckTimer.start()
        dimScreenTimer.cancel()
        dimScreenTimer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        callBackViewModel.getMeterState().removeObservers(this)
    }
}
