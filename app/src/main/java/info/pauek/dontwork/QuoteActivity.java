package info.pauek.dontwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class QuoteActivity extends Activity {

    public static final String firstTime = "info.pauek.dontwork.firstTime";
    private RelativeLayout quote_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote);

        quote_layout = (RelativeLayout) findViewById(R.id.quote_layout);

        Intent intent = getIntent();
        boolean isFirst = intent.getBooleanExtra(firstTime, false);
        if (isFirst) {
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableClick();
                }
            }, 10 * 1000);
        }
        else {
            enableClick();
        }
    }

    private void enableClick() {
        Log.i("DontWork", "You can click now...");
        quote_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuoteActivity.this.finish();
            }
        });
    }
}
