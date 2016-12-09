var description = {
    "author":"mengcraft.com",
    "name":"AntiAutoRoad",
    "version":"1.0",
}

var last = {}

var validPlayer = function (p) {
    if (!p.sneaking) {
        var time = last[p.name]
        return (time && now - time < 1200)
    }
    return false
}

var equal = function (i, j) {
    return i.x == j.x && i.y == j.y && i.z == j.z
}

var validPlace = function (i, placed) {
    var b = i.clone().add(0, -1, 0).block
    if (b.typeId > 0 && i.clone().add(0, -3, 0).block.typeId == 0) {
        var stand = b.location
        if (equal(stand, placed)) {
            return placed.clone().add(0, -1, 0).block.typeId == 0;
        }
    }
    return false
}

var watch = {}

plugin.addListener("blockplaceevent", function (event) {
    if (event.cancelled) return
    now = java.lang.System.currentTimeMillis()
    var p = event.player;
    if (validPlayer(p) && validPlace(p.location, event.block.location)) {
        var count = watch[p.name]
        if (!count) count = 0
        if (++count > 2) {
            p.kickPlayer("检测到使用自动搭路工具")
        } else {
            watch[p.name] = count
        }
    } else {
        last[p.name] = now
    }
})
