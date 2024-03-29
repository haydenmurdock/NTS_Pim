package com.example.nts_pim.fragments_viewmodel.advertisement

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.AdInfoHolder
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.MeterEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.fragment_advertisement.*
import kotlinx.serialization.StructureKind


class AdvertisementFragment : ScopedFragment(){
    var advertisementTimer: CountDownTimer? = null
    private lateinit var callBackViewModel: CallBackViewModel
    val currentFragmentId = R.id.advertisementFragment
    private val mediaTypeVideo = "video/mp4"
    private val mediaTypeImage = "image/jpeg"
    private val logTag = "Advertisement_Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_advertisement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        val contentType = AdInfoHolder.getAdInfo().second
        if(contentType == mediaTypeVideo){
            advertisementTimer?.cancel()
            ad_imageView.visibility = View.INVISIBLE
            ad_videoView.visibility = View.VISIBLE
            val path = "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Video"
            ad_videoView.setOnCompletionListener {
                toMeterScreen()
            }
            ad_videoView.setVideoPath(path)
            LoggerHelper.writeToLog("${logTag}, Starting video for ad", LogEnums.AD_INFO.tag)
            ad_videoView.start()
        }
        if(contentType == mediaTypeImage){
            ad_imageView.visibility = View.VISIBLE
            ad_videoView.visibility = View.INVISIBLE
            val path = "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Image"
            val bmImg = BitmapFactory.decodeFile(path)
            LoggerHelper.writeToLog("${logTag}, Setting ad picture", LogEnums.AD_INFO.tag)
            ad_imageView.setImageBitmap(bmImg)
        }
        startAdTimer()

        callBackViewModel.getMeterState().observe(this.viewLifecycleOwner, Observer {meterState ->
            if(meterState == MeterEnum.METER_TIME_OFF.state) {
                advertisementTimer?.cancel()
                ad_videoView.stopPlayback()
                toMeterScreen()
            }
        })
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
