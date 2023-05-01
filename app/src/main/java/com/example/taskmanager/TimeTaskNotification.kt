package com.example.taskmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.taskmanager.sendpush.ApiClient
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.ArrayList

class TimeTaskNotification : BroadcastReceiver() {
    val ALARM_CHANNEL_ID = "alarm_playback_channel"
    private val NOTIFICATION_ID = 105

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.logo1
        ) //replace with your own image
        createChannel(context)
        val message = intent.getStringExtra("message") ?: "message"
        val title = intent.getStringExtra("title") ?: "title"
        val lead_fcm = intent.getStringExtra("lead_fcm") ?: ""

        val i = Intent(context, TaskManagerActivity::class.java)
        i.putExtra("notify", true)

        i.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(context, 0, i, 0)

        val notificationBuilder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setColor(context.getColor(R.color.teal_200))
            .setLargeIcon(largeIcon)
            .setSmallIcon(R.drawable.ic_settings)
            .setContentText(message)
            .setContentTitle(title)
            //.setContentInfo("title3")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setDefaults(Notification.DEFAULT_ALL)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notificationBuilder.build())
        Log.d("anna", "pre send push")
        sendPush(lead_fcm, title, message)

        Log.d("anna", "post send push")
    }

    //отправить пуш конкретному участнику
    private fun sendPush(token: String, title: String, text: String) {
        Log.d("anna", "send push")
        ApiClient.sendPush(token, title, text)
    }

    private fun createChannel(context: Context) {
        // The id of the channel.
        val id: String = ALARM_CHANNEL_ID
        // The user-visible name of the channel.
        val name: CharSequence = "Alarm notify"
        // The user-visible description of the channel.
        val description = "Alarm control"
        val importance: Int
        importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(id, name, importance)
            // Configure the notification channel.
            mChannel.description = description
            mChannel.setShowBadge(false)
            mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                mChannel
            )
        }
    }
}
