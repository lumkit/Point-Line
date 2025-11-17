package io.github.lumkit.pline.core

import io.github.lumkit.pline.application

actual fun getPlatformPath(name: String): String = application.filesDir
    .resolve(dataStoreFileName)
    .absolutePath
