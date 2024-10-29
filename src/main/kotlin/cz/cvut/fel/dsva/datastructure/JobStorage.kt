package cz.cvut.fel.dsva.datastructure

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface JobStorage {
    fun addNewJobFromUser(job: Job): Job
    fun getJobById(jobId: UUID): Job?
    fun removeJobById(jobId: UUID): Job?
}


object JobStorageImpl : JobStorage {

    private val storage: MutableMap<UUID, Job> = ConcurrentHashMap()

    override fun addNewJobFromUser(job: Job): Job {
        storage[job.id] = job
        return job
    }

    override fun getJobById(jobId: UUID): Job? {
        return storage[jobId]
    }

    override fun removeJobById(jobId: UUID): Job? {
        return storage.remove(jobId)
    }
}