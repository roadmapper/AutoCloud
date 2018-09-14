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
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.util.Log;

import com.roadmapper.oauthtest.entities.Activity;
import com.roadmapper.oauthtest.entities.AffiliatedActivities;
import com.roadmapper.oauthtest.entities.Track;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.commons.collections4.map.LinkedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * The music library from SoundCloud.
 */
public class MusicLibrary {

    private static final LinkedMap<String, MediaMetadataCompat> music = new LinkedMap<>();
    private static final LinkedMap<String, MediaMetadataCompat> music2 = new LinkedMap<>();
    private static final LinkedMap<String, MediaMetadataCompat> musicSearchMap = new LinkedMap<>();
    private static final HashMap<String, String> albumRes = new HashMap<>();
    private static final HashMap<String, String> musicRes = new HashMap<>();
    private static final HashMap<String, String> musicStreamRes = new HashMap<>();
    private static final HashMap<String, Bitmap> albumResBitmap = new HashMap<>();

    /*static {
        createAndAddMediaMetadata("Jazz_In_Paris", "Jazz in Paris",
                "Media Right Productions", "Jazz & Blues", "Jazz", 103,
                R.raw.jazz_in_paris, R.drawable.album_jazz_blues, "album_jazz_blues");
        createAndAddMediaMetadata("The_Coldest_Shoulder",
                "The Coldest Shoulder", "The 126ers", "Youtube Audio Library Rock 2", "Rock", 160,
                R.raw.the_coldest_shoulder, R.drawable.album_youtube_audio_library_rock_2,
                "album_youtube_audio_library_rock_2");
    }*/

    public static String getRoot() {
        return "";
    }

    public static String getSongStreamUri(String mediaId) {
        return musicStreamRes.containsKey(mediaId) ? musicStreamRes.get(mediaId) : "";
    }

    private static void setSongStreamUri(String mediaId, String musicStreamUrl) {
        musicStreamRes.put(mediaId, musicStreamUrl);
    }

    public static String getSongUri(String mediaId) {
        //return getMusicRes(mediaId);
        return getSongStreamUri(mediaId);
    }

    /*private static String getAlbumArtUri(String albumArtResName) {
        return "android.resource://" + BuildConfig.APPLICATION_ID + "/drawable/" + albumArtResName;
    }*/

    private static String getMusicRes(String mediaId) {
        return musicRes.containsKey(mediaId) ? musicRes.get(mediaId) : "";
    }

    public static Bitmap getAlbumBitmap(String mediaId) {
        return albumResBitmap.containsKey(mediaId) ? albumResBitmap.get(mediaId) : null;
    }

    public static void setAlbumResBitmap(String mediaId, Bitmap bitmap) {
        albumResBitmap.put(mediaId, bitmap);
    }

    public static String getAlbumRes(String mediaId) {
        return albumRes.containsKey(mediaId) ? albumRes.get(mediaId) : "";
    }

    public static void getAlbumBitmap(Context ctx, String mediaId, Target target) {
        String url = getAlbumRes(mediaId);
        if (url != null)
            url = url.replace("-large.jpg", "-crop.jpg");
        Picasso.get().load(url).into(target);
    }

