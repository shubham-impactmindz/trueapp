package com.app.truewebapp.utils

import java.util.regex.Pattern

object RejexUtils {
    private val EMAIL_ADDRESS: Pattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )
    fun isValidEmail(email: String): Boolean {
        return EMAIL_ADDRESS.matcher(email).matches()
    }
}