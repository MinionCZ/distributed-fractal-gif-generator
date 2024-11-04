package cz.cvut.fel.dsva

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cz.cvut.fel.dsva.datastructure.WorkStationConfig


fun main(args: Array<String>) {
    val objectMapper = ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())
    val workStationConfig = WorkStationConfig.fromPropertiesFile(args.getPropertiesFileName(), objectMapper)


}

private fun Array<String>.getPropertiesFileName(): String? {
    if (this.isEmpty()) {
        return null
    }
    return this[0]
}