package cz.cvut.fel.dsva.datastructure.system

import java.net.InetAddress

data class WorkStation(
    val ip: InetAddress,
    var state: WorkStationState,
)

enum class WorkStationState {
    IDLE,
    WORKING
}
