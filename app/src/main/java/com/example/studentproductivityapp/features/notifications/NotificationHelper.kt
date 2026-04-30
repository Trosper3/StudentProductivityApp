package com.example.studentproductivityapp.features.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.studentproductivityapp.features.assignments.database.Assignment

object NotificationHelper {
    fun scheduleNotification(context: Context, assignment: Assignment) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AssignmentAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", assignment.title)
            putExtra("EXTRA_COURSE", assignment.courseName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            assignment.title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //For DEMO: Trigger exactly 10 seconds from right now.
        //For PRODUCTION: val triggerTime = assignment.dueDateMillis - (24 * 60 * 60 * 1000)
        val triggerTime = System.currentTimeMillis() + 10000

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) {

        }
    }
}