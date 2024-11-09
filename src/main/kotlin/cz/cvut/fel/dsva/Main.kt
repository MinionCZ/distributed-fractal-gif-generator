package cz.cvut.fel.dsva

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cz.cvut.fel.dsva.api.JuliaSetApiHandler
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJobStoreImpl
import cz.cvut.fel.dsva.images.ImagesGeneratorImpl
import cz.cvut.fel.dsva.input.UserInputHandler
import cz.cvut.fel.dsva.service.JobServiceImpl
import cz.cvut.fel.dsva.service.JuliaSetServiceImpl
import cz.cvut.fel.dsva.service.UserInputServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ServerBuilder
import kotlin.math.log
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val objectMapper = ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())
    val workStationConfig = WorkStationConfig.fromPropertiesFile(args.getPropertiesFileName(), objectMapper)
    val systemJobStore = SystemJobStoreImpl(workStationConfig)
    val imagesGenerator = ImagesGeneratorImpl()
    val jobService = JobServiceImpl(systemJobStore, imagesGenerator, workStationConfig)
    val juliaSetService = JuliaSetServiceImpl(systemJobStore, workStationConfig, jobService)
    val userInputService = UserInputServiceImpl(systemJobStore, workStationConfig, imagesGenerator, jobService)
    val userInputHandler = UserInputHandler(systemJobStore, workStationConfig, objectMapper, userInputService)
    val juliaSetApiHandler = JuliaSetApiHandler(juliaSetService)
    val server = ServerBuilder
        .forPort(workStationConfig.port)
        .addService(juliaSetApiHandler)
        .maxInboundMessageSize(Int.MAX_VALUE)
        .build()
    server.start()
    logger.info { "Server started, listening on ip ${workStationConfig.ip} and port ${workStationConfig.port}" }
    runBlocking {
        userInputHandler.startInputHandler()
    }
}

private fun Array<String>.getPropertiesFileName(): String? {
    if (this.isEmpty()) {
        return null
    }
    return this[0]
}