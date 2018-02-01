package com.roadmapper.oauthtest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    private final String redirectUri = "echo://auth";

    // This is the unique URL to connect to SoundCloud Connect to get the OAuth token
    // TODO: This string is the only documentation of how to get to the Connect endpoint, this needs to be more clear.
    // This URL is opened up in a browser and the result of it is sent back to the
    // callback URI that is intercepted by this application and the token is embedded
    // in the URI and parsed out for future usage.
    // It is currently set to non-expiring, but that may need to change if pushed
    // to the Play Store. That would need logic to check the expiry and then request a new
    // token if possible to auth with SoundCloud's API
    private final String url = AutoCloudApplication.BASE_URL + "/connect" + "?client_id=" +
            AutoCloudApplication.CLIENT_ID + "&redirect_uri=" + redirectUri +
            "&response_type=token&display=popup&scope=non-expiring";

    @BindView(R.id.sign_in_button)
    Button signInButton;

    @BindView(R.id.web_button)
    Button webButton;

    @Override
    public void onStart() {
        super.onStart();

        String code = AutoCloudApplication.OAUTH_TOKEN_WEB;
        //String code = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_token);
        //String scope = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_scope);
        String scope = "*";

        if (!"".equals(code) && !"".equals(scope)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });

        webButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), WebActivity.class);
                intent.putExtra("url", "http://www.soundcloud.com");
                startActivity(intent);
            }
        });
    }

}
