package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.SystemJobStore
import cz.cvut.fel.dsva.datastructure.toRemoteWorkStation
import cz.cvut.fel.dsva.images.ImagesGenerator
import cz.cvut.fel.dsva.input.UserInputHolder
import kotlinx.coroutines.CoroutineScope
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
            launch(Dispatchers.Default) {
                val remoteJobs = prepareRemoteJobs()
                sendRemoteJobs(remoteJobs)
                jobService.calculateTasks()
                jobService.awaitCalculationFinish()
                workStationConfig.vectorClock.increment()
                logger.info("Starting gif writing")
                imagesGenerator.createGif(
                    userInputHolder.gifProperties,
                    userInputHolder.imageProperties,
                    systemJobStore.getSystemJob().calculatedImagesCopy
                )
                workStationConfig.vectorClock.increment()
                logger.info("Done writing of gif")
                systemJobStore.removeSystemJob()
                workStationConfig.vectorClock.increment()
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

    private fun sendRemoteJobs(jobs: List<RemoteTaskBatch>) {
        workStationConfig.vectorClock.increment()
        logger.info("Sending remote jobs")
        for (job in jobs) {
            CoroutineScope(Dispatchers.IO).launch {
                logger.info("Sending remote job to ${job.worker.workStation.toRemoteWorkStation()}")
                job.worker.createClient().use {
                    try {
                        val remoteJobBatch = job.toBatchCalculationRequest(workStationConfig)
                        it.sendCalculationRequest(remoteJobBatch)
                        logger.info("Successfully sent job to ${job.worker.workStation.toRemoteWorkStation()} with image payload ${remoteJobBatch.requestsList.map { it.imageProperties.id }}")
                    } catch (e: IllegalStateException) {
                        systemJobStore.getSystemJob().deleteRemoteJob(job)
                        logger.info("Failed to send job to ${job.worker.workStation.toRemoteWorkStation()}")
                    }
                }
            }
        }
    }
}