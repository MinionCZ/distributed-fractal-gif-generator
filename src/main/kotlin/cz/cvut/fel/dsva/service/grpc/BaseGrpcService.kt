package cz.cvut.fel.dsva.service.grpc

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import java.time.Duration

abstract class BaseGrpcService<T : Any>(protected val workStationConfig: WorkStationConfig) {
    protected abstract val logger: LoggerWrapper<T>

    fun <T : Any> applyDelay(innerFunction: () -> T): T {

        try {
            if (workStationConfig.messageDelay != Duration.ZERO) {
                Thread.sleep(workStationConfig.messageDelay)
            }
        } catch (e: InterruptedException) {
            logger.error("Fatal error occurred during delay of messaging $e")
        }
        return innerFunction()
    }

}