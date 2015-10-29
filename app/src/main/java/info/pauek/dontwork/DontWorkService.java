package info.pauek.dontwork;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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

    ScreenOnOffReceiver receiver;
    private View blockScreenView;

    @Override
    public void onCreate() {
        receiver = new ScreenOnOffReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DontWork", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class ScreenOnOffReceiver extends BroadcastReceiver {

        private boolean screenOn = false;
        private long lastOn = -1;
        private int timesOn = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                screenOn = false;
                if (lastOn != -1) {
                    long diff = System.currentTimeMillis() - lastOn;
                    Log.i("DontWork", String.format("Screen on: %.3f seconds", (float) diff / 1000.0f));
                    lastOn = -1;
                }
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                screenOn = true;
            } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                if (screenOn) {
                    lastOn = System.currentTimeMillis();
                    timesOn++;
                    if (timesOn % 3 == 0) {
                        showBlockingScreen();
                    }
                }
            }
        }

        private void showBlockingScreen() {
            Log.i("DontWork", "showBlockingScreen");
            Rect displaySize = new Rect();
            WindowManager wmgr = (WindowManager) getSystemService(WINDOW_SERVICE);
            wmgr.getDefaultDisplay().getRectSize(displaySize);
            Log.i("DontWork", String.format("DisplaySize is %d, %d", displaySize.width(), displaySize.height()));

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
                    removeBlockingScreen();
                }
            }, 20000);
        }

        private void removeBlockingScreen() {
            Log.i("DontWork", "removeBlockingScreen");
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.removeView(blockScreenView);
        }
    }
}
