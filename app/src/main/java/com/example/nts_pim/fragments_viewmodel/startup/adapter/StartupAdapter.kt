package com.example.nts_pim.fragments_viewmodel.startup.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.startup.StartupRequirement

class StartupAdapter(context: Context,
                     arrayNames: MutableList<StartupRequirement>)
        : BaseAdapter() {
        private val mContext: Context
        private val mStartupItemList = arrayNames
        private val inflater: LayoutInflater
                = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        init{
            this.mContext = context
        }

        override fun getItem(arg0: Int): Any? {
            return mStartupItemList[arg0]
        }

        override fun getCount(): Int {
            return mStartupItemList.count()
        }

        override fun getItemId(arg0: Int): Long {
            return arg0.toLong()
        }
        // responsible for rendering out view
        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val rowView = inflater.inflate(R.layout.startup_cell, viewGroup, false)
            if (mStartupItemList.size > 0) {
                val startupNameTextView = rowView.findViewById(R.id.requirementNameTextView) as TextView
                val isStartupItemComplete =
                    rowView.findViewById(R.id.isStartupItemCompleteImageView) as ImageView
                val startupItem = mStartupItemList[position]
                startupNameTextView.text =  startupItem.name
                if(startupItem.complete){
                    isStartupItemComplete.setImageResource(R.drawable.ic_checkmark_btn_green)
                } else {
                    isStartupItemComplete.setImageResource(R.drawable.ic_close_btn_red)
                }
            }
            return rowView
        }
}