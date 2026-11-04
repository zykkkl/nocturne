package com.nocturne.learn.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 读取已安装包的 versionName，避免为了版本号打开 BuildConfig 生成（release 构建不需要 Java 编译）。 */
@Composable
fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
}
