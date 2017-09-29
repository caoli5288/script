# Script
Load JavaScript plugin in bukkit server. 

## 示例
一个最简单的有功能的脚本如下例，它将给登陆的玩家发送信息。`description`对象定义了脚本的基本属性（非必须）。对象中的`handle`字段定义了脚本监听的事件。
```JS
var description = {
    "author":"mengcraft.com",
    "name":"Hello",
    "version":"1.0",
    "handle":"playerjoinevent"
}

var handle = function handle(event) {
    var p = event.player
    p.sendMessage("hello, " + p.name)
}
```

### 加载
插件在启动时自动加载插件根目录下文件名匹配`*.js`的脚本，不匹配或位于子目录的脚本请使用指令`/script load <文件名>`加载。脚本文件名后的字符作为全局变量`arg`传入脚本中，类型为`string[]`。自动加载的脚本`arg`始终为未定义。
```JS
if (arg) {
    loader.sendMessage(arg.join(" "));
}
```

### 单次脚本
如果脚本没有添加指令、事件监听和任务调度等行为，那么该脚本为单次脚本。单次脚本执行后不驻留，因此可以通过指令反复加载。

### 事件
这里有另一种更加灵活的方式监听一个事件。你可以随时添加或者去除一个或者多个监听器。
```JS
var description = {
    "author":"mengcraft.com",
    "name":"Hello",
    "version":"1.0",
}

var listener = plugin.addListener("playerjoinevent", function(event) {...})

listener.remove()
```

对于第三方插件中的事件，需要先注册到脚本插件中才能使用。
```JS
plugin.mapping.init("AnyPlugin")
```

如果类加载器未把事件类加载到内存中，可能需要使用类加载器加载。
```JS
plugin.mapping.init(java.lang.Class.forName("com.ext.plugin.AnyEvent"))
```

请注意脚本处理事件继承与插件有所区别。例如，插件中监听`EntityDamageEvent`会将该类的子类一并监听，而脚本的监听器对此做了额外处理以确保不会监听到子类。

### 指令
你可以添加一个或者多个指令。以下是添加`/echo`和`/script:echo`指令的示例代码。函数的第一个参数是指令执行者，第二个参数是指令参数，类型为`Array`。
```JS
plugin.addExecutor("echo", function(sender, i) {
    if (Array.isArray(i)) sender.sendMessage(i.join(" "))
})
```

可以直接指定执行指令所需的权限。
```JS
plugin.addExecutor("echo", "echo.use", function(sender, i) {...})
```

理所当然的，添加的指令随时可以撤销。
```JS
var exec = plugin.addExecutor(...)
exec.remove()
```

### 服务
加载服务是可能的，并且比Java插件中的做法更为简单。但是你无法在脚本中提供一个服务。
```JS
var economy = plugin.getService("Economy")
var p = plugin.getPlayer("him")
if (economy && p) {
    economy.depositPlayer(p, 100)
    p.sendMessage("you receive 100 dollar")
    plugin.playerList.forEach(function(player) {
        player.sendMessage("lucky! " + p.name + " receive 100 dollar");
    })
}
```

### 广播
广播消息将发送给当前在线的所有玩家。消息支持任意多行，默认将替换`&`为颜色代码。
```JS
plugin.broadcast("&1这是第一行消息",
    "&2这是第二行消息",
    "&3这是第三行消息")
```

### 插件依赖
脚本加载的时机之于服务器插件加载的时机是不确定的。有些函数的调用必须确保在一些插件加载之后调用，否则可能得到意料之外的结果。
```JS
plugin.depend("Vault", function () {
    var eco = plugin.getService("Economy");
    plugin.addListener("playerjoinevent", function () {
        eco.depositPlayer(p, 100);
    })
})
```

在这个用例中，如果`Vault`插件已经加载那么回调函数将立即执行，否则将推迟到下tick执行，该行为视同任务调度。如果插件仍未加载，回调函数将被抛弃并在控制台输出一个警告信息。

也可以同时依赖多个插件。
```JS
plugin.depend(["Vault", "AnyPlugin"], function () {...});
```

你可以给该行为添加一个失败时执行的回调函数。
```JS
plugin.depend("Vault", function () {...})
    .onFail(function () {
        plugin.logger.info("Vault NOT found!");
        plugin.unload();
    });
```

### 交互
与其他脚本或者插件进行交互是不安全的，但你仍可以在必要时这么做。使用时请注意脚本或插件加载顺序，必要时使用`plugin.depend`函数。
```
var p = plugin.unsafe.getPlugin("PlayerPoints").getApi()
p.give(plugin.getPlayer("Him"), 100)

plugin.unsafe.getScript("other_script").plugin.unload()
```

### 任务调度
尝试在脚本加载6000tick后卸载脚本，你可以随时在任务未执行前取消它。
```JS
var task = plugin.runTask(function() {...}, 6000)
task.cancel()
```

或者调度一个在下tick执行，并且每1800tick循环执行的任务。
```JS
plugin.runTask(function() plugin.logger.info("hello, world"), 1, 1800)
```

或者在下tick执行一个任务。
```JS
plugin.runTask(function() plugin.logger.info("hello, world"))
```

异步的任务调度都是可行的，只需要在参数后传入`true`标识。
```JS
plugin.runTask(function() {...}, true)
plugin.runTask(function() {...}, 1, true)
plugin.runTask(function() {...}, 1, 1800, true)
```

### 后台指令
```JS
plugin.runCommand("kill him");
```

`loader`为脚本加载者。如果脚本是通过指令加载的，那么`loader`为指令输入者，其他情况为为控制台。

### 引用其他插件类
一些服务端架构方面的限制导致脚本中无法直接使用内置方法引用其他插件类，此时请使用这个方法获取。
```JS
var viaversion = plugin.loadType("us.myles.ViaVersion.api.Via");
var api = viaversion.static.getAPI();// 静态方法
```

### 卸载
脚本卸载时将移除所有的事件监听、指令和未完成的任务，请谨慎操作。
```JS
plugin.unload();
```

如果你定义了卸载钩子，卸载钩子将在脚本卸载完成时被执行。钩子中无法执行大部分`plugin`接口调用。
```
plugin.setUnloadHook(function() {...});
plugin.setUnloadHook(null);
```

### Placeholder
如果服务器安装有PlaceholderAPI，那么可以很方便的注册placeholder。
```
val hook = plugin.addPlaceholder("sp", function (p, input) {
    // sp_any_world -> any|world
    return input[0] + "|" + input[1];
});
hook.remove();// remove it
```

### 插件指令
执行下列权限需要`script.admin`权限。
- /script list
    - 列出所有已加载的脚本名。
- /script load <文件名>
    - 加载指定文件名的脚本。文件名以插件数据目录为相对目录。
- /script unload <脚本名|file:文件名>
    - 卸载指定名的脚本。脚本名如未在`description`字段定义则为`file:文件名`。
    - 亦可直接使用`file:文件名`。
- /script reload
    - 所有已加载的脚本将被卸载，然后加载所有定义在配置中`script`列的脚本。

### 注意
初次接触js脚本的童鞋注意，脚本可以正确调用`Java`中的重载方法，但是脚本无法声明重载函数，请务必注意。
