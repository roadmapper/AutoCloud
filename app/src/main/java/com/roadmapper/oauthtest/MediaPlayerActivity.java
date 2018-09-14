package com.roadmapper.oauthtest;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity that will play a stream URL from a given stream.
 */

// TODO: This activity currently doesn't handle state change correctly
// TODO: and will play music on top of each other. This needs to change to a music service.
public class MediaPlayerActivity extends AppCompatActivity {

    @BindView(R.id.pause)
    ImageButton mPauseImageButton;
    @BindView(R.id.play)
    ImageButton mPlayImageButton;
    @BindView(R.id.artist_tv)
    TextView mArtistTextView;
    @BindView(R.id.title_tv)
    TextView mTitleTextView;
    @BindView(R.id.cover_image_iv)
    ImageView mCoverImageImageView;
    @BindView(R.id.sb)
    SeekBar mSeekBar;
    @BindView(R.id.total_time_tv)
    TextView mTotalTimeTextView;
    @BindView(R.id.current_time_tv)
    TextView mCurrentTimeTextView;
    @BindView(R.id.media_rl)
    RelativeLayout mMediaRelativeLayout;
    @BindView(R.id.pb)
    ProgressBar mProgressBar;

    private MediaPlayer mMediaPlayer;
    private String mArtist = "";
    private String mTitle = "";
    private String mCoverImage = "";
    private AccessToken token = null;


