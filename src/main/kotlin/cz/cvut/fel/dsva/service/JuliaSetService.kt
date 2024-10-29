package cz.cvut.fel.dsva.service

import com.google.protobuf.Empty
import com.google.protobuf.empty
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJob
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.calculationResult
import cz.cvut.fel.dsva.grpc.requestCalculationRequestResult
import cz.cvut.fel.dsva.images.ImagesGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class JuliaSetServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val imagesGenerator: ImagesGenerator,
    private val thisWorkStation: WorkStationConfig,
) : JuliaSetService {
    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        return if (systemJobStore.isSystemJobPresent()) {
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.ALREADY_IN_COMPUTATION
            }
        } else {
            val newSystemJob = SystemJob(request.requester, request.requestsList)
            systemJobStore.persistNewSystemJob(systemJob = newSystemJob)
            runCalculationOnBackground()
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.OK
            }
        }
    }

    override fun handleNewWorkRequest(workStation: WorkStation): BatchCalculationRequest {
        return try {
            systemJobStore
                .getSystemJob()
                .createRemoteJob(thisWorkStation.batchSize, workStation)
                .toBatchCalculationRequest()
        } catch (e: IllegalStateException) {
            BatchCalculationRequest.getDefaultInstance()
        }
    }

    override fun handleDoneWork(calculationResult: BatchCalculationResult): Empty {
        try {
            systemJobStore.getSystemJob().addCalculationResults(calculationResult.resultsList, calculationResult.worker)
        } catch (e: IllegalStateException) {
            //TODO log
        }
        return Empty.getDefaultInstance()
    }


    private suspend fun runCalculationOnBackground() {
        coroutineScope {
            launch(Dispatchers.Default) {
                val systemJob = systemJobStore.getSystemJob()
                while (true) {
                    val task = systemJob.popFirstTask() ?: break
                    val calculatedImage =
                        imagesGenerator.generateJuliaSetImage(task.imageProperties, task.juliaSetProperties)
                    systemJob.addCalculationResult(calculationResult {
                        imageProperties = task.imageProperties
                        pixels.addAll(calculatedImage.toList())
                    })
                }
                //TODO check if remote computation is done as well and return results
            }
        }
    }
}


interface JuliaSetService {
    suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult
    fun handleNewWorkRequest(workStation: WorkStation): BatchCalculationRequest
    fun handleDoneWork(calculationResult: BatchCalculationResult): Empty

}