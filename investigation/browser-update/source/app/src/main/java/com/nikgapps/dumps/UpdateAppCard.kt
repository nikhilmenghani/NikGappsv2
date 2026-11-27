package com.nikgapps.dumps

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException

fun getCurrentVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
} catch (_: NameNotFoundException) { "Unknown" }
