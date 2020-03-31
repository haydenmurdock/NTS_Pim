package com.example.nts_pim.utilities.view_walker

import android.app.Activity
import android.transition.Scene
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class ViewWalker {
    private var level: Int = 0
    private val logTextOnly = false

    fun walk(activity: Activity) {

        level = 0
        try {
            Log.v(TAG, "Activity: " + activity.localClassName)
            val scene = activity.contentScene
            Log.v(TAG, "Scene " + scene.toString());
            //val viewGroup = scene.getSceneRoot();
            val viewGroup = activity.findViewById<View>(android.R.id.content) as ViewGroup
            walkViewGroup(viewGroup)
        } catch (e: Exception) {
        }

    }

    fun walk(viewGroup: ViewGroup) {
        level = 0
        try {
            walkViewGroup(viewGroup)
        } catch (e: Exception) {
        }

    }

    fun walk(view: View) {

        level = 0
        try {
            walkViewGroup(view as ViewGroup)
        } catch (e: Exception) {
        }

    }

    private fun walkViewGroup(root: ViewGroup) {
        var logit = !logTextOnly
        val sb = StringBuilder()
        for (i in 0 until level)
            sb.append(' ')
        val indent = sb.toString()
        level++
        if (logit)
            Log.v("ViewWalker", indent + "ViewGroup " + root.id + " (" + Integer.toHexString(root.id) + ")")
        for (i in 0 until root.childCount) {
            val v = root.getChildAt(i)
            var text = ""
            if (v.javaClass.getName().contains("Text")) {
                try {
                    val tv = v as TextView
                    text = " TEXT: '" + tv.text.toString() + "'"
                    logit = true
                } catch (e: Exception) {
                }

            } else if (v.javaClass.getName().contains("Button")) {
                try {
                    val b = v as Button
                    text = " BUTTON: '" + b.getText().toString() + "'"
                } catch (e: Exception) {
                }

            }
            if (logit) {
                //Log.v(TAG, indent + " View " + v.getId() + " (" + Integer.toHexString(v.getId()) + ") " + v.getClass().toString() + " Vis:" + v.getVisibility() + text);
                val attributues = StringBuilder()
                if (v.isClickable)
                    attributues.append("Clickable ")
                if (v.isContextClickable)
                    attributues.append("Ctx-Clickable ")
                if (v.isEnabled)
                    attributues.append("Enabled ")
                if (v.isFocusable)
                    attributues.append("Focusable ")
                if (attributues.length != 0)
                    attributues.insert(0, " ")
                Log.v(
                    TAG,
                    indent + " View " + v.id + " (" + Integer.toHexString(v.id) + ") " + v.javaClass.toString() + " Vis:" + v.visibility + attributues + text
                )
            }
            try {
                val vg = v as ViewGroup
                walkViewGroup(vg)
            } catch (e: Exception) {
            }

        }
    }

    companion object {

        private val TAG = "PIM-" + ViewWalker::class.java.simpleName
    }
}