package com.example.c001apk.util

import com.example.c001apk.constant.Constants.APP_ID
import com.example.c001apk.constant.Constants.CHANNEL
import com.example.c001apk.constant.Constants.DARK_MODE
import com.example.c001apk.constant.Constants.LOCALE
import com.example.c001apk.constant.Constants.MODE
import com.example.c001apk.constant.Constants.REQUEST_WITH
import com.example.c001apk.util.CookieUtil.SESSID
import com.example.c001apk.util.TokenDeviceUtils.getLastingDeviceCode
import com.example.c001apk.util.TokenDeviceUtils.getTokenV2
import okhttp3.Interceptor
import okhttp3.Response

internal class AddCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val deviceCode = getLastingDeviceCode()
        val token = getTokenV2(deviceCode)
        val prefManager = PrefManager
        val userCookie = if (prefManager.isLogin) {
            "uid=${prefManager.uid}; username=${prefManager.username}; token=${prefManager.token}"
        } else {
            SESSID
        }

        val request = originalRequest.newBuilder().apply {
            addHeader("User-Agent", prefManager.USER_AGENT)
            addHeader("X-Requested-With", REQUEST_WITH)
            addHeader("X-Sdk-Int", prefManager.SDK_INT)
            addHeader("X-Sdk-Locale", LOCALE)
            addHeader("X-App-Id", APP_ID)
            addHeader("X-App-Token", token)
            addHeader("X-App-Version", prefManager.VERSION_NAME)
            addHeader("X-App-Code", prefManager.VERSION_CODE)
            addHeader("X-Api-Version", prefManager.API_VERSION)
            addHeader("X-App-Device", deviceCode)
            addHeader("X-Dark-Mode", DARK_MODE.toString())
            addHeader("X-App-Channel", CHANNEL)
            addHeader("X-App-Mode", MODE.toString())
            addHeader("X-App-Supported", prefManager.VERSION_CODE)
            addHeader("Content-Type", "application/x-www-form-urlencoded")
            addHeader("Cookie", userCookie)
        }.build()

        return chain.proceed(request)
    }
}
