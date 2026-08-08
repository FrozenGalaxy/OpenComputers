package li.cil.oc.integration.create

import li.cil.oc.integration.{ModProxy, Mods}

object ModCreate extends ModProxy {
  override def getMod = Mods.Create

  override def initialize(): Unit = {
    CreateContraptionTransformers.register()
    CreateDrivers.register()
  }
}
