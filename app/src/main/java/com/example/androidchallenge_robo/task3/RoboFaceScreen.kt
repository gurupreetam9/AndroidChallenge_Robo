package com.example.androidchallenge_robo.task3

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import kotlin.math.PI


/** --- Types & Constants --- */
private typealias EmotionType = String

private data class EmotionConfig(
    val primaryColor: Int,
    val secondaryColor: Int,
    val coreColor: Int,
    val glowAmount: Float,
    val pulseSpeed: Float,
    val mouthSpeed: Float,
    val mouthBaseHeight: Float,
    val eyeScaleY: Float,
    val eyeRotationSpeed: Float,
    val shakeIntensity: Float
)

private fun hexColor(hex: String) = Color.parseColor(hex)

private val EMOTIONS: Map<EmotionType, EmotionConfig> = mapOf(
    "Happy" to EmotionConfig(
        primaryColor = hexColor("#00ffff"),
        secondaryColor = hexColor("#0088ff"),
        coreColor = hexColor("#ffffff"),
        glowAmount = 20f,
        pulseSpeed = 0.005f,
        mouthSpeed = 0.1f,
        mouthBaseHeight = 20f,
        eyeScaleY = 1f,
        eyeRotationSpeed = 0.002f,
        shakeIntensity = 0f
    ),
    "Angry" to EmotionConfig(
        primaryColor = hexColor("#ff0000"),
        secondaryColor = hexColor("#880000"),
        coreColor = hexColor("#ffaaaa"),
        glowAmount = 30f,
        pulseSpeed = 0.02f,
        mouthSpeed = 0.3f,
        mouthBaseHeight = 25f,
        eyeScaleY = 0.8f,
        eyeRotationSpeed = 0.05f,
        shakeIntensity = 5f
    ),
    "Sad" to EmotionConfig(
        primaryColor = hexColor("#4466aa"),
        secondaryColor = hexColor("#223355"),
        coreColor = hexColor("#8899bb"),
        glowAmount = 5f,
        pulseSpeed = 0.002f,
        mouthSpeed = 0.02f,
        mouthBaseHeight = 5f,
        eyeScaleY = 1f,
        eyeRotationSpeed = 0.001f,
        shakeIntensity = 0f
    ),
    "Sleep" to EmotionConfig(
        primaryColor = hexColor("#112233"),
        secondaryColor = hexColor("#051015"),
        coreColor = hexColor("#223344"),
        glowAmount = 2f,
        pulseSpeed = 0.001f,
        mouthSpeed = 0f,
        mouthBaseHeight = 0f,
        eyeScaleY = 0.05f,
        eyeRotationSpeed = 0f,
        shakeIntensity = 0f
    ),
    "Curious" to EmotionConfig(
        primaryColor = hexColor("#d66aff"),
        secondaryColor = hexColor("#8800ff"),
        coreColor = hexColor("#ffffff"),
        glowAmount = 15f,
        pulseSpeed = 0.005f,
        mouthSpeed = 0.05f,
        mouthBaseHeight = 15f,
        eyeScaleY = 1f,
        eyeRotationSpeed = 0.02f,
        shakeIntensity = 0f
    )
)

