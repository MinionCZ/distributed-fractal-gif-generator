package cz.cvut.fel.dsva.service

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.Job
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.BatchCalculationResult
import cz.cvut.fel.dsva.grpc.NewWorkRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResult
import cz.cvut.fel.dsva.grpc.requestCalculationRequestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class JuliaSetServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val workStation: WorkStationConfig,
    private val jobService: JobService,
) : JuliaSetService {
    override suspend fun requestCalculation(request: BatchCalculationRequest): RequestCalculationRequestResult {
        this.workStation.vectorClock.update(request.vectorClockList)
        return if (systemJobStore.isSystemJobPresent()) {
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.ALREADY_IN_COMPUTATION
                vectorClock.addAll(workStation.vectorClock.toGrpcFormat())
            }
        } else {
            val newJob = Job(request.requester, request.requestsList)
            systemJobStore.persistNewSystemJob(job = newJob)
            runCalculationOnBackground()
            requestCalculationRequestResult {
                status = RequestCalculationRequestResponseStatus.OK
                vectorClock.addAll(workStation.vectorClock.toGrpcFormat())
            }
        }
    }

    override fun handleNewWorkRequest(newWorkRequest: NewWorkRequest): BatchCalculationRequest {
        return try {
            this.workStation.vectorClock.update(newWorkRequest.vectorClockList)
            val remoteWorkStation = this.workStation.findRemoteWorkStation(newWorkRequest.station)
            systemJobStore
                .getSystemJob()
                .createRemoteJob(this.workStation.batchSize, remoteWorkStation)
                .toBatchCalculationRequest(this.workStation.vectorClock.toGrpcFormat())
        } catch (e: IllegalStateException) {
            BatchCalculationRequest.getDefaultInstance()
        }
    }

    override fun handleDoneWork(calculationResult: BatchCalculationResult): Empty {
        this.workStation.vectorClock.update(calculationResult.vectorClockList)
        try {
            val remoteWorker = workStation.findRemoteWorkStation(calculationResult.worker)
            systemJobStore.getSystemJob().addCalculationResults(calculationResult.resultsList, remoteWorker)
        } catch (e: IllegalStateException) {
            //TODO log
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