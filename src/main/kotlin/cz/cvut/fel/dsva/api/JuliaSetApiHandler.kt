package cz.cvut.fel.dsva.api

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.NewWorkRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.service.JuliaSetService

class JuliaSetApiHandler(private val juliaSetService: JuliaSetService) :
    JuliaSetCalculatorGrpcKt.JuliaSetCalculatorCoroutineImplBase() {
    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        return juliaSetService.requestCalculation(request)
    }

    override suspend fun requestNewWork(request: NewWorkRequest): BatchCalculationRequest {
        return juliaSetService.handleNewWorkRequest(request)
    }

    override suspend fun submitRequestedWork(request: BatchCalculationResult): Empty {
        return juliaSetService.handleDoneWork(request)
    }
}