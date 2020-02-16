package com.example.nts_pim.utilities.keyboards

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.KeyboardWatcher

class PhoneKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    // keyboard keys (buttons)
    private var mButton1: Button? = null
    private var mButton2: Button? = null
    private var mButton3: Button? = null
    private var mButton4: Button? = null
    private var mButton5: Button? = null
    private var mButton6: Button? = null
    private var mButton7: Button? = null
    private var mButton8: Button? = null
    private var mButton9: Button? = null
    private var mButton0: ImageButton? = null
    private var mButtonDelete: ImageButton? = null
    private var mButtonEnter: ImageButton? = null

    // This will map the button resource id to the String value that we want to
    // input when that button is clicked.
    internal var keyValues = SparseArray<String>()

    // Our communication link to the EditText
    internal var inputConnection: InputConnection? = null

    init {
        init(context)
    }

    private fun init(context: Context) {

        // initialize buttons
        LayoutInflater.from(context).inflate(R.layout.phone_keyboard, this, true)
        mButton1 = findViewById<View>(R.id.button_1) as Button
        mButton2 = findViewById<View>(R.id.button_2) as Button
        mButton3 = findViewById<View>(R.id.button_3) as Button
        mButton4 = findViewById<View>(R.id.button_4) as Button
        mButton5 = findViewById<View>(R.id.button_5) as Button
        mButton6 = findViewById<View>(R.id.button_6) as Button
        mButton7 = findViewById<View>(R.id.button_7) as Button
        mButton8 = findViewById<View>(R.id.button_8) as Button
        mButton9 = findViewById<View>(R.id.button_9) as Button
        mButton0 = findViewById<View>(R.id.button_0) as ImageButton
        mButtonDelete = findViewById<View>(R.id.button_delete) as ImageButton
        mButtonEnter = findViewById<View>(R.id.button_enter) as ImageButton

        // set button click listeners
        mButton1!!.setOnClickListener(this)
        mButton2!!.setOnClickListener(this)
        mButton3!!.setOnClickListener(this)
        mButton4!!.setOnClickListener(this)
        mButton5!!.setOnClickListener(this)
        mButton6!!.setOnClickListener(this)
        mButton7!!.setOnClickListener(this)
        mButton8!!.setOnClickListener(this)
        mButton9!!.setOnClickListener(this)
        mButton0!!.setOnClickListener(this)
        mButtonDelete!!.setOnClickListener(this)
        mButtonEnter!!.setOnClickListener(this)

        // map buttons IDs to input strings
        keyValues.put(R.id.button_1, "1")
        keyValues.put(R.id.button_2, "2")
        keyValues.put(R.id.button_3, "3")
        keyValues.put(R.id.button_4, "4")
        keyValues.put(R.id.button_5, "5")
        keyValues.put(R.id.button_6, "6")
        keyValues.put(R.id.button_7, "7")
        keyValues.put(R.id.button_8, "8")
        keyValues.put(R.id.button_9, "9")
        keyValues.put(R.id.button_0, "0")
        keyValues.put(R.id.button_enter, "")

    }

    override fun onClick(v: View) {

        // do nothing if the InputConnection has not been set yet
        if (inputConnection == null) return
        if (v.id == R.id.button_enter) {
            KeyboardWatcher.enterButtonPressed("phone")
        }

        // Delete text or input key value
        // All communication goes through the InputConnection
        if (v.id != R.id.button_delete){
            KeyboardWatcher.phoneNumberIsGoingForward()
        }
        if (v.id == R.id.button_delete) {
            val characterBefore = inputConnection?.getTextBeforeCursor(1,0)
            val selectedText = inputConnection!!.getSelectedText(0)
            if (TextUtils.isEmpty(selectedText) && characterBefore != "") {
                KeyboardWatcher.phoneNumberKeyboardIsGoingBackward()
                // no selection, so delete previous character
                inputConnection!!.deleteSurroundingText(1, 0)
            } else {
                // delete the selection
                inputConnection!!.commitText("", 1)
            }
        } else {
            val value = keyValues.get(v.id)
            inputConnection!!.commitText(value, 1)
        }
    }

    // The activity (or some parent or controller) must give us
    // a reference to the current EditText's InputConnection
    fun setInputConnection(ic: InputConnection) {
        this.inputConnection = ic
    }
}// constructors