package com.example.nts_pim.data.repository

import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.startup.StartupRequirement

/**
 * pointer for all startup procedures
 */
object SetupHolder {
    private var stepOneList = mutableListOf<StartupRequirement>()
    private var stepTwoList = mutableListOf<StartupRequirement>()
    private var stepThreeList = mutableListOf<StartupRequirement>()
    private var permissionsCheckedMLD = MutableLiveData<Boolean>()
    private var isInternetConnectedMLD = MutableLiveData<Boolean>()
    private var isPairedMLD = MutableLiveData<Boolean>()
    private var isSubscribedToAWSMLD = MutableLiveData<Boolean>()
    private var isPimSettingsUpdatedMLD = MutableLiveData<Boolean>()
    private var pimBluetoothOnMLD = MutableLiveData<Boolean>()
    private var bluetoothAdapterAvailableMLD = MutableLiveData<Boolean>()
    private var squareInitMLD = MutableLiveData<Boolean>()
    private var squareAuthorizedMLD = MutableLiveData<Boolean>()
    private var contactedReaderMLD = MutableLiveData<Boolean>()
    private var readerStatusFoundMLD = MutableLiveData<Boolean>()
    private var updatedAWSWithReaderStatusMLD = MutableLiveData<Boolean>()
    private var driverBTAddressCorrectMLD = MutableLiveData<Boolean>()
    private var foundDriverTabletMLD =  MutableLiveData<Boolean>()
    private var sentConnectionPacketMLD = MutableLiveData<Boolean>()
    private var receivedConnectionPacketMLD = MutableLiveData<Boolean>()
    private var bluetoothConnectionCompleteMLD = MutableLiveData<Boolean>()

    private var startupErrorMLD = MutableLiveData<String>()

    private var permissionsReq: StartupRequirement? = null
    private var internetReq: StartupRequirement? = null
    private var pairedReq: StartupRequirement? = null
    private var awsReq: StartupRequirement? = null
    private var pimSettingsReq: StartupRequirement? = null
    private var blueToothOnReq: StartupRequirement? = null
    private var blueToothAdapReq: StartupRequirement? = null
    private var squareInitReq: StartupRequirement? = null
    private var squareAuthorizedReq: StartupRequirement? = null
    private var contactedReaderReq: StartupRequirement? = null
    private var readerStatusFoundReq: StartupRequirement? = null
    private var updatedAWSWithReaderStatusReq: StartupRequirement? = null
    private var driverBTAddressCorrectReq: StartupRequirement? = null
    private var foundDriverTabletReq: StartupRequirement? = null
    private var sentConnectionPacketReq: StartupRequirement?  = null
    private var receivedConnectionPacketReq: StartupRequirement? = null
    private var bluetoothConnectionCompleteReq: StartupRequirement? = null


    init {
        //These are for keeping track of UI elements
         permissionsReq = StartupRequirement("Permissions valid", false)
         internetReq = StartupRequirement("Internet connection", false)
         pairedReq = StartupRequirement("Paired to vehicle", false)
         awsReq = StartupRequirement("Subscribing to AWS and Logging Setup", false)
         pimSettingsReq = StartupRequirement("Getting/Updating PIM Settings", false)
         stepOneList = mutableListOf(permissionsReq!!, internetReq!!, pairedReq!!, awsReq!!, pimSettingsReq!!)

        squareInitReq = StartupRequirement("Square Reader SDK initialized", false)
        blueToothOnReq = StartupRequirement("Turn on tablet bluetooth", false)
        blueToothAdapReq = StartupRequirement("Check bluetooth adapter availability", false)
        squareAuthorizedReq = StartupRequirement("Authorized with square", false)
        contactedReaderReq= StartupRequirement("Contacting reader for status", false)
        readerStatusFoundReq = StartupRequirement("Reader status found", false)
        updatedAWSWithReaderStatusReq = StartupRequirement("Updated reader status in AWS", false)

        stepTwoList =
            mutableListOf(squareInitReq!!, blueToothOnReq!!, blueToothAdapReq!!,squareAuthorizedReq!!, contactedReaderReq!!, readerStatusFoundReq!!, updatedAWSWithReaderStatusReq!!)

        driverBTAddressCorrectReq = StartupRequirement("Validating Dispatch App bluetooth address", false)
        foundDriverTabletReq = StartupRequirement("Connecting to driver tablet", false)
        sentConnectionPacketReq = StartupRequirement("Sent BT packet", false)
        receivedConnectionPacketReq = StartupRequirement("Received BT Packet", false)
        bluetoothConnectionCompleteReq = StartupRequirement("Bluetooth connection successful", false)
        stepThreeList = mutableListOf(driverBTAddressCorrectReq!!, foundDriverTabletReq!!, sentConnectionPacketReq!!, receivedConnectionPacketReq!!, bluetoothConnectionCompleteReq!!)
    }

