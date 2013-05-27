package net.simonvt.trakt.util;

import net.simonvt.trakt.R;

import android.content.Context;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.Locale;

public final class UiUtils {

    private static final String TAG = "UiUtils";

    private UiUtils() {
    }

    // TODO: Check that this actually works.. It returns a date string, but no idea if it's correctly converted to the
    //       users timezone.. It probably isn't.
    public static String secondsToDate(Context context, long seconds) {
        final long millis = seconds * DateUtils.SECOND_IN_MILLIS;

        if (millis < DateUtils.YEAR_IN_MILLIS) {
            return context.getResources().getString(R.string.airdate_unknown);
        }

        final Calendar cal = Calendar.getInstance();
        final int year = cal.get(Calendar.YEAR);
        cal.setTimeInMillis(millis);
        final int millisYear = cal.get(Calendar.YEAR);

        String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        StringBuilder sb = new StringBuilder();

        sb.append(month).append(" ").append(day);
        if (millisYear != year) {
            sb.append(", ").append(millisYear);
        }

        return sb.toString();
    }
}
