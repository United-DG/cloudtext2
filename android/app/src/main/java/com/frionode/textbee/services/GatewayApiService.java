package com.frionode.textbee.services;

import com.frionode.textbee.dtos.MessagesResponse;
import com.frionode.textbee.dtos.SMSDTO;
import com.frionode.textbee.dtos.SMSForwardResponseDTO;
import com.frionode.textbee.dtos.RegisterDeviceInputDTO;
import com.frionode.textbee.dtos.RegisterDeviceResponseDTO;
import com.frionode.textbee.dtos.HeartbeatInputDTO;
import com.frionode.textbee.dtos.HeartbeatResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GatewayApiService {
    @POST("gateway/devices")
    Call<RegisterDeviceResponseDTO> registerDevice(@Header("x-api-key") String apiKey, @Body() RegisterDeviceInputDTO body);

    @PATCH("gateway/devices/{deviceId}")
    Call<RegisterDeviceResponseDTO> updateDevice(@Path("deviceId") String deviceId, @Header("x-api-key") String apiKey, @Body() RegisterDeviceInputDTO body);

    @POST("gateway/devices/{deviceId}/receive-sms")
    Call<SMSForwardResponseDTO> sendReceivedSMS(@Path("deviceId") String deviceId, @Header("x-api-key") String apiKey, @Body() SMSDTO body);

    @PATCH("gateway/devices/{deviceId}/sms-status")
    Call<SMSForwardResponseDTO> updateSMSStatus(@Path("deviceId") String deviceId, @Header("x-api-key") String apiKey, @Body() SMSDTO body);

    @POST("gateway/devices/{deviceId}/heartbeat")
    Call<HeartbeatResponseDTO> heartbeat(@Path("deviceId") String deviceId, @Header("x-api-key") String apiKey, @Body() HeartbeatInputDTO body);

    @GET("gateway/devices/{deviceId}/messages")
    Call<MessagesResponse> getMessages(@Path("deviceId") String deviceId, @Header("x-api-key") String apiKey, @Query("page") int page, @Query("limit") int limit, @Query("type") String type);
}