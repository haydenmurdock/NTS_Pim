package com.example.nts_pim.fragments_viewmodel.calculating_price

import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation

import com.example.nts_pim.R
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import kotlinx.android.synthetic.main.calculating_upfront_price_fragment.*

class CalculatingUpfrontPriceFragment : Fragment() {
    val currentFragmentId = R.id.calculatingUpfrontPriceFragment
    private var inactiveScreenTimer: CountDownTimer? = null

    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.calculating_upfront_price_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        startProgressBar()

        val upfrontPriceFactory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, upfrontPriceFactory)
            .get(UpfrontPriceViewModel::class.java)

        upfrontPriceViewModel.hasUpfrontPriceUpdated().observe(this.viewLifecycleOwner, Observer {
            if(it){
                receivedUpfrontPriceBtPacket()
            }
        })

        upfrontPriceViewModel.needToSendDestUpfrontPrice().observe(this.viewLifecycleOwner, Observer {
            if(it){
                upfrontPriceViewModel.sentDestForUpfrontPrice()
                sendDestinationBtPacket()
            }
        })

        startInactivityTimeout()
    }

    private fun sendDestinationBtPacket() {
        val destination = upfrontPriceViewModel.getUpfrontPriceDest()
        if (destination != null) {
            BlueToothHelper.sendGetUpFrontPricePacket(destination, this.requireActivity())
        }
    }

    private fun receivedUpfrontPriceBtPacket(){
        toUpFrontPriceDetail()
    }

    private fun startProgressBar(){
        if(!get_upfront_price_progressBar.isAnimating){
            get_upfront_price_progressBar.animate()
        }
    }
    private fun startInactivityTimeout(){
        inactiveScreenTimer = object: CountDownTimer(120000, 60000) {
            // this is set to 1 min and will finish if a new trip is started.
            override fun onTick(millisUntilFinished: Long) {

            }
            override fun onFinish() {
                upfrontPriceViewModel.clearUpfrontPriceTrip()
                backToWelcomeScreen()
                }
        }.start()
    }
    //Navigation
    private fun toUpFrontPriceDetail(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_calculatingUpfrontPriceFragment_to_upFrontPriceDetailFragment)
        }
    }

    private fun backToWelcomeScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_calculatingUpfrontPriceFragment_to_welcome_fragment)
        }
    }

    override fun onStop() {
        super.onStop()
        inactiveScreenTimer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactiveScreenTimer?.cancel()
    }

}
