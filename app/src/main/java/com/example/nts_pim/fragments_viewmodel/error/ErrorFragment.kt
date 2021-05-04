package com.example.nts_pim.fragments_viewmodel.error

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.nts_pim.R
import kotlinx.android.synthetic.main.error_view.*


class ErrorFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.error_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        error_cancel_btn.setOnClickListener {
           Log.i("test", "error cancel button hit")
        }

        error_retry_btn.setOnClickListener {
           Log.i("test", "error retry button hit")
        }

    }
}
