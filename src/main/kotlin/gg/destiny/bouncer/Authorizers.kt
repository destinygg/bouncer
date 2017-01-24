package gg.destiny.bouncer

import com.google.common.cache.CacheBuilder
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import rx.Observable
import rx.Single
import java.util.*
import java.util.concurrent.TimeUnit

interface Authorizer {
  class AuthFailedException : Exception()

  /** Authorizes the given player and returns their subscriber name **/
  fun authorize(playerId: UUID, playerName: String): Single<String>
}

class DggAuthorizer(val secret: String, val gson: Gson  = Gson()) : Authorizer {
  internal interface Endpoints {
    data class AuthenticationResponse(@SerializedName("end") val end: Long)
    data class ChatnameResponse(@SerializedName("nick") val nick: String, @SerializedName("end") val end: Long)

    @GET("auth/minecraft")
    fun authenticate(@Query("privatekey") privateKey: String,
                     @Query("uuid") playerId: UUID): Observable<Response<String>>

    @POST("auth/minecraft")
    fun retrieveChatName(@Query("privatekey") privateKey: String,
                         @Query("uuid") playerId: UUID,
                         @Query("name") playerName: String): Observable<Response<String>>
  }

  data class Authorization(val subscriberName: String, var subscriptionExpiration: Long, var lastCheck: Long)

  private val endpoints: Endpoints
  private val authorizationCache = CacheBuilder.newBuilder()
    .concurrencyLevel(16)
    .maximumSize(1000)
    .expireAfterWrite(24, TimeUnit.HOURS)
    .build<UUID, Authorization>()

  init {
    val retrofit = Retrofit.Builder()
      .baseUrl("https://www.destiny.gg")
      .addConverterFactory(ScalarsConverterFactory.create())
      .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
      .build()
    endpoints = retrofit.create(Endpoints::class.java)
  }

  override fun authorize(playerId: UUID, playerName: String): Single<String> {
    val now = System.currentTimeMillis()
    val cachedAuthorization = authorizationCache.getIfPresent(playerId)
    cachedAuthorization?.let {
      if (it.lastCheck + TimeUnit.HOURS.toMillis(1) < now || it.subscriptionExpiration < now) {
        return endpoints.authenticate(secret, playerId)
          .map { response ->
            if (!response.isSuccessful) {
              authorizationCache.invalidate(playerId)
              throw Authorizer.AuthFailedException()
            }

            // Update the cache
            val authResponse = gson.fromJson(response.body(), Endpoints.AuthenticationResponse::class.java)
            it.subscriptionExpiration = authResponse.end
            it.lastCheck = now
            authorizationCache.put(playerId, it)

            return@map it.subscriberName
          }
          .toSingle()
      } else {
        return Single.just(it.subscriberName)
      }
    }

    return endpoints.retrieveChatName(secret, playerId, playerName)
      .map { response ->
        if (!response.isSuccessful) {
          throw Authorizer.AuthFailedException()
        }

        val chatnameResponse = gson.fromJson(response.body(), Endpoints.ChatnameResponse::class.java)
        val end = chatnameResponse.end
        if (end < now) {
          // Expired subscription
          throw Authorizer.AuthFailedException()
        }

        val subNick = chatnameResponse.nick
        authorizationCache.put(playerId, Authorization(subNick, end, now))
        return@map subNick
      }
      .toSingle()
  }
}
