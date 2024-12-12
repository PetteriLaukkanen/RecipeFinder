package com.example.recipefinder

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("categories.php")
    suspend fun getCategories(): ApiResponse

    @GET("filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): RecipeResponse

    companion object {
        fun getApi(): ApiService = RetrofitInstance.api
    }

        @GET("lookup.php")
        suspend fun getRecipeDetails(@Query("i") idMeal: String): RecipeResponse

    @GET("random.php")
    suspend fun getRandomRecipe(): RecipeResponse
}