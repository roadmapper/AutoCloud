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
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.List;

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
                mPlayOnFocusGain = false;
                mp.start();
                mState = PlaybackStateCompat.STATE_PLAYING;
                updatePlaybackState();
            } else {
                mPlayOnFocusGain = true;
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
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
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
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(mContext.getApplicationContext(),
                        Uri.parse(MusicLibrary.getSongUri(mediaId)));
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setOnPreparedListener(mMediaPlayerOnPreparedListener);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (tryToGetAudioFocus()) {
                mPlayOnFocusGain = false;
                mMediaPlayer.start();
                mState = PlaybackStateCompat.STATE_PLAYING;
                updatePlaybackState();
            } else {
                mPlayOnFocusGain = true;
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
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            gotFullFocus = true;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
        }

        if (gotFullFocus || canDuck) {
            if (mMediaPlayer != null) {
                if (mPlayOnFocusGain) {
                    mPlayOnFocusGain = false;
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

        SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
        Call<ResponseBody> call = client.getStreamInfo(Long.parseLong(MusicLibrary.getNextSong(mCurrentMedia.getDescription().getMediaId())));
        call.enqueue(streamUrlCallback);
    }

    private retrofit2.Callback<ResponseBody> streamUrlCallback = new retrofit2.Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            List<String> url = call.request().url().pathSegments();
            String trackId = url.get(url.indexOf("tracks") + 1);
            String streamUrl = null;
            okhttp3.Headers headers = response.headers();
            streamUrl = headers.get("Location");

            if (!TextUtils.isEmpty(streamUrl)) {
                MusicLibrary.setSongStreamUri(trackId, streamUrl);
                MediaMetadataCompat metadata = MusicLibrary.getMetadata(trackId);
                play(metadata);
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.d("MainActivity", "Failure");
            Log.d("MainActivity", t.getMessage());
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
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT  | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
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
        //if (mPlayback != null && mPlayback.isConnected()) {
            position = getCurrentStreamPosition();
        //}

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        //setCustomAction(stateBuilder);
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

    public interface Callback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);

        void onMetadataUpdated(MediaMetadataCompat metadata);
    }

}