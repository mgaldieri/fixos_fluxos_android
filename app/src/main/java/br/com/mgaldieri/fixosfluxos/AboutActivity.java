package br.com.mgaldieri.fixosfluxos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends AppCompatActivity {

    private WebView about;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        about = (WebView)findViewById(R.id.about_text);
        about.loadUrl("file:///android_asset/about.html");

    }

}
