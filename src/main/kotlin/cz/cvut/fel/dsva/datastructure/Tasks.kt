package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.Clock
import cz.cvut.fel.dsva.grpc.batchCalculationRequest
import java.time.LocalDateTime


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