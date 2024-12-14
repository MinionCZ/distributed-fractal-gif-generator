package cz.cvut.fel.dsva.api

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.input.UserInputHandler
import cz.cvut.fel.dsva.service.UserInputService
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HttpServerApiHandler(
    private val currentWorkStationConfig: WorkStationConfig,
    private val userInputService: UserInputService,
    private val httpServer: Javalin,
    private val objectMapper: ObjectMapper,
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
            val parsedObject = objectMapper.readValue(context.body(), GenerateImageDto::class.java)
            parsedObject.validate()
            CoroutineScope(Dispatchers.IO).launch {
                userInputService.startNewDistributedJob(parsedObject)
            }
            currentWorkStationConfig.vectorClock.increment()
            logger.info("Successfully created new job")
        } catch (e: JacksonException) {
            logger.info("Error occurred while creating new job")
            throw BadRequestResponse("Invalid json")
        } catch (e: IllegalArgumentException) {
            logger.info("Error occurred while creating new job")
            throw BadRequestResponse("Json contains invalid data: ${e.message}")
        } catch (e: IllegalStateException) {
            logger.info("Error occurred while creating new job")
            throw ConflictResponse(e.message ?: "Job is already running")
        }
    }

    private fun revive(context: Context) {

    }

    private fun kill(context: Context) {

    }

    private fun leave(context: Context) {

    }

    private fun join(context: Context) {

    }
}
