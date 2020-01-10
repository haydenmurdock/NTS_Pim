package com.example.nts_pim.fragments_viewmodel.startup

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.power_cycle.PowerAccessibilityService
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class StartupFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance()
    private lateinit var viewModel: VehicleSetupViewModel

    private val fullBrightness = 255
    private var permissionDraw = false
    private var permissionWrite = false
    private var permissionAccessibility = false
    private var setupStatus = false
    private var navController: NavController? = null
    private val currentFragmentId = R.id.startupFragment


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.startup, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)

        navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
    }
    private fun checkAccessibilityPermission():Boolean {
        val retVal = PowerAccessibilityService().isAccessibilitySettingsOn(context!!)
        if(!retVal){
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return retVal
    }
    private fun checkDrawPermission(): Boolean {
        val retValue = Settings.canDrawOverlays(context)
        if(!retValue){
            openDrawPermissionsMenu()
        }
        return retValue
    }

    private fun openDrawPermissionsMenu(){
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context!!.getPackageName()))
        startActivity(intent)
    }
    private fun checkSystemWritePermission(): Boolean {
        var retVal = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(context)
            if(retVal){
                // when we have permission we manually set to brightness 255
                changeScreenBrightness(fullBrightness)

            }else{
                //We don't have permissions so we push to grant permissions
                openAndroidPermissionsMenu()
            }
        }
        return retVal
    }

    private fun openAndroidPermissionsMenu() {
        val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        i.setData(Uri.parse("package:" + context!!.getPackageName()))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun changeScreenBrightness(screenBrightness: Int) {
        Settings.System.putInt(
            context!!.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )  //this will set the manual mode (set the automatic mode off)
        Settings.System.putInt(
            context!!.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            screenBrightness
        )  //this will set the brightness to maximum (255)

        //refreshes the screen
        val br =
            Settings.System.getInt(context!!.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        val lp = activity!!.window.attributes
        lp.screenBrightness = br.toFloat() / 255
        activity!!.window.attributes = lp
    }
    private fun checkPermissions() {
        permissionDraw = checkDrawPermission()
        permissionWrite = checkSystemWritePermission()
        permissionAccessibility = checkAccessibilityPermission()
        setupStatus = viewModel.isSetUpComplete()
    }

    private fun checkDestinations(navController: NavController){
        if (setupStatus && permissionWrite && permissionDraw && permissionAccessibility){
            viewModel.vehicleIDExists()
            if (navController.currentDestination?.id == currentFragmentId) {
                navController.navigate(R.id.toWelcomeScreenFromStartUP)
            }
        } else if(permissionDraw && permissionWrite && permissionAccessibility) {
            if (navController.currentDestination?.id == currentFragmentId) {
                navController.navigate(R.id.toVehicleSetupFragment)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!permissionDraw || !permissionWrite || !permissionAccessibility) {
            checkPermissions()
        } else if (permissionWrite && permissionDraw && permissionAccessibility){
            checkDestinations(navController!!)
        }
        checkDestinations(navController!!)
    }
}