package cz.cvut.fel.dsva.input

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.system.Job
import cz.cvut.fel.dsva.datastructure.system.SystemJobStore
import cz.cvut.fel.dsva.grpc.CalculationRequest
import cz.cvut.fel.dsva.grpc.calculationRequest
import cz.cvut.fel.dsva.grpc.complexNumber
import cz.cvut.fel.dsva.grpc.imageProperties
import cz.cvut.fel.dsva.grpc.juliaSetProperties
import cz.cvut.fel.dsva.service.UserInputService
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.util.LinkedList
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UserInputHandler(
    private val systemJobStore: SystemJobStore,
    private val currentWorkStationConfig: WorkStationConfig,
    private val objectMapper: ObjectMapper,
    private val userInputService: UserInputService,
) {
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

    private suspend fun handleUserInput(line: String) {
        try {
            val parsedInput = parseUserInput(line)
            parsedInput.validate()
            validateWorkstationState()
            enqueueNewUserJob(parsedInput)
            println("New job has started")
            userInputService.startNewDistributedJob(parsedInput)
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

    private fun validateWorkstationState() {
        if (systemJobStore.isSystemJobPresent()) {
            error("System is already calculating, please wait until calculation is done")
        }
    }

    private fun enqueueNewUserJob(userInputHolder: UserInputHolder) {
        val newJob = Job(
            workRequester = this.currentWorkStationConfig.toWorkStation(),
            createTasksFromUserInput(userInputHolder)
        )
        systemJobStore.persistNewSystemJob(newJob)
    }

    private fun createTasksFromUserInput(userInputHolder: UserInputHolder): List<CalculationRequest> {
        val offsetStep =
            (userInputHolder.juliaSetProperties.endingOffset - userInputHolder.juliaSetProperties.startingOffset).divByScalar(
                userInputHolder.gifProperties.numberOfFrames.toDouble()
            )
        val iterationStep =
            (userInputHolder.juliaSetProperties.endingNumberOfIterations - userInputHolder.juliaSetProperties.startingNumberOfIterations) / userInputHolder.gifProperties.numberOfFrames

        val tasks = LinkedList<CalculationRequest>()
        var calculationOffset = userInputHolder.juliaSetProperties.startingOffset
        var iteration = userInputHolder.juliaSetProperties.startingNumberOfIterations
        val topRightCornerInProtobufFormat = userInputHolder.juliaSetProperties.topRightCorner.toProtobufFormat()
        val bottomLeftCornerInProtobufFormat = userInputHolder.juliaSetProperties.bottomLeftCorner.toProtobufFormat()
        for (i in 0..<userInputHolder.gifProperties.numberOfFrames) {
            val task = calculationRequest {
                imageProperties = userInputHolder.imageProperties.toProtobufFormat(i)
                juliaSetProperties = juliaSetProperties {
                    offset = calculationOffset.toProtobufFormat()
                    topRightCorner = topRightCornerInProtobufFormat
                    bottomLeftCorner = bottomLeftCornerInProtobufFormat
                    escapeRadius = userInputHolder.juliaSetProperties.escapeRadius
                    maxIterations = iteration
                }
            }
            calculationOffset += offsetStep
            iteration += iterationStep
            tasks.add(task)
        }
        return tasks
    }
}

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
