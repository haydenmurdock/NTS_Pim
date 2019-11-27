package com.example.nts_pim.fragments_viewmodel.interaction_complete

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import com.example.nts_pim.R
import kotlinx.android.synthetic.main.interaction_complete_screen.*
import androidx.lifecycle.ViewModelProviders
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import kotlinx.coroutines.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance



class InteractionCompleteFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: InteractionCompleteViewModelFactory by instance()
    private var vehicleId = ""
    private var tripId = ""
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var callbackViewModel: CallBackViewModel
    private lateinit var viewModel: InteractionCompleteViewModel
    private val restartAppTimer = object: CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
           restartApp()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.interaction_complete_screen,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callbackViewModel = ViewModelProviders.of(this,factory).get(CallBackViewModel::class.java)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(InteractionCompleteViewModel::class.java)
        tripId = callbackViewModel.getTripId()
        vehicleId = viewModel.getVehicleID()
        thank_you_textView.isVisible = false
        checkIfTransactionIsComplete()
        runEndTripMutation()
        thank_you_textView.isVisible = true
        setInternalCurrentTripStatus()
        restartAppTimer.start()
    }

    private fun restartApp() {
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == R.id.interaction_complete_fragment) {
                navController.navigate(R.id.toRestartNewTrip)
            }
    }
    private fun checkIfTransactionIsComplete(){
        val isTransactionComplete = callbackViewModel.getIsTransactionComplete().value
            if(isTransactionComplete != null)
                if(isTransactionComplete)
                    callbackViewModel.squareChangeTransaction()

    }
    private fun runEndTripMutation() = launch {
      PIMMutationHelper.updateTripStatus(vehicleId, VehicleStatusEnum.TRIP_END.status, mAWSAppSyncClient!!, tripId)
    }
    private fun setInternalCurrentTripStatus(){
        val currentTrip = ModelPreferences(context!!).getObject(
            SharedPrefEnum.CURRENT_TRIP.key,
            CurrentTrip::class.java)
        currentTrip?.isActive = false
        ModelPreferences(context!!).putObject(SharedPrefEnum.CURRENT_TRIP.key, currentTrip)
    }

    override fun onPause() {
        super.onPause()
        callbackViewModel.clearAllTripValues()
    }
}