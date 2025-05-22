package com.app.accutecherp.data.dto.state

data class state_data(
    val StateId: String,
    val State: String
) {
    override fun toString(): String = State
}