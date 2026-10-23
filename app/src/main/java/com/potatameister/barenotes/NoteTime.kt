package com.potatameister.barenotes

import android.content.Context
import android.text.format.DateUtils

object NoteTime {

    fun relative(context: Context, time: Long): String {
        val now = System.currentTimeMillis()
        return if (now - time < DateUtils.MINUTE_IN_MILLIS) {
            context.getString(R.string.just_now)
        } else {
            DateUtils.getRelativeTimeSpanString(
                time,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    }

    fun absolute(context: Context, time: Long): String =
        DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_ABBREV_MONTH
        )
}
