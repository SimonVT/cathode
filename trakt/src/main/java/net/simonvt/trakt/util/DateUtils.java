package net.simonvt.trakt.util;

import net.simonvt.trakt.R;

import android.content.Context;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtils {

    private static final String TAG = "DateUtils";

    public static final long MINUTE_IN_SECONDS = 60;

    public static final long HOUR_IN_SECONDS = 60 * MINUTE_IN_SECONDS;

    public static final long DAY_IN_SECONDS = 24 * HOUR_IN_SECONDS;

    public static final long YEAR_IN_SECONDS = 365 * DAY_IN_SECONDS;

    public static final long MINUTE_IN_MILLIS = android.text.format.DateUtils.MINUTE_IN_MILLIS;

    public static final long HOUR_IN_MILLIS = android.text.format.DateUtils.HOUR_IN_MILLIS;

    public static final long DAY_IN_MILLIS = android.text.format.DateUtils.DAY_IN_MILLIS;

    public static final long YEAR_IN_MILLIS = android.text.format.DateUtils.YEAR_IN_MILLIS;

    private DateUtils() {
    }

    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    public static long getServerTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        return cal.getTimeInMillis() / 1000L;
    }

    // TODO: Check that this actually works.. It returns a date string, but no idea if it's correctly converted to the
    //       users timezone.. It probably isn't.
    public static String secondsToDate(Context context, long seconds) {
        long millis = seconds * android.text.format.DateUtils.SECOND_IN_MILLIS;

        if (millis < android.text.format.DateUtils.YEAR_IN_MILLIS) {
            return context.getResources().getString(R.string.airdate_unknown);
        }

        // Trakt timestamps are in PST (-8) .. Correct!
        millis += 8 * HOUR_IN_MILLIS;

        final Calendar cal = Calendar.getInstance();

        final long rightNow = cal.getTimeInMillis();

        if (rightNow >= millis - 2 * MINUTE_IN_MILLIS && rightNow <= millis + 2 * MINUTE_IN_MILLIS) {
            return context.getString(R.string.now);
        }

        if (rightNow > millis && rightNow < millis + 1 * HOUR_IN_MILLIS) {
            final long minutes = (rightNow - millis) / MINUTE_IN_MILLIS;
            return context.getString(R.string.in_minutes, minutes);
        }

        if (rightNow < millis && rightNow > millis - 1 * HOUR_IN_MILLIS) {
            final long minutes = (millis - rightNow) / MINUTE_IN_MILLIS;
            return context.getString(R.string.minutes_ago, minutes);
        }

        if (rightNow > millis && rightNow <= millis + 24 * HOUR_IN_MILLIS) {
            final long hours = (rightNow - millis) / HOUR_IN_MILLIS;
            return context.getString(R.string.in_hours, hours);
        }

        if (rightNow < millis && rightNow >= millis - 24 * HOUR_IN_MILLIS) {
            final long hours = (millis - rightNow) / HOUR_IN_MILLIS;
            return context.getString(R.string.hours_ago, hours);
        }

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
