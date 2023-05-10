package sh.toh.app.srv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.system.Os;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sh.toh.app.log.Logger;
import sh.toh.app.msg.BroadcasterWrapper;

public class TohService extends Service {

    private final Logger log = new Logger(this::sendBroadcast);
    private Process tohSocks5;

    private final OkHttpClient httpClient = new OkHttpClient();

    private final ExecutorService executors = Executors.newFixedThreadPool(1);

    private final BroadcasterWrapper msgBus = new BroadcasterWrapper(this::sendBroadcast) {
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Os.setenv("HOME", getFilesDir().getPath(), false);
            tohSocks5 = Runtime.getRuntime().exec(getApplicationInfo().nativeLibraryDir
                    + "/libtoh.so s5 --dns 8.8.8.8 --dns-fake 10.88.77.2");
            log.show("toh", tohSocks5,true);
            executors.submit(() -> {
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        log.show("toh", e);
                    }
                    try (Response ignored = httpClient.newCall(new Request.Builder().url("http://127.0.0.1:2080").get().build()).execute()) {
                        msgBus.pub("tohStarted", null);
                        break;
                    } catch (IOException ignored) {
                    }
                }
            });
        } catch (Exception e) {
            log.show("toh", e);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tohSocks5.destroy();
        executors.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
