package com.example.nts_pim.fragments_viewmodel.vehicle_setup


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Adapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.*
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.dialog_composer.PIMDialogComposer
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.keyboards.QwertyKeyboard
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.github.ybq.android.spinkit.style.DoubleBounce
import com.github.ybq.android.spinkit.style.ThreeBounce
import com.google.gson.Gson
import com.sdsmdg.tastytoast.TastyToast
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.authorization.AuthorizationManager
import com.squareup.sdk.reader.authorization.AuthorizeErrorCode
import com.squareup.sdk.reader.authorization.Location
import com.squareup.sdk.reader.core.CallbackReference
import com.squareup.sdk.reader.core.Result
import com.squareup.sdk.reader.core.ResultError
import com.squareup.sdk.reader.hardware.ReaderSettingsErrorCode
import kotlinx.android.synthetic.main.vehicle_setup.*
import kotlinx.android.synthetic.main.vehicle_setup.qwertyKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.lang.Error


class VehicleSetupFragment:ScopedFragment(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModelFactory: VehicleSetupModelFactory by instance()
    private lateinit var viewModel: VehicleSetupViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var authorizeCallbackRef: CallbackReference? = null
    private var readerSettingsCallbackRef: CallbackReference? = null


    private val setNamesArray =
        arrayOf("UUID", "PIN", "Vehicle ID", "Authorization", "Bluetooth Setup")
    private val setBooleanArray = arrayOf(false, false, false, false, false)

    private var uuid = ""

    private var pin = ""

    private var vehicleID = ""

    private var authCode = ""
    private var authorized = false
    var pinEntered = false
    var incorrectPin = ""
    var adapter: Adapter? = null
    var doesVehicleIdExist = false
    val authManager = ReaderSdk.authorizationManager()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.vehicle_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MyCustomAdapter(context!!, setNamesArray, setBooleanArray)
        listView.adapter = adapter as MyCustomAdapter


        authorizeCallbackRef =
            authManager.addAuthorizeCallback(this::onAuthorizeResult)
        val readerManager = ReaderSdk.readerManager()
        readerSettingsCallbackRef =
            readerManager.addReaderSettingsActivityCallback(this::onReaderSettingsResult)

        mAWSAppSyncClient = ClientFactory.getInstance(context)

        val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)

        keyboardViewModel = ViewModelProviders.of(this, keyboardFactory)
            .get(SettingsKeyboardViewModel::class.java)
        setUpKeyboard()
        val doubleBounce = DoubleBounce()
        auth_progressBar.setIndeterminateDrawable(doubleBounce)
        val threeBounce = ThreeBounce()
        vehicle_id_progressBar.setIndeterminateDrawable(threeBounce)
        requestMic()
        keyboardViewModel.isQwertyKeyboardUp().observe(this, Observer {
            //Once a pin has been created we will try to pull the vehicle_Id from AWS
            if (it)
                pinEntered = true
            if (!it && pinEntered) {
                val enteredPin = enter_pin_editText.text.toString()
                pin = enteredPin
                println(uuid)
                vehicle_id_progressBar.isVisible = true
                vehicle_id_progressBar.animate()
                queryVehicleId(uuid, pin, adapter as MyCustomAdapter)
                pinEntered = false
                keyboardViewModel.qwertyKeyboardisUp()
            }
        })

        doesVehicleIdExist = viewModel.doesVehicleIDExist()
            if (!doesVehicleIdExist) {
                checkDeviceID(view, adapter as MyCustomAdapter)
                checkForPin(adapter as MyCustomAdapter)
            } else {
                updateChecklist(0, true, adapter as MyCustomAdapter)
                updateChecklist(1, true, adapter as MyCustomAdapter)
                updateChecklist(2, true, adapter as MyCustomAdapter)
                vehicle_id_progressBar.isVisible = false
                pin_detail_scrollView.isVisible = false
                setup_detail_scrollView.isVisible = true
                setup_detail_text_view.text = ""
                Thread {
                    checkAuthorization(vehicleID, authManager)
                }.start()
            }

        viewModel.isThereAuthCode().observe(this, Observer {
            if (it) {
                //Mark 1
                auth_progressBar.isVisible = true
                auth_progressBar.animate()
                retrieveAuthorizationCode(authCode)
            }
        })

        viewModel.isSquareAuthorized().observe(this, Observer {
            if (it) {
                updateChecklist(3, true, adapter as MyCustomAdapter)
                pin_detail_scrollView.isVisible = false
                setup_detail_scrollView.isVisible = true
                setup_complete_btn.isVisible = false
                setup_complete_btn.isEnabled = false
                setup_complete_btn.isVisible = true
                setup_complete_btn.text = "DONE"
                authorized = true
                auth_progressBar.isVisible = false
                auth_progressBar.clearAnimation()
                setup_detail_text_view.text = "Bluetooth setup complete"
                setup_detail_text_view.isVisible = true
                SoundHelper.turnOffSound(context!!)
                startReaderSettings(adapter as MyCustomAdapter)
            }
        })

        viewModel.isPinEnteredWrong().observe(this, Observer {
            val isPinWrong = it
            if (isPinWrong) {
                enter_pin_editText.setText("")
                val toast = Toast.makeText(context, "$incorrectPin is incorrect", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 145, 0)
                toast.show()
                viewModel.pinWasEnteredWrong()
                vehicle_id_progressBar.isVisible = false
                vehicle_id_progressBar.clearAnimation()
            }
        })

        setup_complete_btn.setOnClickListener {
            //This is for if there needs to be a check auth because its the first one in fleet.
            if (authorized) {
                setUpComplete()
                SoundHelper.turnOnSound(context!!)
                val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                if (navController.currentDestination?.id == R.id.vehicleSetupFragment){
                    navController.navigate(R.id.toCheckVehicleInfoFragment)
                }
            } else {
                setup_detail_text_view.isVisible = false
                setup_complete_btn.isVisible = false
                auth_progressBar.animate()
                checkAuthorization(vehicleID, authManager)
            }
        }
    }

    private fun checkDeviceID(view: View, adapter: MyCustomAdapter) = launch {
        uuid = viewModel.getDeviceID.await()
        // This in case there is no device ID, we make one
        if (uuid == "") {
            val deviceIDString = Settings.Secure.getString(
                getContext()?.getContentResolver(),
                Settings.Secure.ANDROID_ID
            )

            val deviceId =
                DeviceID(deviceIDString)

            ModelPreferences(view.context)
                .putObject(SharedPrefEnum.DEVICE_ID.key, deviceId)
            uuid = deviceId.number
            setup_detail_text_view.text = "UUID was created and updated: $uuid"
            updateChecklist(0, true, adapter)
            Log.i(
                LogEnums.PIM_SETTING.tag,
                "Device Id: $deviceIDString saved to Shared Preferences"
            )

        } else {
            setup_detail_text_view.text = "UUID was found $uuid"
            updateChecklist(0, true, adapter)
            Log.i(
                LogEnums.PIM_SETTING.tag,
                "Device Id: Already saved into preferences and is $uuid"
            )
        }
    }

    private fun checkForPin(adapter: MyCustomAdapter) = launch {
        pin = viewModel.getPin.await()
        if (pin == "") {
            //There is no pin so we will need to show the pin screen
            keyboardViewModel.qwertyKeyboardisUp()
            setup_detail_text_view.text = "No pin in settings"
            setup_detail_scrollView.isVisible = false
            pin_detail_scrollView.isVisible = true
            Log.i(LogEnums.PIM_SETTING.tag, "No Pin In Settings")
        } else {
            // There is a pin found so we will get the Vehicle ID
            setup_detail_text_view.text = "Pin: $pin found in settings"
            updateChecklist(1, true, adapter)
            if (uuid != "") {
                queryVehicleId(uuid, pin, adapter)
            }
            Log.i(LogEnums.PIM_SETTING.tag, "Pin: $pin found in settings")
        }
    }

    private fun createPin(enteredPin: String, adapter: MyCustomAdapter) {
        val pin = PIN(enteredPin)
        // Puts pin into preferences and updates listView
        ModelPreferences(context!!).putObject(SharedPrefEnum.PIN_PASSWORD.key, pin)
        launch(Dispatchers.Main.immediate) {
            updateChecklist(1, true, adapter)
        }
        Log.i(LogEnums.PIM_SETTING.tag, "Pin Password: $enteredPin saved to Shared Preferences")
    }


    private fun queryVehicleId(deviceID: String, pin: String, adapter: MyCustomAdapter) = launch {
        var pimSettingsQueryCallBack = object : GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
            override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {
                val callBackVehicleID = response.data()?.pimSettings?.vehicleId()
                if (callBackVehicleID != null) {
                    createPin(pin, adapter)
                    vehicleID = callBackVehicleID
                    saveVehicleID(vehicleID)
                }
                if (callBackVehicleID == null) {
                    pinIsWrong(pin)
                }
                Log.i(LogEnums.PIM_SETTING.tag, "Vehicle Id from call back is $callBackVehicleID")
            }

            override fun onFailure(e: ApolloException) {
                PIMDialogComposer.showNoVehicleIDError(activity!!)
                Log.e("ERROR", e.toString())
            }
        }
        mAWSAppSyncClient?.query(GetPimSettingsQuery.builder().deviceId(deviceID).pin(pin).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(pimSettingsQueryCallBack)
        Log.i("Results", "data used for callback was $deviceID and $pin")
    }

    private fun pinIsWrong(pin: String) {
        incorrectPin = pin
        launch(Dispatchers.Main.immediate) {
            viewModel.pinWasEnteredWrong()
        }
    }

    private fun saveVehicleID(vehicleId: String) {
        val vehicleID = VehicleID(vehicleId)
        ModelPreferences(context!!).putObject(SharedPrefEnum.VEHICLE_ID.key, vehicleID)
        launch(Dispatchers.Main.immediate) {
            viewModel.vehicleIDExists()
            updateChecklist(0, true, adapter as MyCustomAdapter)
            updateChecklist(1, true, adapter as MyCustomAdapter)
            updateChecklist(2, true, adapter as MyCustomAdapter)
            vehicle_id_progressBar.isVisible = false
            pin_detail_scrollView.isVisible = false
            setup_detail_scrollView.isVisible = true
            setup_detail_text_view.text = ""
            checkAuthorization(vehicleId, authManager)
        }
        Log.i(
            LogEnums.PIM_SETTING.tag,
            "Vehicle ID: ${vehicleID.vehicleID} saved to System Preferences"
        )
    }

    private fun checkAuthorization(vehicleID: String, authManager: AuthorizationManager) {
        if (authManager.authorizationState.isAuthorized) {
            launch {
                viewModel.squareIsAuthorized()
                val toast =
                    Toast.makeText(context, "Successful authorization of Square", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP, 125, 0)
                toast.show()
            }
        } else {
            launch {
                getAuthorizationCode(vehicleID)
            }
        }
    }

    private fun goToAuthWebView(vehicleID: String) {
        pin_detail_scrollView.isVisible = false
        setup_detail_scrollView.isVisible = true
        setup_complete_btn.isVisible = true
        setup_complete_btn.isClickable = true
        setup_detail_text_view.text =
            "First Vehicle in Fleet for Authorization. Get Authorization"
        setup_complete_btn.text = "Authorization"
        setUpWebView(vehicleID)

    }

    private fun retrieveAuthorizationCode(authCode: String) {
        onAuthorizationCodeRetrieved(authCode)
    }

    private fun onAuthorizationCodeRetrieved(authorizationCode: String) = launch {
        ReaderSdk.authorizationManager().authorize(authorizationCode)
    }

    private fun onAuthorizeResult(result: Result<Location, ResultError<AuthorizeErrorCode>>) {
        if (result.isSuccess) {
            val toast =
                TastyToast.makeText(context, "Successful Square Authorization", TastyToast.LENGTH_LONG, TastyToast.SUCCESS)
            toast.setGravity(Gravity.TOP, 125, 0)
            toast.show()
            launch(Dispatchers.Main.immediate) {
                viewModel.squareIsAuthorized()
            }
        } else {
            val error = result.error
            if (error.code == AuthorizeErrorCode.NO_NETWORK) {
                Toast.makeText(
                    context!!,
                    "No Network ${error.message}", Toast.LENGTH_LONG
                ).show()
            }
            if (error.code == AuthorizeErrorCode.USAGE_ERROR) {
                if (error.debugCode == "authorize_already_authorized") {
                    TastyToast.makeText(context, "Square SDK already authorized for vehicle", TastyToast.LENGTH_LONG, TastyToast.ERROR).show()
                    launch(Dispatchers.Main.immediate) {
                        viewModel.squareIsAuthorized()
                    }
                } else {
                    Toast.makeText(
                        context!!,
                        " Usage_Error: ${error.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setUpWebView(vehicleID: String) {
        val url =
            "https://connect.squareup.com/oauth2/authorize?client_id=sq0idp-HJ_x8bWoeez4H7q5Cfnjug&scope=MERCHANT_PROFILE_READ+PAYMENTS_WRITE_IN_PERSON&state=$vehicleID&session=false"
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browser)
        Log.i("URL", "$url")
    }

    private fun onReaderSettingsResult(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {

        if (result.isSuccess){
        }
        if (result.isError) {
            val error = result.error
            when (error.code) {
                ReaderSettingsErrorCode.SDK_NOT_AUTHORIZED -> Toast.makeText(
                    context!!,
                    "SDK not authorized${error.message}", Toast.LENGTH_LONG
                ).show()
                ReaderSettingsErrorCode.USAGE_ERROR -> Toast.makeText(
                    context!!,
                    "Usage error ${error.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startReaderSettings(adapter: MyCustomAdapter) {
        setup_complete_btn.isEnabled = true
        setup_complete_btn.isVisible = true
        val readerManager = ReaderSdk.readerManager()
        readerManager.startReaderSettingsActivity(context!!)
        updateChecklist(4, true, adapter)
    }

    private fun setUpKeyboard() {
        enter_pin_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        enter_pin_editText.setTextIsSelectable(false)

        val qwertyKeyboard = qwertyKeyboard as QwertyKeyboard
        val ic = enter_pin_editText.onCreateInputConnection(EditorInfo())
        qwertyKeyboard.setInputConnection(ic)
        ViewHelper.hideSystemUI(activity!!)
    }

    private fun updateChecklist(position: Int, boolean: Boolean, adapter: MyCustomAdapter) {
        setBooleanArray[position] = boolean
        adapter.notifyDataSetChanged()
    }
    private fun getAuthorizationCode(vehicleID: String) {
        val url =
            "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleID"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code == 200) {
                        val gson = Gson()
                        val convertedObject =
                            gson.fromJson(response.body?.string(), JsonAuthCode::class.java)
                        authCode = convertedObject.authCode
                        launch(Dispatchers.Main.immediate) {
                            viewModel.successfulAuthCode()
                        }
                    }
                    if (response.code == 404) {
                        Log.i("URL", "Vehicle not found in fleet")
                    }
                    if (response.code == 401) {
                        Log.i("URL", "Need to authorize fleet with log in")
                        launch {
                            goToAuthWebView(vehicleID)
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("failure")
                }
            })
        } catch (e: Error) {
            println(e)
        }
    }

    private fun requestMic() {
        val REQUEST_RECORD_AUDIO_PERMISSION = 200
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    private fun setUpComplete(){
        val setUpStatus = SetupComplete(true)
        ModelPreferences(context!!).putObject(SharedPrefEnum.SETUP_COMPLETE.key, setUpStatus)
    }

    override fun onResume() {
        super.onResume()
        requestMic()
        ViewHelper.hideSystemUI(activity!!)
    }

    override fun onPause() {
        super.onPause()
        ViewHelper.hideSystemUI(activity!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardViewModel.isQwertyKeyboardUp().removeObservers(this)
        viewModel.isSquareAuthorized().removeObservers(this)
        viewModel.isThereAuthCode().removeObservers(this)
        auth_progressBar.clearAnimation()
        vehicle_id_progressBar.clearAnimation()
    }
}

