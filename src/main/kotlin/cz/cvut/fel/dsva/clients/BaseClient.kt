package cz.cvut.fel.dsva.clients

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import io.grpc.StatusException
import java.time.Duration

abstract class BaseClient<T : Any>(protected val workStationConfig: WorkStationConfig) {
    protected abstract val logger: LoggerWrapper<T>


    protected suspend fun <T : Any> runRequestRepeatedly(sendRequest: suspend () -> T): T {
        if (!workStationConfig.nodeRunning) {
            logger.info("Work station is turned off")
            error("Work station is turned off")
        }
        for (i in 0..<workStationConfig.maxRequestRepeat) {
            delayMessageRequest()
            try {
                return sendRequest()
            } catch (e: StatusException) {
                println(e)
                logger.info("Unable to contact remote machine in attempt ${i + 1} out of ${workStationConfig.maxRequestRepeat}, because of $e")
            }
        }
        error("Unable to contact remote machine")
    }

    private fun delayMessageRequest() {
        try {
            if (workStationConfig.messageDelay != Duration.ZERO) {
                Thread.sleep(workStationConfig.messageDelay)
            }
        } catch (e: InterruptedException) {
            logger.error("Exception occurred while delaying message request, $e")
        }
    }
}