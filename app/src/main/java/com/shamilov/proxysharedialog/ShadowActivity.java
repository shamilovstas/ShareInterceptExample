package com.shamilov.proxysharedialog;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

public class ShadowActivity extends AppCompatActivity {
    public static final String ACTION_REDELIVER_INTENT = "action.redeliver.intent";
    public static final String EXTRA_PROXY_REDELIVERED = "extra.proxy.redelivered";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Intent redeliverProxy = new Intent(ACTION_REDELIVER_INTENT);
        redeliverProxy.putExtra(EXTRA_PROXY_REDELIVERED, intent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(redeliverProxy);
        finish();
    }
}
