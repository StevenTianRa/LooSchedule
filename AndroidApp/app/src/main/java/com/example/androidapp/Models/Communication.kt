package com.example.androidapp.Models

import com.google.gson.annotations.SerializedName

class Communication {
    @SerializedName("courseId")
    var courseId : String = ""

    @SerializedName("subject")
    var subject : String = ""

    @SerializedName("code")
    var code : String = ""

    @SerializedName("listNumber")
    var listNumber : Int = 0

    @SerializedName("year")
    var year : String = ""
}