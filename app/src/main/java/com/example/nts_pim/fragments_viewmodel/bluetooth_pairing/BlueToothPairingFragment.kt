package com.example.nts_pim.fragments_viewmodel.bluetooth_pairing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation

import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.ConnectThread
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.fragment_blue_tooth_pairing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.lang.IllegalArgumentException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class BlueToothPairingFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val logTag = "Bluetooth_Pairing_Fragment"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var navController: NavController? = null
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private val currentFragmentId = R.id.blueToothPairingFragment
    private var tabletConnected = false
    private var vehicleId: String? = null
    private var isBluetoothOn:Boolean? = null
    private var discoveryTimer: CountDownTimer? = null



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
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(VehicleSetupViewModel::class.java)
        isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value
        vehicleId = viewModel.getVehicleID()
        bt_connected_textView.text = "Pair with driver tablet: $isBluetoothOn"
        bt_description_textView.text = "Searching for $vehicleId..."


        if(!isBluetoothOn!!){
            Log.i("${logTag}", "Bluetooth pairing is off. Going to welcome screen")
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is off. Going to welcome screen")
            toWelcomeScreen()
        } else {
            Log.i("${logTag}", "Bluetooth pairing is on. Starting pairing process")
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is on. Starting pairing process")
            bluetoothAdapter?.startDiscovery()
            startDiscoveryTimer()
           // BlueToothHelper.getPairedDevicesAndRegisterBTReceiver(requireActivity())
        }

        BluetoothDataCenter.isConnectedToDriverTablet().observe(this.viewLifecycleOwner, Observer { tabletConnected ->
            if (tabletConnected){
                toWelcomeScreen()
            }
        })
    }

    private fun startDiscoveryTimer(){
          discoveryTimer = object : CountDownTimer(10000, 1000){
            override fun onTick(p0: Long) {

            }

            override fun onFinish() {
                if(!bluetoothAdapter!!.isDiscovering && !tabletConnected){
                    bluetoothAdapter.startDiscovery()
                }
                startDiscoveryTimer()
            }
        }.start()
    }

    private fun destroyDiscoveryTimer(){
        discoveryTimer?.cancel()
        discoveryTimer = null
    }


    // Start bluetoothPair
   //Navigation
   private fun toWelcomeScreen() = launch(Dispatchers.Main.immediate){
        destroyDiscoveryTimer()
       if (navController?.currentDestination?.id == currentFragmentId) {
           navController?.navigate(R.id.action_blueToothPairingFragment_to_welcome_fragment)
       }
   }

    override fun onDestroy() {
        BluetoothDataCenter.isConnectedToDriverTablet().removeObservers(this)
        Log.i(logTag, "Discovery timer canceled")
        discoveryTimer?.cancel()
        super.onDestroy()
    }

}
