package com.bunny.entertainment.factoid.networks;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static Retrofit animeRetrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://uselessfacts.jsph.pl/api/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
    public static ApiService getApiServiceAnimeFacts() {
        if (animeRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", "NDQ4NTI2NTQxNDA0NzY2MjA5.MTcxOTA3MDc0Mw--.1a2c284af86")
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            });

            animeRetrofit = new Retrofit.Builder()
                    .baseUrl("https://waifu.it/api/v4/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return animeRetrofit.create(ApiService.class);
    }
}
