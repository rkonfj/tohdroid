package sh.toh.app.msg;

import android.content.Intent;

public class MessageBus {
    private final Broadcaster broadcaster;
    public static final String PACKAGE_NAME = "sh.toh.app";

    public MessageBus(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void pub(int action, String data) {
        Intent i = new Intent(PACKAGE_NAME + ".msgBus");
        i.putExtra("action", action);
        i.putExtra("data", data);
        broadcaster.sendBroadcast(i);
    }

    public void pub(int action) {
        pub(action, null);
    }
}
