package com.example.nts_pim.fragments_viewmodel.safety_warning


import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.nts_pim.R
import kotlinx.android.synthetic.main.safety_warning_screen.*
import java.util.*
import kotlin.concurrent.timerTask

class SafetyWarningFragment : Fragment() {
    //This Fragment does not need ViewModel/Factory since it doesn't touch the Repo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.safety_warning_screen,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //checks for animation and Navigates to the next view
         checkAnimation(view)

    }

    private fun toNextScreen(view: View){
        Timer().schedule(timerTask {
            navigate(view)
        }, 5000)
    }

    private fun navigate(view: View){
        Navigation.findNavController(view).navigate(R.id.toLiveMeter)
    }

    private fun checkAnimation(view: View) {
        val animationIsOn = resources.getBoolean(R.bool.animationIsOn)
        if (animationIsOn) {
            playSafetyMessage()
            if (buckle_up_text_view != null){
                buckle_up_text_view.animate().alpha(1f).setDuration(2500).withEndAction(Runnable {
                    if (buckle_up_text_view != null){
                        buckle_up_text_view.animate().alpha(0.0f).setDuration(2500).withEndAction(Runnable {
                        })
                    }
                })
            }
        } else {
            toNextScreen(view)
        }
    }
    private fun playSafetyMessage(){
        val mediaPlayer = MediaPlayer.create(context, R.raw.saftey_message_test)
        mediaPlayer.setOnCompletionListener {
            navigate(view!!)
            mediaPlayer.release()
        }
        mediaPlayer.start()
        }
    }
