package com.example.nts_pim.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.here_maps.SuggestionResults
import com.example.nts_pim.data.repository.model_objects.trip.*
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper

object UpfrontPriceRepository {
    var upfrontTrip: UpfrontTrip? = null
    private var tripUpdated = MutableLiveData<Boolean>()
    private var sendBTDestination = MutableLiveData<Boolean>()
    private var suggestedDestinations = mutableListOf<SuggestionResults>()
    private var suggestedDestData = MutableLiveData<MutableList<SuggestionResults>>()
    private var driverSignedIn = false
    private var driverSignedInMLD = MutableLiveData<Boolean>()

    init {
        tripUpdated.postValue(false)
        sendBTDestination.postValue(false)
    }


    fun createUpfrontTrip(destination: Destination){
        upfrontTrip = UpfrontTrip(null,
                    destination,
                    null,
                    null,
                    false,
                    null)
        sendBTDestination.postValue(true)
    }

    fun updateTrip(pickup: Pickup, upfrontPrice: UpFrontPrice){
        upfrontTrip?.pickup = pickup
        upfrontTrip?.upfrontPrice = upfrontPrice
        tripUpdated.postValue(true)
    }

    fun updateNameForTrip(name: String){
        upfrontTrip?.passengerName = name
    }

    fun getUpfrontPriceDetails() = upfrontTrip

    @JvmName("setSuggestedDestinations1")
    fun setSuggestedDestinations(destinations: MutableList<SuggestionResults>){
        suggestedDestinations = filterSuggestions(destinations)
        suggestedDestData.postValue(suggestedDestinations)
        Log.i("URL", "Suggested locations updated on Upfront price repository")
    }

    private fun filterSuggestions(destinations: MutableList<SuggestionResults>): MutableList<SuggestionResults>{
        val filteredSuggestions: MutableList<SuggestionResults> = mutableListOf()
        for(suggestion in destinations){
            if(!suggestion.highlightedVicinity.isNullOrBlank() ||
                !suggestion.highlightedTitle.isNullOrBlank() ||
                    !suggestion.vicinity.isNullOrBlank()){
                val checkedTitleResult = updateHighlightedVicinity(suggestion)
                    filteredSuggestions.add(checkedTitleResult)
            }
        }
        return filteredSuggestions
    }

    private fun updateHighlightedVicinity(suggestionResult: SuggestionResults): SuggestionResults{
        val updatedResult = suggestionResult
        val delimiter = ","
        if(suggestionResult.highlightedTitle == suggestionResult.highlightedVicinity){
            val titleParts = suggestionResult.highlightedTitle.split(delimiter)
            updatedResult.highlightedTitle = titleParts.first()
            updatedResult.highlightedVicinity = titleParts[1] + ", " + titleParts[2].filter { it.isLetter() }
            Log.i("URL", "Returning suggested destination after equals check: Title: ${updatedResult.highlightedTitle}, Vinicity: ${updatedResult.highlightedVicinity}")
            return updatedResult
        }
        if(suggestionResult.highlightedVicinity.contains(suggestionResult.highlightedTitle)) {
            Log.i("URL", "highlightedVicinity contained ${suggestionResult.highlightedTitle}")
            val titleParts = suggestionResult.highlightedTitle.split(delimiter)
            updatedResult.highlightedTitle = titleParts.first()
            updatedResult.highlightedVicinity = titleParts[1] + ", " + titleParts[2].filter { it.isLetter() }
            Log.i(
                "URL",
                "Returning suggested destination after contains check: Title: ${updatedResult.highlightedTitle}, Vicinity ${updatedResult.highlightedVicinity}"
            )
        }
        return updatedResult
    }


    private fun checkStateAndRemoveZipCode(highlightedVicinity: String?): String {
        if(highlightedVicinity.isNullOrBlank()){
            return "Word"
        }
        return highlightedVicinity.dropLast(6)
    }

    fun getSuggestedDestinations() = suggestedDestData

    fun getUpfrontPriceDestination() = upfrontTrip?.dest

    fun hasUpfrontPriceBeenUpdated() = tripUpdated

    fun needToSendDestinationUpfrontPrice() = sendBTDestination

    fun sentDestinationUpfrontPrice(){
        sendBTDestination.postValue(false)
    }

    fun clearUpfrontPrice(){
        upfrontTrip = null
        tripUpdated.postValue(false)
        sendBTDestination.postValue(false)
    }

    fun getPassengerName(): String? {
        return upfrontTrip?.passengerName
    }

    fun driverSignedIn() {
        driverSignedIn = true
        driverSignedInMLD.postValue(driverSignedIn)
    }

    fun driverSignedOut() {
        driverSignedIn = false
        driverSignedInMLD.postValue(driverSignedIn)
    }

    fun getIsDriverSignedIn() = driverSignedInMLD

}