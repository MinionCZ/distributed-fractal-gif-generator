package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.images.ImagesGenerator
import cz.cvut.fel.dsva.input.UserInputHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface UserInputService {
    suspend fun startNewDistributedJob(userInputHolder: UserInputHolder)
}


class UserInputServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val workStationConfig: WorkStationConfig,
    private val imagesGenerator: ImagesGenerator,
    private val jobService: JobService,
) : UserInputService {
    override suspend fun startNewDistributedJob(userInputHolder: UserInputHolder) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val remoteJobs = prepareRemoteJobs()
                sendRemoteJobs(remoteJobs)
                jobService.calculateTasks()
                jobService.awaitCalculationFinish()
                imagesGenerator.createGif(
                    userInputHolder.gifProperties,
                    userInputHolder.imageProperties,
                    systemJobStore.getSystemJob().calculatedImagesCopy
                )
                systemJobStore.removeSystemJob()
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
}