package com.example.nts_pim.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import com.amazonaws.amplify.generated.graphql.*
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.*
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.receivers.BatteryPowerReceiver
import com.example.nts_pim.receivers.BluetoothReceiver
import com.example.nts_pim.receivers.NetworkReceiver
import com.example.nts_pim.utilities.bluetooth_helper.*
import com.example.nts_pim.utilities.driver_receipt.DriverReceiptHelper
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.enums.PaymentTypeEnum
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.overheat_email.OverHeatEmail
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.coroutines.CoroutineContext

open class MainActivity : AppCompatActivity(), CoroutineScope, KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private var vehicleId = ""
    private var vehicleSubscriptionComplete = false
    private var tripSubscriptionComplete = false
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
    private lateinit var callbackViewModel: CallBackViewModel
    private var internetConnection = false
    private var resync = false
    private var meterStateQueryComplete = false
    private var mSuccessfulSetup = false
    private var mOverheatEmailSent = false
    private var tripId = ""
    private var lastTrip: CurrentTrip? = null
    private var subscriptionWatcherDoPimPayment: AppSyncSubscriptionCall<OnDoPimPaymentSubscription.Data>? =
        null
    private var subscriptionWatcherUpdateVehTripStatus: AppSyncSubscriptionCall<OnUpdateVehTripStatusSubscription.Data>? =
        null
    private var subscriptionWatcherUpdatePimSettings: AppSyncSubscriptionCall<OnPimSettingsUpdateSubscription.Data>? =
        null
    private var subscriptionWatcherUnPairPIM: AppSyncSubscriptionCall<OnPimUnpairSubscription.Data>? = null
    private var loggingTimer: CountDownTimer? = null
    private var internetConnectionTimer: CountDownTimer? = null
    private var vehicleSubscriptionTimer: CountDownTimer? = null
    private val logFragment = "Background Activity"
    private var mNetworkReceiver: NetworkReceiver? = null
    private var mBatteryReceiver: BatteryPowerReceiver? = null
    private var mBluetoothReceiver: BluetoothReceiver? = null
    private var watchingTripId = ""
    internal var unpairPIMSubscription = false
    private var isBluetoothOnAWS = false
    private var driverTablet: BluetoothDevice? = null
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var blueToothAddressDriverTablet: String? = null
    //read thread is not private so write thread can be aware if it was canceled
    var readThread: ReadThread? = null
    private var writeThread: WriteThread? = null
    private var connectThread: ConnectThread? = null
    private var ackThread: ACKThread? = null



    companion object  {
        lateinit var mainActivity: MainActivity
        lateinit var navigationController: NavController
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //This is for screen wake
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        UnlockScreenLock()
        mJob = Job()
        mainActivity = this
        navigationController = findNavController(this, R.id.nav_host_fragment)
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
                Log.i("Bluetooth", "successful setup: $mSuccessfulSetup")
            }
        })
        callbackViewModel.getTripHasEnded().observe(this, Observer { tripEnded ->
            if (tripEnded) {
                meterStateQueryComplete = false
                LoggerHelper.writeToLog("$logFragment, Trip ended and meterStateQueryComplete is set to false")
            }
        })
        turnOnBluetooth()
        checkNavBar()
        registerReceivers()
        LoggerHelper.getOrStartInternalLogs()
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
                if  (!vehicleSubscriptionComplete) {
                    Log.i("Results", "Vehicle subscription was started from re-sync")
                    subscribeToUpdateVehTripStatus(vehicleId)
                    subscribeToUpdatePimSettings()
                }
                if (currentTrip != null && currentTrip.tripID != "" && internetConnection) {
                    Log.i("Results", "Trip Id was updated on Main Activity from re-sync")
                    tripId = currentTrip.tripID
                }
            }
        })
        callbackViewModel.hasNewTripStarted().observe(this, Observer { hasTripStarted ->
            if (hasTripStarted) {
                val navController = findNavController(this, R.id.nav_host_fragment)
                if (navController.currentDestination?.id != R.id.welcome_fragment &&
                    navController.currentDestination?.id != R.id.taxi_number_fragment &&
                    navController.currentDestination?.id != R.id.bluetoothSetupFragment &&
                    navController.currentDestination?.id != R.id.startupFragment &&
                    navController.currentDestination?.id != R.id.vehicle_settings_detail_fragment &&
                    navController.currentDestination?.id != R.id.vehicleSetupFragment &&
                    navController.currentDestination?.id != R.id.checkVehicleInfoFragment &&
                    navigationController.currentDestination?.id != R.id.live_meter_fragment) {
                    val currentTripId = callbackViewModel.getTripId()
                    if(!isBluetoothOnAWS){
                        getMeterOwedQuery(currentTripId)
                    }
                    LoggerHelper.writeToLog("${logFragment}, New trip was started by the driver while the pim trip was not finished")
                    callbackViewModel.getMeterState().observe(this, Observer { meterValue ->
                        if(meterValue == MeterEnum.METER_ON.state){
                            sendDriverReceipt()
                            callbackViewModel.clearAllTripValues()
                            navController.navigate(R.id.action_global_taxi_number_fragment)
                            clearObserverOnMeter()
                        }
                    })
                } else {
                    Log.i(
                        "TripStart",
                        "current Nav destination is ${navController.currentDestination.toString()}. resync == $resync"
                    )
                    LoggerHelper.writeToLog("${logFragment}, Driver tried to start new trip, but the Pim was on Welcome/taxi number/bluetoothsetup screen")
                }
            }
            callbackViewModel.isPIMOverheating().observe(this, Observer {overheating ->
                if(overheating && !mOverheatEmailSent){
                    mOverheatEmailSent = true
                    val startTime = VehicleTripArrayHolder.pimStartTime
                    val overheat = VehicleTripArrayHolder.pimOverHeat
                    if(startTime != null && overheat != null){
                        OverHeatEmail.sendMail(vehicleId, startTime, overheat)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe{(LoggerHelper.writeToLog("Overheating email sent: overheat timeStamp:$overheat"))
                                VehicleTripArrayHolder.updateInternalPIMStatus(PIMStatusEnum.OVERHEATING.status)
                                val dataObject = NTSPimPacket.PimStatusObj()
                                val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
                                Log.i("Bluetooth", "status request packet to be sent == $statusObj")
                                Log.i("Bluetooth","Overheating email sent: overheat timeStamp:$overheat" )
                                sendBluetoothPacket(statusObj)
                            }
                    }
                }
            })
        })
        if (loggingTimer == null) {
            startTimerToSendLogsToAWS(vehicleId, this@MainActivity)
        }

        callbackViewModel.getIsPimOnline().observe(this, Observer { onlineStatus ->
            if(!onlineStatus && mSuccessfulSetup){
                clearAllSubscriptions()
            }
            if(onlineStatus &&
                !vehicleSubscriptionComplete &&
                mSuccessfulSetup){
                watchingTripId = ""
                if(!isBluetoothOnAWS) {
                    subscribeToUpdateVehTripStatus(vehicleId)
                }
                if(navigationController.currentDestination?.id == R.id.live_meter_fragment ||
                    navigationController.currentDestination?.id == R.id.trip_review_fragment){
                    if(!isBluetoothOnAWS){
                        getMeterOwedQuery(tripId)
                    }
                }
                subscribeToUpdatePimSettings()
            }
        })

        //Bluetooth Stuff
        BluetoothDataCenter.isBluetoothOn().observe(this, Observer { blueToothOn ->
            if(blueToothOn){
                isBluetoothOnAWS = true
                BluetoothDataCenter.getIsDeviceFound().observe(this, Observer {deviceIsFound ->
                    if(deviceIsFound){
                        val bAdapter = bluetoothAdapter
                        if(bAdapter != null) {
                            blueToothAddressDriverTablet = BluetoothDataCenter.getDriverTabletAddress()
                            if(blueToothAddressDriverTablet != "" && blueToothAddressDriverTablet != null){
                                driverTablet = bAdapter.getRemoteDevice(blueToothAddressDriverTablet)
                            } else {
                                Log.i("Bluetooth", "Issue with driver's bluetooth address. It is either blank or null when fetched to setup driver tablet connection. Driver Bt address == $blueToothAddressDriverTablet")
                                LoggerHelper.writeToLog("Bluetooth, Issue with driver's bluetooth address. It is either blank or null when fetched to setup driver tablet connection. Driver Bt address == $blueToothAddressDriverTablet")
                            }
                        }
                        val setupComplete = viewModel.isSetUpComplete()
                        if(driverTablet != null && setupComplete){
                                    Log.i("Bluetooth", "trying to start connect thread. connect thread status: Running: ${connectThread?.isAlive}")
                                    connectThread = ConnectThread(driverTablet!!, this)
                                    connectThread?.start()
                                    Log.i("Bluetooth", "Driver tablet != null and setup was complete. Starting ConnectThread")
                        } else {
                            Log.i("Bluetooth", "Connect thread didn't start. driverTablet: $driverTablet. Setup: $mSuccessfulSetup")
                            LoggerHelper.writeToLog("Connect thread didn't start. driverTablet: $driverTablet. Setup: $mSuccessfulSetup")
                        }
                    }
                    if(!deviceIsFound){
                        Log.i("Bluetooth", "Driver tablet was lost/not connected")
                        LoggerHelper.writeToLog("Driver tablet was lost/not connected")
                    }
                })
            }
        })
        BluetoothDataCenter.isBluetoothSocketConnected().observe(this, Observer { socketConnected ->
            if(isBluetoothOnAWS && socketConnected) {
                Log.i("Bluetooth", "Socket is connected and aws bluetooth is on. Creating read/write thread")
                LoggerHelper.writeToLog("Socket is connected and aws bluetooth is on. Creating read/write thread")
                val socket = BluetoothDataCenter.getBTSocket()
                if (socket != null) {
                    if (readThread == null) {
                        readThread = ReadThread(socket, this)
                        LoggerHelper.writeToLog("ReadThread is created. It was previously null on MainActivity")
                        Log.i("Bluetooth", "ReadThread is created. It was previously null on MainActivity")
                    } else {
                        Log.i("Bluetooth", "ReadThread was already created. ReadThread Status of is alive: ${readThread!!.isAlive}")
                    }
                    if (writeThread == null) {
                        writeThread = WriteThread(socket, this)
                        LoggerHelper.writeToLog("WriteThread is created. It was previously null on MainActivity")
                        Log.i("Bluetooth", "WriteThread is created. It was previously null on MainActivity")

                    }
                    else {
                        Log.i("Bluetooth", "WriteThread was already created. ReadThread Status of is alive: ${writeThread!!.isAlive}")
                    }
                    if (ackThread == null) {
                        ackThread = ACKThread(socket, this)
                        LoggerHelper.writeToLog("ACKThread is created. It was previously null on MainActivity")
                        Log.i("Bluetooth", "ACKThread is created. It was previously null on MainActivity")
                    } else {
                        Log.i("Bluetooth", "ACKThread was already created. ReadThread Status of is alive: ${ackThread!!.isAlive}")
                    }
                    readThread?.start()
                    LoggerHelper.writeToLog("ReadThread is started on MainActivity")
                    Log.i("Bluetooth", "ReadThread is started on  MainActivity")
                    writeThread?.start()
                    LoggerHelper.writeToLog("WriteThread is started on MainActivity")
                    Log.i("Bluetooth", "WriteThread is started on  MainActivity")
                    ackThread?.start()
                    LoggerHelper.writeToLog("ACKThread is started on MainActivity")
                    Log.i("Bluetooth", "ACKThread is started on  MainActivity")
                    requestDriverTabletStatus()
                    clearVehicleOfAWSTripSubscription()
                }
            }
            if(isBluetoothOnAWS && !socketConnected){
                val socket = BluetoothDataCenter.getBTSocket()
                if(socket == null){
                    Log.i("Bluetooth", "The socket connection hasn't happened for the first time")
                }
            }
        })
        BluetoothDataCenter.isConnectedToDriverTablet().observe(this, Observer { readWriteConnected ->
            if(!readWriteConnected){
                val socket = BluetoothDataCenter.getBTSocket()
                if(socket != null){
//                    connectThread?.cancel()
//                    Log.i("Bluetooth", "Connect_Thread is canceled on  MainActivity")
//                    connectThread = null
                    readThread?.cancel()
                    Log.i("Bluetooth", "Read_Thread is canceled on  MainActivity")
                    writeThread?.cancel()
                    Log.i("Bluetooth", "Write_Thread is canceled on  MainActivity")
                    ackThread?.cancel()
                    Log.i("Bluetooth", "ack_Thread is canceled on  MainActivity")
                    writeThread = null
                    readThread = null
                    ackThread = null
                    //This restart connection with same device will trigger the MainActivity: BluetoothDataCenter.getIsDeviceFound().observe(this, Observer {deviceIsFound ->
                    BluetoothDataCenter.restartConnectionWithSameDevice()
                    Log.i("Bluetooth", "Issue with read or write thread. Tablet lost connection")
                    LoggerHelper.writeToLog("Issue with read/write thread. Tablet lost connection")
                }
            }
        })
    }

    fun sendBluetoothPacket(ntsPimPacket: NTSPimPacket){
        val byteArrayToSend = toBytes(ntsPimPacket)
        Log.i("Bluetooth", "Sending packet to driver tablet. Command: ${ntsPimPacket.command} Data: ${ntsPimPacket.packetData}")
        LoggerHelper.writeToLog("Sending packet to driver tablet. Command: ${ntsPimPacket.command} Data: ${ntsPimPacket.packetData}")
        writeThread?.write(byteArrayToSend)

    }

    fun sendACKPacket(ntsPimPacket: NTSPimPacket){
        val byteArrayToSend = toBytes(ntsPimPacket)
        LoggerHelper.writeToLog("Main Activity: Sending ack packet code hit")
        ackThread?.write(byteArrayToSend)
    }

    fun requestDriverTabletStatus(){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val statusObj = NTSPimPacket(NTSPimPacket.Command.STATUS_REQ, null)
            Log.i("Bluetooth", "requesting status request from driver tablet")
            LoggerHelper.writeToLog("requesting status request from driver tablet")
            sendBluetoothPacket(statusObj)
        }
    }


    private fun toBytes(ntsPimPacket: NTSPimPacket): ByteArray? {
        var packet: ByteArray? = null
        val json: JSONObject
        val JSON_COMMAND = "Command"
        val JSON_DATA = "Data"
        val STX = 0x02
        val ETX = 0x03
        // First, create a JSON object that contains the command.
        try {
            json = JSONObject()
            json.put(JSON_COMMAND, ntsPimPacket.command)

            // If this packet has additional data, add it as a JSON object.
            if (ntsPimPacket.packetData != null) {
                json.put(JSON_DATA, ntsPimPacket.packetData!!.toJson())
            }

            // Convert JSON to string and then bytes and add to byte araay plus 2 bytes for STX and ETX.
            val data = json.toString().toByteArray()
            packet = ByteArray(data.size + 2)
            packet[0] = STX.toByte()
            System.arraycopy(data, 0, packet, 1, data.size)
            packet[1 + data.size] = ETX.toByte()
        } catch (e: Exception) {
        }
        return packet
    }

    private fun clearAllSubscriptions(){
       clearVehicleOfAWSTripSubscription()
        if (!isBluetoothOnAWS){
          clearVehicleOfAWSPIMSubscription()
        } else {
            LoggerHelper.writeToLog("PIM is suppose to use bluetooth. keeping subscription to pimSettings and Unpair pim")
            Log.i(
                "Bluetooth",
                "PIM is suppose to use bluetooth. keeping subscription to pimSettings and Unpair pim"
            )
        }
        LoggerHelper.writeToLog("Canceled all subscriptions")
        Log.i("$logFragment", "Canceled all subscriptions")
    }

    private fun clearVehicleOfAWSTripSubscription(){
        vehicleSubscriptionComplete = false
        tripSubscriptionComplete = false
        subscriptionWatcherDoPimPayment?.cancel()
        subscriptionWatcherDoPimPayment = null
        subscriptionWatcherUpdateVehTripStatus?.cancel()
        subscriptionWatcherUpdateVehTripStatus = null
        LoggerHelper.writeToLog("Canceled subscription to driver tablet for trip info")
        Log.i("$logFragment", "Canceled subscription to driver tablet for trip info")
    }

    private fun clearVehicleOfAWSPIMSubscription(){
        subscriptionWatcherUpdatePimSettings?.cancel()
        subscriptionWatcherUpdatePimSettings = null
        subscriptionWatcherUnPairPIM?.cancel()
        subscriptionWatcherUnPairPIM = null
        LoggerHelper.writeToLog("Canceled subscription to PIM in AWS")
        Log.i("$logFragment", "Canceled subscription to PIM in AWS")
    }

    internal fun restartDriverTabletAWSConnection(){
        // We use this in case the bluetooth connection is continually lost and it has already been set up
        //  or AWS has switched back to internet from bluetooth.
        vehicleSubscriptionComplete = false
        tripSubscriptionComplete = false
        val vehicleId = viewModel.getVehicleID()
        subscribeToUpdateVehTripStatus(vehicleId)
        val tripQuery = GetStatusQuery.builder().vehicleId(vehicleId).build()
        mAWSAppSyncClient?.query(tripQuery)?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)?.enqueue(getTripIdQuery)
    }
    private fun clearObserverOnMeter(){
        callbackViewModel.getMeterState().removeObservers(this)
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
                LoggerHelper.writeToLog("subscription for vehicle subscription was created. Subscribing to vehicle Id: $vehicleId")
                Log.i("vehicle_subscription", "subscription for vehicle subscription was created. Subscribing to vehicle Id: $vehicleId")
            } else {
                subscriptionWatcherUpdateVehTripStatus?.cancel()
                subscriptionWatcherUpdateVehTripStatus = mAWSAppSyncClient?.subscribe(subscription)
                subscriptionWatcherUpdateVehTripStatus?.execute(subscribeToUpdateVehCallback)
                LoggerHelper.writeToLog("subscription for vehicle subscription existed. Canceled prior subscription and subscribed to vehicle Id: $vehicleId")
                Log.i("vehicle_subscription", "subscription for vehicle subscription existed. Canceled prior subscription and subscribed to vehicle Id: $vehicleId")
            }
        }
        if(!unpairPIMSubscription){
            unpairPIMSubscription = true
            val unPairSubscription = OnPimUnpairSubscription.builder().vehicleId(vehicleId).build()
            if (subscriptionWatcherUnPairPIM == null){
                subscriptionWatcherUnPairPIM = mAWSAppSyncClient?.subscribe(unPairSubscription)
                subscriptionWatcherUnPairPIM?.execute(subscribeToUnpairPimSubscriptionCallback)
            } else {
                subscriptionWatcherUnPairPIM?.cancel()
                subscriptionWatcherUnPairPIM = mAWSAppSyncClient?.subscribe(unPairSubscription)
                subscriptionWatcherUnPairPIM?.execute(subscribeToUnpairPimSubscriptionCallback)
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
                Log.i("vehicle_subscription", "AWS trip Id wasn't null or blank. Subscripting to $awsTripId")
                subscribeToDoPIMPayment(awsTripId)
                tripId = awsTripId
            } else {
                if(navigationController.currentDestination?.id != R.id.welcome_fragment &&
                    navigationController.currentDestination?.id != R.id.bluetoothSetupFragment &&
                    !tripSubscriptionComplete){
                    val currentTrip = ModelPreferences(applicationContext)
                        .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
                    if(currentTrip != null){
                        tripId = currentTrip.tripID
                        insertTripId(currentTrip.tripID)
                        if(!isBluetoothOnAWS){
                            getMeterOwedQuery(tripId)
                            subscribeToDoPIMPayment(currentTrip.tripID)
                        }
                        Log.i("test", "trip subscription awsTripId was null so we needed to use the last trip Id to subscribe to")
                    }
                }
            }
        }

        override fun onFailure(e: ApolloException) {
            vehicleSubscriptionComplete = false
            subscriptionWatcherUpdateVehTripStatus?.cancel()
            subscriptionWatcherUpdateVehTripStatus = null
            LoggerHelper.writeToLog("On failure for subscription vehicle subscription. Error: ${e.message}")
        }

        override fun onCompleted() {}
    }


    private var subscribeToUnpairPimSubscriptionCallback = object: AppSyncSubscriptionCall.Callback<OnPimUnpairSubscription.Data> {
        override fun onResponse(response: Response<OnPimUnpairSubscription.Data>) {
            if (!response.hasErrors()) {
                val isPimPaired = response.data()?.onPIMUnpair()?.paired()!!
                if (!isPimPaired) {
                    insertPimPairedChange(isPimPaired)
                }
            } else {
                Log.i("Errors", "${response.errors()}")
            }

        }
        override fun onFailure(e: ApolloException) {
            unpairPIMSubscription = false
            subscriptionWatcherUnPairPIM?.cancel()
            subscriptionWatcherUnPairPIM = null

        }
        override fun onCompleted() {

        }
    }

    private fun subscribeToDoPIMPayment(tripId: String){
        if (tripId != watchingTripId && tripId != ""){
            tripSubscriptionComplete = true
            val tripSubscription = OnDoPimPaymentSubscription.builder().tripId(tripId).build()
            if(subscriptionWatcherDoPimPayment == null) {
                subscriptionWatcherDoPimPayment = mAWSAppSyncClient?.subscribe(tripSubscription)
                subscriptionWatcherDoPimPayment?.execute(doPimPaymentCallback)
                LoggerHelper.writeToLog("subscription for do pim payment was created. Subscribing to trip Id: $tripId")
                Log.i("Vehicle_Subscription", "subscription for do pim payment was created. Subscribing to trip Id: $tripId")
            } else {
                subscriptionWatcherDoPimPayment?.cancel()
                subscriptionWatcherDoPimPayment = mAWSAppSyncClient?.subscribe(tripSubscription)
                subscriptionWatcherDoPimPayment?.execute(doPimPaymentCallback)
                LoggerHelper.writeToLog("subscription for do pim payment existed. Canceled prior do pim payment and subscribing to trip Id: $tripId")
                Log.i("Vehicle_Subscription","subscription for do pim payment existed. Canceled prior do pim payment and subscribing to trip Id: $tripId" )
            }

        } else {
            LoggerHelper.writeToLog("couldn't subscribe to do pim payment since trip id: $tripId == $watchingTripId or tripId == empty string")
            Log.i("Vehicle_Subscription", "couldn't subscribe to do pim payment since trip id: $tripId == $watchingTripId or tripId == empty string")

        }
    }
    private var doPimPaymentCallback = object: AppSyncSubscriptionCall.Callback<OnDoPimPaymentSubscription.Data>{
        override fun onResponse(response: Response<OnDoPimPaymentSubscription.Data>) {
            val meterState = response.data()?.onDoPimPayment()?.meterState()
            val pimNoReceipt = response.data()?.onDoPimPayment()?.pimNoReceipt()
            val pimPaymentAmount = response.data()?.onDoPimPayment()?.pimPayAmt()
            val tripNumber = response.data()?.onDoPimPayment()?.tripNbr()
            val transactionId = response.data()?.onDoPimPayment()?.pimTransId()
            val pimPaidAmount = response.data()?.onDoPimPayment()?.pimPaidAmt()
            val driverId = response.data()?.onDoPimPayment()?.driverId()
            val paymentType = response.data()?.onDoPimPayment()?.paymentType()

            if (tripNumber != null){
                insertTripNumber(tripNumber)
                //we get the transactionId when we get the trip number.
                if (!transactionId.isNullOrBlank()){
                    insertTransactionID(transactionId)
                }
            }
            if(pimPaymentAmount != null){
                insertPimPayAmount(pimPaymentAmount)
            }
            if(pimPaidAmount != null){
                insertPimPaidAmount(pimPaidAmount)
            }
            if (meterState != null) {
                insertMeterState(meterState)
            }
            if(pimNoReceipt != null
                && pimNoReceipt.trim() == "Y"){
                insertPimNoReceipt(true)
            }
            if(driverId != null){
                insertDriverId(driverId)
            }
            if(paymentType != null){
                insertPaymentType(paymentType)
            }
        }

        override fun onFailure(e: ApolloException) {
            tripSubscriptionComplete = false
            subscriptionWatcherDoPimPayment?.cancel()
            subscriptionWatcherDoPimPayment = null
            LoggerHelper.writeToLog("On failure for onDoPimPayment subscription. Error: ${e.message}")
        }
        override fun onCompleted() {

        }
    }
    private var getTripIdQuery = object: GraphQLCall.Callback<GetStatusQuery.Data>() {
        override fun onResponse(response: Response<GetStatusQuery.Data>) {
           val queriedId = response.data()?.status?.tripId() ?: ""
            if(queriedId != ""){
                getMeterOwedQuery(queriedId)
                subscribeToDoPIMPayment(queriedId)
            }
        }

        override fun onFailure(e: ApolloException) {

        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToUpdatePimSettings(){
        val deviceId  = DeviceIdCheck.getDeviceId()
        val subscription =  OnPimSettingsUpdateSubscription.builder().deviceId(deviceId).build()
        if (deviceId != null) {
            subscriptionWatcherUpdatePimSettings = mAWSAppSyncClient?.subscribe(subscription)
            subscriptionWatcherUpdatePimSettings?.execute(updatePimSettingsCallback)
            Log.i("LOGGER","Tried to subscribe to updatePIM with deviceID $deviceId")
            LoggerHelper.writeToLog("Tried to subscribe to updatePIM with deviceID $deviceId")
        }
    }

    private var updatePimSettingsCallback = object : AppSyncSubscriptionCall.Callback<OnPimSettingsUpdateSubscription.Data>{
        @SuppressLint("RestrictedApi")
        override fun onResponse(response: Response<OnPimSettingsUpdateSubscription.Data>) {
            if(!response.hasErrors()){
                val awsLog = response.data()?.onPIMSettingsUpdate()?.log()!!
                //We will use this value if the bluetooth is changed in fleet mgmt portal
                val useBluetooth = response.data()?.onPIMSettingsUpdate()?.useBluetooth()
                LoggerHelper.logging = awsLog
                if(awsLog){
                    launch(Dispatchers.IO) {
                        LoggerHelper.addInternalLogsToAWS(vehicleId)
                    }
                }
                if(useBluetooth != null){
                    if(!isBluetoothOnAWS && useBluetooth){
                        Log.i("Bluetooth", "The aws subscription flipped to on for bluetooth. Starting bluetooth setup process.")
                        LoggerHelper.writeToLog("The aws subscription flipped to on for bluetooth. Starting bluetooth setup process.")
                        closeBluetoothThreads()
                        isBluetoothOnAWS = true
                        BluetoothDataCenter.blueToothSocketIsDisconnected()
                        BluetoothDataCenter.resetBluetoothPairedObserver()
                        BluetoothDataCenter.turnOnBlueTooth()
                        BluetoothDataCenter.connectedToDriverTablet()
                        val deviceId = DeviceIdCheck.getDeviceId() ?: ""
                        getDriverTabletBluetoothAddress(deviceId)
                    }
                    if(isBluetoothOnAWS && !useBluetooth) {
                        Log.i("Bluetooth", "The aws subscription flipped to OFF for bluetooth. Restarting internet Connection.")
                        LoggerHelper.writeToLog("The aws subscription flipped to OFF for bluetooth. Restarting internet Connection.")
                        BluetoothDataCenter.turnOffBlueTooth()
                        BluetoothDataCenter.disconnectedToDriverTablet()
                        isBluetoothOnAWS = false
                        resync = false
                        clearVehicleOfAWSTripSubscription()
                        restartDriverTabletAWSConnection()
                    }
                }
                Log.i("Logging", "Logging == $awsLog from updatePimSettingsCallBack")
            }
            if(response.hasErrors()){
                Log.i("LOGGER", "ERROR SUBSCRIBING TO UPDATING PIM SETTINGS ${response.errors()[0].message()}")
                LoggerHelper.writeToLog("ERROR SUBSCRIBING TO UPDATING PIM SETTINGS ${response.errors()[0].message()}")
            }
        }
        override fun onFailure(e: ApolloException) {
            subscriptionWatcherUpdatePimSettings?.cancel()
            subscriptionWatcherUpdatePimSettings = null
            LoggerHelper.writeToLog("On failure for onPimUpdatePImSettings subscription. Error: ${e.message}")
        }

        override fun onCompleted() {

        }
    }

    fun closeBluetoothThreads(){
        connectThread = null
        readThread = null
        writeThread = null
        ackThread = null
    }

    private fun getMeterOwedQuery(tripId: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(this@MainActivity.applicationContext)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(getTripQueryCallBack)
    }
    private var getTripQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (response.data() != null &&
                !response.hasErrors()
            ) {

                val pimPayAmount = response.data()?.trip?.pimPayAmt()
                val meterState = response.data()?.trip?.meterState()
                if (meterState != null || meterState != "") {
                    insertMeterState(meterState!!)
                }
                if(pimPayAmount != null || pimPayAmount != 0.0){
                    insertPimPayAmount(pimPayAmount!!)
                }
            }
        }
        override fun onFailure(e: ApolloException) {
        }
    }
    //Coroutine to insert Meter Value
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
    private fun insertPimPairedChange(change: Boolean) = launch {
        callbackViewModel.pimPairingValueChangedViaFMP(change)
    }
    private fun insertPaymentType(paymentType: String){
        callbackViewModel.setPaymentMethod(paymentType)
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
    private fun startTimerToSendLogsToAWS(vehicleId: String, context: Context){
        val logTimerTime = LoggerHelper.loggingTime
        loggingTimer = object: CountDownTimer(logTimerTime, 1000){
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                Log.i("LOGGER", "Log Timer: onFinish, Logging time:$logTimerTime")
                launch(Dispatchers.IO) {
                    LoggerHelper.sendLogToAWS(vehicleId)
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

    @SuppressLint("MissingPermission")
    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun registerReceivers(){
        // Internet Receiver
        mNetworkReceiver = NetworkReceiver()
        registerReceiver(mNetworkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        //Battery receiver
        mBatteryReceiver = BatteryPowerReceiver()
        registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        mBluetoothReceiver = BluetoothReceiver()
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(mBluetoothReceiver, filter)

    }
    private fun recheckInternetConnection(context: Context){
        if(internetConnectionTimer == null){
            internetConnectionTimer = object: CountDownTimer(5000, 1000){
                override fun onTick(millisUntilFinished: Long) {
                    internetConnection = isOnline(context)
                    if(!internetConnection){
                        subscriptionWatcherDoPimPayment?.cancel()
                        subscriptionWatcherDoPimPayment = null
                        vehicleSubscriptionComplete = false
                        subscriptionWatcherUpdateVehTripStatus?.cancel()
                        subscriptionWatcherUpdateVehTripStatus = null
                    }
                }
                override fun onFinish() {
                    if(!internetConnection){
                        LoggerHelper.writeToLog("$logFragment, recheck internet connection timer finished. internet was not connected. retrying in 5 seconds")
                        recheckInternetConnection(this@MainActivity)
                        // this is for a resync of trip
                    } else if (resync) {
                        val currentTrip = ModelPreferences(applicationContext)
                            .getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
                        LoggerHelper.writeToLog("$logFragment, recheck internet connection timer finished. Internet is connected. Trying to start subscription on ${vehicleId} due to resync.")
                        subscribeToUpdateVehTripStatus(vehicleId)
                        if (currentTrip != null && currentTrip.tripID != "" && internetConnection){
                            LoggerHelper.writeToLog("$logFragment, recheck internet connection timer finished.Internet is connected. Trying to start subscription on ${currentTrip.tripID} due to resync.")
                            resync = false
                        }
                    }
                    internetConnectionTimer = null
                }
            }.start()
        }
    }

    private fun sendDriverReceipt() = launch(Dispatchers.IO){
        val tripIdForPayment = VehicleTripArrayHolder.getTripIdForPayment()
        val tripIdForReceipt = if(tripIdForPayment != ""){
            tripIdForPayment
        } else {
            callbackViewModel.getTripId()
        }
        var transactionType = VehicleTripArrayHolder.paymentTypeSelected
        if(transactionType == "none"){
            transactionType = PaymentTypeEnum.CASH.paymentType
        }
        var transactionId = callbackViewModel.getTransactionId()
        if(transactionId == ""){
            transactionId = UUID.randomUUID().toString()
        }
        DriverReceiptHelper.sendReceipt(tripIdForReceipt,transactionType, transactionId)
    }

    private fun checkNavBar(){
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener((View.OnSystemUiVisibilityChangeListener {
            if(mSuccessfulSetup){
                ViewHelper.hideSystemUI(this@MainActivity)
            }
        }))
    }
    override fun onBackPressed() {
        LoggerHelper.writeToLog("${logFragment}, back button on nav bar pressed")
        Log.i("Back Button", "Back button was pressed")
    }

    @SuppressLint("MissingPermission")
    private fun turnOnBluetooth(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null){
            LoggerHelper.writeToLog("$logFragment, bluetooth is not supported on this device")
            Log.i("$logFragment", "bluetooth is not supported on this device")
            return
        }
        if (!mBluetoothAdapter.isEnabled) {
            LoggerHelper.writeToLog("${logFragment}, bluetooth was off, turned on programmatically")
            mBluetoothAdapter.enable()
        } else {
            LoggerHelper.writeToLog("${logFragment}, bluetooth was on during start up")
        }
    }

    private fun getDriverTabletBluetoothAddress(deviceId: String){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(applicationContext)
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

    override fun onDestroy() {
        Log.i("SubscriptionWatcher", "Subscription watcher canceled for $vehicleId")
        subscriptionWatcherUpdateVehTripStatus?.cancel()
        viewModel.isSquareAuthorized().removeObservers(this)
        viewModel.watchSetUpComplete().removeObservers(this)
        callbackViewModel.getTripHasEnded().removeObservers(this)
        callbackViewModel.getIsPimOnline().removeObservers(this)
        callbackViewModel.isPIMOverheating().removeObservers(this)
        unregisterReceiver(mNetworkReceiver)
        unregisterReceiver(mBatteryReceiver)
            unregisterReceiver(mBluetoothReceiver)
        stopLogTimer()
        vehicleSubscriptionTimer?.cancel()
        LoggerHelper.writeToLog("$logFragment, MainActivity onDestroy hit")
        super.onDestroy()
    }

    override fun onStop() {
        Log.i("onStop", "onStop was hit")
        LoggerHelper.writeToLog("$logFragment, onStop was hit for main Activity.")
        super.onStop()
    }


    override fun onPause() {
        super.onPause()
        if(mSuccessfulSetup){
            ViewHelper.hideSystemUI(this)
        }
        LoggerHelper.writeToLog("$logFragment, MainActivity onPause hit")
    }

    override fun onResume() {
        super.onResume()
        if (mSuccessfulSetup) {
            ViewHelper.hideSystemUI(this)
        }
    }
}

