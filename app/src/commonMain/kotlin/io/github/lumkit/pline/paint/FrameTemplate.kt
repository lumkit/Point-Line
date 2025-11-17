package io.github.lumkit.pline.paint

import io.github.lumkit.pline.graphics.Frame
import kotlinx.serialization.Serializable

@Serializable
data class FrameTemplate(
    val frame: Frame,
    val name: String,
)

object FrameTool {
    val defaultTemplates = listOf(
        FrameTemplate(
            frame = Frame(
                color = 0xFFFFFFFF,
                width = 1024,
                height = 1024,
            ),
            name = "1:1(1024x1024)",
        ),
        FrameTemplate(
            frame = Frame(
                color = 0xFFFFFFFF,
                width = 1920,
                height = 1080,
            ),
            name = "16:9(1920x1080)",
        ),
        FrameTemplate(
            frame = Frame(
                color = 0xFFFFFFFF,
                width = 1280,
                height = 720,
            ),
            name = "4:3(1280x720)",
        ),
        FrameTemplate(
            frame = Frame(
                color = 0xFFFFFFFF,
                width = 1280,
                height = 1024,
            ),
            name = "5:4(1280x1024)",
        ),
        FrameTemplate(
            frame = Frame(
                color = 0xFFFFFFFF,
                width = 1080,
                height = 1920,
            ),
            name = "9:16(1080x1920)",
        ),
    )
}
