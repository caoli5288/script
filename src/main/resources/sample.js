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

// try load vault economy
var economy = plugin.getService("Economy")

if (economy) {
    plugin.schedule(function() {
        plugin.onlineList.forEach(function(i) {
            economy.depositPlayer(p, 100)
            p.sendMessage("you receive 100 dollar")
        })
    }, 0, 6000)
}

plugin.setUnloadHook(function() {
    plugin.logger.info("script unloaded")
})

plugin.addListener("playerquitevent", function(event) {
    plugin.onlineList.forEach(function(i) {
        i.sendMessage("bye bye, " + event.player.name)
    })
    // unload script and clean all handled listener and task
    // will run unload hook if exist
    plugin.unload()
})

plugin.logger.info("script enabled")