package com.app.truewebapp

//9857233201
//User@123

const val SHARED_PREF_NAME="true-web_pref"
const val BASE_URL = "https://goappadmin.zapto.org"
const val SOMETHING_WENT_WRONG = "Something went wrong please try again later!"
internal interface httpCodes {
    companion object {
      const val STATUS_OK = 200
      const val STATUS_BAD_REQUEST = 400
      const val STATUS_SESSION_EXPIRED = 401
      const val STATUS_PLAN_EXPIRED = 403
      const val STATUS_VALIDATION_ERROR = 404
      const val STATUS_SERVER_ERROR = 500
      const val STATUS_UNKNOWN_ERROR = 503
      const val STATUS_API_VALIDATION_ERROR = 422
      const val SESSION_EXPIRED = 203
    }
}