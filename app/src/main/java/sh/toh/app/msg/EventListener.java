package sh.toh.app.msg;

import android.content.BroadcastReceiver;
import android.content.ContextWrapper;
import android.content.IntentFilter;

public abstract class EventListener extends BroadcastReceiver {

    public void register(ContextWrapper contextWrapper) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MessageBus.packageName + ".msgBus");
        contextWrapper.registerReceiver(this, intentFilter);
    }

    public void unregister(ContextWrapper contextWrapper) {
        contextWrapper.unregisterReceiver(this);
    }

}
