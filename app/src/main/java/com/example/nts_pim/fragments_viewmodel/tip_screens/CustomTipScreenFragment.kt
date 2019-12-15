package com.example.nts_pim.fragments_viewmodel.tip_screens

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlinx.android.synthetic.main.custom_tip_screen.*
import java.text.DecimalFormat
import android.os.Handler
import java.util.*


class CustomTipScreenFragment : ScopedFragment() {

    // this is how we change from dollar to percentage
    private var customTipViewPercentageMode = false
    private var customTipViewAmountString = ""
    private var tripTotal = 00.00
    private var tripTotalWithTip = 00.00
    private val tripTotalDF = DecimalFormat("####00.00")
    private val tripTotalDFUnderTen = DecimalFormat("###0.00")
    private var timer: Timer? = null
    private lateinit var handler: Handler
    private var cursorTimer: CountDownTimer? = null

    val screenTimeOutTimer = object: CountDownTimer(45000, 1000) {
        // this is set to 45 seconds.
        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.custom_tip_screen, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        updateUI()
        screenTimeOutTimer.start()
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        screenTimeOutTimer.cancel()
                        screenTimeOutTimer.start()
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })

        timer = Timer()
        handler = Handler()
        close_custom_tip_screen_btn.setOnClickListener {
            val noTipChosen = 00.00.toFloat()
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            val action = CustomTipScreenFragmentDirections.backToTipScreenFragment(tripTotalWithTip.toFloat(), noTipChosen)
                .setTipScreenTripTotal(tripTotal.toFloat())
                .setDoneButtonTouchedOnCustomTipScreen(false)
                .setTipChosenFromCustomTipScreen(noTipChosen)
            cursorTimer?.cancel()
            if (navController.currentDestination?.id == (R.id.customTipScreenFragment)) {
                navController.navigate(action)
            }
        }
        //Touch listeners for making button turn grey on touch down.
        custom_tip_screen_one_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_one_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_one_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_two_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_two_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_two_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_three_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_three_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_three_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_four_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_four_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_four_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_five_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_five_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_five_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_six_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_six_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_six_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_seven_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_seven_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_seven_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_eight_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_eight_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_eight_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_nine_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_nine_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_nine_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_zero_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    setTextToGrey(custom_tip_screen_zero_btn)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setTextToWhite(custom_tip_screen_zero_btn)
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_done_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (custom_tip_screen_done_btn.isEnabled){
                        setTextToGrey(custom_tip_screen_done_btn)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
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
                        context!!,
                        R.drawable.ic_backspace_arrow_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    custom_tip_screen_backspace_btn.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_backspace_arrow_white
                    ))
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_plus_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    custom_tip_screen_plus_btn.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_add_circular_outlined_button_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    custom_tip_screen_plus_btn.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_add_circular_outlined_button
                    ))
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })
        custom_tip_screen_minus_btn.setOnTouchListener(View.OnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    custom_tip_screen_minus_btn.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_minus_circular_button_grey
                    ))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    custom_tip_screen_minus_btn.setImageDrawable(ContextCompat.getDrawable(
                        context!!,
                        R.drawable.ic_minus_circular_button
                    ))
                    v.performClick()
                    true
                }
                else -> {
                    false
                }
            }
        })


        custom_tip_screen_one_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "1"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_two_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "2"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_three_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "3"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_four_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "4"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_five_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "5"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_six_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "6"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_seven_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "7"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_eight_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "8"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_nine_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2) {
                val buttonValue = "9"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_zero_btn.setOnClickListener {
            if (customTipViewAmountString.length != 2 &&
                customTipViewAmountString.length == 1
            ) {
                val buttonValue = "0"
                customTipViewAmountString += buttonValue
                val tipAdded = customTipViewAmountString
                custom_tip_screen_editText.setText(tipAdded)
            }
        }
        custom_tip_screen_backspace_btn.setOnClickListener {
            backSpaceLogic()
        }
        custom_tip_screen_dollar_amt_btn.setOnClickListener {
            customTipViewPercentageMode = false
            dollar_textView.visibility = View.VISIBLE
            percent_textView.visibility = View.INVISIBLE
            custom_tip_screen_percentage_amt_btn.isEnabled = true
            custom_tip_screen_dollar_amt_btn.isEnabled = false
            customTipViewAmountString = ""
            custom_tip_screen_editText.setText(customTipViewAmountString)
        }
        custom_tip_screen_percentage_amt_btn.setOnClickListener {
            customTipViewPercentageMode = true
            dollar_textView.visibility = View.INVISIBLE
            percent_textView.visibility = View.VISIBLE
            custom_tip_screen_dollar_amt_btn.isEnabled = true
            custom_tip_screen_percentage_amt_btn.isEnabled = false
            customTipViewAmountString = ""
            custom_tip_screen_editText.setText(customTipViewAmountString)
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
           if (customTipViewAmountString != "0") {
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
            val tipPicked: Float
            if (customTipViewPercentageMode){
                tipPicked = tipAmountInEditText.toFloat() * 0.01.toFloat()
            } else {
                tipPicked = tipAmountInEditText.toFloat()
            }

            val action = CustomTipScreenFragmentDirections.backToTipScreenFragment(tripTotal.toFloat(), tipPicked)
                .setTipScreenTripTotal(tripTotal.toFloat())
                .setDoneButtonTouchedOnCustomTipScreen(true)
                .setTipChosenFromCustomTipScreen(tipPicked)
            val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
            if (navController.currentDestination?.id == (R.id.customTipScreenFragment)) {
                navController.navigate(action)
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
                if (s != "0"){
                    custom_tip_screen_editText.setTextColor(ContextCompat.getColor(context!!, R.color.whiteTextColor))
                }else {
                    custom_tip_screen_editText.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                }
                if (count == 1 || count == 2) {
                    custom_tip_screen_done_btn.isEnabled = true
                }
                if (before == 1 && start == 0) {
                    updateUI()
                }
                if(count == 1){
                   addMiddleCursor()
                }
                if(count == 2){
                    removeCursors()
                }
                updateTripWithTip()
            }
        })

    }

    private fun updateUI() {
        val args = arguments?.getFloat("tripTotalFromTipScreen") as Float
        if (args != 00.00.toFloat()) {
            if (args < 10) {
                val formattedArgs = tripTotalDFUnderTen.format(args)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                custom_tip_screen_trip_total_textView.text = "$tripTotalToString"
            } else {
                val formattedArgs = tripTotalDF.format(args)
                tripTotal = formattedArgs.toDouble()
                val tripTotalToString = formattedArgs.toString()
                custom_tip_screen_trip_total_textView.text = "$tripTotalToString"
            }
        }
    }
    private fun updateTripWithTip() {
        //customTipAmount = editTextField
        //tripTotal = amount passed along
        //tripTotalWithTip = customTipAmount and tripTotal combined
        if (customTipViewPercentageMode && customTipViewAmountString != "") {
            //this is for tip amount set for percentage
            val percentage = customTipViewAmountString.toDouble() * 00.01
            val tripTotalPercent = tripTotal * percentage
            val triptotalPercentFormatted = formatString(tripTotalPercent)
            val tripTotalFormatted = formatString(tripTotal)
            tripTotalWithTip = tripTotal + tripTotalPercent
            if (tripTotalWithTip < 10) {
                val formattedTripTotal = tripTotalDFUnderTen.format(tripTotalWithTip)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if(!custom_tip_screen_tip_breakdown_textView2.isVisible){
                    custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                }
                custom_tip_screen_tip_breakdown_textView2.text = (" ($$tripTotalFormatted + $$triptotalPercentFormatted tip)")
            } else {
                val formattedTripTotal = tripTotalDF.format(tripTotalWithTip)
                custom_tip_screen_trip_total_textView.text = "$$formattedTripTotal"
                if (formattedTripTotal.length > 5){
                    if(!custom_tip_screen_tip_breakdown_textView.isVisible){
                        custom_tip_screen_tip_breakdown_textView.visibility = View.VISIBLE
                    }
                    custom_tip_screen_tip_breakdown_textView.text = (" ($$tripTotalFormatted + $$triptotalPercentFormatted tip)")
                } else {
                    if(!custom_tip_screen_tip_breakdown_textView2.isVisible){
                        custom_tip_screen_tip_breakdown_textView2.visibility = View.VISIBLE
                    }
                    custom_tip_screen_tip_breakdown_textView2.text = (" ($$tripTotalFormatted + $$triptotalPercentFormatted tip)")
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
    private fun addBeginningCursor(){
            if(!beginningCursorBtn.isVisible){
                beginningCursorBtn.visibility = View.VISIBLE
                beginningCursorBtn.alpha = 1f
            }
            if (middleCursorBtn.isVisible) {
                middleCursorBtn.visibility = View.INVISIBLE
                middleCursorBtn.alpha = 0f
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
                middleCursorBtn.alpha =1f
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
        cursorTimer?.cancel()
    }
    private fun animateAlphaOnCursor(){
        if (beginningCursorBtn.isVisible){
            startCursorAnimation(beginningCursorBtn)
        }
        if (middleCursorBtn.isVisible){
          startCursorAnimation(middleCursorBtn)
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
        button.setTextColor((ContextCompat.getColor(context!!, R.color.grey)))
    }
    private fun setTextToWhite(button:Button){
        button.setTextColor((ContextCompat.getColor(context!!, R.color.whiteTextColor)))
    }
    private fun formatString(enteredDouble: Double):String{
        if(enteredDouble < 10){
            return tripTotalDFUnderTen.format(enteredDouble)
        }
        return tripTotalDF.format(enteredDouble)
    }
    private fun backSpaceLogic(){
        val tipLength = customTipViewAmountString.length
        if (tipLength > 0) {
            val updatedTipValue = customTipViewAmountString.dropLast(1)
            customTipViewAmountString = updatedTipValue
            custom_tip_screen_editText.setText(customTipViewAmountString)
            custom_tip_screen_backspace_btn.isEnabled = true
        }
    }
    private fun toTipScreenWithTip(){

    }
    override fun onDestroy() {
        cursorTimer?.cancel()
        super.onDestroy()
        timer?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}