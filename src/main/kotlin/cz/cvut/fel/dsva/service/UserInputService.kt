package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.SystemJobStore
import cz.cvut.fel.dsva.datastructure.toRemoteWorkStation
import cz.cvut.fel.dsva.grpc.RequestCalculationRequestResponseStatus
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
                jobService.sendRemoteJobs(jobService.prepareRemoteJobs())
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
}