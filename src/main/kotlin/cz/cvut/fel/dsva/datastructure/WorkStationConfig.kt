package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.workStation
import java.time.Duration

data class WorkStationConfig(
    val ip: String,
    val port: Int,
    val maxConnectionTimeout: Duration,
    val batchSize: Int,
) {
    fun toWorkStation(): WorkStation = workStation {
        ip = this.ip
        port = this.port
    }
}