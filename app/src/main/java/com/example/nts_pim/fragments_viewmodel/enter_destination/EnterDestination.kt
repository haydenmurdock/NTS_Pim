package com.example.nts_pim.fragments_viewmodel.enter_destination

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.PIMLocation
import com.example.nts_pim.data.repository.model_objects.trip.Destination
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.announcement_center.AnnouncementCenter
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.here_maps.CallbackFunction
import com.example.nts_pim.utilities.here_maps.HerePlacesAPI
import com.example.nts_pim.utilities.here_maps.PlaceSuggestion
import com.example.nts_pim.utilities.here_maps.SuggestionResults
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.enter_destination_screen.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein


class EnterDestination : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.enter_destination_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
           fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            AnnouncementCenter(this.requireContext()).playEnterDestinationMessage()
        editTextTextPostalAddress.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateAddressResultsOnView(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        showSoftKeyboard()
    }

    private fun showSoftKeyboard(){
        val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextTextPostalAddress, InputMethodManager.SHOW_IMPLICIT)
            editTextTextPostalAddress.requestFocus()
            ViewHelper.hideSystemUI(requireActivity())
            LoggerHelper.writeToLog("Showing Keyboard on enter destination fragment", null)
    }

    private fun updateAddressResultsOnView(query: String){
        var suggestions: MutableList<PlaceSuggestion>
        val currentLocation = getCurrentLatLong()
        var callback = CallbackFunction<SuggestionResults> {
               suggestions = it.Suggestions
                for(place in suggestions){
                   address_title_one_textView.text = place.title
                    address_title_one_textView.text = place.hightedLighted
                }
        }
        HerePlacesAPI.getSuggestions(currentLocation!!.lat, currentLocation.long, 200, query, 3, callback)
    }

    private fun sendGetUpFrontPricePacket(destination: Destination, activity: Activity){
       BlueToothHelper.sendGetUpFrontPricePacket(destination, activity)

    }
    @SuppressLint("MissingPermission")
    private fun getCurrentLatLong():PIMLocation? {
                var currentLocation: PIMLocation? = null
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location->
                        if(location != null) {
                            currentLocation = PIMLocation(location.latitude, location.longitude)
                    }
        }
        LoggerHelper.writeToLog("current location: $currentLocation", LogEnums.TRIP.tag)
        return currentLocation
    }
}