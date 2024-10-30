package cz.cvut.fel.dsva.datastructure.user

import cz.cvut.fel.dsva.datastructure.RemoteTaskBatch
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.Pixel
import java.util.concurrent.CopyOnWriteArrayList

data class UserJob(
    val width: Int,
    val height: Int,
    val escapeRadius: Double,
    val remainingTasks: MutableList<CalculationRequest>,
    val resultName: String,
) {
    val calculatedImages: MutableList<CalculatedImage> = CopyOnWriteArrayList()
    val remoteTasks: Collection<RemoteTaskBatch> = CopyOnWriteArrayList()
}


data class ComplexNumber(val real: Double, val imaginary: Double)

data class CalculatedImage(
    val id: Int,
    val pixels: Array<Pixel>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CalculatedImage

        if (id != other.id) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}