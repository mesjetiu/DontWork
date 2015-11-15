package info.pauek.dontwork;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        WebView webview = (WebView)findViewById(R.id.webview);
        webview.loadData(getString(R.string.about_text), "text/html; charset=UTF-8", null);
    }
}
