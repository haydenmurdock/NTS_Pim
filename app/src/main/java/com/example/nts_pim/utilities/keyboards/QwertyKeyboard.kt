package com.example.nts_pim.utilities.keyboards

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.KeyboardWatcher
import com.example.nts_pim.utilities.view_helper.ViewHelper

class QwertyKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    // keyboard keys (buttons)
    //line 1
    private var mButton1: Button? = null
    private var mButton2: Button? = null
    private var mButton3: Button? = null
    private var mButton4: Button? = null
    private var mButton5: Button? = null
    private var mButton6: Button? = null
    private var mButton7: Button? = null
    private var mButton8: Button? = null
    private var mButton9: Button? = null
    private var mButton0: Button? = null
    // line 2
    private var mButtonQ: Button? = null
    private var mButtonW: Button? = null
    private var mButtonE: Button? = null
    private var mButtonR: Button? = null
    private var mButtonT: Button? = null
    private var mButtonY: Button? = null
    private var mButtonU: Button? = null
    private var mButtonI: Button? = null
    private var mButtonO: Button? = null
    private var mButtonP: Button? = null
    // line 3
    private var mButtonA: Button? = null
    private var mButtonS: Button? = null
    private var mButtonD: Button? = null
    private var mButtonF: Button? = null
    private var mButtonG: Button? = null
    private var mButtonH: Button? = null
    private var mButtonJ: Button? = null
    private var mButtonK: Button? = null
    private var mButtonL: Button? = null
    //line 4
    private var mButtonZ: Button? = null
    private var mButtonX: Button? = null
    private var mButtonC: Button? = null
    private var mButtonV: Button? = null
    private var mButtonB: Button? = null
    private var mButtonN: Button? = null
    private var mButtonM: Button? = null
    private var mButtonPointer: Button? = null
    private var mButtonEmail: Button? = null
    // line 5
    private var mButtonCapChange: ImageButton? = null
    private var mButtonSpecialChar: ImageButton? = null
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
        LayoutInflater.from(context).inflate(R.layout.qwerty_keyboard, this, true)

        mButton1 = findViewById<View>(R.id.capKeyboard_button_One) as Button
        mButton2 = findViewById<View>(R.id.capKeyboard_button_Two) as Button
        mButton3 = findViewById<View>(R.id.capKeyboard_button_Three) as Button
        mButton4 = findViewById<View>(R.id.capKeyboard_button_Four) as Button
        mButton5 = findViewById<View>(R.id.capKeyboard_button_Five) as Button
        mButton6 = findViewById<View>(R.id.capKeyboard_button_Six) as Button
        mButton7 = findViewById<View>(R.id.capKeyboard_button_Seven) as Button
        mButton8 = findViewById<View>(R.id.capKeyboard_button_Eight) as Button
        mButton9 = findViewById<View>(R.id.capKeyboard_button_Nine) as Button
        mButton0 = findViewById<View>(R.id.capKeyboard_button_Zero) as Button

        mButtonQ = findViewById<View>(R.id.capKeyboard_button_Q) as Button
        mButtonW = findViewById<View>(R.id.capKeyboard_button_W) as Button
        mButtonE = findViewById<View>(R.id.capKeyboard_button_E) as Button
        mButtonR = findViewById<View>(R.id.capKeyboard_button_R) as Button
        mButtonT = findViewById<View>(R.id.capKeyboard_button_T) as Button
        mButtonY = findViewById<View>(R.id.capKeyboard_button_Y) as Button
        mButtonU = findViewById<View>(R.id.capKeyboard_button_U) as Button
        mButtonI = findViewById<View>(R.id.capKeyboard_button_I) as Button
        mButtonO = findViewById<View>(R.id.capKeyboard_button_O) as Button
        mButtonP = findViewById<View>(R.id.capKeyboard_button_P) as Button

        mButtonA = findViewById<View>(R.id.capKeyboard_button_A) as Button
        mButtonS = findViewById<View>(R.id.capKeyboard_button_S) as Button
        mButtonD = findViewById<View>(R.id.capKeyboard_button_D) as Button
        mButtonF = findViewById<View>(R.id.capKeyboard_button_F) as Button
        mButtonG = findViewById<View>(R.id.capKeyboard_button_G) as Button
        mButtonH = findViewById<View>(R.id.capKeyboard_button_H) as Button
        mButtonJ = findViewById<View>(R.id.capKeyboard_button_J) as Button
        mButtonK = findViewById<View>(R.id.capKeyboard_button_K) as Button
        mButtonL = findViewById<View>(R.id.capKeyboard_button_L) as Button

        mButtonZ = findViewById<View>(R.id.capKeyboard_button_Z) as Button
        mButtonX = findViewById<View>(R.id.capKeyboard_button_X) as Button
        mButtonC = findViewById<View>(R.id.capKeyboard_button_C) as Button
        mButtonV = findViewById<View>(R.id.capKeyboard_button_V) as Button
        mButtonB = findViewById<View>(R.id.capKeyboard_button_B) as Button
        mButtonN = findViewById<View>(R.id.capKeyboard_button_N) as Button
        mButtonM = findViewById<View>(R.id.capKeyboard_button_M) as Button
        mButtonPointer = findViewById<View>(R.id.capKeyboard_button_pointer) as Button
        mButtonEmail = findViewById<View>(R.id.capKeyboard_button_emailSign) as Button

        mButtonCapChange = findViewById<View>(R.id.capKeyboard_button_changeCap) as ImageButton
        mButtonSpecialChar = findViewById<View>(R.id.capKeyboard_button_specialChar) as ImageButton
        mButtonDelete = findViewById<View>(R.id.capKeyboard_button_delete) as ImageButton
        mButtonEnter = findViewById<View>(R.id.capKeyboard_button_enter) as ImageButton


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

        mButtonQ!!.setOnClickListener(this)
        mButtonW!!.setOnClickListener(this)
        mButtonE!!.setOnClickListener(this)
        mButtonR!!.setOnClickListener(this)
        mButtonT!!.setOnClickListener(this)
        mButtonY!!.setOnClickListener(this)
        mButtonU!!.setOnClickListener(this)
        mButtonI!!.setOnClickListener(this)
        mButtonO!!.setOnClickListener(this)
        mButtonP!!.setOnClickListener(this)

        mButtonA!!.setOnClickListener(this)
        mButtonS!!.setOnClickListener(this)
        mButtonD!!.setOnClickListener(this)
        mButtonF!!.setOnClickListener(this)
        mButtonG!!.setOnClickListener(this)
        mButtonH!!.setOnClickListener(this)
        mButtonJ!!.setOnClickListener(this)
        mButtonK!!.setOnClickListener(this)
        mButtonL!!.setOnClickListener(this)

        mButtonZ!!.setOnClickListener(this)
        mButtonX!!.setOnClickListener(this)
        mButtonC!!.setOnClickListener(this)
        mButtonV!!.setOnClickListener(this)
        mButtonB!!.setOnClickListener(this)
        mButtonN!!.setOnClickListener(this)
        mButtonM!!.setOnClickListener(this)
        mButtonPointer!!.setOnClickListener(this)
        mButtonEmail!!.setOnClickListener(this)

        mButtonCapChange!!.setOnClickListener(this)
        mButtonSpecialChar!!.setOnClickListener(this)
        mButtonDelete!!.setOnClickListener(this)
        mButtonEnter!!.setOnClickListener(this)

        // map buttons IDs to input strings
        keyValues.put(R.id.capKeyboard_button_One, "1")
        keyValues.put(R.id.capKeyboard_button_Two, "2")
        keyValues.put(R.id.capKeyboard_button_Three, "3")
        keyValues.put(R.id.capKeyboard_button_Four, "4")
        keyValues.put(R.id.capKeyboard_button_Five, "5")
        keyValues.put(R.id.capKeyboard_button_Six, "6")
        keyValues.put(R.id.capKeyboard_button_Seven, "7")
        keyValues.put(R.id.capKeyboard_button_Eight, "8")
        keyValues.put(R.id.capKeyboard_button_Nine, "9")
        keyValues.put(R.id.capKeyboard_button_Zero, "0")

        if (ViewHelper.isCapsLockOn && !ViewHelper.isSpecialCharOn) {
            keyValues.put(R.id.capKeyboard_button_Q, "Q")
            mButtonQ!!.text = "Q"
            keyValues.put(R.id.capKeyboard_button_W, "W")
            mButtonW!!.text = "W"
            keyValues.put(R.id.capKeyboard_button_E, "E")
            mButtonE!!.text = "E"
            keyValues.put(R.id.capKeyboard_button_R, "R")
            mButtonR!!.text = "R"
            keyValues.put(R.id.capKeyboard_button_T, "T")
            mButtonT!!.text = "T"
            keyValues.put(R.id.capKeyboard_button_Y, "Y")
            mButtonY!!.text = "Y"
            keyValues.put(R.id.capKeyboard_button_U, "U")
            mButtonU!!.text = "U"
            keyValues.put(R.id.capKeyboard_button_I, "I")
            mButtonI!!.text = "I"
            keyValues.put(R.id.capKeyboard_button_O, "O")
            mButtonO!!.text = "O"
            keyValues.put(R.id.capKeyboard_button_P, "P")
            mButtonP!!.text = "P"

            keyValues.put(R.id.capKeyboard_button_A, "A")
            mButtonA!!.text = "A"
            keyValues.put(R.id.capKeyboard_button_S, "S")
            mButtonS!!.text = "S"
            keyValues.put(R.id.capKeyboard_button_D, "D")
            mButtonD!!.text = "D"
            keyValues.put(R.id.capKeyboard_button_F, "F")
            mButtonF!!.text = "F"
            keyValues.put(R.id.capKeyboard_button_G, "G")
            mButtonG!!.text = "G"
            keyValues.put(R.id.capKeyboard_button_H, "H")
            mButtonH!!.text = "H"
            keyValues.put(R.id.capKeyboard_button_J, "J")
            mButtonJ!!.text = "J"
            keyValues.put(R.id.capKeyboard_button_K, "K")
            mButtonK!!.text = "K"
            keyValues.put(R.id.capKeyboard_button_L, "L")
            mButtonL!!.text = "L"

            keyValues.put(R.id.capKeyboard_button_Z, "Z")
            mButtonZ!!.text = "Z"
            keyValues.put(R.id.capKeyboard_button_X, "X")
            mButtonX!!.text = "X"
            keyValues.put(R.id.capKeyboard_button_C, "C")
            mButtonC!!.text = "C"
            keyValues.put(R.id.capKeyboard_button_V, "V")
            mButtonV!!.text = "V"
            keyValues.put(R.id.capKeyboard_button_B, "B")
            mButtonB!!.text = "B"
            keyValues.put(R.id.capKeyboard_button_N, "N")
            mButtonN!!.text = "N"
            keyValues.put(R.id.capKeyboard_button_M, "M")
            mButtonM!!.text = "M"

            mButtonSpecialChar!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                R.drawable.ic_hash))

            mButtonCapChange!!.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_upload_button_white))
        } else if (!ViewHelper.isCapsLockOn && !ViewHelper.isSpecialCharOn){
            keyValues.put(R.id.capKeyboard_button_Q, "q")
            mButtonQ!!.text = "q"
            keyValues.put(R.id.capKeyboard_button_W, "w")
            mButtonW!!.text = "w"
            keyValues.put(R.id.capKeyboard_button_E, "e")
            mButtonE!!.text = "e"
            keyValues.put(R.id.capKeyboard_button_R, "r")
            mButtonR!!.text = "r"
            keyValues.put(R.id.capKeyboard_button_T, "t")
            mButtonT!!.text = "t"
            keyValues.put(R.id.capKeyboard_button_Y, "y")
            mButtonY!!.text = "y"
            keyValues.put(R.id.capKeyboard_button_U, "u")
            mButtonU!!.text = "u"
            keyValues.put(R.id.capKeyboard_button_I, "i")
            mButtonI!!.text = "i"
            keyValues.put(R.id.capKeyboard_button_O, "o")
            mButtonO!!.text = "o"
            keyValues.put(R.id.capKeyboard_button_P, "p")
            mButtonP!!.text = "p"

            keyValues.put(R.id.capKeyboard_button_A, "a")
            mButtonA!!.text = "a"
            keyValues.put(R.id.capKeyboard_button_S, "s")
            mButtonS!!.text = "s"
            keyValues.put(R.id.capKeyboard_button_D, "d")
            mButtonD!!.text = "d"
            keyValues.put(R.id.capKeyboard_button_F, "f")
            mButtonF!!.text = "f"
            keyValues.put(R.id.capKeyboard_button_G, "g")
            mButtonG!!.text = "g"
            keyValues.put(R.id.capKeyboard_button_H, "h")
            mButtonH!!.text = "h"
            keyValues.put(R.id.capKeyboard_button_J, "j")
            mButtonJ!!.text = "j"
            keyValues.put(R.id.capKeyboard_button_K, "k")
            mButtonK!!.text = "k"
            keyValues.put(R.id.capKeyboard_button_L, "l")
            mButtonL!!.text = "l"

            keyValues.put(R.id.capKeyboard_button_Z, "z")
            mButtonZ!!.text = "z"
            keyValues.put(R.id.capKeyboard_button_X, "x")
            mButtonX!!.text = "x"
            keyValues.put(R.id.capKeyboard_button_C, "c")
            mButtonC!!.text = "c"
            keyValues.put(R.id.capKeyboard_button_V, "v")
            mButtonV!!.text = "v"
            keyValues.put(R.id.capKeyboard_button_B, "b")
            mButtonB!!.text = "b"
            keyValues.put(R.id.capKeyboard_button_N, "n")
            mButtonN!!.text = "n"
            keyValues.put(R.id.capKeyboard_button_M, "m")
            mButtonM!!.text = "m"

            mButtonSpecialChar!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_hash))

            mButtonCapChange!!.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_upload_button))

        } else if(
            ViewHelper.isSpecialCharOn && ViewHelper.isCapsLockOn ||
            ViewHelper.isSpecialCharOn && !ViewHelper.isCapsLockOn
        ){
            keyValues.put(R.id.capKeyboard_button_Q, "@")
            mButtonQ!!.text = "@"
            keyValues.put(R.id.capKeyboard_button_W, "#")
            mButtonW!!.text = "#"
            keyValues.put(R.id.capKeyboard_button_E, "$")
            mButtonE!!.text = "$"
            keyValues.put(R.id.capKeyboard_button_R, "_")
            mButtonR!!.text = "_"
            keyValues.put(R.id.capKeyboard_button_T, "&")
            mButtonT!!.text = "&"
            keyValues.put(R.id.capKeyboard_button_Y, "-")
            mButtonY!!.text = "-"
            keyValues.put(R.id.capKeyboard_button_U, "+")
            mButtonU!!.text = "+"
            keyValues.put(R.id.capKeyboard_button_I, "(")
            mButtonI!!.text = "("
            keyValues.put(R.id.capKeyboard_button_O, ")")
            mButtonO!!.text = ")"
            keyValues.put(R.id.capKeyboard_button_P, "/")
            mButtonP!!.text = "/"

            keyValues.put(R.id.capKeyboard_button_A, "%")
            mButtonA!!.text = "%"
            keyValues.put(R.id.capKeyboard_button_S, "*")
            mButtonS!!.text = "*"
            keyValues.put(R.id.capKeyboard_button_D, "'")
            mButtonD!!.text = "'"
            keyValues.put(R.id.capKeyboard_button_F, ":")
            mButtonF!!.text = ":"
            keyValues.put(R.id.capKeyboard_button_G, ";")
            mButtonG!!.text = ";"
            keyValues.put(R.id.capKeyboard_button_H, "!")
            mButtonH!!.text = "!"
            keyValues.put(R.id.capKeyboard_button_J, "?")
            mButtonJ!!.text = "?"
            keyValues.put(R.id.capKeyboard_button_K, ",")
            mButtonK!!.text = ","
            keyValues.put(R.id.capKeyboard_button_L, ".")
            mButtonL!!.text = "."

            keyValues.put(R.id.capKeyboard_button_Z, ".com")
            mButtonZ!!.text = ".com"
            keyValues.put(R.id.capKeyboard_button_X, "")
            mButtonX!!.text = ""
            keyValues.put(R.id.capKeyboard_button_C, "")
            mButtonC!!.text = ""
            keyValues.put(R.id.capKeyboard_button_V, "")
            mButtonV!!.text = ""
            keyValues.put(R.id.capKeyboard_button_B, "")
            mButtonB!!.text = ""
            keyValues.put(R.id.capKeyboard_button_N, "")
            mButtonN!!.text = ""
            keyValues.put(R.id.capKeyboard_button_M, "")
            mButtonM!!.text = ""

            mButtonSpecialChar!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_hash_white))
        }

        keyValues.put(R.id.capKeyboard_button_pointer, ".")
        keyValues.put(R.id.capKeyboard_button_emailSign, "@")
        keyValues.put(R.id.capKeyboard_button_changeCap, "")
        keyValues.put(R.id.capKeyboard_button_specialChar, "")
        keyValues.put(R.id.capKeyboard_button_enter, "")
    }

    override fun onClick(v: View) {

        // do nothing if the InputConnection has not been set yet
        if (inputConnection == null) return


        if(v.id == R.id.capKeyboard_button_changeCap){
            ViewHelper.isCapsLockOn = !ViewHelper.isCapsLockOn
            Log.i("Keyboard", "Cap Change button hit")
            init(context)
        }

        if(v.id == R.id.capKeyboard_button_specialChar){
            ViewHelper.isSpecialCharOn = !ViewHelper.isSpecialCharOn
            Log.i("Keyboard", "special Char button hit")
            init(context)
        }

        if (v.id == R.id.capKeyboard_button_enter) {
            KeyboardWatcher.enterButtonPressed("qwerty")
        }

        // Delete text or input key value
        // All communication goes through the InputConnection
        if (v.id == R.id.capKeyboard_button_delete) {
            val characterBefore = inputConnection?.getTextBeforeCursor(1,0)
            val selectedText = inputConnection?.getSelectedText(0)
            if (TextUtils.isEmpty(selectedText) && characterBefore != "") {
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
    fun setInputConnection(ic1: InputConnection) {
        this.inputConnection = ic1
    }
}