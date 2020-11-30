package com.example.nts_pim.fragments_viewmodel.advertisement

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation

import com.example.nts_pim.R
import com.example.nts_pim.data.repository.AdInfoHolder
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlinx.android.synthetic.main.fragment_advertisement.*
import kotlinx.android.synthetic.main.receipt_information_text.*


class AdvertisementFragment : ScopedFragment(){
    var advertisementTimer: CountDownTimer? = null
    val currentFragmentId = R.id.advertisementFragment
    val mediaTypeVideo = "video/mp4"
    val mediaTypeImage = "image/png"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_advertisement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contentType = AdInfoHolder.getAdInfo().second
        if(contentType == mediaTypeVideo){
            val path = "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Video"
            ad_videoView.setVideoPath(path)
            ad_videoView.start()
        }
        if(contentType == mediaTypeImage){

        }
        startAdTimer()
    }


    private fun startAdTimer(){
      var timerLength =  AdInfoHolder.getAdInfo().first?.toLong()
        if(timerLength == null){
            timerLength = 30
        }
        advertisementTimer = object: CountDownTimer(timerLength * 1000, 10000){
            override fun onTick(p0: Long) {

                }

            override fun onFinish() {
                toMeterScreen()
                }
            }.start()
    }


    private fun toMeterScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_advertisementFragment_to_live_meter_fragment)
        }
    }

    override fun onDestroy() {
        advertisementTimer?.cancel()
        super.onDestroy()
    }
}
