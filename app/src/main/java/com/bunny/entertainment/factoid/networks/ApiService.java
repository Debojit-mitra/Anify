package com.bunny.entertainment.factoid.networks;


import com.bunny.entertainment.factoid.models.AnimeFactResponse;
import com.bunny.entertainment.factoid.models.FactResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface ApiService {
    @GET("facts/random")
    Call<FactResponse> getRandomFact();

    @Headers("Authorization: NDQ4NTI2NTQxNDA0NzY2MjA5.MTcxOTA3MDc0Mw--.1a2c284af86")
    @GET("fact")
    Call<AnimeFactResponse> getAnimeFact();
}