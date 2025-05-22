package com.app.accutecherp.data.dto.clientstatus

data class client_status(
    val ClientStatusId: String,
    val ClientStatus: String,
    val IsActive: String
){
    override fun toString(): String = ClientStatus
}
