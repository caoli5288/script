package com.mengcraft.script;

import java.util.HashMap;

/**
 * Created on 16-10-18.
 */
public final class ScriptDescription extends HashMap<String, String> {

    @Override
    public String put(String key, String value) {
        if (!containsKey(key)) {
            super.put(key, value);
        }
        return null;
    }

}
