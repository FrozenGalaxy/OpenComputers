package li.cil.oc.client.renderer.block

import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.common.datacomponents.OCComponents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.BlockModelShaper
import net.minecraft.client.resources.model.{BakedModel, ModelResourceLocation}
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.{DyeColor, Item, ItemStack}
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ModelEvent

import scala.collection.mutable

@OnlyIn(Dist.CLIENT)
object ModelInitialization {
  final val CableBlockLocation           = loc(Constants.BlockName.Cable,             "")
  final val CableItemLocation            = loc(Constants.BlockName.Cable,             "inventory")
  final val NetSplitterBlockLocation     = loc(Constants.BlockName.NetSplitter,       "")
  final val NetSplitterItemLocation      = loc(Constants.BlockName.NetSplitter,       "inventory")
  final val PrintBlockLocation           = loc(Constants.BlockName.Print,             "")
  final val PrintItemLocation            = loc(Constants.BlockName.Print,             "inventory")
  final val RobotBlockLocation           = loc(Constants.BlockName.Robot,             "")
  final val RobotItemLocation            = loc(Constants.BlockName.Robot,             "inventory")
  final val RobotAfterimageBlockLocation = loc(Constants.BlockName.RobotAfterimage,   "")
  final val RackBlockLocation            = loc(Constants.BlockName.Rack,              "")

  private def loc(name: String, variant: String): ModelResourceLocation = {
    val id = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, name)
    if (variant == "inventory") ModelResourceLocation.inventory(id)
    else new ModelResourceLocation(id, variant)
  }

  private val modelRemappings = mutable.Map.empty[ModelResourceLocation, ModelResourceLocation]

  // Called from ClientProxy.preInit().
  // Model remappings are intentionally NOT built here: on modern NeoForge the
  // model bake/reload may already be running while common setup work executes.
  def preInit(): Unit = {
    registerItemColors()
  }

  private def rebuildModelRemappings(): Unit = {
    modelRemappings.clear()
    registerBlockRemapping(Constants.BlockName.Cable,             CableBlockLocation,           CableItemLocation)
    registerBlockRemapping(Constants.BlockName.NetSplitter,       NetSplitterBlockLocation,     NetSplitterItemLocation)
    registerBlockRemapping(Constants.BlockName.Print,             PrintBlockLocation,           PrintItemLocation)
    registerBlockRemapping(Constants.BlockName.Robot,             RobotBlockLocation,           RobotItemLocation)
    registerBlockRemapping(Constants.BlockName.RobotAfterimage,   RobotAfterimageBlockLocation, null)
  }

  // Item models are loaded from assets on modern NeoForge.
  def registerModel(_instance: ItemLike, _id: String): Unit = {}

  // ── Dynamic item models ────────────────────────────────────────────────────

  // ── Item colors ────────────────────────────────────────────────────────────

  private def registerItemColors(): Unit = {
    withItem(Constants.ItemName.Floppy) { item =>
      Minecraft.getInstance.getItemColors.register(
        (stack: ItemStack, tintIndex: Int) => {
          if (tintIndex == 1) {
            val color =
              Option(stack.get(OCComponents.DISK_COLOR.get())).getOrElse {
                if (stack.has(DataComponents.CUSTOM_DATA) && stack.get(DataComponents.CUSTOM_DATA).contains(Settings.namespace + "color")) {
                  val legacyColor = stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt(Settings.namespace + "color")
                  DyeColor.byId(legacyColor max 0 min 15)
                }
                else DyeColor.GRAY
              }

            color.getTextureDiffuseColor
          }
          else 0xFFFFFFFF
        },
        item
      )
    }
  }

  private def withItem(name: String)(f: Item => Unit): Unit =
    Option(api.Items.get(name)).map(_.item()).filter(_ != null).foreach(f)

  // ── Block model state remapping ────────────────────────────────────────────

  private def registerBlockRemapping(
                                      blockName:     String,
                                      blockLocation: ModelResourceLocation,
                                      itemLocation:  ModelResourceLocation
                                    ): Unit = {
    val descriptor = api.Items.get(blockName)
    if (descriptor == null) return

    if (blockLocation != null) {
      val block = descriptor.block()
      if (block != null)
        block.getStateDefinition.getPossibleStates.forEach { state =>
          modelRemappings += stateToModelLocation(state) -> blockLocation
        }
    }
  }

  private def stateToModelLocation(state: BlockState): ModelResourceLocation =
    BlockModelShaper.stateToModelLocation(state)

  // ── Event handlers ─────────────────────────────────────────────────────────

  @SubscribeEvent
  def onModifyBakingResult(e: ModelEvent.ModifyBakingResult): Unit = {
    rebuildModelRemappings()
    val registry = e.getModels

    registry.put(CableBlockLocation,           CableModel)
    registry.put(CableItemLocation,            CableModel)
    registry.put(NetSplitterBlockLocation,     NetSplitterModel)
    registry.put(PrintBlockLocation,           PrintModel)
    registry.put(PrintItemLocation,            PrintModel)
    registry.put(RobotBlockLocation,           NullModel)
    registry.put(RobotItemLocation,            RobotModel)
    registry.put(RobotAfterimageBlockLocation, NullModel)
    registry.put(loc(Constants.ItemName.Drone, "inventory"), DroneModel)

    val modelOverrides = Map[String, BakedModel => BakedModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier4 -> (_ => ScreenModel),
      Constants.BlockName.Rack        -> (parent => new ServerRackModel(parent))
    )

    registry.keySet.toArray.foreach {
      case location: ModelResourceLocation =>
        for ((name, model) <- modelOverrides) {
          val pattern = s"^${Settings.resourceDomain}:$name#.*"
          if (location.toString.matches(pattern))
            registry.put(location, model(registry.get(location)))
        }

      case _ =>
    }

    for ((real, virtual) <- modelRemappings)
      registry.put(real, registry.get(virtual))
  }
}
