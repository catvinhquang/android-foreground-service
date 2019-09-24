package com.quangcv.fs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

/**
 * Created by QuangCV on 24-Sep-2019
 **/

public class MyService extends Service {

    String TAG = MyService.class.getSimpleName();
    String CHANNEL_ID = "CHANNEL_ID";
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    OutputStreamWriter w;
    PowerManager.WakeLock wakeLock;
    long lastTime;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager m = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (m != null) {
                    log(m.isDeviceIdleMode() ? "IDLE ON" : "IDLE OFF");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
            registerReceiver(receiver, filter);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager m = getSystemService(NotificationManager.class);
            if (m != null) {
                NotificationChannel c = m.getNotificationChannel(CHANNEL_ID);
                if (c == null) {
                    c = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
                    c.enableVibration(false);
                    c.enableLights(false);
                    c.setShowBadge(false);
                    c.setSound(null, null);
                    m.createNotificationChannel(c);
                }
            }
        }

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle("Title")
                .setContentText("Text")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(1, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyService:wakelock");
            wakeLock.acquire();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream s = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/log.txt");
                    w = new OutputStreamWriter(s);

                    log("START");
                    lastTime = System.currentTimeMillis();

                    while (true) {
                        try {
                            Thread.sleep(1000);
                            log(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    void log(String message) {
        long current = System.currentTimeMillis();
        if (w != null) {
            try {
                w.append(formatter.format(current));
                if (!TextUtils.isEmpty(message)) {
                    w.append(" ")
                            .append(message)
                            .append("\n");
                } else {
                    w.append(" ")
                            .append(String.valueOf((current - lastTime) / 1000))
                            .append(" seconds\n");
                    lastTime = current;
                }
                w.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}