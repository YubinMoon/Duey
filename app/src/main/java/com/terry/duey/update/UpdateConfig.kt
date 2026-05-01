package com.terry.duey.update

import com.terry.duey.BuildConfig

object UpdateConfig {
    val isStage: Boolean = BuildConfig.IS_STAGE
    val updateCheckEnabled: Boolean = BuildConfig.UPDATE_CHECK_ENABLED
    val updateCheckUrl: String = BuildConfig.UPDATE_CHECK_URL
    val channel: String = BuildConfig.UPDATE_CHANNEL

    fun canCheckForUpdates(): Boolean {
        return isStage && updateCheckEnabled && updateCheckUrl.isNotBlank()
    }
}
