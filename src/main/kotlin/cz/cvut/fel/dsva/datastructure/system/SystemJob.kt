package cz.cvut.fel.dsva.datastructure.system

import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.datastructure.Task
import java.util.concurrent.CopyOnWriteArrayList

data class SystemJob(
    val workRequester: WorkStation,
    val tasks: List<Task>,
) {
    val remoteTasks: Collection<RemoteTaskBatch> = CopyOnWriteArrayList()
}