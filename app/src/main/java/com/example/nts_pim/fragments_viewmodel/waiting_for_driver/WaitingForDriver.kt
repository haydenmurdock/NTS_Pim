package com.example.nts_pim.fragments_viewmodel.waiting_for_driver

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.waiting_for_driver_fragment.*

class WaitingForDriver : Fragment() {
    private lateinit var viewModel: WaitingForDriverViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private val currentFragmentId = R.id.waitingForDriver


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.waiting_for_driver_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)

        setUpView()
        val currentMeter = getCurrentMeterState()
        checkMeterState(currentMeter)

        callBackViewModel.getMeterState().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer { meterState ->
            if(meterState == MeterEnum.METER_ON.state || meterState == MeterEnum.METER_TIME_OFF.state){
                LoggerHelper.writeToLog( "Meter $meterState is picked up on waiting for driver. Starting trip animation", LogEnums.TRIP_STATUS.tag)
                checkMeterState(meterState)
            }
        })
    }

    private fun setUpView(){
        waiting_progressBar.animate()
    }

    private fun getCurrentMeterState(): String {
       return callBackViewModel.getMeterState().value ?: ""
    }

   private fun checkMeterState(meterState: String){
        when(meterState){
            MeterEnum.METER_ON.state -> {
                toLiveMeter()
            }
            MeterEnum.METER_TIME_OFF.state -> {
                toLiveMeter()
            }
            MeterEnum.METER_OFF.state -> {
                toTripReview()
            }
        }
    }


   private fun toLiveMeter(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_waitingForDriver_to_live_meter_fragment)
        }
    }

   private fun toTripReview(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_waitingForDriver_to_trip_review_fragment)
        }
    }
}
