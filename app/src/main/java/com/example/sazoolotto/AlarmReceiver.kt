package com.example.sazoolotto // 🔥 여기도 기본 패키지여야 합니다.

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔥 MainActivity를 찾을 수 있게 됩니다.
        val appIntent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, "daily_saju_channel")
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("🌙 자정입니다! 오늘의 운세가 도착했어요")
            .setContentText("지금 접속해서 액운을 막고 대박 번호를 확인하세요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}