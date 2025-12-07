package com.example.speechsync

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Apple Siri-inspired audio visualization
 * Features smooth, organic wave curves with multiple harmonics
 */
class SiriWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waves = mutableListOf<Wave>()
    private var phase = 0f
    private var amplitude = 0f
    private var targetAmplitude = 0f
    private var isAnimating = false
    private var animator: ValueAnimator? = null

    // Paint configurations
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Gradient colors for waves
    private val primaryColor = ContextCompat.getColor(context, R.color.primary)
    private val accentColor = ContextCompat.getColor(context, R.color.accent_light)

    // Wave configuration
    private data class Wave(
        val frequency: Float,
        val amplitude: Float,
        val phase: Float,
        val alpha: Int,
        val strokeWidth: Float
    )

    init {
        // Create multiple wave layers for depth
        waves.add(Wave(1.5f, 1.0f, 0f, 180, 3f))
        waves.add(Wave(2.0f, 0.8f, PI.toFloat() / 4, 140, 2.5f))
        waves.add(Wave(2.5f, 0.6f, PI.toFloat() / 2, 100, 2f))
        waves.add(Wave(3.0f, 0.4f, PI.toFloat() * 3 / 4, 60, 1.5f))
    }

    fun startAnimation() {
        if (isAnimating) return
        isAnimating = true

        // Animate target amplitude
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            addUpdateListener { animation ->
                targetAmplitude = animation.animatedValue as Float
            }
            start()
        }

        // Phase animation for smooth wave motion
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                phase = animation.animatedValue as Float

                // Smooth amplitude transitions
                amplitude += (targetAmplitude - amplitude) * 0.15f

                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        isAnimating = false

        // Smooth fade out
        ValueAnimator.ofFloat(targetAmplitude, 0f).apply {
            duration = 600
            addUpdateListener { animation ->
                targetAmplitude = animation.animatedValue as Float
                if (targetAmplitude < 0.01f) {
                    animator?.cancel()
                    animator = null
                }
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (amplitude < 0.01f) return

        val centerY = height / 2f
        val maxAmplitude = height / 3f

        waves.forEach { wave ->
            drawWave(canvas, centerY, maxAmplitude, wave)
        }
    }

    private fun drawWave(canvas: Canvas, centerY: Float, maxAmplitude: Float, wave: Wave) {
        val path = Path()
        val width = width.toFloat()
        val points = 100

        wavePaint.strokeWidth = wave.strokeWidth
        wavePaint.alpha = (wave.alpha * amplitude).toInt()

        // Create gradient shader
        val gradient = LinearGradient(
            0f, centerY - maxAmplitude,
            0f, centerY + maxAmplitude,
            intArrayOf(primaryColor, accentColor, primaryColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        wavePaint.shader = gradient

        var isFirst = true

        for (i in 0..points) {
            val x = (i.toFloat() / points) * width
            val normalizedX = (x / width) * 2 - 1 // Range: -1 to 1

            // Create organic wave using multiple sine components
            val baseWave = sin(normalizedX * PI * wave.frequency + Math.toRadians(phase.toDouble() + wave.phase))

            // Add harmonics for more natural movement
            val harmonic1 = sin(normalizedX * PI * wave.frequency * 2 + Math.toRadians(phase.toDouble() * 1.5))
            val harmonic2 = sin(normalizedX * PI * wave.frequency * 0.5 + Math.toRadians(phase.toDouble() * 0.7))

            // Combine waves with envelope
            val envelope = (1 - normalizedX.pow(2)).coerceIn(0f, 1f)
            val combined = (baseWave + harmonic1 * 0.3 + harmonic2 * 0.2) / 1.5

            val y = centerY + (combined * maxAmplitude * wave.amplitude * amplitude * envelope).toFloat()

            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, wavePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}

/**
 * Modern ripple/pulse visualization with glassmorphism effect
 * Perfect for showing active listening state
 */
class ModernRippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ripples = mutableListOf<Ripple>()
    private var isAnimating = false
    private var animator: ValueAnimator? = null
    private var globalPhase = 0f

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val primaryColor = ContextCompat.getColor(context, R.color.primary)
    private val accentColor = ContextCompat.getColor(context, R.color.accent_light)

    private data class Ripple(
        var radius: Float,
        var alpha: Float,
        var strokeWidth: Float,
        val speed: Float,
        val color: Int
    )

    fun startAnimation() {
        if (isAnimating) return
        isAnimating = true

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                globalPhase = animation.animatedValue as Float
                updateRipples()
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        isAnimating = false
        animator?.cancel()
        animator = null

        // Fade out existing ripples
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 400
            addUpdateListener { animation ->
                ripples.forEach { it.alpha *= (animation.animatedValue as Float) }
                invalidate()

                if ((animation.animatedValue as Float) < 0.1f) {
                    ripples.clear()
                }
            }
            start()
        }

    }

    private fun updateRipples() {
        val maxRadius = (Math.min(width, height) / 2f) * 1.2f
        val centerX = width / 2f
        val centerY = height / 2f

        // Add new ripple periodically
        if (ripples.isEmpty() || ripples.last().radius > maxRadius * 0.15f) {
            val color = if (ripples.size % 2 == 0) primaryColor else accentColor
            ripples.add(Ripple(0f, 1f, 8f, 1.0f, color))
        }

        // Update existing ripples
        ripples.forEach { ripple ->
            ripple.radius += maxRadius * 0.012f * ripple.speed
            ripple.alpha = (1f - ripple.radius / maxRadius).coerceIn(0f, 1f)
            ripple.strokeWidth = 8f * (1f - ripple.radius / maxRadius).coerceIn(0.3f, 1f)
        }

        // Remove invisible ripples
        ripples.removeAll { it.radius > maxRadius }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isAnimating && ripples.isEmpty()) return

        val centerX = width / 2f
        val centerY = height / 2f

        // Draw glow effect in center
        if (isAnimating) {
            val glowRadius = 40f + sin(globalPhase * 2 * PI).toFloat() * 10f
            val glowGradient = RadialGradient(
                centerX, centerY, glowRadius * 2,
                intArrayOf(
                    Color.argb(80, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                    Color.argb(0, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
                ),
                null,
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = glowGradient
            canvas.drawCircle(centerX, centerY, glowRadius * 2, glowPaint)
        }

        // Draw ripples
        ripples.forEach { ripple ->
            ripplePaint.strokeWidth = ripple.strokeWidth
            ripplePaint.color = ripple.color
            ripplePaint.alpha = (ripple.alpha * 255).toInt()

            // Add blur effect for depth
            if (ripple.alpha > 0.3f) {
                ripplePaint.maskFilter = BlurMaskFilter(ripple.strokeWidth, BlurMaskFilter.Blur.NORMAL)
            }

            canvas.drawCircle(centerX, centerY, ripple.radius, ripplePaint)
            ripplePaint.maskFilter = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}

/**
 * Particle system visualization - premium feel
 * Floating particles that respond to voice
 */
class ParticleWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private var isAnimating = false
    private var animator: ValueAnimator? = null
    private val random = kotlin.random.Random

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val primaryColor = ContextCompat.getColor(context, R.color.primary)
    private val accentColor = ContextCompat.getColor(context, R.color.accent_light)

    private data class Particle(
        var x: Float,
        var y: Float,
        var baseY: Float,
        val radius: Float,
        var alpha: Float,
        val speed: Float,
        val amplitude: Float,
        val frequency: Float,
        val phase: Float,
        val color: Int
    )

    fun startAnimation() {
        if (isAnimating) return
        isAnimating = true

        // Create particles
        val numParticles = 50
        particles.clear()

        for (i in 0 until numParticles) {
            val x = (width.toFloat() / numParticles) * i
            val baseY = height / 2f
            val color = if (i % 3 == 0) primaryColor else accentColor

            particles.add(
                Particle(
                    x = x,
                    y = baseY,
                    baseY = baseY,
                    radius = random.nextFloat() * 4f + 2f,
                    alpha = random.nextFloat() * 0.5f + 0.5f,
                    speed = random.nextFloat() * 0.5f + 0.5f,
                    amplitude = random.nextFloat() * 40f + 20f,
                    frequency = random.nextFloat() * 2f + 1f,
                    phase = random.nextFloat() * 2f * PI.toFloat(),
                    color = color
                )
            )
        }

        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val phase = animation.animatedValue as Float
                updateParticles(phase)
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        isAnimating = false

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500
            addUpdateListener { animation ->
                val alpha = animation.animatedValue as Float
                particles.forEach { it.alpha *= alpha }
                invalidate()
                if (alpha < 0.1f) {
                    animator?.cancel()
                    animator = null
                    particles.clear()
                }
            }
            start()
        }
    }

    private fun updateParticles(globalPhase: Float) {
        particles.forEach { particle ->
            val wave = sin((globalPhase + particle.phase) * PI / 180 * particle.frequency)
            particle.y = particle.baseY + (wave * particle.amplitude * particle.speed).toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        particles.forEach { particle ->
            particlePaint.color = particle.color
            particlePaint.alpha = (particle.alpha * 255).toInt()

            // Draw particle with glow
            val glowRadius = particle.radius * 2
            val gradient = RadialGradient(
                particle.x, particle.y, glowRadius,
                intArrayOf(particle.color, Color.TRANSPARENT),
                floatArrayOf(0.3f, 1f),
                Shader.TileMode.CLAMP
            )
            particlePaint.shader = gradient
            canvas.drawCircle(particle.x, particle.y, glowRadius, particlePaint)

            // Draw solid center
            particlePaint.shader = null
            canvas.drawCircle(particle.x, particle.y, particle.radius, particlePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}