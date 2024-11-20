package cz.cvut.fel.dsva.service

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.RemoteWorkStation
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.Job
import cz.cvut.fel.dsva.datastructure.SystemJobStore
import cz.cvut.fel.dsva.datastructure.toRemoteWorkStation
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.NewWorkRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.batchCalculationRequest
import cz.cvut.fel.dsva.grpc.requestCalculationRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JuliaSetServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val workStationConfig: WorkStationConfig,
    private val jobService: JobService,
) : JuliaSetService {
    private val logger = LoggerWrapper(JuliaSetServiceImpl::class, workStationConfig = workStationConfig)

    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        this.workStationConfig.vectorClock.update(request.vectorClockList)
        logger.info("Handling request calculation from remote machine ${request.requester.toRemoteWorkStation()}")
        workStationConfig.vectorClock.increment()
        return if (systemJobStore.isSystemJobPresent()) {
            logger.info("This machine is already computing current task")
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.ALREADY_IN_COMPUTATION
                vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
            }
        } else {
            val newJob = Job(RemoteWorkStation(request.requester.ip, request.requester.port), request.requestsList)
            systemJobStore.persistNewSystemJob(job = newJob)
            logger.info("Successfully started new computation for images with ids ${request.requestsList.map { it.imageProperties.id }}")
            jobService.sendRemoteJobs(jobService.prepareRemoteJobs())
            runCalculationOnBackground()
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.OK
                vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
            }
        }
    }

    override fun handleNewWorkRequest(newWorkRequest: NewWorkRequest): BatchCalculationRequest {
        this.workStationConfig.vectorClock.update(newWorkRequest.vectorClockList)
        logger.info("Handling new work request")
        return try {
            val remoteWorkStation = this.workStationConfig.findRemoteWorkStation(newWorkRequest.station)
            workStationConfig.vectorClock.increment()
            systemJobStore
                .getSystemJob()
                .createRemoteJob(this.workStationConfig.batchSize, remoteWorkStation)
                .toBatchCalculationRequest(workStationConfig).also {
                    logger.info("Successfully created and sent remote job with ids ${it.requestsList.map { it.imageProperties.id }}")
                }
        } catch (e: IllegalStateException) {
            batchCalculationRequest {
                vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
                requester = workStationConfig.toWorkStation()
            }.also {
                logger.info("Job is done on this machine or there are not enough tasks to calculate, returning empty list of requests to ${newWorkRequest.station.toRemoteWorkStation()}")
            }
        }
    }

    override fun handleDoneWork(calculationResult: BatchCalculationResult): Empty {
        this.workStationConfig.vectorClock.update(calculationResult.vectorClockList)
        logger.info("Handling remote done work")
        try {
            val remoteWorker = workStationConfig.findRemoteWorkStation(calculationResult.worker)
            systemJobStore.getSystemJob().addCalculationResults(calculationResult.resultsList, remoteWorker)
            logger.info("Added calculation results from remote job from machine $remoteWorker images with ids ${calculationResult.resultsList.map { it.imageProperties.id }}")
        } catch (e: IllegalStateException) {
            logger.error("Fatal error has occurred during handling of done work: ${e.message}")
        }
        return Empty.getDefaultInstance()
    }


    private fun runCalculationOnBackground() {
        CoroutineScope(Dispatchers.Default).launch {
            jobService.calculateTasks()
            jobService.awaitCalculationFinish()
            workStationConfig.vectorClock.increment()
            logger.info("Calculation finished, removing system job")
            systemJobStore.removeSystemJob()
            workStationConfig.vectorClock.increment()
            logger.info("Ready to accept new work")
        }
    }
}


interface JuliaSetService {
    suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult
    fun handleNewWorkRequest(newWorkRequest: NewWorkRequest): BatchCalculationRequest
    fun handleDoneWork(calculationResult: BatchCalculationResult): Empty
}