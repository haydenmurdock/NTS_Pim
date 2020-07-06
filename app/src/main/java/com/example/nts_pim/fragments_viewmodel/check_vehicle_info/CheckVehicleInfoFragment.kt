package com.example.nts_pim.fragments_viewmodel.check_vehicle_info

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.amazonaws.amplify.generated.graphql.GetCompanyNameQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.PimApplication
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.VehicleSettings
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.github.ybq.android.spinkit.style.ThreeBounce
import kotlinx.android.synthetic.main.check_vehicle_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance


class CheckVehicleInfoFragment: ScopedFragment(), KodeinAware {

    override val kodein by closestKodein()
    private val viewModelFactory: CheckVehicleInfoModelFactory by instance()
    private lateinit var viewModel: CheckVehicleInfoViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var vehicleId = ""
    private var cabNumber = ""
    private var companyName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.check_vehicle_info, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(CheckVehicleInfoViewModel::class.java)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
       com.example.nts_pim.utilities.view_helper.ViewHelper.hideSystemUI(activity!!)
        progressBar.animate()
        Thread {
            getVehicleInfo()
        }.start()
        val threeBounce = ThreeBounce()
        progressBar.setIndeterminateDrawable(threeBounce)
        callBackViewModel.pimPairingValueChangedViaFMP(true)
        viewModel.doesCompanyNameExist().observe(this.viewLifecycleOwner, Observer {
            if(it){
                saveVehicleSettings()
                doRestart(PimApplication.pimContext)
            }

        })
    }
    private fun doRestart(c: Context?) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                val pm: PackageManager = c.packageManager
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    val mStartActivity = pm.getLaunchIntentForPackage(
                        c.packageName
                    )
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        val mPendingIntentId = 223344
                        val mPendingIntent = PendingIntent
                            .getActivity(
                                c, mPendingIntentId, mStartActivity,
                                PendingIntent.FLAG_CANCEL_CURRENT
                            )
                        val mgr =
                            c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = mPendingIntent
                        //kill the application
                        System.exit(0)
                    } else {
                        Log.e(
                            "Error",
                            "Was not able to restart application, mStartActivity null"
                        )
                    }
                } else {
                    Log.e("Error", "Was not able to restart application, PM null")
                }
            } else {
                Log.e("Error","Was not able to restart application, Context null")
            }
        } catch (ex: Exception) {
            Log.e("Error", "Was not able to restart application")
        }
    }
    private fun getVehicleInfo() = launch{
        vehicleId = viewModel.getVehicleID()
        if(vehicleId != ""){
            queryCompanyName(vehicleId)
        }
        cabNumber = getCabNumber(vehicleId)
    }
    private fun queryCompanyName(vehicleID: String)= launch {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context)
        }
        mAWSAppSyncClient?.query(GetCompanyNameQuery.builder().vehicleId(vehicleID).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(queryCompanyNameCallBack)
    }


    private var queryCompanyNameCallBack = object: GraphQLCall.Callback<GetCompanyNameQuery.Data>(){
        override fun onResponse(response: Response<GetCompanyNameQuery.Data>) {
            Log.i("Results", response.data()?.companyName?.name().toString())
            Log.i("Results", response.data().toString())
            if (response.data()!!.companyName.name() != null) {
                companyName = response.data()!!.companyName.name()!!
                launch(Dispatchers.Main.immediate){
                    viewModel.companyNameExists()
                }.start()
            }
        }
        override fun onFailure(e: ApolloException) {
            Log.e("ERROR", e.toString())
        }
    }
    private fun getCabNumber(vehicleID: String): String{
        var cabNumber = ""
        val numberArray = "1234567890".toCharArray()

        for (i in vehicleID) {
            if (numberArray.contains(i)) {
                cabNumber += i
            }
        }
        return cabNumber
    }

    private fun saveVehicleSettings(){
        val vehicleSettings = VehicleSettings(
            cabNumber,
            companyName
        )
        ModelPreferences(context!!).putObject(SharedPrefEnum.VEHICLE_SETTINGS.key, vehicleSettings)
        Log.i("Check Vehicle Info", "Vehicle Settings Saved: cab Number: $cabNumber company Name: $companyName")
    }

    override fun onDestroy() {
        super.onDestroy()
        if(this::viewModel.isInitialized){
            viewModel.doesCompanyNameExist().removeObservers(this)
        }
        if(progressBar != null){
            progressBar.clearAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        progressBar.clearAnimation()
    }
}