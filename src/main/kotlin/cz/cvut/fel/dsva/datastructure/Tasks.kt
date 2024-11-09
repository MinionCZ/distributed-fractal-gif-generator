package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationResult
import cz.cvut.fel.dsva.grpc.Clock
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.batchCalculationRequest
import java.time.LocalDateTime
import java.util.LinkedList


data class RemoteTaskBatch(
    val tasks: List<CalculationRequest>,
    val startTimestamp: LocalDateTime,
    val worker: RemoteWorkStation,
) {
    fun toBatchCalculationRequest(clocks: Collection<Clock>): BatchCalculationRequest = batchCalculationRequest {
        requests.addAll(tasks)
        requester = worker.workStation
        vectorClock.addAll(clocks)
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