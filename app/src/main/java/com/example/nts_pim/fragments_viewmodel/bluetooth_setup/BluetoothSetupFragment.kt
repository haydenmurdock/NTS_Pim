package com.example.nts_pim.fragments_viewmodel.bluetooth_setup

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothServerController
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothServer
import kotlinx.android.synthetic.main.bluetooth_setup_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothSetupFragment: ScopedFragment() {
    var mArrayAdapter: ArrayAdapter<String>? = null
    var message = ""
    var devices = ArrayList<String>()
    private var navController: NavController? = null
    private lateinit var callBackViewModel: CallBackViewModel
    private val currentFragmentId = R.id.bluetoothSetupFragment


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bluetooth_setup_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        val pairedDevices = BlueToothHelper.getPairedDevicesAndRegisterBTReceiver(activity!!)
        devices = ArrayList()
        mArrayAdapter = ArrayAdapter(activity, R.layout.dialog_select_bluetooth_device)
        navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        launch(Dispatchers.Main.immediate){
            setUpBluetoothServer(activity!!)
        }
        pairedDevices.forEach { device ->
            devices.add(device.first)
            mArrayAdapter!!.add((if (device.first != null) device.first else "Unknown") + "\n" + device.second + "\nPaired")
        }
        blueToothSetup_button.setOnClickListener {
            if (BluetoothAdapter.getDefaultAdapter().startDiscovery()) {
                val dialog = SelectDeviceDialog(mArrayAdapter!!, devices)
                if (this.fragmentManager != null) {
                    dialog.show(this.fragmentManager!!, "select a device")
                    devices.clear()
                }
            }
        }
       BluetoothDataCenter.getResponseMessage().observe(this.viewLifecycleOwner, Observer { tripStatus ->
            bluetoothFragment_messageReceivedTextView.text = tripStatus
        })
        toWelcomeScreen()
    }
    //Navigation
    private fun setUpBluetoothServer(activity: Activity){
        BlueToothServerController(activity).start()
    }

    private fun toWelcomeScreen(){
        if (navController?.currentDestination?.id == currentFragmentId) {
            navController?.navigate(R.id.action_bluetoothSetupFragment_to_welcome_fragment)
        }
    }

    override fun onDestroy() {
        if(view != null){
            callBackViewModel.getTripStatus().removeObservers(this.viewLifecycleOwner)
        }
        super.onDestroy()
    }
}
class SelectDeviceDialog(private val mArrayAdapter: ArrayAdapter<String>, private val devices: ArrayList<String>) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(this.activity)
        builder.setTitle("List of Paired Devices")

        builder.setAdapter(mArrayAdapter) { _, which: Int ->
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            //BluetoothClient(devices[which],"test").start()
        }

        return builder.create()
    }
}