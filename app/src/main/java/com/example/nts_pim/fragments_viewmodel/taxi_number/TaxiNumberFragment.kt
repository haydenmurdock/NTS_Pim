package com.example.nts_pim.fragments_viewmodel.taxi_number

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
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
            checkAnimation(view)
        }
    }

    private fun checkAnimation(view: View) {
        // Checks to see if animations are on or not
        val animationIsOn = resources.getBoolean(R.bool.animationIsOn)

        if (animationIsOn) {

            taxi_number_text_view.animate().alpha(1f).setDuration(2500).withEndAction(Runnable {

                taxi_number_text_view.animate().alpha(0.0f).setDuration(2500).withEndAction(Runnable {
                    println("view should be gone")
                    navigate(view)
                })
            }
            )
        } else {
            toNextScreen(view)
        }
    }

    private fun updateUI(cabNumber: String){
            taxi_number_text_view.text = "Taxi number "+ cabNumber
    }

    private fun toNextScreen(view: View) {
        // 5 seconds to next screen
        Timer().schedule(timerTask {
            navigate(view)
        }, 5000)
    }

    private fun navigate(view: View) {
        // Navigates to the next screen
        Navigation.findNavController(view).navigate(R.id.toSafetyWarning)
    }
}