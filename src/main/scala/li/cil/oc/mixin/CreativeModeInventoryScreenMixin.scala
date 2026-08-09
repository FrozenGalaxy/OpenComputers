package li.cil.oc.mixin

import li.cil.oc.CreativeTab
import li.cil.oc.mixin.accessor.CreativeModeInventoryScreenAccess
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.{At, Inject}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

// Inspired by :
//  https://github.com/Creators-of-Aeronautics/Simulated-Project/blob/main/simulated/common/src/main/java/dev/simulated_team/simulated/mixin/creative_tab_sections/CreativeModeInventoryScreenMixin.java
// On 09/08/26 under MIT Licence

@Mixin(Array(classOf[CreativeModeInventoryScreen]))
class CreativeModeInventoryScreenMixin {

  @Inject(
    method = Array("render"),
    at = Array(new At("TAIL"))
  )
  private def openComputers$render(
                                    guiGraphics: GuiGraphics,
                                    mouseX: Int,
                                    mouseY: Int,
                                    partialTick: Float,
                                    ci: CallbackInfo
                                  ): Unit = {
    val tab = CreativeModeInventoryScreenAccess.selectedTab
    if (tab eq CreativeTab.MAIN.get()) {
      CreativeTab.renderBanners(
        this.asInstanceOf[CreativeModeInventoryScreen],
        guiGraphics,
        mouseX,
        mouseY
      )
    }
  }
}
