package com.reallyusefulribbits.mod.client

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import java.util.Random

/**
 * Рисует молнию из руки как ванильную: аддитивные ленты одного цвета, только вдоль луча.
 * Вызывается из события отрисовки мира, пока в RenderSystem стоит поворот камеры.
 */
object BeamRenderer {
    private val LAYERS = floatArrayOf(0.05f, 0.11f, 0.2f, 0.32f)

    fun render(partialTick: Float) {
        val beams = ClientBeams.snapshot()
        if (beams.isEmpty()) return
        val minecraft = Minecraft.getInstance()
        val camera = minecraft.gameRenderer.mainCamera.position
        val buffers = minecraft.renderBuffers().bufferSource()
        val consumer = buffers.getBuffer(RenderType.lightning())
        val matrix = Matrix4f()
        for (beam in beams) {
            val fade = (1f - (beam.age + partialTick) / ClientBeams.LIFETIME).coerceIn(0f, 1f)
            drawBeam(consumer, matrix, beam, camera, fade)
        }
        buffers.endBatch(RenderType.lightning())
    }

    private fun drawBeam(consumer: VertexConsumer, matrix: Matrix4f, beam: ClientBeams.Beam, camera: Vec3, fade: Float) {
        val span = beam.to.subtract(beam.from)
        val length = span.length()
        if (length < 0.5) return
        val dir = span.scale(1.0 / length)
        val side = (if (kotlin.math.abs(dir.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).cross(dir).normalize()
        val up = dir.cross(side).normalize()
        val random = Random(beam.seed + beam.age / 2)
        val segments = (length / 1.6).toInt().coerceIn(4, 320)
        val points = ArrayList<Vec3>(segments + 1)
        for (i in 0..segments) {
            val along = beam.from.add(span.scale(i / segments.toDouble()))
            val jitter = if (i == 0 || i == segments) 0.0 else 0.55
            val offset = side.scale((random.nextDouble() - 0.5) * 2.0 * jitter).add(up.scale((random.nextDouble() - 0.5) * 2.0 * jitter))
            points += along.add(offset).subtract(camera)
        }
        for (width in LAYERS) {
            for (i in 0 until segments) {
                ribbon(consumer, matrix, points[i], points[i + 1], side, width, 0.3f * fade)
                ribbon(consumer, matrix, points[i], points[i + 1], up, width, 0.3f * fade)
            }
        }
    }

    private fun ribbon(consumer: VertexConsumer, matrix: Matrix4f, a: Vec3, b: Vec3, across: Vec3, width: Float, alpha: Float) {
        val wx = (across.x * width).toFloat()
        val wy = (across.y * width).toFloat()
        val wz = (across.z * width).toFloat()
        val r = 0.45f
        val g = 0.45f
        val bl = 0.5f
        // The lightning render type culls back faces, so each ribbon is drawn from both sides.
        vertex(consumer, matrix, a, -wx, -wy, -wz, r, g, bl, alpha)
        vertex(consumer, matrix, b, -wx, -wy, -wz, r, g, bl, alpha)
        vertex(consumer, matrix, b, wx, wy, wz, r, g, bl, alpha)
        vertex(consumer, matrix, a, wx, wy, wz, r, g, bl, alpha)
        vertex(consumer, matrix, a, wx, wy, wz, r, g, bl, alpha)
        vertex(consumer, matrix, b, wx, wy, wz, r, g, bl, alpha)
        vertex(consumer, matrix, b, -wx, -wy, -wz, r, g, bl, alpha)
        vertex(consumer, matrix, a, -wx, -wy, -wz, r, g, bl, alpha)
    }

    private fun vertex(consumer: VertexConsumer, matrix: Matrix4f, at: Vec3, dx: Float, dy: Float, dz: Float, r: Float, g: Float, b: Float, a: Float) {
        consumer.addVertex(matrix, at.x.toFloat() + dx, at.y.toFloat() + dy, at.z.toFloat() + dz).setColor(r, g, b, a)
    }
}
