package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.api.RemoteWorkStationDto
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.toRemoteWorkStation
import io.javalin.http.ConflictResponse
import kotlinx.coroutines.runBlocking


interface WorkStationHttpManagementService {
    fun join(remoteWorkStations: Collection<RemoteWorkStationDto>)
    fun leave()
    fun kill()
    fun revive()
}


class WorkStationHttpManagementServiceImpl(private val workStationConfig: WorkStationConfig) :
    WorkStationHttpManagementService {
    private val logger = LoggerWrapper(WorkStationHttpManagementService::class, workStationConfig)
    override fun join(remoteWorkStations: Collection<RemoteWorkStationDto>) {
        workStationConfig.vectorClock.increment()
        logger.info("Joining topology and sending information about joining to workstations $remoteWorkStations")
        for (station in remoteWorkStations) {
            try {
                val remoteWorkStation = workStationConfig.addRemoteWorkstation(station.toDao())
                runBlocking {
                    remoteWorkStation.createWorkStationManagementClient(workStationConfig).join()
                }
                logger.info("Registered in workstation $remoteWorkStation")
            } catch (e: IllegalStateException) {
                logger.info("Workstation $station already exists")
            }
        }
        workStationConfig.turnOnCommunication()
        workStationConfig.vectorClock.increment()
        logger.info("Successfully joined the workstations $remoteWorkStations and turned on")
    }

    override fun leave() {
        workStationConfig.vectorClock.increment()
        val remoteWorkStations = workStationConfig.getOtherWorkstations()
        logger.info("Leaving topology and sending information about leaving to workstations $remoteWorkStations")
        for (remoteWorkStation in remoteWorkStations) {
            try {
                workStationConfig.removeRemoteWorkstation(remoteWorkStation)
                runBlocking {
                    remoteWorkStation.createWorkStationManagementClient(workStationConfig).leave()
                }
                logger.info("Left workstation $remoteWorkStation")
            } catch (e: IllegalStateException) {
                logger.info("Workstation $remoteWorkStation already left")
            }
        }
        workStationConfig.turnOffCommunication()
        workStationConfig.vectorClock.increment()
        logger.info("Successfully left the workstations $remoteWorkStations")
    }

    override fun kill() {
        try {
            workStationConfig.vectorClock.increment()
            logger.info("Killing work station")
            workStationConfig.turnOffCommunication()
            workStationConfig.vectorClock.increment()
            logger.info("Work station killed")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $workStationConfig is already killed")
            throw ConflictResponse("Workstation $workStationConfig is already killed")
        }
    }

    override fun revive() {
        try {
            workStationConfig.vectorClock.increment()
            logger.info("Reviving work station")
            workStationConfig.turnOnCommunication()
            workStationConfig.vectorClock.increment()
            logger.info("Work station is running now")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $workStationConfig is already running")
            throw ConflictResponse("Workstation $workStationConfig is already running")
        }
    }


}