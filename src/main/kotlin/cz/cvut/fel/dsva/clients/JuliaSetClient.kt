package cz.cvut.fel.dsva.clients

import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import io.grpc.ManagedChannel
import io.grpc.StatusException
import java.io.Closeable
import java.util.concurrent.TimeUnit

class JuliaSetClient(private val channel: ManagedChannel) : Closeable {
    private val stub = JuliaSetCalculatorGrpcKt.JuliaSetCalculatorCoroutineStub(channel)

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    suspend fun sendCompletedCalculation(result: BatchCalculationResult) {
        try {
            stub.submitRequestedWork(result)
        } catch (e: StatusException) {
            //todo add error handling
        }
    }

    suspend fun requestNewWorkload(currentMachine: WorkStationConfig): BatchCalculationRequest {
        try {
            return stub.requestNewWork(currentMachine.toWorkStation())
        } catch (e: StatusException) {
            //TODO add error handling
        }
        return BatchCalculationRequest.getDefaultInstance()
    }

    suspend fun sendCalculationRequest(batchCalculationRequest: BatchCalculationRequest): RequestCalculationRequestResult {
        try {
            return stub.requestCalculation(batchCalculationRequest)
        } catch (e: StatusException) {
            error("Unable to contact remote machine")
        }
    }

}