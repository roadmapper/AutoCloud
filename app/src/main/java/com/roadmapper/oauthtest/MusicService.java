package com.roadmapper.oauthtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.util.Util;
import com.google.gson.Gson;
import com.roadmapper.oauthtest.entities.StreamUrl;
import com.roadmapper.oauthtest.entities.TrackUrn;
import com.roadmapper.oauthtest.entities.Urls;
import com.roadmapper.oauthtest.entities.UserProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

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
    private PackageValidator mPackageValidator;

    @Override
    public void onCreate() {
        super.onCreate();

        mPackageValidator = new PackageValidator(this);

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
        Util.startForegroundService(this, new Intent(this, MusicService.class));
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
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName +
                "; clientUid=" + clientUid + " ; rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            Log.w(TAG, "OnGetRoot: IGNORING request from untrusted package "
                    + clientPackageName);
            return null;
        }

        return new BrowserRoot("ROOT", null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaItem>> result) {
        /*Log.d(TAG, "OnLoadChildren: parentMediaId=" + parentMediaId);
        if (mMusicProvider.isInitialized()) {
            // if music library is ready, return immediately
            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
        } else {
            // otherwise, only return results when the music library is retrieved
            result.detach();
            mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                }
            });
        }*/
        Log.d(TAG, "onLoadChildren");
        Log.d(TAG, "mediaId: " + parentMediaId);

        if ("ROOT".equals(parentMediaId)) {
            Drawable vectorDrawable = null;

            int currentNightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {

                // Night mode is not active, we're in day time
                vectorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_heart);
            }
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                // Night mode is active, we're at night!
                vectorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_heart_night);
            }
            if (currentNightMode == Configuration.UI_MODE_NIGHT_UNDEFINED) {
                // We don't know what mode we're in, assume notnight
                vectorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_heart);
            }

            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);

            // build the media items at the top level and then put in the result list
            MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                    .setMediaId("LIKES")
                    .setTitle("Likes")
                    .setIconBitmap(bitmap)
                    /*.setIconUri(Uri.parse("android.resource://" +
                            "com.roadmapper.oauthtest/drawable/ic_heart"))*/
                    .build();
            MediaItem item = new MediaBrowserCompat.MediaItem(description,
                    MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);

            // build the media items at the top level and then put in the result list
            MediaDescriptionCompat description2 = new MediaDescriptionCompat.Builder()
                    .setMediaId("STREAM")
                    .setTitle("Stream")
                    //.setIconBitmap(bitmap)
                    /*.setIconUri(Uri.parse("android.resource://" +
                            "com.roadmapper.oauthtest/drawable/ic_heart"))*/
                    .build();
            MediaItem item2 = new MediaBrowserCompat.MediaItem(description2,
                    MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);

            List<MediaItem> arr = new ArrayList<>();
            arr.add(item);
            arr.add(item2);
            result.sendResult(arr);
        } else {
            // examine the passed in parentMediaId to see which submenu we are at
            Log.d(TAG, "parent: " + parentMediaId);


            if ("STREAM".equals(parentMediaId)) {
                if (MusicLibrary.isInitialized()) {
                    // if music library is ready, return immediately
                    result.sendResult(MusicLibrary.getMediaItems(parentMediaId));
                } else {
                    // otherwise, only return results when the music library is retrieved
                    result.detach();
                    MusicLibrary.retrieveMediaAsync(new MusicLibrary.Callback() {
                        @Override
                        public void onMusicCatalogReady(boolean success) {
                            result.sendResult(MusicLibrary.getMediaItems(parentMediaId));
                        }
                    });
                }
            } else if ("LIKES".equals(parentMediaId)) {
                if (MusicLibrary.isInitialized()) {
                    // if music library is ready, return immediately
                    result.sendResult(MusicLibrary.getMediaItems(parentMediaId));
                } else {
                    // otherwise, only return results when the music library is retrieved
                    result.detach();
                    MusicLibrary.retrieveMediaAsync(new MusicLibrary.Callback() {
                        @Override
                        public void onMusicCatalogReady(boolean success) {
                            result.sendResult(MusicLibrary.getMediaItems(parentMediaId));
                        }
                    });
                }
            }

            //result.sendResult(MusicLibrary.getMediaItems());

        }


    }

    private Callback<ResponseBody> streamUrlCallback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            List<String> url = call.request().url().pathSegments();
            String trackId = url.get(url.indexOf("tracks") + 1);
            String streamUrl;
            okhttp3.Headers headers = response.headers();
            streamUrl = headers.get("Location");
            Log.d(TAG, "HEADERS: " + headers);
            Log.d(TAG, "url: " + streamUrl);

            if (!TextUtils.isEmpty(streamUrl)) {
                MediaMetadataCompat metadata = MusicLibrary.updateMusicUri(trackId, streamUrl);
                setSessionMetadataAndPlay(metadata);
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.d("MusicService", "Failure");
            Log.d("MusicService", t.getMessage());
        }
    };

    private void setSessionMetadataAndPlay(MediaMetadataCompat metadata) {
        String currentId = mPlayback.getCurrentMediaId();
        String metadataId = metadata.getDescription().getMediaId();

        mSession.setActive(true);
        mSession.setMetadata(metadata);
        mPlayback.play(metadata);

        if (!metadataId.equals(currentId)) {
            TrackUrn urn = new TrackUrn();
            urn.track_urn = "soundcloud:tracks:" + metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            SoundCloud2Client client2 = ServiceGenerator.createService(SoundCloud2Client.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
            Call<ResponseBody> update = client2.updatePlayHistory(AutoCloudApplication.CLIENT_ID_WEB, urn);
            update.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d(TAG, "updated play history: " + response.toString());
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d(TAG, "failed to update play history", t);
                }
            });
        }
    }

