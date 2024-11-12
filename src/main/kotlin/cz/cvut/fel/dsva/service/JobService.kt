package cz.cvut.fel.dsva.service

import com.google.protobuf.ByteString
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.RemoteWorkStation
import cz.cvut.fel.dsva.datastructure.RequestedTaskBatch
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.SystemJobStore
import cz.cvut.fel.dsva.datastructure.toRemoteWorkStation
import cz.cvut.fel.dsva.grpc.BatchCalculationRequest
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
import cz.cvut.fel.dsva.grpc.batchCalculationResult
import cz.cvut.fel.dsva.grpc.calculationResult
import cz.cvut.fel.dsva.grpc.newWorkRequest
import cz.cvut.fel.dsva.images.ImagesGenerator
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface JobService {
    fun calculateTasks()
    fun awaitCalculationFinish()
    fun sendRemoteJobs(jobs: List<RemoteTaskBatch>)
    fun prepareRemoteJobs(): List<RemoteTaskBatch>
}


class JobServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val imagesGenerator: ImagesGenerator,
    private val workStationConfig: WorkStationConfig,
) : JobService {
    private val logger = LoggerWrapper(JobServiceImpl::class, workStationConfig = workStationConfig)

    override fun calculateTasks() {
        workStationConfig.vectorClock.increment()
        logger.info("Starting task calculation")
        while (true) {
            val task = systemJobStore.getSystemJob().popFirstTask() ?: break
            val calculatedPixels = imagesGenerator.generateJuliaSetImage(task.imageProperties, task.juliaSetProperties)
            systemJobStore.getSystemJob().addCalculationResult(calculationResult {
                imageProperties = task.imageProperties
                pixels = ByteString.copyFrom(calculatedPixels)
            })
        }
        workStationConfig.vectorClock.increment()
        logger.info("Finished task calculation")
    }

    override fun awaitCalculationFinish() {
        runBlocking {
            workStationConfig.vectorClock.increment()
            logger.info("Waiting for calculation finish")
            do {
                val status = systemJobStore.getSystemJob().checkIfWorkIsDone()
                if (!status.remoteTasksCalculated) {
                    val timeOutedJobs =
                        systemJobStore.getSystemJob().getTimeOutedJobs(workStationConfig.maxCalculationDuration)
                    systemJobStore.getSystemJob().deleteRemoteJobs(timeOutedJobs)
                }
                if (!status.tasksCalculated) {
                    coroutineScope {
                        launch(Dispatchers.IO) {
                            calculateTasks()
                        }
                    }
                }
                if (status.timeToReturnRequestedJob()) {
                    returnCalculationResult()
                }
                if (status.requestedTasksCalculated) {
                    requestForWorkOthers()
                }
                if (!status.tasksCalculated || !status.remoteTasksCalculated) {
                    Thread.sleep(DELAY.toMillis())
                }
            } while (!systemJobStore.getSystemJob().checkIfWorkIsDone().addDone())
            workStationConfig.vectorClock.increment()
            logger.info("Calculation has finished")
        }
    }

    private suspend fun requestForWorkOthers() {
        workStationConfig.vectorClock.increment()
        logger.info("Request work from other stations started")
        for (remoteWorkStation in workStationConfig.otherWorkstations) {
            logger.info("Requesting work from $remoteWorkStation")
            val response = remoteWorkStation.createClient().use {
                try {
                    it.requestNewWorkload(newWorkRequest {
                        vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
                        station = workStationConfig.toWorkStation()
                    })
                } catch (e: IllegalStateException) {
                    logger.info("Unable to request work from $remoteWorkStation")
                    null
                }
            }
            if (response != null && handleNewWorkloadResponse(response)) {
                break
            }
        }
        coroutineScope {
            launch(Dispatchers.IO) {
                calculatedRequestedWork()
                sendCalculationRequestResult()
            }
        }
    }

    private fun handleNewWorkloadResponse(batchCalculationRequest: BatchCalculationRequest): Boolean {
        workStationConfig.vectorClock.update(batchCalculationRequest.vectorClockList)
        if (batchCalculationRequest.requestsCount == 0) {
            logger.info("No tasks received from machine ${batchCalculationRequest.requester.toRemoteWorkStation()}")
            return false
        }
        systemJobStore.getSystemJob().enqueueNewRequestedTask(
            RequestedTaskBatch(
                batchCalculationRequest.requestsList,
                LocalDateTime.now(),
                batchCalculationRequest.requester.toRemoteWorkStation()
            )
        )
        workStationConfig.vectorClock.increment()
        logger.info("Enqueued new request for remote job calculation")
        return true
    }


    private fun calculatedRequestedWork() {
        workStationConfig.vectorClock.increment()
        logger.info("Starting calculation of requested work")
        while (true) {
            val task = systemJobStore.getSystemJob().getRequestedTask()?.popTask() ?: break
            val result = imagesGenerator.generateJuliaSetImage(task.imageProperties, task.juliaSetProperties)
            systemJobStore.getSystemJob().getRequestedTask()?.addCalculationResult(calculationResult {
                imageProperties = task.imageProperties
                pixels = ByteString.copyFrom(result)
            })
        }
        logger.info("Finished calculation of requested work")
    }

    private suspend fun sendCalculationRequestResult() {
        workStationConfig.vectorClock.increment()
        logger.info(
            "Starting sending calculation result to ${
                systemJobStore.getSystemJob().getRequestedTask()?.requester
            }"
        )
        systemJobStore.getSystemJob().getRequestedTask()?.let { task ->
            task.requester.createClient().use {
                try {
                    it.sendCompletedCalculation(batchCalculationResult {
                        worker = workStationConfig.toWorkStation()
                        vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
                        results.addAll(task.getAllResults())
                    })
                    logger.info("Successfully sent calculation result to ${task.requester}")
                } catch (e: IllegalStateException) {
                    logger.info("Unable to send calculation result to ${task.requester}")
                }
            }
        }
        systemJobStore.getSystemJob().clearRequestedTask()
    }

    private suspend fun returnCalculationResult() {
        systemJobStore.getSystemJob().workRequester?.let { requester ->
            workStationConfig.vectorClock.increment()
            val resultList = systemJobStore.getSystemJob().getAndClearCalculatedImages()
            logger.info("Trying to send calculation results to starting point of this calculation $requester")
            requester.createClient().use {
                it.sendCompletedCalculation(batchCalculationResult {
                    worker = workStationConfig.toWorkStation()
                    vectorClock.addAll(workStationConfig.vectorClock.toGrpcFormat())
                    results.addAll(resultList)
                })
                logger.info("Successfully sent result to requester $requester")
            }
        }
    }

    override fun sendRemoteJobs(jobs: List<RemoteTaskBatch>) {
        workStationConfig.vectorClock.increment()
        logger.info("Sending remote jobs")
        for (job in jobs) {
            CoroutineScope(Dispatchers.IO).launch {
                logger.info("Sending remote job to ${job.worker.workStation.toRemoteWorkStation()}")
                job.worker.createClient().use {
                    try {
                        val remoteJobBatch = job.toBatchCalculationRequest(workStationConfig)
                        val requestStatus = it.sendCalculationRequest(remoteJobBatch)
                        if (requestStatus.status == RequestCalculationRequestResponseStatus.OK) {
                            logger.info("Successfully sent job to ${job.worker.workStation.toRemoteWorkStation()} with image payload ${remoteJobBatch.requestsList.map { it.imageProperties.id }}")
                        } else {
                            logger.info("Machine ${job.worker.workStation.toRemoteWorkStation()} is already computing")
                            systemJobStore.getSystemJob().deleteRemoteJob(job)
                        }
                    } catch (e: IllegalStateException) {
                        systemJobStore.getSystemJob().deleteRemoteJob(job)
                        logger.info("Failed to send job to ${job.worker.workStation.toRemoteWorkStation()}")
                    }
                }
            }
        }
    }

    override fun prepareRemoteJobs(): List<RemoteTaskBatch> {
        val jobs = ArrayList<RemoteTaskBatch>()
        for (remoteWorkStation in workStationConfig.otherWorkstations) {
            val taskBatch = systemJobStore.getSystemJob()
                .createRemoteJob(workStationConfig.batchSize, remoteWorkStation)
            jobs.add(taskBatch)
        }
        return jobs
    }

    private companion object {
        private val DELAY = Duration.ofSeconds(1)
    }
}