package com.example.nts_pim.utilities.upfront_price_errors

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper

object UpfrontPriceErrorHelper {

    fun showDriverNotSignedInError(activity: Activity, vehicleId: String){
        LoggerHelper.writeToLog("Upfront Price Error: No driver signed in: Generating UI", LogEnums.UPFRONT_PRICE.tag)
        val viewGroup = activity.findViewById<View>(android.R.id.content) as ViewGroup
        View.inflate(activity, R.layout.error_view, viewGroup)
        val cancelBtn = activity.findViewById<Button>(R.id.error_cancel_btn)
        val retryBtn = activity.findViewById<Button>(R.id.error_retry_btn)
        val titleLabel = activity.findViewById<TextView>(R.id.error_view_title_textView)
        val detailText = activity.findViewById<TextView>(R.id.error_view_detail_textView)
        titleLabel?.text = "Oops! Driver not signed in"
        detailText?.text = "No driver is signed in for vehicle:$vehicleId"
        cancelBtn?.setOnClickListener {
            val errView = activity.findViewById<View>(R.id.error_view_constraint)
            viewGroup.removeView(errView)
        }

        retryBtn?.setOnClickListener {
            val errView = activity?.findViewById<View>(R.id.error_view_constraint)
            viewGroup.removeView(errView)
        }
    }

    fun showNoBluetoothError(activity: Activity){
        LoggerHelper.writeToLog("Upfront Price Error: No bluetooth error: Generating UI", LogEnums.UPFRONT_PRICE.tag)
        val viewGroup = activity.findViewById<View>(android.R.id.content) as ViewGroup
        View.inflate(activity, R.layout.error_view, viewGroup)
        val cancelBtn = activity.findViewById<Button>(R.id.error_cancel_btn)
        val retryBtn = activity.findViewById<Button>(R.id.error_retry_btn)
        val titleLabel = activity.findViewById<TextView>(R.id.error_view_title_textView)
        val detailText = activity.findViewById<TextView>(R.id.error_view_detail_textView)
        titleLabel?.text = "Oops! Connection issues"
        detailText?.text = "No bluetooth connection with driver tablet. Please pair and try again"
        cancelBtn.visibility = View.INVISIBLE
        retryBtn.text = "Okay"
        retryBtn?.setOnClickListener {
            val errView = activity?.findViewById<View>(R.id.error_view_constraint)
            viewGroup.removeView(errView)
        }
    }

    fun showUpfrontPriceError(activity: Activity, errorMsg: String){
        LoggerHelper.writeToLog("Upfront Price Error: upfront price error: Generating UI", LogEnums.UPFRONT_PRICE.tag)
        val viewGroup = activity.findViewById<View>(android.R.id.content) as ViewGroup
        View.inflate(activity, R.layout.error_view, viewGroup)
        val cancelBtn = activity.findViewById<Button>(R.id.error_cancel_btn)
        val retryBtn = activity.findViewById<Button>(R.id.error_retry_btn)
        val titleLabel = activity.findViewById<TextView>(R.id.error_view_title_textView)
        val detailText = activity.findViewById<TextView>(R.id.error_view_detail_textView)
        titleLabel?.text = "Oops! Issue with address"
        detailText?.text = errorMsg
        cancelBtn.visibility = View.INVISIBLE
        retryBtn.text = "Okay"
        retryBtn?.setOnClickListener {
            val errView = activity.findViewById<View>(R.id.error_view_constraint)
            viewGroup.removeView(errView)
            (activity as MainActivity).checkIfDriverRejectUpfrontPrice()
        }
    }

    fun showGenericError(activity: Activity, message: String?){
        val messageForView = message ?: "There was an issue. Please try again"
        val viewGroup = activity.findViewById<View>(android.R.id.content) as ViewGroup
        View.inflate(activity, R.layout.error_view, viewGroup)
        val cancelBtn = activity.findViewById<Button>(R.id.error_cancel_btn)
        val retryBtn = activity.findViewById<Button>(R.id.error_retry_btn)
        val titleLabel = activity.findViewById<TextView>(R.id.error_view_title_textView)
        val detailText = activity.findViewById<TextView>(R.id.error_view_detail_textView)
        titleLabel?.text = "Oops! There was problem"
        detailText?.text = messageForView
        cancelBtn.visibility = View.INVISIBLE
        retryBtn.text = "Okay"
        retryBtn?.setOnClickListener {
            val errView = activity.findViewById<View>(R.id.error_view_constraint)
            viewGroup.removeView(errView)
        }
    }


}