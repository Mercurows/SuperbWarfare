package com.atsuishio.superbwarfare.data.lang_executor.js;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class JsExecutor {
    //        此处设置映射
    private final Object[][] kv = {
            {"key1", 123},
            {"key2", "abc"},
            {"$pow", -1},
    };

    private final Object[][] staticObjectMap = {
            {"Test", testObj}
    };

    public static Object testObj = new TestObject();

    public Object execute(String js) {
        var ctx = Context.enter();
        ScriptableObject scope = ctx.initStandardObjects();

        for (var item : kv) {
            ScriptableObject.putConstProperty(scope, item[0].toString(), item[1]);
        }
//        执行 js 期间确定对象的访问值
        StringBuilder serializeObjects = new StringBuilder();
        for (var item : staticObjectMap) {
            serializeObjects.append("const ").append(item[0]).append(" = ").append(((JsVisitableObject) item[1]).toJSON()).append(";\n");
        }
        String template = "(function() {" + serializeObjects + "return " + js + "})()";

        try (ctx) {
            return ctx.evaluateString(scope, template, "<cmd>", 1, null);
        }
    }

    public static void main(String[] args) {
        System.out.println(
//                类静态成员 + 方法 + 常量替换
                new JsExecutor().execute("Test.TestStatic + com.atsuishio.superbwarfare.data.lang_executor.js.TestObject.aStaticMethod() + $pow")
        );
    }
}
