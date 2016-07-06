package com.roadmapper.oauthtest;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.roadmapper.oauthtest.entities.Activity;
import com.roadmapper.oauthtest.entities.AffiliatedActivities;
import com.roadmapper.oauthtest.entities.Track;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // This is the redirect URI that is sent by the SoundCloud API, unique to the app
    private final String redirectUri = "echo://auth";

    public static AccessToken token = null;

    public static SoundCloudClient client;

    private MediaBrowserCompat mMediaBrowser;

    //@Bind(R.id.activityButton)
    //Button getActivityButton;
    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout swipeContainer;
    @BindView(R.id.play_pause)
    ImageButton mPlayPause;
    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.artist)
    TextView mSubtitle;
    @BindView(R.id.album_art)
    ImageView mAlbumArt;
    @BindView(R.id.playback_controls)
    ViewGroup mPlaybackControls;
    @BindView(R.id.sb)
    SeekBar seekBar;

    private MediaMetadataCompat mCurrentMetadata;
    private PlaybackStateCompat mCurrentState;


    @BindView(R.id.tracks)
    //ListView trackListView;
            RecyclerView trackListView;
    ArrayList<Track> trackList = new ArrayList<>();
    TrackRecyclerAdapter trackAdapter;

    private Callback<AffiliatedActivities> callback = new Callback<AffiliatedActivities>() {
        @Override
        public void onResponse(Call<AffiliatedActivities> call, Response<AffiliatedActivities> response) {
            Log.d("MainActivity", response.message());
            Log.d("MainActivity", response.code() + "");
            Log.d("MainActivity", response.headers().toString());
            List<Activity> activities = response.body().collection;
            if (activities != null) {
                trackList.clear();
                for (Activity activity : activities) {
                    Log.d("MainActivity",
                            activity.origin.user.username + " (" + activity.origin.title + ")");
                    trackList.add(activity.origin);
                    MusicLibrary.createMediaMetadata(activity.origin);
                }
                trackAdapter.notifyDataSetChanged();
            }

        }

        @Override
        public void onFailure(Call<AffiliatedActivities> call, Throwable t) {
            Log.d("MainActivity", "Failure");
            Log.d("MainActivity", t.getMessage());
        }
    };

    private Callback<List<Track>> callback2 = new Callback<List<Track>>() {
        @Override
        public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
            Log.d("MainActivity", response.message());
            Log.d("MainActivity", response.code() + "");
            Log.d("MainActivity", response.headers().toString());
            List<Track> tracks = response.body();
            if (tracks != null) {
                trackList.clear();
                trackList.addAll(tracks);
                for (Track track : tracks) {
                    Log.d("MainActivity",
                            track.user.username + " (" + track.title + ")");
                    //trackList.add(activity.origin);
                    MusicLibrary.createMediaMetadata(track);
                }
                trackAdapter.notifyDataSetChanged();

                // Remember to CLEAR OUT old items before appending in the new ones
                //adapter.clear();
                // ...the data has come back, add new items to your adapter...
                //adapter.addAll(...);
                // Now we call setRefreshing(false) to signal refresh has finished
                swipeContainer.setRefreshing(false);

            }
        }

        @Override
        public void onFailure(Call<List<Track>> call, Throwable t) {
            Log.d("MainActivity", "Failure");
            Log.d("MainActivity", t.getMessage());
        }
    };

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mSubscriptionCallback);
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(
                                MainActivity.this, mMediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    updatePlaybackState(mediaController.getPlaybackState());
                    updateMetadata(mediaController.getMetadata());
                    mediaController.registerCallback(mMediaControllerCallback);
                    setSupportMediaController(mediaController);
                }
            };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            updateMetadata(metadata);
            trackAdapter.notifyDataSetChanged();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updatePlaybackState(state);
            trackAdapter.notifyDataSetChanged();
        }

        @Override
        public void onSessionDestroyed() {
            updatePlaybackState(null);
            trackAdapter.notifyDataSetChanged();
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
                    onMediaLoaded(children);
                }
            };

    private void onMediaLoaded(List<MediaBrowserCompat.MediaItem> media) {
        //trackAdapter..clear();
        //trackAdapter.addAll(media);
        trackAdapter.notifyDataSetChanged();
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        mCurrentState = state;
        if (state == null || state.getState() == PlaybackStateCompat.STATE_PAUSED ||
                state.getState() == PlaybackStateCompat.STATE_STOPPED) {
            mPlayPause.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
        } else {
            mPlayPause.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause));

            //update seekbar
            mRunnable.run();

        }
        mPlaybackControls.setVisibility(state == null ? View.GONE : View.VISIBLE);
    }

    private final Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (getMediaController().getPlaybackState() != null || getMediaController().getPlaybackState().getState() == PlaybackState.ACTION_PLAY) {

                //set max value
                long mDuration = getMediaController().getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION);
                seekBar.setMax((int) mDuration);

                //update total time text view
                //mTotalTimeTextView.setText(getTimeString(mDuration));

                //set progress to current position
                long mCurrentPosition = getMediaController().getPlaybackState().getPosition();
                seekBar.setProgress((int) mCurrentPosition);

                //update current time text view
                //mCurrentTimeTextView.setText(getTimeString(mCurrentPosition));

                //handle drag on seekbar
                //mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

                /*if (mCurrentPosition == mDuration) {
                    mPauseImageButton.setVisibility(View.GONE);
                    mPlayImageButton.setVisibility(View.VISIBLE);
                }*/
            }

            //repeat above code every second
            mHandler.postDelayed(this, 10);
        }
    };
    private static Handler mHandler = new Handler();

    private void updateMetadata(final MediaMetadataCompat metadata) {
        mCurrentMetadata = metadata;
        final MediaMetadataCompat meta = metadata;
        mTitle.setText(metadata == null ? "" : metadata.getDescription().getTitle());
        mSubtitle.setText(metadata == null ? "" : metadata.getDescription().getSubtitle());

        if (metadata != null) {
            //MusicLibrary.getAlbumBitmap(this, metadata.getDescription().getMediaId(), target);
            String artUrl = null;
            //Log.d("MainActivity", metadata.toString());

            //Log.d("MainActivity", metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI));
            if (metadata.getDescription().getIconUri() != null) {
                //Log.d("MainActivity", metadata.getDescription().getIconUri().toString());
                artUrl = metadata.getDescription().getIconUri().toString();
            }
            if (!TextUtils.isEmpty(artUrl)) {
                //mArtUrl = artUrl;
                Bitmap art = null; //metadata.getDescription().getIconBitmap();
                AlbumArtCache cache = AlbumArtCache.getInstance();
                //if (art == null) {
                    art = cache.getBigImage(artUrl);
                //}
                if (art != null) {
                    mAlbumArt.setImageBitmap(art);
                } else {
                    cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                                @Override
                                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                                    if (icon != null) {
                                        Log.d("MainActivity", "album art icon of w=" + icon.getWidth() +
                                                " h=" + icon.getHeight());

                                            mAlbumArt.setImageBitmap(icon);
                                        //}
                                    }
                                    MusicLibrary.updateMusicArt(meta.getDescription().getMediaId(), bitmap, icon);
                                }
                            }
                    );
                }
            }
        }
        //try {

        /*} catch (IOException e) {
            mAlbumArt.setImageBitmap(null);
        }*/
        //trackAdapter.notifyDataSetChanged();
    }

    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mAlbumArt.setImageBitmap(bitmap);
            MusicLibrary.setAlbumResBitmap(mCurrentMetadata.getDescription().getMediaId(), bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            mAlbumArt.setImageBitmap(null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    public void onStart() {
        super.onStart();

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicService.class), mConnectionCallback, null);
        mMediaBrowser.connect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        addToken(getIntent());

        LinearLayoutManager llm = new LinearLayoutManager(this);
        trackListView.setLayoutManager(llm);
        //trackAdapter = new TrackAdapter(this, trackList);

        trackAdapter = new TrackRecyclerAdapter(this, trackList);
        trackListView.setAdapter(trackAdapter);


        /*trackListView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        }){
            @Override
            public void onItemClick(View v) {
                Track track = trackList.get(position);

                *//**//*Intent intent = new Intent(AutoCloudApplication.getAppContext(), MediaPlayerActivity.class);
                intent.putExtra("streamUrl", track.streamUrl);
                intent.putExtra("artist", track.user.fullName);
                intent.putExtra("title", track.title);
                intent.putExtra("artworkUrl", track.artworkUrl);
                startActivity(intent);*//**//*

                Intent intent = new Intent(AutoCloudApplication.getAppContext(), MediaPlayerActivity.class);
                intent.putExtra("mediaId", track.id);
                startActivity(intent);

                return true;
            }
        });
*/

        String scope = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_scope);
        token = new AccessToken(AutoCloudApplication.OAUTH_TOKEN_WEB, scope);
        client = ServiceGenerator.createService(SoundCloudClient.class, token);
        //if (getActivityButton != null) {
            //getActivityButton.setOnClickListener(new View.OnClickListener() {
                //@Override
                //public void onClick(View v) {
                    Log.d("MainActivity", "getActivity");
                    /*Call<AffiliatedActivities> call = client.getStreamTracks(50);
                    call.enqueue(callback);*/

                    Call<List<Track>> call = client.getMyFavorites(100);
                    call.enqueue(callback2);
                //}
            //});
        //}

        trackAdapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View v, int adapterPosition) {
                    MediaBrowserCompat.MediaItem item = MusicLibrary.getMediaItem(trackList.get(adapterPosition).id.toString());
                    onMediaItemSelected(item);
                }
            });

        mPlayPause.setOnClickListener(mPlaybackButtonListener);

        /*LinearLayout firstItem = (LinearLayout) findViewById(R.id.firstItem);
        if (firstItem != null) {
            firstItem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent intent = new Intent(AutoCloudApplication.getAppContext(), MediaPlayerActivity.class);
                    intent.putExtra("streamUrl", firstTrack.streamUrl);
                    intent.putExtra("artist", firstTrack.user.fullName);
                    intent.putExtra("title", firstTrack.title);
                    intent.putExtra("artworkUrl", firstTrack.artworkUrl);
                    startActivity(intent);
                    return true;
                }
            });
        }*/

        /*// Create a very simple REST adapter which points the SoundCloud API endpoint.
        SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class);

        // Fetch and print a list of the contributors to this library.
        Call<List<Track>> call =
                client.getFavoriteTracks("vinay-dandekar");

        List<Track> tracks = null;
        call.enqueue(callback);*/

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                //fetchTimelineAsync(0);

                Log.d("MainActivity", "getActivity");
                    /*Call<AffiliatedActivities> call = client.getStreamTracks(50);
                    call.enqueue(callback);*/

                Call<List<Track>> call = client.getMyFavorites(100);
                call.enqueue(callback2);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

    }

    private final View.OnClickListener mPlaybackButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int state = mCurrentState == null ?
                    PlaybackState.STATE_NONE : mCurrentState.getState();
            if (state == PlaybackState.STATE_PAUSED ||
                    state == PlaybackState.STATE_STOPPED ||
                    state == PlaybackState.STATE_NONE) {

                if (mCurrentMetadata == null) {
                    mCurrentMetadata = MusicLibrary.getMetadata(
                            MusicLibrary.getMediaItems().get(0).getMediaId());
                    updateMetadata(mCurrentMetadata);
                }
                getMediaController().getTransportControls().playFromMediaId(
                        mCurrentMetadata.getDescription().getMediaId(), null);

            } else {
                getMediaController().getTransportControls().pause();
            }
        }
    };

    private void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        addToken(getIntent());
    }

    private void addToken(Intent intent) {
        // the intent filter defined in AndroidManifest will handle the return from ACTION_VIEW intent
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith(redirectUri)) {
            Log.d("MainActivity", uri.toString());
            // use the parameter your API exposes for the code (mostly it's "code")
            //String code = uri.getQueryParameter("code");
            uri = Uri.parse(uri.toString().replace("#", ""));
            String atoken = uri.getQueryParameter("access_token");
            String scope = uri.getQueryParameter("scope");

            if (atoken != null) {

                token = new AccessToken(AutoCloudApplication.OAUTH_TOKEN_WEB, scope);
                client = ServiceGenerator.createService(SoundCloudClient.class, token);//, AutoCloudApplication.CLIENT_ID, AutoCloudApplication.CLIENT_SECRET);
                //SharedPrefManager.getInstance().writeSharedPrefString(R.string.oauth_token, atoken);
                SharedPrefManager.getInstance().writeSharedPrefString(R.string.oauth_scope, scope);


            } else if (uri.getQueryParameter("error") != null) {
                Log.d("MainActivity", uri.toString());
            }
        }
    }
}
