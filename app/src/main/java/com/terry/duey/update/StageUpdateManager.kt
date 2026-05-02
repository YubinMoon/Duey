package com.terry.duey.update

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StageUpdateManager(
    private val updateChecker: UpdateChecker = UpdateChecker(),
    private val apkDownloader: ApkDownloader = ApkDownloader(),
    private val apkInstaller: ApkInstaller = ApkInstaller(),
) {
    fun checkAndPrompt(activity: Activity, scope: CoroutineScope) {
        if (!UpdateConfig.canCheckForUpdates()) return

        scope.launch {
            val updateInfo = updateChecker.fetchUpdateInfo(UpdateConfig.updateCheckUrl).getOrNull() ?: return@launch
            if (!updateChecker.isUpdateAvailable(activity, updateInfo)) return@launch

            withContext(Dispatchers.Main) {
                AlertDialog.Builder(activity)
                    .setTitle("새 Stage 업데이트")
                    .setMessage("버전 ${updateInfo.versionName} 이(가) 있습니다.\n\n${updateInfo.releaseNotes}")
                    .setCancelable(!updateInfo.forceUpdate)
                    .setPositiveButton("업데이트") { _, _ ->
                        downloadAndInstall(activity, scope, updateInfo)
                    }
                    .apply {
                        if (!updateInfo.forceUpdate) {
                            setNegativeButton("나중에") { dialog, _ -> dialog.dismiss() }
                        }
                    }
                    .show()
            }
        }
    }

    private fun downloadAndInstall(activity: Activity, scope: CoroutineScope, updateInfo: UpdateInfo) {
        scope.launch {
            val loadingDialog = withContext(Dispatchers.Main) {
                AlertDialog.Builder(activity)
                    .setTitle("업데이트 다운로드")
                    .setMessage("APK를 다운로드하는 중입니다...")
                    .setCancelable(false)
                    .show()
            }

            val apkFile = apkDownloader.download(activity, updateInfo.apkUrl).getOrNull()
            withContext(Dispatchers.Main) { loadingDialog.dismiss() }

            if (apkFile == null) {
                Toast.makeText(activity, "업데이트 다운로드에 실패했습니다.", Toast.LENGTH_LONG).show()
                return@launch
            }

            if (!apkInstaller.canInstallPackages(activity)) {
                Toast.makeText(activity, "알 수 없는 앱 설치 권한을 허용해 주세요.", Toast.LENGTH_LONG).show()
                apkInstaller.openUnknownSourcesSettings(activity)
                return@launch
            }

            val installResult = apkInstaller.installApk(activity, apkFile)
            if (installResult.isFailure) {
                Toast.makeText(activity, "설치 화면을 열 수 없습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
