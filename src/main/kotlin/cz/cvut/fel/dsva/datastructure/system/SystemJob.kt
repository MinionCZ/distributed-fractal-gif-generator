package cz.cvut.fel.dsva.datastructure.system

import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationResult
import cz.cvut.fel.dsva.grpc.WorkStation
import java.time.LocalDateTime
import java.util.LinkedList

class SystemJob(
    val workRequester: WorkStation,
    tasks: List<CalculationRequest>,
) {
    private val remoteTasks: MutableList<RemoteTaskBatch> = LinkedList()
    private val tasks = LinkedList(tasks)
    private val calculatedImages = LinkedList<CalculationResult>()

    fun popFirstTask(): CalculationRequest? {
        return synchronized(this) {
            if (tasks.isEmpty()) {
                null
            } else {
                tasks.removeFirst()
            }
        }
    }

    fun addCalculationResult(calculationResult: CalculationResult) {
        synchronized(this) {
            calculatedImages.add(calculationResult)
        }
    }

    fun createRemoteJob(numberOfTasks: Int, worker: WorkStation): RemoteTaskBatch {
        return synchronized(this) {
            check(numberOfTasks <= tasks.size) {
                "Not enough tasks to create remote job"
            }
            val remoteTasks = LinkedList(tasks.subList(tasks.size - numberOfTasks, tasks.size))
            tasks.removeAll(remoteTasks)
            val remoteTaskBatch = RemoteTaskBatch(remoteTasks, LocalDateTime.now(), worker)
            this.remoteTasks.add(remoteTaskBatch)
            remoteTaskBatch
        }
    }


    fun addCalculationResults(calculationResults: List<CalculationResult>, workStation: WorkStation) {
        synchronized(this) {
            val removed = remoteTasks.removeIf {
                it.worker == workStation
            }
            check(removed) {
                "Unknown results from worker"
            }
            calculatedImages.addAll(calculationResults)
        }
    }
}

interface SystemJobStore {
    fun isSystemJobPresent(): Boolean
    fun getSystemJob(): SystemJob
    fun persistNewSystemJob(systemJob: SystemJob)
    fun removeSystemJob()
}

class SystemJobStoreImpl : SystemJobStore {
    private var systemJob: SystemJob? = null

    override fun isSystemJobPresent(): Boolean {
        return synchronized(this) { systemJob != null }
    }

    override fun getSystemJob(): SystemJob {
        return synchronized(this) { systemJob ?: error("System job is null") }
    }

    override fun persistNewSystemJob(systemJob: SystemJob) {
        synchronized(this) { this.systemJob = systemJob }
    }

    override fun removeSystemJob() {
        synchronized(this) { systemJob = null }
    }
}