    fun getStepOneList() = stepOneList

    fun getStepTwoList() = stepTwoList

    fun getStepThreeList() = stepThreeList

    fun permissionsChecked(){
        permissionsCheckedMLD.postValue(true)
        permissionsReq?.complete = true
    }
    fun permissionsNotCorrect(deniedPermission: String){
        permissionsCheckedMLD.postValue(false)
        startupErrorMLD.postValue("Issue with permission. $deniedPermission")
        permissionsReq?.complete = false
    }

    fun doesPIMHavePermissions() = permissionsCheckedMLD

    fun internetIsConnected(){
        isInternetConnectedMLD.postValue(true)
        internetReq?.complete = true
    }

    fun internetDisconnected(){
        isInternetConnectedMLD.postValue(false)
        startupErrorMLD.postValue("Internet connection can not be established")
        internetReq?.complete = false
    }

    fun doesPIMHaveInternet() = isInternetConnectedMLD

    fun isPaired(){
        isPairedMLD.postValue(true)
        pairedReq?.complete = true
    }

    fun isNotPaired(vehicleId: String?){
        isPairedMLD.postValue(false)
        startupErrorMLD.postValue("Issue Pairing to vehicle: $vehicleId")
        pairedReq?.complete = false
    }

    fun isPIMSetupAndPaired() = isPairedMLD

    fun subscribedToAWS(){
        isSubscribedToAWSMLD.postValue(true)
        awsReq?.complete = true
    }

    fun isNotSubscribedToAWS(error: String?){
        isSubscribedToAWSMLD.postValue(false)
        startupErrorMLD.postValue("Issue subscribing to AWS. Error: $error")
        awsReq?.complete = false
    }

    fun isPIMSubscribedToAWS() = isSubscribedToAWSMLD

    fun pimSettingsAreUpdated(){
        isPimSettingsUpdatedMLD.postValue(true)
        pimSettingsReq?.complete = true
    }

    fun pimSettingsNotUpdated(error: String?){
        isPimSettingsUpdatedMLD.postValue(false)
        startupErrorMLD.postValue("Issue getting/updating pim settings from AWS. Error: $error")
        pimSettingsReq?.complete = false
    }

    fun doesPIMHaveSettings() = isPimSettingsUpdatedMLD


    fun pimBluetoothIsOn(){
        pimBluetoothOnMLD.postValue(true)
        blueToothOnReq?.complete = true
    }

    fun pimBluetoothIsOff(){
        pimBluetoothOnMLD.postValue(false)
        startupErrorMLD.postValue("Bluetooth is off")
        blueToothOnReq?.complete = false
    }

    fun isBluetoothON() = pimBluetoothOnMLD

    fun blueToothAdapterIsReady(){
        bluetoothAdapterAvailableMLD.postValue(true)
        blueToothAdapReq?.complete = true
    }

    fun blueToothAdapterNotReady(){
        bluetoothAdapterAvailableMLD.postValue(false)
        startupErrorMLD.postValue("Bluetooth adapter is having issues and is not available")
        blueToothAdapReq?.complete = false

    }

    fun isBluetoothAdapterReady() = bluetoothAdapterAvailableMLD


    fun squareIsInit(){
        squareInitMLD.postValue(true)
        squareInitReq?.complete = true
    }

    fun squareNoInit(){
        squareInitMLD.postValue(false)
        startupErrorMLD.postValue("Square did not init")
        squareInitReq?.complete = false
    }

    fun isSquareInit() = squareInitMLD


    fun isAuthorized(){
        squareAuthorizedMLD.postValue(true)
        squareAuthorizedReq?.complete = true
    }

    fun notAuthorized(){
        squareAuthorizedMLD.postValue(false)
        startupErrorMLD.postValue("square is not Authorized on this device")
        squareAuthorizedReq?.complete = false
    }

    fun isAuthorizedWithSquare() = squareAuthorizedMLD

    fun contactedChipReader(){
        contactedReaderMLD.postValue(true)
        contactedReaderReq?.complete = true
    }

    fun unsuccessfulContactWithSquareReader(){
        contactedReaderMLD.postValue(false)
        startupErrorMLD.postValue("Could not contact square reader to check status")
        contactedReaderReq?.complete = false
    }

    fun hasContactedReader() = contactedReaderMLD

    fun foundReaderStatus(status: String){
        readerStatusFoundMLD.postValue(true)
        readerStatusFoundReq?.name = "Reader Status: $status"
        readerStatusFoundReq?.complete = true
    }

