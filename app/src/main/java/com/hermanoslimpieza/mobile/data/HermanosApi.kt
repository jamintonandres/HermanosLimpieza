package com.hermanoslimpieza.mobile.data

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface HermanosApi {
    @FormUrlEncoded
    @POST("mobile_api.php?action=login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    @POST("mobile_api.php?action=logout")
    suspend fun logout(): BasicResponse

    @GET("mobile_api.php?action=collaborators")
    suspend fun collaborators(): CollaboratorsResponse

    @GET("mobile_api.php?action=today_services")
    suspend fun todayServices(
        @Query("date") date: String
    ): AppointmentsResponse

    @GET("mobile_api.php?action=calendar")
    suspend fun calendar(
        @Query("start") start: String,
        @Query("end") end: String
    ): AppointmentsResponse

    @GET("mobile_api.php?action=appointment_detail")
    suspend fun appointmentDetail(
        @Query("id") id: Long
    ): AppointmentDetailResponse

    @FormUrlEncoded
    @POST("mobile_api.php?action=appointment_create")
    suspend fun createAppointment(
        @Field("client_phone") phone: String,
        @Field("client_country_code") countryCode: String,
        @Field("client_name") clientName: String,
        @Field("service_description") serviceDescription: String,
        @Field("assigned_user_id") assignedUserId: Long,
        @Field("scheduled_date") scheduledDate: String,
        @Field("time_slot") timeSlot: String,
        @Field("price") price: String,
        @Field("address") address: String,
        @Field("city") city: String,
        @Field("notes") notes: String
    ): CreateAppointmentResponse

    @GET("mobile_api.php?action=crm_chats")
    suspend fun chats(): ChatsResponse

    @GET("mobile_api.php?action=crm_messages")
    suspend fun messages(
        @Query("jid") jid: String,
        @Query("limit") limit: Int = 30
    ): MessagesResponse

    @FormUrlEncoded
    @POST("mobile_api.php?action=crm_send")
    suspend fun sendMessage(
        @Field("jid") jid: String,
        @Field("message") message: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("mobile_api.php?action=crm_analyze")
    suspend fun analyze(
        @Field("jid") jid: String
    ): AnalyzeResponse

    @FormUrlEncoded
    @POST("mobile_api.php?action=crm_mark_read")
    suspend fun markRead(
        @Field("jid") jid: String
    ): BasicResponse
}
