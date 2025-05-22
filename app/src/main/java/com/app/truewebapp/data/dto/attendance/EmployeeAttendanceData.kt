package com.app.accutecherp.data.dto.attendance

data class EmployeeAttendanceData(
    val PunchDate:String,
    val PunchTime:String,
    var PunchType:String,
    val GeoCoordinates:String
)