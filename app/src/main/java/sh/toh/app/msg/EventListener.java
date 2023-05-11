package sh.toh.app.msg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EventListener extends BroadcastReceiver {
    private final Map<Integer, Subscriber> eventSubscriber = new HashMap<>();

    public void register(ContextWrapper contextWrapper) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MessageBus.PACKAGE_NAME + ".msgBus");
        contextWrapper.registerReceiver(this, intentFilter);
    }

    public void unregister(ContextWrapper contextWrapper) {
        contextWrapper.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int action = intent.getIntExtra("action", Integer.MIN_VALUE);
        if (eventSubscriber.containsKey(action)) {
            Objects.requireNonNull(eventSubscriber.get(action)).consume(intent.getStringExtra("data"));
        }
    }

    public void subscribe(int action, Subscriber subscriber) {
        eventSubscriber.put(action, subscriber);
    }

}
