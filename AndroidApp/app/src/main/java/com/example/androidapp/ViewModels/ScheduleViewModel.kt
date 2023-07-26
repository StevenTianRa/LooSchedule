package com.example.androidapp.viewModels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.models.Course
import com.example.androidapp.models.Schedule
import com.example.androidapp.services.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody

data class ValidateData(
    val schedule: MutableMap<String, MutableList<Course>>,
    val academicPlan: AcademicPlan
)

data class AcademicPlan(
    val majors: List<String>,
    val startYear: String,
    val sequence: String,
    val minors: List<String>,
    val specializations: List<String>,
)

class ScheduleViewModel(input: Schedule) : ViewModel() {

    private var _schedule = MutableStateFlow(input)
    val schedule: Schedule get() = _schedule.value

    private val _termList = schedule.termSchedule.keys.toList()
    val termList : List<String> get() = _termList

    private var _currentTerm = schedule.termSchedule.keys.first()
    val currentTerm: String get() = _currentTerm

    private val _courseList = MutableStateFlow(schedule.termSchedule.getValue(schedule.termSchedule.keys.first()))
    val courseList: MutableList<Course> get() = _courseList.value

    private var _showAlert = MutableStateFlow(false)
    val showAlert: StateFlow<Boolean> get() = _showAlert

    private var _isValidated = MutableStateFlow(_schedule.value.validated)
    val isValidated: StateFlow<Boolean> get() = _showAlert

    init {
        updateCourseList()
    }

    private fun updateCourseList() {
        _courseList.value = (schedule.termSchedule[currentTerm] ?: emptyList()) as MutableList<Course>
    }

    fun onTermSelected(term: String) {
        _currentTerm = term
        _courseList.value = schedule.termSchedule[term]!!
    }


    fun validateCourseSchedule (
        schedule: Schedule,
        context: Context,
        position: Int
    ){
        val validateData = ValidateData(
            schedule = schedule.termSchedule,
            academicPlan = AcademicPlan(
                majors = schedule.degree,
                sequence = schedule.mySequence,
                minors = schedule.minor,
                specializations = schedule.specialization,
                startYear = schedule.year
            )
        )

        val gson = Gson()
        val jsonBody = gson.toJson(validateData)
        println(jsonBody)


        val api = RetrofitClient.create()
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody)

        viewModelScope.launch {
            try {
                val response = api.validateSchedule(requestBody)
                if (response.isSuccessful) {
                    val output = response.body()
                    println(output)
                    var newSchedule = schedule
                    if (output != null) {
                        newSchedule.validated = output.overallResult
                        newSchedule.courseValidation = output.courseValidationResult
                        newSchedule.degreeValidation = output.degreeValidationResult

                        _schedule.value.validated = output.overallResult
                        _schedule.value.courseValidation = output.courseValidationResult
                        _schedule.value.degreeValidation = output.degreeValidationResult
                        _isValidated.value = output.overallResult

                        val sharedPreferences = context.getSharedPreferences("MySchedules", Context.MODE_PRIVATE)
                        val existingList = sharedPreferences.getString("scheduleList", "[]")
                        val type = object : TypeToken<MutableList<Schedule>>() {}.type
                        val scheduleList : MutableList<Schedule> = Gson().fromJson(existingList, type)
                        scheduleList.removeAt(position)
                        scheduleList.add(position, newSchedule)
                        val editor = sharedPreferences.edit()
                        val jsonList = Gson().toJson(scheduleList)
                        editor.putString("scheduleList", jsonList)
                        editor.apply()
                        toggleAlert()
                    }
                }
                    else {
                    println(response.message())
                    Toast.makeText(context, response.message(), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                println(e.message)
            }
        }
//        val call = api.validateSchedule(requestBody)
//
//        call.enqueue(object: Callback<ValidationResults>{
//            override fun onResponse(
//                call: Call<ValidationResults>,
//                response: Response<ValidationResults>
//            ) {
//                if (response.isSuccessful) {
//                    val output = response.body()
//                    println(output)
//                    var newSchedule = schedule
//                    if (output != null) {
//                        newSchedule.validated = output.overallResult
//                        newSchedule.courseValidation = output.courseValidationResult
//                        newSchedule.degreeValidation = output.degreeValidationResult
//
//                        _schedule.value.validated = output.overallResult
//                        _schedule.value.courseValidation = output.courseValidationResult
//                        _schedule.value.degreeValidation = output.degreeValidationResult
//
//
//                        val sharedPreferences = context.getSharedPreferences("MySchedules", Context.MODE_PRIVATE)
//                        val existingList = sharedPreferences.getString("scheduleList", "[]")
//                        val type = object : TypeToken<MutableList<Schedule>>() {}.type
//                        val scheduleList : MutableList<Schedule> = Gson().fromJson(existingList, type)
//                        scheduleList.removeAt(position)
//                        scheduleList.add(position, newSchedule)
//                        val editor = sharedPreferences.edit()
//                        val jsonList = Gson().toJson(scheduleList)
//                        editor.putString("scheduleList", jsonList)
//                        editor.apply()
//                    }
//                } else {
//                    println(response.message())
//                    Toast.makeText(context, response.message(), Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<ValidationResults>, t: Throwable) {
//                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
//                println(t.message)
//                call.cancel()
//            }
//
//        })
    }

    fun toggleAlert() {
        _showAlert.value = !_showAlert.value
    }
}