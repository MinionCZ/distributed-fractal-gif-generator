package cz.cvut.fel.dsva.service

import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.grpc.WorkStation
import io.github.oshai.kotlinlogging.KotlinLogging

class WorkStationManagementService(
    private val workStationConfig: WorkStationConfig
) {
    private val logger = LoggerWrapper(WorkStationManagementService::class, workStationConfig)

    fun join(request: WorkStation) {
        try {
            workStationConfig.vectorClock.increment()
            workStationConfig.addRemoteWorkstation(request)
            logger.info("Correctly joined new remote workstation $request")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $request is already registered")
        }
    }

    fun leave(request: WorkStation) {
        try {
            workStationConfig.vectorClock.increment()
            workStationConfig.removeRemoteWorkstation(request)
            logger.info("Correctly removed remote workstation $request")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $request has been already removed")
        }
    }


}