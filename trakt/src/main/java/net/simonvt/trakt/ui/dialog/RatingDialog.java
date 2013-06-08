package net.simonvt.trakt.ui.dialog;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.MovieTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import javax.inject.Inject;

public class RatingDialog extends DialogFragment {

    public enum Type {
        SHOW,
        EPISODE,
        MOVIE,
    }

    private static final String TAG = "RatingDialog";

    private static final String ARG_TYPE = "net.simonvt.trakt.ui.dialog.RatingDialog.type";
    private static final String ARG_ID = "net.simonvt.trakt.ui.dialog.RatingDialog.id";
    private static final String ARG_RATING = "net.simonvt.trakt.ui.dialog.RatingDialog.rating";

    @Inject ShowTaskScheduler mShowScheduler;

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    @Inject MovieTaskScheduler mMovieScheduler;

    private Type mType;

    private long mId;

    private String[] mRatingText;

    public static RatingDialog newInstance(Type type, long id, int rating) {
        RatingDialog dialog = new RatingDialog();

        Bundle args = new Bundle();
        args.putSerializable(ARG_TYPE, type);
        args.putLong(ARG_ID, id);
        args.putInt(ARG_RATING, rating);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);

        Bundle args = getArguments();
        mType = (Type) args.getSerializable(ARG_TYPE);
        mId = args.getLong(ARG_ID);

        mRatingText = getResources().getStringArray(R.array.ratings);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View v = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_rating, null);
        final int rating = getArguments().getInt(ARG_RATING);
        final TextView ratingText = (TextView) v.findViewById(R.id.ratingText);
        final RatingBar ratingBar = (RatingBar) v.findViewById(R.id.rating);
        ratingBar.setRating(rating);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                ratingText.setText(mRatingText[(int) v]);
            }
        });

        builder.setView(v);
        builder.setPositiveButton(R.string.action_rate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (mType) {
                    case SHOW:
                        mShowScheduler.rate(mId, (int) ratingBar.getRating());
                        break;

                    case EPISODE:
                        mEpisodeScheduler.rate(mId, (int) ratingBar.getRating());
                        break;

                    case MOVIE:
                        mMovieScheduler.rate(mId, (int) ratingBar.getRating());
                        break;
                }
            }
        });

        return builder.create();
    }
}
