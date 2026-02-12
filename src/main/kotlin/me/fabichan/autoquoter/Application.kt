package me.fabichan.autoquoter

import io.github.freya022.botcommands.api.core.BotCommands
import io.github.freya022.botcommands.api.core.config.DevConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import me.fabichan.autoquoter.config.Config
import java.lang.management.ManagementFactory
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.system.exitProcess
import ch.qos.logback.classic.ClassicConstants as LogbackConstants

private val logger by lazy { KotlinLogging.logger {} }

private const val mainPackageName = "me.fabichan.autoquoter"

object Main {
    @JvmStatic
    fun main(args: Array<out String>) {

        try {
            if (Environment.logbackConfigPath.exists()) {
                System.setProperty(
                    LogbackConstants.CONFIG_FILE_PROPERTY,
                    Environment.logbackConfigPath.absolutePathString()
                )
                logger.info { "Loading logback configuration at ${Environment.logbackConfigPath.absolutePathString()}" }
            }
            logger.info { "Running on Java ${System.getProperty("java.version")}" }

            val config = Config.instance
            if (Environment.isDev) {
                logger.warn { "Running in development mode" }
            }

            BotCommands.create {
                if (Environment.isDev) {
                    disableExceptionsInDMs = true
                    disableExceptionsInDMs = true
                }

                addPredefinedOwners(*config.ownerIds.toLongArray())

                addSearchPath(mainPackageName)

                applicationCommands {
                    @OptIn(DevConfig::class)
                    disableAutocompleteCache = Environment.isDev

                    fileCache {
                        @OptIn(DevConfig::class)
                        checkOnline = !Environment.isDev
                    }

                    testGuildIds += config.testGuildIds
                }

                components {
                    enable = true
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "Unable to start the bot" }
            exitProcess(1)
        }
    }
}
