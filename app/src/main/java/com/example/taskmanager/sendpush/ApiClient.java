package com.example.taskmanager.sendpush;
import android.util.Log;

import com.example.taskmanager.BuildConfig;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//класс для работы с отправкой пуш уведомлений в чате через firebase cloud messaging
public class ApiClient {
    public static NotificationApiService getApiService() {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.FCM_BASE_URL)
                .client(provideClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NotificationApiService.class);
    }
    private static OkHttpClient provideClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(chain -> {
            Request request = chain.request();
            return chain.proceed(request);
        }).build();
    }
    private static JsonObject buildNotificationPayload(String token, String title, String text) {
        // compose notification json payload

        JsonObject payload = new JsonObject();
        payload.addProperty("to", token);
        payload.addProperty("mutable-content", "true");
        // compose data payload here
        JsonObject data = new JsonObject();
        data.addProperty("title", title);
        data.addProperty("body", text);
        payload.add("data", data);
        JsonObject notification = new JsonObject();
        notification.addProperty("title", title);
        notification.addProperty("body", text);
        notification.addProperty("sound", "default");
        payload.add("notification", notification);
        return payload;
    }
    public static void sendPush(String token, String title, String text){
        JsonObject payload = buildNotificationPayload(token, title, text);
        // send notification to receiver ID
        ApiClient.getApiService().sendNotification(payload).enqueue(
                new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.d("anna", "push send");
                        }
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                    }
                });
    }
}
