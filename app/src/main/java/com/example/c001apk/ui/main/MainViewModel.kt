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

    val badgeCount = MutableLiveData<Int>()
    val isLoginChanged = MutableLiveData<Event<Boolean>>()

    init {
        badgeCount.value = 0
    }

    fun fetchAppInfo(id: String) {
        viewModelScope.launch {
            val result = Repository.getAppInfo(id)
            val appInfo = result.getOrNull()?.data
            appInfo?.let {
                updatePrefManagerWithAppInfo(it)
                getCheckLoginInfo()
            }
        }
    }

    private fun updatePrefManagerWithAppInfo(appInfo: AppInfoData) {
        // for brevity, assuming these are properties of AppInfoData and PrefManager
        PrefManager.VERSION_NAME = appInfo.apkversionname
        PrefManager.API_VERSION = "13"
        PrefManager.VERSION_CODE = appInfo.apkversioncode
        PrefManager.USER_AGENT = buildUserAgent(appInfo)
    }

    private fun buildUserAgent(appInfo: AppInfoData): String {
        // Assuming constants and properties for versions and device info are set in PrefManager
        return "Dalvik/2.1.0 (Linux; U; Android ${PrefManager.ANDROID_VERSION}; " +
               "${PrefManager.MODEL} ${PrefManager.BUILDNUMBER}) (#Build; " +
               "${PrefManager.BRAND}; ${PrefManager.MODEL}; ${PrefManager.BUILDNUMBER}; " +
               "${PrefManager.ANDROID_VERSION}) " +
               "+CoolMarket/${appInfo.apkversionname}-${appInfo.apkversioncode}-${Constants.MODE}"
    }

    private fun getCheckLoginInfo() {
        viewModelScope.launch {
            val result = Repository.checkLoginInfo()
            val loginResponse = result.getOrNull()?.body()
            loginResponse?.let {
                if (it.data?.token != null) {
                    updateBadge(it.data.notifyCount.badge)
                    updateLoginPreferences(it.data)
                    CookieUtil.updateCookies(it)
                    triggerBadgeEvent()
                } else if (it.message == "登录信息有误") {
                    clearLoginPreferences()
                }
            }
        }
    }

    private fun updateBadge(newBadge: Int) {
        badgeCount.value = newBadge
    }

    private fun updateLoginPreferences(loginData: LoginData) {
        with(PrefManager) {
            isLogin = true
            uid = loginData.uid
            username = URLEncoder.encode(loginData.username, "UTF-8")
            token = loginData.token
            userAvatar = loginData.userAvatar
        }
    }

    private fun clearLoginPreferences() {
        PrefManager.clearLoginInfo()
        isLoginChanged.postValue(Event(false))
    }

    private fun triggerBadgeEvent() {
        badgeCount.value?.let {
            if (it > 0) {
                isLoginChanged.postValue(Event(true))
            }
        }
    }
}
