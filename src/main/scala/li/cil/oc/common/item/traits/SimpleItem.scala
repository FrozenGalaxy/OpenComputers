package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.api.internal.Robot
import li.cil.oc.client.renderer.item.ItemUpgradeRenderer
import li.cil.oc.common.blockentity
import li.cil.oc.util.{BlockPosition, ClientAccessHelper, ItemUtils, Rarity, Tooltip}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelReader
import net.minecraft.core.Direction
import net.minecraft.world.{InteractionHand, InteractionResult, InteractionResultHolder, item}
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.world.item.TooltipFlag
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToScala._
import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.common.item.data.TabletData
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item.TooltipContext
import net.neoforged.neoforge.common.extensions.IItemExtension
import li.cil.oc.common.datacomponents.OCComponents

trait SimpleItem extends Item with api.driver.item.UpgradeRenderer with IItemExtension {
  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  @Deprecated
  protected var unlocalizedName = getClass.getSimpleName.toLowerCase

  @Deprecated
  override def getDescriptionId = "item.oc." + unlocalizedName

  override def doesSneakBypassUse(stack: ItemStack, level: LevelReader, pos: BlockPos, player: Player): Boolean = {
    level.getBlockEntity(pos) match {
      case drive: blockentity.DiskDrive => true
      case _ => super.doesSneakBypassUse(stack, level, pos, player)
    }
  }

  @Deprecated
  override def onItemUseFirst(stack: ItemStack, ctx: UseOnContext): InteractionResult = {
    val pos = ctx.getClickedPos
    val hitPos = ctx.getClickLocation
    onItemUseFirst(stack, ctx.getPlayer, ctx.getPlayer.level, pos, ctx.getClickedFace,
      (hitPos.x - pos.getX).toFloat, (hitPos.y - pos.getY).toFloat, (hitPos.z - pos.getZ).toFloat, ctx.getHand)
  }

  @Deprecated
  def onItemUseFirst(stack: ItemStack, player: Player, level: Level, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: InteractionHand): InteractionResult = InteractionResult.PASS

  @Deprecated
  override def useOn(ctx: UseOnContext): InteractionResult =
    ctx.getItemInHand match {
      case stack: ItemStack => {
        val world = ctx.getLevel
        val pos = BlockPosition(ctx.getClickedPos, world)
        val hitPos = ctx.getClickLocation
        val success = onItemUse(stack, ctx.getPlayer, pos, ctx.getClickedFace,
          (hitPos.x - pos.x).toFloat, (hitPos.y - pos.y).toFloat, (hitPos.z - pos.z).toFloat)
        if (success) InteractionResult.sidedSuccess(world.isClientSide) else InteractionResult.PASS
      }
      case _ => super.useOn(ctx)
    }

  @Deprecated
  def onItemUse(stack: ItemStack, player: Player, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  /**
   * Opt in to the shared double sneak-right-click gesture handled by SimpleItem.
   * Items only need to override this flag; the timing and action live here.
   */
  protected def canResetComponentIdentity: Boolean = false

  @Deprecated
  override def use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] =
    player.getItemInHand(hand) match {
      case stack: ItemStack if canResetComponentIdentity && player.isShiftKeyDown =>
        if (!world.isClientSide) {
          if (SimpleItem.registerSneakClick(player, hand, stack.getItem, world.getGameTime)) {
            stack.remove(OCComponents.ADDRESS.get())
            stack.remove(OCComponents.VISIBILITY.get())

            player.displayClientMessage(Component.literal("Component UUID reset."), true)
          }
          else {
            player.displayClientMessage(Component.literal("Double click quickly to reset"), true)
          }
        }
        new InteractionResultHolder(InteractionResult.sidedSuccess(world.isClientSide), stack)

      case stack: ItemStack => use(stack, world, player)
      case _ => super.use(world, player, hand)
    }

  @Deprecated
  def use(stack: ItemStack, level: Level, player: Player): InteractionResultHolder[ItemStack] =
    new InteractionResultHolder(InteractionResult.PASS, stack)

  protected def tierFromDriver(stack: ItemStack): Int =
    api.Driver.driverFor(stack) match {
      case driver: api.driver.DriverItem => driver.tier(stack)
      case _ => 0
    }

  protected def tooltipName = Option(unlocalizedName)

  protected def tooltipData = Seq.empty[Any]

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    if (tooltipName.isDefined) {
      for (curr <- Tooltip.get(tooltipName.get, tooltipData: _*)) {
        tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle))
      }
      tooltipExtended(stack, tooltip)
    }
    else {
      for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase)) {
        tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle))
      }
    }
    tooltipCosts(stack, tooltip)
  }

  // For stuff that goes to the normal 'extended' tooltip, before the costs.
  protected def tooltipExtended(stack: ItemStack, tooltip: java.util.List[Component]): Unit = {}

  protected def tooltipCosts(stack: ItemStack, tooltip: java.util.List[Component]): Unit = {
    val tag = ItemUtils.getTag(stack)
    if (tag != null && tag.contains(Settings.namespace + "data")) {
      val data = tag.getCompound(Settings.namespace + "data")
      if (data.contains("node") && data.getCompound("node").contains("address")) {
        tooltip.add(Component.literal("§8" + data.getCompound("node").getString("address").substring(0, 13) + "...§7"))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def computePreferredMountPoint(stack: ItemStack, robot: Robot, availableMountPoints: util.Set[String]): String =
    ItemUpgradeRenderer.preferredMountPoint(stack, availableMountPoints)

  @OnlyIn(Dist.CLIENT)
  override def render(matrix: PoseStack, buffer: MultiBufferSource, light: Int, stack: ItemStack, mountPoint: MountPoint, robot: Robot, pt: Float): Unit =
    ItemUpgradeRenderer.render(matrix, buffer, light, stack, mountPoint)
}

object SimpleItem {
  private val DoubleSneakClickWindowTicks = 6L
  private val lastSneakClicks = scala.collection.mutable.HashMap.empty[(java.util.UUID, InteractionHand, Item), Long]

  private def registerSneakClick(player: Player, hand: InteractionHand, item: Item, now: Long): Boolean = synchronized {
    val key = (player.getUUID, hand, item)
    val isDoubleClick = lastSneakClicks.get(key).exists(last => now - last > 0 && now - last <= DoubleSneakClickWindowTicks)

    if (isDoubleClick) lastSneakClicks.remove(key)
    else lastSneakClicks.update(key, now)

    isDoubleClick
  }
}
