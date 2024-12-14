package cz.cvut.fel.dsva

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cz.cvut.fel.dsva.api.HttpServerApiHandler
import cz.cvut.fel.dsva.api.JuliaSetApiHandler
import cz.cvut.fel.dsva.api.WorkStationManagementApiHandler
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.SystemJobStoreImpl
import cz.cvut.fel.dsva.images.ImagesGeneratorImpl
import cz.cvut.fel.dsva.service.JobServiceImpl
import cz.cvut.fel.dsva.service.JuliaSetServiceImpl
import cz.cvut.fel.dsva.service.UserInputServiceImpl
import cz.cvut.fel.dsva.service.WorkStationHttpManagementServiceImpl
import cz.cvut.fel.dsva.service.WorkStationManagementService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import io.javalin.Javalin

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val objectMapper = ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())
    val workStationConfig = WorkStationConfig.fromPropertiesFile(args.getPropertiesFileName(), objectMapper)
    val systemJobStore = SystemJobStoreImpl(workStationConfig)
    val imagesGenerator = ImagesGeneratorImpl()
    val jobService = JobServiceImpl(systemJobStore, imagesGenerator, workStationConfig)
    val juliaSetService = JuliaSetServiceImpl(systemJobStore, workStationConfig, jobService)
    val userInputService = UserInputServiceImpl(systemJobStore, workStationConfig, imagesGenerator, jobService)
    val httpServer = initHttpServer(workStationConfig.httpServerPort, workStationConfig.ip)
    val remoteWorkStationHttpManagementService = WorkStationHttpManagementServiceImpl(workStationConfig)
    HttpServerApiHandler(
        workStationConfig,
        userInputService,
        httpServer,
        objectMapper,
        remoteWorkStationHttpManagementService
    )
    val workStationManagementApiHandler =
        WorkStationManagementApiHandler(WorkStationManagementService(workStationConfig))
    val juliaSetApiHandler = JuliaSetApiHandler(juliaSetService)
    httpServer.start()
    logger.info { "HTTP server started, listening on ip ${workStationConfig.ip} and port ${workStationConfig.httpServerPort}" }
    GrpcServerWrapper.initInstance(juliaSetApiHandler, workStationManagementApiHandler, workStationConfig).start()


}

private fun Array<String>.getPropertiesFileName(): String? {
    if (this.isEmpty()) {
        return null
    }
    return this[0]
}

private fun initHttpServer(port: Int, ip: String): Javalin {
    return Javalin.create { config ->
        config.useVirtualThreads = true
        config.jetty.defaultPort = port
        config.jetty.defaultHost = ip
    }
}

class GrpcServerWrapper private constructor(
    private val juliaSetApiHandler: JuliaSetApiHandler,
    private val workStationManagementApiHandler: WorkStationManagementApiHandler,
    private val workStationConfig: WorkStationConfig,
) {
    private lateinit var server: Server
    var running = false
        get() = synchronized(this) { field }
        private set
    private val logger = LoggerWrapper(GrpcServerWrapper::class, workStationConfig)

    fun start() {
        synchronized(this) {
            check(!running) { "Already running" }
            logger.info("Starting grpc server")
            this.server = ServerBuilder
                .forPort(workStationConfig.port)
                .addService(juliaSetApiHandler)
                .addService(workStationManagementApiHandler)
                .maxInboundMessageSize(Int.MAX_VALUE)
                .build()
            this.server.start()
            workStationConfig.vectorClock.increment()
            logger.info("Grpc server started, listening on ip ${workStationConfig.ip} and port ${workStationConfig.port}")
        }
    }

    fun stop() {
        synchronized(this) {
            check(running) { "Already stopped" }
            logger.info("Shutting down grpc server")
            this.server.shutdownNow().awaitTermination()
            workStationConfig.vectorClock.increment()
            logger.info("Shut down grpc server")
        }
    }


    companion object {
        private lateinit var instance: GrpcServerWrapper
        fun initInstance(
            juliaSetApiHandler: JuliaSetApiHandler,
            workStationManagementApiHandler: WorkStationManagementApiHandler,
            workStationConfig: WorkStationConfig
        ): GrpcServerWrapper {
            if (!::instance.isInitialized) {
                instance = GrpcServerWrapper(juliaSetApiHandler, workStationManagementApiHandler, workStationConfig)
            }
            return instance
        }

        fun getInstance(): GrpcServerWrapper {
            if (!::instance.isInitialized) {
                throw IllegalStateException("Grpc server is not initialized")
            }
            return instance
        }
    }
}