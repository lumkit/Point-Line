package io.github.lumkit.pline.core

import java.io.File

actual fun getPlatformPath(name: String): String {
    val file = File(System.getProperty("java.io.tmpdir"), name)
    return file.absolutePath
}
