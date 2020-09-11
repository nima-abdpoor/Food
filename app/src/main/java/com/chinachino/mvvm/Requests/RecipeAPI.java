package com.chinachino.mvvm.Requests;

import com.chinachino.mvvm.Requests.Responses.RecipeSearchResponse;
import com.chinachino.mvvm.models.Recipe;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RecipeAPI {
    @GET("api/search")
    Call<RecipeSearchResponse> searchResponses(
            @Query("q") String q,
            @Query("page") String page
    );

    @GET("api/get")
    Call<Recipe> getRecipeResponses(
            @Query("rid") String id
    );

}
