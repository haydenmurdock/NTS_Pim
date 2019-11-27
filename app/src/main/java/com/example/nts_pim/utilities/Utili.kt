package com.example.nts_pim.utilities

import android.app.Activity
import android.content.Context
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

object Utili {

     fun showKeyboard(activity: Activity?) {
        if (activity == null) return
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    /**
     * Hide the soft keyboard.
     * @param activity the current activity
     */
     fun hideKeyboard(activity: Activity?) {
        if (activity == null) return
        val view = activity.currentFocus
        if (view != null) {
            hideKeyboard(activity, view.windowToken)
        }
    }

    /**
     * Focus the given view and show the soft keyboard.
     * @param activity the current activity
     * *
     * @param view the view to focus
     */
    fun focusAndShowKeyboard(activity: Activity?, view: View) {
        if (activity == null) return
        if (view.isFocusable) {
            view.requestFocus()
        }
        if (view is EditText) {
            showKeyboard(activity)
        }
    }

    /**
     * Clear focus from the current view and hide the soft keyboard.
     * @param activity the current activity
     */
     fun defocusAndHideKeyboard(activity: Activity?) {
        if (activity == null) return
        val view = activity.currentFocus
        if (view != null) {
            view.clearFocus()
            hideKeyboard(activity)
        }
    }

    private fun hideKeyboard(activity: Activity?, windowToken: IBinder?) {
        if (activity == null) return
        val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}