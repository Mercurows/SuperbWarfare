package com.atsuishio.superbwarfare.js;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

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

    public record CustomScript(String name, Context context, ScriptableObject scope, Script script) {
        public Object exec() {
            return script.exec(context, scope);
        }

        public void putProperty(String name, Object value) {
            ScriptableObject.putProperty(scope, name, value);
        }

        public void putConstant(String name, Object value) {
            ScriptableObject.putConstProperty(scope, name, value);
        }
    }

    public static @Nullable ScriptManager.CustomScript createSafeScript(String name, String source) {
        var ctx = getContext();
        if (!ctx.stringIsCompilableUnit(source)) return null;

        var scope = ctx.initSafeStandardObjects();
        var script = ctx.compileString(source, name, 1, null);
        return new CustomScript(name, ctx, scope, script);
    }

    public static CustomScript createScript(String name, String source) {
        var ctx = getContext();
        if (!ctx.stringIsCompilableUnit(source)) return null;

        var scope = ctx.initStandardObjects();
        var script = ctx.compileString(source, name, 1, null);
        return new CustomScript(name, ctx, scope, script);
    }
}
