package sh.toh.app.msg;

import android.content.Context;
import android.content.Intent;

public abstract class UIUpdateEventListener extends EventListener {

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
            default:
        }
    }

    public abstract void onLog(String log);

    public abstract void onTohSocks5Started();

    public abstract void onTohSocks5Stopped();

}

