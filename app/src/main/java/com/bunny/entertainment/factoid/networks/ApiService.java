package com.bunny.entertainment.factoid.networks;


import com.bunny.entertainment.factoid.models.FactResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("facts/random")
    Call<FactResponse> getRandomFact();
}