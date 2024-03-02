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
import java.io.IOException

internal class AddCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val deviceCode = getLastingDeviceCode()
        val token = getTokenV2(deviceCode)

        val builder = originalRequest.newBuilder()

        builder.addHeader("User-Agent", PrefManager.USER_AGENT)
        builder.addHeader("X-Requested-With", REQUEST_WITH)
        builder.addHeader("X-Sdk-Int", PrefManager.SDK_INT)
        builder.addHeader("X-Sdk-Locale", LOCALE)
        builder.addHeader("X-App-Id", APP_ID)
        builder.addHeader("X-App-Token", token)
        builder.addHeader("X-App-Version", PrefManager.VERSION_NAME)
        builder.addHeader("X-App-Code", PrefManager.VERSION_CODE)
        builder.addHeader("X-Api-Version", PrefManager.API_VERSION)
        builder.addHeader("X-App-Device", deviceCode)
        builder.addHeader("X-Dark-Mode", if (DARK_MODE) "1" else "0")
        builder.addHeader("X-App-Channel", CHANNEL)
        builder.addHeader("X-App-Mode", if (MODE) "1" else "0")
        builder.addHeader("X-App-Supported", PrefManager.VERSION_CODE)
        builder.addHeader("Content-Type", "application/x-www-form-urlencoded")

        if (PrefManager.isLogin) {
            builder.addHeader("Cookie", "uid=${PrefManager.uid}; username=${PrefManager.username}; token=${PrefManager.token}")
        } else {
            builder.addHeader("Cookie", SESSID)
        }

        return chain.proceed(builder.build())
    }
}
