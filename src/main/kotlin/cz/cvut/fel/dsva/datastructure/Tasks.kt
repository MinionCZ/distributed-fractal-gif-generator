package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.batchCalculationRequest
import java.time.LocalDateTime


data class RemoteTaskBatch(
    val tasks: List<CalculationRequest>,
    val startTimestamp: LocalDateTime,
    val worker: WorkStation,
) {
    fun toBatchCalculationRequest(): BatchCalculationRequest = batchCalculationRequest {
        requests.addAll(tasks)
        requester = worker
    }
}