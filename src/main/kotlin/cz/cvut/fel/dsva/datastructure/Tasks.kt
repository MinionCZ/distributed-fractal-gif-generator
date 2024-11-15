package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationResult
import cz.cvut.fel.dsva.grpc.batchCalculationRequest
import java.time.Duration
import java.time.LocalDateTime
import java.util.LinkedList


data class RemoteTaskBatch(
    val tasks: List<CalculationRequest>,
    val startTimestamp: LocalDateTime,
    val worker: RemoteWorkStation,
) {
    private var lastUpdateTimestamp: LocalDateTime = startTimestamp

    fun toBatchCalculationRequest(workStationConfig: WorkStationConfig): BatchCalculationRequest =
        batchCalculationRequest {
            requests.addAll(tasks)
            requester = workStationConfig.toWorkStation()
            vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
        }

    fun updateLastUpdateTimestamp() {
        synchronized(this) {
            lastUpdateTimestamp = LocalDateTime.now()
        }
    }

    fun isTimeOuted(maxRunTime: Duration): Boolean =
        synchronized(this) {
            this.lastUpdateTimestamp.plus(maxRunTime) <= LocalDateTime.now()
        }

}

class RequestedTaskBatch(
    tasks: List<CalculationRequest>,
    val startTimestamp: LocalDateTime,
    val requester: RemoteWorkStation,
) {
    private val tasks: MutableList<CalculationRequest> = LinkedList(tasks)
    private val calculatedImages: MutableList<CalculationResult> = LinkedList()


    fun popTask(): CalculationRequest? {
        synchronized(this) {
            return if (tasks.isNotEmpty()) {
                tasks.removeAt(0)
            } else {
                null
            }
        }
    }

    fun addCalculationResult(result: CalculationResult) {
        synchronized(this) {
            calculatedImages.add(result)
        }
    }

    fun getAllResults(): List<CalculationResult> {
        synchronized(this) {
            return LinkedList(calculatedImages).also {
                calculatedImages.clear()
            }
        }
    }
}