/** --- Composable Screen --- */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Preview
@Composable
fun RoboFaceScreen() {
    val context = LocalContext.current
    val sensorController = remember { SensorController(context) }
    val sensorState by sensorController.state.collectAsState()

    DisposableEffect(Unit) {
        sensorController.startListening()
        onDispose { sensorController.stopListening() }
    }

    var currentEmotion by remember { mutableStateOf("Happy") }
    // Store previous emotion to restore after sleep
    var preSleepEmotion by remember { mutableStateOf("Happy") }

    // Sensor Logic Reactivity
    LaunchedEffect(sensorState.isProximityNear) {
        if (sensorState.isProximityNear) {
            if (currentEmotion != "Sleep") {
                preSleepEmotion = currentEmotion
                currentEmotion = "Sleep"
            }
        } else {
            if (currentEmotion == "Sleep") {
                currentEmotion = preSleepEmotion
            }
        }
    }

    LaunchedEffect(sensorState.isShaking) {
        if (sensorState.isShaking && currentEmotion != "Sleep") {
            currentEmotion = "Angry"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0B0B0D)) // similar to bg-neutral-900
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ROBO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF00FFFF)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Sensor Fusion ΓÇô Motion, Tilt & Proximity Interaction (Task 3)", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color(0xFF9CA3AF))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Canvas container: responsive square using BoxWithConstraints
        BoxWithConstraints(
            modifier = Modifier
                .padding(8.dp)
        ) {
            val maxW = maxWidth
            val maxH = maxHeight
            // emulate: min(window.innerWidth - 32, window.innerHeight - 200)
            val sizeDp = with(LocalDensity.current) {
                // subtract margins like React example
                val widthCandidate = maxW - 32.dp
                val heightCandidate = maxH - 200.dp
                val chosen = if (widthCandidate < heightCandidate) widthCandidate else heightCandidate
                // clamp to something sensible
                chosen.coerceAtLeast(200.dp).coerceAtMost(800.dp)
            }

            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .border(width = 1.dp, color = androidx.compose.ui.graphics.Color(0xFF111111), shape = RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Color.Black, shape = RoundedCornerShape(20.dp))
                    .padding(0.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                RoboFaceCanvas(
                    sizeDp = sizeDp,
                    currentEmotion = currentEmotion,
                    sensorState = sensorState
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Controls: buttons for each emotion
        FlowRowControls(currentEmotion = currentEmotion, setCurrentEmotion = { currentEmotion = it })

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** --- Buttons row --- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowControls(
    currentEmotion: EmotionType,
    setCurrentEmotion: (EmotionType) -> Unit
) {
    val emotions = EMOTIONS.keys.toList()

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3 // optional, good for phones
    ) {
        emotions.forEach { emotion ->
            val isActive = emotion == currentEmotion

            Button(
                onClick = { setCurrentEmotion(emotion) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive)
                        androidx.compose.ui.graphics.Color(0xFF003B44)
                    else
                        androidx.compose.ui.graphics.Color(0xFF2A3441),
                    contentColor = if (isActive)
                        androidx.compose.ui.graphics.Color(0xFF7FEFFF)
                    else
                        androidx.compose.ui.graphics.Color(0xFFE5E7EB)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = emotion.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** --- Canvas and animation --- */
@Composable
private fun RoboFaceCanvas(
    sizeDp: androidx.compose.ui.unit.Dp,
    currentEmotion: EmotionType,
    sensorState: RoboSensorState
) {

    val transitionProgress = remember { Animatable(1f) }
    var previousEmotion by remember { mutableStateOf(currentEmotion) }

    // Frame counter drives animations. Updating it triggers a recomposition and redraw of Canvas.
    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        var frame = 0L
        while (true) {
            withFrameNanos {
                frame++
                frameState.value = frame
            }
        }
    }
    // Start animation loop
    LaunchedEffect(currentEmotion) {
        val fromEmotion = previousEmotion
        transitionProgress.snapTo(0f)

        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 450,
                easing = FastOutSlowInEasing
            )
        )

        previousEmotion = currentEmotion
    }



    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .size(sizeDp)
    ) {

        val t = transitionProgress.value

        val from = EMOTIONS[previousEmotion]!!
        val to = EMOTIONS[currentEmotion]!!

        val config = EmotionConfig(
            primaryColor = lerpColor(from.primaryColor, to.primaryColor, t),
            secondaryColor = lerpColor(from.secondaryColor, to.secondaryColor, t),
            coreColor = lerpColor(from.coreColor, to.coreColor, t),
            glowAmount = lerp(from.glowAmount, to.glowAmount, t),
            pulseSpeed = lerp(from.pulseSpeed, to.pulseSpeed, t),
            mouthSpeed = lerp(from.mouthSpeed, to.mouthSpeed, t),
            mouthBaseHeight = lerp(from.mouthBaseHeight, to.mouthBaseHeight, t),
            eyeScaleY = lerp(from.eyeScaleY, to.eyeScaleY, t),
            eyeRotationSpeed = lerp(from.eyeRotationSpeed, to.eyeRotationSpeed, t),
            shakeIntensity = lerp(from.shakeIntensity, to.shakeIntensity, t)
        )


        val canvasWidth = size.width
        val canvasHeight = size.height
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        val tiltDegrees = -sensorState.roll * (180f / PI.toFloat())

        val maxOffset = canvasWidth * 0.3f

        val rollFraction = (sensorState.roll / (PI.toFloat() / 2)).coerceIn(-1f, 1f)
        val pitchFraction = (sensorState.pitch / (PI.toFloat() / 2)).coerceIn(-1f, 1f)
        
        val lookX = rollFraction * maxOffset
        val lookY = calcPitchLookMode(pitchFraction) * maxOffset

        val frame = frameState.value.toFloat()

        // Global shake for Angry
        var shakeX = 0f
        var shakeY = 0f
        if (config.shakeIntensity * t > 0f) {
            shakeX = (Math.random().toFloat() - 0.5f) * config.shakeIntensity
            shakeY = (Math.random().toFloat() - 0.5f) * config.shakeIntensity
        }

        // Clear & Background
        val nativeCanvas = drawContext.canvas.nativeCanvas

        nativeCanvas.save()

// --- Gradient background ---
        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                canvasWidth / 2f,
                canvasHeight * 0.45f,
                canvasWidth * 0.85f,
                intArrayOf(
                    Color.parseColor("#0E2A35"),
                    Color.parseColor("#05080C"),
                    Color.parseColor("#020305")
                ),
                floatArrayOf(0f, 0.6f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }

        nativeCanvas.drawRect(
            0f,
            0f,
            canvasWidth,
            canvasHeight,
            gradientPaint
        )

// apply shake
        nativeCanvas.translate(shakeX, shakeY)

// Pulse
        val pulse = sin(frame * config.pulseSpeed) * 0.05f + 1f
        val eyeSpacing = canvasWidth * 0.25f
        val eyeY = cy - canvasHeight * 0.1f
        val baseEyeRadius = canvasWidth * 0.15f
// Eyes
        
        nativeCanvas.save()
        nativeCanvas.rotate(tiltDegrees, cx, cy)

        drawEye(
            nativeCanvas = nativeCanvas,
            x = cx - eyeSpacing + lookX,
            y = eyeY + lookY,
            radius = baseEyeRadius * pulse,
            config = config,
            time = frame,
            isRight = false,
            currentEmotion = currentEmotion
        )

        drawEye(
            nativeCanvas = nativeCanvas,
            x = cx + eyeSpacing + lookX,
            y = eyeY + lookY,
            radius = baseEyeRadius * pulse,
            config = config,
            time = frame,
            isRight = true,
            currentEmotion = currentEmotion
        )

// Nose & mouth
        drawNose(nativeCanvas, cx + lookX * 0.5f, cy + canvasHeight * 0.1f + lookY * 0.5f, config)
        drawMouth(nativeCanvas, cx + lookX * 0.5f, cy + canvasHeight * 0.25f + lookY * 0.5f, config, frame, currentEmotion)

        nativeCanvas.restore() // restore rotation

        nativeCanvas.restore()


    }
}

private fun lerp(a: Float, b: Float, t: Float): Float =
    a + (b - a) * t

private fun lerpColor(a: Int, b: Int, t: Float): Int {
    return Color.argb(
        lerp(Color.alpha(a).toFloat(), Color.alpha(b).toFloat(), t).toInt(),
        lerp(Color.red(a).toFloat(), Color.red(b).toFloat(), t).toInt(),
        lerp(Color.green(a).toFloat(), Color.green(b).toFloat(), t).toInt(),
        lerp(Color.blue(a).toFloat(), Color.blue(b).toFloat(), t).toInt()
    )
}


/** --- Helper drawing functions (Android Canvas) --- */
private fun drawEye(
    nativeCanvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    radius: Float,
    config: EmotionConfig,
    time: Float,
    isRight: Boolean,
    currentEmotion: EmotionType
) {
    nativeCanvas.save()
    nativeCanvas.translate(x, y)

    // Angry Slant
    if (currentEmotion == "Angry") {
        val slant = if (isRight) -25f else 25f
        nativeCanvas.rotate(slant)
    }

    var currentScaleY = config.eyeScaleY
    var currentRadius = radius

    if (currentEmotion == "Curious" && isRight) {
        currentRadius *= 1.1f
    }

    // scale Y by using a matrix: approximate by scaling the canvas vertically
    nativeCanvas.scale(1f, currentScaleY)

    // Outer glow (implemented by shadow layer on paints)
    // 1) Outer Identity Ring
    val outerPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.primaryColor
        strokeWidth = currentRadius * 0.1f
        isAntiAlias = true
        setShadowLayer(config.glowAmount, 0f, 0f, config.primaryColor)
    }
    nativeCanvas.drawCircle(0f, 0f, currentRadius, outerPaint)

    // 2) Inner Processing Ring (secondary)
    val innerPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.secondaryColor
        strokeWidth = currentRadius * 0.08f
        isAntiAlias = true
        // slightly less shadow
        setShadowLayer(config.glowAmount * 0.5f, 0f, 0f, config.secondaryColor)
    }
    nativeCanvas.drawCircle(0f, 0f, currentRadius * 0.75f, innerPaint)

    // 3) Circuit Details (Rotating)
    nativeCanvas.save()
    val rotationDir = if (isRight) 1f else -1f
    val rotationRad = time * config.eyeRotationSpeed * rotationDir
    val rotationDeg = rotationRad * (180f / Math.PI.toFloat())
    nativeCanvas.rotate(rotationDeg)
    // no heavy glow for details
    val detailPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.primaryColor
        strokeWidth = 2f
        isAntiAlias = true
        clearShadowLayer()
    }
    // Draw 4 dashed-ish arcs and nodes
    val arcRadius = currentRadius * 0.88f
    val startAngleRadSegments = arrayOf(0f, (Math.PI / 2).toFloat(), Math.PI.toFloat(), (3 * Math.PI / 2).toFloat())
    for (i in 0 until 4) {
        val startRad = startAngleRadSegments[i]
        val startDeg = startRad * (180f / Math.PI.toFloat())
        val sweepDeg = 0.5f * (180f / Math.PI.toFloat()) // 0.5 rad in degrees
        val oval = RectF(-arcRadius, -arcRadius, arcRadius, arcRadius)
        nativeCanvas.drawArc(oval, startDeg, sweepDeg, false, detailPaint)

        val nodeX = cos(startRad.toDouble()).toFloat() * arcRadius
        val nodeY = sin(startRad.toDouble()).toFloat() * arcRadius
        val nodePaint = Paint().apply {
            style = Paint.Style.FILL
            color = config.coreColor
            isAntiAlias = true
        }
        nativeCanvas.drawCircle(nodeX, nodeY, 3f, nodePaint)
    }

    // Hexagonal hints inside (if eyeScaleY > 0.5)
    if (config.eyeScaleY > 0.5f) {
        val hexPath = Path()
        for (i in 0 until 6) {
            val angle = (i * Math.PI / 3).toFloat()
            val hx = cos(angle.toDouble()).toFloat() * (currentRadius * 0.5f)
            val hy = sin(angle.toDouble()).toFloat() * (currentRadius * 0.5f)
            if (i == 0) hexPath.moveTo(hx, hy) else hexPath.lineTo(hx, hy)
        }
        hexPath.close()
        val hexPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = adjustAlpha(config.secondaryColor, 0.53f) // ~88 hex
            strokeWidth = 1f
            isAntiAlias = true
        }
        nativeCanvas.drawPath(hexPath, hexPaint)
    }

    nativeCanvas.restore()

    // 4) Core (White Energy)
    val corePaint = Paint().apply {
        style = Paint.Style.FILL
        color = config.coreColor
        isAntiAlias = true
        setShadowLayer(config.glowAmount * 1.5f, 0f, 0f, config.coreColor)
    }
    nativeCanvas.drawCircle(0f, 0f, currentRadius * 0.35f, corePaint)

    nativeCanvas.restore()
}