    fun noReaderStatus(lastReaderStatus: String){
        readerStatusFoundMLD.postValue(false)
        startupErrorMLD.postValue("Reader status was not acceptable. Last status reading $lastReaderStatus")
        readerStatusFoundReq?.complete = false
    }

    fun hasFoundReaderStatus() = readerStatusFoundMLD


    fun updatedAWSWithReaderStatus(){
        updatedAWSWithReaderStatusMLD.postValue(true)
        updatedAWSWithReaderStatusReq?.complete = true
    }

    fun didNotUpdateAWSWithReaderStatus(error: String?){
        updatedAWSWithReaderStatusMLD.postValue(false)
        startupErrorMLD.postValue("Issue updating AWS with reader status. Error: $error ")
        updatedAWSWithReaderStatusReq?.complete = false
    }

    fun hasUpdatedAWSWithReaderStatus() = updatedAWSWithReaderStatusMLD

    fun driverBTAddressIsCorrect(driverAddress: String){
        driverBTAddressCorrectMLD.postValue(true)
        driverBTAddressCorrectReq?.name = "Driver BT: $driverAddress"
        driverBTAddressCorrectReq?.complete = true
    }

    fun driverBTAddressNotCorrect(incorrectDriverAddress: String){
        driverBTAddressCorrectMLD.postValue(false)
        startupErrorMLD.postValue("Incorrect formatting for Driver BT Address: $incorrectDriverAddress")
        driverBTAddressCorrectReq?.complete = false
    }

    fun isDriverBTAddressCorrect() = driverBTAddressCorrectMLD

    fun foundDriverTablet(){
        foundDriverTabletMLD.postValue(true)
        foundDriverTabletReq?.name = "Found Driver tablet"
        foundDriverTabletReq?.complete = true
    }

    fun noDriverTabletFound(){
        foundDriverTabletMLD.postValue(false)
        foundDriverTabletReq?.name = "searching for driver tablet..."
        foundDriverTabletReq?.complete = false
    }

    fun didFindDriverTablet() = foundDriverTabletMLD

    fun sentTestPacket(){
        sentConnectionPacketMLD.postValue(true)
        sentConnectionPacketReq?.complete = true
    }

    fun didNotSendTestPacket(error: String){
        sentConnectionPacketMLD.postValue(false)
        startupErrorMLD.postValue("Error sending test BT packet: $error")
        sentConnectionPacketReq?.complete = false
    }

    fun didSendTestPacket() = sentConnectionPacketMLD

    fun receivedTestPacket(){
        receivedConnectionPacketMLD.postValue(true)
        receivedConnectionPacketReq?.complete = true
    }

    fun didNotReceiveTestPacket(error: String){
        receivedConnectionPacketMLD.postValue(false)
        startupErrorMLD.postValue("Error receiving test BT packet: $error")
        receivedConnectionPacketReq?.complete = false
    }

    fun didReceiveTestPacket() = receivedConnectionPacketMLD


    fun bluetoothConnectionFinished(){
        bluetoothConnectionCompleteMLD.postValue(true)
        bluetoothConnectionCompleteReq?.complete = true
    }

    fun issueWithBTConnection(error: String){
        bluetoothConnectionCompleteMLD.postValue(false)
        startupErrorMLD.postValue("Issue with BT Connection: $error")
        bluetoothConnectionCompleteReq?.complete = false

    }

    fun isBluetoothConnectionFinished() = bluetoothConnectionCompleteMLD

    fun clearStartupList(): Boolean {
       stepOneList.forEach {requirement ->
           requirement.complete = false

       }
        stepTwoList.forEach { requirement ->
            requirement.complete = false

        }

        stepThreeList.forEach {requirement ->
            requirement.complete = false
        }
        readerStatusFoundReq?. name = "Reader status found"
        driverBTAddressCorrectReq?.name = "Validating Dispatch App bluetooth address"
        foundDriverTabletReq?.name = "Connecting to driver tablet"

        permissionsCheckedMLD.postValue(false)
        isInternetConnectedMLD.postValue( false)
        isPairedMLD.postValue(false)
        isSubscribedToAWSMLD.postValue( false)
        isPimSettingsUpdatedMLD.postValue(false)
        pimBluetoothOnMLD.postValue(false)
        bluetoothAdapterAvailableMLD.postValue(false)
        squareInitMLD.postValue(false)
        squareAuthorizedMLD.postValue(false)
        contactedReaderMLD.postValue(false)
        readerStatusFoundMLD.postValue(false)
        updatedAWSWithReaderStatusMLD.postValue(false)
        driverBTAddressCorrectMLD.postValue(false)
        foundDriverTabletMLD.postValue(false)
        sentConnectionPacketMLD.postValue(false)
        receivedConnectionPacketMLD.postValue(false)
        bluetoothConnectionCompleteMLD.postValue(false)

        return true
    }

}