    private MediaBrowser mMediaBrowser;
    public final MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mSubscriptionCallback);
                    MediaController mediaController = new MediaController(
                            AutoCloudApplication.getAppContext(), mMediaBrowser.getSessionToken());
                    updatePlaybackState(mediaController.getPlaybackState());
                    updateMetadata(mediaController.getMetadata());
                    mediaController.registerCallback(mMediaControllerCallback);
                    setMediaController(mediaController);
                }
            };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateMetadata(metadata);
            //mBrowserAdapter.notifyDataSetChanged();
        }
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updatePlaybackState(state);
            //mBrowserAdapter.notifyDataSetChanged();
        }
        @Override
        public void onSessionDestroyed() {
            updatePlaybackState(null);
            //mBrowserAdapter.notifyDataSetChanged();
        }
    };
    private final MediaBrowser.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                    onMediaLoaded(children);
                }
            };
    private MediaMetadata mCurrentMetadata;
    private PlaybackState mCurrentState;

    private void onMediaLoaded(List<MediaBrowser.MediaItem> media) {
        /*mBrowserAdapter.clear();
        mBrowserAdapter.addAll(media);
        mBrowserAdapter.notifyDataSetChanged();*/
    }

    private void onMediaItemSelected(MediaBrowser.MediaItem item) {
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
        }
    }

    private static Handler mHandler = new Handler();

    private MediaPlayer.OnPreparedListener mMediaPlayerOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mProgressBar.setVisibility(View.GONE);
            mMediaRelativeLayout.setVisibility(View.VISIBLE);

            mArtistTextView.setText(mArtist);
            mTitleTextView.setText(mTitle);

            Picasso.get()
                    .load(mCoverImage)
                    .into(mCoverImageImageView);

            mPlayImageButton.setVisibility(View.GONE);
            mPauseImageButton.setVisibility(View.VISIBLE);

            //start media player
            mp.start();

            //update seekbar
            mRunnable.run();
        }
    };

    private final Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mMediaPlayer != null) {

                //set max value
                int mDuration = mMediaPlayer.getDuration();
                mSeekBar.setMax(mDuration);

                //update total time text view
                mTotalTimeTextView.setText(getTimeString(mDuration));

                //set progress to current position
                int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);

                //update current time text view
                mCurrentTimeTextView.setText(getTimeString(mCurrentPosition));

                //handle drag on seekbar
                mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

                if (mCurrentPosition == mDuration) {
                    mPauseImageButton.setVisibility(View.GONE);
                    mPlayImageButton.setVisibility(View.VISIBLE);
                }
            }

            //repeat above code every second
            mHandler.postDelayed(this, 10);
        }
    };
    // endregion

    // region Listeners
    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mMediaPlayer != null && fromUser) {
                mMediaPlayer.seekTo(progress);
            }
        }
    };

    /*private Callback<ResponseBody> callback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            if (!isFinishing()) {

                String url = null;
                okhttp3.Headers headers = response.headers();
                url = headers.get("Location");

                String audioFile = url;

                if (!TextUtils.isEmpty(audioFile)) {

                    // create a media player
                    mMediaPlayer = new MediaPlayer();

                    // try to load data and play
                    try {
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        // give data to mMediaPlayer
                        mMediaPlayer.setDataSource(audioFile);
                        // media player asynchronous preparation
                        mMediaPlayer.prepareAsync();

                        // execute this code at the end of asynchronous media player preparation
                        mMediaPlayer.setOnPreparedListener(mMediaPlayerOnPreparedListener);
                    } catch (IOException e) {
                        finish();
//                        Toast.makeText(MediaPlayerActivity.this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mProgressBar.setVisibility(View.GONE);
                    mMediaRelativeLayout.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {

        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title and go full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setTitle(getString(R.string.app_name));
        setContentView(R.layout.activity_media_player);
        ButterKnife.bind(this);


        /*String streamUrl = "";

        // get data from main activity intent
        Intent intent = getIntent();
        if (intent != null) {
            streamUrl = intent.getStringExtra("streamUrl");
            mArtist = intent.getStringExtra("artist");
            mTitle = intent.getStringExtra("title");
            mCoverImage = intent.getStringExtra("artworkUrl");
        }

        mCoverImage = mCoverImage.replace("-large.jpg", "-crop.jpg");

        Uri streamUri = Uri.parse(streamUrl);
        long trackId = Long.valueOf(streamUri.getPathSegments().get(1));

        //Api.getService(Api.getEndpointUrl()).getMediaStream(trackId, mGetStreamInfoCallback);

        String code = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_token);
        String scope = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_scope);
        if (!"".equals(code) && !"".equals(scope)) {
            token = new AccessToken(code, scope);
        }


        SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class, token);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
        Call<ResponseBody> call = client.getMediaStream(trackId);
        call.enqueue(callback);*/

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class), mConnectionCallback, null);
        mMediaBrowser.connect();





    }

    @Override
    public void onStart() {
        super.onStart();
        /*mMediaBrowser = new MediaBrowser(this,
               new ComponentName(this, MusicService.class), mConnectionCallback, null);
        mMediaBrowser.connect();*/


        /*Long mediaId;
        Intent intent = getIntent();
        if (intent != null) {
            mediaId = intent.getLongExtra("mediaId", -1);
            if (mediaId != -1) {
                MediaBrowser.MediaItem item = MusicLibrary.getMediaItem(mediaId.toString());
                onMediaItemSelected(item);
            }
        }*/
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            getMediaController().unregisterCallback(mMediaControllerCallback);
            mMediaBrowser.unsubscribe(mMediaBrowser.getRoot());
        } finally {
            mMediaBrowser.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
    }
    // endregion

    private void updatePlaybackState(PlaybackState state) {
        mCurrentState = state;
        if (state == null || state.getState() == PlaybackState.STATE_PAUSED ||
                state.getState() == PlaybackState.STATE_STOPPED) {
            mPlayImageButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
        } else {
            mPlayImageButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause));
        }
        //mPlaybackControls.setVisibility(state == null ? View.GONE : View.VISIBLE);
    }

    private void updateMetadata(MediaMetadata metadata) {
        mCurrentMetadata = metadata;
        mTitleTextView.setText(metadata == null ? "" : metadata.getDescription().getTitle());
        //mSubtitle.setText(metadata == null ? "" : metadata.getDescription().getSubtitle());
        Bitmap artwork = null;
        /*try {
            artwork = metadata == null ? null : MusicLibrary.getAlbumBitmap(this,
                    metadata.getDescription().getMediaId());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        mCoverImageImageView.setImageBitmap(artwork);
    }

    // region Helper Methods
    public void play(View view) {
        mPlayImageButton.setVisibility(View.GONE);
        mPauseImageButton.setVisibility(View.VISIBLE);
        mMediaPlayer.start();
    }

    public void pause(View view) {
        mPlayImageButton.setVisibility(View.VISIBLE);
        mPauseImageButton.setVisibility(View.GONE);
        mMediaPlayer.pause();
    }

    public void stop(View view) {
        mMediaPlayer.seekTo(0);
        mMediaPlayer.pause();
    }

    public void seekForward(View view) {
        //set seek time
        int seekForwardTime = 5000;

        // get current song position
        int currentPosition = mMediaPlayer.getCurrentPosition();
        // check if seekForward time is lesser than song duration
        if (currentPosition + seekForwardTime <= mMediaPlayer.getDuration()) {
            // forward song
            mMediaPlayer.seekTo(currentPosition + seekForwardTime);
        } else {
            // forward to end position
            mMediaPlayer.seekTo(mMediaPlayer.getDuration());
        }
    }

    public void seekBackward(View view) {
        //set seek time
        int seekBackwardTime = 5000;

        // get current song position
        int currentPosition = mMediaPlayer.getCurrentPosition();
        // check if seekBackward time is greater than 0 sec
        if (currentPosition - seekBackwardTime >= 0) {
            // forward song
            mMediaPlayer.seekTo(currentPosition - seekBackwardTime);
        } else {
            // backward to starting position
            mMediaPlayer.seekTo(0);
        }
    }

    public void onBackPressed() {
        super.onBackPressed();

        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        finish();
    }

    private String getTimeString(long millis) {
        StringBuilder builder = new StringBuilder();

        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = ((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        if (hours > 0L) {
            builder.append(String.format(Locale.US, "%02d", hours))
                    .append(":");
        }

        if (minutes > 9) {
            builder.append(String.format(Locale.US, "%02d", minutes))
                    .append(":");
        } else {
            builder.append(String.format(Locale.US, "%01d", minutes))
                    .append(":");
        }

        builder.append(String.format(Locale.US, "%02d", seconds));

        return builder.toString();
    }
}
