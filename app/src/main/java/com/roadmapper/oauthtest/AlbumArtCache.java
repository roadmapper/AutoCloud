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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * Implements a basic cache of album arts, with async loading support.
 */
public final class AlbumArtCache {
    private static final String TAG = AlbumArtCache.class.getSimpleName();

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 24 * 1024 * 1024;  // 24 MiB
    private static final int MAX_ART_WIDTH = 500;  // pixels
    private static final int MAX_ART_HEIGHT = 500;  // pixels

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight. If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    private static final int MAX_ART_WIDTH_ICON = 100;  // pixels
    private static final int MAX_ART_HEIGHT_ICON = 100;  // pixels

    private static final int BIG_BITMAP_INDEX = 0;
    private static final int ICON_BITMAP_INDEX = 1;

    private final LruCache<String, Bitmap[]> mCache;

    private static final AlbumArtCache sInstance = new AlbumArtCache();

    public static AlbumArtCache getInstance() {
        return sInstance;
    }

    private AlbumArtCache() {
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by maxmemory/4 and
        // Integer.MAX_VALUE:
        int maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
                (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / 4)));
        mCache = new LruCache<String, Bitmap[]>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap[] value) {
                return value[BIG_BITMAP_INDEX].getByteCount()
                        + value[ICON_BITMAP_INDEX].getByteCount();
            }
        };
    }

    public Bitmap getBigImage(String artUrl) {
        artUrl = artUrl.replace("-large.jpg", "-t500x500.jpg");
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[BIG_BITMAP_INDEX];
    }

    public Bitmap getIconImage(String artUrl) {
        artUrl = artUrl.replace("-large.jpg", "-t500x500.jpg");
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[ICON_BITMAP_INDEX];
    }

    @SuppressLint("StaticFieldLeak")
    public void fetch(final String artUrl, final FetchListener listener) {
        // WARNING: for the sake of simplicity, simultaneous multi-thread fetch requests
        // are not handled properly: they may cause redundant costly operations, like HTTP
        // requests and bitmap rescales. For production-level apps, we recommend you use
        // a proper image loading library, like Glide.

        if (artUrl == null) {
            /*Bitmap bitmap = Bitmap.createBitmap(MAX_ART_WIDTH, MAX_ART_HEIGHT, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas();
            Shader shader = new LinearGradient(0, 0, MAX_ART_WIDTH, MAX_ART_HEIGHT, Color.parseColor("#8e8485"), Color.parseColor("#e6846e"), Shader.TileMode.CLAMP);
            Paint paint = new Paint();
            paint.setShader(shader);
            canvas.drawRect(0, 0, MAX_ART_WIDTH, MAX_ART_HEIGHT, paint);
            canvas.setBitmap(bitmap);

            Bitmap icon = BitmapHelper.scaleBitmap(bitmap,
                    MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON);

            listener.onFetched(null, bitmap, icon);*/
            listener.onError(null, new IllegalArgumentException("got null bitmaps"));
        } else {
            final String hiResUrl = artUrl.replace("-large.jpg", "-t500x500.jpg");
            Bitmap[] bitmap = mCache.get(hiResUrl);
            if (bitmap != null) {
                Log.d(TAG, "getOrFetch: cache hit on album art, using it: " + hiResUrl);
                listener.onFetched(hiResUrl, bitmap[BIG_BITMAP_INDEX], bitmap[ICON_BITMAP_INDEX]);
                return;
            }
            Log.d(TAG, "getOrFetch: cache miss, starting asynctask to fetch: " + hiResUrl);

            new AsyncTask<Void, Void, Bitmap[]>() {
                @Override
                protected Bitmap[] doInBackground(Void[] objects) {
                    Bitmap[] bitmaps;
                    try {
                        Bitmap bitmap = Picasso.with(AutoCloudApplication.getAppContext())
                                .load(hiResUrl).get();
                        Bitmap icon = BitmapHelper.scaleBitmap(bitmap,
                                MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON);
                        bitmaps = new Bitmap[]{bitmap, icon};
                        mCache.put(hiResUrl, bitmaps);
                    } catch (IOException e) {
                        return null;
                    }
                    Log.d(TAG, "doInBackground: putting bitmap in cache. cache size=" +
                            mCache.size());
                    return bitmaps;
                }

                @Override
                protected void onPostExecute(Bitmap[] bitmaps) {
                    if (bitmaps == null) {
                        listener.onError(hiResUrl, new IllegalArgumentException("got null bitmaps"));
                    } else {
                        listener.onFetched(hiResUrl,
                                bitmaps[BIG_BITMAP_INDEX], bitmaps[ICON_BITMAP_INDEX]);
                    }
                }
            }.execute();
        }
    }

    public static abstract class FetchListener {
        public abstract void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage);

        public void onError(String artUrl, Exception e) {
            Log.w(TAG, "AlbumArtFetchListener: error while downloading " + artUrl, e);
        }
    }
}