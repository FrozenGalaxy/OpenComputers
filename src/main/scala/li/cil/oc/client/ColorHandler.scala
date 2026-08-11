package li.cil.oc.client

import li.cil.oc.api.internal.Colored
import li.cil.oc.common.block
import li.cil.oc.common.block.ChameliumBlock
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.init.{OCBlocks, OCItems}
import li.cil.oc.util.{Color, ItemColorizer, ItemUtils}
import net.minecraft.util.FastColor
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent

object ColorHandler {
  @SubscribeEvent
  def onRegisterBlocks(event: RegisterColorHandlersEvent.Block): Unit = {
    event.register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Cable => block.colorMultiplierOverride.getOrElse(0xFFFFFFFF)
      case _ => 0xFFFFFFFF
    },
      OCBlocks.Cable.get())

    event.register((state, world, pos, tintIndex) => if (pos == null) 0xFFFFFFFF else world.getBlockEntity(pos) match {
      case colored: Colored => colored.getColor
      case _ => state.getBlock match {
        case block: block.Case => Color.byTier(block.tier)
        case _ => 0xFFFFFFFF
      }
    },
      OCBlocks.CaseTier1.get(),
      OCBlocks.CaseTier2.get(),
      OCBlocks.CaseTier3.get(),
      OCBlocks.CaseTier4.get(),
      OCBlocks.CaseCreative.get())

    event.register((state, world, pos, tintIndex) => Color.rgbValues(state.getValue(block.ChameliumBlock.Color)),
      OCBlocks.ChameliumBlock.get())

    event.register((state, world, pos, tintIndex) => tintIndex,
      OCBlocks.Print.get())

    event.register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Screen => Color.byTier(block.tier)
      case _ => 0xFFFFFFFF
    },
      OCBlocks.ScreenTier1.get(),
      OCBlocks.ScreenTier2.get(),
      OCBlocks.ScreenTier3.get())
  }

  @SubscribeEvent
  def onRegisterItems(event: RegisterColorHandlersEvent.Item): Unit = {
    event.register((stack, tintIndex) =>
      if (ItemColorizer.hasColor(stack)) FastColor.ARGB32.opaque(ItemColorizer.getColor(stack)) else 0xFFFFFFFF,
      OCBlocks.Cable.get())

    event.register((stack, tintIndex) =>
      FastColor.ARGB32.opaque(Color.byTier(ItemUtils.caseTier(stack))),
      OCBlocks.CaseTier1.get(),
      OCBlocks.CaseTier2.get(),
      OCBlocks.CaseTier3.get(),
      OCBlocks.CaseTier4.get(),
      OCBlocks.CaseCreative.get())

    event.register((stack, tintIndex) =>
      FastColor.ARGB32.opaque(Color.rgbValues(stack.getOrDefault(OCComponents.CHAMELIUM_COLOR.get(), ChameliumBlock.DEFAULT_COLOR))),
      OCBlocks.ChameliumBlock.get())

    event.register((stack, tintIndex) => 0xFFFFFFFF,
      OCBlocks.ScreenTier1.get(),
      OCBlocks.ScreenTier2.get(),
      OCBlocks.ScreenTier3.get(),
      OCBlocks.Print.get(),
      OCBlocks.Robot.get())

    event.register((stack, tintIndex) => tintIndex match {
      case 1 => FastColor.ARGB32.opaque(if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else 0x66DD55)
      case _ => 0xFFFFFFFF
    }, OCItems.HoverBoots.get())

    event.register((stack: ItemStack, tintIndex: Int) => tintIndex match {
      case 1 => stack.getOrDefault(OCComponents.DISK_COLOR.get(), DyeColor.GRAY).getTextureDiffuseColor
      case _ => 0xFFFFFFFF
    }, OCItems.Floppy.get())
  }
}
