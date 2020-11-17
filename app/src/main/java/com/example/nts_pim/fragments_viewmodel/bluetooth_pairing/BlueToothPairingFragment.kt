package com.example.nts_pim.fragments_viewmodel.bluetooth_pairing

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.fragment_blue_tooth_pairing.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class BlueToothPairingFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val logTag = "Bluetooth_Pairing_Fragment"
    private var navController: NavController? = null
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var viewModel: VehicleSetupViewModel
    private val currentFragmentId = R.id.blueToothPairingFragment
    private var vehicleId: String? = null
    private var isBluetoothOn:Boolean? = null
    private var deviceId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blue_tooth_pairing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this, viewModelFactory).get(VehicleSetupViewModel::class.java)
        isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value
        vehicleId = viewModel.getVehicleID()
        deviceId = DeviceIdCheck.getDeviceId() ?: ""
        VehicleTripArrayHolder.updateInternalPIMStatus(PIMStatusEnum.PIM_PAIRING.status)
        bt_connected_textView.text = "Pair with driver tablet: $isBluetoothOn"
        bt_description_textView.text = "Searching for $vehicleId..."
        if(!isBluetoothOn!!){
            Log.i("${logTag}", "Bluetooth pairing is off. Going to welcome screen")
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is off. Going to welcome screen")
            toWelcomeScreen()
        } else {
            Log.i("${logTag}", "Bluetooth pairing is on. Starting pairing process")
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is on. Starting pairing process")
            getDriverTabletBluetoothAddress(deviceId!!)
        }

        BluetoothDataCenter.isConnectedToDriverTablet().observe(this.viewLifecycleOwner, Observer { tabletConnected ->
            if (tabletConnected){
                val dataObject = NTSPimPacket.PimStatusObj()
                val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
                Log.i("Bluetooth", "status request packet to be sent == $statusObj")
                (activity as MainActivity).sendBluetoothPacket(statusObj)
                toWelcomeScreen()
            }
        })
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


   //Navigation
   private fun toWelcomeScreen() = launch(Dispatchers.Main.immediate){
       if (navController?.currentDestination?.id == currentFragmentId) {
           navController?.navigate(R.id.action_blueToothPairingFragment_to_welcome_fragment)
       }
   }

    override fun onDestroy() {
        BluetoothDataCenter.isConnectedToDriverTablet().removeObservers(this)
        Log.i(logTag, "Discovery timer canceled")
        super.onDestroy()
    }



    }
