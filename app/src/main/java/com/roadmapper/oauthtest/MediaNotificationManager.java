package com.roadmapper.oauthtest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. This is required so that the music service
 * doesn't get killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private static final String ACTION_PAUSE = "com.roadmapper.autocloud.pause";
    private static final String ACTION_PLAY = "com.roadmapper.autocloud.play";
    private static final String ACTION_NEXT = "com.roadmapper.autocloud.next";
    private static final String ACTION_PREV = "com.roadmapper.autocloud.prev";

    private final MusicService mService;

    private final NotificationManager mNotificationManager;

    private final Notification.Action mPlayAction;
    private final Notification.Action mPauseAction;
    private final Notification.Action mNextAction;
    private final Notification.Action mPrevAction;

    private boolean mStarted;

    public MediaNotificationManager(MusicService service) {
        mService = service;

        String pkg = mService.getPackageName();
        PendingIntent playIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent nextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent prevIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        mPlayAction = new Notification.Action.Builder(android.R.drawable.ic_media_play,
                "Play", playIntent).build();
        mPauseAction = new Notification.Action.Builder(android.R.drawable.ic_media_pause,
                "Pause", pauseIntent).build();
        mNextAction = new Notification.Action.Builder(android.R.drawable.ic_media_next,
                "Next", nextIntent).build();
        mPrevAction = new Notification.Action.Builder(android.R.drawable.ic_media_previous,
                "Previous", prevIntent).build();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PREV);

        mService.registerReceiver(this, filter);

        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case ACTION_PAUSE:
                mService.mCallback.onPause();
                break;
            case ACTION_PLAY:
                mService.mCallback.onPlay();
                break;
            case ACTION_NEXT:
                mService.mCallback.onSkipToNext();
                break;
            case ACTION_PREV:
                mService.mCallback.onSkipToPrevious();
                break;
        }
    }

    public void update(MediaMetadata metadata, PlaybackState state, MediaSession.Token token) {
        if (state == null || state.getState() == PlaybackState.STATE_STOPPED ||
                state.getState() == PlaybackState.STATE_NONE) {
            mService.stopForeground(true);
            try {
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore receiver not registered
            }
            mService.stopSelf();
            return;
        }
        if (metadata == null) {
            return;
        }
        boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
        Notification.Builder notificationBuilder = new Notification.Builder(mService);
        MediaDescription description = metadata.getDescription();

        notificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(token)
                        .setShowActionsInCompactView(0, 1, 2))
                //.setColor(mService.getApplication().getResources().getColor(R.color.notification_bg))
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setOngoing(isPlaying)
                .setWhen(isPlaying ? System.currentTimeMillis() - state.getPosition() : 0)
                .setShowWhen(isPlaying)
                .setUsesChronometer(isPlaying);

        /*try {
            notificationBuilder.setLargeIcon(
                    MusicLibrary.getAlbumBitmap(mService, description.getMediaId()));
        } catch (IOException e) {
            Log.e("MediaNotificationMgr", "error retrieving album artwork");
        }*/

        // If skip to next action is enabled
        if ((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(mPrevAction);
        }

        notificationBuilder.addAction(isPlaying ? mPauseAction : mPlayAction);

        // If skip to prev action is enabled
        if ((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(mNextAction);
        }

        notify(notificationBuilder, isPlaying, metadata.getDescription().getMediaId());
    }

    private void notify(Notification.Builder notificationBuilder, boolean isPlaying, String mediaId) {
        Bitmap bitmap = MusicLibrary.getAlbumBitmap(mediaId);

        /*if (bitmap == null) {
            try {
                bitmap = Picasso.with(mService).load(MusicLibrary.getAlbumRes(mediaId)).get();
            } catch (IOException e) {
                Log.e("MediaNotificationMgr", "error retrieving album artwork");
            }
        }*/

        notificationBuilder.setLargeIcon(bitmap);

        Notification notification = notificationBuilder.build();

        if (isPlaying && !mStarted) {
            mService.startService(new Intent(mService.getApplicationContext(), MusicService.class));
            mService.startForeground(NOTIFICATION_ID, notification);
            mStarted = true;
        } else {
            if (!isPlaying) {
                mService.stopForeground(false);
                mStarted = false;
            }
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }



    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

}
