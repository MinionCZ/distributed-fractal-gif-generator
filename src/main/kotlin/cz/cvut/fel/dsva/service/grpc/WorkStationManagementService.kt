package cz.cvut.fel.dsva.service.grpc

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.toRemoteWorkStation
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.service.WorkStationHttpManagementServiceImpl

class WorkStationManagementServiceImpl(
    workStationConfig: WorkStationConfig
) : WorkStationManagementService, BaseGrpcService<WorkStationManagementServiceImpl>(workStationConfig) {
    override val logger = LoggerWrapper(WorkStationManagementServiceImpl::class, workStationConfig)

    override fun join(request: WorkStation): Empty = applyDelay {
        try {
            workStationConfig.vectorClock.increment()
            workStationConfig.addRemoteWorkstation(request.toRemoteWorkStation())
            logger.info("Correctly joined new remote workstation $request")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $request is already registered")
        }
        return@applyDelay Empty.getDefaultInstance()
    }

    override fun leave(request: WorkStation): Empty = applyDelay {
        try {
            workStationConfig.vectorClock.increment()
            workStationConfig.removeRemoteWorkstation(request.toRemoteWorkStation())
            logger.info("Correctly removed remote workstation $request")
        } catch (e: IllegalStateException) {
            logger.info("Workstation $request has been already removed")
        }
        return@applyDelay Empty.getDefaultInstance()
    }
}

interface WorkStationManagementService {
    fun join(request: WorkStation): Empty
    fun leave(request: WorkStation): Empty
}
