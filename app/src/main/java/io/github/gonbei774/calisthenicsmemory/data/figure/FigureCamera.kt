package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.cos
import kotlin.math.sin

/** A point on screen, in metres, with its depth; larger depth is nearer the viewer. */
data class Projected(val x: Float, val y: Float, val depth: Float)

/**
 * The three-quarter view the owner chose (2026-10-01): the camera turned about 35 degrees round the
 * figure and looking slightly down. It is orthographic, so lengths stay comparable.
 * An exercise may turn it further where that reads better, such as push-ups seen from the head side.
 */
data class FigureCamera(val yaw: Float = DEFAULT_YAW, val pitch: Float = DEFAULT_PITCH) {

    fun project(p: Vec3): Projected {
        val a = yaw.rad()
        // Turn the world about the vertical axis, then tip it towards the viewer.
        val x1 = p.x * cos(a) + p.z * sin(a)
        val z1 = -p.x * sin(a) + p.z * cos(a)
        val b = pitch.rad()
        val y2 = p.y * cos(b) - z1 * sin(b)
        val z2 = p.y * sin(b) + z1 * cos(b)
        // The viewer faces the figure, so the figure's right is on the viewer's left.
        return Projected(-x1, y2, z2)
    }

    /** The world direction pointing from the figure towards the viewer; surfaces facing it are seen. */
    val towardViewer: Vec3
        get() = Vec3(-sin(yaw.rad()) * cos(pitch.rad()), sin(pitch.rad()), cos(yaw.rad()) * cos(pitch.rad()))

    companion object {
        const val DEFAULT_YAW = 35f
        const val DEFAULT_PITCH = 12f
    }
}
