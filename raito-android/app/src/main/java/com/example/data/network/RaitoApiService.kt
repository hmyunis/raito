package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Response entities and Request bodies
data class HealthResponse(
  val ok: Boolean,
  val service: String? = null,
  val app_name: String? = null,
  val version: String? = null
)

data class RegisterDeviceRequest(
  val display_name: String,
  val device_label: String
)

data class UserDto(
  val public_id: String,
  val display_name: String?,
  val device_label: String?
)

data class ErrorDto(
  val code: String,
  val message: String
)

data class RegisterDeviceResponse(
  val ok: Boolean,
  val user: UserDto? = null,
  val device_token: String? = null,
  val error: ErrorDto? = null
)

data class UserMeDto(
  val public_id: String,
  val display_name: String?,
  val device_label: String?,
  val status: String,
  val last_seen_at: String?,
  val created_at: String?
)

data class MeResponse(
  val ok: Boolean,
  val user: UserMeDto? = null,
  val error: ErrorDto? = null
)

data class PairingDto(
  val code: String,
  val expires_in_minutes: Int,
  val instructions: String
)

data class PairingResponse(
  val ok: Boolean,
  val pairing: PairingDto? = null,
  val error: ErrorDto? = null
)

data class RemotePanelDto(
  val remote_panel_id: String,
  val source: String,
  val content: String,
  val content_hash: String,
  val created_at: String
)

data class PendingPanelsResponse(
  val ok: Boolean,
  val pending_count: Int,
  val returned_count: Int,
  val panels: List<RemotePanelDto>? = null,
  val error: ErrorDto? = null
)

data class MarkImportedRequest(
  val panel_ids: List<String>,
  val client_sync_id: String,
  val app_version: String
)

data class DiscardPanelsRequest(
  val panel_ids: List<String>
)

data class GenericResponse(
  val ok: Boolean,
  val error: ErrorDto? = null
)

interface RaitoApiService {
  @GET("api/health")
  suspend fun getHealth(): HealthResponse

  @POST("api/auth/register-device")
  suspend fun registerDevice(@Body request: RegisterDeviceRequest): RegisterDeviceResponse

  @GET("api/me")
  suspend fun checkMe(@Header("Authorization") bearerToken: String): MeResponse

  @POST("api/telegram/create-pairing-code")
  suspend fun createPairingCode(@Header("Authorization") bearerToken: String): PairingResponse

  @GET("api/telegram/pending-panels")
  suspend fun getPendingPanels(
    @Header("Authorization") bearerToken: String,
    @Query("limit") limit: Int = 50
  ): PendingPanelsResponse

  @POST("api/telegram/mark-imported")
  suspend fun markImported(
    @Header("Authorization") bearerToken: String,
    @Body request: MarkImportedRequest
  ): GenericResponse

  @POST("api/telegram/discard-panels")
  suspend fun discardPanels(
    @Header("Authorization") bearerToken: String,
    @Body request: DiscardPanelsRequest
  ): GenericResponse

  companion object {
    fun create(rawBaseUrl: String): RaitoApiService {
      // Normalize base url: must end with /
      val normalizedUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"

      val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
      }

      val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

      val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

      val retrofit = Retrofit.Builder()
        .baseUrl(normalizedUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

      return retrofit.create(RaitoApiService::class.java)
    }
  }
}
