package com.example.tftvoicebridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.core.app.NotificationCompat;
import java.net.URISyntaxException;
import java.util.Arrays;
import io.socket.client.IO;
import io.socket.client.Socket;

public class FloatingAudioService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private Socket socket;

    // --- BURAYI KENDİ VDS IP ADRESİNLE DEĞİŞTİR ---
    private static final String VDS_URL = "http://192.168.1.XX:5000"; 
    // ----------------------------------------------

    private static final int SAMPLE_RATE = 48000;
    private boolean isMicActive = false;
    private AudioRecord recorder;
    private AudioTrack player;
    private Thread recordingThread;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService(); // Bildirimi göster
        setupSocket();            // VDS'e bağlan
        setupFloatingWidget();    // Butonu ekrana koy
        setupAudioPlayer();       // Hoparlörü hazırla
    }

    private void setupSocket() {
        try {
            socket = IO.socket(VDS_URL);
            socket.connect();
            
            // Sunucudan (Discord'dan) ses gelince çal
            socket.on("discord_audio", args -> {
                if (args.length > 0 && player != null) {
                    byte[] data = (byte[]) args[0];
                    player.write(data, 0, data.length);
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void setupAudioPlayer() {
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        player = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, 
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 
                bufferSize, AudioTrack.MODE_STREAM);
        player.play();
    }

    private void setupFloatingWidget() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        ImageView icon = new ImageView(this);
        icon.setBackgroundColor(Color.RED); // Başlangıçta KAPALI (Kırmızı)
        
        int size = (int) (50 * getResources().getDisplayMetrics().density); // 50dp boyut

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        floatingView = icon;

        // Sürükleme ve Tıklama Mantığı
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long startClickTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        startClickTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Kısa dokunuş ise TIKLAMA say
                        if (Math.abs(event.getRawX() - initialTouchX) < 10 && (System.currentTimeMillis() - startClickTime) < 200) {
                            toggleMic((ImageView) v);
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Parmağı takip et (Sürükle)
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);
    }

    private void toggleMic(ImageView view) {
        if (isMicActive) {
            // Kapat
            isMicActive = false;
            view.setBackgroundColor(Color.RED);
        } else {
            // Aç
            isMicActive = true;
            view.setBackgroundColor(Color.GREEN);
            startRecording();
        }
    }

    private void startRecording() {
        recordingThread = new Thread(() -> {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            byte[] buffer = new byte[minBufferSize];
            
            if (recorder == null) {
                recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
            }
            
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording();
            }

            while (isMicActive) {
                int readBytes = recorder.read(buffer, 0, buffer.length);
                // Sadece dolu veriyi gönder (Fix)
                if (readBytes > 0 && socket.connected()) {
                    byte[] validData = Arrays.copyOf(buffer, readBytes);
                    socket.emit("mic_data", validData);
                }
            }
            
            try {
                recorder.stop();
                recorder.release();
                recorder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        recordingThread.start();
    }

    private void startForegroundService() {
        String CHANNEL_ID = "TFTBridgeChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Voice Bridge", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TFT Ses Köprüsü")
                .setContentText("Arka planda çalışıyor.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isMicActive = false;
        if (floatingView != null) windowManager.removeView(floatingView);
        if (socket != null) socket.disconnect();
        if (player != null) player.release();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
