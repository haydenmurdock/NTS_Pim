package com.example.nts_pim.fragments_viewmodel.upfront_price_detail


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.fragment.app.viewModels

import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.trip.Trip
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.up_front_price_detail_fragment.*

class UpFrontPriceDetailFragment : Fragment() {
    val currentFragmentId = R.id.upFrontPriceDetailFragment

    companion object {
        fun newInstance() =
            UpFrontPriceDetailFragment()
    }

   val viewModel: UpFrontPriceDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.up_front_price_detail_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        request_trip_btn.setOnClickListener {
            toEnterNameFragment()
        }

        val upfrontTrip = viewModel.getUpFrontPriceDetails()
        updateUIWithUpfrontPrice(upfrontTrip)

    }

    private fun updateUIWithUpfrontPrice(trip: Trip?){
        if(trip?.upfrontPrice == null || trip?.pickup == null || trip?.upfrontPrice == null){
            LoggerHelper.writeToLog("Up front trip details was null. Didn't update UI correctly due to objects being null", LogEnums.BLUETOOTH.tag)
            return
        }
        upfront_price_textView.text = trip.upfrontPrice?.price.toString()
        upFront_pickup_textView.text = trip.pickup?.pickupStreet + ", ${trip.pickup?.pickupCity}+ ${trip.pickup?.pickupState}"
        upfront_destination_textView.text = trip.dest.destStreet + ", ${trip.dest.destCity}+ ${trip.dest.destState}"
        upfront_trip_time_textView.text = trip.upfrontPrice?.timeEst.toString()
        upfront_distance_textView.text = trip.upfrontPrice?.miles.toString()
    }


    //Navigation

    private fun toEnterNameFragment(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_upFrontPriceDetailFragment_to_enterNameFragment)
        }
    }

}
