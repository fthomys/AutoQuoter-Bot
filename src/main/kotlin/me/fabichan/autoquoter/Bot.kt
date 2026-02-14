package me.fabichan.autoquoter

import io.github.freya022.botcommands.api.core.JDAService
import io.github.freya022.botcommands.api.core.events.BReadyEvent
import io.github.freya022.botcommands.api.core.lightSharded
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.fabichan.autoquoter.config.Config
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.net.Proxy
import java.util.concurrent.TimeUnit

private val logger by lazy { KotlinLogging.logger {} }

private const val CONNECT_TIMEOUT = 10L
private const val READ_TIMEOUT = 10L
private const val WRITE_TIMEOUT = 10L
private const val RETRY_ON_CONNECTION_FAILURE = true
private const val MAX_IDLE_CONNECTIONS = 5
private const val KEEP_ALIVE_DURATION = 5L


@BService
class Bot(private val config: Config) : JDAService() {
    override val intents: Set<GatewayIntent> =
        defaultIntents + GatewayIntent.MESSAGE_CONTENT


    override val cacheFlags: Set<CacheFlag> = setOf(
    )

    val restConfig =
        getDefaultRestConfig().apply {
            if (config.proxyUrl != null) logger.info { "Using proxy URL: ${config.proxyUrl}" }
            baseUrl = config.proxyUrl?.let { it + "/api/v" + JDAInfo.DISCORD_REST_VERSION + "/" } ?: baseUrl
        }

    override fun createJDA(event: BReadyEvent, eventManager: IEventManager) {
        val shards = ShardHelper.getShardIdsForCurrentPod()
        lightSharded(
            config.token,
            restConfig = restConfig,
            memberCachePolicy = MemberCachePolicy.NONE,
            chunkingFilter = ChunkingFilter.NONE,
            shardsTotal = ShardHelper.getTotalShards(),
            shardRange = ShardHelper.toIntRange(shards),
        ) {
            logger.info { "Created shards: ${shards.size}" }

            setStatus(OnlineStatus.DO_NOT_DISTURB)
            setActivityProvider { Activity.playing("Booting up...") }

            if (config.proxyUrl != null) {
                setHttpClient(
                    OkHttpClient
                        .Builder()
                        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(RETRY_ON_CONNECTION_FAILURE)
                        .apply {
                            val proxyUri =
                                config.proxyUrl
                                    .let { java.net.URI.create(it) }
                            val proxy =
                                Proxy(
                                    Proxy.Type.HTTP,
                                    java.net.InetSocketAddress(proxyUri.host, proxyUri.port),
                                )
                            logger.info { "Using proxy for HTTP gateway connection: ${proxyUri.host}:${proxyUri.port}" }
                            proxy(proxy)
                        }.connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.SECONDS))
                        .build(),
                )
            }
        }
    }

}