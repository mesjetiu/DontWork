package info.pauek.dontwork;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by pauek on 29/10/15.
 */
public class DontWorkService extends Service {

    ScreenOnOffReceiver receiver;



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
                    if (timesOn % 5 == 0) {
                        Intent i = new Intent(DontWorkService.this, AlertActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                }
            }
        }
    }
}
