package info.pauek.dontwork;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by pauek on 29/10/15.
 *
 * Documentación interesante sobre permisos y SYSTEM_ALERT_WINDOW
 *
 * http://www.androidpolice.com/2015/09/07/android-m-begins-locking-down-floating-apps-requires-users-to-grant-special-permission-to-draw-on-other-apps/
 *
 *
 *
 */
public class DontWorkService extends Service {

    // Constants
    private static final float minutesPerDay = 16.0f * 60.0f;

    // Parameters
    private float paramScreenOnMinutesPerDay = 60.0f; // 1 hour screen on time every day (16 hours of wake time)
    private float paramMaxConsecutiveMinutes = 10.0f;

    void setParam1(float x) { paramScreenOnMinutesPerDay = x; }
    void setParam2(float x) { paramMaxConsecutiveMinutes = x; }

    private float ratio; // the relative weight of screenOffTime with respect to screenOnTime

    // State
    private boolean watching = false;
    private ScreenOnOffReceiver receiver = null;
    private IntentFilter filter = null;
    private boolean blocking = false;

    @Override
    public void onCreate() {
        Log.i("DontWork", "DontWorkService.onCreate");
        receiver = new ScreenOnOffReceiver();
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DontWork", "DontWorkService.onStartCommand: start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        DontWorkService getService() {
            return DontWorkService.this;
        }
    }

    public boolean isWatching() {
        return watching;
    }

    public void startWatching() {
        Log.i("DontWork", "DontWorkService.startWatching");
        ratio = paramScreenOnMinutesPerDay / (minutesPerDay - paramScreenOnMinutesPerDay);
        Log.i("DontWork", String.format("ratio = %.3f", ratio));
        reset();
        registerScreenOnOffReceiver();
        postTick(5000);
    }

    public void stopWatching() {
        Log.i("DontWork", "DontWorkService.stopWatching");
        unregisterScreenOnOffReceiver();
    }

    private void registerScreenOnOffReceiver() {
        if (!watching) {
            registerReceiver(receiver, filter);
            watching = true;
        }
    }

    private void unregisterScreenOnOffReceiver() {
        if (watching) {
            unregisterReceiver(receiver);
            watching = false;
        }
    }

    private View blockScreenView;

    private boolean screenOn = false;
    private boolean screenUnlocked = false;
    private long last = -1;
    private int timesOn = 0;
    private long totalOn = 0, totalOff = 0;
    private float bucket = 0.0f; // the bucket fills with screen-on-time and empties with screen-off-time * ratio
                                 // In minutes!

    private void reset() {
        // start counting now
        totalOn = 0;
        totalOff = 0;
        bucket = 0.0f;
        screenOn = true;       // can only call reset with startWatching and that is from the activity
        screenUnlocked = true; // idem
        last = System.currentTimeMillis();
        timesOn = 1;
    }

    private void onScreenOff() {
        if (screenUnlocked && !blocking) {
            addOnTime();
        }
        last = System.currentTimeMillis();
    }

    private void onScreenUnlocked() {
        if (!screenOn) {
            Log.e("DontWork", "onScreenUnlocked and screenOn == false????");
        }
        addOffTime();
        timesOn++;
        if (!blocking) {
            considerBlockingScreen();
            if (!blocking) {
                postTick(5000);
            }
        }
    }

    private void postTick(int millis) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, millis);
    }

    private void tick() {
        if (watching && screenUnlocked && !blocking) {
            addOnTime();
            considerBlockingScreen();
            if (!blocking) {
                postTick(5000);
            }
        }
    }

    private void considerBlockingScreen() {
        if (blocking) {
            return;
        }
        if (bucket > paramMaxConsecutiveMinutes) {
            // block the screen to halve the size of the bucket
            long blockTimeMillis = (long)((1 / ratio) * (bucket / 2 * 60 * 1000));
            blockScreen(blockTimeMillis);
        }
    }

    private void addOnTime() {
        long diff = System.currentTimeMillis() - last;
        totalOn += diff;
        bucket += (float) diff / 60000.0f;
        Log.i("DontWork", String.format("ON %.3f seconds (bucket %.3f)", (float) diff / 1000.0f, bucket));
        last = System.currentTimeMillis();
    }

    private void addOffTime() {
        long diff = System.currentTimeMillis() - last;
        totalOff += diff;
        bucket -= ((float)diff / 60000.0f) * ratio;
        if (bucket < 0.0f) {
            bucket = 0.0f;
        }
        Log.i("DontWork", String.format("OFF %.3f seconds (bucket %.3f)", (float) diff / 1000.0f, bucket));
        last = System.currentTimeMillis();
    }

    public class ScreenOnOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                    if (watching) {
                        onScreenOff();
                    }
                    screenOn = false;
                    screenUnlocked = false;
                    break;
                case Intent.ACTION_SCREEN_ON:
                    screenOn = true;
                    break;
                case Intent.ACTION_USER_PRESENT:
                    screenUnlocked = true;
                    if (watching) {
                        onScreenUnlocked();
                    }
                    break;
            }
        }
    }

    private void blockScreen(long blockTimeMillis) {
        blocking = true;

        Log.i("DontWork", String.format("Blocking screen for %.3f minutes", (float)blockTimeMillis / 60000.0f));
        Rect displaySize = new Rect();
        WindowManager wmgr = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmgr.getDefaultDisplay().getRectSize(displaySize);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        blockScreenView = inflater.inflate(R.layout.blockscreen, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                displaySize.width(),
                displaySize.height(),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                0,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.setTitle("Load Average");
        wmgr.addView(blockScreenView, params);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                unblockScreen();
            }
        }, blockTimeMillis);
    }

    private void unblockScreen() {
        addOffTime();
        blocking = false;

        Log.i("DontWork", "unblockScreen");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.removeView(blockScreenView);

        if (screenOn) {
            postTick(5000);
        }
    }
}
