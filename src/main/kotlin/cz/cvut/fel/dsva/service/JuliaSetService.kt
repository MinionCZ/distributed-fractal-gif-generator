package cz.cvut.fel.dsva.service

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.Job
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.NewWorkRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.requestCalculationRequestResult
import kotlin.math.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class JuliaSetServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val workStationConfig: WorkStationConfig,
    private val jobService: JobService,
) : JuliaSetService {
    private val logger = LoggerWrapper(JuliaSetServiceImpl::class, workStationConfig = workStationConfig)

    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        this.workStationConfig.vectorClock.update(request.vectorClockList)
        logger.info("Handling request calculation from remote machine ${request.requester}")
        workStationConfig.vectorClock.increment()
        return if (!systemJobStore.getSystemJob().isLocalCalculationCompleted()) {
            logger.info("Machine is already computing")
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.ALREADY_IN_COMPUTATION
                vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
            }
        } else {
            synchronized(this) {
                if (systemJobStore.isSystemJobPresent()) {
                    systemJobStore.getSystemJob().addNewTasks(request.requestsList)
                } else {
                    val newJob = Job(request.requester, request.requestsList)
                    systemJobStore.persistNewSystemJob(job = newJob)
                }
            }
            runCalculationOnBackground()
            logger.info("Successfully started new computation")
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
                .toBatchCalculationRequest(this.workStationConfig.vectorClock.toGrpcFormat()).also {
                    logger.info("Successfully created and sent remote job")
                }
        } catch (e: IllegalStateException) {
            logger.error("Fatal error has occurred during handing of new work ${e.message}")
            throw e
        }
    }

    override fun handleDoneWork(calculationResult: BatchCalculationResult): Empty {
        this.workStationConfig.vectorClock.update(calculationResult.vectorClockList)
        logger.info("Handling remote done work")
        try {
            val remoteWorker = workStationConfig.findRemoteWorkStation(calculationResult.worker)
            systemJobStore.getSystemJob().addCalculationResults(calculationResult.resultsList, remoteWorker)
        } catch (e: IllegalStateException) {
            logger.error("Fatal error has occurred during handing of new work ${e.message}")
            throw e
        }
        return Empty.getDefaultInstance()
    }


    private suspend fun runCalculationOnBackground() {
        coroutineScope {
            launch(Dispatchers.Default) {
                jobService.calculateTasks()
                jobService.awaitCalculationFinish()
            }
        }
    }
}


interface JuliaSetService {
    suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult
    fun handleNewWorkRequest(newWorkRequest: NewWorkRequest): BatchCalculationRequest
    fun handleDoneWork(calculationResult: BatchCalculationResult): Empty
}