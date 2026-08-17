package com.app.portal

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("api/get_students.php")
    suspend fun getStudents(): StudentResponse

    @Multipart
    @POST("api/create_student.php")
    suspend fun addStudent(
        @Part("nis") nis: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("tanggal_lahir") tglLahir: RequestBody,
        @Part foto: MultipartBody.Part?
    ): ApiResponse

    @Multipart
    @POST("api/update_student.php")
    suspend fun updateStudent(
        @Part("id") id: RequestBody,
        @Part("nis") nis: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("tanggal_lahir") tglLahir: RequestBody,
        @Part foto: MultipartBody.Part?
    ): ApiResponse

    @FormUrlEncoded
    @POST("api/delete_student.php")
    suspend fun deleteStudent(@Field("id") id: String): ApiResponse
}

object DynamicRetrofitClient {
    fun getService(baseUrl: String): ApiService {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
