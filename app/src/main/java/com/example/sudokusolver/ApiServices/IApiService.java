package com.example.sudokusolver.ApiServices;

import com.example.sudokusolver.Models.SudokuGrid;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface IApiService{
//    @POST("recognize")
//    Call<SudokuGrid> getResponse();

    @Multipart
    @POST("recognize")
    Call<SudokuGrid> uploadImage(
            @Part MultipartBody.Part image
    );
}
