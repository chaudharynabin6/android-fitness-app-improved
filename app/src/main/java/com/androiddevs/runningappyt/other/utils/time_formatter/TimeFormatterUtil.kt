package com.androiddevs.runningappyt.other.utils.time_formatter

import java.util.concurrent.TimeUnit

object TimeFormatterUtil {
    fun getFormattedStopWatchTime(ms: Long, isMillisIncluded: Boolean = false): String {

        var milliseconds = ms
//        hour will be
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
//        remaining seconds after subtracting hours millisecond
        milliseconds -= TimeUnit.HOURS.toMillis(hours)

//        similarly
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)

        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)

        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        if (!isMillisIncluded) {

            return "${if (hours <= 9) "0" else ""}$hours:" +
                    "${if (minutes <= 9) "0" else ""}$minutes:" +
                    "${if (seconds <= 9) "0" else ""}$seconds"
        }

        milliseconds -= TimeUnit.SECONDS.toMillis(seconds)
        milliseconds /= 10
        return "${if (hours <= 9) "0" else ""}$hours:" +
                "${if (minutes <= 9) "0" else ""}$minutes:" +
                "${if (seconds <= 9) "0" else ""}$seconds:" +
                "${if (milliseconds <= 9) "0" else ""}$milliseconds"


    }
}