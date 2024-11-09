package cz.cvut.fel.dsva

import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class LoggerWrapper(clazz: KClass<Any>, private val workStationConfig: WorkStationConfig) {
    private val logger =
        KotlinLogging.logger(clazz.qualifiedName ?: throw IllegalStateException("Unable to create logger for class without name"))

    fun info(message: String) {
        logger.info {
            message.prependVectorClock()
        }
    }

    fun error(message: String) {
        logger.error {
            message.prependVectorClock()
        }
    }

    private fun String.prependVectorClock(): String = "${workStationConfig.vectorClock.toLogFormat()} | $this"
}