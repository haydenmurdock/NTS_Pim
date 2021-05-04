package com.example.nts_pim.fragments_viewmodel.welcome

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.text.InputType
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.PimApplication
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.AppVersion
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.keyboards.PhoneKeyboard
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.upfront_price_errors.UpfrontPriceErrorHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.error_view.*
import kotlinx.android.synthetic.main.up_front_price_detail_fragment.*
import kotlinx.android.synthetic.main.welcome_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timerTask


class WelcomeFragment : ScopedFragment(), KodeinAware {

    // Kodein and ViewModel/Factory
    override val kodein by closestKodein()
    private val viewModelFactory: WelcomeViewModelFactory by instance<WelcomeViewModelFactory>()
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private lateinit var viewModel: WelcomeViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var failedReaderTimer: CountDownTimer? = null
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
    private var isDriverSignedIn = false
    private val logFragment = "Welcome_Screen"

    private val batteryCheckTimer = object : CountDownTimer( 600000, 60000) {
        //Every 10 minutes  we are doing a battery check.
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            if (!resources.getBoolean(R.bool.isSquareBuildOn)){
                batteryStatusCheck()
            }
        }
    }
    private val dimScreenTimer = object : CountDownTimer(120000, 60000) {
        //after a status has changed to end we run a 2 min timer to dimScreen
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            if (!resources.getBoolean(R.bool.isSquareBuildOn)){
                changeScreenBrightness(dimBrightness)
            }
        }
    }
    private val restartAppTimer = object: CountDownTimer(30000, 10000){
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
    @SuppressLint("ClickableViewAccessibility", "ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        val keyboardViewModelFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
        ViewHelper.hideSystemUI(requireActivity())
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(WelcomeViewModel::class.java)
        keyboardViewModel = ViewModelProvider(this, keyboardViewModelFactory)
            .get(SettingsKeyboardViewModel::class.java)
        val upfrontPriceFactory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, upfrontPriceFactory)
            .get(UpfrontPriceViewModel::class.java)
        // checks for animation and navigates to next Screen
        setUpKeyboard()
        lastTrip = checkToSeeIfOnTrip()
        if (lastTrip.first != null) {
            isOnActiveTrip = lastTrip.first!!
            tripId = lastTrip.second
        }
        getTimeOfDayAndUpdateUI()
        viewModel.isSetupComplete()
        vehicleId = viewModel.getVehicleId()
        updateVehicleInfoUI()
        checkAppBuildVersion()
        isDriverSignedIn = upfrontPriceViewModel.isDriverSignedIn().value ?: false

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
        view.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    dimScreenTimer.cancel()
                    changeScreenBrightness(fullBrightness)
                    dimScreenTimer.start()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        enter_destination_view.setOnTouchListener{v, event ->
            when(event?.action){
                MotionEvent.ACTION_DOWN -> {
                    if(isDriverSignedIn){
                        toEnterDestination()
                    } else {
                     UpfrontPriceErrorHelper.showDriverNotSignedInError(requireActivity(), vehicleId)
                    }
                }
            }
            v?.onTouchEvent(event) ?: true
        }
        open_vehicle_settings_button.setOnClickListener {
            dimScreenTimer.cancel()
            changeScreenBrightness(fullBrightness)
            dimScreenTimer.start()
            buttonCount += 1
            if (buttonCount in 2..5) {
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
        callBackViewModel.isPimPaired().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer { isPaired ->
            if(!isPaired){
                toVehicleSettingsDetail()
            }
        })

        callBackViewModel.getMeterState().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer { meterState ->
            if(meterState == MeterEnum.METER_ON.state || meterState == MeterEnum.METER_TIME_OFF.state){
               LoggerHelper.writeToLog( "Meter $meterState is picked up on welcome screen. Starting trip animation", LogEnums.TRIP_STATUS.tag)
                changeScreenBrightness(fullBrightness)
                toNextScreen()
            }
        })

        PIMMutationHelper.updatePIMStatus(vehicleId, PIMStatusEnum.WELCOME_SCREEN.status, mAWSAppSyncClient!!)
        changeLoggingTimer()
        sendPimStatusBluetooth()
        SoundHelper.turnOnSound(PimApplication.pimContext)
        VehicleTripArrayHolder.squareHasBeenSetUp = true
        upfrontPriceViewModel.isDriverSignedIn().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer {isSignedIn ->
           isDriverSignedIn = isSignedIn
        })
    }


    private fun getTimeOfDayAndUpdateUI() {
        val currentDate = LocalDateTime.now()
        val formatter =
            DateTimeFormatter.ofPattern("HH")
        val hour = currentDate.format(formatter).toInt()
        Log.i("Hour", "$hour")
        val morning = 4.rangeTo(11)
        val afternoon = 12.rangeTo(16)
        val evening = 17.rangeTo(24)
        val earlyEvening = 0.rangeTo(3)
        if(morning.contains(hour)){
            greetings_textView.text = "Good Morning!"
        }
        if(afternoon.contains(hour)){
            greetings_textView.text = "Good Afternoon!"
        }
        if(evening.contains(hour)){
            greetings_textView.text = "Good Evening!"
        }
        if(earlyEvening.contains(hour)) {
            greetings_textView.text = "Good Evening!"
        }
    }

    private fun sendPimStatusBluetooth(){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val dataObject = NTSPimPacket.PimStatusObj()
            val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
            (activity as MainActivity).sendBluetoothPacket(statusObj)
        }
    }
    private fun changeLoggingTimer(){
        LoggerHelper.loggingTime = 180000
        Log.i(tag,"Logging time was set to ${LoggerHelper.loggingTime}")
    }

    private fun tripIsCurrentlyRunning(isTripActive: Boolean){
        if (!isTripActive){
            LoggerHelper.writeToLog("$logFragment. Trip Check: Last saved trip is not active", null)
            return
        }
        changeScreenBrightness(fullBrightness)
        LoggerHelper.writeToLog("$logFragment Trip Check: Last saved trip is active. To Meter Screen", null)
        toLiveMeterScreen()
    }
    private fun updateUI(companyName: String) {
        password_editText.isClickable = false
        password_editText.isFocusable = false
    }

    private fun toNextScreen() {
        if(view != null && requireView().isVisible){
            if (welcome_text_view2 != null){
                welcome_text_view2.animate().alpha(0f).setDuration(2500).withEndAction(Runnable {
                    toTaxiNumber()
                })
             }
        }
    }
    private fun markPimStartTime(){
        val time = LocalDateTime.now()
        TripDetails.tripStartTime = time
        LoggerHelper.writeToLog("$logFragment: StartTime of trip on pim: $time", LogEnums.TRIP_STATUS.tag)
    }
    private fun checkPassword() {
        if (password_editText.text.toString() == password) {
            changeScreenBrightness(255)
            toVehicleSettingsDetail()
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
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
        if (!isCharging) {
            LoggerHelper.writeToLog("$logFragment: Battery Check: is charging: $isCharging, sending request for shutdown", LogEnums.OVERHEATING.tag)
            val action =  "com.claren.tablet_control.shutdown"
            val p = "com.claren.tablet_control"
            val intent = Intent()
            intent.action = action
            intent.`package` = p
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            activity?.sendBroadcast(intent)
        }

        if (isCharging) {
            LoggerHelper.writeToLog("$logFragment: Battery Check: is charging: $isCharging", LogEnums.OVERHEATING.tag)
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
    }

    private fun checkToSeeIfOnTrip(): Pair<Boolean?, String>{
        if (!resources.getBoolean(R.bool.isDevModeOn)){
            val lastTrip = ModelPreferences(requireContext())
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

    private fun updateVehicleInfoUI(){
        val vehicleSettings = viewModel.getvehicleSettings()
        if (vehicleSettings != null) {
            val companyName = vehicleSettings.companyName
            cabNumber = vehicleSettings.cabNumber
            updateUI(companyName)
        }
    }

    private fun checkAppBuildVersion(){
        val lastSavedAppVersion = ModelPreferences(requireContext().applicationContext).getObject(SharedPrefEnum.BUILD_VERSION.key,
            AppVersion::class.java)
        val currentBuildVersion = BuildConfig.VERSION_NAME
        when {
            lastSavedAppVersion == null -> {
                saveAppBuildVersion()
            }
            lastSavedAppVersion.version != currentBuildVersion -> {
                if(!VehicleTripArrayHolder.flaggedTestVehicles.contains(vehicleId)){
                    lastSavedAppVersion.version = currentBuildVersion
                    ModelPreferences(requireContext().applicationContext).putObject(SharedPrefEnum.BUILD_VERSION.key, lastSavedAppVersion)
                    LoggerHelper.writeToLog("${logFragment}: Build Version is different. Updating ${lastSavedAppVersion.version} to $currentBuildVersion. Restarting Tablet", LogEnums.TRIP_STATUS.tag)
                    // if we wanted to restart PIM this is where we would write that code.
                    restartAppTimer.start()
                } else {
                    lastSavedAppVersion.version = currentBuildVersion
                    ModelPreferences(requireContext().applicationContext).putObject(SharedPrefEnum.BUILD_VERSION.key, lastSavedAppVersion)
                    LoggerHelper.writeToLog("This is a test vehicle so restart for new app version ignored.", LogEnums.TRIP_STATUS.tag)
                    val toast = Toast.makeText(activity, "Test vehicle: $vehicleId, app version: $currentBuildVersion, restart ignored", Toast.LENGTH_LONG)
                    toast.show()
                }
            }
            else -> {
                LoggerHelper.writeToLog("${logFragment}: Build Version is the same from last startup. Build version $currentBuildVersion", LogEnums.TRIP_STATUS.tag)
            }
        }
    }
    private fun saveAppBuildVersion(){
        val buildName = BuildConfig.VERSION_NAME
        val appVersion = AppVersion(buildName)
        ModelPreferences(requireContext().applicationContext).putObject(SharedPrefEnum.BUILD_VERSION.key,appVersion)
        LoggerHelper.writeToLog("Current app version is $buildName. It has been saved to Model Preferences", null)
    }
    // Navigation
    private fun toLiveMeterScreen() = launch(Dispatchers.Main.immediate) {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.welcomeFragmentToLiveMeterSceen)
        }
    }
    private fun toTaxiNumber() {
        markPimStartTime()
        if(tripId != ""){
            callBackViewModel.updateCurrentTrip(true, tripId, "off", requireContext())
        }
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.toTaxiNumber)
        }
    }
    private fun toVehicleSettingsDetail(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.toVehicleSettingsDetail)
        }
    }
    private fun toEnterDestination(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_welcome_fragment_to_enterDestination)
        }

    }

    override fun onStop() {
        super.onStop()
        Log.i("PimApplication", "OnStop")
        keyboardViewModel.bothKeyboardsDown()
        batteryCheckTimer.cancel()
        dimScreenTimer.cancel()
        failedReaderTimer?.cancel()
    }

    override fun onPause() {
        super.onPause()
        Log.i("PimApplication", "OnPause")
        ViewHelper.hideSystemUI(requireActivity())
        batteryCheckTimer.cancel()
        dimScreenTimer.cancel()
        failedReaderTimer?.cancel()
    }

    override fun onResume() {
        super.onResume()
        ViewHelper.hideSystemUI(requireActivity())
        vehicleId = viewModel.getVehicleId()
        batteryCheckTimer.cancel()
        batteryCheckTimer.start()
        dimScreenTimer.cancel()
        dimScreenTimer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(this::callBackViewModel.isInitialized){
            callBackViewModel.hasNewTripStarted().removeObservers(this)
            callBackViewModel.isPimPaired().removeObservers(this)
            callBackViewModel.getMeterState().removeObservers(this)
        }
        failedReaderTimer?.cancel()
        restartAppTimer.cancel()
    }
}
