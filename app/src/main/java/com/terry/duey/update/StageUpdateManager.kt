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
            val updateInfo = updateChecker.fetchUpdateInfo(UpdateConfig.githubReleasesUrl).getOrNull() ?: return@launch
            if (!updateChecker.isUpdateAvailable(activity, updateInfo)) return@launch

            withContext(Dispatchers.Main) {
                AlertDialog.Builder(activity)
                    .setTitle("Stage update")
                    .setMessage("Version ${updateInfo.versionName} is available.\n\n${updateInfo.releaseNotes}")
                    .setCancelable(!updateInfo.forceUpdate)
                    .setPositiveButton("Update") { _, _ ->
                        downloadAndInstall(activity, scope, updateInfo)
                    }
                    .apply {
                        if (!updateInfo.forceUpdate) {
                            setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
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
                    .setTitle("Downloading update")
                    .setMessage("Downloading APK...")
                    .setCancelable(false)
                    .show()
            }

            val apkFile = apkDownloader.download(activity, updateInfo.apkUrl).getOrNull()
            withContext(Dispatchers.Main) { loadingDialog.dismiss() }

            if (apkFile == null) {
                Toast.makeText(activity, "Failed to download update.", Toast.LENGTH_LONG).show()
                return@launch
            }

            if (!apkInstaller.canInstallPackages(activity)) {
                Toast.makeText(activity, "Allow installing unknown apps, then try again.", Toast.LENGTH_LONG).show()
                apkInstaller.openUnknownSourcesSettings(activity)
                return@launch
            }

            val installResult = apkInstaller.installApk(activity, apkFile)
            if (installResult.isFailure) {
                Toast.makeText(activity, "Could not open the install screen.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
