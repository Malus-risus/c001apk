package com.example.c001apk.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.adapter.Event
import com.example.c001apk.constant.Constants
import com.example.c001apk.logic.network.Repository
import com.example.c001apk.util.CookieUtil
import com.example.c001apk.util.PrefManager
import kotlinx.coroutines.launch
import java.net.URLEncoder

class MainViewModel : ViewModel() {

    var badge: Int = 0
    var isInit: Boolean = true
    val setBadge = MutableLiveData<Event<Boolean>>()

    fun fetchAppInfo(id: String) {
        viewModelScope.launch {
            Repository.getAppInfo(id).collect { result ->
                result.getOrNull()?.data?.let { appInfo ->
                    updatePreferences(appInfo)
                }
                getCheckLoginInfo()
            }
        }
    }

    private fun updatePreferences(appInfo: AppInfoData) {
        with(PrefManager) {
            VERSION_NAME = appInfo.apkversionname
            API_VERSION = "13"
            VERSION_CODE = appInfo.apkversioncode
            USER_AGENT = generateUserAgent(appInfo)
        }
    }

    private fun generateUserAgent(appInfo: AppInfoData): String {
        return "Dalvik/2.1.0 (Linux; U; Android ${PrefManager.ANDROID_VERSION}; ${PrefManager.MODEL} ${PrefManager.BUILD_NUMBER}) (#Build; ${PrefManager.BRAND}; ${PrefManager.MODEL}; ${PrefManager.BUILD_NUMBER}; ${PrefManager.ANDROID_VERSION}) +CoolMarket/${appInfo.apkversionname}-${appInfo.apkversioncode}-${Constants.MODE}"
    }

    private fun getCheckLoginInfo() {
        viewModelScope.launch {
            Repository.checkLoginInfo().collect { result ->
                result.getOrNull()?.body()?.let { responseBody ->
                    updateLoginInfo(responseBody)
                    updateCookies(responseBody.headers())
                    setBadgeIfNeeded()
                }
            }
        }
    }

    private fun updateLoginInfo(loginResponse: LoginResponse) {
        with(CookieUtil) {
            loginResponse.data?.token?.let { loginData ->
                badge = loginData.notifyCount.badge
                notification = loginData.notifyCount.notification
                contacts_follow = loginData.notifyCount.contactsFollow
                message = loginData.notifyCount.message
                atme = loginData.notifyCount.atme
                atcommentme = loginData.notifyCount.atcommentme
                feedlike = loginData.notifyCount.feedlike
                badge = loginData.notifyCount.badge
                PrefManager.apply {
                    isLogin = true
                    uid = loginData.uid
                    username = URLEncoder.encode(loginData.username, "UTF-8")
                    token = loginData.token
                    userAvatar = loginData.userAvatar
                }
            } ?: run {
                if (loginResponse.message == "登录信息有误") {
                    PrefManager.clearLoginInfo()
                }
            }
        }
    }

    private fun updateCookies(headers: Headers) {
        headers["Set-Cookie"]?.let { cookies ->
            cookies.firstOrNull()?.substringBefore(";")?.let { sessionID ->
                CookieUtil.SESSID = sessionID
            }
        }
    }

    private fun setBadgeIfNeeded() {
        if (badge != 0) {
            setBadge.postValue(Event(true))
        }
    }
}
