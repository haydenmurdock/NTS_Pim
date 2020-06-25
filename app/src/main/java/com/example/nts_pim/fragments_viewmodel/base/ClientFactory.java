package com.example.nts_pim.fragments_viewmodel.base;

import android.content.Context;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;

// We use this anytime we need to make a client factory for App Sync
public class ClientFactory {
    private static volatile AWSAppSyncClient client;

    public synchronized static AWSAppSyncClient getInstance(Context context) {
        if (client == null) {
            AWSConfiguration awsConfig = new AWSConfiguration(context);

            client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfig)
                    .useClientDatabasePrefix(true)
                    .build();
        }
        return client;
    }
}
