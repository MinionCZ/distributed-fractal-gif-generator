package cz.cvut.fel.dsva.input

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UserInputHandler(val systemJobStore: SystemJobStore, private val objectMapper: ObjectMapper) {
    suspend fun startInputHandler() {
        coroutineScope {
            launch(Dispatchers.IO) {
                BufferedReader(InputStreamReader(System.`in`)).use { reader ->
                    while (true) {
                        println("Insert path to file with computation properties:")
                        val line = reader.readLine() ?: break
                        handleUserInput(line)
                    }
                }
            }
        }
    }

    private fun handleUserInput(line: String) {
        try {
            val parsedInput = parseUserInput(line)
            parsedInput.validate()
        } catch (e: IllegalStateException) {
            System.err.println(e.message)
        } catch (e: IOException) {
            System.err.println("Error occurred during reading of file $line")
        } catch (e: JacksonException) {
            System.err.println("Error occurred during parsing of file $line")
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
        }
    }

    private fun parseUserInput(pathToProperties: String): UserInputHolder {
        val file = File(pathToProperties)
        if (!file.exists()) {
            error("File does not exist: $pathToProperties")
        }
        val content = file.readText()
        return objectMapper.readValue(content, UserInputHolder::class.java)
    }

}

data class ComplexNumber(val real: Double, val imaginary: Double) {
    val size: Double
        get() = (real.pow(2) + imaginary.pow(2)).pow(0.5)
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

    private companion object {
        private const val MAX_WIDTH = 7680;
        private const val MAX_HEIGHT = 4320;
    }
}

data class GifProperties(
    val numberOfFrames: Int,
    val duration: Duration,
) : Validatable {
    override fun validate() {
        if (numberOfFrames !in NUMBER_OF_FRAME_RANGE) {
            throw IllegalArgumentException("Number of frames must be in range $NUMBER_OF_FRAME_RANGE, but was $numberOfFrames")
        }

        if (duration > MAX_DURATION) {
            throw IllegalArgumentException("Duration should be less than $MAX_DURATION, but was $duration")
        }
    }

    private companion object {
        private val MAX_DURATION = Duration.ofSeconds(30)
        private val NUMBER_OF_FRAME_RANGE = 1..900
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


data class UserInputHolder(
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
