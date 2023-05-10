package sh.toh.app;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import sh.toh.app.msg.UIUpdateEventListener;
import sh.toh.app.srv.Socks5VpnService;
import sh.toh.app.srv.TohService;

public class MainActivity extends AppCompatActivity {

    private TextView logView;

    private Button connectButton;

    private Intent tohService;

    private Intent vpnService;

    ActivityResultLauncher<Intent> getPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != RESULT_OK) {
            Snackbar.make(findViewById(R.id.logContainer), "Permission not granted!", Snackbar.LENGTH_LONG).show();
            return;
        }
        startVpnService();
    });

    UIUpdateEventListener broadcastReceiver = new UIUpdateEventListener() {

        @Override
        public void onLog(String log) {
            logView.append(log);
        }

        @Override
        public void onTohSocks5Started() {
            connectButton.setText("Connected");
        }

        @Override
        public void onTohSocks5Stopped() {
            connectButton.setText("Connect");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        broadcastReceiver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        broadcastReceiver.unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        logView = findViewById(R.id.logView);
        connectButton = findViewById(R.id.button);
        connectButton.setOnClickListener(this::connect);
        tohService = new Intent(this, TohService.class);
        vpnService = new Intent(this, Socks5VpnService.class);
    }

    private void connect(View v) {
        startVpnService();
        connectButton.setText("Connecting");
        connectButton.setOnClickListener(this::disconnect);
    }

    private void disconnect(View v) {
        stopService(vpnService);
        sendBroadcast(new Intent(getPackageName() + ".closeEvent"));
        stopService(tohService);
        connectButton.setText("Connect");
        connectButton.setOnClickListener(this::connect);
    }

    private void startVpnService() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            getPermission.launch(intent);
            Snackbar.make(findViewById(R.id.logContainer), "Please grant permission", Snackbar.LENGTH_LONG).show();
        } else {
            startService(vpnService);
            startService(tohService);
        }
    }
}