package li.cil.oc.common.blockentity.traits

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.api.network.Node
import li.cil.oc.common.datacomponents.CompoundStorage
import net.minecraft.core.HolderLookup
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.minecraft.nbt.{CompoundTag, NbtOps}

trait TextBuffer extends Environment with Tickable {
  private final val ClientBufferComponentsTag = Settings.namespace + "clientBufferComponents"
  private var pendingServerBufferData: CompoundTag = _
  lazy val buffer: internal.TextBuffer = {
    val screenItem = api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1)
    val buffer = api.Driver.driverFor(screenItem, getClass).createEnvironment(screenItem, this).asInstanceOf[api.internal.TextBuffer]
    val (maxWidth, maxHeight) = Settings.screenResolutionsByTier(tier)
    buffer.setMaximumResolution(maxWidth, maxHeight)
    buffer.setMaximumColorDepth(Settings.screenDepthsByTier(tier))
    buffer
  }

  override def node: Node = buffer.node

  def tier: Int

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (isClient || isConnected) {
      buffer.update()
    }
  }

  // ----------------------------------------------------------------------- //

  private def reapplyTierToBuffer(): Unit = {
    // Re-apply tier-based limits before loading data. This guards against the
    // case where the `buffer` lazy val was forced before `load(nbt)` ran
    // (e.g. during network join scheduled by initialize()), which would leave
    // it initialised with the default tier-0 (OneBit) depth even for a
    // higher-tier screen.  setMaximumColorDepth only updates the `maxDepth`
    // field; it does NOT touch the already-constructed data buffer, so calling
    // it again here is safe and idempotent.
    val (maxWidth, maxHeight) = Settings.screenResolutionsByTier(tier)
    buffer.setMaximumResolution(maxWidth, maxHeight)
    buffer.setMaximumColorDepth(Settings.screenDepthsByTier(tier))
  }

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    // BlockEntity.loadStatic calls loadWithComponents/loadAdditional before it
    // assigns the level. Loading the external screen contents needs the
    // dimension and position, so postpone it until clearRemoved/initialize.
    if (getLevel == null) pendingServerBufferData = nbt.copy()
    else loadServerBufferData(nbt, provider)
  }

  private def loadServerBufferData(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    reapplyTierToBuffer()
    buffer.loadData(nbt, provider)
  }

  override protected def initialize(): Unit = {
    super.initialize()
    if (isServer && getLevel != null && pendingServerBufferData != null) {
      val data = pendingServerBufferData
      pendingServerBufferData = null
      loadServerBufferData(data, getLevel.registryAccess())
    }
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    buffer.saveData(nbt, provider)
  }

  override def loadForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForClient(nbt, provider)
    reapplyTierToBuffer()
    if (nbt.contains(ClientBufferComponentsTag)) {
      val storage = CompoundStorage.CODEC.parse(NbtOps.INSTANCE, nbt.get(ClientBufferComponentsTag)).getOrThrow()
      buffer.loadData(storage)
    }
    else buffer.loadData(nbt, provider)
  }

  @OnlyIn(Dist.CLIENT)
  override def saveForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForClient(nbt, provider)
    val storage = new CompoundStorage()
    buffer.saveData(storage)
    nbt.put(ClientBufferComponentsTag, CompoundStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow())
  }
}
