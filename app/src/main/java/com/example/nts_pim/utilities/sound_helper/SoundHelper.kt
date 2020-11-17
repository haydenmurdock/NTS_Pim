package com.example.nts_pim.utilities.sound_helper

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer

object SoundHelper {
    //This is to help with the square sound and the different sound states
    fun turnOnSound(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = (AudioManager.STREAM_MUSIC)
        //Making sure the volume is up to hear the sound.
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
        val streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        if (streamVolume != maxVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVolume, 0)
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        //this is the trick to make sure the microphone is not muted
        audioManager.isMicrophoneMute = true
        audioManager.isSpeakerphoneOn = true
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
    }

    fun turnOffSound(context: Context){
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = (AudioManager.MODE_NORMAL)
        //this is the trick to make sure the microphone is not muted
        audioManager.isMicrophoneMute = false
        audioManager.isSpeakerphoneOn = false
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioStreamType(AudioManager.MODE_NORMAL)
    }
}