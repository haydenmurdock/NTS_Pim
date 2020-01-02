package com.example.nts_pim.fragments_viewmodel.trip_details

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.model_objects.PimError
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlinx.android.synthetic.main.trip_review.*
import java.time.format.DateTimeFormatter

class TripDetailFragment : ScopedFragment() {

    private val currentFragmentId = R.id.tripReviewFragment
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.trip_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI()

        trip_review_back_btn.setOnClickListener {
            backToRecentTrip()
        }
        getLocation()
    }
    private fun updateUI(){
        var receiptMessage = TripDetails.receiptMessage
        if (receiptMessage.isNullOrBlank()){
            receiptMessage = "No message"
        }

        var serverCode = TripDetails.receiptCode.toString()
        if(serverCode == "0"){
            serverCode = "No code received"
        }
        trip_start_time_textView.text = "PIM Start Time: " +formatter.format(TripDetails.tripStartTime)
        trip_end_time_textView.text = "PIM End Time: " +formatter.format(TripDetails.tripEndTime)
        receipt_server_code_textView.text = "Receipt server code: " +serverCode
        if (TripDetails.receiptCode == 200){
            receipt_server_message_textView.text = "Receipt server message: Success"
        } else {
            receipt_server_message_textView.text = "Receipt server message: " +receiptMessage
        }
        isOnline_textView.text = "LTE Network Connected: " +isOnline(context!!)
        getLastError()
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getLocation() {
        val locationManager =
            activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProvider: String = LocationManager.NETWORK_PROVIDER
        if (ContextCompat.checkSelfPermission(
                this.activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                pim_current_lat_textView != null &&
            pim_current_long_textView != null)
        { val lastLocation = locationManager.getLastKnownLocation(locationProvider)
            pim_current_lat_textView.text =  "Pim Current Latitude: "+lastLocation.latitude.toString()
            pim_current_long_textView.text =  "Pim Current Longitude: " +lastLocation.longitude.toString()
        }
    }

    private fun getLastError(){
        val lastError = ModelPreferences(context!!).getObject("PimError", PimError::class.java)
        println(lastError?.message)
    }
    private fun backToRecentTrip(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_tripReviewFragment_to_recentTripAWSFragment)
        }
    }
}
