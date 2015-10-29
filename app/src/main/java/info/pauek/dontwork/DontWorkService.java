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

        private long lastOn;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                long diff = System.currentTimeMillis() - lastOn;
                Log.i("DontWork", String.format("Screen on: %.3f seconds", (float)diff / 1000.0f));
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                lastOn = System.currentTimeMillis();
            }
        }
    }
}
