package com.example.nts_pim.fragments_viewmodel.safety_warning


import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.AdInfoHolder
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.safety_warning_screen.*
import java.util.*
import kotlin.concurrent.timerTask

class SafetyWarningFragment : Fragment() {
    //This Fragment does not need ViewModel/Factory since it doesn't touch the Repo
    val currentFragmentId = R.id.safety_warning_fragment
    val logFragment = "Safety Warning"
    var showAdd = AdInfoHolder.isThereAnAdd


    private val safetyScreenWarningTimer = object: CountDownTimer(10000, 1000){
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            if(view != null){
                LoggerHelper.writeToLog("$logFragment: Did not play safety message", LogEnums.TEXT_READER.tag)
                checkScreenDestination()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.safety_warning_screen,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playSafetyMessage()
        //checks for animation and Navigates to the next view
         checkAnimation()
    }



    private fun checkScreenDestination(){
        if(showAdd){
            toAdScreen()
        } else {
            toMeterScreen()
        }
    }

    private fun toMeterScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.toLiveMeter)
        }
    }

    private fun toAdScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_safety_warning_fragment_to_advertisementFragment)
        }
    }

    private fun checkAnimation() {
        val animationIsOn = resources.getBoolean(R.bool.animationIsOn)
        if (animationIsOn) {
            if (buckle_up_text_view != null){
                buckle_up_text_view.animate().alpha(1f).setDuration(2500).withEndAction(Runnable {
                    if (buckle_up_text_view != null){
                        buckle_up_text_view.animate().alpha(0.0f).setDuration(2500).withEndAction(Runnable {
                        })
                    }
                })
            }
        } else {
            checkScreenDestination()
        }
    }
    private fun playSafetyMessage(){
        val mediaPlayer = MediaPlayer.create(context, R.raw.saftey_message_test)
        mediaPlayer.setOnCompletionListener { mP ->
            LoggerHelper.writeToLog("$logFragment: Finished Safety Message", LogEnums.TEXT_READER.tag)
            mP.release()
            if(view != null){
                checkScreenDestination()
            }
        }
        mediaPlayer.start()
        LoggerHelper.writeToLog("$logFragment: Started Safety Message", LogEnums.TEXT_READER.tag)
    }

    override fun onResume() {
        super.onResume()
        safetyScreenWarningTimer.cancel()
        safetyScreenWarningTimer.start()
    }
}
