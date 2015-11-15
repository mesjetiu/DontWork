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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
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
    private static final float minutesPerDay = 24.0f * 60.0f;

    // Parameters
    private int param1 = 1; // 1h
    private int param2 = 5; // 10 minutes
    private float paramScreenOnMinutesPerDay = 60.0f; // 1 hour screen on time every day
    private float paramMaxConsecutiveMinutes = 10.0f;

    public void setParam1(int x) {
        param1 = x;
        paramScreenOnMinutesPerDay = (param1 + 1) * 30.0f;
    }

    public String getTextParam1() {
        String s;
        int min = (int)paramScreenOnMinutesPerDay;
        int H = min / 60;
        int M = min % 60;
        String sh = "", sm = "";
        if (H > 1) {
            String shours = getResources().getString(R.string.hours);
            sh = String.format("%d %s", H, shours);
        } else if (H == 1) {
            String shour = getResources().getString(R.string.hour);
            sh = String.format("1 %s", shour);
        }
        if (M != 0) {
            String sminutes = getResources().getString(R.string.minutes);
            sm = String.format("%d %s", M, sminutes);
        }
        String result = sh;
        if (!sm.isEmpty()) {
            result = result + " " + sm;
        }
        return result;
    }

    public void setParam2(int x) {
        param2 = x;
        int minutes = param2 + 5;
        paramMaxConsecutiveMinutes = (float)minutes;
    }

    public int getMinutesParam2() {
        return param2 + 5;
    }

    public String getTextParam2() {
        return String.format("%d minutes", getMinutesParam2());
    }

    public String getTextDetail3() {
        String template = getString(R.string.detail_text_3);
        int ratio = (int)(1.0f / getRatio());
        return String.format(template, ratio, ratio);
    }

    public String getTextDetail4() {
        String template = getString(R.string.detail_text_4);
        int blockTimeMinutes = getBlockTimeMinutes();
        return String.format(template, blockTimeMinutes);
    }

    public float getRatio() {
        return paramScreenOnMinutesPerDay / (minutesPerDay - paramScreenOnMinutesPerDay);
    }

    public int getBlockTimeMinutes() {
        return (int)((1 / getRatio()) * (paramMaxConsecutiveMinutes / 2.0f));
    }

    // State
    private boolean watching = false;
    private DontWorkReceiver receiver = null;
    private IntentFilter filter = null;
    private boolean blocking = false;

    private View blockScreenView;
    private boolean blockingViewActive = false;
    private WindowManager.LayoutParams blockViewParams;
    private WindowManager wmgr;

    @Override
    public void onCreate() {
        Log.i("DontWork", "DontWorkService.onCreate");
        receiver = new DontWorkReceiver();
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        blockScreenView = inflater.inflate(R.layout.blockscreen, null);

        Rect displaySize = new Rect();
        wmgr = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmgr.getDefaultDisplay().getRectSize(displaySize);

        blockViewParams = new WindowManager.LayoutParams(
                displaySize.width(),
                displaySize.height(),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                0,
                PixelFormat.TRANSLUCENT);
        blockViewParams.gravity = Gravity.CENTER;
        blockViewParams.setTitle("Load Average");

        setupCallStateListener();
    }

    private void setupCallStateListener() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // Create a new PhoneStateListener
        PhoneStateListener listener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        activateBlockingView();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        deactivateBlockingView();
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        deactivateBlockingView();
                        break;
                }
            }
        };

        // Register the listener with the telephony manager
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
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

    public int getParam1() { return param1; }
    public int getParam2() { return param2; }

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
        Log.i("DontWork", String.format("ratio = %.3f", getRatio()));
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
    }

    private void onScreenUnlocked() {
        if (!screenOn) {
            Log.e("DontWork", "onScreenUnlocked and screenOn == false????");
        }
        if (!blocking) {
            addOffTime();
            timesOn++;
            bucket += 1.0; // penalty for every screenOn (except if already blocking)
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
            long blockTimeMillis = (long)(getBlockTimeMinutes() * 60 * 1000);
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
        bucket -= ((float)diff / 60000.0f) * getRatio();
        if (bucket < 0.0f) {
            bucket = 0.0f;
        }
        Log.i("DontWork", String.format("OFF %.3f seconds (bucket %.3f)", (float) diff / 1000.0f, bucket));
        last = System.currentTimeMillis();
    }

    public class DontWorkReceiver extends BroadcastReceiver {
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
        activateBlockingView();

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
        wmgr.removeView(blockScreenView);

        if (screenOn) {
            postTick(5000);
        }
    }

    private void activateBlockingView() {
        if (!blocking) {
            return;
        }
        if (!blockingViewActive) {
            wmgr.addView(blockScreenView, blockViewParams);
            blockingViewActive = true;
        }

    }

    private void deactivateBlockingView() {
        if (!blocking) {
            return;
        }
        if (blockingViewActive) {
            wmgr.removeView(blockScreenView);
            blockingViewActive = false;
        }
    }
}
