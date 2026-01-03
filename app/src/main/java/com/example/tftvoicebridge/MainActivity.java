package com.example.tftvoicebridge;

import android.app.Activity; // Düz Activity
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

// AppCompatActivity yerine Activity kullanıyoruz (Tema istemez)
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);

        Button btnStart = new Button(this);
        btnStart.setText("BAŞLAT");
        
        Button btnStop = new Button(this);
        btnStop.setText("DURDUR");
        btnStop.setBackgroundColor(0xFFFF0000);
        btnStop.setTextColor(0xFFFFFFFF);

        layout.addView(btnStart);
        layout.addView(btnStop);
        setContentView(layout);

        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) {
                Intent serviceIntent = new Intent(this, FloatingAudioService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                moveTaskToBack(true);
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingAudioService.class));
            Toast.makeText(this, "Servis Durduruldu", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
                Toast.makeText(this, "Lütfen 'Üstte Göster' iznini verin", Toast.LENGTH_LONG).show();
                return false;
            }
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
                return false;
            }
        }
        return true;
    }
}
