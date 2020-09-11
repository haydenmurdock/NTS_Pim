package com.example.nts_pim.utilities.dialog_composer

import android.app.Activity
import android.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import kotlin.system.exitProcess


object PIMDialogComposer: ScopedFragment() {
    private const val exitCode = 0

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

    internal fun androidVersionNotSupported(activity: FragmentActivity){
        val androidVersionNotSupported = AlertDialog.Builder(activity)
            androidVersionNotSupported.setTitle("Not compatible with android 10")
            androidVersionNotSupported.setMessage("The current OS version is not supported by this application. Please contact dev team")
                .setPositiveButton("okay", null)
                .show()

    }


}