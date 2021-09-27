package com.example.nts_pim.fragments_viewmodel.bluetooth_pairing

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.amplify.generated.graphql.GetStatusQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.SetupHolder
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.startup.adapter.StartupAdapter
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.dialog_composer.PIMDialogComposer
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.keyboards.PhoneKeyboard
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.startup.*
import kotlinx.android.synthetic.main.startup.password_editText
import kotlinx.android.synthetic.main.startup.password_scroll_view
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.lang.IllegalStateException

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class BlueToothPairingFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val logTag = "Bluetooth_Pairing_Fragment"
    private var navController: NavController? = null
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var viewModel: VehicleSetupViewModel
    private val currentFragmentId = R.id.blueToothPairingFragment
    private var vehicleId: String? = null
    private var isBluetoothOn:Boolean? = null
    private var deviceId: String? = null
    private var noBTConnectionTimer: CountDownTimer? = null
    private var noBTView: View? = null
    private var viewGroup: ViewGroup? = null
    private var adapterOne: StartupAdapter? = null
    private var adapterTwo: StartupAdapter? = null
    private var adapterThree: StartupAdapter? = null
    private var buttonCount = 0
    private val password = "1234"
    private var isPasswordEntered = false
    private  val fullBrightness = 255

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.startup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this, viewModelFactory).get(VehicleSetupViewModel::class.java)
        val keyboardViewModelFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
        val factory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, factory)
            .get(UpfrontPriceViewModel::class.java)
        keyboardViewModel = ViewModelProvider(this, keyboardViewModelFactory)
            .get(SettingsKeyboardViewModel::class.java)
        isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value
        initRecyclerViews()
        vehicleId = viewModel.getVehicleID()
        deviceId = DeviceIdCheck.getDeviceId() ?: ""
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        VehicleTripArrayHolder.updateInternalPIMStatus(PIMStatusEnum.PIM_PAIRING.status)
        startBluetoothConnectionTimer()
        setUpKeyboard()
        if(PIMMutationHelper.stopPimSetup){
            val error = PIMMutationHelper.pimError
            error?.message?.let {
                val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
               val keyboardViewModel = ViewModelProvider(this, keyboardFactory)
                    .get(SettingsKeyboardViewModel::class.java)
                PIMDialogComposer.wrongPhoneNumberForPIM(this.requireActivity(),
                    it, viewModel, keyboardViewModel, vehicleId!!,mAWSAppSyncClient!! )
            }
        }
        stepOneImageButton.setOnClickListener {
            openCloseStepOneListView()
        }

        stepTwoImageButton.setOnClickListener {
            openCloseStepTwoListView()
        }

        stepThreeImageButton.setOnClickListener {
            openCloseStepThreeListView()
        }
        if(!isBluetoothOn!!){
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is off. Going to welcome screen", LogEnums.BLUETOOTH.tag)
            toWelcomeScreen()
        } else {
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is on. Starting pairing process", LogEnums.BLUETOOTH.tag)
            checkIfDriverIsSignedIn(vehicleId!!)
            getDriverTabletBluetoothAddress(deviceId!!)
        }

        BluetoothDataCenter.isConnectedToDriverTablet().observe(this.viewLifecycleOwner, Observer { tabletConnected ->
            if (tabletConnected){
                val dataObject = NTSPimPacket.PimStatusObj()
                val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
                Log.i("Bluetooth", "status request packet to be sent == $statusObj")
                SetupHolder.sentTestPacket()
                (activity as MainActivity).sendBluetoothPacket(statusObj)
                BluetoothDataCenter.startUpBTPairSuccessful()
            }
        })

        SetupHolder.isDriverBTAddressCorrect().observe(this.viewLifecycleOwner, Observer { valid ->
            if(valid){
                updateAdapter(adapterThree!!)
            }
        })

        SetupHolder.didFindDriverTablet().observe(this.viewLifecycleOwner, Observer { foundTablet ->
            if(foundTablet){
                updateAdapter(adapterThree!!)
                if(noBTView != null){
                    val bluetoothLayout = view?.findViewById<View>(R.id.no_bluetooth_connection_layout)
                    viewGroup?.removeView(bluetoothLayout)
                    viewGroup?.removeView(noBTView)
                }
            }
        })

        SetupHolder.didSendTestPacket().observe(this.viewLifecycleOwner, Observer { sentPacket ->
            if(sentPacket){
                updateAdapter(adapterThree!!)
            }
        })
        SetupHolder.didReceiveTestPacket().observe(this.viewLifecycleOwner, Observer { receivedPacket ->
            if(receivedPacket){
                updateAdapter((adapterThree!!))
                SetupHolder.bluetoothConnectionFinished()
            }
        })

        SetupHolder.isBluetoothConnectionFinished().observe(this.viewLifecycleOwner, Observer { btConnectionFinished ->
            if(btConnectionFinished){
             updateAdapter(adapterThree!!)
                stopPowerCheckTimer()
                toWelcomeScreen()
            }
        })

        keyboardViewModel.isPhoneKeyboardUp().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it) {
                isPasswordEntered = true
            }
            if (!it && isPasswordEntered) {
                checkPassword()
                isPasswordEntered = false
            }
        })
        admin_screen_btn.setOnClickListener {
            changeScreenBrightness(fullBrightness)
            buttonCount += 1
            if (buttonCount in 2..5) {
                showToastMessage("$buttonCount", 1000)
            }
            if (buttonCount == 5) {
                admin_screen_btn.animate().alpha(1.00f).duration = 500
                keyboardViewModel.phoneKeyboardIsUp()
                ViewHelper.viewSlideUp(password_scroll_view, 500)
            }
            if (buttonCount >= 6) {
                if (buttonCount % 2 == 0) {
                    admin_screen_btn.animate().alpha(0.0f).duration = 500
                    ViewHelper.viewSlideDown(password_scroll_view, 500)
                    password_editText.setText("")
                    keyboardViewModel.bothKeyboardsDown()
                } else {
                    admin_screen_btn.animate().alpha(1.00f).duration = 500
                    keyboardViewModel.phoneKeyboardIsUp()
                    ViewHelper.viewSlideUp(password_scroll_view, 500)
                }
            }
        }
    }

    private fun setUpKeyboard() {
        password_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        password_editText.setTextIsSelectable(false)
        val phoneKeyboard = startupPasswordPhoneKeyboard as PhoneKeyboard
        val ic = password_editText.onCreateInputConnection(EditorInfo())
        phoneKeyboard.setInputConnection(ic)
    }
    private fun checkPassword() {
        val passwordEntered = password_editText.text.toString().replace("\\s".toRegex(), "")
        if (passwordEntered == password) {
            changeScreenBrightness(255)
            toVehicleSettingsDetail()
        }
    }

    private fun toVehicleSettingsDetail(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_blueToothPairingFragment_to_vehicle_settings_detail_fragment)
        }
    }
    private fun showToastMessage(text: String, duration: Int) {
        val toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT)
        toast.show()
        val handler = Handler()
        handler.postDelayed( { toast.cancel() }, duration.toLong())
    }

    private fun stopPowerCheckTimer(){
        (activity as MainActivity).stopPowerCheckTimerForStartup()
    }
    private fun updateAdapter(adapter: StartupAdapter){
        adapter.notifyDataSetChanged()
    }

    private fun openCloseStepOneListView(){
        if(stepOneListView.visibility == View.VISIBLE) {
            stepOneListView.visibility = View.GONE
            stepOneImageButton.setImageResource(R.drawable.ic_plus_btn_white)
            return
        }

        if(stepOneListView.visibility == View.GONE){
            stepOneListView.visibility = View.VISIBLE
            stepOneImageButton.setImageResource(R.drawable.ic_close_btn_white)
        }
    }

    private fun openCloseStepTwoListView(){
        if(stepTwoListView.visibility == View.VISIBLE) {
            stepTwoListView.visibility = View.GONE
            stepTwoImageButton.setImageResource(R.drawable.ic_plus_btn_white)
            return
        }

        if(stepTwoListView.visibility == View.GONE){
            stepTwoListView.visibility = View.VISIBLE
            stepTwoImageButton.setImageResource(R.drawable.ic_close_btn_white)
        }
    }

    private fun openCloseStepThreeListView(){
        if(stepThreeListView.visibility == View.VISIBLE) {
            stepThreeListView.visibility = View.GONE
            stepThreeImageButton.setImageResource(R.drawable.ic_plus_btn_white)
            return
        }

        if(stepThreeListView.visibility == View.GONE){
            stepThreeListView.visibility = View.VISIBLE
            stepThreeImageButton.setImageResource(R.drawable.ic_close_btn_white)
        }
    }

    private fun initRecyclerViews(){
        stepOne_title_textView.text = "AWS: Connected"
        step_one_status_imageView.visibility = View.VISIBLE

        stepTwo_title_textView.text = "Reader Status: ${VehicleTripArrayHolder.cardReaderStatus}"
        step_two_status_ImageView.visibility = View.VISIBLE
        makeStartupList()
        if(stepOneListView != null) {
            stepOneListView.adapter = adapterOne as StartupAdapter
        }
        if(stepTwoListView != null) {
            stepTwoListView.adapter = adapterTwo as StartupAdapter
        }
        if(stepThreeListView != null){
            stepThreeListView.adapter = adapterThree as StartupAdapter
        }
    }



    private fun makeStartupList() {
        val stepOneList = SetupHolder.getStepOneList()
        val stepTwoList = SetupHolder.getStepTwoList()
        val stepThreeList = SetupHolder.getStepThreeList()
        adapterOne = StartupAdapter(requireContext(), stepOneList)
        adapterTwo = StartupAdapter(requireContext(), stepTwoList)
        adapterThree = StartupAdapter(requireContext(), stepThreeList)
    }

    private fun getDriverTabletBluetoothAddress(deviceId: String){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        }

        mAWSAppSyncClient?.query(GetPimSettingsQuery.builder().deviceId(deviceId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(awsBluetoothAddressCallback)
    }
    private var awsBluetoothAddressCallback = object: GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
        override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {
            Log.i("Bluetooth", "Bluetooth query response == ${response.data()}")

            val bluetoothAddress = response.data()?.pimSettings?.vehBtAddr()

            if(bluetoothAddress != null){
                BluetoothDataCenter.updateDriverTabletBTDevice(bluetoothAddress)
            }
        }

        override fun onFailure(e: ApolloException) {

        }
    }

    private fun checkIfDriverIsSignedIn(vehicleId:String) {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        }
        mAWSAppSyncClient?.query(GetStatusQuery.builder().vehicleId(vehicleId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(driverSignedInQuery)
    }
    private var driverSignedInQuery = object: GraphQLCall.Callback<GetStatusQuery.Data>(){
        override fun onResponse(response: Response<GetStatusQuery.Data>) {
            response.data()?.status?.signinStatusTimeStamp()
           val driverId = response.data()?.status?.driverId()
            if(driverId != 0){
                upfrontPriceViewModel.driverSignedIn()
               LoggerHelper.writeToLog("Driver signed in", LogEnums.TRIP_STATUS.tag)
            }
            if(driverId == 0) {
                upfrontPriceViewModel.driverSignedOut()
                LoggerHelper.writeToLog("Driver not signed in", LogEnums.TRIP_STATUS.tag)
            }
        }

        override fun onFailure(e: ApolloException) {

        }
    }

    private fun startBluetoothConnectionTimer(){
        val timerLength = 600000.toLong()
         viewGroup = activity?.findViewById<View>(R.id.linearLayout2) as ViewGroup
        if(noBTConnectionTimer == null){
            noBTConnectionTimer = object: CountDownTimer(timerLength, 60000){
                override fun onFinish() {
                    try {
                        noBTView =  View.inflate(activity, R.layout.no_bluetooth_connection, viewGroup)
                        val closeBtn = noBTView!!.findViewById<ImageView>(R.id.close_no_bt_connection_screen_imageView)
                        closeBtn.setOnClickListener {
                            viewGroup!!.removeView(noBTView)
                        }
                    }catch (e: IllegalStateException){
                        LoggerHelper.writeToLog("Issue with the no_bluetooth_connection inflating. Error: $e", LogEnums.BLUETOOTH.tag)
                    }

                }

                override fun onTick(p0: Long) {
                }
            }.start()
        }
    }
    private fun changeScreenBrightness(screenBrightness: Int) {
        Settings.System.putInt(
            requireContext().contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )  //this will set the manual mode (set the automatic mode off)
        Settings.System.putInt(
            requireContext().contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            screenBrightness
        )  //this will set the brightness to maximum (255)

        //refreshes the screen
        val br =
            Settings.System.getInt(
                requireContext().contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        val lp = requireActivity().window.attributes
        lp.screenBrightness = br.toFloat() / 255
        requireActivity().window.attributes = lp
    }

   //Navigation
   private fun toWelcomeScreen() = launch(Dispatchers.Main.immediate){

       if (navController?.currentDestination?.id == currentFragmentId) {
           LoggerHelper.writeToLog("From Bluetooth_Pairing_Fragment to Welcome_Fragment", LogEnums.LIFE_CYCLE.tag)
           navController?.navigate(R.id.action_blueToothPairingFragment_to_welcome_fragment)
       }
       if(noBTView != null){
           val bluetoothLayout = view?.findViewById<View>(R.id.no_bluetooth_connection_layout)
           viewGroup?.removeView(bluetoothLayout)
           viewGroup?.removeView(noBTView)
       }
       noBTConnectionTimer?.cancel()
   }

    override fun onDestroy() {
       BluetoothDataCenter.isConnectedToDriverTablet().removeObservers(this)
        noBTConnectionTimer?.cancel()
        super.onDestroy()
    }
}
