package com.example.nts_pim.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.here_maps.SuggestionResults
import com.example.nts_pim.data.repository.model_objects.trip.*

object UpfrontPriceRepository {
    var upfrontTrip: UpfrontTrip? = null
    private var tripUpdated = MutableLiveData<Boolean>()
    private var sendBTDestination = MutableLiveData<Boolean>()
    private var suggestedDestinations = mutableListOf<SuggestionResults>()
    private var suggestedDestData = MutableLiveData<MutableList<SuggestionResults>>()
    private var driverSignedIn = false
    private var driverSignedInMLD = MutableLiveData<Boolean>()
    private var upfrontPriceError = false
    private var upfrontPriceErrorMLD = MutableLiveData<Boolean>()

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
            if(!suggestion.highlightedVicinity.isBlank() ||
                !suggestion.highlightedTitle.isNullOrBlank() ||
                    !suggestion.vicinity.isNullOrBlank() ||
                suggestion.highlightedVicinity.isNotEmpty()
            ){
                val checkedTitleResult = updateHighlightedVicinity(suggestion)
                    filteredSuggestions.add(checkedTitleResult)
            }
        }
        return filteredSuggestions
    }

    private fun updateHighlightedVicinity(suggestionResult: SuggestionResults): SuggestionResults{
        var updatedResult = suggestionResult
        val delimiter = ","
        if(suggestionResult.highlightedTitle.contains("(")){
            for ((index, char) in suggestionResult.highlightedTitle.withIndex()){
                if(char.toString() == ("(")){
                 val newTitle =  suggestionResult.highlightedTitle.removeRange(index..suggestionResult.highlightedTitle.lastIndex)
                    Log.i("New Title after removing ( == $newTitle", "URL")
                    updatedResult.highlightedTitle = newTitle
                    if(updatedResult.highlightedVicinity.contains(newTitle)){
                        val vicinityParts = updatedResult.highlightedVicinity.split(delimiter)
                        if(vicinityParts.count() > 3) {
                            updatedResult.highlightedVicinity = vicinityParts[1] + ", " + vicinityParts[2].filter { it.isLetter() || it.isWhitespace() }.trim()
                        }
                    }
                }
            }
            return updatedResult
        }
        if(suggestionResult.highlightedTitle == suggestionResult.highlightedVicinity){
            val titleParts = suggestionResult.highlightedTitle.split(delimiter)
            updatedResult.highlightedTitle = titleParts.first()
            if(titleParts.count() > 3) {
                updatedResult.highlightedVicinity = titleParts[1] + ", " + titleParts[2].filter { it.isLetter()|| it.isWhitespace() }.trim()
            }
            Log.i("URL", "Returning suggested destination after equals check: Title: ${updatedResult.highlightedTitle}, Vinicity: ${updatedResult.highlightedVicinity}")
            return updatedResult
        }
        if(suggestionResult.highlightedVicinity.contains(updatedResult.title)) {
            Log.i("URL", "highlightedVicinity contained ${suggestionResult.highlightedTitle}")
            val vicinityParts = suggestionResult.highlightedVicinity.split(delimiter)
            updatedResult.highlightedTitle = vicinityParts.first()
            if(vicinityParts.count() > 3) {
                updatedResult.highlightedVicinity = vicinityParts[1] + ", " + vicinityParts[2].filter { it.isLetter()|| it.isWhitespace() }.trim()
            }
            Log.i(
                "URL",
                "Returning suggested destination after contains check: Title: ${updatedResult.highlightedTitle}, Vicinity ${updatedResult.highlightedVicinity}"
            )
            return updatedResult
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
        suggestedDestinations.clear()
        suggestedDestData.postValue(suggestedDestinations)
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

    fun errorForUpfrontPrice(errorMsg: String?){
        if(!errorMsg.isNullOrEmpty()){
            upfrontTrip?.errorMsg = errorMsg
            upfrontPriceError = true
            upfrontPriceErrorMLD.postValue(true)
        }
    }

    fun errorDisplayed(){
        upfrontPriceError = false
        upfrontPriceErrorMLD.postValue(false)
    }

    fun wasThereAnUpfrontError() = upfrontPriceErrorMLD


}