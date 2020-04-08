package com.example.nts_pim.fragments_viewmodel.taxi_number

import android.os.Bundle
import android.provider.Settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.taxi_number_screen.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import java.util.*
import kotlin.concurrent.timerTask
import org.kodein.di.generic.instance

class TaxiNumberFragment : ScopedFragment(), KodeinAware {

    //Kodein and viewModel/Factory
    override val kodein by closestKodein()
    private val viewModelFactory: TaxiNumberViewModelFactory by instance()
    private lateinit var viewModel: TaxiNumberViewModel
    private var fullBrightness = 255
    private val currentFragmentId = R.id.taxi_number_fragment
    private val logFragment = "Taxi Number"

    //Local Variables

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.taxi_number_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // we init the viewModel here for a lateinit, this gives us all the functions inside the repo.

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(TaxiNumberViewModel::class.java)
        val settings = viewModel.getVehicleSettings()
        if (settings != null){
            updateUI(settings.cabNumber)
            LoggerHelper.writeToLog("$logFragment: updated cab number. Starting animation")
            checkAnimation()
        }
        val br = Settings.System.getInt(context?.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        if(br != fullBrightness){
            changeScreenBrightness(fullBrightness)
        }
    }

    private fun checkAnimation() {
        // Checks to see if animations are on or not
        val animationIsOn = resources.getBoolean(R.bool.animationIsOn)

        if (animationIsOn) {
            if (taxi_number_text_view != null){
                taxi_number_text_view.animate().alpha(1f).setDuration(2500).withEndAction(Runnable {
                    if (taxi_number_text_view != null){
                        taxi_number_text_view.animate().alpha(0.0f).setDuration(2500).withEndAction(Runnable {
                            println("view should be gone")
                            navigate()
                        })
                    }else {
                        navigate()
                    }
                })
            }
        } else {
            toNextScreen()
        }
    }

    private fun updateUI(cabNumber: String){
            taxi_number_text_view.text = "Taxi number "+ cabNumber
    }

    private fun changeScreenBrightness(screenBrightness: Int) {
        Settings.System.putInt(
            context?.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )  //this will set the manual mode (set the automatic mode off)
        Settings.System.putInt(
            context?.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            screenBrightness
        )  //this will set the brightness to maximum (255)

        //refreshes the screen
        val br =
            Settings.System.getInt(context?.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        val lp = activity?.window?.attributes
        lp?.screenBrightness = br.toFloat() / 255
        activity?.window?.attributes = lp
    }


    private fun toNextScreen() {
        // 5 seconds to next screen
        Timer().schedule(timerTask {
            navigate()
        }, 5000)
    }

    private fun navigate() {
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == currentFragmentId) {
               navController.navigate(R.id.toSafetyWarning)
            }
    }
}