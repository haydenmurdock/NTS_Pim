package com.example.nts_pim.utilities.dialog_composer

import android.app.Activity
import android.app.AlertDialog
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlin.system.exitProcess


object PIMDialogComposer: ScopedFragment() {
    val exitCode = 0

    internal fun showNoVehicleIDError(activity: Activity){
        val noVehicleIDError = AlertDialog.Builder(activity)
        noVehicleIDError.setTitle("No vehicle ID ERROR")
        noVehicleIDError.setMessage("There was an error retrieving the vehicle ID from server, double checking PIN and internet connection")
            .setNegativeButton("Okay", null)
        noVehicleIDError.show()
    }

    internal fun exitApplication(activity: Activity){
        val exitApplicationAlert = AlertDialog.Builder(activity)
        exitApplicationAlert.setTitle("Exit Application")
        exitApplicationAlert.setMessage("Would you like to exit the application?")
            .setPositiveButton("Yes"){ _, _->
                activity.finish()
                exitProcess(exitCode)
            }
            .setNegativeButton("Cancel",null)
            .show()
    }
    internal fun showSquareNotAuthorized(activity: Activity){
        val squareNotAuthorizedAlert = AlertDialog.Builder(activity)
        squareNotAuthorizedAlert.setTitle("Square needs Authorization")
        squareNotAuthorizedAlert.setMessage("Please hit the re-authorize button before calling square checkout flow")
            .setPositiveButton("okay", null)
            .show()
    }



}