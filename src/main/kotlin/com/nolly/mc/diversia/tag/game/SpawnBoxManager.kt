package com.nolly.mc.diversia.tag.game

import com.nolly.mc.diversia.tag.config.TagConfig
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

class SpawnBoxManager(private val config: TagConfig) {
	private val savedBlocks = mutableMapOf<String, Map<Location, Material>>()

	private val BOX_OFFSETS: List<Triple<Int, Int, Int>> by lazy {
		val offsets = mutableListOf<Triple<Int, Int, Int>>()
		for (x in -1..1) for (y in 0..2) for (z in -1..1) {
			val isWall = x == -1 || x == 1 || z == -1 || z == 1 || y == 0 || y == 2
			if (isWall) offsets.add(Triple(x, y, z))
		}
		offsets
	}

	fun placeBox(key: String, spawnData: TagConfig.LocationData) {
		val world = Bukkit.getWorld(spawnData.world) ?: return
		val center = Location(world, spawnData.x, spawnData.y, spawnData.z)
		val saved = mutableMapOf<Location, Material>()
		for ((dx, dy, dz) in BOX_OFFSETS) {
			val loc = center.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble())
			saved[loc] = loc.block.type
			loc.block.type = Material.GLASS
		}
		savedBlocks[key] = saved
	}

	fun restoreBox(key: String) {
		savedBlocks.remove(key)?.forEach { (loc, mat) -> loc.block.type = mat }
	}

	fun restoreAll() {
		savedBlocks.keys.toList().forEach { restoreBox(it) }
	}
}
