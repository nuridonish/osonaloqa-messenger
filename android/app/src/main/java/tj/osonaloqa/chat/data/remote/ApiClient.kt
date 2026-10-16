package tj.osonaloqa.chat.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

interface OsonApi {
    @retrofit2.http.POST("v1/auth/request-otp")
    suspend fun requestOtp(@retrofit2.http.Body body: RequestOtpReq): RequestOtpRes

    @retrofit2.http.POST("v1/auth/verify")
    suspend fun verify(@retrofit2.http.Body body: VerifyReq): VerifyRes

    @retrofit2.http.POST("v1/auth/refresh")
    suspend fun refresh(@retrofit2.http.Body body: RefreshReq): RefreshRes

    @retrofit2.http.GET("v1/chats")
    suspend fun chats(@retrofit2.http.Header("Authorization") auth: String): ChatsRes

    @retrofit2.http.POST("v1/chats")
    suspend fun createChat(@retrofit2.http.Header("Authorization") auth: String, @retrofit2.http.Body body: CreateChatReq): CreateChatRes

    @retrofit2.http.GET("v1/chats/{chatId}/messages")
    suspend fun messages(
        @retrofit2.http.Header("Authorization") auth: String,
        @retrofit2.http.Path("chatId") chatId: String,
        @retrofit2.http.Query("limit") limit: Int = 100,
    ): MessagesRes

    @retrofit2.http.GET("v1/statuses")
    suspend fun statuses(@retrofit2.http.Header("Authorization") auth: String): StatusesRes

    @retrofit2.http.POST("v1/statuses")
    suspend fun createStatus(@retrofit2.http.Header("Authorization") auth: String, @retrofit2.http.Body body: CreateStatusReq): StatusesRes

    @retrofit2.http.POST("v1/users/lookup")
    suspend fun lookup(@retrofit2.http.Header("Authorization") auth: String, @retrofit2.http.Body body: LookupReq): LookupRes

    @retrofit2.http.PATCH("v1/users/me")
    suspend fun updateProfile(@retrofit2.http.Header("Authorization") auth: String, @retrofit2.http.Body body: UpdateProfileReq): UpdateProfileRes

    @retrofit2.http.POST("v1/chats/{chatId}/read")
    suspend fun markRead(
        @retrofit2.http.Header("Authorization") auth: String,
        @retrofit2.http.Path("chatId") chatId: String,
        @retrofit2.http.Body body: ReadReq,
    ): Map<String, Boolean>
}

@kotlinx.serialization.Serializable
data class UpdateProfileReq(val name: String, val about: String, val username: String? = null)

@kotlinx.serialization.Serializable
data class UpdateProfileRes(val user: tj.osonaloqa.chat.data.remote.UserDto)

@kotlinx.serialization.Serializable
data class ReadReq(val seq: Long)

@kotlinx.serialization.Serializable
data class LookupReq(val phones: List<String>)

@kotlinx.serialization.Serializable
data class LookupRes(val users: List<UserDto>)

object ApiClient {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    fun create(baseUrl: String, client: OkHttpClient): OsonApi {
        val clean = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(clean)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OsonApi::class.java)
    }
}
