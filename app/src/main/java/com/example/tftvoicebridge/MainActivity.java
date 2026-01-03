package com.example.tftvoicebridge;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Arayüzü kod ile oluşturuyoruz (XML dosyasıyla uğraşmamak için)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);

        Button btnStart = new Button(this);
        btnStart.setText("BAŞLAT (Odaya Gir)");
        
        Button btnStop = new Button(this);
        btnStop.setText("DURDUR (Odadan Çık)");
        btnStop.setBackgroundColor(0xFFFF0000); // Kırmızı
        btnStop.setTextColor(0xFFFFFFFF);

        layout.addView(btnStart);
        layout.addView(btnStop);
        setContentView(layout);

        // Başlat Butonu
        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) {
                Intent serviceIntent = new Intent(this, FloatingAudioService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                // Uygulamayı alta at (Ana ekrana dön)
                moveTaskToBack(true);
            }
        });

        // Durdur Butonu
        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingAudioService.class));
            Toast.makeText(this, "Bağlantı Kesildi", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Yüzen Pencere İzni Kontrolü
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
                Toast.makeText(this, "Lütfen 'Üstte Göster' iznini verin", Toast.LENGTH_LONG).show();
                return false;
            }
            // Mikrofon İzni Kontrolü
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
                return false;
            }
        }
        return true;
    }
}
