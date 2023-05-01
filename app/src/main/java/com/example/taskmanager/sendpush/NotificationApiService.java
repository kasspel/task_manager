package com.example.taskmanager.sendpush;
import com.example.taskmanager.BuildConfig;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

//интерфейс к класу отправки пушей (смотреть документацию к retrofit 2)
public interface NotificationApiService {
    @Headers({
            "Authorization: key="+ BuildConfig.FCM_SERVER_KEY ,
            "Content-Type: application/json"
    })
    @POST("fcm/send")
    Call<JsonObject> sendNotification(@Body JsonObject payload);
}