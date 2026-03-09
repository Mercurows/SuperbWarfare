package com.atsuishio.superbwarfare.procedures

import com.atsuishio.superbwarfare.Mod
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.InterModComms.IMCMessage
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.loading.LoadingModList
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.stream.Stream

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object WelcomeProcedure {
    @SubscribeEvent
    fun onFMLCommonSetup(event: FMLCommonSetupEvent?) {
        if (event != null) {
            execute(event, event.imcStream)
        }
    }

    fun execute(stream: Stream<IMCMessage?>?) {
        execute(null, stream)
    }

    private fun execute(event: Event?, stream: Stream<IMCMessage?>?) {
        if (event == null) return
        var logger: Logger? = null
        if ((if (logger == null) Mod.LOGGER.also {
                logger = it
            } else LogManager.getLogger(Mod::class.java)) is Logger) {
            run {
                val _lgr = ((if (logger == null) Mod.LOGGER.also {
                    logger = it
                } else LogManager.getLogger(Mod::class.java)) as Logger)
                val _str = """Now Loading...
* This Mod used to be made by MCreator *
  _____  ______  __          __ 
 / ____| |  __ \ \ \        / / 
| (___   | |__) | \ \  /\  / /  
 \___ \  |  __ (   \ \/  \/ /   
 ____) | | |__) |   \  /\  /    
|_____/  |_____/     \/  \/
* Superb Warfare - Version: ${DefaultArtifactVersion(LoadingModList.get().getModFileById(Mod.MODID)?.versionString())} *
                """.trimIndent()
                _lgr.info(_str)
            }
        }
    }
}