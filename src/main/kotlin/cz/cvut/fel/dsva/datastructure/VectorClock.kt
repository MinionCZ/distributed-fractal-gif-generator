package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.Clock
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.clock
import java.util.LinkedHashMap
import kotlin.math.max

class VectorClock(
    private val currentWorkStation: WorkStationConfig,
    private val remoteWorkStations: List<RemoteWorkStation>
) {
    private val vectorClock: MutableMap<WorkStation, Long> = LinkedHashMap<WorkStation, Long>()

    init {
        vectorClock[currentWorkStation.toWorkStation()] = 0
        for (remoteWorkStation in remoteWorkStations) {
            vectorClock[remoteWorkStation.workStation] = 0
        }
    }

    fun toGrpcFormat(): Collection<Clock> = synchronized(this) {
        this.vectorClock.map { (workStation, counter) ->
            clock {
                station = workStation
                timestamp = counter
            }
        }
    }

    fun increment() {
        synchronized(this) {
            val currentValue = vectorClock[currentWorkStation.toWorkStation()]
                ?: error("Counter for current workstation ${currentWorkStation.toWorkStation()} is null")
            vectorClock[currentWorkStation.toWorkStation()] = currentValue + 1
        }
    }

    fun update(receivedClocks: Collection<Clock>) {
        synchronized(this) {
            for (clock in receivedClocks) {
                val foundValue = this.vectorClock[clock.station] ?: continue
                this.vectorClock[clock.station] = max(foundValue, clock.timestamp)
            }
            increment()
        }
    }

    fun toLogFormat(): String = synchronized(this) {
        val sortedClock = this.vectorClock.toList().sortedWith(compareBy({ it.first.ip }, { it.first.port }))
        val builder = StringBuilder()
        sortedClock.forEachIndexed { index, pair ->
            if (pair.first == this.currentWorkStation.toWorkStation()) {
                builder.append("${CURRENT_MACHINE_COLOR_START}${pair.second}$RESET_COLOR")
            } else {
                builder.append(pair.second)
            }
            if (index != sortedClock.size - 1) {
                builder.append(" ")
            }
        }
        builder.toString()
    }


    private companion object {
        private const val CURRENT_MACHINE_COLOR_START = "\u001b[32m"
        private const val RESET_COLOR = "\u001b[0m"
    }
}
