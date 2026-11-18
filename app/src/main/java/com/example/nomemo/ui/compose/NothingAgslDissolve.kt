package com.example.nomemo.ui.compose

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

private const val AGSL_NOISE_DISSOLVE = """
    uniform shader composable;
    uniform float progress;
    uniform float2 resolution;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    half4 main(float2 fragCoord) {
        half4 color = composable.eval(fragCoord);
        if (progress <= 0.001) return color;
        if (progress >= 0.999) return half4(0.0);

        float2 uv = fragCoord / max(resolution, float2(1.0, 1.0));
        float n = noise(uv * 12.0);
        float edge = n - progress;
        if (edge < 0.0) {
            return half4(0.0);
        }
        if (edge < 0.06) {
            float glow = 1.0 - (edge / 0.06);
            return half4(color.rgb + half3(glow * 0.3), color.a * (edge / 0.06));
        }
        return color;
    }
"""

/**
 * Lightweight AGSL RuntimeShader noise-dissolve modifier (300ms sweep) for card delete or burn.
 * Gracefully falls back to fadeOut + scaleOut(0.9f) on older Android APIs (< 33).
 */
fun Modifier.nothingAgslDissolve(
    isDissolving: Boolean,
    onDissolveComplete: () -> Unit = {},
): Modifier = composed {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isDissolving) {
        if (isDissolving) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            )
            onDissolveComplete()
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember {
            try {
                RuntimeShader(AGSL_NOISE_DISSOLVE)
            } catch (_: Throwable) {
                null
            }
        }

        if (shader != null) {
            this.graphicsLayer {
                val p = progress.value
                if (p > 0.001f) {
                    shader.setFloatUniform("progress", p)
                    shader.setFloatUniform("resolution", size.width, size.height)
                    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
                    alpha = if (p >= 0.99f) 0f else 1f
                }
            }
        } else {
            this.graphicsLayer {
                val p = progress.value
                alpha = (1f - p).coerceIn(0f, 1f)
                val s = 1f - (p * 0.10f)
                scaleX = s
                scaleY = s
            }
        }
    } else {
        // Fallback for API < 33: fadeOut + scaleOut(0.9f)
        this.graphicsLayer {
            val p = progress.value
            alpha = (1f - p).coerceIn(0f, 1f)
            val s = 1f - (p * 0.10f)
            scaleX = s
            scaleY = s
        }
    }
}
