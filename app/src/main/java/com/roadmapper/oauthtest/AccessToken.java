package com.roadmapper.oauthtest;

import java.util.Date;

/**
 * An OAuth access token holder.
 */
public class AccessToken {

    String accessToken;
    String scope;
    Date expiresIn;
    String refreshToken;

    AccessToken(String accessToken, String scope) {
        this.accessToken = accessToken;
        this.scope = scope;
    }

    /*AccessToken(String accessToken, String scope) {
        this.accessToken = accessToken;
        this.scope = scope;
    }*/
}
