package cz.cvut.fel.dsva.api

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.service.JuliaSetService

class JuliaSetApiHandler(private val juliaSetService: JuliaSetService) :
    JuliaSetCalculatorGrpcKt.JuliaSetCalculatorCoroutineImplBase() {
    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        return juliaSetService.requestCalculation(request)
    }

    override suspend fun requestNewWork(request: Empty): BatchCalculationRequest {
        return super.requestNewWork(request)
    }

    override suspend fun submitRequestedWork(request: BatchCalculationResult): Empty {
        return super.submitRequestedWork(request)
    }
}