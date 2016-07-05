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
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.roadmapper.oauthtest.entities.Track;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

class MusicLibrary {

    private static final TreeMap<String, MediaMetadataCompat> music = new TreeMap<>();
    private static final HashMap<String, String> albumRes = new HashMap<>();
    private static final HashMap<String, String> musicRes = new HashMap<>();
    private static final HashMap<String, String> musicStreamRes = new HashMap<>();
    private static final HashMap<String, Bitmap> albumResBitmap = new HashMap<>();

    /*static {
        createMediaMetadata("Jazz_In_Paris", "Jazz in Paris",
                "Media Right Productions", "Jazz & Blues", "Jazz", 103,
                R.raw.jazz_in_paris, R.drawable.album_jazz_blues, "album_jazz_blues");
        createMediaMetadata("The_Coldest_Shoulder",
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

    public static void setSongStreamUri(String mediaId, String musicStreamUrl) {
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
        Picasso.with(ctx).load(url).into(target);
    }

    public static List<MediaBrowserCompat.MediaItem> getMediaItems() {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for (MediaMetadataCompat metadata: music.values()) {
            result.add(new MediaBrowserCompat.MediaItem(metadata.getDescription(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    public static MediaBrowserCompat.MediaItem getMediaItem(String mediaId) {
        return new MediaBrowserCompat.MediaItem(music.get(mediaId).getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    public static String getPreviousSong(String currentMediaId) {
        String prevMediaId = music.lowerKey(currentMediaId);
        if (prevMediaId == null) {
            prevMediaId = music.firstKey();
        }
        return prevMediaId;
    }

    public static String getNextSong(String currentMediaId) {
        String nextMediaId = music.higherKey(currentMediaId);
        if (nextMediaId == null) {
            nextMediaId = music.firstKey();
        }
        return nextMediaId;
    }

    /*public static synchronized void updateSong(String musicId, MediaMetadata metadata) {
        music.put(musicId, metadata);
    }*/

    public static MediaMetadataCompat getMetadata(Context ctx, String mediaId) {
        MediaMetadataCompat metadataWithoutBitmap = music.get(mediaId);
        Bitmap albumArt = null;
        //try {
            //albumArt = getAlbumBitmap(mediaId);
        //} catch (IOException e) {
            //Log.e("MusicLibrary", "Error getting bitmap");
        //}

        // Since MediaMetadata is immutable, we need to create a copy to set the album art
        // We don't set it initially on all items so that they don't take unnecessary memory
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder(metadataWithoutBitmap);
        /*for (String key: new String[]{MediaMetadata.METADATA_KEY_MEDIA_ID,
                MediaMetadata.METADATA_KEY_ALBUM, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_GENRE, MediaMetadata.METADATA_KEY_TITLE}) {
            builder.putString(key, metadataWithoutBitmap.getString(key));
        }
        builder.putLong(MediaMetadata.METADATA_KEY_DURATION,
                metadataWithoutBitmap.getLong(MediaMetadata.METADATA_KEY_DURATION));
        if (albumArt != null)
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);*/
        return builder.build();
    }

    private static void createMediaMetadata(String mediaId, String title, String artist,
                                            String genre, long duration,
                                            String description,
                                            String musicUrl, String albumArtUrl) {
        String hiResUrl = albumArtUrl.replace("-large.jpg", "-t500x500.jpg");
        music.put(mediaId,
                new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                        //.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, description)
                        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                        .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, hiResUrl)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, hiResUrl)
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumArtUrl)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
                        .build());
        //Log.d("MusicLibrary", albumArtUrl);
        albumRes.put(mediaId, albumArtUrl);
        musicRes.put(mediaId, musicUrl);
    }

    public static void createMediaMetadata(Track track) {
        //Log.d("MusicLibrary", track.toString());
        //Log.d("MusicLibrary", track.artworkUrl);
        createMediaMetadata(track.id.toString(), track.title, track.user.username,
                track.genre, track.duration, track.description,
                track.streamUrl, track.artworkUrl != null ? track.artworkUrl : "");
    }
}