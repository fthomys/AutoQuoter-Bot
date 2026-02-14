package me.fabichan.autoquoter

import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.github.oshai.kotlinlogging.KotlinLogging
import com.sun.net.httpserver.HttpServer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

@BService
class Metrics {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
        val podName = System.getenv("POD_NAME") ?: "unknown-pod"
        config().commonTags("pod", podName)
    }
    
    private var quoteCount = AtomicInteger(0)
    private var guildCount = AtomicInteger(0)
    private val shardPings = ConcurrentHashMap<Int, Double>()
    private val quotesCreatedCounter = Counter.builder("autoquoter_quotes_created_total")
        .description("Total number of quotes created in this session")
        .register(prometheusRegistry)

    init {
        // Bind JVM metrics
        ClassLoaderMetrics().bindTo(prometheusRegistry)
        JvmMemoryMetrics().bindTo(prometheusRegistry)
        JvmGcMetrics().bindTo(prometheusRegistry)
        ProcessorMetrics().bindTo(prometheusRegistry)
        JvmThreadMetrics().bindTo(prometheusRegistry)
        FileDescriptorMetrics().bindTo(prometheusRegistry)
        UptimeMetrics().bindTo(prometheusRegistry)

        Gauge.builder("autoquoter_quotes_all_total", quoteCount) { it.get().toDouble() }
            .description("Total number of quotes recorded")
            .register(prometheusRegistry)

        Gauge.builder("autoquoter_guilds_total", guildCount) { it.get().toDouble() }
            .description("Total number of guilds the bot is in")
            .register(prometheusRegistry)
            
        startServer()
    }

    fun updateQuoteCount(count: Int) {
        quoteCount.set(count)
    }

    fun updateGuildCount(count: Int) {
        guildCount.set(count)
    }
    
    fun incrementQuotesCreated() {
        quotesCreatedCounter.increment()
    }

    fun updateShardPing(shardId: Int, ping: Long) {
        shardPings[shardId] = ping.toDouble()
        // Register the gauge only once per shardId
        // Micrometer handles multiple registrations with the same tags by returning the existing gauge
        Gauge.builder("autoquoter_gateway_ping", shardPings) { 
            it[shardId] ?: 0.0 
        }
            .tag("shard_id", shardId.toString())
            .description("Gateway ping for a specific shard")
            .register(prometheusRegistry)
    }

    private fun startServer() {
        val port = System.getenv("PROMETHEUS_PORT")?.toIntOrNull() ?: 8080
        try {
            val server = HttpServer.create(InetSocketAddress(port), 0)
            server.createContext("/metrics") { exchange ->
                val response = prometheusRegistry.scrape()
                val bytes = response.toByteArray()
                exchange.responseHeaders.add("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { os ->
                    os.write(bytes)
                }
            }
            server.executor = Executors.newSingleThreadExecutor()
            server.start()
            logger.info { "Prometheus metrics server started on port $port" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to start Prometheus metrics server" }
        }
    }
}
