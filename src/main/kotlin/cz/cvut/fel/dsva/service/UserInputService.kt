package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.images.ImagesGenerator
import cz.cvut.fel.dsva.input.UserInputHolder
import kotlin.math.log
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
    private val logger = LoggerWrapper(UserInputServiceImpl::class, workStationConfig)
    override suspend fun startNewDistributedJob(userInputHolder: UserInputHolder) {
        logger.info("Starting new distributed job")
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
                logger.info("Done job")
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
        workStationConfig.vectorClock.increment()
        logger.info("Sending remote jobs")
        for (job in jobs) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    logger.info("Sending remote job to ${job.worker.workStation}")
                    job.worker.createClient().use {
                        try {
                            it.sendCalculationRequest(job.toBatchCalculationRequest(workStationConfig.vectorClock.toGrpcFormat()))
                            logger.info("Successfully sent job to ${job.worker.workStation}")
                        } catch (e: IllegalArgumentException) {
                            systemJobStore.getSystemJob().deleteRemoteJob(job)
                            logger.info("Failed to send job to ${job.worker.workStation}")
                        }
                    }
                }
            }
        }
    }
}