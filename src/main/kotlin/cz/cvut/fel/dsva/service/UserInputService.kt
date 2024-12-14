package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.api.GenerateImageDto
import cz.cvut.fel.dsva.datastructure.Job
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.SystemJobStore
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.calculationRequest
import cz.cvut.fel.dsva.grpc.juliaSetProperties
import cz.cvut.fel.dsva.images.ImagesGenerator
import java.util.LinkedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface UserInputService {
    suspend fun startNewDistributedJob(generateImageDto: GenerateImageDto)
}


class UserInputServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val workStationConfig: WorkStationConfig,
    private val imagesGenerator: ImagesGenerator,
    private val jobService: JobService,
) : UserInputService {
    private val logger = LoggerWrapper(UserInputServiceImpl::class, workStationConfig)
    override suspend fun startNewDistributedJob(generateImageDto: GenerateImageDto) {
        validateWorkstationState()
        enqueueNewUserJob(generateImageDto)
        logger.info("Starting new distributed job")
        coroutineScope {
            launch(Dispatchers.Default) {
                jobService.sendRemoteJobs(jobService.prepareRemoteJobs())
                jobService.calculateTasks()
                jobService.awaitCalculationFinish()
                workStationConfig.vectorClock.increment()
                logger.info("Starting gif writing")
                imagesGenerator.createGif(
                    generateImageDto.gifProperties,
                    generateImageDto.imageProperties,
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

    private fun createTasksFromUserInput(generateImageDto: GenerateImageDto): List<CalculationRequest> {
        val offsetStep =
            (generateImageDto.juliaSetProperties.endingOffset - generateImageDto.juliaSetProperties.startingOffset).divByScalar(
                generateImageDto.gifProperties.numberOfFrames.toDouble()
            )
        val iterationStep =
            (generateImageDto.juliaSetProperties.endingNumberOfIterations - generateImageDto.juliaSetProperties.startingNumberOfIterations) / generateImageDto.gifProperties.numberOfFrames

        val tasks = LinkedList<CalculationRequest>()
        var calculationOffset = generateImageDto.juliaSetProperties.startingOffset
        var iteration = generateImageDto.juliaSetProperties.startingNumberOfIterations
        val topRightCornerInProtobufFormat = generateImageDto.juliaSetProperties.topRightCorner.toProtobufFormat()
        val bottomLeftCornerInProtobufFormat = generateImageDto.juliaSetProperties.bottomLeftCorner.toProtobufFormat()
        for (i in 0..<generateImageDto.gifProperties.numberOfFrames) {
            val task = calculationRequest {
                imageProperties = generateImageDto.imageProperties.toProtobufFormat(i)
                juliaSetProperties = juliaSetProperties {
                    offset = calculationOffset.toProtobufFormat()
                    topRightCorner = topRightCornerInProtobufFormat
                    bottomLeftCorner = bottomLeftCornerInProtobufFormat
                    escapeRadius = generateImageDto.juliaSetProperties.escapeRadius
                    maxIterations = iteration
                }
            }
            calculationOffset += offsetStep
            iteration += iterationStep
            tasks.add(task)
        }
        return tasks
    }

    private fun validateWorkstationState() {
        if (systemJobStore.isSystemJobPresent()) {
            error("System is already calculating, please wait until calculation is done")
        }
    }

    private fun enqueueNewUserJob(generateImageDto: GenerateImageDto) {
        val newJob = Job(
            workRequester = null,
            createTasksFromUserInput(generateImageDto)
        )
        systemJobStore.persistNewSystemJob(newJob)
    }
}