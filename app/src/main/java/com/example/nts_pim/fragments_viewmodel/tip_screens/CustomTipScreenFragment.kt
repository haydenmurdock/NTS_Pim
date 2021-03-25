package com.example.nts_pim.fragments_viewmodel.tip_screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.custom_tip_screen.*
import java.text.DecimalFormat


class CustomTipScreenFragment : ScopedFragment() {

    // this is how we change from dollar to percentage
    private var customTipViewPercentageMode = false
    private var customTipViewAmountString = ""
    private var tripTotal = 00.00
    private var tripTotalWithTip = 00.00
    private var tipPicked: Float = 0.0.toFloat()
    private val tripTotalDF = DecimalFormat("####00.00")
    private val tripTotalDFUnderTen = DecimalFormat("###0.00")
    private var cursorTimer: CountDownTimer? = null
    private lateinit var callbackViewModel: CallBackViewModel
    private var tipIsOver500 = false
    private var tipInCaseOfThreshold = ""
    private var isOver100ScreenShowing = false
    private var over100View: View? = null
    private var viewGroup: ViewGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.custom_tip_screen, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callbackViewModel = ViewModelProvider(this, factory)
            .get(CallBackViewModel::class.java)
        updateUI()
        view.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        close_custom_tip_screen_btn.setOnClickListener {
            if(!isOver100ScreenShowing){
               toTipScreen()
            }
        }
        //Touch listeners for making button turn grey on touch down.
        custom_tip_screen_one_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_one_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_one_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_two_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_two_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_two_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_three_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_three_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_three_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_four_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_four_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_four_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_five_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_five_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_five_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_six_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_six_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_six_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_seven_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_seven_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_seven_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_eight_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_eight_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_eight_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_nine_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_nine_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_nine_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_zero_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_zero_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_zero_btn)
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_done_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (custom_tip_screen_done_btn.isEnabled){
                        setTextToGrey(custom_tip_screen_done_btn)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_backspace_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    custom_tip_screen_backspace_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_backspace_arrow_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    custom_tip_screen_backspace_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_backspace_arrow_white
                    ))
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_plus_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    custom_tip_screen_plus_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_add_circular_outlined_button_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    custom_tip_screen_plus_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_add_circular_outlined_button
                    ))
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_minus_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    custom_tip_screen_minus_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_minus_circular_button_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    custom_tip_screen_minus_btn.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_minus_circular_button
                    ))
                    if(!isOver100ScreenShowing){
                        v.performClick()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }))
        custom_tip_screen_percentage_amt_btn.setOnTouchListener((View.OnTouchListener { v, event ->
            when(event?.action){
                MotionEvent.ACTION_UP ->{
                    if(!isOver100ScreenShowing){
                        v.performClick()
                     }
                    true
                } else -> {
                false
                }
            }
        }))
        custom_tip_screen_one_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "1"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_two_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "2"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_three_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "3"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_four_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "4"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_five_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "5"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_six_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "6"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_seven_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "7"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_eight_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "8"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_nine_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString)) {
                val buttonValue = "9"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_zero_btn.setOnClickListener {
            if (isLongEnough(customTipViewAmountString) && customTipViewAmountString.isNotEmpty()) {
                val buttonValue = "0"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_backspace_btn.setOnClickListener {
            if(!isOver100ScreenShowing){
                backSpaceLogic()
            }
        }
        custom_tip_screen_dollar_amt_btn.setOnClickListener {
            if(!isOver100ScreenShowing){
                customTipViewPercentageMode = false
                dollar_textView.visibility = View.VISIBLE
                percent_textView.visibility = View.INVISIBLE
                custom_tip_screen_percentage_amt_btn.isEnabled = true
                custom_tip_screen_dollar_amt_btn.isEnabled = false
                customTipViewAmountString = ""
                custom_tip_screen_editText.setText(customTipViewAmountString)
            }
        }
        custom_tip_screen_percentage_amt_btn.setOnClickListener {
            if(!isOver100ScreenShowing){
                customTipViewPercentageMode = true
                dollar_textView.visibility = View.INVISIBLE
                percent_textView.visibility = View.VISIBLE
                custom_tip_screen_dollar_amt_btn.isEnabled = true
                custom_tip_screen_percentage_amt_btn.isEnabled = false
                customTipViewAmountString = ""
                custom_tip_screen_editText.setText(customTipViewAmountString)
            }
        }
        custom_tip_screen_plus_btn.setOnClickListener {
            if (customTipViewAmountString == "") {
                customTipViewAmountString = "0"
            }
            val tipInt = customTipViewAmountString.toInt()
            val newValue = tipInt + 1
            customTipViewAmountString = newValue.toString()
            custom_tip_screen_editText.setText(customTipViewAmountString)
        }
        custom_tip_screen_minus_btn.setOnClickListener {
           if (customTipViewAmountString != "0" && customTipViewAmountString != "") {
               val tipInt = customTipViewAmountString.toInt()
               val newValue = tipInt - 1
               customTipViewAmountString = newValue.toString()
               custom_tip_screen_editText.setText(customTipViewAmountString)
           }
            if (customTipViewAmountString == "0"){
                backSpaceLogic()
            }
        }
        custom_tip_screen_done_btn.setOnClickListener {
            val  tipAmountInEditText = custom_tip_screen_editText.text.toString()
            removeCursors()
            if(customTipViewPercentageMode){
                tipPicked = tipPicked
            } else {
                tipPicked = tipAmountInEditText.toFloat()
            }
            val isAmountOver = showOver100Alert(tipPicked)
            if(!isAmountOver){
                toSquare()
            } else {
                //show over 100 box.
                inflateOver100Alert()
            }

        }
        custom_tip_screen_editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s!!.isEmpty()) {
                    custom_tip_screen_editText.setText("0")
                    custom_tip_screen_editText.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                    customTipViewAmountString = ""
                    custom_tip_screen_done_btn.isEnabled = false
                    addBeginningCursor()
                    if(custom_tip_screen_tip_breakdown_textView.isVisible){
                        custom_tip_screen_tip_breakdown_textView.visibility = View.INVISIBLE
                    }
                    if(custom_tip_screen_tip_breakdown_textView2.isVisible){
                        custom_tip_screen_tip_breakdown_textView2.visibility = View.INVISIBLE
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString() == "500"){
                    Log.i("test", "This was skipped")
                    addCursor(count)
                    return
                }
                if (s != "0"){
                    custom_tip_screen_editText.setTextColor(ContextCompat.getColor(context!!, R.color.whiteTextColor))
                }else {
                    custom_tip_screen_editText.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                }
                if (count == 1 || count == 2 || count == 3) {
                    custom_tip_screen_done_btn.isEnabled = true
                }
                if (before == 1 && start == 0) {
                    updateUI()
                }
                if(count <= 3){
                    addCursor(count)
                   val showTipCutOff = overFiveHundred(s.toString())
                    if(showTipCutOff){
                    showOver500Toast()
                    }
                }
                updateTripWithTip()
            }
        })

    }
    private fun isLongEnough(tip: String): Boolean {
        if (!customTipViewPercentageMode){
            if (tip.length != 3){
                return true
            }
            return false
        } else {
            if (tip.length != 2){
                return true
            }
            return false
        }
    }
    private fun overFiveHundred(tip: String):Boolean {
        if(tip == ""){
            return false
        }
        val toInt = tip.toInt()
        if(toInt >= 500){
            tipIsOver500 = true
            tipInCaseOfThreshold = customTipViewAmountString.dropLast(1)
            customTipViewAmountString = "500"
            return true
        }
        return false
    }
    private fun showOver500Toast(){
        Toast.makeText(this.context, "Tip amount cannot exceed $500", Toast.LENGTH_LONG).show()
    }
    private fun showOver100Alert(tip: Float):Boolean {
        if (tip >= 100){
            return true
        }
        return false
    }
    @SuppressLint("ResourceType")
    private fun inflateOver100Alert() {
        viewGroup = activity?.findViewById<View>(android.R.id.content) as ViewGroup
        over100View = View.inflate(this.context, R.layout.tip_over_one_hundred_view, viewGroup)
        blocker_view.visibility = View.VISIBLE
        custom_tip_screen_percentage_amt_btn.alpha = 0.5.toFloat()
        isOver100ScreenShowing = true
        val tipAmount = activity?.findViewById<TextView>(R.id.over_oneHundred_textView)
        val goBackButton = activity?.findViewById<Button>(R.id.over_oneHundred_goBack_btn)
        val confirmButton = activity?.findViewById<Button>(R.id.over_OneHundered_confirm_button)
        tipAmount?.text = "$$customTipViewAmountString"
        goBackButton?.setOnClickListener {
            Log.i("test", "go back button clicked")
            remove100View()
            setTextToWhite(custom_tip_screen_done_btn)
        }
        confirmButton?.setOnClickListener {
            Log.i("test", "confirm button clicked")
            toSquare()
            remove100View()
        }
    }

    private fun toSquare(){
        callbackViewModel.setTipAmount(tipPicked.toDouble())
        val action = CustomTipScreenFragmentDirections.backToTipScreenFragment(tripTotal.toFloat(), tipPicked)
            .setTipScreenTripTotal(tripTotal.toFloat())
            .setPercentagePickedForCustomTip(customTipViewPercentageMode)
            .setDoneButtonTouchedOnCustomTipScreen(true)
            .setTipChosenFromCustomTipScreen(tipPicked)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == (R.id.customTipScreenFragment)) {
            navController.navigate(action)
        }
        LoggerHelper.writeToLog("Customer is viewing Custom Tip Screen", null)
    }

    private fun toTipScreen(){
        val noTipChosen = 00.00.toFloat()
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        val action = CustomTipScreenFragmentDirections.backToTipScreenFragment(tripTotalWithTip.toFloat(), noTipChosen)
            .setTipScreenTripTotal(tripTotal.toFloat())
            .setDoneButtonTouchedOnCustomTipScreen(false)
            .setTipChosenFromCustomTipScreen(noTipChosen)
        cursorTimer?.cancel()
        if (navController.currentDestination?.id == (R.id.customTipScreenFragment)) {
            navController.navigate(action)
        }
    }

    private fun remove100View() {
        blocker_view.visibility = View.INVISIBLE
        custom_tip_screen_percentage_amt_btn.alpha = 1.0.toFloat()
        if(over100View == null){
            Log.i("test", "over100View is null. returning")
            return
        }
        if(over100View!!.isVisible && viewGroup != null){
            Log.i("test", "over100View is visible. trying to remove view")
            val view = activity?.findViewById<View>(R.id.over_oneHunderedContraintLayout)
            viewGroup!!.removeView(view)
        } else {
            Log.i("test", "over100View is not visible. did not remove view")
        }
        isOver100ScreenShowing = false
    }
    private fun updateUI() {
        val args = arguments?.getFloat("tripTotalFromTipScreen") as Float
        if (args != 00.00.toFloat()) {
            if (args < 10) {
                val formattedArgs = tripTotalDFUnderTen.format(args)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                custom_tip_screen_trip_total_textView.text = "$$tripTotalToString"
            } else {
                val formattedArgs = tripTotalDF.format(args)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                custom_tip_screen_trip_total_textView.text = "$$tripTotalToString"
            }
        }
    }
    private fun updateTripWithTip() {
        //customTipAmount = editTextField
        //tripTotal = amount passed along
        //tripTotalWithTip = customTipAmount and tripTotal combined
        Log.i("test", "Trips is being updated")
        if(tipIsOver500){
            custom_tip_screen_editText.setText("500")
        }
        if (customTipViewPercentageMode && customTipViewAmountString != "") {
            //this is for tip amount set for percentage
            val percentage = customTipViewAmountString.toDouble() * 00.01
            val tripTotalPercent = tripTotal * percentage
            val tripTotalPercentFormatted = formatString(tripTotalPercent)
            tipPicked = tripTotalPercentFormatted.toFloat()
            val tripTotalFormatted = formatString(tripTotal)
            tripTotalWithTip = tripTotalFormatted.toDouble() + tripTotalPercentFormatted.toDouble()
            if (tripTotalWithTip < 10) {
                val formattedTripTotal = tripTotalDFUnderTen.format(tripTotalWithTip)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if(!custom_tip_screen_tip_breakdown_textView2.isVisible){
                    custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                }
                custom_tip_screen_tip_breakdown_textView2.text = (" ($$tripTotalFormatted + $$tripTotalPercentFormatted tip)")
            } else {
                val formattedTripTotal = tripTotalDF.format(tripTotalWithTip)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if (formattedTripTotal.length > 5){
                    if(!custom_tip_screen_tip_breakdown_textView.isVisible){
                        custom_tip_screen_tip_breakdown_textView.visibility = View.VISIBLE
                    }
                    custom_tip_screen_tip_breakdown_textView.text = (" ($$tripTotalFormatted + $$tripTotalPercentFormatted tip)")
                } else {
                    if(!custom_tip_screen_tip_breakdown_textView2.isVisible){
                        custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                    }
                    custom_tip_screen_tip_breakdown_textView2.text = (" ($$tripTotalFormatted + $$tripTotalPercentFormatted tip)")
                }
            }
        } else if (!customTipViewPercentageMode && customTipViewAmountString != "") {
           //This is for tip amount set for dollar amount
            tripTotalWithTip = tripTotal + customTipViewAmountString.toDouble()
            val tripTotalFormatted = formatString(tripTotal)
            if (tripTotalWithTip < 10) {
                val formattedTripTotal = tripTotalDFUnderTen.format(tripTotalWithTip)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if(!custom_tip_screen_tip_breakdown_textView2.isVisible){
                    custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                }
                custom_tip_screen_tip_breakdown_textView2.text =" ($$tripTotalFormatted + $${customTipViewAmountString} tip)"
            } else {
                val formattedTripTotal = tripTotalDF.format(tripTotalWithTip)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if(!custom_tip_screen_tip_breakdown_textView2.isVisible){
                    custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                }
                custom_tip_screen_tip_breakdown_textView2.text =" ($$tripTotalFormatted + $${customTipViewAmountString} tip)"
            }
        } else {
            if (tripTotal < 10) {
                val formattedTripTotal = tripTotalDFUnderTen.format(tripTotal)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if(custom_tip_screen_tip_breakdown_textView.isVisible){
                    custom_tip_screen_tip_breakdown_textView.visibility = View.INVISIBLE
                }
                if(custom_tip_screen_tip_breakdown_textView2.isVisible){
                    custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                }
            } else {
                val formattedTripTotal = tripTotalDF.format(tripTotal)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if(custom_tip_screen_tip_breakdown_textView.isVisible){
                    custom_tip_screen_tip_breakdown_textView.visibility = View.INVISIBLE
                }
                if(custom_tip_screen_tip_breakdown_textView2.isVisible){
                    custom_tip_screen_tip_breakdown_textView2.visibility = View.INVISIBLE
                }
            }
        }
    }
    private fun addCursor(length: Int){
            when(length){
                1 -> {
                    addBeginningCursor()
                }
                2 -> {
                   addMiddleCursor()
                }
                3 -> {
                    addEndCursor()
                }
                0 -> {
                    removeCursors()
                }
            }
    }
    private fun addBeginningCursor(){
        if(!beginningCursorBtn.isVisible){
            beginningCursorBtn.visibility = View.VISIBLE
            beginningCursorBtn.alpha = 1f
        }
        if (middleCursorBtn.isVisible) {
            middleCursorBtn.visibility = View.INVISIBLE
            middleCursorBtn.alpha = 0f
        }
        if (endCursorBtn.isVisible){
            endCursorBtn.visibility = View.INVISIBLE
            endCursorBtn.alpha = 0f
        }
        animateAlphaOnCursor()
    }
    private fun addMiddleCursor(){
            if(beginningCursorBtn.isVisible){
                beginningCursorBtn.visibility = View.INVISIBLE
                beginningCursorBtn.alpha = 0f
            }
            if(!middleCursorBtn.isVisible){
                middleCursorBtn.visibility = View.VISIBLE
                middleCursorBtn.alpha = 1f
            }
            if(endCursorBtn.isVisible){
                endCursorBtn.visibility = View.INVISIBLE
                endCursorBtn.alpha = 0f
            }
        animateAlphaOnCursor()
    }

    private fun addEndCursor(){
        if(beginningCursorBtn.isVisible){
            beginningCursorBtn.visibility = View.INVISIBLE
            beginningCursorBtn.alpha = 0f
        }
        if(middleCursorBtn.isVisible){
            middleCursorBtn.visibility = View.INVISIBLE
            middleCursorBtn.alpha = 0f
        }
        if(!endCursorBtn.isVisible){
            endCursorBtn.visibility = View.VISIBLE
            endCursorBtn.alpha = 0f
        }
        animateAlphaOnCursor()
    }
    private fun removeCursors(){
        beginningCursorBtn.visibility = View.INVISIBLE
        beginningCursorBtn.alpha = 0f
        beginningCursorBtn.animate().cancel()
        middleCursorBtn.visibility = View.INVISIBLE
        middleCursorBtn.alpha = 0f
        middleCursorBtn.animate().cancel()
        endCursorBtn.visibility = View.INVISIBLE
        endCursorBtn.alpha = 0f
        endCursorBtn.animate().cancel()
        cursorTimer?.cancel()
    }
    private fun animateAlphaOnCursor(){
        if (beginningCursorBtn.isVisible){
            startCursorAnimation(beginningCursorBtn)
        }
        if (middleCursorBtn.isVisible){
          startCursorAnimation(middleCursorBtn)
        }
        if (endCursorBtn.isVisible){
            startCursorAnimation(endCursorBtn)
        }
    }
    private fun startCursorAnimation(button: Button) {
        cursorTimer?.cancel()
        cursorTimer = object: CountDownTimer(120000, 600) {
                override fun onTick(millisUntilFinished: Long) {
                    if (button.alpha != 1f) {
                        button.animate().alpha(1f).setDuration(250).start()
                    } else {
                        button.animate().alpha(0.0f).setDuration(500).start()
                    }
                }

                override fun onFinish() {
                }
            }.start()
    }
    private fun setTextToGrey(button: Button){
        button.setTextColor((ContextCompat.getColor(requireContext(), R.color.grey)))
    }
    private fun setTextToWhite(button:Button){
        button.setTextColor((ContextCompat.getColor(requireContext(), R.color.whiteTextColor)))
    }
    private fun formatString(enteredDouble: Double):String{
        if(enteredDouble < 10){
            return tripTotalDFUnderTen.format(enteredDouble)
        }
        return tripTotalDF.format(enteredDouble)
    }
    private fun backSpaceLogic(){
        val tipLength = customTipViewAmountString.length
        if(tipIsOver500){
            tipIsOver500 = false
            Log.i("test", "$tipInCaseOfThreshold is the old amount")
            customTipViewAmountString = tipInCaseOfThreshold
            Log.i("test", "$customTipViewAmountString should show on edit text")
            custom_tip_screen_editText.setText(customTipViewAmountString)
        } else if (tipLength > 0){
            val updatedTipValue = customTipViewAmountString.dropLast(1)
            customTipViewAmountString = updatedTipValue
            custom_tip_screen_editText.setText(customTipViewAmountString)
            custom_tip_screen_backspace_btn.isEnabled = true
        }
    }

    override fun onDestroy() {
        cursorTimer?.cancel()
        super.onDestroy()
    }
}

