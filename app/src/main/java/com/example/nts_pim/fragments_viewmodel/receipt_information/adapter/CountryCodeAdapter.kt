package com.example.nts_pim.fragments_viewmodel.receipt_information.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.Country

class CountryCodeAdapter(context: Context,
                         arrayNames: MutableList<Country>)
 : BaseAdapter() {
    private val mContext: Context
    private val mCountryArray = arrayNames
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init{
        this.mContext = context
    }

    override fun getItem(arg0: Int): Any? {
        return mCountryArray[arg0]
    }

    override fun getCount(): Int {
        return mCountryArray.count()
    }

    override fun getItemId(arg0: Int): Long {
        return arg0.toLong()
    }
    // responsible for rendering out view
    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.country_list_cell, viewGroup, false)
        if (mCountryArray.size > 0) {
            val countryNameTextView = rowView.findViewById(R.id.countryNameTextView) as TextView
            val countryCodeTextView =
                rowView.findViewById(R.id.countryListCodeTextView) as TextView
            val country = mCountryArray[position]
            countryNameTextView.text = country.name
            if (!country.callingCodes.isNullOrEmpty()) {
                countryCodeTextView.text = country.callingCodes.first()
            }
        }
        return rowView
    }
}