package sh.toh.app.msg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class UIUpdateEventListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        switch (action) {
            case "log":
                onLog(intent.getStringExtra("data") + "\n");
                break;
            case "tohStarted":
                onTohSocks5Started();
                break;
            case "tohStopped":
                onTohSocks5Stopped();
                break;
        }
    }

    public void register(ContextWrapper contextWrapper) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcasterWrapper.packageName + ".msgBus");
        contextWrapper.registerReceiver(this,intentFilter);
    }

    public void unregister(ContextWrapper contextWrapper) {
        contextWrapper.unregisterReceiver(this);
    }

    public abstract void onLog(String log);
    public abstract void onTohSocks5Started();
    public abstract void onTohSocks5Stopped();

}

