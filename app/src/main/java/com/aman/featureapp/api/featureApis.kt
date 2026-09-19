package com.aman.featureapp.api

import com.aman.featureapp.requests.feature.ClarifyFeatureRequest
import com.aman.featureapp.requests.feature.CreateFeatureRequest
import com.aman.featureapp.requests.feature.EditFeatureRequest
import com.aman.featureapp.requests.feature.UpdateFeatureRequest
import com.aman.featureapp.responses.Response
import com.aman.featureapp.responses.feature.FeatureItem
import com.aman.featureapp.responses.feature.FeatureStatusResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface featureApis {

    @POST("features/create")
    suspend fun createFeature(
        @Body request: CreateFeatureRequest
    ): Response<FeatureItem>

    @GET("features/")
    suspend fun listFeatures(): Response<List<FeatureItem>>

    @GET("features/{uid}")
    suspend fun getFeature(
        @Path("uid") uid: String
    ): Response<FeatureItem>

    @PUT("features/{uid}")
    suspend fun updateFeature(
        @Path("uid") uid: String,
        @Body request: UpdateFeatureRequest
    ): Response<FeatureItem>

    @DELETE("features/{uid}")
    suspend fun deleteFeature(
        @Path("uid") uid: String
    ): Response<Any?>

    @GET("features/{uid}/status")
    suspend fun getFeatureStatus(
        @Path("uid") uid: String
    ): Response<FeatureStatusResponse>

    @POST("features/{uid}/clarify")
    suspend fun clarifyFeature(
        @Path("uid") uid: String,
        @Body request: ClarifyFeatureRequest
    ): Response<FeatureItem>

    @POST("features/{uid}/edit")
    suspend fun editFeature(
        @Path("uid") uid: String,
        @Body request: EditFeatureRequest
    ): Response<FeatureItem>

    @POST("features/{uid}/clear-storage")
    suspend fun requestClearStorage(
        @Path("uid") uid: String
    ): Response<Any?>

    @POST("features/{uid}/ack-cleared")
    suspend fun ackStorageCleared(
        @Path("uid") uid: String
    ): Response<Any?>
}