//    private okhttp3.Callback streamUrlCallback2 = new Callback<Urls>() {
//        @Override
//        public void onResponse(Call<Urls> call, Response<Urls> response) {
//            Urls urls = response.body();
//            List<String> url = call.request().url().pathSegments();
//            String trackId = url.get(url.indexOf("tracks") + 1);
//            String streamUrl = null;
//            if (urls == null) {
//                mSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(
//                        PlaybackStateCompat.STATE_ERROR, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0).build());
//            } else {
////                if (urls.hls_url != null) {
////                    streamUrl = urls.hls_url;
////                } else {
//                    streamUrl = urls.http_url;
//                //}
//                Log.d(TAG, "url: " + streamUrl);
//
//
//                if (!TextUtils.isEmpty(streamUrl)) {
//                    MediaMetadataCompat metadata = MusicLibrary.updateMusicUri(trackId, streamUrl);
//                    setSessionMetadataAndPlay(metadata);
//                }
//            }
//        }
//
//        @Override
//        public void onFailure(Call<Urls> call, Throwable t) {
//            Log.d("MusicService", "Failure");
//            Log.d("MusicService", t.getMessage());
//        }
//    };

    final MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            if (mPlayback.getCurrentMediaId() != null)
                onPlayFromMediaId(mPlayback.getCurrentMediaId(), null);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            /*mSession.setActive(true);*/
            mSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_CONNECTING, 0, 0).build());
            MediaMetadataCompat metadata = MusicLibrary.getMetadata(mediaId);
            // Don't want to always call the API to get the URL if we have cached it
            if (metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI) != null) {
                setSessionMetadataAndPlay(metadata);
            } else {
                //SoundCloud2Client client = ServiceGenerator.createService(SoundCloud2Client.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);

                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                httpClient.addInterceptor(chain -> {
                    Request original = chain.request();
                    if (original.url().queryParameter("oauth_token") == null) {
                        HttpUrl url = original.url().newBuilder().addQueryParameter("oauth_token", BuildConfig.token).build();
                        Log.d("MusicService", url.toString());
                        Request request = original.newBuilder().url(url).build();
                        return chain.proceed(request);
                    }
                    else
                        return chain.proceed(original);
                });
                OkHttpClient client = httpClient.build();

                Request request = new Request.Builder()
                        .url(metadata.getString("android.media.metadata.STREAM_URL"))
                        .get()
                        .build();
                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        Log.d("MusicService", "Failure");
                        Log.d("MusicService", e.getMessage());
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Gson gson = new Gson();
                            ResponseBody responseBody = response.body();
                            StreamUrl url = gson.fromJson(responseBody.string(), StreamUrl.class);

                            if (url == null || url.url.isEmpty()) {
                                mSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(
                                        PlaybackStateCompat.STATE_ERROR, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0).build());
                            } else {
                                String streamUrl = url.url;
                                Log.d(TAG, "stream url: " + streamUrl);

                                if (!TextUtils.isEmpty(url.url)) {
                                    MediaMetadataCompat metadata2 = MusicLibrary.updateMusicUri(mediaId, streamUrl);
                                    setSessionMetadataAndPlay(metadata2);
                                }
                            }
                        }
                    }
                });

                //call2.enqueue(streamUrlCallback2);
            }



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

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            switch (action) {
                case PlaybackManager.CUSTOM_ACTION_SKIP_FORWARD:
                    //LogHelper.i(TAG, "onCustomAction: favorite for current track");
            /*MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
            if (currentMusic != null) {
                String mediaId = currentMusic.getDescription().getMediaId();
                if (mediaId != null) {
                    String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
                    mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId));
                }
            }*/
                    // playback state needs to be updated because the "Favorite" icon on the
                    // custom action will change to reflect the new favorite state.
                    //updatePlaybackState(null);
                    mPlayback.skip(30 * 1000);
                    break;
                case PlaybackManager.CUSTOM_ACTION_LIKE:
                    final String currentMusicId = mPlayback.getCurrentMedia().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    if (currentMusicId != null) {
                        SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
                        Call<UserProfile> call = client.getMe();
                        call.enqueue(new Callback<UserProfile>() {
                            @Override
                            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                                SoundCloud2Client client2 = ServiceGenerator.createService(SoundCloud2Client.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
                                Call<ResponseBody> call2 = client2.likeTrack(response.body().id.toString(), currentMusicId, AutoCloudApplication.CLIENT_ID_WEB, "");
                                call2.enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call3, Response<ResponseBody> response) {
                                        Log.d("MusicService", call3.request().url().toString());
                                        Log.d("MusicService", response.message());
                                        MusicLibrary.updateMusicRating(currentMusicId, RatingCompat.newHeartRating(true));
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                                    }
                                });
                            }

                            @Override
                            public void onFailure(Call<UserProfile> call, Throwable t) {

                            }
                        });
                        //}
                    }
                    break;
                case PlaybackManager.CUSTOM_ACTION_UNLIKE:
                    final String currentMusicId2 = mPlayback.getCurrentMedia().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    if (currentMusicId2 != null) {
                        SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
                        Call<UserProfile> call = client.getMe();
                        call.enqueue(new Callback<UserProfile>() {
                            @Override
                            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                                SoundCloud2Client client2 = ServiceGenerator.createService(SoundCloud2Client.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
                                Call<ResponseBody> call2 = client2.unlikeTrack(response.body().id.toString(), currentMusicId2, AutoCloudApplication.CLIENT_ID_WEB);
                                call2.enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call3, Response<ResponseBody> response) {
                                        Log.d("MusicService", call3.request().url().toString());
                                        Log.d("MusicService", response.message());
                                        MusicLibrary.updateMusicRating(currentMusicId2, RatingCompat.newHeartRating(false));
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                                    }
                                });
                            }

                            @Override
                            public void onFailure(Call<UserProfile> call, Throwable t) {

                            }
                        });
                        //}
                    }
                    break;
                default:
                    //LogHelper.e(TAG, "Unsupported action: ", action);
                    break;
            }
        }

        /*@Override
        public void onPlayFromSearch(String query, Bundle extras) {
            boolean isArtistFocus = false;
            boolean isAlbumFocus = false;

            if (TextUtils.isEmpty(query)) {
                // The user provided generic string e.g. 'Play music'
                // Build appropriate playlist queue
            } else {
                // Build a queue based on songs that match "query" or "extras" param
                String mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS);
                if (TextUtils.equals(mediaFocus,
                        MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)) {
                    isArtistFocus = true;
                    artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST);
                } else if (TextUtils.equals(mediaFocus,
                        MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)) {
                    isAlbumFocus = true;
                    album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM);
                }
                else {}

                // Implement additional "extras" param filtering
            }

            // Implement your logic to retrieve the queue
            if (isArtistFocus) {
                result = searchMusicByArtist(artist);
            } else if (isAlbumFocus) {
                result = searchMusicByAlbum(album);
            } else {
                // No focus found, search by query for song title
                //result = searchMusicBySongTitle(query);
                SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
                Call<List<Track>> call = client.getTracks(query);
                call.enqueue(new Callback<List<Track>>() {

                    @Override
                    public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                        List<Track> tracks = response.body();
                        if (tracks != null) {
                            for (Track track : tracks) {
                                Log.d("MusicLibrary",
                                        track.user.username + " (" + track.title + ")");
                                createMediaMetadata(track);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Track>> call, Throwable t) {

                    }
                }
            }

            if (result != null && !result.isEmpty()) {
                // Immediately start playing from the beginning of the search results
                // Implement your logic to start playing music
                playMusic(result);
            } else {
                // Handle no queue found. Stop playing if the app
                // is currently playing a song
            }
        }*/
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
        if (metadata != null) {
            Log.d(TAG, "Metadata updated: " + metadata.toString());
            mSession.setMetadata(metadata);
        }
    }


}
