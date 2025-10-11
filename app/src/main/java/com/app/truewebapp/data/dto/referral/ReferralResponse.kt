package com.app.truewebapp.data.dto.referral

data class ReferralResponse (
    val status: Boolean,
    val message: String,
    val invite: Invite,
)

data class Invite(
    val sender_user_id: Int,
    val name: String,
    val city: String,
    val email: String,
    val referral_code: String,
    val updated_at: String,
    val created_at: String,
    val referral_invite_id: Int,
)