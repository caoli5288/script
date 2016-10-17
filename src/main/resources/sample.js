var description = {
    "name":"Hello",
    "version":"1.0",
    "author":"mengcraft.com",
    "handle":"playerjoinevent"
}

var handle = function(event) {
    var p = event.player
    p.sendMessage("hello, " + p.name)
    plugin.schedule(function() {
        p.sendMessage("bye bye")
        plugin.logger.info("unload...")
        plugin.unload()
    }, 40);
}
