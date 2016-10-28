package com.mengcraft.script.util;

import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Created on 16-10-28.
 */
public class ArrayHelperTest {

    @Test
    public void toJSArray() throws Exception {
        String[] input = {"a", "b", "c"};
        Object arr = ArrayHelper.toScriptArray(input);
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        engine.put("arr", arr);
        engine.eval("" +
                "print(arr.join(\" \"));\n" +
                "i = [\"d\", \"e\", \"f\"];\n" +
                "arr.push.apply(arr, i);\n" +
                "print(arr.join(\" \"));");
    }

}