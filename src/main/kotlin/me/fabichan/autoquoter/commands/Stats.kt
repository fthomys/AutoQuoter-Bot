package me.fabichan.autoquoter.commands

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.provider.GlobalApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GlobalApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import me.fabichan.autoquoter.config.Config
import net.dv8tion.jda.api.Permission

@Command
class Stats(
    private val context: BContext,
    private val database: Database
) : GlobalApplicationCommandProvider {

    suspend fun onStatsCommand(
        event: GuildSlashEvent
    ) {
        event.deferReply().await()
        val guildId = event.guild.idLong
        val userId = event.user.idLong

        val topQuotedUsers = getTopQuotedUsers(guildId)
        val topQuotedChannels = getTopQuotedChannels(guildId)
        val personalQuotes = getPersonalQuotes(guildId, userId)
        val totalQuotes = getTotalQuotes(guildId)

        val embed = Embed {
            title = "Quote Statistics for ${event.guild.name}"
            color = Config.Constants.EMBED_COLOR
            thumbnail = event.guild.iconUrl

            field {
                name = "Total Quotes"
                value = totalQuotes.toString()
                inline = true
            }

            field {
                name = "Your Quotes"
                value = personalQuotes.toString()
                inline = true
            }
            
            field {
                name = ""
                value = ""
                inline = false
            }

            field {
                name = "Top 5 Quoted Users"
                value = if (topQuotedUsers.isEmpty()) "No quotes yet." else topQuotedUsers.joinToString("\n") { (id, count) ->
                    "<@$id>: $count quotes"
                }
                inline = true
            }

            field {
                name = "Top 5 Quoted Channels"
                value = if (topQuotedChannels.isEmpty()) "No quotes yet." else topQuotedChannels.joinToString("\n") { (id, count) ->
                    "<#$id>: $count quotes"
                }
                inline = true
            }

            footer {
                name = "AutoQuoter Statistics"
                iconUrl = event.jda.selfUser.effectiveAvatarUrl
            }
        }

        event.hook.sendMessageEmbeds(embed).queue()
    }

    private suspend fun getTopQuotedUsers(guildId: Long): List<Pair<Long, Int>> {
        return database.preparedStatement("SELECT user_id, COUNT(*) as count FROM qoutestats WHERE guild_id = ? GROUP BY user_id ORDER BY count DESC LIMIT 5") {
            executeQuery(guildId).map { it.getLong("user_id") to it.getInt("count") }
        }
    }

    private suspend fun getTopQuotedChannels(guildId: Long): List<Pair<Long, Int>> {
        return database.preparedStatement("SELECT channel_id, COUNT(*) as count FROM qoutestats WHERE guild_id = ? GROUP BY channel_id ORDER BY count DESC LIMIT 5") {
            executeQuery(guildId).map { it.getLong("channel_id") to it.getInt("count") }
        }
    }

    private suspend fun getPersonalQuotes(guildId: Long, userId: Long): Int {
        return database.preparedStatement("SELECT COUNT(*) as count FROM qoutestats WHERE guild_id = ? AND user_id = ?") {
            executeQuery(guildId, userId).map { it.getInt("count") }.firstOrNull() ?: 0
        }
    }

    private suspend fun getTotalQuotes(guildId: Long): Int {
        return database.preparedStatement("SELECT COUNT(*) as count FROM qoutestats WHERE guild_id = ?") {
            executeQuery(guildId).map { it.getInt("count") }.firstOrNull() ?: 0
        }
    }

    override fun declareGlobalApplicationCommands(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("stats", function = ::onStatsCommand) {
            description = "Shows quote statistics for this server."
            botPermissions += Permission.MESSAGE_SEND
            botPermissions += Permission.MESSAGE_EMBED_LINKS
        }
    }
}
