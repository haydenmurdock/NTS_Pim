package com.example.nts_pim.fragments_viewmodel.recent_trip_aws

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetTripQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import kotlinx.android.synthetic.main.recent_trip_aws_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentTripAWSFragment: ScopedFragment() {

    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    var tripId = ""
    var vehicleId = ""
    val emptyStringPlaceHolder = "Empty"
    private val currentFragmentId = R.id.recentTripAWSFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.recent_trip_aws_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProviders.of(this, factory)
            .get(CallBackViewModel::class.java)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        tripId = ModelPreferences(requireContext())
            .getObject(
                SharedPrefEnum.CURRENT_TRIP.key,
                CurrentTrip::class.java)?.tripID ?: ""
        recentTrip_back_btn.isEnabled = false
        if(tripId_textView != null){
            tripId_textView.text = "Trip Id: $tripId"
        }
        getRecentTripDetails(tripId)
        recentTrip_back_btn.setOnClickListener {
            backToVehicleDetail()
        }
        more_btn.setOnClickListener {
            toTripDetails()
        }
    }
    private fun getRecentTripDetails(tripId: String){
            if (mAWSAppSyncClient == null) {
                mAWSAppSyncClient = ClientFactory.getInstance(context)
            }
            mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
                ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                ?.enqueue(getRecentTripQueryCallBack)
        }

        private var getRecentTripQueryCallBack = object : GraphQLCall.Callback<GetTripQuery.Data>() {
            override fun onResponse(response: Response<GetTripQuery.Data>) {
                if (response.data() != null && !this@RecentTripAWSFragment.isRemoving && this@RecentTripAWSFragment.isVisible) {

                    val airPortFee = response.data()?.trip?.airportFee()
                    val appliedFareMin = response.data()?.trip?.appliedFareMin()
                    val appliedFareRate = response.data()?.trip?.appliedFareRate()
                    val cardInfo = response.data()?.trip?.cardInfo() ?: emptyStringPlaceHolder
                    val custEmail = response.data()?.trip?.custEmail() ?: emptyStringPlaceHolder
                    val custPhoneNumber = response.data()?.trip?.custPhoneNbr() ?: emptyStringPlaceHolder
                    val discountAmt = response.data()?.trip?.discountAmt()
                    val discountPercent = response.data()?.trip?.discountPercent()
                    val meterDistance = response.data()?.trip?.meterDistance()
                    val meterDistFare = response.data()?.trip?.meterDistFare()
                    val meterError = response.data()?.trip?.meterError() ?: "No Error"
                    val meterFare = response.data()?.trip?.meterFare()
                    val meterRate = response.data()?.trip?.meterRate()
                    val meterState = response.data()?.trip?.meterState() ?: emptyStringPlaceHolder
                    val meterTimeFare = response.data()?.trip?.meterTimeFare()
                    val meterWaitTime = response.data()?.trip?.meterWaitTime()
                    val owedPrice = response.data()?.trip?.owedPrice()
                    val owedPriceSource = response.data()?.trip?.owedPriceSource() ?: emptyStringPlaceHolder
                    val paidAmt = response.data()?.trip?.paidAmt()
                    val paymentType = response.data()?.trip?.paymentType() ?: emptyStringPlaceHolder
                    val pimPaidAmount = response.data()?.trip?.pimPaidAmt()
                    val pimTransDate = response.data()?.trip?.pimTransDate() ?: emptyStringPlaceHolder
                    val pimTransId = response.data()?.trip?.pimTransId() ?: emptyStringPlaceHolder
                    val tipAmt = response.data()?.trip?.tipAmt()
                    val tipPercent = response.data()?.trip?.tipPercent()
                    val toll = response.data()?.trip?.toll()
                    val tripEndTime = response.data()?.trip?.tripEndTime() ?: emptyStringPlaceHolder
                    val tripNumber = response.data()?.trip?.tripNbr()
                    val tripStartTime = response.data()?.trip?.tripStartTime() ?: emptyStringPlaceHolder
                    val tripStatus = response.data()?.trip?.tripStatus() ?: emptyStringPlaceHolder
                    val tripTime =  response.data()?.trip?.tripTime()
                    val voucherAccountNumber =response.data()?.trip?.voucherAcctNbr() ?: emptyStringPlaceHolder
                    val voucherEnteredAmt = response.data()?.trip?.voucherEnteredAmt()
                    val voucherNumber = response.data()?.trip?.voucherNbr() ?: emptyStringPlaceHolder

                    launch(Dispatchers.Main.immediate) {
                        if(awsProgressBar != null &&
                            awsProgressBar.isAnimating){
                            awsProgressBar.visibility = View.INVISIBLE
                        }
                        if (airportFee_textView != null){
                            airportFee_textView.text = "AirPortFee: $airPortFee"
                        }
                        if(appliedFareMin != null){
                            appliedFairMin_textView.text = "AppliedFairFee: $appliedFareMin"
                        }
                        if(appliedFairRate_textView != null){
                            appliedFairRate_textView.text = "AppliedFairRate: $appliedFareRate"
                        }
                        if(cardInfo_textView != null){
                            cardInfo_textView.text = "CardInfo: $cardInfo"
                        }
                        if(custEmail_textView != null){
                            custEmail_textView.text = "CustEmail: $custEmail"
                        }
                        if(custPhoneNumber_textView != null){
                            custPhoneNumber_textView.text = "CustPhoneNumber: $custPhoneNumber"
                        }
                        if(discountAmt_textView != null){
                            discountAmt_textView.text = "DiscountAmt: $discountAmt"
                        }
                        if(discountPercent_textView != null){
                            discountPercent_textView.text = "DiscountPercentage: $discountPercent"
                        }
                        if(meterDistance_textView != null){
                            meterDistance_textView.text = "MeterDistance: $meterDistance"
                        }
                        if(meterDistFare_textView != null){
                            meterDistFare_textView.text = "MeterDistanceFare: $meterDistFare"
                        }
                        if(meterError_textView != null){
                            meterError_textView.text = "MeterError: $meterError"
                        }
                        if (meterFare_textView != null){
                            meterFare_textView.text = "MeterFare: $meterFare"
                        }
                       if(meterRate_textView != null){
                           meterRate_textView.text = "MeterRate $meterRate"
                       }
                        if(meterState_textView != null){
                            meterState_textView.text = "MeterState: $meterState"
                        }
                        if(meterTimeFare_textView != null){
                            meterTimeFare_textView.text = "MeterTimeFare $meterTimeFare"
                        }
                        if(meterWaitTime_textView != null){
                            meterWaitTime_textView.text = "MeterWaitTime $meterWaitTime"
                        }
                        if(owedPrice_textView != null){
                            owedPrice_textView.text = "OwedPrice: $owedPrice"
                        }
                        if(owedPriceSource_textView != null){
                            owedPriceSource_textView.text = "OwedPriceSource: $owedPriceSource"
                        }
                        if(pimPaidAmount_textView != null){
                            pimPaidAmount_textView.text = "PimPaidAmount: $paidAmt"
                        }
                        if(paymentType_textView != null){
                            paymentType_textView.text = "PimPaymentType: $paymentType"
                        }
                        if(pimPaidAmount_textView != null){
                            pimPaidAmount_textView.text = "PimPaidAmount $pimPaidAmount"
                        }
                        if(pimTransDate_textView != null){
                            pimTransDate_textView.text = "PimTransDate: $pimTransDate"
                        }
                        if(pimTransId_textView != null){
                            pimTransId_textView.text = "PimTransId: $pimTransId"
                        }
                        if(tipAmount_textView != null){
                            tipAmount_textView.text = "Tip Amount: $tipAmt"
                        }
                        if(tipPercent_textView != null){
                            tipPercent_textView.text = "Tip Percent: $tipPercent"
                        }
                        if(toll_textView != null){
                            toll_textView.text = "Toll: $toll"
                        }
                        if(tripEndTime_textView != null){
                            tripEndTime_textView.text = "TripEndTime: $tripEndTime"
                        }
                        if(tripNumber_textview != null){
                            tripNumber_textview.text = "TripNumber: $tripNumber"
                        }
                        if(tripStartTime_textView != null){
                            tripStartTime_textView.text = "TripStartTime: $tripStartTime"
                        }
                        if(tripStatus_textView != null){
                            tripStatus_textView.text = "TripStatus: $tripStatus"
                        }
                        if(tripTime_textView != null){
                            tripTime_textView.text = "TripTime: $tripTime"
                        }
                        if(voucherAcctNbr_textView != null){
                            voucherAcctNbr_textView.text = "VoucherAccountNumber: $voucherAccountNumber"
                        }
                        if(voucherEnteredAmt_textView != null){
                            voucherEnteredAmt_textView.text= "VoucherEnteredAmount: $voucherEnteredAmt"
                        }
                       if(voucherNbr_textView != null){
                           voucherNbr_textView.text = "VoucherNumber: $voucherNumber"
                       }
                        if(recentTrip_back_btn != null){
                            recentTrip_back_btn.isEnabled = true
                        }
                    }
                }
            }
            override fun onFailure(e: ApolloException) {
                println("Failure")
            }
        }

    private fun toTripDetails(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_recentTripAWSFragment_to_tripReviewFragment)
        }
    }
    private fun backToVehicleDetail(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_recentTripAWSFragment_to_vehicle_settings_detail_fragment)
        }
    }
}