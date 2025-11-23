package com.atsuishio.superbwarfare.js;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;

public class ScriptManager {

    private static final LoadingCache<Thread, Context> CONTEXT = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<>() {
                @Override
                public @NotNull Context load(@NotNull Thread key) {
                    return Context.enter();
                }
            });

    public static Context getContext() {
        return CONTEXT.getUnchecked(Thread.currentThread());
    }
}
