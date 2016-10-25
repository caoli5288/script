package com.mengcraft.script;

import junit.framework.Assert;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

/**
 * Created on 16-10-17.
 */
public class MainTest {

    @Test
    public void loadAndEval() {
        File file = new File("src/main/resources/hello.js");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        try {
            engine.eval(new FileReader(file));
            Object description = engine.get("description");
            Assert.assertNotNull(description);
            Assert.assertTrue(description instanceof Map);
        } catch (ScriptException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}