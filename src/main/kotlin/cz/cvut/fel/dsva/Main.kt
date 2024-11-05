package cz.cvut.fel.dsva

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cz.cvut.fel.dsva.api.JuliaSetApiHandler
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJobStoreImpl
import cz.cvut.fel.dsva.images.ImagesGeneratorImpl
import cz.cvut.fel.dsva.input.UserInputHandler
import cz.cvut.fel.dsva.service.JuliaSetServiceImpl
import cz.cvut.fel.dsva.service.UserInputServiceImpl
import io.grpc.ServerBuilder
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) {
    val objectMapper = ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())
    val workStationConfig = WorkStationConfig.fromPropertiesFile(args.getPropertiesFileName(), objectMapper)
    val systemJobStore = SystemJobStoreImpl()
    val imagesGenerator = ImagesGeneratorImpl()
    val juliaSetService = JuliaSetServiceImpl(systemJobStore, imagesGenerator, workStationConfig)
    val userInputService = UserInputServiceImpl(systemJobStore, workStationConfig, imagesGenerator)
    val userInputHandler = UserInputHandler(systemJobStore, workStationConfig, objectMapper, userInputService)
    val juliaSetApiHandler = JuliaSetApiHandler(juliaSetService)
    val server = ServerBuilder.forPort(workStationConfig.port).addService(juliaSetApiHandler).build()
    server.start()
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