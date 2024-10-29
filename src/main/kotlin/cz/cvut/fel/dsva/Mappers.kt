package cz.cvut.fel.dsva

import cz.cvut.fel.dsva.datastructure.Task
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.Pixel

fun Pixel.toRenderPixel(): com.sksamuel.scrimage.pixels.Pixel =
    com.sksamuel.scrimage.pixels.Pixel(this.x, this.y, this.argb)

fun Collection<CalculationRequest>.toTasks(): List<Task> = this.map {
    Task(it.imageProperties, it.juliaSetProperties)
}