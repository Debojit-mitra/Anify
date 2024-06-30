package com.bunny.entertainment.anify.network;


import com.bunny.entertainment.anify.models.AnimeFactResponse;
import com.bunny.entertainment.anify.models.AnimeImageResponse;
import com.bunny.entertainment.anify.models.FactResponse;
import com.bunny.entertainment.anify.models.NekoBotImageResponse;
import com.bunny.entertainment.anify.models.WaifuImResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("facts/random")
    Call<FactResponse> getRandomFact();

    @GET("fact")
    Call<AnimeFactResponse> getAnimeFact();

    @GET("sfw/{category}")
    Call<AnimeImageResponse> getSfwImageWaifuPics(@Path("category") String category);

    @GET("nsfw/{category}")
    Call<AnimeImageResponse> getNsfwImageWaifuPics(@Path("category") String category);

    @GET("api/image")
    Call<NekoBotImageResponse> getNekoBotImage(@Query("type") String type);

    @GET("search")
    Call<WaifuImResponse> getWaifuImImage(@Query("included_tags") String tag, @Query("byte_size") String byteSize);
}