private fun drawNose(
    nativeCanvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    config: EmotionConfig
) {
    nativeCanvas.save()
    nativeCanvas.translate(x, y)
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.coreColor
        strokeWidth = 3f
        isAntiAlias = true
        setShadowLayer(config.glowAmount / 2f, 0f, 0f, config.primaryColor)
    }
    val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = config.primaryColor
        isAntiAlias = true
    }

    val size = 20f
    val p = Path()
    p.moveTo(-size, size)   // bottom left
    p.lineTo(0f, -size)     // top peak
    p.lineTo(size, size)    // bottom right
    nativeCanvas.drawPath(p, strokePaint)

    // Center connecting line
    val connector = Path()
    connector.moveTo(0f, -size + 5f)
    connector.lineTo(0f, size)
    val connectorPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = config.primaryColor
        strokeWidth = 2f
        isAntiAlias = true
    }
    nativeCanvas.drawPath(connector, connectorPaint)

    // Sensor dot
    nativeCanvas.drawCircle(0f, size + 5f, 4f, fillPaint)

    nativeCanvas.restore()
}

private fun drawMouth(
    nativeCanvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    config: EmotionConfig,
    time: Float,
    currentEmotion: EmotionType
) {
    nativeCanvas.save()
    nativeCanvas.translate(x, y)

    val bars = 5
    val spacing = 12f
    val width = 10f
    val totalWidth = (bars * width) + ((bars - 1) * spacing)
    val startX = -totalWidth / 2f

    val paint = Paint().apply {
        style = Paint.Style.FILL
        color = config.primaryColor
        isAntiAlias = true
        setShadowLayer(config.glowAmount, 0f, 0f, config.primaryColor)
    }

    for (i in 0 until bars) {
        var barHeight = config.mouthBaseHeight
        if (currentEmotion != "Sleep") {
            val wave = sin((time * config.mouthSpeed) + (i * 0.5f))
            barHeight += wave * 10f
            if (currentEmotion == "Angry") {
                barHeight += (Math.random().toFloat() * 15f)
            }
        }
        barHeight = max(2f, barHeight)
        val bx = startX + (i * (width + spacing))
        val by = -barHeight / 2f
        nativeCanvas.drawRect(bx, by, bx + width, by + barHeight, paint)
    }

    nativeCanvas.restore()
}

/** --- Utilities --- */
private fun adjustAlpha(colorInt: Int, alphaFactor: Float): Int {
    val alpha = (Color.alpha(colorInt) * alphaFactor).roundToInt().coerceIn(0, 255)
    return Color.argb(alpha, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
}

private fun calcPitchLookMode(pitchParam: Float): Float {
    // Pitch: Rotation around X axis.
    // Ensure inverted appropriately for screen coordinates (Down is +Y)
    return -pitchParam
}
