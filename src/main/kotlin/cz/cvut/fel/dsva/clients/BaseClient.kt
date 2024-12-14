package cz.cvut.fel.dsva.clients

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import io.grpc.StatusException

abstract class BaseClient <T: Any>(protected val workStationConfig: WorkStationConfig) {
    protected abstract val logger: LoggerWrapper<T>


    protected suspend fun <T : Any> runRequestRepeatedly(sendRequest: suspend () -> T): T {
        for (i in 0..<workStationConfig.maxRequestRepeat) {
            try {
                return sendRequest()
            } catch (e: StatusException) {
                println(e)
                logger.info("Unable to contact remote machine in attempt ${i + 1} out of ${workStationConfig.maxRequestRepeat}, because of $e")
            }
        }
        error("Unable to contact remote machine")
    }
}