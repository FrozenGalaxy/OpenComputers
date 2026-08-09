package li.cil.oc

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.common.init.OCItems
import li.cil.oc.common.init.OCItems.SECTION_Y_VALUES
import li.cil.oc.mixin.accessor.AbstractContainerScreenAccess
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.{CreativeModeTab, CreativeModeTabs}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.{DeferredHolder, DeferredRegister}

import scala.jdk.CollectionConverters.MapHasAsScala

object CreativeTab {
  val CREATIVE_TABS: DeferredRegister[CreativeModeTab] =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.ID)
  var CURRENT_ROW = 0
  val MAIN: DeferredHolder[CreativeModeTab, CreativeModeTab] = CREATIVE_TABS.register("main", () =>
    CreativeModeTab.builder()
      .title(Component.translatable(s"itemGroup.${OpenComputers.Name}"))
      .icon(() => api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1))
      .build()
  )

  @SubscribeEvent
  def onBuildContents(event: BuildCreativeModeTabContentsEvent): Unit = {
    if (event.getTabKey == MAIN.getKey) {
      //OCItems.decorateCreativeTab(event, ModOpenComputers.hasRedstoneCardT2)
      //OpenPrinter.addCreativeItems(event)
    } else if (event.getTabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
      event.accept(OCItems.createChargedHoverBoots())
    }
  }

  def renderBanners(screen: CreativeModeInventoryScreen, graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    val ps = graphics.pose
    ps.pushPose()
    RenderSystem.enableDepthTest()
    RenderSystem.setShaderColor(1, 1, 1, 1)

    val left = screen.asInstanceOf[AbstractContainerScreenAccess].getLeftPos + 8
    val top = screen.asInstanceOf[AbstractContainerScreenAccess].getTopPos + 17
    ps.translate(left, top, 0)

    val BannerWidth = 162
    val BannerHeight = 18
    val VisibleRows = 5


    for ((id, yValue) <- SECTION_Y_VALUES.asScala
         if yValue >= CURRENT_ROW && yValue < CURRENT_ROW + VisibleRows) {

      val bannerTexture = ResourceLocation.fromNamespaceAndPath(
        OpenComputers.ID,
        s"textures/gui/banner.png"
      )

      //OpenComputers.log.warn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      //OpenComputers.log.warn(bannerTexture)

      val y = (yValue - CURRENT_ROW) * BannerHeight

      graphics.blit(
        bannerTexture,
        0,
        y,
        0,
        0,
        BannerWidth,
        BannerHeight,
        BannerWidth,
        BannerHeight
      )

      val font = Minecraft.getInstance.font
      graphics.drawString(font, Component.translatable(s"itemGroup.${OpenComputers.Name}.section.$id"), 8, y+6, 0x7, false)
    }

    RenderSystem.disableDepthTest();
    ps.popPose();
  }
}