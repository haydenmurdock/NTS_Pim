package com.example.nts_pim.fragments_viewmodel.enter_destination

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import kotlinx.android.synthetic.main.enter_destination_screen.*
import kotlinx.android.synthetic.main.receipt_information_email.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class EnterDestination : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.enter_destination_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onResume() {
        super.onResume()
        showSoftKeyboard()
    }

    private fun showSoftKeyboard(){
        val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextTextPostalAddress, InputMethodManager.SHOW_IMPLICIT)
            editTextTextPostalAddress.requestFocus()
            ViewHelper.hideSystemUI(requireActivity())
            LoggerHelper.writeToLog( "Showing Keyboard on enter destination fragment", null)
    }
}