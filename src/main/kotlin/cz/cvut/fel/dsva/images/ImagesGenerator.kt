package cz.cvut.fel.dsva.images

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.pixels.PixelTools
import cz.cvut.fel.dsva.grpc.CalculationResult
import cz.cvut.fel.dsva.grpc.JuliaSetProperties
import cz.cvut.fel.dsva.grpc.Pixel
import cz.cvut.fel.dsva.input.GifProperties
import cz.cvut.fel.dsva.input.ImageProperties
import cz.cvut.fel.dsva.toRenderPixels
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.log
import kotlin.math.pow
import cz.cvut.fel.dsva.grpc.ImageProperties as GrpcImageProperties

interface ImagesGenerator {
    fun generateJuliaSetImage(imageProperties: GrpcImageProperties, juliaSetProperties: JuliaSetProperties): Array<Pixel>
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
    ): Array<Pixel> {
        val totalLength = imageProperties.height * imageProperties.width
        val generatedImage = Array(totalLength) { index ->
            val x = index % imageProperties.width
            val y = index / imageProperties.width
            calculatePixelColor(imageProperties, juliaSetProperties, x, y)
        }
        return generatedImage
    }

    private fun calculatePixelColor(
        imageProperties: GrpcImageProperties,
        juliaSetProperties: JuliaSetProperties,
        x: Int,
        y: Int
    ): Pixel {
        val escapeRadius = juliaSetProperties.escapeRadius
        val startingXOffset =
            ((juliaSetProperties.topRightCorner.real - juliaSetProperties.bottomLeftCorner.real) / imageProperties.width) * x
        val startingYOffset =
            ((juliaSetProperties.topRightCorner.imaginary - juliaSetProperties.bottomLeftCorner.imaginary) / imageProperties.height) * y
        var xValue: Double = juliaSetProperties.bottomLeftCorner.real + startingXOffset
        var yValue: Double = juliaSetProperties.bottomLeftCorner.imaginary + startingYOffset
        val pictureY = imageProperties.height - y - 1
        for (i in 0..<juliaSetProperties.maxIterations) {
            val xTemp = xValue * xValue - yValue * yValue
            yValue = 2 * xValue * yValue + juliaSetProperties.offset.imaginary
            xValue = xTemp + juliaSetProperties.offset.real
            if (xValue * xValue + yValue * yValue >= escapeRadius * escapeRadius) {
                val absZ = xValue * xValue + yValue * yValue
                val lastIteration =
                    i + 1 - log(log(absZ, Math.E), Math.E) / log(juliaSetProperties.maxIterations.toDouble(), Math.E)
                val t: Double = lastIteration / juliaSetProperties.maxIterations.toDouble()
                val red = (9 * (1 - t) * t.pow(3.0) * 255.0).toInt()
                val green = (15 * (1 - t).pow(2.0) * t.pow(2.0) * 255.0).toInt()
                val blue = (8.5 * (1 - t).pow(3.0) * t * 255.0).toInt()
                val argb = PixelTools.rgb(red, green, blue)
                return Pixel.newBuilder().setX(x).setY(pictureY).setArgb(argb).build()
            }
        }
        return Pixel.newBuilder().setX(x).setY(pictureY).setArgb(0).build()
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
                val pixels = frame.pixelsList.toRenderPixels().toTypedArray()
                it.writeFrame(ImmutableImage.create(imageProperties.width, imageProperties.height, pixels))
            }
        }
    }
}
