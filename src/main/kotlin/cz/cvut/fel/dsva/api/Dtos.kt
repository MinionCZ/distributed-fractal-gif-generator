package cz.cvut.fel.dsva.api

import cz.cvut.fel.dsva.datastructure.RemoteWorkStation
import cz.cvut.fel.dsva.grpc.complexNumber
import cz.cvut.fel.dsva.grpc.imageProperties
import java.time.Duration
import kotlin.math.pow


data class ComplexNumber(val real: Double, val imaginary: Double) {
    val size: Double
        get() = (real.pow(2) + imaginary.pow(2)).pow(0.5)


    operator fun minus(other: ComplexNumber): ComplexNumber {
        val real = this.real - other.real
        val imaginary = this.imaginary - other.imaginary
        return ComplexNumber(real, imaginary)
    }

    fun divByScalar(scalar: Double): ComplexNumber {
        return ComplexNumber(real / scalar, imaginary / scalar)
    }

    operator fun plus(other: ComplexNumber): ComplexNumber {
        return ComplexNumber(real + other.real, imaginary + other.imaginary)
    }

    fun toProtobufFormat(): cz.cvut.fel.dsva.grpc.ComplexNumber = complexNumber {
        imaginary = this@ComplexNumber.imaginary
        real = this@ComplexNumber.real
    }
}


data class ImageProperties(
    val width: Int,
    val height: Int,
) : Validatable {
    override fun validate() {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Width and height must be at least 0, but was $width and height $height")
        }
        if (width > MAX_WIDTH) {
            throw IllegalArgumentException("Width ($width) is higher than max possible value ($MAX_WIDTH)")
        }
        if (height > MAX_HEIGHT) {
            throw IllegalArgumentException("Height ($height) is higher than max possible value ($MAX_HEIGHT)")
        }
    }

    fun toProtobufFormat(frameId: Int): cz.cvut.fel.dsva.grpc.ImageProperties = imageProperties {
        id = frameId
        width = this@ImageProperties.width
        height = this@ImageProperties.height
    }

    private companion object {
        private const val MAX_WIDTH = 7680;
        private const val MAX_HEIGHT = 4320;
    }
}

data class GifProperties(
    val numberOfFrames: Int,
    val duration: Duration,
    val filename: String,
) : Validatable {
    override fun validate() {
        if (numberOfFrames !in NUMBER_OF_FRAME_RANGE) {
            throw IllegalArgumentException("Number of frames must be in range $NUMBER_OF_FRAME_RANGE, but was $numberOfFrames")
        }

        if (duration > MAX_DURATION) {
            throw IllegalArgumentException("Duration should be less than $MAX_DURATION, but was $duration")
        }

        if (filename.isBlank()) {
            throw IllegalArgumentException("Filename must not be blank")
        }
        if (!filename.endsWith(VALID_FILE_EXTENSION)) {
            throw IllegalArgumentException("Filename must end with a valid extension $VALID_FILE_EXTENSION")
        }
    }

    private companion object {
        private val MAX_DURATION = Duration.ofSeconds(30)
        private val NUMBER_OF_FRAME_RANGE = 1..900
        private const val VALID_FILE_EXTENSION = ".gif"
    }
}

data class JuliaSetProperties(
    val escapeRadius: Double,
    val startingOffset: ComplexNumber,
    val endingOffset: ComplexNumber,
    val bottomLeftCorner: ComplexNumber,
    val topRightCorner: ComplexNumber,
    val startingNumberOfIterations: Int,
    val endingNumberOfIterations: Int,
) : Validatable {
    override fun validate() {
        if (startingNumberOfIterations !in ITERATIONS_RANGE) {
            throw IllegalArgumentException("Starting number of iterations is not in required range $ITERATIONS_RANGE, but was $startingNumberOfIterations")
        }
        if (endingNumberOfIterations !in ITERATIONS_RANGE) {
            throw IllegalArgumentException("Ending number of iterations is not in required range $ITERATIONS_RANGE, but was $startingNumberOfIterations")
        }
        if (escapeRadius.pow(2) - escapeRadius < startingOffset.size) {
            throw IllegalArgumentException("Escape radius R must follow equation R**2 - R >= sqrt(offsetRealPart ** 2 + offsetImaginaryPart **2), but was not for startingOffset")
        }
        if (escapeRadius.pow(2) - escapeRadius < endingOffset.size) {
            throw IllegalArgumentException("Escape radius R must follow equation R**2 - R >= sqrt(offsetRealPart ** 2 + offsetImaginaryPart **2), but was not for endingOffset")
        }
        if (bottomLeftCorner.real >= topRightCorner.real) {
            throw IllegalArgumentException("Bottom left corner real part (${bottomLeftCorner.real}) is higher than top right corner real (${topRightCorner.real})")
        }
        if (bottomLeftCorner.imaginary >= topRightCorner.imaginary) {
            throw IllegalArgumentException("Bottom left corner imaginary part (${bottomLeftCorner.imaginary}) is higher than top right corner imaginary (${topRightCorner.imaginary})")
        }
    }

    private companion object {
        private val ITERATIONS_RANGE = 1..10_000
    }
}


data class GenerateImageDto(
    val imageProperties: ImageProperties,
    val gifProperties: GifProperties,
    val juliaSetProperties: JuliaSetProperties,
) : Validatable {
    override fun validate() {
        imageProperties.validate()
        gifProperties.validate()
        juliaSetProperties.validate()
    }
}

interface Validatable {
    fun validate()
}

data class RemoteWorkStationDto(val ip: String, val port: Int) : Validatable {
    override fun validate() {
        validateIpAddress(ip)
        require(port in PORT_RANGE) {
            "Port ($port) is not in valid port range $PORT_RANGE"
        }
    }

    private fun validateIpAddress(ip: String) {
        for (c in ip) {
            require(c in VALID_IP_CHARACTERS) {
                "Invalid IP address: $ip"
            }
        }
        val parts = ip.split(".")
        require(parts.size == CORRECT_NUMBER_OF_PARTS) {
            "Invalid IP address: $ip"
        }
        parts.forEach {
            it.toIntOrNull()?.let { parsedNumber ->
                require(parsedNumber <= MAX_IP_VALUE) {
                    "Invalid IP address: $ip"
                }
            } ?: throw IllegalArgumentException("Invalid IP address: $ip")
        }
    }

    fun toDao() = RemoteWorkStation(ip, port)

    private companion object {
        private val VALID_IP_CHARACTERS = buildSet {
            add('.')
            addAll('0'..'9')
        }
        private const val CORRECT_NUMBER_OF_PARTS = 4
        private const val MAX_IP_VALUE = 255
        private val PORT_RANGE = 1000..65535
    }
}

