package sh.toh.app.srv;

import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import engine.Engine;
import sh.toh.app.R;
import sh.toh.app.log.Logger;
import sh.toh.app.msg.EventListener;

public class Socks5VpnService extends VpnService {
    private final Logger log = new Logger(this::sendBroadcast);
    private final ExecutorService executors = Executors.newFixedThreadPool(1);
    private final EventListener eventListener = new EventListener();
    private ParcelFileDescriptor tun;

    @Override
    public void onCreate() {
        super.onCreate();
        eventListener.register(this);
        eventListener.subscribe(R.string.stopVpn, data -> {
            if (tun != null) {
                try {
                    tun.close();
                } catch (IOException e) {
                    log.show("tun2socks", e);
                }
            }
        });

        try {
            Builder builder = new Builder()
                    .addAddress("10.88.77.2", 24)
                    .addDnsServer("10.88.77.3")
                    .addRoute("0.0.0.0", 0)
                    .addDisallowedApplication(this.getApplication().getPackageName());
            tun = builder.establish();
        } catch (PackageManager.NameNotFoundException e) {
            log.show("tun2socks", e);
        }


        engine.Key key = new engine.Key();
        key.setMark(0);
        key.setMTU(0);
        key.setDevice("fd://" + tun.getFd());
        key.setInterface("");
        key.setLogLevel("debug");
        key.setProxy("socks5://127.0.0.1:2080");
        key.setRestAPI("");
        key.setTCPSendBufferSize("");
        key.setTCPReceiveBufferSize("");
        key.setTCPModerateReceiveBuffer(false);
        engine.Engine.insert(key);

        executors.submit(Engine::start);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executors.shutdown();
        eventListener.unregister(this);
    }
}
