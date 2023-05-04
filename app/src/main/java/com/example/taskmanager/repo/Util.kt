package com.example.taskmanager.repo

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import com.example.taskmanager.TimeMessageNotification
import com.example.taskmanager.TimeNotification
import com.example.taskmanager.TimeTaskNotification
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

object Util {
    //текстовое представление статуса проекта
    fun getTextStatus(status: Long, oid: String, uid: String) = when(status){
        0L -> "Не начато"
        1L -> "В работе"
        2L -> if(oid.equals(uid)) "На контроле" else "Выполнено"
        3L -> "Просрочено"
        4L -> "Принято"
        else -> "Не известно"
    }
    //графическое представление статуса проекта
    fun setTextStyle(status: Long, oid: String, uid: String, textView: TextView){
        val mode = when(status){
            0L -> 0
            1L -> 1
            2L -> if(oid.equals(uid)) 2 else 3
            3L -> 4
            4L -> 5
            else -> 6
        }
        when(mode){
            0 -> {
                //обычный шрифт черного цвета
                textView.setTextColor(Color.BLACK)
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setPaintFlags(textView.getPaintFlags() and Paint.STRIKE_THRU_TEXT_FLAG.inv())
            }
            1 -> {
                //курсив черного цвета
                textView.setTextColor(Color.BLACK)
                textView.setTypeface(null, Typeface.ITALIC)
                textView.setPaintFlags(textView.getPaintFlags() and Paint.STRIKE_THRU_TEXT_FLAG.inv())
            }
            2 -> {
                //обычный шрифт оранжевого цвета
                textView.setTextColor(Color.parseColor("#f76316"))
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setPaintFlags(textView.getPaintFlags() and Paint.STRIKE_THRU_TEXT_FLAG.inv())
            }
            3 -> {
                //зачеркнутый шрифт серого цвета
                textView.setTextColor(Color.parseColor("#707070"))
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setPaintFlags(textView.getPaintFlags() or Paint.STRIKE_THRU_TEXT_FLAG)
            }
            4 -> {
                //обычный шрифт красного цвета
                textView.setTextColor(Color.RED)
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setPaintFlags(textView.getPaintFlags() and Paint.STRIKE_THRU_TEXT_FLAG.inv())
            }
            5 -> {
                //обычный шрифт зеленого цвета
                textView.setTextColor(Color.GREEN)
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setPaintFlags(textView.getPaintFlags() and Paint.STRIKE_THRU_TEXT_FLAG.inv())
            }
        }
    }
    //0 - member
    //1 - lead ответственный
    //2 - owner
    //определения статуса авторизованного пользователя в проекте
    fun getUserStatus(): Int {
        if(Repository.isArchive) return 0
        val oid = Repository.currentProject?. oid ?: return 0
        val uid = Repository.user?. uid ?: return 0
        if(oid.equals(uid)) return 2
        val lid = Repository.currentTask?.leadUid ?: return 0
        if(lid.equals(uid)) return 1
        return 0
    }
    
    fun checkTaskStatusTimeout(){
        if(Repository.isArchive) return
        val pid = Repository.currentProject?.pid ?: return
        val db = Firebase.firestore
        val cal = Calendar.getInstance()
        val timestamp = cal.timeInMillis
        db.collection("tasks")
            .whereEqualTo("pid", pid)
            .get()
            .addOnCompleteListener { result ->
                if(result.isSuccessful){
                    val documents = result.result.documents
                    for(doc in documents){
                        val time = doc["date_of_end_timestamp"] as Long? ?: 0L
                        if(time == 0L) continue
                        val status = doc["status"] as Long? ?: continue
                        if(status == 3L) continue
                        if(status == 4L) continue
                        if(timestamp > time){
                            val id = doc.id
                            val data = hashMapOf(
                                "status" to 3L
                            )
                            db.collection("tasks").document(id).update(data as Map<String, Any>)
                        }
                    }
                }
            }
    }
    fun setAlarmTaskDeadline(context: Context, date: String, name: String, lead_fcm: String){
        val dt = date.split(".")
        val day = dt[0].toInt() - 1
        val month = dt[1].toInt() - 1
        val year = dt[2].toInt()

        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month)
        c.set(Calendar.DAY_OF_MONTH, day)
        val _hour = c.get(Calendar.HOUR_OF_DAY)
        val _minute = c.get(Calendar.MINUTE)
        val _second = c.get(Calendar.SECOND)

