package sh.toh.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Color;
import android.net.UrlQuerySanitizer;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sh.toh.app.msg.EventListener;
import sh.toh.app.msg.MessageBus;
import sh.toh.app.srv.Socks5VpnService;
import sh.toh.app.srv.TohService;

public class MainActivity extends AppCompatActivity {
    private final MessageBus msgBus = new MessageBus(this::sendBroadcast);

    private final EventListener eventListener = new EventListener();

    private final ExecutorService executors = Executors.newFixedThreadPool(2);

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .followRedirects(true).build();

    private TextView logView;

    private Button connectButton;

    private Intent tohService;

    private Intent vpnService;

    private ActivityResultLauncher<Intent> vpnLauncher;
    private ActivityResultLauncher<ScanOptions> scanLauncher;

    @Override
    protected void onResume() {
        super.onResume();
        eventListener.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        eventListener.unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        logView = findViewById(R.id.logView);
        connectButton = findViewById(R.id.button);

        logView.setOnLongClickListener(this::showClearLogButton);
        connectButton.setOnClickListener(this::connect);
        connectButton.setOnLongClickListener(this::showScanQRButton);

        findViewById(R.id.clearLog).setOnClickListener(this::clearLog);
        findViewById(R.id.clearLog).setOnLongClickListener(this::showClearLogButton);
        findViewById(R.id.overlayView).setOnClickListener(this::showLogComponent);

        tohService = new Intent(this, TohService.class);
        vpnService = new Intent(this, Socks5VpnService.class);

        findViewById(R.id.scanQR).setOnLongClickListener(this::hideScanQRButton);
        findViewById(R.id.scanQR).setOnClickListener(this::scanQR);

        preparePermission();
        registerEventListener();
    }


