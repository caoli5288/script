package com.mengcraft.script;

import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptPluginException;

public interface IScriptSpi {

    ScriptLoader.ScriptBinding loadScript(ScriptLoader.ScriptInfo info) throws ScriptPluginException;

    ScriptLoader.ScriptBinding getScript(String name);
}
