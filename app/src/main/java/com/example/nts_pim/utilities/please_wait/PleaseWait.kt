package com.example.nts_pim.utilities.please_wait

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.nts_pim.R

class PleaseWait @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    init {
            init(context)
    }

   private fun init(context: Context) {

        // initialize layout
        LayoutInflater.from(context).inflate(R.layout.please_wait, this, true)
    }
}