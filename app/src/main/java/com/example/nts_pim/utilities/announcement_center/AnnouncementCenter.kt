package com.example.nts_pim.utilities.announcement_center

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.example.nts_pim.R
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.util.*

/**
 * Responsible for playing announcements to passengers
 */

class AnnouncementCenter(var context: Context) {
    var textToSpeech: TextToSpeech? = null
    var initialized = false
    private val mediaPlayerEnterDest: MediaPlayer = MediaPlayer.create(context, R.raw.please_enter_your_destination)

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
        mediaPlayerEnterDest.setOnCompletionListener { mP ->
            mP.release()
        }
        mediaPlayerEnterDest.start()
        LoggerHelper.writeToLog("Playing enter destination message", LogEnums.TEXT_READER.tag)
    }

    fun stopAnnouncement(){
        if(mediaPlayerEnterDest.isPlaying){
            mediaPlayerEnterDest.stop()
            mediaPlayerEnterDest.release()
        }
        if(textToSpeech != null && textToSpeech!!.isSpeaking){
            textToSpeech?.stop()
        }
    }

    private fun playPIMPayAmountMessage(messageToSpeak: String) {
        stopAnnouncement()
        if(initialized){
            LoggerHelper.writeToLog("Announcement: $messageToSpeak", LogEnums.TEXT_READER.tag)
            textToSpeech?.setSpeechRate(0.8.toFloat())
            textToSpeech!!.speak(messageToSpeak,
                TextToSpeech.QUEUE_FLUSH,
                null,null
            )
        }
    }
}