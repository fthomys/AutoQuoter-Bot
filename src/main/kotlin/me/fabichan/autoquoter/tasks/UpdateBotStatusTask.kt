package me.fabichan.autoquoter.tasks

import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.fabichan.autoquoter.config.Config
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger { }

@BService
class UpdateBotStatusTask(private val database: Database, private val metrics: me.fabichan.autoquoter.Metrics) {
    private val updateBotStatusScope = namedDefaultScope("UpdateBotStatus", 1)
    private val started = AtomicBoolean(false)

    @BEventListener
    fun onShardReady(event: ReadyEvent) {
        if (started.getAndSet(true)) return
        val shardManager = event.jda.shardManager ?: return
        updateBotStatusScope.launch {
            logger.info { "Starting bot status update task" }
            while (true) {
                try {
                    updateGlobalStats(shardManager)
                } catch (e: Exception) {
                    logger.error(e) { "Error updating bot status" }
                }

                delay(2.minutes)
            }
        }
    }

    private suspend fun updateGlobalStats(shardManager: ShardManager) {
        val quoteCountInt = getQuoteCount()
        metrics.updateQuoteCount(quoteCountInt)
        val guilds = shardManager.guildCache.size().toInt()
        metrics.updateGuildCount(guilds)

        val quoteCount = "${quoteCountInt}x"
        val status = if (guilds > 0) OnlineStatus.ONLINE else OnlineStatus.IDLE

        for (shard in shardManager.shards) {
            metrics.updateShardPing(shard.shardInfo.shardId, shard.gatewayPing)
            val activity = Activity.customStatus("Quoted $quoteCount | on Shard ${shard.shardInfo.shardId} • ${shard.gatewayPing}ms ping")
            shard.presence.setPresence(status, activity, false)
        }
    }

    private fun createActivity(activityText: String): Activity =
        Activity.customStatus(activityText)


    private suspend fun getQuoteCount(): Int {
        return database.preparedStatement("SELECT COUNT(*) FROM qoutestats") {
            executeQuery().use { rs ->
                rs.next()
                val i = rs.getInt(1)
                Config.Constants.quotes = i.toString()
                i
            }
        }
    }
}