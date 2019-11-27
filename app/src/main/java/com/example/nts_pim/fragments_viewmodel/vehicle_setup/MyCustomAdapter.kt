package com.example.nts_pim.fragments_viewmodel.vehicle_setup

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox


class MyCustomAdapter(context: Context,
                      arrayNames: Array<String>,
                      arrayBool: Array<Boolean>) : BaseAdapter() {

    private val mContext: Context
    private val mArrayNames = arrayNames
    private val mArrayBool = arrayBool

    init{
        this.mContext = context
    }

    override fun getItem(arg0: Int): Any? {
        return "Test String"
    }

    override fun getCount(): Int {
        return 5
    }

    override fun getItemId(arg0: Int): Long {
        return arg0.toLong()
    }
    // responsible for rendering out view
    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val checkbox = CheckBox(mContext)
        checkbox.text = mArrayNames[position]
        checkbox.isChecked = mArrayBool[position]
        checkbox.setTextColor(Color.WHITE)
        checkbox.textSize = 22.toFloat()
        checkbox.isClickable = false
        checkbox.isFocusable = false
        return checkbox
    }
}
