package cz.cvut.fel.dsva.communication

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetCalculatorGrpcKt
import cz.cvut.fel.dsva.grpc.batchCalculationResult
import cz.cvut.fel.dsva.grpc.calculationResult
import cz.cvut.fel.dsva.images.ImagesGenerator

class JuliaSetService(private val imagesGenerator: ImagesGenerator) :
    JuliaSetCalculatorGrpcKt.JuliaSetCalculatorCoroutineImplBase() {
    override suspend fun calculate(request: BatchCalculationRequest): BatchCalculationResult {
        val computationResult = request.requestsList.map { req ->
            val calculatedPixels =
                imagesGenerator.generateJuliaSetImage(req.imageProperties, req.juliaSetProperties).toList()
            calculationResult {
                calculationId = req.calculationId
                imageProperties = req.imageProperties
                pixels.addAll(calculatedPixels)
            }
        }
        return batchCalculationResult{
            results.addAll(computationResult)
        }
    }

    override suspend fun requestNewWork(request: Empty): BatchCalculationRequest {
        return super.requestNewWork(request)
    }

    override suspend fun submitRequestedWork(request: BatchCalculationResult): Empty {
        return super.submitRequestedWork(request)
    }
}