# Script
Load script in bukkit server. 

## 示例
一个最简单的有功能的脚本如下例，它将给登陆的玩家发送信息。`description`对象定义了脚本的基本属性（非必须）。
对象中的`handle`字段定义了脚本监听的事件。
```JS
var description = {
    "author":"mengcraft.com",
    "name":"Hello",
    "version":"1.0",
    "handle":"playerjoinevent"
}

var handle = function(event) {
    var p = event.player
    p.sendMessage("hello, " + p.name)
}

```

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

### 服务
加载服务是可能的，并且比Java插件中的做法更为简单。你无法在脚本中提供一个服务。
```JS
var economy = plugin.getService("Economy")
var p = plugin.getPlayer("him")
if (economy && p) {
    economy.depositPlayer(p, 100)
    p.sendMessage("you receive 100 dollar")
    plugin.onlineList.forEach(function(player) {
        player.sendMessage("lucky! " + p.name + " receive 100 dollar");
    })
}
```

### 任务调度
尝试在脚本加载6000tick后卸载脚本，你可以随时在任务未执行前取消它。
```JS
var task = plugin.schedule(function() {...}, 6000)

task.cancel()
```

或者调度一个在下tick执行，并且每1800tick循环执行的任务。
```JS
plugin.schedule(function() plugin.logger.info("hello, world"), 1, 1800)
```

或者在下tick执行一个任务。
```JS
plugin.schedule(function() plugin.logger.info("hello, world"))
```

异步的任务调度都是可行的，只需要在参数后传入`true`标识。
```JS
plugin.schedule(function() {...}, true)
plugin.schedule(function() {...}, 1, true)
plugin.schedule(function() {...}, 1, 1800, true)

```

### 卸载
脚本卸载时将移除所有的事件监听和未完成的任务，请谨慎操作。
```JS
plugin.unload();
```

同时，如果你定义了卸载钩子，卸载钩子将在脚本卸载完成时被执行。此时无法执行大部分接口调用。
```
plugin.setUnloadHook(function() {...})
plugin.setUnloadHook(null)
```

### 交互
与其他脚本或者插件进行交互是不安全的，但你仍可以在必要时这么做。使用时请注意脚本加载顺序。
```
var p = plugin.unsafe.getPlugin("PlayerPoints").getApi()
p.give(plugin.getPlayer("Him"), 100)

plugin.unsafe.getScript("other_script").plugin.unload()
```

### 注意
脚本可以正确调用`Java`中的重载方法，但是脚本本身无法定义重载函数，请务必注意这一点。