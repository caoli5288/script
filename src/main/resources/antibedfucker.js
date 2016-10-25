var description = {
    "author":"mengcraft.com",
    "name":"AntiBedFucker",
    "version":"1.0",
}

plugin.addListener("blockbreakevent", function(event) {
    var b = event.block
    if (b.type == org.bukkit.Material.BED_BLOCK) {
        var p = event.player
        var target = p.getTargetBlock(null, 5)
        if (target == null || target.type != org.bukkit.Material.BED_BLOCK) {
            event.cancelled = true
        }
    }
})
