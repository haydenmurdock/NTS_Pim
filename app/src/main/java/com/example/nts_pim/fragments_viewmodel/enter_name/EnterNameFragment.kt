package com.example.nts_pim.fragments_viewmodel.enter_name

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation

import com.example.nts_pim.R
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.utilities.announcement_center.AnnouncementCenter
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import kotlinx.android.synthetic.main.enter_name_fragment.*

class EnterNameFragment : Fragment() {
    val viewModel: EnterNameViewModel by viewModels()
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
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
        val upfrontPriceFactory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, upfrontPriceFactory)
            .get(UpfrontPriceViewModel::class.java)

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
}
