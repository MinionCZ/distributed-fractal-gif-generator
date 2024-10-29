package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.datastructure.system.WorkStation
import cz.cvut.fel.dsva.datastructure.user.ComplexNumber
import java.time.LocalDateTime


data class Task(
    val id: Int,
    val lowerCorner: ComplexNumber,
    val upperCorner: ComplexNumber,
    val offset: ComplexNumber,
    val numberOfIterations: Int
)

data class RemoteTaskBatch(
    val tasks: List<Task>,
    val startTimestamp: LocalDateTime,
    val worker: WorkStation,
)