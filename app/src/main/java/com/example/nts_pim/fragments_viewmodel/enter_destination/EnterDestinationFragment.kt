package com.example.nts_pim.fragments_viewmodel.enter_destination

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.data.repository.model_objects.PIMLocation
import com.example.nts_pim.data.repository.model_objects.here_maps.SuggestionResults
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.announcement_center.AnnouncementCenter
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.here_maps.HerePlacesAPI
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.upfront_price_errors.UpfrontPriceErrorHelper
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
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
    private var enteredSearchTextView = ""
    private var finishedAddressText = ""
    private var listOfSuggestedAddress = mutableListOf<SuggestionResults>()
    private val numberOfChar = 3

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
        val upfrontPriceFactory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, upfrontPriceFactory)
            .get(UpfrontPriceViewModel::class.java)
        showAddressLabels(false)
        showSoftKeyboard()
        back_to_welcome_btn.setOnClickListener {
            backToWelcomeScreen()
        }

        editTextTextPostalAddress.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(charSeq: CharSequence?, start: Int, before: Int, count: Int) {
                enteredSearchTextView = charSeq.toString()
                entered_address_TextView.text = enteredSearchTextView
                if(charSeq.isNullOrBlank() || charSeq.isEmpty()){
                    showAddressLabels(false)
                    listOfSuggestedAddress.clear()
                    updateTitle(listOfSuggestedAddress)
                }
                if(charSeq.toString().count() >= numberOfChar) {
                    updateAddressResultsOnView(charSeq.toString())
                    updateAutoCompleteSuggestAddress(charSeq.toString())
                }

                if (charSeq != null) {
                    if(charSeq.isEmpty()){
                        showAddressLabels(false)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        editTextTextPostalAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
         //       if(listOfSuggestedAddress.isNotEmpty()){
        //            gettingDetailsAboutPrice(listOfSuggestedAddress.first())
         //       }
         //       toCalculatingPriceScreen()
                true
            }
            false
        }

        upfrontPriceViewModel.getUpfrontPriceSuggestDest().observe(this.viewLifecycleOwner, Observer { suggestionResultsList ->
            if(suggestionResultsList.count() == 0){
                listOfSuggestedAddress = suggestionResultsList
                showAddressLabels(false)
            }
            if(suggestionResultsList.isNotEmpty()){
                listOfSuggestedAddress = suggestionResultsList
                for ((index, obj) in suggestionResultsList.withIndex()) {
                    if(index == 0){
                        finishedAddressText = obj.highlightedVicinity
                        updateAutoCompleteSuggestAddress(finishedAddressText)
                        address_title_one_textView.text = checkDestinationLengthForLabel(obj.highlightedTitle)
                        address_detail_one_textView.text = checkDestinationLengthForLabel(obj.highlightedVicinity)
                    }
                    if(index == 1){
                        address_title_two_textView.text = checkDestinationLengthForLabel(obj.highlightedTitle)
                        address_detail_two_textView.text = checkDestinationLengthForLabel(obj.highlightedVicinity)
                    }
                    if(index == 2){
                        address_title_three_textView.text = checkDestinationLengthForLabel(obj.highlightedTitle)
                        address_detail_three_textView.text = checkDestinationLengthForLabel(obj.highlightedVicinity)
                    }
                }
                showAddressLabels(true)
            }
            updateTitle(listOfSuggestedAddress)
        })

        address_one_view.setOnTouchListener((View.OnTouchListener{ v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if(listOfSuggestedAddress.isNotEmpty()){
                       val isBluetoothConnected = BluetoothDataCenter.isBluetoothSocketConnected().value ?: false
                        if(isBluetoothConnected){
                            gettingDetailsAboutPrice(listOfSuggestedAddress.first())
                            toCalculatingPriceScreen()
                        } else {
                            UpfrontPriceErrorHelper.showNoBluetoothError(requireActivity())
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))

        address_two_view.setOnTouchListener((View.OnTouchListener{ v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if(listOfSuggestedAddress.isNotEmpty()
                        && listOfSuggestedAddress.count() > 1){
                        val isBluetoothConnected = BluetoothDataCenter.isBluetoothSocketConnected().value ?: false
                        if(isBluetoothConnected){
                            gettingDetailsAboutPrice(listOfSuggestedAddress[1])
                            toCalculatingPriceScreen()
                        } else {
                            UpfrontPriceErrorHelper.showNoBluetoothError(requireActivity())
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))

        address_three_view.setOnTouchListener((View.OnTouchListener{ v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if(listOfSuggestedAddress.isNotEmpty()
                        && listOfSuggestedAddress.count() > 2){
                        val isBluetoothConnected = BluetoothDataCenter.isBluetoothSocketConnected().value ?: false
                        if(isBluetoothConnected) {
                            gettingDetailsAboutPrice(listOfSuggestedAddress[2])
                            toCalculatingPriceScreen()
                        } else {
                            UpfrontPriceErrorHelper.showNoBluetoothError(requireActivity())
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
    }

    private fun showSoftKeyboard(){
        val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextTextPostalAddress, InputMethodManager.SHOW_IMPLICIT)
            editTextTextPostalAddress.requestFocus()
            ViewHelper.hideSystemUI(requireActivity())
            LoggerHelper.writeToLog("Showing Keyboard on enter destination fragment", LogEnums.BLUETOOTH.tag)
    }

    private fun updateAddressResultsOnView(query: String){
        val currentLocation = getCurrentLatLong()
        if(currentLocation != null){
            HerePlacesAPI.getSuggestedAddress(currentLocation.lat, currentLocation.lng, query)
        } else {
            LoggerHelper.writeToLog("Issue with current address location", LogEnums.ERROR.tag)
        }
    }

    private fun updateTitle(listOfSuggestionResults: MutableList<SuggestionResults>){
        if(listOfSuggestionResults.isNotEmpty()){
            enter_destination_title_textView.text = "Tap a result to select your destination"
        } else {
            enter_destination_title_textView.text = "What's your destination?"
        }
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentLatLong():PIMLocation? {
            val locationManager =
                requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationProvider: String = LocationManager.NETWORK_PROVIDER
            if (ContextCompat.checkSelfPermission(
                    this.requireActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lastLocation = locationManager.getLastKnownLocation(locationProvider)
                if(lastLocation != null){
                    LoggerHelper.writeToLog("Enter Destination Fragment: Lat: ${lastLocation.latitude}, Lng: ${lastLocation.longitude}", LogEnums.BLUETOOTH.tag)
                    return PIMLocation(lastLocation.latitude, lastLocation.longitude)
                }
            } else {
                LoggerHelper.writeToLog("Enter Destination Fragment: access_coarse_location was not granted", LogEnums.SETUP.tag)
            }
        return PIMLocation(0.0,0.0)
    }

    private fun updateAutoCompleteSuggestAddress(finishedText: String){
        val suggestion = getAddressWithSuggestedAddress(finishedText)
        if(auto_complete_TextView.visibility == View.INVISIBLE){
            auto_complete_TextView.text = suggestion
            auto_complete_TextView.visibility = View.VISIBLE
        }
    }

    private fun getAddressWithSuggestedAddress(firstHereAddress: String): String {
        val currentInfoEntered = editTextTextPostalAddress.text.toString()
       return firstHereAddress.replace(currentInfoEntered, "")
    }

    private fun checkDestinationLengthForLabel(address: String):String {
        if(address.length < 30){
            return address
        }
        address.dropLastWhile {
            address.length >= 30
        }
        if(address.length == 29){
            return "$address..."
        }
        return  address
    }

    private fun showAddressLabels(boolean: Boolean){
        val viewVisibility = if(boolean){
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        if(listOfSuggestedAddress.count() > 0){
            address_one_view.visibility = viewVisibility
            address_title_one_textView.visibility = viewVisibility
            address_detail_one_textView.visibility = viewVisibility
            address_one_imageView.visibility = viewVisibility
            address_one_view.isEnabled = boolean
        }
        if(listOfSuggestedAddress.count() > 1){
            address_two_view.visibility = viewVisibility
            address_title_two_textView.visibility = viewVisibility
            address_detail_two_textView.visibility = viewVisibility
            address_two_imageView. visibility = viewVisibility
            address_one_view.isEnabled = boolean
        }

        if(listOfSuggestedAddress.count() > 2){
            address_three_view.visibility = viewVisibility
            address_title_three_textView.visibility = viewVisibility
            address_detail_three_textView.visibility= viewVisibility
            address_three_view.visibility = viewVisibility
            address_three_imageView.visibility = viewVisibility
            address_one_view.isEnabled = boolean
        }
    }

    private fun gettingDetailsAboutPrice(addressPicked: SuggestionResults){
        val tag = addressPicked.tag
        HerePlacesAPI.getDetailAddress(tag)
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

    override fun onDestroy() {
        super.onDestroy()
        upfrontPriceViewModel.getUpfrontPriceSuggestDest().removeObservers(this.viewLifecycleOwner)
    }
}