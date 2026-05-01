package com.terry.duey.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

class ApkInstaller {
    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    fun openUnknownSourcesSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            require(packageInfo?.packageName == context.packageName) { "Package name mismatch" }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val canHandle = context.packageManager.resolveActivity(
                installIntent,
                PackageManager.MATCH_DEFAULT_ONLY,
            )
            require(canHandle != null) { "No activity available for APK install" }
            context.startActivity(installIntent)
        }.recoverCatching { throwable ->
            if (throwable is ActivityNotFoundException) {
                throw IllegalStateException("No installer app available", throwable)
            }
            throw throwable
        }
    }
}