        val endmils = c.timeInMillis
        val amils = endmils - 86340000L
        c.timeInMillis = amils
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH) + 1

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeTaskNotification::class.java)
        intent.putExtra("message","Сутки на задачу")
        intent.putExtra("title",name)
        intent.putExtra("lead_fcm", lead_fcm)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        //am.cancel(pendingIntent)
        am.set(AlarmManager.RTC_WAKEUP, amils, pendingIntent)

        Log.d("anna", "test")
    }
    fun setAlarmDedline(context: Context,date: String, pid: String, name: String){
        val dt = date.split(".")
        val day = dt[0].toInt() - 1
        val month = dt[1].toInt() - 1
        val year = dt[2].toInt()

        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month)
        c.set(Calendar.DAY_OF_MONTH, day)
        val _hour = c.get(Calendar.HOUR_OF_DAY)
        val _minute = c.get(Calendar.MINUTE)
        val _second = c.get(Calendar.SECOND)

        val endmils = c.timeInMillis
        val amils = endmils - 86340000L
        c.timeInMillis = amils
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH) + 1

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeNotification::class.java)
        intent.putExtra("message","Остался день до окончания проекта")
        intent.putExtra("title",name)
        intent.putExtra("pid", pid)
        intent.putExtra("uid", Repository.user?.uid ?: "")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        //am.cancel(pendingIntent)
        am.set(AlarmManager.RTC_WAKEUP, amils, pendingIntent)
        Log.d("anna", "test")
    }

    fun setAlarmDedline5(context: Context,date: String, pid: String, name: String){
        val dt = date.split(".")
        val day = dt[0].toInt() - 1
        val month = dt[1].toInt() - 1
        val year = dt[2].toInt()

        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month)
        c.set(Calendar.DAY_OF_MONTH, day)
        val _hour = c.get(Calendar.HOUR_OF_DAY)
        val _minute = c.get(Calendar.MINUTE)
        val _second = c.get(Calendar.SECOND)

        val endmils = c.timeInMillis
        val amils = endmils - 43170000L
        c.timeInMillis = amils
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH) + 1

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeNotification::class.java)
        intent.putExtra("message","До окончания проекта осталось 5 дней")
        intent.putExtra("title",name)
        intent.putExtra("pid", pid)
        intent.putExtra("uid", Repository.user?.uid ?: "")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        //am.cancel(pendingIntent)
        am.set(AlarmManager.RTC_WAKEUP, amils, pendingIntent)
        Log.d("anna", "test")
    }
    fun setAlarmDedline2(context: Context, pid: String, name2: String){
        val c = Calendar.getInstance()
        // Оставляем только текущую секунду, чтобы установить время на 0 миллисекунд
        c.set(Calendar.SECOND, 0)
        val time = c.timeInMillis

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeNotification::class.java)
        intent.putExtra("message","Новое сообщение в чате")
        intent.putExtra("title",name2)
        intent.putExtra("pid", pid)
        intent.putExtra("uid", Repository.user?.uid ?: "")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        am.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    @SuppressLint("Range")
    fun getFilenameFromUri(context: Context, uri: Uri): String{
        var fileName: String? = null
        if (uri.toString().startsWith("file:")) {
            fileName = uri.path
        } else { // uri.startsWith("content:")
            val c: Cursor? = context.getContentResolver().query(uri, null, null, null, null)
            try {
                if (c != null && c.moveToFirst()) {
                    fileName = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                c?.close();
            }
            if (fileName == null) {
                fileName = uri.getPath();
                val cut = fileName?.lastIndexOf('/') ?: -1
                if (cut != -1) {
                    fileName = fileName?.substring(cut + 1);
                }
            }
        }
        return fileName ?: ""
    }

    fun downloadFileFromPath(context: Context,fileName: String, desc: String, url: String){
        val request = DownloadManager.Request(Uri.parse(url))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setTitle(fileName)
            .setDescription(desc)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName)
        val downloadManager= context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID = downloadManager.enqueue(request)
    }
}