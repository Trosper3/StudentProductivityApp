package com.example.studentproductivityapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

data class CanvasTodoItem(
    val assignment: CanvasAssignment?
)

data class CanvasAssignment(
    val id: Long,
    val name: String,
    val due_at: String?,
    val course_id: Long
)

interface CanvasApiService {
    @GET("api/v1/users/self/todo")
    suspend fun getTodoItems(
        @Header("Authorization") token: String
    ): List<CanvasTodoItem>
}

//Retrofit Client configured for Idaho State University
object CanvasRetrofitClient {
    private const val BASE_URL = "https://isu.instructure.com/"

    val api: CanvasApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CanvasApiService::class.java)

    }
}