package info.pauek.dontwork;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    public static int OVERLAY_PERMISSION_REQ_CODE = 1;

    private DontWorkService service;
    private ScrollView scrollView;
    private Button startButton;
    private SeekBar barParam1, barParam2;
    private TextView textParam1, textParam2;
    private TextView textDetail1, textDetail2, textDetail3, textDetail4;

    private boolean visibleDetails = true;
    private LinearLayout detail_text;
    private ImageView flecha;

    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Log.e("DontWork", "Permission not granted!");
                    Toast.makeText(
                        this,
                        R.string.permission_not_granted,
                        Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            }
        }
    }

    public static final String PREFS_NAME = "info.pauek.dontwork.prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestOverlayPermission();

        // Start service
        Intent intent = new Intent(this, DontWorkService.class);
        startService(intent);

        // Determine if this is the first time we run.
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstRun = settings.getBoolean("firstRun", true);
        if (firstRun) {
            // Lanzar actividad con la cita del filósofo
            Toast.makeText(this, "Cita filósofo", Toast.LENGTH_SHORT).show();

            // Never again...
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("firstRun", false);
            editor.commit();
        } else {
            hideDetails();
        }

        scrollView  = (ScrollView)findViewById(R.id.scroll_view);
        startButton = (Button)findViewById(R.id.button);
        detail_text = (LinearLayout)findViewById(R.id.detail_text);
        flecha      = (ImageView)findViewById(R.id.flecha);
        textParam1  = (TextView)findViewById(R.id.param_text_1);
        textParam2  = (TextView)findViewById(R.id.param_text_2);
        textDetail1 = (TextView)findViewById(R.id.detail_text_1);
        textDetail2 = (TextView)findViewById(R.id.detail_text_2);
        textDetail3 = (TextView)findViewById(R.id.detail_text_3);
        textDetail4 = (TextView)findViewById(R.id.detail_text_4);
        barParam1   = (SeekBar)findViewById(R.id.bar_param_1);
        barParam2   = (SeekBar)findViewById(R.id.bar_param_2);

        barParam1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (service != null) {
                    service.setParam1(progress);
                    updateTexts();
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        barParam2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (service != null) {
                    service.setParam2(progress);
                    updateTexts();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateTexts() {
        textParam1.setText(service.getTextParam1());
        textParam2.setText(service.getTextParam2());
        textDetail1.setText(String.format(getString(R.string.detail_text_1), service.getTextParam2()));
        textDetail2.setText(String.format(getString(R.string.detail_text_2), service.getTextParam1()));
        textDetail3.setText(service.getTextDetail3());
        textDetail4.setText(service.getTextDetail4());
    }

    private ServiceConnection connection;

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to service
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DontWorkService.LocalBinder binder = (DontWorkService.LocalBinder)service;
                MainActivity.this.service = binder.getService();

                boolean isWatching = MainActivity.this.service.isWatching();
                if (isWatching) {
                    startButton.setText(R.string.stop);

                }

                DontWorkService srv = MainActivity.this.service;
                barParam1.setProgress(srv.getParam1());
                barParam2.setProgress(srv.getParam2());
                barParam1.setEnabled(!isWatching);
                barParam2.setEnabled(!isWatching);
                updateTexts();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                MainActivity.this.service = null;
            }
        };

        Intent intent = new Intent(this, DontWorkService.class);
        boolean boundService = bindService(intent, connection, 0);
        if (!boundService) {
            Log.e("DontWork", "Cannot bind to service!");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (service != null) {
            unbindService(connection);
            service = null;
        }
    }

    public void onStartOrStop(View view) {
        if (!service.isWatching()) {
            service.startWatching();
            startButton.setText(R.string.stop);
            startButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause, 0, 0, 0);
            barParam1.setEnabled(false);
            barParam2.setEnabled(false);
        } else {
            service.stopWatching();
            startButton.setText(R.string.start);
            startButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
            barParam1.setEnabled(true);
            barParam2.setEnabled(true);
        }
    }

    public void onClickDetails(View view) {
        visibleDetails = !visibleDetails;
        if (!visibleDetails) {
            hideDetails();
        } else {
            detail_text.setVisibility(View.VISIBLE);
            flecha.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_up_float));
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    public void hideDetails() {
        detail_text.setVisibility(View.GONE);
        flecha.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_down_float));
    }
}
