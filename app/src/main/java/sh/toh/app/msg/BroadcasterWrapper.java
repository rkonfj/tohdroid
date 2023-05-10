package sh.toh.app.msg;

import android.content.Intent;
import android.os.Build;

import sh.toh.app.log.Getter;

public abstract class BroadcasterWrapper {
    private Broadcaster broadcaster;
    public static final String packageName = "sh.toh.app";

    public BroadcasterWrapper(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void pub(String action, String data) {
        Intent i = new Intent(packageName + ".msgBus");
        i.putExtra("action", action);
        i.putExtra("data", data);
        broadcaster.sendBroadcast(i);
    }
}
