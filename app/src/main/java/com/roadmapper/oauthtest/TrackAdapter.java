package com.roadmapper.oauthtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.roadmapper.oauthtest.entities.Track;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An adapter to handle the track list from a JSON list; <br/>
 * acquires the media artwork with Picasso at runtime.
 */
public class TrackAdapter extends ArrayAdapter<Track> {

    private final Context context;
    private List<Track> tracks;


    public TrackAdapter(Context context, List<Track> tracks) {
        super(context, R.layout.track_item);
        this.context = context;
        this.tracks = tracks;
    }

    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {

        ViewHolder holder;
        // Reuse the view
        if (rowView != null) {
            holder = (ViewHolder) rowView.getTag();
        } else {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.track_item, parent, false);
            holder = new ViewHolder(rowView);
            rowView.setTag(holder);
        }

        Track item = tracks.get(position);

        // Fill data into the ViewHolder
        Picasso.get()
                .load(item.artworkUrl)
                .into(holder.artwork);
        holder.trackName.setText(item.title);
        holder.user.setText(item.user.fullName);
        holder.duration.setText(String.format(Locale.US, "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(item.duration),
                TimeUnit.MILLISECONDS.toSeconds(item.duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(item.duration))
        ));

        return rowView;
    }

    static class ViewHolder {
        @BindView(R.id.artwork) ImageView artwork;
        @BindView(R.id.trackname) TextView trackName;
        @BindView(R.id.user) TextView user;
        @BindView(R.id.duration) TextView duration;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
