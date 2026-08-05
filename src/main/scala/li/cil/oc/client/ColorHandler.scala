package li.cil.oc.client

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.internal.Colored
import li.cil.oc.common.block
import li.cil.oc.common.init.{OCBlocks, OCItems}
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
  private def opaque(color: Int): Int =
    0xFF000000 | (color & 0x00FFFFFF)

  def init(): Unit = {
    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Cable => block.colorMultiplierOverride.getOrElse(0xFFFFFFFF)
      case _ => 0xFFFFFFFF
    },
      OCBlocks.Cable.get())

    register((state, world, pos, tintIndex) => if (pos == null) 0xFFFFFFFF else world.getBlockEntity(pos) match {
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

    register((state, world, pos, tintIndex) => Color.rgbValues(state.getValue(block.ChameliumBlock.Color)),
      OCBlocks.ChameliumBlock.get())

    register((state, world, pos, tintIndex) => tintIndex,
      OCBlocks.Print.get())

    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Screen => Color.byTier(block.tier)
      case _ => 0xFFFFFFFF
    },
      OCBlocks.ScreenTier1.get(),
      OCBlocks.ScreenTier2.get(),
      OCBlocks.ScreenTier3.get())

    register((stack, tintIndex) =>
      if (ItemColorizer.hasColor(stack)) opaque(ItemColorizer.getColor(stack)) else 0xFFFFFFFF,
      OCBlocks.Cable.get())

    register((stack, tintIndex) =>
      opaque(Color.byTier(ItemUtils.caseTier(stack))),
      OCBlocks.CaseTier1.get(),
      OCBlocks.CaseTier2.get(),
      OCBlocks.CaseTier3.get(),
      OCBlocks.CaseTier4.get(),
      OCBlocks.CaseCreative.get())

    register((stack, tintIndex) =>
      opaque(Color.rgbValues(DyeColor.byId(stack.getDamageValue))),
      OCBlocks.ChameliumBlock.get())

    register((stack, tintIndex) => 0xFFFFFFFF,
      OCBlocks.ScreenTier1.get(),
      OCBlocks.ScreenTier2.get(),
      OCBlocks.ScreenTier3.get(),
      OCBlocks.Print.get(),
      OCBlocks.Robot.get())

    register((stack, tintIndex) =>
      if (tintIndex == 1) {
        val rgb = if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else 0x66DD55
        opaque(rgb)
      } else 0xFFFFFFFF,
      OCItems.HoverBoots.get())
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