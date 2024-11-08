package cz.cvut.fel.dsva.images

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.pixels.PixelTools
import com.sksamuel.scrimage.pixels.PixelTools.rgb
import cz.cvut.fel.dsva.grpc.CalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetProperties
import cz.cvut.fel.dsva.input.GifProperties
import cz.cvut.fel.dsva.input.ImageProperties
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.time.Duration
import kotlin.math.log
import kotlin.math.pow
import cz.cvut.fel.dsva.grpc.ImageProperties as GrpcImageProperties

interface ImagesGenerator {
    fun generateJuliaSetImage(
        imageProperties: GrpcImageProperties,
        juliaSetProperties: JuliaSetProperties
    ): ByteArray

    fun createGif(
        gifProperties: GifProperties,
        imageProperties: ImageProperties,
        images: List<CalculationResult>
    )
}

class ImagesGeneratorImpl : ImagesGenerator {
    override fun generateJuliaSetImage(
        imageProperties: GrpcImageProperties,
        juliaSetProperties: JuliaSetProperties
    ): ByteArray {
        val totalLength = imageProperties.height * imageProperties.width * 3
        val generatedImage = ByteArray(totalLength)
        for (i in 0 until totalLength step 3) {
            calculatePixelColor(imageProperties, juliaSetProperties, i, generatedImage)
        }
        return generatedImage
    }

    private fun calculatePixelColor(
        imageProperties: GrpcImageProperties,
        juliaSetProperties: JuliaSetProperties,
        index: Int,
        generatedImagePayload: ByteArray,
    ) {
        val x = (index / 3) % imageProperties.width
        val y = (index / 3) / imageProperties.width
        val escapeRadius = juliaSetProperties.escapeRadius
        val startingXOffset =
            ((juliaSetProperties.topRightCorner.real - juliaSetProperties.bottomLeftCorner.real) / imageProperties.width) * x
        val startingYOffset =
            ((juliaSetProperties.topRightCorner.imaginary - juliaSetProperties.bottomLeftCorner.imaginary) / imageProperties.height) * y
        var xValue: Double = juliaSetProperties.bottomLeftCorner.real + startingXOffset
        var yValue: Double = juliaSetProperties.bottomLeftCorner.imaginary + startingYOffset
        for (i in 0..<juliaSetProperties.maxIterations) {
            val xTemp = xValue * xValue - yValue * yValue
            yValue = 2 * xValue * yValue + juliaSetProperties.offset.imaginary
            xValue = xTemp + juliaSetProperties.offset.real
            if (xValue * xValue + yValue * yValue >= escapeRadius * escapeRadius) {
                val absZ = xValue * xValue + yValue * yValue
                val lastIteration =
                    i + 1 - log(log(absZ, Math.E), Math.E) / log(juliaSetProperties.maxIterations.toDouble(), Math.E)
                val t: Double = lastIteration / juliaSetProperties.maxIterations.toDouble()
                generatedImagePayload[index] = (9 * (1 - t) * t.pow(3.0) * 255.0).toInt().toByte()
                generatedImagePayload[index + 1] = (15 * (1 - t).pow(2.0) * t.pow(2.0) * 255.0).toInt().toByte()
                generatedImagePayload[index + 2] = (8.5 * (1 - t).pow(3.0) * t * 255.0).toInt().toByte()
                return
            }
        }
        generatedImagePayload[index] = 0
        generatedImagePayload[index + 1] = 0
        generatedImagePayload[index + 2] = 0
    }

    override fun createGif(
        gifProperties: GifProperties,
        imageProperties: ImageProperties,
        images: List<CalculationResult>
    ) {
        val imageDuration = gifProperties.duration.dividedBy(gifProperties.numberOfFrames.toLong())
        val writer = StreamingGifWriter(imageDuration, true, false)
        writer.prepareStream(File(gifProperties.filename), BufferedImage.TYPE_INT_ARGB).use {
            val sortedImages = images.sortedBy { image -> image.imageProperties.id }
            for (frame in sortedImages) {
                val calculatedPixels = frame.pixels.toByteArray()
                val pixels = Array(imageProperties.width * imageProperties.height) { i ->
                    val x = i % imageProperties.width
                    val y = i / imageProperties.width
                    val r = calculatedPixels[i * 3].toInt() and 0xff
                    val g = calculatedPixels[i * 3 + 1].toInt() and 0xff
                    val b = calculatedPixels[i * 3 + 2].toInt() and 0xff
                    Pixel(x, y, rgb(r, g, b))
                }

                it.writeFrame(ImmutableImage.create(imageProperties.width, imageProperties.height, pixels))
            }
        }
    }
}
