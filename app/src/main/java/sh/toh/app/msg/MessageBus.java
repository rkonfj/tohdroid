package sh.toh.app.msg;

import android.content.Intent;

public abstract class MessageBus {
    private Broadcaster broadcaster;
    public static final String packageName = "sh.toh.app";

    public MessageBus(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void pub(String action, String data) {
        Intent i = new Intent(packageName + ".msgBus");
        i.putExtra("action", action);
        i.putExtra("data", data);
        broadcaster.sendBroadcast(i);
    }

    public void pub(String action) {
        pub(action, null);
    }
}
