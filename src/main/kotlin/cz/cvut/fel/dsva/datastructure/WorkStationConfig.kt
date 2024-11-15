package cz.cvut.fel.dsva.datastructure

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cvut.fel.dsva.clients.JuliaSetClient
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.workStation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ManagedChannelBuilder
import java.io.File
import java.io.IOException
import java.time.Duration

data class WorkStationConfig(
    val ip: String,
    val port: Int,
    val maxCalculationDuration: Duration,
    val batchSize: Int,
    val maxRequestRepeat: Int,
    val otherWorkstations: List<RemoteWorkStation>,
) {
    val vectorClock: VectorClock = VectorClock(this, otherWorkstations)

    fun toWorkStation(): WorkStation = workStation {
        ip = this@WorkStationConfig.ip
        port = this@WorkStationConfig.port
    }

    fun findRemoteWorkStation(workStation: WorkStation): RemoteWorkStation =
        otherWorkstations.find { it.workStation == workStation }
            ?: throw NoSuchElementException("Remote workstation $workStation not found")


    companion object {
        private val logger = KotlinLogging.logger { }
        fun fromPropertiesFile(propertiesFileName: String?, objectMapper: ObjectMapper): WorkStationConfig {
            val properties = if (propertiesFileName != null) {
                logger.info { "Loading properties from file $propertiesFileName" }
                propertiesFileName
            } else {
                logger.info { "Loading properties from default file $DEFAULT_PROPERTIES_FILE_NAME" }
                DEFAULT_PROPERTIES_FILE_NAME
            }
            try {
                val readFile = readFile(properties)
                return deserializeConfig(readFile, objectMapper)
            } catch (e: IOException) {
                logger.error { ("Error occurred while reading properties file $propertiesFileName") }
            } catch (e: IllegalStateException) {
                logger.error { e.message }
            }
            error("Fatal error occurred while reading properties file $properties")
        }

        private fun readFile(propertiesFileName: String): String {
            val inputStream = if (propertiesFileName == DEFAULT_PROPERTIES_FILE_NAME) {
                Companion::class.java.classLoader.getResourceAsStream(propertiesFileName)
            } else {
                File(propertiesFileName).inputStream()
            }
            return inputStream.bufferedReader().use {
                it.readText()
            }
        }

        private fun deserializeConfig(config: String, objectMapper: ObjectMapper): WorkStationConfig {
            return try {
                objectMapper.readValue(config, WorkStationConfig::class.java)
            } catch (e: JsonProcessingException) {
                error("Error occurred during processing of configuration")
            } catch (e: JsonMappingException) {
                error("Error occurred during mapping of configuration")
            }
        }

        private const val DEFAULT_PROPERTIES_FILE_NAME = "default-properties.json"
    }
}

data class RemoteWorkStation(val ip: String, val port: Int) {

    @field:JsonIgnore
    val workStation: WorkStation = workStation {
        ip = this@RemoteWorkStation.ip
        port = this@RemoteWorkStation.port
    }

    fun createClient(workStationConfig: WorkStationConfig): JuliaSetClient =
        JuliaSetClient(ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build(), workStationConfig)
}

fun WorkStation.toRemoteWorkStation(): RemoteWorkStation = RemoteWorkStation(this.ip, this.port)