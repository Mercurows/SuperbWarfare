package com.atsuishio.superbwarfare.data.lang_executor.js;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class JsExecutor {
    //        此处设置映射
    private final Object[][] kv = {
            {"key1", 123},
            {"key2", "abc"},
            {"$pow", -1},
            {"Test", testObj}
    };

    public static Object testObj = new TestObject();

    public Object execute(String js) {
        var ctx = Context.enter();
        ScriptableObject scope = ctx.initStandardObjects();

        for (var item : kv) {
            ScriptableObject.putConstProperty(scope, item[0].toString(), item[1]);
        }

        try (ctx) {
            return ctx.evaluateString(scope, js, "<cmd>", 1, null);
        }
    }

    public static void main(String[] args) {
        System.out.println(
//                类成员 + 软件包路径访问静态方法 + 对象方法 + 常量替换
                new JsExecutor().execute("Test.instanceInt + com.atsuishio.superbwarfare.data.lang_executor.js.TestObject.aStaticMethod() + Test.aObjectMethod() + $pow")
        );
    }
}
