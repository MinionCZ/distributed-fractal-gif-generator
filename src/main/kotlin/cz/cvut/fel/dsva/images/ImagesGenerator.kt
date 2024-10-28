package cz.cvut.fel.dsva.images

import cz.cvut.fel.dsva.grpc.ImageProperties
import cz.cvut.fel.dsva.grpc.JuliaSetProperties
import kotlin.math.pow


data class Pixel(
    val r: Int,
    val g: Int,
    val b: Int,
) {
    companion object {
        const val SIZE_IN_BYTES = 3
        val BLACK = Pixel(0, 0, 0)
    }
}

interface ImagesGenerator {
    fun generateJuliaSetImage(imageProperties: ImageProperties, juliaSetProperties: JuliaSetProperties): ByteArray
}

object ImagesGeneratorImpl : ImagesGenerator {
    override fun generateJuliaSetImage(
        imageProperties: ImageProperties,
        juliaSetProperties: JuliaSetProperties
    ): ByteArray {
        val totalLength = imageProperties.height * imageProperties.width * Pixel.SIZE_IN_BYTES
        val generatedImage = ByteArray(totalLength)
        var oneDimensionIndex = 0
        for (y in 0..<imageProperties.height) {
            for (x in 0..<imageProperties.width) {
                val calculatedPixel = calculatePixelColor(imageProperties, juliaSetProperties, x, y)
                generatedImage[oneDimensionIndex++] = calculatedPixel.r.toByte()
                generatedImage[oneDimensionIndex++] = calculatedPixel.g.toByte()
                generatedImage[oneDimensionIndex++] = calculatedPixel.b.toByte()
            }
        }
        return generatedImage
    }

    private fun calculatePixelColor(
        imageProperties: ImageProperties,
        juliaSetProperties: JuliaSetProperties,
        x: Int,
        y: Int
    ): Pixel {
        val escapeRadius = juliaSetProperties.escapeRadius
        var xValue: Double = ((escapeRadius * 2) / imageProperties.width) * x - escapeRadius
        var yValue: Double = ((escapeRadius * 2) / imageProperties.height) * y - escapeRadius
        for (i in 0..<juliaSetProperties.maxIterations) {
            val xTemp = xValue * xValue - yValue * yValue
            yValue = 2 * xValue * yValue + juliaSetProperties.offset.imaginary
            xValue = xTemp + juliaSetProperties.offset.real
            if (xValue * xValue + yValue * yValue >= escapeRadius) {
                val t: Double = i / juliaSetProperties.maxIterations.toDouble()
                val red = (9 * (1 - t) * t.pow(3.0) * 255.0).toInt()
                val green = (15 * (1 - t).pow(2.0) * t.pow(2.0) * 255.0).toInt()
                val blue = (8.5 * (1 - t).pow(3.0) * t * 255.0).toInt()
                return Pixel(red, green, blue)
            }
        }
        return Pixel.BLACK
    }
}
