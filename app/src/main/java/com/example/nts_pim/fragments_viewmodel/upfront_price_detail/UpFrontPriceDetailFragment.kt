package com.example.nts_pim.fragments_viewmodel.upfront_price_detail


import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.data.repository.model_objects.trip.UpfrontTrip
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.up_front_price_detail_fragment.*

class UpFrontPriceDetailFragment : Fragment() {
    val currentFragmentId = R.id.upFrontPriceDetailFragment
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
    private var inactiveScreenTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.up_front_price_detail_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, factory)
            .get(UpfrontPriceViewModel::class.java)
        startInactivityTimeout()
        request_trip_btn.setOnClickListener {
            toEnterNameFragment()
        }

        val upfrontTrip = upfrontPriceViewModel.getUpFrontPriceDetails()
        updateUIWithUpfrontPrice(upfrontTrip)

        upfront_detail_cancel_btn.setOnClickListener {
            upfrontPriceViewModel.clearUpfrontPriceTrip()
            backToWelcomeScreen()
        }
        view.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    inactiveScreenTimer?.cancel()
                    inactiveScreenTimer?.start()
                }
            }
            v?.onTouchEvent(event) ?: true
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

    @SuppressLint("SetTextI18n")
    private fun updateUIWithUpfrontPrice(upfrontTrip: UpfrontTrip?){
        if(upfrontTrip?.upfrontPrice == null || upfrontTrip.pickup == null || upfrontTrip.upfrontPrice == null){
            LoggerHelper.writeToLog("Up front trip details was null. Didn't update UI correctly due to objects being null", LogEnums.BLUETOOTH.tag)
            return
        }
        upfront_price_textView.text = "$${upfrontTrip.upfrontPrice?.getTripPrice()}"
        upFront_pickup_textView.text = upfrontTrip.pickup?.pickupStreet + ", ${upfrontTrip.pickup?.pickupCity} ${upfrontTrip.pickup?.pickupState}"
        upfront_destination_textView.text = upfrontTrip.dest?.destStreet + ", ${upfrontTrip.dest?.destCity} ${upfrontTrip.dest?.destState}"
        upfront_trip_time_textView.text = " ${upfrontTrip.upfrontPrice?.getFormattedTime()} min"
        upfront_est_textView.text = " ${upfrontTrip.upfrontPrice?.getTripLength()}"
        upfront_distance_textView.text = " ${upfrontTrip.upfrontPrice?.getRoundedMiles()} miles"
    }

    //Navigation
    private fun toEnterNameFragment(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_upFrontPriceDetailFragment_to_enterNameFragment)
        }
    }

    private fun backToWelcomeScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_upFrontPriceDetailFragment_to_welcome_fragment)
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
