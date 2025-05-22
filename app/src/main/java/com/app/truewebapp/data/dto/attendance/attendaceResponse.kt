package com.app.accutecherp.data.dto.attendance

import com.google.gson.annotations.SerializedName

data class attendaceResponse(
    val status:String,
    val message:String,
    @SerializedName("data")
    val attendance: attendance

)
data class attendance(
    val employee_punch_data:List<EmployeeAttendanceData>
)