package com.roadmapper.oauthtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.roadmapper.oauthtest.entities.Track;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An adapter to handle the track list from a JSON list; <br/>
 * acquires the media artwork with Picasso at runtime.
 */
public class TrackRecyclerAdapter extends RecyclerView.Adapter<TrackRecyclerAdapter.ViewHolder> {

    private static final String TAG = TrackRecyclerAdapter.class.getSimpleName();
    private final Context context;
    private List<Track> tracks;
    OnItemClickListener mItemClickListener;

    public TrackRecyclerAdapter(Context context, List<Track> tracks) {
        //super(context, R.layout.track_item_card);
        this.context = context;
        this.tracks = tracks;
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.track_item_card, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder trackViewHolder, int i) {
        final Track track = tracks.get(i);
        trackViewHolder.trackName.setText(track.title);
        trackViewHolder.user.setText(track.user.username);
        trackViewHolder.duration.setText(String.format(Locale.US, "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(track.duration),
                TimeUnit.MILLISECONDS.toSeconds(track.duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(track.duration))
        ));

        //Log.i(TAG, track.artworkUrl);
        String artworkUrl = null;
        if (track.artworkUrl != null) {
            artworkUrl = track.artworkUrl;//.replace("-large.jpg", "-crop.jpg");
        }
        //Log.i(TAG, track.artworkUrl);
        /*Picasso.with(context)
                    .load(artworkUrl).noPlaceholder()
                    .into(trackViewHolder.artwork);*/
        AlbumArtCache.getInstance().fetch(artworkUrl, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage) {
                trackViewHolder.artwork.setImageBitmap(bigImage);

                /*MediaMetadata trackMetadata = MusicLibrary.getMetadata(AutoCloudApplication.getAppContext(), Long.toString(track.id));
                trackMetadata = new MediaMetadata.Builder(trackMetadata)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bigImage)
                        .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, iconImage)
                        .build();
                MusicLibrary.updateSong(Long.toString(track.id), trackMetadata);*/
            }
        });

        /*Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        Shader shader = new LinearGradient(0, 0, 400, 0, Color.parseColor("#008e8485"), Color.parseColor("#00e6846e"), Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.setRotate(180);
        shader.setLocalMatrix(matrix);
        Paint paint = new Paint();
        paint.setShader(shader);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        //canvas.setBitmap(bitmap);
        trackViewHolder.artwork.setImageBitmap(bitmap);*/

        //}
    }

    // Clean all elements of the recycler
    public void clear() {
        tracks.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<Track> list) {
        tracks.addAll(list);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.cv)
        CardView cardView;
        @BindView(R.id.artwork)
        ImageView artwork;
        @BindView(R.id.trackname)
        TextView trackName;
        @BindView(R.id.user)
        TextView user;
        @BindView(R.id.duration)
        TextView duration;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }
}
