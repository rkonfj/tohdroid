package sh.toh.app.srv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import engine.Engine;
import sh.toh.app.MainActivity;
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
            stopSelf();
        });

        try {
            Builder builder = new Builder()
                    .addAddress("10.88.77.2", 24)
                    .addDnsServer("10.88.77.3")
                    .addRoute("0.0.0.0", 0)
                    .addAddress("fd10:8877::2", 64)
                    .addRoute("::",0)
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
        key.setLogLevel("info");
        key.setProxy("socks5://127.0.0.1:2080");
        key.setRestAPI("");
        key.setTCPSendBufferSize("");
        key.setTCPReceiveBufferSize("");
        key.setTCPModerateReceiveBuffer(false);
        engine.Engine.insert(key);

        executors.submit(Engine::start);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel(getPackageName(),
                getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        Objects.requireNonNull(notificationManager).createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(this, getPackageName())
                        .setContentTitle(getText(R.string.app_name))
                        .setContentText(getString(R.string.connected))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.app_name))
                        .build();
        startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executors.shutdown();
        eventListener.unregister(this);
    }
}
