package me.fabichan.autoquoter

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

object Environment {
    val folder: Path = System.getenv("BOT_DATA_DIR")?.let { Path(it) } ?: Path("")

    val isDev: Boolean = folder.resolve("dev-config").exists()

    val configFolder: Path = folder.resolve("config")
    val logbackConfigPath: Path = configFolder.resolve("logback.xml")
}