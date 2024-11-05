package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.grpc.calculationResult
import cz.cvut.fel.dsva.images.ImagesGenerator
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface UserInputService {
    suspend fun startNewDistributedJob()
}


class UserInputServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val workStationConfig: WorkStationConfig,
    private val imagesGenerator: ImagesGenerator,
) : UserInputService {
    override suspend fun startNewDistributedJob() {
        coroutineScope {
            launch(Dispatchers.IO) {
                val remoteJobs = prepareRemoteJobs()
                sendRemoteJobs(remoteJobs)
                calculateTasks()
                awaitCalculationFinish()
            }
        }

    }

    private fun prepareRemoteJobs(): List<RemoteTaskBatch> {
        val jobs = ArrayList<RemoteTaskBatch>()
        for (remoteWorkStation in workStationConfig.otherWorkstations) {
            val taskBatch = systemJobStore.getSystemJob()
                .createRemoteJob(workStationConfig.batchSize, remoteWorkStation)
            jobs.add(taskBatch)
        }
        return jobs
    }

    private suspend fun sendRemoteJobs(jobs: List<RemoteTaskBatch>) {
        for (job in jobs) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    job.worker.createClient().use {
                        try {
                            it.sendCalculationRequest(job.toBatchCalculationRequest())
                        } catch (e: IllegalArgumentException) {
                            systemJobStore.getSystemJob().deleteRemoteJob(job)
                        }
                    }
                }
            }
        }
    }

    private fun calculateTasks() {
        while (true) {
            val task = systemJobStore.getSystemJob().popFirstTask() ?: break
            val calculatedPixels = imagesGenerator.generateJuliaSetImage(task.imageProperties, task.juliaSetProperties)
            systemJobStore.getSystemJob().addCalculationResult(calculationResult {
                imageProperties = task.imageProperties
                pixels.addAll(calculatedPixels.toList())
            })
        }
    }

    private suspend fun awaitCalculationFinish() {
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
            if (!status.tasksCalculated || !status.remoteTasksCalculated) {
                delay(DELAY.toMillis())
            }
        } while (!status.tasksCalculated || !status.remoteTasksCalculated)

    }

    companion object {
        private val DELAY = Duration.ofSeconds(1)
    }
}