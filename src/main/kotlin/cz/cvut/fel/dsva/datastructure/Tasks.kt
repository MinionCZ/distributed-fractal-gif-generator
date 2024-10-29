package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.datastructure.system.WorkStation
import cz.cvut.fel.dsva.datastructure.user.ComplexNumber
import cz.cvut.fel.dsva.grpc.ImageProperties
import cz.cvut.fel.dsva.grpc.JuliaSetProperties
import java.time.LocalDateTime


class Task(
    val imageProperties: ImageProperties,
    val juliaSetProperties: JuliaSetProperties,
)

data class RemoteTaskBatch(
    val tasks: List<Task>,
    val startTimestamp: LocalDateTime,
    val worker: WorkStation,
)