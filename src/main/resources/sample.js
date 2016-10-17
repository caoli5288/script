var description = {
    "name":"Hello",
    "version":"1.0",
    "author":"mengcraft.com",
    "handle":"playerjoinevent"
}

var handle = function(event) {
    var p = event.player
    p.sendMessage("hello, " + p.name)
}