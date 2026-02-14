package me.fabichan.autoquoter

import me.fabichan.autoquoter.config.Config

object ShardHelper {
    private val podName = System.getenv("POD_NAME")
    private val totalShards = Config.instance.shardCountOverride ?: System.getenv("TOTAL_SHARDS")?.toInt() ?: 1
    private val totalReplicas = System.getenv("REPLICAS")?.toInt() ?: 1

    fun getShardIdsForCurrentPod(): List<Int> {
        val override = Config.instance.shardCountOverride
        if (override != null) return (0 until override).toList()
        if (podName == null) return (0 until totalShards).toList()

        val podIndex = podName.substringAfterLast("-").toIntOrNull() ?: 0

        val shardsPerPod = totalShards / totalReplicas
        val remainder = totalShards % totalReplicas

        val start = podIndex * shardsPerPod + minOf(podIndex, remainder)
        val count = shardsPerPod + if (podIndex < remainder) 1 else 0

        return (start until (start + count)).toList()
    }
    
    fun toIntRange(shards: List<Int>): IntRange? = if (shards.isEmpty()) null else shards.first()..shards.last()

    fun getTotalShards() = totalShards
}
