package com.example.nts_pim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.nts_pim.activity.MainActivity;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent i;
        try {
            i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
        catch (Exception e) {
            Log.i("ERROR", "BootReceiver.onReceive");
        }
    }
}

