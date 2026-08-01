package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.{ByteBufferBuilder, PoseStack}
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource

object TextBufferRenderCache {
  val renderer =
    if (Settings.get.fontRenderer == "texture") new font.StaticFontRenderer()
    else new font.DynamicFontRenderer()

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  def render(stack: PoseStack, buffer: TextBufferRenderData): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering")
    renderDirect(stack, buffer)
    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  def renderImmediate(stack: PoseStack, _renderBuffer: MultiBufferSource, buffer: TextBufferRenderData): Unit = {
    RenderState.checkError(getClass.getName + ".renderImmediate: entering")
    renderDirect(stack, buffer)
    RenderState.checkError(getClass.getName + ".renderImmediate: leaving")
  }

  private def renderDirect(stack: PoseStack, buffer: TextBufferRenderData): Unit = {
    for (line <- buffer.data.buffer) {
      renderer.generateChars(line)
    }

    // Match the immediate-mode behavior of the working pre-1.13 renderer.
    // The port's custom VBO cache loses render state in both GUI and block
    // entity paths, leaving otherwise valid screen contents invisible.
    val byteBuffer = new ByteBufferBuilder(786432)
    try {
      val source = MultiBufferSource.immediate(byteBuffer)
      renderer.drawBuffer(stack, source, buffer.data, buffer.viewport._1, buffer.viewport._2)
      source.endBatch()
      buffer.dirty = false
    }
    finally byteBuffer.close()
  }

}
