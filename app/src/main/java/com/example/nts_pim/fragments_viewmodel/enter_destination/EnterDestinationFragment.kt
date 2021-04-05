package com.example.nts_pim.fragments_viewmodel.enter_destination

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
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
import kotlinx.android.synthetic.main.enter_destination_fragment.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein


class EnterDestinationFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val currentFragmentId = R.id.enterDestination
    private var destination: Destination? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.enter_destination_fragment, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        AnnouncementCenter(this.requireContext()).playEnterDestinationMessage()

        back_to_welcome_btn.setOnClickListener {
            backToWelcomeScreen()
        }
        editTextTextPostalAddress.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(charSeq: CharSequence?, start: Int, before: Int, count: Int) {
                 //   updateAddressResultsOnView(s.toString())
                    //updateEditTextWithSuggestAddress(s.toString())
                updateTitle(charSeq)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
            }
        })

        editTextTextPostalAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                LoggerHelper.writeToLog("Enter button tapped", LogEnums.TRIP.tag)
                sendGetUpFrontPricePacket(destination, this.requireActivity())
                toCalculatingPriceScreen()
                true
            }
            false
        }
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
        val callback = CallbackFunction<SuggestionResults> {
               suggestions = it.Suggestions
                for(place in suggestions){
                   address_title_one_textView.text = place.title
                    address_title_one_textView.text = place.hightedLighted
                }
        }
        if(currentLocation != null){
            HerePlacesAPI.getSuggestions(currentLocation.lat, currentLocation.long, 200, query, 3, callback)
        } else {
            LoggerHelper.writeToLog("Issue with current address location", LogEnums.ERROR.tag)
        }

    }

    private fun updateTitle(editText: CharSequence?){
       if(editText == null){
           return
       }
        if(editText.toString().count() > 0){
            enter_destination_title_textView.text = "Tap a result to select your destination"
        } else {
            enter_destination_title_textView.text = "What's your destination?"
        }
    }

    private fun sendGetUpFrontPricePacket(destination: Destination?, activity: Activity){
        if(destination == null){
            LoggerHelper.writeToLog("Destination packet was null. Did not send bluetooth packet to driver tablet", LogEnums.BLUETOOTH.tag)
            return
        }
       BlueToothHelper.sendGetUpFrontPricePacket(destination, activity)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLatLong():PIMLocation? {
            val locationManager =
                requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationProvider: String = LocationManager.NETWORK_PROVIDER
            if (ContextCompat.checkSelfPermission(
                    this.requireActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            { val lastLocation = locationManager.getLastKnownLocation(locationProvider)
                if(lastLocation != null){
                    return PIMLocation(lastLocation.latitude, lastLocation.longitude)
                }
            }
        return PIMLocation(0.0,0.0)
    }

    private fun updateEditTextWithSuggestAddress(currentText: String){
        val suggestion = getAddressWithSuggestedAddress(address_title_one_textView.text.toString())
        editTextTextPostalAddress.setText(currentText + suggestion)
    }

    private fun getAddressWithSuggestedAddress(firstHereAddress: String): String {
        val currentInfoEntered = editTextTextPostalAddress.text.toString()
       return firstHereAddress.replace(currentInfoEntered, "")
    }

    //Navigation

    private fun backToWelcomeScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_enterDestination_to_welcome_fragment)
        }
    }

    private fun toCalculatingPriceScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_enterDestination_to_calculatingUpfrontPriceFragment)
        }
    }


}