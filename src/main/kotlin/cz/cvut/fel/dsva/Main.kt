package cz.cvut.fel.dsva

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import cz.cvut.fel.dsva.grpc.ComplexNumber
import cz.cvut.fel.dsva.grpc.ImageProperties
import cz.cvut.fel.dsva.grpc.JuliaSetProperties
import cz.cvut.fel.dsva.images.ImagesGeneratorImpl
import java.io.File

fun main() {
    val imageProperties = ImageProperties.newBuilder().setWidth(3840).setHeight(2160).build()
    val offset = ComplexNumber.newBuilder().setReal(-0.835).setImaginary(-0.32).build()
    val upperCorner = ComplexNumber.newBuilder().setReal(1.6).setImaginary(1.1).build()
    val lowerCorner = ComplexNumber.newBuilder().setReal(-1.6).setImaginary(-1.1).build()
    val juliaSetProperties =
        JuliaSetProperties.newBuilder().setOffset(offset).setUpperCorner(upperCorner).setLowerCorner(lowerCorner)
            .setEscapeRadius(2.0).setMaxIterations(60).build()
    val generatedImage = ImagesGeneratorImpl.generateJuliaSetImage(imageProperties, juliaSetProperties)
    val image = ImmutableImage.create(imageProperties.width, imageProperties.height, generatedImage.map { it.toRenderPixel() }.toTypedArray())
    image.output(PngWriter.NoCompression, File("/home/stengjir/School/DSVA/SemestralWork/out.png"))
}