    public static List<MediaBrowserCompat.MediaItem> getMediaItems(String mediaId) {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        Map<String, MediaMetadataCompat> items = null;
        if ("STREAM".equals(mediaId)) {
            items = music2;
        } else if ("LIKES".equals(mediaId)) {
            items = music;
        }
        if (items != null) {
            for (MediaMetadataCompat metadata : items.values()) {
                result.add(new MediaBrowserCompat.MediaItem(metadata.getDescription(),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }
        }
        return result;
    }

    public static MediaBrowserCompat.MediaItem getMediaItem(String mediaId) {
        return new MediaBrowserCompat.MediaItem(music.get(mediaId).getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    public static String getPreviousSong(String currentMediaId) {
        String prevMediaId = music.previousKey(currentMediaId);
        if (prevMediaId == null) {
            prevMediaId = music.firstKey();
        }
        return prevMediaId;
    }

    public static String getNextSong(String currentMediaId) {
        String nextMediaId = music.nextKey(currentMediaId);
        if (nextMediaId == null) {
            nextMediaId = music.firstKey();
        }
        return nextMediaId;
    }

    /*public static synchronized void updateSong(String musicId, MediaMetadata metadata) {
        music.put(musicId, metadata);
    }*/

    public static synchronized MediaMetadataCompat updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = getMetadata(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)
                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                .build();

        music.put(musicId, metadata);
        return metadata;
    }

    public static synchronized MediaMetadataCompat updateMusicRating(String musicId, RatingCompat rating) {
        MediaMetadataCompat metadata = getMetadata(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING, rating)
                .build();
        if (rating.hasHeart()) {
            music.put(musicId, metadata);
            return metadata;
        } else {
            music.remove(musicId);
            return metadata;
        }
    }

    public static synchronized MediaMetadataCompat updateMusicUri(String musicId, String streamUri) {
        setSongStreamUri(musicId, streamUri);
        MediaMetadataCompat metadata = getMetadata(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, streamUri)
                .build();
        music.put(musicId, metadata);
        return metadata;
    }

    public static MediaMetadataCompat getMetadata(String mediaId) {
        Log.d("MusicLibrary", mediaId);
        Log.d("MusicLibrary", music.keySet().toString());
        Log.d("MusicLibrary", music2.keySet().toString());
        MediaMetadataCompat metadata = music.get(mediaId);
        if (metadata == null) {
            metadata = music2.get(mediaId);
        }
        Log.d("MusicLibrary", metadata.toString());
        Bitmap albumArt = null;
        //try {
        //albumArt = getAlbumBitmap(mediaId);
        //} catch (IOException e) {
        //Log.e("MusicLibrary", "Error getting bitmap");
        //}

        // Since MediaMetadata is immutable, we need to create a copy to set the album art
        // We don't set it initially on all items so that they don't take unnecessary memory
        //MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder(metadata);
        /*for (String key: new String[]{MediaMetadata.METADATA_KEY_MEDIA_ID,
                MediaMetadata.METADATA_KEY_ALBUM, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_GENRE, MediaMetadata.METADATA_KEY_TITLE}) {
            builder.putString(key, metadataWithoutBitmap.getString(key));
        }
        builder.putLong(MediaMetadata.METADATA_KEY_DURATION,
                metadataWithoutBitmap.getLong(MediaMetadata.METADATA_KEY_DURATION));
        if (albumArt != null)
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);*/
        //return builder.build();
        return metadata;
    }

    private static void createAndAddMediaMetadata(String mediaId, String title, String artist,
                                                  String genre, long duration,
                                                  String description,
                                                  String musicUrl, String albumArtUrl, boolean likes) {
        if (likes) {
            music.put(mediaId,
                    createMetadata(mediaId, title, artist, genre, duration, description,
                            musicUrl, albumArtUrl, true));
        } else {
            music2.put(mediaId,
                    createMetadata(mediaId, title, artist, genre, duration, description,
                            musicUrl, albumArtUrl, false));
        }
        //Log.d("MusicLibrary", albumArtUrl);
        albumRes.put(mediaId, albumArtUrl);
        musicRes.put(mediaId, musicUrl);
    }

    @NonNull
    private static MediaMetadataCompat createMetadata(String mediaId, String title, String artist,
                                                      String genre, long duration,
                                                      String description, String musicUrl,
                                                      String albumArtUrl, boolean like) {
        String hiResUrl = albumArtUrl.replace("-large.jpg", "-t500x500.jpg");

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                //.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, description)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, hiResUrl)
                .putString("android.media.metadata.STREAM_URL", musicUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, hiResUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumArtUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING,
                        RatingCompat.newHeartRating(like))
                .build();
    }

    @NonNull
    private static void createMetadata(Track track, boolean likes) {
        createMetadata(track.id.toString(), track.title, track.user.username,
                track.genre, track.duration, track.description,
                track.streamUrl, track.artworkUrl != null ? track.artworkUrl : "", likes);
    }

    /**
     * Create track metadata for music hierarchy from a SoundCloud {@link Track}.
     *
     * @param track the SoundCloud track
     * @param likes if the track is in the user's likes feed
     */
    public static void createAndAddMediaMetadata(Track track, boolean likes) {
        createAndAddMediaMetadata(track.id.toString(), track.title, track.user.username,
                track.genre, track.duration, track.description,
                track.streamUrl, track.artworkUrl != null ? track.artworkUrl : "", likes);
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public static void retrieveMediaAsync(final Callback callback) {
        Log.d("MusicLibrary", "retrieveMediaAsync called");
        if (currentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        String code = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_token);
        String scope = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_scope);

        if (!"".equals(code) && !"".equals(scope)) {
            AccessToken token = new AccessToken(code, scope);
            SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class, token);

            // Asynchronously load the music catalog in a separate thread
            Call<List<Track>> call = client.getMyFavorites(100);
            call.enqueue(new retrofit2.Callback<List<Track>>() {
                @Override
                public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                    Log.d("MusicLibrary", response.message());
                    Log.d("MusicLibrary", response.code() + "");
                    Log.d("MusicLibrary", response.headers().toString());
                    List<Track> tracks = response.body();
                    if (tracks != null) {
                        for (Track track : tracks) {
                            Log.d("MusicLibrary",
                                    track.user.username + " (" + track.title + ")");
                            createAndAddMediaMetadata(track, true);
                        }
                    }
                    currentState = State.INITIALIZED;
                    callback.onMusicCatalogReady(currentState == State.INITIALIZED);
                }

                @Override
                public void onFailure(Call<List<Track>> call, Throwable t) {
                    Log.d("MusicLibrary", "Failure");
                    Log.d("MusicLibrary", t.getMessage());
                    currentState = State.NON_INITIALIZED;
                    callback.onMusicCatalogReady(currentState == State.INITIALIZED);
                }
            });

            Call<AffiliatedActivities> call2 = client.getStreamTracks(100);
            call2.enqueue(new retrofit2.Callback<AffiliatedActivities>() {
                @Override
                public void onResponse(Call<AffiliatedActivities> call, Response<AffiliatedActivities> response) {
                    Log.d("MusicLibrary", response.message());
                    Log.d("MusicLibrary", response.code() + "");
                    Log.d("MusicLibrary", response.headers().toString());
                    List<Activity> activities = response.body().collection;
                    if (activities != null) {
                        //trackList.clear();
                        for (Activity activity : activities) {
                            // activity.origin can be null, we don't want to show type playlist, unable to extract data out of that currently
                            if (activity.origin != null && !activity.type.equals("playlist")) {
                                Log.d("MusicLibrary",
                                        activity.origin.user.username + " (" + activity.origin.title + ")");
                                //trackList.add(activity.origin);
                                MusicLibrary.createAndAddMediaMetadata(activity.origin, false);
                            }
                        }
                        //trackAdapter.notifyDataSetChanged();
                    }


                }

                @Override
                public void onFailure(Call<AffiliatedActivities> call, Throwable t) {
                    Log.d("MusicLibrary", "Failure");
                    Log.d("MusicLibrary", t.getMessage());
                }
            });
            currentState = State.INITIALIZING;


        } else {
            if (callback != null) {
                callback.onMusicCatalogReady(currentState == State.INITIALIZED);
            }
        }
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public static void retrieveMediaAsync2(final Callback callback) {
        Log.d("MusicLibrary", "retrieveMediaAsync2 called");
        if (currentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        String code = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_token);
        String scope = SharedPrefManager.getInstance().readSharedPrefString(R.string.oauth_scope);

        if (!"".equals(code) && !"".equals(scope)) {
            AccessToken token = new AccessToken(code, scope);
            SoundCloudClient client = ServiceGenerator.createService(SoundCloudClient.class, token);

            // Asynchronously load the music catalog in a separate thread
            Call<AffiliatedActivities> call = client.getStreamTracks(100);
            call.enqueue(new retrofit2.Callback<AffiliatedActivities>() {
                @Override
                public void onResponse(Call<AffiliatedActivities> call, Response<AffiliatedActivities> response) {
                    Log.d("MusicLibrary", response.message());
                    Log.d("MusicLibrary", response.code() + "");
                    Log.d("MusicLibrary", response.headers().toString());
                    List<Activity> activities = response.body().collection;
                    if (activities != null) {
                        //trackList.clear();
                        for (Activity activity : activities) {
                            Log.d("MainActivity",
                                    activity.origin.user.username + " (" + activity.origin.title + ")");
                            //trackList.add(activity.origin);
                            MusicLibrary.createAndAddMediaMetadata(activity.origin, false);
                        }
                        //trackAdapter.notifyDataSetChanged();
                    }

                }

                @Override
                public void onFailure(Call<AffiliatedActivities> call, Throwable t) {
                    Log.d("MainActivity", "Failure");
                    Log.d("MainActivity", t.getMessage());
                }
            });
            currentState = State.INITIALIZING;


        } else {
            if (callback != null) {
                callback.onMusicCatalogReady(currentState == State.INITIALIZED);
            }
        }
    }

    private enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    static boolean isInitialized() {
        return currentState == State.INITIALIZED;
    }

    private static volatile State currentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }
}