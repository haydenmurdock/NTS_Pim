package com.example.nts_pim.utilities.announcement_center

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.util.*

/**
 * Responsible for playing announcements to passengers
 */

class AnnouncementCenter(var context: Context) {
    var textToSpeech: TextToSpeech? = null
    var initialized = false

    init {
        textToSpeech = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                val language = textToSpeech?.setLanguage(Locale.US)
                if (language == TextToSpeech.LANG_MISSING_DATA
                    || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    LoggerHelper.writeToLog("The Language is not supported!", LogEnums.TEXT_READER.tag)
                } else {
                    initialized = true
                    LoggerHelper.writeToLog("TTS Initialization successful", LogEnums.TEXT_READER.tag)
                }
            } else {
                LoggerHelper.writeToLog("Issue setting up Text to Speech. Error code: $it", LogEnums.TEXT_READER.tag)
            }
        }
    }

    fun playEnterDestinationMessage(){
        playMessage(MESSAGE_ENTER_DESTINATION)
    }

    private fun playMessage(messageToSpeak: String) {
        if(initialized){
            LoggerHelper.writeToLog("Announcement: $messageToSpeak", LogEnums.TEXT_READER.tag)
            textToSpeech?.setSpeechRate(0.8.toFloat())
            textToSpeech!!.speak(messageToSpeak,
                TextToSpeech.QUEUE_FLUSH,
                null,null
            )
        }
    }

    companion object {
        private const val  MESSAGE_ENTER_DESTINATION = "Please enter your destination address or place"
    }
}