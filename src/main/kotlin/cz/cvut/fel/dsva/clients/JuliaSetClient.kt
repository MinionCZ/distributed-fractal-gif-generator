package cz.cvut.fel.dsva.clients

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.NewWorkRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import io.grpc.ManagedChannel
import io.grpc.StatusException
import java.io.Closeable
import java.util.concurrent.TimeUnit

class JuliaSetClient(private val channel: ManagedChannel, workStationConfig: WorkStationConfig) :
    Closeable, BaseClient<JuliaSetClient>(workStationConfig) {
    private val stub = JuliaSetCalculatorGrpcKt.JuliaSetCalculatorCoroutineStub(channel)
    override val logger = LoggerWrapper(JuliaSetClient::class, workStationConfig)

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    suspend fun sendCompletedCalculation(result: BatchCalculationResult) =
        runRequestRepeatedly {
            stub.submitRequestedWork(result)
        }

    suspend fun requestNewWorkload(newWorkRequest: NewWorkRequest): BatchCalculationRequest =
        runRequestRepeatedly {
            stub.requestNewWork(newWorkRequest)
        }


    suspend fun sendCalculationRequest(batchCalculationRequest: BatchCalculationRequest): RequestCalculationRequestResult =
        runRequestRepeatedly {
            stub.requestCalculation(batchCalculationRequest)
        }
}