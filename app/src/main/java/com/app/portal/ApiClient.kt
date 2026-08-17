package com.app.portal

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("index.php?action=api_students")
    suspend fun getStudents(): StudentResponse

    @Multipart
    @POST("index.php?action=api_store")
    suspend fun addStudent(
        @Part("nis") nis: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tempat_lahir") tempatLahir: RequestBody,
        @Part("tgl_lahir") tglLahir: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("hobi") hobi: RequestBody,
        @Part("cita_cita") citaCita: RequestBody,
        @Part foto: MultipartBody.Part?
    ): ApiResponse

    @Multipart
    @POST("index.php?action=api_update")
    suspend fun updateStudent(
        @Part("id") id: RequestBody,
        @Part("nis") nis: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tempat_lahir") tempatLahir: RequestBody,
        @Part("tgl_lahir") tglLahir: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("hobi") hobi: RequestBody,
        @Part("cita_cita") citaCita: RequestBody,
        @Part foto: MultipartBody.Part?
    ): ApiResponse

    @FormUrlEncoded
    @POST("index.php?action=api_delete")
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
