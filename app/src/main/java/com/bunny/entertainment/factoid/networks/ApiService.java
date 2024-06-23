package com.bunny.entertainment.factoid.networks;


import com.bunny.entertainment.factoid.models.AnimeFactResponse;
import com.bunny.entertainment.factoid.models.AnimeImageResponse;
import com.bunny.entertainment.factoid.models.FactResponse;
import com.bunny.entertainment.factoid.models.NekoBotImageResponse;

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
    Call<AnimeImageResponse> getAnimeImage(@Path("category") String category);

    @GET("api/image")
    Call<NekoBotImageResponse> getNekoBotImage(@Query("type") String type);
}