package com.example.nts_pim.utilities.dialog_composer

import android.app.Activity
import android.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.PIN
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
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

    internal fun wrongPhoneNumberForPIM(activity: Activity, message: String, vehicleSetupViewModel: VehicleSetupViewModel, keyboardViewModel: SettingsKeyboardViewModel?, vehicleID: String, appSyncClient: AWSAppSyncClient){
        val wrongPhoneNumber = AlertDialog.Builder(activity)
        wrongPhoneNumber.setTitle("Phone number used by other PIM")
        wrongPhoneNumber.setCancelable(false)
        wrongPhoneNumber.setMessage("$message: Press OKAY to restart application")
            .setPositiveButton("OKAY"){ _, _->
                LoggerHelper.writeToLog("Showing incorrect phone number error. Deleting Model_Preferences.", null)
                PIMMutationHelper.unpairPim(vehicleID, appSyncClient)
                VehicleTripArrayHolder.clearAllNonPersistentData()
                context?.deleteSharedPreferences("MODEL_PREFERENCES")
               vehicleSetupViewModel.vehicleIdDoesNotExist()
               vehicleSetupViewModel.recheckAuth()
               vehicleSetupViewModel.companyNameNoLongerExists()
                vehicleSetupViewModel.squareIsNotAuthorized()
                keyboardViewModel?.qwertyKeyboardIsUp()
                var pin = ModelPreferences(activity.applicationContext)
                    .getObject(SharedPrefEnum.PIN_PASSWORD.key, PIN::class.java) as PIN
                if(pin.password != "") {
                    pin.password = ""
                    ModelPreferences(activity.applicationContext).putObject(SharedPrefEnum.PIN_PASSWORD.key, pin)
                }
                activity.finish()
                exitProcess(exitCode)
            }
            .show()
    }
}