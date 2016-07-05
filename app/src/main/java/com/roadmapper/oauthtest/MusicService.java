package com.roadmapper.oauthtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 * <p>
 * To implement a MediaBrowserService, you need to:
 * <p>
 * <ul>
 * <p>
 * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
 * related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
 * {@link android.service.media.MediaBrowserService#onLoadChildren};
 * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
 * with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
 * <p>
 * <li> Set a callback on the
 * {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
 * The callback will receive all the user's actions, like play, pause, etc;
 * <p>
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 * {@link android.media.MediaPlayer})
 * <p>
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
 * {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
 * {@link android.media.session.MediaSession#setQueue(java.util.List)})
 * <p>
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 * <p>
 * </ul>
 * <p>
 * To make your app compatible with Android Auto, you also need to:
 * <p>
 * <ul>
 * <p>
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 * <p>
 * </ul>
 *
 * @see <a href="README.md">README.md</a> for more details.
 */
public class MusicService extends MediaBrowserServiceCompat implements PlaybackManager.Callback {

    private MediaSessionCompat mSession;
    private PlaybackManager mPlayback;
    private static final String TAG = "MusicService";
    private BroadcastReceiver mCarConnectionReceiver;
    private boolean mIsConnectedToCar;
    private MediaNotificationManager2 mediaNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mSession = new MediaSessionCompat(this, "MusicService");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(mCallback);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        registerCarConnectionReceiver();

        try {
            mediaNotificationManager = new MediaNotificationManager2(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
        mPlayback = new PlaybackManager(this, this);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mCarConnectionReceiver);
        // Service is being killed, so make sure we release our resources
        mPlayback.handleStopRequest(null);
        mediaNotificationManager.stopNotification();

        //mDelayedStopHandler.removeCallbacksAndMessages(null);
        mSession.release();
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(getString(R.string.app_name), null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result) {
        result.sendResult(MusicLibrary.getMediaItems());
    }

    private Callback<ResponseBody> streamUrlCallback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            List<String> url = call.request().url().pathSegments();
            String trackId = url.get(url.indexOf("tracks") + 1);
            String streamUrl = null;
            okhttp3.Headers headers = response.headers();
            streamUrl = headers.get("Location");

            if (!TextUtils.isEmpty(streamUrl)) {
                MusicLibrary.setSongStreamUri(trackId, streamUrl);
                MediaMetadataCompat metadata = MusicLibrary.getMetadata(MusicService.this, trackId);
                mSession.setActive(true);
                mSession.setMetadata(metadata);
                mPlayback.play(metadata);
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.d("MainActivity", "Failure");
            Log.d("MainActivity", t.getMessage());
        }
    };

    final MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            if (mPlayback.getCurrentMediaId() != null)
                onPlayFromMediaId(mPlayback.getCurrentMediaId(), null);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            /*mSession.setActive(true);*/
            MediaMetadataCompat metadata = MusicLibrary.getMetadata(MusicService.this, mediaId);


// TODO: Don't want to always call the API to get the URL if we have cached it
            SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
            Call<ResponseBody> call = client.getStreamInfo(Long.parseLong(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)));
            call.enqueue(streamUrlCallback);
            /*mSession.setMetadata(metadata);
            mPlayback.play(metadata);*/
        }

        @Override
        public void onPause() {
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            stopSelf();
        }

        @Override
        public void onSkipToNext() {
            onPlayFromMediaId(MusicLibrary.getNextSong(mPlayback.getCurrentMediaId()), null);
        }

        @Override
        public void onSkipToPrevious() {
            onPlayFromMediaId(MusicLibrary.getPreviousSong(mPlayback.getCurrentMediaId()), null);
        }
    };

    private void registerCarConnectionReceiver() {
        IntentFilter filter = new IntentFilter("com.google.android.gms.car.media.STATUS");

        mCarConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String connectionEvent = intent.getStringExtra("media_connection_status");
                mIsConnectedToCar = "media_connected".equals(connectionEvent);
                Log.i(TAG, "Connection event to Android Auto: " + connectionEvent +
                        " isConnectedToCar=" + mIsConnectedToCar);
            }
        };
        registerReceiver(mCarConnectionReceiver, filter);
    }

    @Override
    public void onPlaybackStart() {
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        //mDelayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), MusicService.class));
    }

    /**
     * (non-Javadoc)
     *
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        if (startIntent != null) {
            String action = startIntent.getAction();
            //String command = startIntent.getStringExtra(CMD_NAME);

        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        //mDelayedStopHandler.removeCallbacksAndMessages(null);
        //mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    @Override
    public void onNotificationRequired() {
        mediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStop() {
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        //mDelayedStopHandler.removeCallbacksAndMessages(null);
        //mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        Log.d(TAG, "Playback state updated: " + newState.toString());
        mSession.setPlaybackState(newState);
    }

    @Override
    public void onMetadataUpdated(MediaMetadataCompat metadata) {
        Log.d(TAG, "Metadata updated: " + metadata.toString());
        mSession.setMetadata(metadata);
    }
}
