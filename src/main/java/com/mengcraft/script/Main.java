package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptPluginException;
import com.mengcraft.script.util.ArrayHelper;
import lombok.SneakyThrows;
import lombok.experimental.var;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created on 16-10-17.
 */
public final class Main extends JavaPlugin {

    private final Map<String, HandledExecutor> executor = new HashMap<>();
    private Map<String, ScriptLoader.ScriptBinding> plugin;
    private ScriptLoader loader;

    @Override
    public void onEnable() {
        loader = new ScriptLoader(this);

        getServer().getConsoleSender().sendMessage(ArrayHelper.toArray(
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"));

        int init = EventMapping.INSTANCE.init();// Register build-in event
        getLogger().info("Initialized " + init + " build-in event(s)");

        getCommand("script").setExecutor(new MainCommand(this, executor));

        saveDefaultConfig();
        load();
    }

    @Override
    public void onDisable() {
        for (Map.Entry<String, ScriptLoader.ScriptBinding> i : new HashMap<>(plugin).entrySet()) {
            i.getValue().getPlugin().unload();
        }
        plugin = null;
    }

    protected void reload() {
        onDisable();
        load();
    }

    private void load() {
        plugin = new HashMap<>();
        for (String l : getDataFolder().list()) {
            val i = new File(getDataFolder(), l);
            if (i.isFile() && l.matches(".+\\.js")) {
                try {
                    load(getServer().getConsoleSender(), l, null);
                } catch (ScriptPluginException e) {
                    getLogger().log(Level.WARNING, e.getMessage());
                }
            }
        }
    }

    @SneakyThrows
    protected void load(CommandSender loader, String path, Object arg) throws ScriptPluginException {
        File loadable = new File(getDataFolder(), path);
        thr(!loadable.isFile() || !isLoaded(loadable), "path not loadable");

        load(ScriptLoader.ScriptInfo.builder()
                .loader(loader)
                .id("file:" + path)
                .contend(new FileReader(loadable))
                .arg(arg).build());
    }

    private void load(ScriptLoader.ScriptInfo info) throws ScriptPluginException {
        ScriptLoader.ScriptBinding binding = loader.load(info);
        ScriptPlugin loaded = binding.getPlugin();
        if (loaded.isHandled() && !loaded.isIdled()) {
            String name = loaded.getDescription("name");
            ScriptLoader.ScriptBinding i = plugin.get(name);
            if (!nil(i)) {
                ScriptPluginException.thr(loaded, "Name conflict with " + i.getPlugin());
            }
            plugin.put(name, binding);
        }
    }

    private boolean isLoaded(File file) {
        String id = "file:" + file.getName();
        return !nil(lookById(id));
    }

    public ImmutableList<String> list() {
        return ImmutableList.copyOf(plugin.keySet());
    }

    @SuppressWarnings("unchecked")
    protected void addExecutor(HandledExecutor handled) {
        String label = handled.getLabel();
        Preconditions.checkState(!executor.containsKey(label));
        try {
            Field field = SimplePluginManager.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            SimpleCommandMap i = SimpleCommandMap.class.cast(field.get(getServer().getPluginManager()));
            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            Map handler = Map.class.cast(f.get(i));
            PluginCommand command = getCommand("script");
            handler.putIfAbsent(label, command);
            handler.putIfAbsent("script:" + label, command);
            executor.put(label, handled);
            executor.put("script:" + label, handled);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @SneakyThrows
    protected boolean remove(HandledExecutor handled) {
        boolean b = executor.containsKey(handled.getLabel());
        if (b) {
            String label = handled.getLabel();
            Field field = SimplePluginManager.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            SimpleCommandMap i = SimpleCommandMap.class.cast(field.get(getServer().getPluginManager()));
            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            Map handler = Map.class.cast(f.get(i));
            PluginCommand command = getCommand("script");
            handler.remove(label, command);
            handler.remove("script:" + label, command);
            executor.remove(label);
            executor.remove("script:" + label);
        }
        return b;
    }

    boolean unload(ScriptPlugin i) {
        String id = i.getDescription("name");
        if (nil(id)) return false;
        ScriptLoader.ScriptBinding binding = plugin.get(id);
        return !nil(binding) && binding.getPlugin() == i && plugin.remove(id, binding);
    }

    boolean unload(String id) {
        val binding = getSBinding(id);
        return !nil(binding) && binding.getPlugin().unload();
    }

    ScriptLoader.ScriptBinding lookById(String id) {
        for (ScriptLoader.ScriptBinding i : plugin.values()) {
            if (i.toString().equals(id)) return i;
        }
        return null;
    }

    public ScriptLoader.ScriptBinding getSBinding(String name) {
        var binding = plugin.get(name);
        if (nil(binding) && name.startsWith("file:")) {
            binding = lookById(name);
        }
        return binding;
    }

    public static void thr(boolean b, String message) {
        if (b) throw new IllegalStateException(message);
    }

    public static boolean nil(Object i) {
        return i == null;
    }

}
