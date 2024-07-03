package com.bunny.entertainment.factoid.network;

import com.bunny.entertainment.factoid.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static Retrofit animeRetrofit = null;
    private static Retrofit animeImageRetrofit = null;
    private static Retrofit nekoBotRetrofit = null;
    private static Retrofit waifuImRetrofit = null;


    public static ApiService getApiServiceFacts() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://uselessfacts.jsph.pl/api/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static ApiService getApiServiceAnimeFacts(String waifuItAPIKey) {
        if (animeRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", waifuItAPIKey) //BuildConfig.WAIFU_DOT_IT_API_KEY
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            });
            System.out.println("API Key: " + BuildConfig.WAIFU_DOT_IT_API_KEY);

            animeRetrofit = new Retrofit.Builder()
                    .baseUrl("https://waifu.it/api/v4/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return animeRetrofit.create(ApiService.class);
    }

    public static ApiService getApiServiceAnimeImages() {
        if (animeImageRetrofit == null) {
            animeImageRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api.waifu.pics/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return animeImageRetrofit.create(ApiService.class);
    }

    public static ApiService getApiServiceNekoBot() {
        if (nekoBotRetrofit == null) {
            nekoBotRetrofit = new Retrofit.Builder()
                    .baseUrl("https://nekobot.xyz/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return nekoBotRetrofit.create(ApiService.class);
    }

    public static ApiService getApiServiceWaifuIm() {
        if (waifuImRetrofit == null) {
            waifuImRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api.waifu.im/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return waifuImRetrofit.create(ApiService.class);
    }
}
