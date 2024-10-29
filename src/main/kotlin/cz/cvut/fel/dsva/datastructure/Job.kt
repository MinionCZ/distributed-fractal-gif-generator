package cz.cvut.fel.dsva.datastructure

import cz.cvut.fel.dsva.grpc.Pixel
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

data class Job(
    val width: Int,
    val height: Int,
    val escapeRadius: Double,
    val remainingTasks: MutableList<Task>,
    val resultName: String,
) {
    val id: UUID = UUID.randomUUID()
    val calculatedImages: MutableList<CalculatedImage> = CopyOnWriteArrayList()
}

data class Task(
    val id: Int,
    val lowerCorner: ComplexNumber,
    val upperCorner: ComplexNumber,
    val offset: ComplexNumber,
    val numberOfIterations: Int
)

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