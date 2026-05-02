package com.terry.duey.update

import com.terry.duey.BuildConfig

object UpdateConfig {
    val isStage: Boolean = BuildConfig.IS_STAGE
    val updateCheckEnabled: Boolean = BuildConfig.UPDATE_CHECK_ENABLED
    val updateCheckUrl: String = BuildConfig.UPDATE_CHECK_URL

    fun canCheckForUpdates(): Boolean = isStage && updateCheckEnabled && updateCheckUrl.isNotBlank()
}
