package com.example.nts_pim.fragments_viewmodel.welcome

import android.content.Intent
import android.content.IntentFilter
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
import com.amazonaws.amplify.generated.graphql.UpdatePimStatusMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.AppVersion
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.keyboards.PhoneKeyboard
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.checkout.CheckoutParameters
import com.squareup.sdk.reader.checkout.CurrencyCode
import com.squareup.sdk.reader.checkout.Money
import kotlinx.android.synthetic.main.welcome_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import type.UpdatePimStatusInput
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
    private var tripId = ""
    private var cabNumber = ""
    private var buttonCount = 0
    private var isPasswordEntered = false
    private val password = "1234"
    private val fullBrightness = 255
    private val dimBrightness = 10
    private var isOnActiveTrip = false
    private var lastTrip:Pair<Boolean?, String> = Pair(false, "")
    private val currentFragmentId = R.id.welcome_fragment
    private var pimIsReadyToTakeTrip = false
    private val logFragment = "Welcome Screen"

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
    private val restartAppTimer = object: CountDownTimer(30000, 1000){
        override fun onTick(millisUntilFinished: Long) {
            val seconds = millisUntilFinished/1000
            showToastMessage("Restarting:  $seconds", 1000)
        }
        override fun onFinish() {
            val action =  "com.claren.tablet_control.shutdown"
            val p = "com.claren.tablet_control"
            val intent = Intent()
            intent.action = action
            intent.`package` = p
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            activity?.sendBroadcast(intent)
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
        ViewHelper.checkForNotifications(activity!!)
        lastTrip = checkToSeeIfOnTrip()
        if (lastTrip.first != null) {
            isOnActiveTrip = lastTrip.first!!
            tripId = lastTrip.second
        }
        checkSquareMode()
        viewModel.isSetupComplete()
        vehicleId = viewModel.getVehicleId()
        updateVehicleInfoUI()
        batteryCheckTimer.start()
        dimScreenTimer.start()
        checkAppBuildVersion()
        //This is encase you have to restart the app during a trip or a trip
        keyboardViewModel.isPhoneKeyboardUp().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer {
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
                open_vehicle_settings_button.animate().alpha(1.00f).duration = 500
                keyboardViewModel.phoneKeyboardIsUp()
                ViewHelper.viewSlideUp(password_scroll_view, 500)
            }
            if (buttonCount >= 6) {
                if (buttonCount % 2 == 0) {
                    open_vehicle_settings_button.animate().alpha(0.0f).duration = 500
                    ViewHelper.viewSlideDown(password_scroll_view, 500)
                    password_editText.setText("")
                    keyboardViewModel.bothKeyboardsDown()
                } else {
                    open_vehicle_settings_button.animate().alpha(1.00f).duration = 500
                    keyboardViewModel.phoneKeyboardIsUp()
                    ViewHelper.viewSlideUp(password_scroll_view, 500)
                }
            }
        }
        tripIsCurrentlyRunning(isOnActiveTrip)
        callBackViewModel.hasNewTripStarted().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer { tripStarted ->
            if(tripStarted){
                val newTripId = callBackViewModel.getTripId()
                if(newTripId != lastTrip.second &&
                    newTripId != "") {
                            Log.i("Welcome Screen", "new trip has started for trip Id: $newTripId")
                        }
                }
            })
        callBackViewModel.getTripStatus().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it == VehicleStatusEnum.TRIP_PICKED_UP.status){
                changeScreenBrightness(fullBrightness)
                LoggerHelper.writeToLog(context!!, "$logFragment received Trip_Pick_Up_Status. Starting Animation")
                checkAnimation()
            }
        })

        welcome_screen_next_screen_button.setOnClickListener {
//            callBackViewModel.setPimPayAmount(1.25)
//            toCashOrCard()
        }
    }
    private fun checkAnimation() {
        val animationIsOn = resources.getBoolean(R.bool.animationIsOn)
        if (animationIsOn) {
            if (welcome_text_view != null) {
                welcome_text_view.animate().alpha(0.0f).setDuration(2500).withEndAction {
                    if (thank_you_text_view != null){
                        thank_you_text_view.animate().alpha(1f).setDuration(2500).withEndAction {
                            if (thank_you_text_view != null){
                                thank_you_text_view.animate().alpha(0.0f).setDuration(2500)
                                    .withEndAction {
                                        toTaxiNumber()
                                    }
                            }
                        }
                    }
                }
            }
        }else {
            toNextScreen()
        }
    }
    private fun tripIsCurrentlyRunning(isTripActive: Boolean){
        if (!isTripActive){
            LoggerHelper.writeToLog(context!!, "$logFragment. Trip Check: Last saved trip is not active")
            return
        }
        val tripId = lastTrip.second
        changeScreenBrightness(fullBrightness)
        Log.i("Welcome Screen", "Trip Active: $isActive Trip Id: $tripId")
        LoggerHelper.writeToLog(context!!, "$logFragment Trip Check: Last saved trip is active. To Meter Screen")
        toLiveMeterScreen()
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
        LoggerHelper.writeToLog(context!!, "$logFragment: PimMarkStartTime")

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
            LoggerHelper.writeToLog(context!!, "$logFragment: Battery Check: is charging: $isCharging, sending request for shutdown")
            val action =  "com.claren.tablet_control.shutdown"
            val p = "com.claren.tablet_control"
            val intent = Intent()
            intent.action = action
            intent.`package` = p
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            activity?.sendBroadcast(intent)
        }

        if (isCharging) {
            LoggerHelper.writeToLog(context!!, "$logFragment: Battery Check: is charging: $isCharging")
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
        )
        //this will set the brightness to maximum (255)
        //refreshes the screen
        val br =
            Settings.System.getInt(context?.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        val lp = activity?.window?.attributes
        lp?.screenBrightness = br.toFloat() / 255
        activity?.window?.attributes = lp
        LoggerHelper.writeToLog(context!!, "$logFragment: Screen set to Max Brightness")
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
            val lastTrip = ModelPreferences(context!!)
                .getObject(SharedPrefEnum.CURRENT_TRIP.key,
                    CurrentTrip::class.java)
            Log.i("Welcome Screen", "Last Trip Id${lastTrip?.tripID}. Is trip active: ${lastTrip?.isActive}, Last Trip MeterState: ${lastTrip?.lastMeterState}")
            if (lastTrip == null){
                return Pair(false, "")
            }
            if (lastTrip.isActive!!){
                    callBackViewModel.reSyncTrip()
                    return Pair(lastTrip.isActive, lastTrip.tripID)
            }
            if(!lastTrip.isActive!!){
                return Pair(false, lastTrip.tripID)
            }
        }
        return Pair(false, "")
    }

    private fun updatePIMStatus(vehicleId: String, pimStatusUpdate: String, appSyncClient: AWSAppSyncClient) = launch(Dispatchers.IO){
        val updatePimStatusInput = UpdatePimStatusInput.builder()?.vehicleId(vehicleId)?.pimStatus(pimStatusUpdate)?.build()

        appSyncClient.mutate(UpdatePimStatusMutation.builder().parameters(updatePimStatusInput).build())
            ?.enqueue(mutationCallbackOnPIM)
        // We are setting the internal PIM Status
        VehicleTripArrayHolder.updateInternalPIMStatus(pimStatusUpdate)
    }

    private val mutationCallbackOnPIM = object : GraphQLCall.Callback<UpdatePimStatusMutation.Data>() {
        override fun onResponse(response: Response<UpdatePimStatusMutation.Data>) {
            Log.i("Results", "PIM Status Updated ${response.data()}")
            if(!response.hasErrors()){
                pimIsReadyToTakeTrip = true
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the pimStatus: $e")
        }
    }
    private fun startSquareFlow(){
            LoggerHelper.writeToLog(context!!, "$logFragment, Started Square Checkout flow")
            SoundHelper.turnOffSound(context!!)
            if (ReaderSdk.authorizationManager().authorizationState.isAuthorized){
                val p = 100.00
                val checkOutTotal = p.toLong()
                val amountMoney = Money(checkOutTotal, CurrencyCode.current())
                val parametersBuilder = CheckoutParameters.newBuilder(amountMoney)
                parametersBuilder.skipReceipt(false)
                val checkoutManager = ReaderSdk.checkoutManager()
                checkoutManager.startCheckoutActivity(context!!, parametersBuilder.build())
            }
    }
    private fun updateVehicleInfoUI(){
        val vehicleSettings = viewModel.getvehicleSettings()
        if (vehicleSettings != null) {
            val companyName = vehicleSettings.companyName
            cabNumber = vehicleSettings.cabNumber
            updateUI(companyName)
        }
    }

    private fun checkAppBuildVersion(){
        val lastSavedAppVersion = ModelPreferences(context!!.applicationContext).getObject(SharedPrefEnum.BUILD_VERSION.key,
            AppVersion::class.java)
        val currentBuildVersion = BuildConfig.VERSION_NAME
        if(lastSavedAppVersion == null){
            saveAppBuildVersion()
        } else if (lastSavedAppVersion.version != currentBuildVersion){
            Log.i("VERSION", "Build Version is different. Updating ${lastSavedAppVersion.version} to $currentBuildVersion")
            lastSavedAppVersion.version = currentBuildVersion
            ModelPreferences(context!!.applicationContext).putObject(SharedPrefEnum.BUILD_VERSION.key, lastSavedAppVersion)
            LoggerHelper.writeToLog(context!!, "${logFragment}: Build Version is different. Updating ${lastSavedAppVersion.version} to $currentBuildVersion. Restarting Tablet")
            // if we wanted to restart PIM this is where we would write that code.
            restartAppTimer.start()
        } else {
            Log.i("VERSION", "Build Version is the same as last saved amount")
            LoggerHelper.writeToLog(context!!, "${logFragment}: Build Version is the same from last startup. Build version $currentBuildVersion")
        }
    }
    private fun saveAppBuildVersion(){
        val buildName = BuildConfig.VERSION_NAME
        val appVersion = AppVersion(buildName)
        ModelPreferences(context!!.applicationContext).putObject(SharedPrefEnum.BUILD_VERSION.key,appVersion)
        Log.i("VERSION", "Current app version is $buildName. It has been saved to Model Preferences")
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
        if(tripId != ""){
            callBackViewModel.updateCurrentTrip(true, tripId, "off", context!!)
        }
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.toTaxiNumber)
        }
    }
    private fun toCashOrCard(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.trip_review_fragment)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i("PimApplication", "OnStop")
        keyboardViewModel.bothKeyboardsDown()
        batteryCheckTimer.cancel()
        dimScreenTimer.cancel()
    }

    override fun onPause() {
        super.onPause()
        Log.i("PimApplication", "OnPause")
        ViewHelper.hideSystemUI(activity!!)
        batteryCheckTimer.cancel()
        dimScreenTimer.cancel()
    }

    override fun onResume() {
        Log.i("PimApplication", "OnResume")
        super.onResume()
        ViewHelper.hideSystemUI(activity!!)
        vehicleId = viewModel.getVehicleId()
        updatePIMStatus(vehicleId,
            PIMStatusEnum.START_SCREEN.status,
            mAWSAppSyncClient!!)
        batteryCheckTimer.cancel()
        batteryCheckTimer.start()
        dimScreenTimer.cancel()
        dimScreenTimer.start()
        if (!VehicleTripArrayHolder.squareHasBeenSetUp) {
            Log.i("PimApplication", "started squareFlow")
            startSquareFlow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callBackViewModel.getTripStatus().removeObservers(this)
        callBackViewModel.hasNewTripStarted().removeObservers(this)
        restartAppTimer.cancel()
    }
}
