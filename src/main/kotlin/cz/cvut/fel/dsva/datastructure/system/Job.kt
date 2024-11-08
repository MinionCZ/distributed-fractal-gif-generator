package cz.cvut.fel.dsva.datastructure.system

import com.google.rpc.Help.Link
import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.RemoteWorkStation
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationResult
import cz.cvut.fel.dsva.grpc.WorkStation
import java.time.Duration
import java.time.LocalDateTime
import java.util.LinkedList

class Job(
    val workRequester: WorkStation,
    tasks: List<CalculationRequest>,
) {
    private val remoteTasks: MutableList<RemoteTaskBatch> = LinkedList()
    private val tasks = LinkedList(tasks)
    private val calculatedImages = LinkedList<CalculationResult>()

    val calculatedImagesCopy: List<CalculationResult>
        get() = LinkedList(this.calculatedImages)

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

    fun createRemoteJob(numberOfTasks: Int, worker: RemoteWorkStation): RemoteTaskBatch {
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


    fun addCalculationResults(calculationResults: List<CalculationResult>, workStation: RemoteWorkStation) {
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

    fun deleteRemoteJob(remoteTaskBatch: RemoteTaskBatch) {
        synchronized(this) {
            remoteTasks.remove(remoteTaskBatch)
            tasks.addAll(remoteTaskBatch.tasks)
        }
    }

    fun deleteRemoteJobs(remoteTaskBatches: List<RemoteTaskBatch>) {
        synchronized(this) {
            for (remoteTaskBatch in remoteTaskBatches) {
                if (remoteTasks.remove(remoteTaskBatch)) {
                    tasks.addAll(remoteTaskBatch.tasks)
                }
            }
        }
    }

    fun checkIfWorkIsDone(): ComputationStatus {
        synchronized(this) {
            val tasksCalculated = this.tasks.isEmpty()
            val remoteTasksCalculated = tasks.isEmpty()
            return ComputationStatus(tasksCalculated, remoteTasksCalculated)
        }
    }

    fun getTimeOutedJobs(timeout: Duration): List<RemoteTaskBatch> {
        synchronized(this) {
            val cutOffTime = LocalDateTime.now()
            return this.remoteTasks.filter { it.startTimestamp.plus(timeout) <= cutOffTime }
        }
    }


    data class ComputationStatus(val tasksCalculated: Boolean, val remoteTasksCalculated: Boolean)
}

interface SystemJobStore {
    fun isSystemJobPresent(): Boolean
    fun getSystemJob(): Job
    fun persistNewSystemJob(job: Job)
    fun removeSystemJob()
}

class SystemJobStoreImpl(private val workStationConfig: WorkStationConfig) : SystemJobStore {
    private var job: Job? = null

    override fun isSystemJobPresent(): Boolean {
        return synchronized(this) { job != null }
    }

    override fun getSystemJob(): Job {
        return synchronized(this) { job ?: error("System job is null") }
    }

    override fun persistNewSystemJob(job: Job) {
        synchronized(this) {
            if (this.job != null) {
                error("System job is already persisted")
            }
            this.job = job
            workStationConfig.vectorClock.increment()
        }
    }

    override fun removeSystemJob() {
        synchronized(this) {
            job = null
            workStationConfig.vectorClock.increment()
        }
    }
}



