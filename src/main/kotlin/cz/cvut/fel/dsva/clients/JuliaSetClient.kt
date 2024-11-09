package cz.cvut.fel.dsva.clients

import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.NewWorkRequest
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
            println(e)
            error("Unable to contact remote machine")
        }
    }

    suspend fun requestNewWorkload(newWorkRequest: NewWorkRequest): BatchCalculationRequest {
        try {
            return stub.requestNewWork(newWorkRequest)
        } catch (e: StatusException) {
            println(e)
            error("Unable to contact remote machine")
        }
    }

    suspend fun sendCalculationRequest(batchCalculationRequest: BatchCalculationRequest): RequestCalculationRequestResult {
        try {
            return stub.requestCalculation(batchCalculationRequest)
        } catch (e: StatusException) {
            println(e)
            error("Unable to contact remote machine")
        }
    }

}