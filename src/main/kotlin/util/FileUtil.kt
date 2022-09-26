package util

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun swapAndWrite(target: String, content: String) {
    val old = "${target}_old"
    val cur = "${target}_new"
    val file = File(cur).also { it.parentFile.mkdirs() }
        .also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
    file.writeBytes(content.toByteArray())
    if (File(target).exists()) {
        Files.move(Paths.get(target), Paths.get(old), StandardCopyOption.REPLACE_EXISTING)
    }
    Files.move(Paths.get(cur), Paths.get(target), StandardCopyOption.ATOMIC_MOVE)
}