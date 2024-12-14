package cz.cvut.fel.dsva.api

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.input.UserInputHandler
import cz.cvut.fel.dsva.service.UserInputService
import cz.cvut.fel.dsva.service.WorkStationHttpManagementService
import cz.cvut.fel.dsva.service.WorkStationManagementService
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HttpServerApiHandler(
    private val currentWorkStationConfig: WorkStationConfig,
    private val userInputService: UserInputService,
    private val httpServer: Javalin,
    private val objectMapper: ObjectMapper,
    private val workStationHttpManagementService: WorkStationHttpManagementService
) {
    private val logger = LoggerWrapper(UserInputHandler::class, currentWorkStationConfig)

    init {
        httpServer.post("/new-job", this::newJob)
        httpServer.post("/join", this::join)
        httpServer.post("/leave", this::leave)
        httpServer.post("/kill", this::kill)
        httpServer.post("/revive", this::revive)
    }


    private fun newJob(context: Context) {
        try {
            currentWorkStationConfig.vectorClock.increment()
            logger.info("Received new job")
            val parsedObject = context.body().parseJson<GenerateImageDto>()
            parsedObject.validate()
            CoroutineScope(Dispatchers.IO).launch {
                userInputService.startNewDistributedJob(parsedObject)
            }
            currentWorkStationConfig.vectorClock.increment()
            logger.info("Successfully created new job")
            context.status(HttpStatus.NO_CONTENT)
        } catch (e: IllegalArgumentException) {
            logger.info("Error occurred while creating new job")
            throw BadRequestResponse("Json contains invalid data: ${e.message}")
        }
    }

    private fun revive(context: Context) {

    }

    private fun kill(context: Context) {

    }

    private fun leave(context: Context) {
        workStationHttpManagementService.leave()
        context.status(HttpStatus.NO_CONTENT)
    }

    private fun join(context: Context) {
        try {
            currentWorkStationConfig.vectorClock.increment()
            logger.info("Received new join request")
            val remoteStations = context.body().parseJson<Collection<RemoteWorkStationDto>>()
            remoteStations.forEach { it.validate() }
            workStationHttpManagementService.join(remoteStations)
            currentWorkStationConfig.vectorClock.increment()
            logger.info("Successfully joined topology")
            context.status(HttpStatus.NO_CONTENT)
        } catch (e: IllegalArgumentException) {
            logger.info("Error occurred while validating of remote stations: ${e.message}")
            throw BadRequestResponse("Json contains invalid data: ${e.message}")
        }
    }


    private inline fun <reified T> String.parseJson(): T {
        try {
            return objectMapper.readValue(this, T::class.java)
        } catch (e: JacksonException) {
            logger.info("Error occurred while creating new job")
            throw BadRequestResponse("Invalid json")
        }

    }
}
