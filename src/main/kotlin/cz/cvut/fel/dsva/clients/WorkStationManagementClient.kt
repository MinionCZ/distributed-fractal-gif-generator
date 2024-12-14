package cz.cvut.fel.dsva.clients

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.NewWorkRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.WorkStationManagementGrpcKt
import cz.cvut.fel.dsva.service.WorkStationManagementService
import io.grpc.ManagedChannel
import io.grpc.StatusException
import java.io.Closeable
import java.util.concurrent.TimeUnit

class WorkStationManagementClient(
    private val channel: ManagedChannel,
    workStationConfig: WorkStationConfig
) : Closeable, BaseClient<WorkStationManagementService>(workStationConfig) {
    private val stub = WorkStationManagementGrpcKt.WorkStationManagementCoroutineStub(channel)
    override val logger: LoggerWrapper<WorkStationManagementService> =
        LoggerWrapper(WorkStationManagementService::class, workStationConfig)

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    suspend fun join() {
        runRequestRepeatedly {
            stub.join(workStationConfig.toWorkStation())
        }
    }

    suspend fun leave() {
        runRequestRepeatedly {
            stub.leave(workStationConfig.toWorkStation())
        }
    }
}