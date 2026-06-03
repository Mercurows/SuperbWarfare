/*
 * Ported from Raise Sound Limit Simplified (Unofficial Forge Port)
 * https://github.com/RelativityMC/raise-sound-limit-simplified
 *
 * Original authors: RelativityMC, ishland, mrqx0195
 * Licensed under the MIT License.
 *
 * Thanks to the original authors for their work on raising Minecraft's sound source limit.
 */
package com.atsuishio.superbwarfare.sound

import com.atsuishio.superbwarfare.Mod
import io.netty.util.internal.PlatformDependent
import org.lwjgl.system.APIUtil
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryUtil
import java.nio.file.Files

/**
 * Increases Minecraft's OpenAL sound source limit from 255 to 4096.
 *
 * Two things are needed:
 * 1. Tell OpenAL Soft to allow 4096 sources (via ALSOFT_CONF env var)
 * 2. Override Minecraft's own Mth.clamp cap of 255 (via LibraryMixin)
 *
 * The ALSOFT_CONF is set via native putenv because OpenAL reads env vars
 * at the C level; Java's System.setProperty has no effect on native libraries.
 */
object SoundLimit {

    /** Maximum static (non-streaming) sound sources */
    @JvmField
    var maxSourcesCount: Int = 4096

    /** Maximum streaming sound sources */
    @JvmField
    var maxStreamingSources: Int = 8

    private var injected = false

    fun init() {
        if (injected) return
        injected = true

        try {
            // Write OpenAL Soft config to a temp file
            val confContent = "[general]\nsources = $maxSourcesCount\n"
            val tempFile = Files.createTempFile("sbw-openal", ".conf")
            Files.writeString(tempFile, confContent)

            // Register shutdown hook to clean up
            Runtime.getRuntime().addShutdownHook(Thread {
                try { Files.deleteIfExists(tempFile) } catch (_: Throwable) {}
            })

            // Set ALSOFT_CONF via native putenv (OpenAL reads env vars at C level)
            val envString = "ALSOFT_CONF=${tempFile.toAbsolutePath()}"
            Mod.LOGGER.info("Setting ALSOFT_CONF via native putenv: {}", envString)

            val buf = MemoryUtil.memASCII(envString)
            val libName = when {
                PlatformDependent.isWindows() -> "msvcrt.dll"
                PlatformDependent.isOsx() -> "libSystem.dylib"
                else -> "libc.so.6"
            }
            val funcName = if (PlatformDependent.isWindows()) "_putenv" else "putenv"
            val lib = APIUtil.apiCreateLibrary(libName)
            val funcAddr = APIUtil.apiGetFunctionAddress(lib, funcName)
            val result = JNI.invokePI(MemoryUtil.memAddress(buf), funcAddr)

            if (result != 0) {
                Mod.LOGGER.error("putenv failed with error code: {}", result)
            } else {
                Mod.LOGGER.info("ALSOFT_CONF set successfully")
            }
        } catch (e: Throwable) {
            Mod.LOGGER.error("Failed to set ALSOFT_CONF, sound limit may not increase", e)
        }
    }
}
