package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.Clock
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.clock
import java.util.LinkedHashMap
import kotlin.math.max

class VectorClock(
    private val currentWorkStation: WorkStationConfig,
    remoteWorkStations: Collection<RemoteWorkStation>
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
}