package cz.cvut.fel.dsva.datastructure.system

import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.Task
import cz.cvut.fel.dsva.grpc.CalculationResult
import java.util.LinkedList

class SystemJob(
    val workRequester: WorkStation,
    tasks: List<Task>,
) {
    private val remoteTasks: MutableList<RemoteTaskBatch> = ArrayList()
    private val tasks = LinkedList(tasks)
    private val calculatedImages = LinkedList<CalculationResult>()

    fun removeTask(task: Task) {
        synchronized(this) {
            tasks.remove(task)
        }
    }

    fun popFirstTask(): Task? {
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



