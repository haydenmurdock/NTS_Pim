package com.example.nts_pim.fragments_viewmodel.enter_name

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels

import com.example.nts_pim.R
import kotlinx.android.synthetic.main.enter_name_fragment.*

class EnterNameFragment : Fragment() {

    companion object {
        fun newInstance() = EnterNameFragment()
    }

    val viewModel: EnterNameViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.enter_name_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enter_name_editText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(charSeq: CharSequence?, start: Int, before: Int, count: Int) {
                enableDoneBtn(charSeq)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun enableDoneBtn(enterNameEditText: CharSequence?){
        if(enterNameEditText == null){
            return
        }
        enter_name_done_button.isEnabled = enterNameEditText.toString().count() > 1
    }


}
