package cz.cvut.fel.dsva.service

import com.google.protobuf.ByteString
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.grpc.calculationResult
import cz.cvut.fel.dsva.images.ImagesGenerator
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface JobService {
    fun calculateTasks()
    fun awaitCalculationFinish()
}


class JobServiceImpl(
    private val systemJobStore: SystemJobStore,
    private val imagesGenerator: ImagesGenerator,
    private val workStationConfig: WorkStationConfig,
) : JobService {
    override fun calculateTasks() {
        while (true) {
            val task = systemJobStore.getSystemJob().popFirstTask() ?: break
            val calculatedPixels = imagesGenerator.generateJuliaSetImage(task.imageProperties, task.juliaSetProperties)
            systemJobStore.getSystemJob().addCalculationResult(calculationResult {
                imageProperties = task.imageProperties
                pixels = ByteString.copyFrom(calculatedPixels)
            })
        }
    }

    override fun awaitCalculationFinish() {
        runBlocking {
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
                    Thread.sleep(DELAY.toMillis())
                }
            } while (!status.tasksCalculated || !status.remoteTasksCalculated)
        }
    }

    private companion object {
        private val DELAY = Duration.ofSeconds(1)
    }
}