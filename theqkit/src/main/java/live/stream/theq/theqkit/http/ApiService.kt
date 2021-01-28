package live.stream.theq.theqkit.http

import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.Deferred
import live.stream.theq.theqkit.data.app.CreateSubscriptionResponse
import live.stream.theq.theqkit.data.app.SubscriptionCreatePayload
import live.stream.theq.theqkit.data.app.SubscriptionResponse
import live.stream.theq.theqkit.data.sdk.AuthResponse
import live.stream.theq.theqkit.data.app.Device
import live.stream.theq.theqkit.data.app.FeedResponse
import live.stream.theq.theqkit.data.sdk.GameListResponse
import live.stream.theq.theqkit.data.sdk.LoginAuthData
import live.stream.theq.theqkit.data.sdk.SeasonResponse
import live.stream.theq.theqkit.data.sdk.SeasonScoreResponse
import live.stream.theq.theqkit.data.sdk.SignupAuthData
import live.stream.theq.theqkit.data.sdk.SubmitAnswerResponse
import live.stream.theq.theqkit.data.sdk.SuccessResponse
import live.stream.theq.theqkit.data.sdk.UserResponse
import live.stream.theq.theqkit.data.sdk.UserUpdateRequest
import retrofit2.adapter.rxjava2.Result
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.HEAD
import retrofit2.http.Query
import java.util.UUID


/** @suppress */
interface ApiService {

  // Coroutines

  @GET("games?limit=50&includeSubscriberOnly=true&gameTypes=TRIVIA,POPULAR")
  fun scheduledGamesAsync(
    @Query("userId") userId: String? = null,
    @Query("partnerCode") partnerCode: String? = null): Deferred<Response<GameListResponse>>

  @GET("test-games?&gameTypes=TRIVIA,POPULAR")
  fun scheduledTestGamesAsync(
    @Query("partnerCode") partnerCode: String? = null,
    @Query("offset") offset: Int = 0,
    @Query("limit") limit: Int = 25): Deferred<Response<GameListResponse>>

  @POST("oauth/token")
  fun partnerLoginAsync(
    @Query("partnerCode") partnerCode: String,
    @Body authData: LoginAuthData
  ): Deferred<Response<AuthResponse>>

  @POST("users")
  fun parterSignupAsync(
    @Query("partnerCode") partnerCode: String,
    @Body authData: SignupAuthData
  ): Deferred<Response<AuthResponse>>

  @DELETE("oauth/token")
  fun logoutAsync(): Deferred<Response<SuccessResponse>>

  @GET("users/{userId}")
  fun fetchUserAsync(@Path("userId") userId: String): Deferred<Response<UserResponse>>

  @HEAD("users/{username}")
  fun usernameCheckAsync(@Path("username") query: String): Deferred<Response<Void>>

  @PUT("users/{userId}")
  fun updateUserAsync(
    @Path("userId") userId: String,
    @Body userUpdateRequest: UserUpdateRequest
  ): Deferred<Response<UserResponse>>

  @POST("games/{gameId}/questions/{questionId}/responses")
  fun submitAnswerAsync(
    @Path("gameId") gameId: UUID,
    @Path("questionId") questionId: UUID,
    @Query("response") response: String,
    @Query("useHeart") useHeart: Boolean
  ): Deferred<Response<SubmitAnswerResponse>>

  @PUT("users/{userId}/withdrawal-request")
  fun cashoutRequestAsync(@Path("userId") userId: String): Deferred<Response<SuccessResponse>>

  // Rx

  @GET("season")
  fun seasons(
    @Query("includeCategories") includeCategories: String,
    @Query("includeLeaderboards") includeLeaderboards: String
  ): Observable<Result<SeasonResponse>>

  @GET("season")
  fun seasonAsync(
    @Query("includeCategories") includeCategories: Boolean,
    @Query("includeLeaderboards") includeLeaderboards: Boolean,
    @Query("partnerCode") partnerCode: String? = null
  ): Deferred<Response<SeasonResponse>>

  @POST("users")
  fun signup(
    @Query("referralCode") referralCode: String?,
    @Query("partnerCode") partnerCode: String? = null,
    @Body authData: SignupAuthData
  ): Observable<Result<AuthResponse>>

  @POST("oauth/token")
  fun login(
    @Body authData: LoginAuthData,
    @Query("partnerCode") partnerCode: String? = null
  ): Observable<Result<AuthResponse>>

  @DELETE("oauth/token")
  fun logout(): Observable<Result<SuccessResponse>>

  @HEAD("users/{username}")
  fun checkUsernameAvailability(@Path("username") username: String): Call<Void>

  @GET("users/{userId}")
  fun findUserById(@Path("userId") userId: String): Observable<Result<UserResponse>>

  @PUT("users/{userId}")
  fun updateUser(
    @Path("userId") userId: String,
    @Body userUpdateRequest: UserUpdateRequest
  ): Observable<Result<UserResponse>>

  @GET("users/activity")
  fun activity(
    @Query("after") after: Long? = null,
    @Query("before") before: Long? = null,
    @Query("limit") limit: Int = 50
  ): Single<FeedResponse>

  @GET("users/activity/{itemId}/children")
  fun activityItemChildren(@Path("itemId") itemId: String): Single<FeedResponse>

  @PUT("users/{userId}/withdrawal-request")
  fun withdrawalRequest(@Path("userId") userId: String): Observable<Result<SuccessResponse>>

  @POST("users/device")
  fun registerDevice(@Body device: Device): Observable<Result<SuccessResponse>>

  @GET("category/scores")
  fun userSeasonScore(): Observable<Result<SeasonScoreResponse>>

  @GET("users/subscription")
  fun userSubscription(): Observable<Result<SubscriptionResponse>>

  @POST("users/subscription")
  fun createSubscription(
    @Body subscriptionCreatePayload: SubscriptionCreatePayload
  ): Observable<Result<CreateSubscriptionResponse>>
}