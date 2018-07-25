package com.shamilov.proxysharedialog;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private static final String EXTRA_ORIGINAL_INTENT = "extra.original.intent";
    private String textToShare = "Random text to share with app: %s";
    private static final String EXTRA_REQUEST_CODE = "extra.request.code";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.share).setOnClickListener(button -> {
            share(textToShare);
        });
    }

    private void share(String text) {
        Intent intent = getInitialIntent(text);
        List<Intent> proxyIntents = getProxyIntents(intent);
        Intent initialIntent = extractInitialIntentFromProxies(proxyIntents);
        Intent chooserIntent = createChooser(initialIntent, proxyIntents);
        OnIntentRedeliveredConsumer onIntentRedeliveredConsumer = this::onIntentRedelivered;
        createReceiverForShadowActivity(this, ShadowActivity.ACTION_REDELIVER_INTENT, onIntentRedeliveredConsumer);
        startActivity(chooserIntent);
    }

    private void onIntentRedelivered(Intent redeliveredIntent) {
        Intent proxyIntent = redeliveredIntent.getParcelableExtra(ShadowActivity.EXTRA_PROXY_REDELIVERED);
        Intent realIntent = proxyIntent.getParcelableExtra(EXTRA_ORIGINAL_INTENT);
        Log.d("Redeliver", "Shared with app: " + realIntent.getComponent().getPackageName());
        startActivity(realIntent);
    }

    private Intent createChooser(Intent initialIntent, List<Intent> proxyIntents) {
        Intent chooserIntent = Intent.createChooser(initialIntent, "Share via");
        Intent[] intentsArray = new Intent[proxyIntents.size()];
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, proxyIntents.toArray(intentsArray));
        return chooserIntent;
    }

    private void createReceiverForShadowActivity(Context context, String action, OnIntentRedeliveredConsumer intentRedeliveredConsumer) {
        IntentFilter intentFilter = new IntentFilter(action);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intentRedeliveredConsumer.onRedeliverIntent(intent);
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
    }

    private Intent extractInitialIntentFromProxies(List<Intent> proxyIntents) {
        //Can be some custom logic
        return proxyIntents.remove(0).getParcelableExtra(EXTRA_ORIGINAL_INTENT);
    }

    private List<Intent> getProxyIntents(Intent initialIntent) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(initialIntent, 0);
        List<Intent> proxyIntents = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            Intent realIntent = buildRealIntent(resolveInfo);
            proxyIntents.add(
                    createProxy(
                            realIntent,
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.loadLabel(packageManager).toString(),
                            resolveInfo.activityInfo.getIconResource(),
                            ShadowActivity.class
                    )
            );
        }
        return proxyIntents;
    }

    private Intent buildRealIntent(ResolveInfo resolveInfo) {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        intent.setComponent(componentName);
        intent.setType("text/plain");
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getTextToShare(resolveInfo.activityInfo.applicationInfo.name));
        return intent;
    }

    private Intent createProxy(Intent original, String packageName, String name, int icon, Class<? extends Activity> shadowActivityClass) {
        Intent proxy = new Intent(this, shadowActivityClass);
        proxy.putExtra(EXTRA_ORIGINAL_INTENT, original);
        return new LabeledIntent(proxy, packageName, name, icon);
    }

    private String getTextToShare(String marker) {
        return String.format(Locale.getDefault(), textToShare, marker);
    }

    private Intent getInitialIntent(String data) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, data);
        return intent;
    }

    static interface OnIntentRedeliveredConsumer {
        void onRedeliverIntent(Intent intent);
    }

}
