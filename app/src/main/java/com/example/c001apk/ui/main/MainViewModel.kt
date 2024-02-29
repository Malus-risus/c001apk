package com.example.c001apk.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.adapter.Event
import com.example.c001apk.logic.network.Repository
import com.example.c001apk.util.CookieUtil
import com.example.c001apk.util.PrefManager
import kotlinx.coroutines.launch
import java.net.URLEncoder

class MainViewModel : ViewModel() {

    val badgeState = MutableLiveData<Event<Boolean>>().apply { value = Event(false) }
    var badge: Int = 0
    var isInit: Boolean = true

    fun fetchAppInfo(id: String) {
        viewModelScope.launch {
            handleAppInfoResponse(Repository.getAppInfo(id))
        }
    }

    private suspend fun handleAppInfoResponse(result: Result<AppInfo>) {
        result.getOrNull()?.data?.let { appInfoData ->
            PrefManager.apply {
                VERSION_NAME = appInfoData.apkversionname
                API_VERSION = "13"
                VERSION_CODE = appInfoData.apkversioncode.toString()
                USER_AGENT = buildUserAgent(appInfoData)
            }
        }
        handleLoginInfoResponse(Repository.checkLoginInfo())
    }

    private fun buildUserAgent(data: AppInfoData): String {
        return "Dalvik/2.1.0 (Linux; U; Android ${PrefManager.ANDROID_VERSION}; ${PrefManager.MODEL} ${PrefManager.BUILD_NUMBER}) " +
               "(#Build; ${PrefManager.BRAND}; ${PrefManager.MODEL}; ${PrefManager.BUILD_NUMBER}; ${PrefManager.ANDROID_VERSION}) " +
               "+CoolMarket/${data.apkversionname}-${data.apkversioncode}-${Constants.MODE}"
    }

    private suspend fun handleLoginInfoResponse(result: Result<LoginResponse>) {
        val loginData = result.getOrNull()?.data
        if (loginData != null && loginData.token.isNotEmpty()) {
            CookieUtil.setLoginCookies(result)
            updateBadge(loginData.notifyCount.badge)
            PrefManager.isLogin = true
            PrefManager.uid = loginData.uid
            PrefManager.username = URLEncoder.encode(loginData.username, "UTF-8")
            PrefManager.token = loginData.token
            PrefManager.userAvatar = loginData.userAvatar
            badgeState.postValue(Event(true))
        } else {
            clearLoginPreferences()
        }
    }

    private fun updateBadge(newBadge: Int) {
        badge = newBadge
        if (badge > 0) {
            badgeState.postValue(Event(true))
        }
    }

    private fun clearLoginPreferences() {
        with(PrefManager) {
            isLogin = false
            uid = ""
            username = ""
            token = ""
            userAvatar = ""
            badgeState.postValue(Event(false))
        }
    }
}
