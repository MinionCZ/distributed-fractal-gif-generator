package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.CalculationResult
import java.time.Duration
import java.time.LocalDateTime
import java.util.LinkedList

class Job(
    val workRequester: RemoteWorkStation?,
    tasks: List<CalculationRequest>,
) {
    private val remoteTasks: MutableList<RemoteTaskBatch> = LinkedList()
    private val tasks = LinkedList(tasks)
    private val calculatedImages = LinkedList<CalculationResult>()
    private var requestedTask: RequestedTaskBatch? = null

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
        fun compareCalculationResultsWithTask(
            calculationResultsImageIds: Set<Int>,
            remoteTask: RemoteTaskBatch
        ): Boolean {
            val remoteTaskImageIds = remoteTask.tasks.map { it.imageProperties.id }.toSet()
            return (calculationResultsImageIds == remoteTaskImageIds)
        }

        synchronized(this) {
            val calculationResultsIds = calculationResults.map { it.imageProperties.id }.toSet()
            val removed = remoteTasks.removeIf {
                it.worker == workStation && compareCalculationResultsWithTask(calculationResultsIds, it)
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
            val remoteTasksCalculated = remoteTasks.isEmpty()
            val requestedTasksCalculated = this.requestedTask == null
            val jobsReturned =
                workRequester == null || (this.calculatedImages.isEmpty() && tasksCalculated && remoteTasksCalculated)
            return ComputationStatus(tasksCalculated, remoteTasksCalculated, requestedTasksCalculated, jobsReturned)
        }
    }

    fun getTimeOutedJobs(maxRunDuration: Duration): List<RemoteTaskBatch> {
        synchronized(this) {
            return this.remoteTasks.filter { it.isTimeOuted(maxRunDuration) }
        }
    }

    fun enqueueNewRequestedTask(requestedTask: RequestedTaskBatch) {
        synchronized(this) {
            if (this.requestedTask != null) {
                error("Requested task is already full")
            }
            this.requestedTask = requestedTask
        }
    }

    fun getRequestedTask(): RequestedTaskBatch? = synchronized(this) {
        this.requestedTask
    }

    fun clearRequestedTask() {
        synchronized(this) {
            this.requestedTask = null
        }
    }

    fun getAndClearCalculatedImages(): List<CalculationResult> {
        synchronized(this) {
            val result = this.calculatedImagesCopy
            this.calculatedImages.clear()
            return result
        }
    }


    data class ComputationStatus(
        val tasksCalculated: Boolean,
        val remoteTasksCalculated: Boolean,
        val requestedTasksCalculated: Boolean,
        val resultsReturnedToJobRequester: Boolean,
    ) {
        fun addDone(): Boolean =
            tasksCalculated && remoteTasksCalculated && requestedTasksCalculated && resultsReturnedToJobRequester

        fun timeToReturnRequestedJob(): Boolean = tasksCalculated && remoteTasksCalculated && requestedTasksCalculated
    }
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
        }
    }

    override fun removeSystemJob() {
        synchronized(this) {
            job = null
        }
    }
}



