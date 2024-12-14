package cz.cvut.fel.dsva.input

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cvut.fel.dsva.LoggerWrapper
import cz.cvut.fel.dsva.datastructure.WorkStationConfig
import cz.cvut.fel.dsva.datastructure.Job
import cz.cvut.fel.dsva.datastructure.SystemJobStore
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

class UserInputHandler
