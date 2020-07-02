/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.roadmapper.oauthtest;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.AudioAttributesCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.roadmapper.oauthtest.entities.StreamUrl;
import com.roadmapper.oauthtest.entities.Urls;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static android.media.MediaPlayer.OnCompletionListener;

/**
 * Handles media playback using a {@link MediaPlayer}.
 */
class PlaybackManager implements AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = PlaybackManager.class.getSimpleName();
    private final Context mContext;

    @PlaybackStateCompat.State
    private int mState;
    private boolean mPlayOnFocusGain;
    private volatile MediaMetadataCompat mCurrentMedia;

    private MediaPlayer mMediaPlayer;

    private final Callback mCallback;
    private final AudioManager mAudioManager;

    // Action to thumbs up a media item
    protected static final String CUSTOM_ACTION_SKIP_FORWARD = "com.roadmapper.oauthtest.SKIP_FORWARD";
    protected static final String CUSTOM_ACTION_LIKE = "com.roadmapper.oauthtest.LIKE";
    protected static final String CUSTOM_ACTION_UNLIKE = "com.roadmapper.oauthtest.UNLIKE";

    public PlaybackManager(Context context, Callback callback) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mCallback = callback;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMedia;
    }

    public String getCurrentMediaId() {
        return mCurrentMedia == null ? null : mCurrentMedia.getDescription().getMediaId();
    }

    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    private MediaPlayer.OnPreparedListener mMediaPlayerOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            //mProgressBar.setVisibility(View.GONE);
            //mMediaRelativeLayout.setVisibility(View.VISIBLE);

            //mArtistTextView.setText(mArtist);
            //mTitleTextView.setText(mTitle);

            //Picasso.with(getApplicationContext())
            //.load(mCoverImage)
            //.into(mCoverImageImageView);

            //mPlayImageButton.setVisibility(View.GONE);
            //mPauseImageButton.setVisibility(View.VISIBLE);

            if (tryToGetAudioFocus()) {
                mPlayOnFocusGain = true;
                mp.start();
                mState = PlaybackStateCompat.STATE_PLAYING;
                updatePlaybackState();
            } else {
                mPlayOnFocusGain = false;
            }

            //start media player
            //mp.start();

            //update seekbar
            //mRunnable.run();
        }
    };

    public void play(MediaMetadataCompat metadata) {
        String mediaId = metadata.getDescription().getMediaId();
        boolean mediaChanged = (mCurrentMedia == null || !getCurrentMediaId().equals(mediaId));

        mState = PlaybackStateCompat.STATE_BUFFERING;
        updatePlaybackState();
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mMediaPlayer.setAudioAttributes(attr);
            mMediaPlayer.setWakeMode(mContext.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnCompletionListener(this);
        } else {
            if (mediaChanged) {
                mMediaPlayer.reset();
            }
        }

        if (mediaChanged) {
            mCurrentMedia = metadata;
            try {
                String path = mCurrentMedia.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
                Uri uri = Uri.parse(path);

                mMediaPlayer.setDataSource(mContext.getApplicationContext(),
                        uri);
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setOnPreparedListener(mMediaPlayerOnPreparedListener);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (tryToGetAudioFocus()) {
                mPlayOnFocusGain = true;
                mMediaPlayer.start();
                mState = PlaybackStateCompat.STATE_PLAYING;
                updatePlaybackState();
            } else {
                mPlayOnFocusGain = false;
            }
        }

        /*if (tryToGetAudioFocus()) {
            mPlayOnFocusGain = false;
            mMediaPlayer.start();
            mState = PlaybackState.STATE_PLAYING;
            updatePlaybackState();
        } else {
            mPlayOnFocusGain = true;
        }*/
    }

    public void pause() {
        if (isPlaying()) {
            mMediaPlayer.pause();
            mAudioManager.abandonAudioFocus(this);
        }
        mState = PlaybackStateCompat.STATE_PAUSED;
        updatePlaybackState();
    }

    public void stop() {
        mState = PlaybackStateCompat.STATE_STOPPED;
        updatePlaybackState();
        // Give up Audio focus
        mAudioManager.abandonAudioFocus(this);
        // Relax all resources
        releaseMediaPlayer();
    }

    public void skip(int durationMilli) {
        if (isPlaying()) {
            // TODO: Not sure if this is necessary
            //mState = PlaybackStateCompat.STATE_FAST_FORWARDING;
            //updatePlaybackState();
            Log.d("PlaybackManager", "Skip duration secs:" + durationMilli / 1000);
            Log.d("PlaybackManager", "current duration secs:" + mMediaPlayer.getCurrentPosition() / 1000);
            Log.d("PlaybackManager", "final duration secs:" + (mMediaPlayer.getCurrentPosition() + durationMilli) / 1000);
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + durationMilli);
            updatePlaybackState();
        }
    }

    /**
     * Try to get the system audio focus.
     */
    private boolean tryToGetAudioFocus() {
        int result = mAudioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        boolean gotFullFocus = false;
        boolean canDuck = false;

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Pause playback because your Audio Focus was
                // temporarily stolen, but will be back soon.
                // i.e. for a phone call
                canDuck = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Stop playback, because you lost the Audio Focus.
                // i.e. the user started some other playback app
                // Remember to unregister your controls/buttons here.
                // And release the kra — Audio Focus!
                // You’re done.
                mAudioManager.abandonAudioFocus(this);
                canDuck = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lower the volume, because something else is also
                // playing audio over you.
                // i.e. for notifications or navigation directions
                // Depending on your audio playback, you may prefer to
                // pause playback here instead. You do you.

                // We have lost focus. If we can duck (low playback volume), we can keep playing.
                // Otherwise, we need to pause the playback.
                canDuck = true;
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // Resume playback, because you hold the Audio Focus
                // again!
                // i.e. the phone call ended or the nav directions
                // are finished
                // If you implement ducking and lower the volume, be
                // sure to return it to normal here, as well.
                gotFullFocus = true;
                break;
        }

        if (gotFullFocus || canDuck) {
            if (mMediaPlayer != null) {
                if (mPlayOnFocusGain) {
                    //mPlayOnFocusGain = false;
                    mMediaPlayer.start();
                    mState = PlaybackStateCompat.STATE_PLAYING;
                    updatePlaybackState();
                }
                float volume = canDuck ? 0.2f : 1.0f;
                mMediaPlayer.setVolume(volume, volume);
            }
        } else if (mState == PlaybackStateCompat.STATE_PLAYING) {
            mMediaPlayer.pause();
            mState = PlaybackStateCompat.STATE_PAUSED;
            updatePlaybackState();
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        //stop();
        //player.reset();
        String mediaId = MusicLibrary.getNextSong(mCurrentMedia.getDescription().getMediaId());
        MediaMetadataCompat nextSong = MusicLibrary.getMetadata(mediaId);
        if (nextSong.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI) != null) {
            play(nextSong);
        } else {
//            SoundCloud2Client client = ServiceGenerator.createService(SoundCloud2Client.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
//            //Call<ResponseBody> call = client.getMediaStream(Long.parseLong(MusicLibrary.getNextSong(mCurrentMedia.getDescription().getMediaId())));
//            //call.enqueue(streamUrlCallback);
//            Call<Urls> call2 = client.getMediaStreams(Long.parseLong(mediaId));
//            call2.enqueue(streamUrlCallback2);
            String nextMediaId = MusicLibrary.getNextSong(mCurrentMedia.getDescription().getMediaId());
            MediaMetadataCompat metadata = MusicLibrary.getMetadata(nextMediaId);

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
            mState = PlaybackStateCompat.STATE_BUFFERING;
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
                            mState = PlaybackStateCompat.STATE_ERROR;
                            updatePlaybackState();
                        } else {
                            String streamUrl = url.url;
                            Log.d(TAG, "stream url: " + streamUrl);

                            if (!TextUtils.isEmpty(streamUrl)) {
                                MediaMetadataCompat metadata = MusicLibrary.updateMusicUri(nextMediaId, streamUrl);
                                play(metadata);
                            }
                        }
                    }
                }
            });
        }
    }

    private retrofit2.Callback<ResponseBody> streamUrlCallback = new retrofit2.Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            List<String> url = call.request().url().pathSegments();
            String trackId = url.get(url.indexOf("tracks") + 1);
            String streamUrl;
            okhttp3.Headers headers = response.headers();
            streamUrl = headers.get("Location");
            Log.d(TAG, streamUrl);

            if (!TextUtils.isEmpty(streamUrl)) {
                MediaMetadataCompat metadata = MusicLibrary.updateMusicUri(trackId, streamUrl);
                play(metadata);
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.d(TAG, "Failure");
            Log.d(TAG, t.getMessage());
        }
    };

    private retrofit2.Callback<Urls> streamUrlCallback2 = new retrofit2.Callback<Urls>() {
        @Override
        public void onResponse(Call<Urls> call, Response<Urls> response) {
            Urls urls = response.body();
            List<String> url = call.request().url().pathSegments();
            String trackId = url.get(url.indexOf("tracks") + 1);
            String streamUrl;
            if (urls == null) {
                mState = PlaybackStateCompat.STATE_ERROR;
                updatePlaybackState();
            } else {
                //if (urls.hls_url != null) {
                //    streamUrl = urls.hls_url;
                //} else {
                    streamUrl = urls.http_url;
                //}
                Log.d(TAG, "URL: " + streamUrl);

                if (!TextUtils.isEmpty(streamUrl)) {
                    MediaMetadataCompat metadata = MusicLibrary.updateMusicUri(trackId, streamUrl);
                    play(metadata);
                }
            }
        }

        @Override
        public void onFailure(Call<Urls> call, Throwable t) {
            Log.d("MusicService", "Failure");
            Log.d("MusicService", t.getMessage());
        }
    };

    /**
     * Releases resources used by the service for playback.
     */
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        if (isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    private void updatePlaybackState() {
        if (mCallback == null) {
            return;
        }

        Log.d(TAG, "updatePlaybackState, playback state=" + mState);
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {//.isConnected()) {
            position = getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        setCustomAction(stateBuilder);
        //int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        //if (error != null) {
        // Error states are really only supposed to be used for errors that cause playback to
        // stop unexpectedly and persist until the user takes action to fix it.
        //stateBuilder.setErrorMessage(error);
        //state = PlaybackStateCompat.STATE_ERROR;
        //}
        //noinspection ResourceType
        stateBuilder.setState(mState, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        /*MediaSession.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }*/

        mCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (mState == PlaybackStateCompat.STATE_PLAYING ||
                mState == PlaybackStateCompat.STATE_PAUSED) {
            mCallback.onNotificationRequired();
            mCallback.onMetadataUpdated(mCurrentMedia);
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     *                  message will be set in the PlaybackState and will be visible to
     *                  MediaController clients.
     */
    public void handleStopRequest(String withError) {
        Log.d(TAG, "handleStopRequest: mState=" + mState + " error=" + withError);
        stop();
        //mCallback.onPlaybackStop();
        updatePlaybackState();
    }

    private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
        //MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        //if (currentMusic == null) {
        //    return;
        //}
        // Set appropriate "Favorite" icon on Custom action:
        /*String mediaId = currentMusic.getDescription().getMediaId();
        if (mediaId == null) {
            return;
        }*/
        //String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
        int skipIcon = R.drawable.ic_forward_30_black_48dp;
        /*LogHelper.d(TAG, "updatePlaybackState, setting Favorite custom action of music ",
                musicId, " current favorite=", mMusicProvider.isFavorite(musicId));*/
        Bundle customActionExtras = new Bundle();
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_SKIP_FORWARD, "Forward 30", skipIcon)
                .setExtras(customActionExtras)
                .build());

        int likeIcon = R.drawable.ic_heart;
        int likeIcon2 = R.drawable.ic_radio_button_unchecked_black;

        if (mCurrentMedia != null) {


            if (mCurrentMedia.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING).hasHeart()) {
                stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                        CUSTOM_ACTION_LIKE, "Unlike", likeIcon2).setExtras(customActionExtras).build());
            } else {
                stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                        CUSTOM_ACTION_LIKE, "Like", likeIcon).setExtras(customActionExtras).build());
            }
        }
    }

    public interface Callback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);

        void onMetadataUpdated(MediaMetadataCompat metadata);


    }

}