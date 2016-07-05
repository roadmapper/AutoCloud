package com.roadmapper.oauthtest;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;

public class WebActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        /*AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("soundcloud.com");
        for (Account a: accounts
             ) {
            Log.d("WebActivity", a.toString());
            try {
                String token = manager.getAuthToken(a, "SID", null, this, null, null)
                        .getResult().getString(AccountManager.KEY_AUTHTOKEN);
                Log.d("WebActivity", token);
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                e.printStackTrace();
            }
        }*/


        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebClient());



        WebSettings settings = webView.getSettings();
        settings.setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);


        webView.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("Show Cookie");
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                Toast.makeText(getApplicationContext(), "Cookie: " + CookieManager.getInstance().getCookie("soundcloud.com"), Toast.LENGTH_SHORT).show();
                String cookie = CookieManager.getInstance().getCookie("soundcloud.com");
                Log.d("WebActivity", cookie);
                String[] temp=cookie.split(";");
                for (String ar1 : temp ){
                    //if(ar1.contains(CookieName)){
                    Log.d("WebActivity", ar1);
                        String[] temp1 = ar1.split("=");
                        //CookieValue = temp1[1];
                    //}
                }

                webView.evaluateJavascript("javascript:window.localStorage.getItem(\"V2::local::broadcast\")", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Log.d("WebView", value);

                        /*JsonParser parser = new JsonParser();
                        parser.parse(value);*/

                        JsonReader reader = new JsonReader(new StringReader(value));

                        // Must set lenient to parse single values
                        reader.setLenient(true);

                        try {
                            if(reader.peek() != JsonToken.NULL) {
                                if(reader.peek() == JsonToken.STRING) {
                                    String msg = reader.nextString();
                                    if(msg != null) {
                                        Toast.makeText(getApplicationContext(),
                                                msg, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }

                            //reader.
                        } catch (IOException e) {
                            Log.e("TAG", "MainActivity: IOException", e);
                        } finally {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                // NOOP
                            }
                        }

                    }
                });


                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private class MyWebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().endsWith("soundcloud.com"))
                return false;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }
}