    private void preparePermission() {
        vpnLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK) {
                Snackbar.make(findViewById(R.id.logContainer), "permission not granted", BaseTransientBottomBar.LENGTH_LONG).show();
                return;
            }
            startVpnService();
        });

        scanLauncher = registerForActivityResult(new ScanContract(), result -> {
            hideScanQRButton(findViewById(R.id.scanQR));
            if (result.getContents() != null) {
                downloadConfig(result.getContents());
            }
        });

    }

    private void downloadConfig(String qrCode) {
        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(qrCode);
        if (!sanitizer.hasParameter("key")) {
            Snackbar.make(findViewById(R.id.buttonLayout), "invalid format", BaseTransientBottomBar.LENGTH_LONG).show();
            return;
        }
        String key = sanitizer.getValue("key");
        String host = "toh.sh";
        if (sanitizer.hasParameter("host")) {
            host = sanitizer.getValue("host");
        }
        String url = String.format("https://%s/s5/%s", host, key);
        Snackbar.make(findViewById(R.id.buttonLayout), "downloading toh-s5 config", BaseTransientBottomBar.LENGTH_LONG).show();
        connectButton.setEnabled(false);
        executors.submit(() -> {
            try (Response resp = httpClient.newCall(new Request.Builder().url(url).get().build()).execute()) {
                File tohRootPath = Paths.get(getFilesDir().getPath(), ".config", "toh").toFile();
                if (!tohRootPath.exists()) {
                    boolean ret = tohRootPath.mkdirs();
                    if (!ret) {
                        runOnUiThread(() -> tips("can not create " + tohRootPath.getPath()));
                        return;
                    }
                }
                if (resp.body() == null) {
                    runOnUiThread(() -> tips("download error"));
                    return;
                }
                byte[] config = resp.body().bytes();
                Files.write(Paths.get(getFilesDir().getPath(), ".config", "toh", "socks5.yml"), config);
                runOnUiThread(() -> tips("downloaded"));
            } catch (IOException e) {
                runOnUiThread(() -> tips(e.getMessage()));
            } finally {
                runOnUiThread(() -> connectButton.setEnabled(true));
            }
        });
    }

    private void tips(String tips) {
        Snackbar.make(findViewById(R.id.buttonLayout), tips, BaseTransientBottomBar.LENGTH_LONG).show();
    }

    private void registerEventListener() {
        eventListener.subscribe(R.string.log, log -> {
            logView.append(log + "\n");
            ScrollView scrollView = findViewById(R.id.logContainer);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
        eventListener.subscribe(R.string.tohStarted, ignored -> connectButton.setText(R.string.connected));
        eventListener.subscribe(R.string.tohStopped, ignored -> disconnect(null));

    }

    private void connect(View v) {
        startVpnService();
    }

    private int clickCount;
    private long lastClickTime;

    private void showLogComponent(View v) {
        if (lastClickTime > 0 && System.currentTimeMillis() - lastClickTime > 300) {
            clickCount = 0;
        }
        clickCount++;
        lastClickTime = System.currentTimeMillis();
        if (clickCount == 2) {
            showLogComponent();
        }
    }

    private void showLogComponent() {
        View logComponent = findViewById(R.id.logComponent);
        View btnComponent = findViewById(R.id.buttonLayout);

        int logComponentHeight = getResources().getDisplayMetrics().heightPixels - btnComponent.getHeight();

        if (logComponent.getVisibility() == VISIBLE) {
            logComponent.animate()
                    .translationY(logComponentHeight)
                    .setDuration(800)
                    .withEndAction(() -> logComponent.setVisibility(GONE))
                    .start();
            btnComponent.animate().translationY((int) (logComponentHeight / 2))
                    .setDuration(800)
                    .withEndAction(() -> btnComponent.setTranslationY(0))
                    .start();
            return;
        }
        logComponent.setVisibility(VISIBLE);
        System.out.println(logComponent.getHeight() + "------------");
        logComponent.setTranslationY(logComponentHeight); // 将组件的 Y 轴偏移量设置为组件高度，使其位于底部
        logComponent.animate()
                .translationY(0) // 将组件的 Y 轴偏移量设置为 0，向上移动到原始位置
                .setDuration(300) // 设置动画持续时间为 1000 毫秒
                .start(); // 启动动画
        btnComponent.setTranslationY((int) (logComponentHeight / 2));
        btnComponent.animate().translationY(0).setDuration(300).start();
    }

    private void disconnect(View v) {
        msgBus.pub(R.string.stopVpn);
        stopService(vpnService);
        stopService(tohService);
        connectButton.setText(R.string.connect);
        connectButton.setOnClickListener(this::connect);
        showLogComponent();
    }

    private boolean showScanQRButton(View v) {
        findViewById(R.id.scanQR).setVisibility(VISIBLE);
        return true;
    }

    private boolean hideScanQRButton(View v) {
        findViewById(R.id.scanQR).setVisibility(GONE);
        Snackbar.make(findViewById(R.id.buttonLayout), "scan QR canceled", BaseTransientBottomBar.LENGTH_LONG).show();
        return true;
    }

    private boolean showClearLogButton(View v) {
        if (findViewById(R.id.clearLog).getVisibility() == VISIBLE) {
            findViewById(R.id.clearLog).setVisibility(GONE);
        } else {
            findViewById(R.id.clearLog).setVisibility(VISIBLE);
        }
        return true;
    }

    private void clearLog(View v) {
        logView.setText("");
        v.setVisibility(GONE);
    }

    private void scanQR(View v) {
        scanLauncher.launch(new ScanOptions()
                .setPrompt("Scan a ToH QR Code")
                .setBeepEnabled(false)
                .setBarcodeImageEnabled(true));
    }

    private void startVpnService() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            vpnLauncher.launch(intent);
        } else {
            connectButton.setText(R.string.connecting);
            startService(vpnService);
            startService(tohService);
            showLogComponent();
            connectButton.setOnClickListener(this::disconnect);
        }
    }
}