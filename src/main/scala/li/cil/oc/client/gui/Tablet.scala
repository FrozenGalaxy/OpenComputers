package li.cil.oc.client.gui

import li.cil.oc.common.menu
import li.cil.oc.Localization
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.client.Textures
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Player
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

import scala.jdk.CollectionConverters._
import scala.collection.IterableOnce.iterableOnceExtensionMethods

class Tablet(state: menu.Tablet, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[menu.Tablet] {

  protected var powerButton: ImageButton = _

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    powerButton.toggled = inventoryContainer.isRunning
    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def init(): Unit = {
    super.init()
    powerButton = new ImageButton(leftPos + 68, topPos + 34, 18, 18, (_: Button) => ClientPacketSender.sendTabletPower(inventoryContainer, !inventoryContainer.isRunning), Textures.GUI.ButtonPower, canToggle = true)
    addRenderableWidget(powerButton)
  }

  override protected def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY)
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[Component]
      tooltip.addAll(
        (if (inventoryContainer.isRunning) Localization.Computer.TurnOff
        else Localization.Computer.TurnOn)
          .linesIterator
          .map(Component.literal)
          .toSeq
          .asJava
      )
      graphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos)
    }
  }

  override def lockedStack = inventoryContainer.stack
}
