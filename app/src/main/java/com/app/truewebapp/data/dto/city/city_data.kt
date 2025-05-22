package com.app.accutecherp.data.dto.city

data class city_data(
    val CityId: String,
    val City: String,
    val StateId: String,
) {
    override fun toString(): String = City
}

