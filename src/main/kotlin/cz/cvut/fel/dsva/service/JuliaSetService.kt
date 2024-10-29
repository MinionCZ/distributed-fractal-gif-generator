package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.datastructure.system.SystemJob
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.datastructure.system.WorkStation
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.calculationResult
import cz.cvut.fel.dsva.grpc.requestCalculationRequestResult
import cz.cvut.fel.dsva.images.ImagesGenerator
import cz.cvut.fel.dsva.toTasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class JuliaSetServiceImpl(private val systemJobStore: SystemJobStore, private val imagesGenerator: ImagesGenerator) :
    JuliaSetService {
    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        return if (systemJobStore.isSystemJobPresent()) {
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.ALREADY_IN_COMPUTATION
            }
        } else {
            val ip = request.requester.ipList.map { it.toByte() }.toByteArray()
            val requester = WorkStation(ip, request.requester.port)
            val newSystemJob = SystemJob(requester, request.requestsList.toTasks())
            systemJobStore.persistNewSystemJob(systemJob = newSystemJob)
            runCalculationOnBackground()
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.OK
            }
        }
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

}