package com.example.nts_pim.fragments_viewmodel.enter_name

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.utilities.announcement_center.AnnouncementCenter
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.enter_name_fragment.*

class EnterNameFragment : Fragment() {
    val viewModel: EnterNameViewModel by viewModels()
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
    private var inactiveScreenTimer: CountDownTimer? = null
    private val currentFragmentId = R.id.enterNameFragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.enter_name_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnnouncementCenter(this.requireContext()).playEnterNameMessage()
        showSoftKeyboard()
        val upfrontPriceFactory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, upfrontPriceFactory)
            .get(UpfrontPriceViewModel::class.java)
        startInactivityTimeout()
        enter_name_editText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                charSeq: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                enableDoneBtn(charSeq)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        enter_name_done_button.setOnClickListener {
            sendUpdateUpfrontPriceWithName()
        }
        enter_name_done_button.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendUpdateUpfrontPriceWithName()
                true
            }
            false
        }

        enter_name_cancel_btn.setOnClickListener {
            backToUpFrontPriceDetail()
            upfrontPriceViewModel.updateNameOfPassenger("")
        }

        view.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    inactiveScreenTimer?.cancel()
                    inactiveScreenTimer?.start()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

    }
    private fun startInactivityTimeout(){
        inactiveScreenTimer = object: CountDownTimer(120000, 60000) {
            // this is set to 1 min and will finish if a new trip is started.
            override fun onTick(millisUntilFinished: Long) {

            }
            override fun onFinish() {

                backToUpFrontPriceDetail()
            }
        }.start()
    }

    private fun showSoftKeyboard(){
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(enter_name_editText, InputMethodManager.SHOW_IMPLICIT)
        enter_name_editText.requestFocus()
        ViewHelper.hideSystemUI(requireActivity())
        LoggerHelper.writeToLog("Showing Keyboard on enter destination fragment", LogEnums.BLUETOOTH.tag)
    }


    private fun enableDoneBtn(enterNameEditText: CharSequence?){
        if(enterNameEditText == null){return}
        enter_name_done_button.isEnabled = enterNameEditText.toString().count() > 1
       upfrontPriceViewModel.updateNameOfPassenger(enterNameEditText.toString())

    }

    private fun sendUpdateUpfrontPriceWithName(){
        val name = upfrontPriceViewModel.getPassengersName()
        if(!name.isNullOrBlank()){
            BlueToothHelper.sendUpdateTripPacket(name, this.requireActivity())
            toWaitingForDriver()
        }
    }
    //Navigation
    private fun toWaitingForDriver(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_enterNameFragment_to_waitingForDriver)
        }
    }

    private fun backToUpFrontPriceDetail(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_enterNameFragment_to_upFrontPriceDetailFragment)
        }
    }
    override fun onStop() {
        super.onStop()
        inactiveScreenTimer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactiveScreenTimer?.cancel()
    }
}
