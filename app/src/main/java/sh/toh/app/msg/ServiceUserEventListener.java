package sh.toh.app.msg;

import android.content.Context;
import android.content.Intent;

public abstract class ServiceUserEventListener extends EventListener {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        switch (action) {
            case "stopVpn":
                onStopEvent();
                break;
            default:
        }
    }

    public abstract void onStopEvent();
}
