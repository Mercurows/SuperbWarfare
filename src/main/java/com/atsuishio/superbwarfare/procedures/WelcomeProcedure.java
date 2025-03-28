package com.atsuishio.superbwarfare.procedures;

import com.atsuishio.superbwarfare.Mod;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WelcomeProcedure {
    @SubscribeEvent
    public static void onFMLCommonSetup(FMLCommonSetupEvent event) {
        if (event != null) {
            execute(event, event.getIMCStream());
        }
    }

    public static void execute(Stream<InterModComms.IMCMessage> stream) {
        execute(null, stream);
    }

    private static void execute(@Nullable Event event, Stream<InterModComms.IMCMessage> stream) {
        if (event == null)
            return;
        Logger logger = null;
        if ((logger == null ? logger = Mod.LOGGER : LogManager.getLogger(Mod.class)) instanceof Logger) {
            {
                Logger _lgr = logger == null ? logger = Mod.LOGGER : LogManager.getLogger(Mod.class);
                _lgr.info("This Mod used to be made by MCreator!");
            }
        }
    }
}
