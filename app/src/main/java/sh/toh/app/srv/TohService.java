package sh.toh.app.srv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.system.Os;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sh.toh.app.R;
import sh.toh.app.log.Logger;
import sh.toh.app.msg.MessageBus;

public class TohService extends Service {
    private final Logger log = new Logger(this::sendBroadcast);

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .followRedirects(true).build();

    private final ExecutorService executors = Executors.newFixedThreadPool(2);

    private final MessageBus msgBus = new MessageBus(this::sendBroadcast);

    private Process tohSocks5;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Os.setenv("HOME", getFilesDir().getPath(), false);
            tohSocks5 = Runtime.getRuntime().exec(getApplicationInfo().nativeLibraryDir
                    + "/libtoh.so s5 --dns 8.8.8.8 --dns-fake 10.88.77.3");
            log.show("toh", tohSocks5, true);
            executors.submit(() -> {
                while (true) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        log.show("toh", e);
                        Thread.currentThread().interrupt();
                    }
                    try (Response ignored = httpClient.newCall(new Request.Builder()
                            .url("http://127.0.0.1:2080")
                            .get()
                            .build()).execute()) {
                        msgBus.pub(R.string.tohStarted);
                        break;
                    } catch (IOException ignored) {
                    }
                }
            });

            executors.submit(() -> {
                while (true) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        log.show("toh", e);
                        Thread.currentThread().interrupt();
                    }
                    if (!tohSocks5.isAlive()) {
                        msgBus.pub(R.string.tohStopped);
                        break;
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
