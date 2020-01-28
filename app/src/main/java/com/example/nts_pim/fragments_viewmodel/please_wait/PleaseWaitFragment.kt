package com.example.nts_pim.fragments_viewmodel.please_wait

import android.os.Bundle
import android.util.Log
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
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import kotlinx.android.synthetic.main.please_wait.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PleaseWaitFragment: ScopedFragment() {
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private val currentFragmentId = R.id.pleaseWaitFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.please_wait, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()

        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        mAWSAppSyncClient = ClientFactory.getInstance(context!!)
        setUpUI()
        val tripId = callBackViewModel.getTripId()
        getMeterOwedQuery(tripId)
    }

    private fun setUpUI(){
        if(progressBar3 != null){
            progressBar3.animate()
        }
    }
    private fun getMeterOwedQuery(tripId: String) = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context)
        }
        mAWSAppSyncClient?.query(GetTripQuery.builder().tripId(tripId).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(getTripQueryCallBack)
    }

    private var getTripQueryCallBack = object: GraphQLCall.Callback<GetTripQuery.Data>() {
        override fun onResponse(response: Response<GetTripQuery.Data>) {
            if (response.data() != null &&
                !response.hasErrors()){
                val meterOwed = response.data()?.trip?.owedPrice()
                val meterValue = response.data()?.trip?.meterState()
                Log.i("PleaseWait", "meterOwed from Meter query = $meterOwed")
                Log.i("PLeaseWait", "meterValue from Meter query = $meterValue")
                if(meterOwed != null) {
                    if(meterOwed > 0){
                        launch(Dispatchers.Main.immediate) {
                             callBackViewModel.addMeterValue(meterOwed)
                        }
                    }

                }
                if(!meterValue.isNullOrBlank()){
                    launch(Dispatchers.Main.immediate) {
                        callBackViewModel.addMeterState(meterValue)
                        toTaxiScreen()
                    }
                }
            }
        }

        override fun onFailure(e: ApolloException) {

        }
    }

    private fun toTaxiScreen()=launch(Dispatchers.Main.immediate) {
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            navController.navigate(R.id.action_global_taxi_number_fragment)
        }
    }

}