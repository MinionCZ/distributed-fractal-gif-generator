package cz.cvut.fel.dsva.api

import com.google.protobuf.Empty
import cz.cvut.fel.dsva.grpc.WorkStation
import cz.cvut.fel.dsva.grpc.WorkStationManagementGrpcKt
import cz.cvut.fel.dsva.service.WorkStationManagementService

class WorkStationManagementApiHandler(private val workStationManagementService: WorkStationManagementService) :
    WorkStationManagementGrpcKt.WorkStationManagementCoroutineImplBase() {

    override suspend fun join(request: WorkStation): Empty {
        workStationManagementService.join(request)
        return Empty.getDefaultInstance()
    }

    override suspend fun leave(request: WorkStation): Empty {
        workStationManagementService.leave(request)
        return Empty.getDefaultInstance()
    }
}