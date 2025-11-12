package com.atsuishio.superbwarfare.data.lang_executor.js;

public class TestObject implements JsVisitableObject {
    public static int TestStatic = 1;
    public int instanceInt = 2;

//    for js call
    public static int aStaticMethod() {
        return 114514;
    }

    @Override
    public String toJSON() {
        return "{ TestStatic: " + TestStatic + ", instanceInt: " + instanceInt + " }";
    }
}
