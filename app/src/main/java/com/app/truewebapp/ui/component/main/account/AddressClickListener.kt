package com.app.truewebapp.ui.component.main.account

interface AddressClickListener {
    fun onEditAddressClicked(
        addressId: String,
        companyName: String,
        companyAddress1: String,
        companyAddress2: String?,
        companyCity: String,
        companyCountry: String,
        companyPostcode: String
    )
}
