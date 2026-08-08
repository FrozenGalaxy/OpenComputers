package li.cil.oc.client

import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import com.google.common.base.Charsets
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent

import scala.collection.mutable
import scala.ref.WeakReference
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.client.resources.sounds.{AbstractSoundInstance, SoundInstance, TickableSoundInstance}
import net.minecraft.sounds.SoundSource
import net.neoforged.neoforge.event.level.LevelEvent

object Sound {
  private val sources = mutable.WeakHashMap.empty[BlockEntity, PseudoLoopingStream]

  private val commandQueue = mutable.PriorityQueue.empty[Command]

  private val updateTimer = new Timer("OpenComputers-SoundUpdater", true)
  if (Settings.get.soundVolume > 0) {
    updateTimer.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = {
        sources.synchronized(Sound.updateCallable = Some(() => processQueue()))
      }
    }, 500, 50)
  }

  private var updateCallable = None: Option[() => Unit]

  private def processQueue(): Unit = {
    if (commandQueue.nonEmpty) {
      commandQueue.synchronized {
        while (commandQueue.nonEmpty && commandQueue.head.when < System.currentTimeMillis()) {
          try commandQueue.dequeue()() catch {
            case t: Throwable => OpenComputers.log.warn("Error processing sound command.", t)
          }
        }
      }
    }
  }

  def startLoop(BlockEntity: BlockEntity, name: String, volume: Float = 1f, delay: Long = 0): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StartCommand(System.currentTimeMillis() + delay, WeakReference(BlockEntity), name, volume)
      }
    }
  }

  def stopLoop(BlockEntity: BlockEntity): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StopCommand(WeakReference(BlockEntity))
      }
    }
  }

  def updatePosition(BlockEntity: BlockEntity): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new UpdatePositionCommand(WeakReference(BlockEntity))
      }
    }
  }

  @SubscribeEvent
  def onTick(e: ClientTickEvent.Pre): Unit = {
    sources.synchronized {
      updateCallable.foreach(_ ())
      updateCallable = None
    }
  }

  @SubscribeEvent
  def onWorldUnload(event: LevelEvent.Unload): Unit = {
    commandQueue.synchronized(commandQueue.clear())
    sources.synchronized(try sources.foreach(_._2.stop()) catch {
      case _: Throwable => // Ignore.
    })
    sources.clear()
  }

  private abstract class Command(val when: Long, val blockEntity: WeakReference[BlockEntity]) extends Ordered[Command] {
    def apply(): Unit

    override def compare(that: Command) = (that.when - when).toInt
  }

  private class StartCommand(when: Long, blockEntity: WeakReference[BlockEntity], val name: String, val volume: Float) extends Command(when, blockEntity) {
    override def apply(): Unit = {
      blockEntity.get.foreach { entity =>
        sources.synchronized {
          val current = sources.getOrElse(entity, null)
          if (current == null || !current.getLocation.getPath.equals(name)) {
            if (current != null) current.stop()
            val sound = new PseudoLoopingStream(blockEntity, volume, name)
            sources(entity) = sound
            Minecraft.getInstance.getSoundManager.play(sound)
          }
        }
      }
    }
  }

  private class StopCommand(blockEntity: WeakReference[BlockEntity]) extends Command(System.currentTimeMillis() + 1, blockEntity) {
    override def apply(): Unit = {
      blockEntity.get.foreach { entity =>
        sources.synchronized {
          sources.remove(entity) match {
            case Some(sound) => sound.stop()
            case _ =>
          }
        }
        commandQueue.synchronized {
          // Remove all other commands for this block entity from the queue.
          commandQueue ++= commandQueue.dequeueAll.filter(_.blockEntity.get.exists(_ ne entity))
        }
      }
    }
  }

  private class UpdatePositionCommand(blockEntity: WeakReference[BlockEntity]) extends Command(System.currentTimeMillis(), blockEntity) {
    override def apply(): Unit = {
      blockEntity.get.foreach { entity =>
        sources.synchronized {
          sources.get(entity) match {
            case Some(sound) => sound.updatePosition()
            case _ =>
          }
        }
      }
    }
  }

  private class PseudoLoopingStream(val blockEntity: WeakReference[BlockEntity], val subVolume: Float, name: String)
    extends AbstractSoundInstance(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name), SoundSource.BLOCKS, SoundInstance.createUnseededRandom()) with TickableSoundInstance {

    var stopped = false
    volume = subVolume * Settings.get.soundVolume
    // Computer running sounds are world-positioned and must attenuate with
    // distance. Relative sounds are listener-relative and bypass world range.
    relative = false
    looping = true
    updatePosition()

    def updatePosition(): Unit = {
      blockEntity.get.foreach { entity =>
        val pos = entity.getBlockPos
        x = pos.getX + 0.5
        y = pos.getY + 0.5
        z = pos.getZ + 0.5
      }
    }

    override def canStartSilent() = true

    override def isStopped() = stopped

    // Required by ITickableSound, which is required to update position while playing
    override def tick(): Unit = if (blockEntity.get.isDefined) updatePosition() else stop()

    def stop(): Unit = {
      stopped = true
      looping = false
    }
  }
}
