package li.cil.oc.client

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.internal.Colored
import li.cil.oc.common.block
import li.cil.oc.common.init.{Blocks, Items}
import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import li.cil.oc.util.ItemUtils
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockColor
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.level.{BlockAndTintGetter, BlockGetter, ItemLike}

object ColorHandler {
  def init(): Unit = {
    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Cable => block.colorMultiplierOverride.getOrElse(0xFFFFFFFF)
      case _ => 0xFFFFFFFF
    },
      Blocks.Cable.get())

    register((state, world, pos, tintIndex) => if (pos == null) 0xFFFFFFFF else world.getBlockEntity(pos) match {
      case colored: Colored => colored.getColor
      case _ => state.getBlock match {
        case block: block.Case => Color.byTier(block.tier)
        case _ => 0xFFFFFFFF
      }
    },
      Blocks.CaseTier1.get(),
      Blocks.CaseTier2.get(),
      Blocks.CaseTier3.get(),
      Blocks.CaseTier4.get(),
      Blocks.CaseCreative.get())

    register((state, world, pos, tintIndex) => Color.rgbValues(state.getValue(block.ChameliumBlock.Color)),
      Blocks.ChameliumBlock.get())

    register((state, world, pos, tintIndex) => tintIndex,
      Blocks.Print.get())

    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Screen => Color.byTier(block.tier)
      case _ => 0xFFFFFFFF
    },
      Blocks.ScreenTier1.get(),
      Blocks.ScreenTier2.get(),
      Blocks.ScreenTier3.get())

    register((stack, tintIndex) => if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else tintIndex,
      Blocks.Cable.get())

    register((stack, tintIndex) => Color.byTier(ItemUtils.caseTier(stack)),
      Blocks.CaseTier1.get(),
      Blocks.CaseTier2.get(),
      Blocks.CaseTier3.get(),
      Blocks.CaseTier4.get(),
      Blocks.CaseCreative.get())

    register((stack, tintIndex) => Color.rgbValues(DyeColor.byId(stack.getDamageValue)),
      Blocks.ChameliumBlock.get())

    register((stack, tintIndex) => tintIndex,
      Blocks.ScreenTier1.get(),
      Blocks.ScreenTier2.get(),
      Blocks.ScreenTier3.get(),
      Blocks.Print.get(),
      Blocks.Robot.get())

    register((stack, tintIndex) =>
      if (tintIndex == 1) {
        // Item tint colors are ARGB in modern Minecraft. Preserve full opacity;
        // returning a 24-bit RGB value makes alpha = 0 and the layer invisible.
        val rgb = if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else 0x66DD55
        0xFF000000 | (rgb & 0x00FFFFFF)
      } else 0xFFFFFFFF,
      Items.HoverBoots.get())
  }

  def register(handler: (BlockState, BlockGetter, BlockPos, Int) => Int, blocks: Block*): Unit = {
    Minecraft.getInstance.getBlockColors.register((state: BlockState, world: BlockAndTintGetter, pos: BlockPos, tintIndex: Int) =>
      handler(state, world, pos, tintIndex), blocks: _*)
  }

  def register(handler: (ItemStack, Int) => Int, items: ItemLike*): Unit = {
    Minecraft.getInstance.getItemColors.register((stack: ItemStack, tintIndex: Int)
    => handler(stack, tintIndex), items: _*)
  }
}
