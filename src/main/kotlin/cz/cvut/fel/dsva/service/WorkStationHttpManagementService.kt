package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.GrpcServerWrapper
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.api.RemoteWorkStationDto
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import io.javalin.http.ConflictResponse
import java.time.Duration
import kotlinx.coroutines.runBlocking


interface WorkStationHttpManagementService {
    fun join(remoteWorkStations: Collection<RemoteWorkStationDto>)
    fun leave()
    fun kill()
    fun revive()
    fun setDelay(delay: Duration)
}


class WorkStationHttpManagementServiceImpl(private val workStationConfig: WorkStationConfig) :
    WorkStationHttpManagementService {
    private val logger = LoggerWrapper(WorkStationHttpManagementService::class, workStationConfig)
    override fun join(remoteWorkStations: Collection<RemoteWorkStationDto>) {
        workStationConfig.vectorClock.increment()
        logger.info("Joining topology and sending information about joining to workstations $remoteWorkStations")
        if (workStationConfig.nodeRunning) {
            logger.info("Workstation is already running")
            throw ConflictResponse("Workstation is already running")
        }
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
        GrpcServerWrapper.getInstance().start()
        workStationConfig.vectorClock.increment()
        logger.info("Successfully joined the workstations $remoteWorkStations and turned on")
    }

    override fun leave() {
        workStationConfig.vectorClock.increment()
        val remoteWorkStations = workStationConfig.getOtherWorkstations()
        logger.info("Leaving topology and sending information about leaving to workstations $remoteWorkStations")
        if (!workStationConfig.nodeRunning) {
            logger.info("Workstation is already stopped")
            throw ConflictResponse("Workstation is already stopped")
        }
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
        GrpcServerWrapper.getInstance().stop()
        workStationConfig.vectorClock.increment()
        logger.info("Successfully left the workstations $remoteWorkStations")
    }

    override fun kill() {
        try {
            workStationConfig.vectorClock.increment()
            logger.info("Killing work station")
            if (!workStationConfig.nodeRunning) {
                logger.info("Workstation is already stopped")
                throw ConflictResponse("Workstation is already stopped")
            }
            GrpcServerWrapper.getInstance().stop()
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
            if (workStationConfig.nodeRunning) {
                logger.info("Workstation is already running")
                throw ConflictResponse("Workstation is already running")
            }
            workStationConfig.vectorClock.increment()
            GrpcServerWrapper.getInstance().start()
            logger.info("Work station is running now")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $workStationConfig is already running")
            throw ConflictResponse("Workstation $workStationConfig is already running")
        }
    }

    override fun setDelay(delay: Duration) {
        workStationConfig.vectorClock.increment()
        logger.info("Setting messaging delay to $delay")
        workStationConfig.messageDelay = delay
        logger.info("Successfully set delay to $delay")
    }
}