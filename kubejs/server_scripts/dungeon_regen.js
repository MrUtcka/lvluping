const DUNGEON_CONFIG_PATH = 'kubejs/config/dungeon_regen.json'

function dungeonDefaultConfig() {
  return {
    tickInterval: 20,
    debug: false,
    peacefulBypass: {
      enabled: false,
      dungeonDifficulty: 'easy',
      fallbackDifficulty: 'peaceful'
    },
    lootPools: {
      common: [{ id: 'minecraft:bread', min: 1, max: 3, weight: 2, chanceWeight: 8 }]
    },
    mobLootPools: {
      basic: [{ id: 'minecraft:rotten_flesh', min: 1, max: 2, weight: 1, chanceWeight: 10 }]
    },
    chests: [],
    resources: [],
    mobZones: [],
    barriers: []
  }
}

function dungeonLoadConfig() {
  var data = null
  try {
    data = JsonIO.read(DUNGEON_CONFIG_PATH)
  } catch (e) {
    console.error('[dungeon_regen] Invalid JSON in ' + DUNGEON_CONFIG_PATH + ': ' + e)
    return dungeonDefaultConfig()
  }
  if (!data) return dungeonDefaultConfig()
  if (data.tickInterval == null) data.tickInterval = 20
  if (data.debug == null) data.debug = false
  if (!data.peacefulBypass) data.peacefulBypass = { enabled: false, dungeonDifficulty: 'easy', fallbackDifficulty: 'peaceful' }
  if (data.peacefulBypass.enabled == null) data.peacefulBypass.enabled = false
  if (!data.peacefulBypass.dungeonDifficulty) data.peacefulBypass.dungeonDifficulty = 'easy'
  if (!data.peacefulBypass.fallbackDifficulty) data.peacefulBypass.fallbackDifficulty = 'peaceful'
  if (!data.lootPools) data.lootPools = {}
  if (!data.mobLootPools) data.mobLootPools = {}
  if (!data.chests) data.chests = []
  if (!data.resources) data.resources = []
  if (!data.mobZones) data.mobZones = []
  if (!data.barriers) data.barriers = []
  return data
}

var dungeonConfig = dungeonLoadConfig()
var dungeonRuntime = {
  chestTimers: [],
  resourceTimers: [],
  mobTimers: [],
  zoneAliveWeight: {},
  peacefulOverrideActive: false
}

function secToTicks(seconds) {
  var s = Number(seconds)
  if (!isFinite(s) || s <= 0) s = 60
  return Math.max(1, Math.floor(s * 20))
}

function rndInt(min, max) {
  var a = Math.floor(Number(min))
  var b = Math.floor(Number(max))
  if (!isFinite(a)) a = 0
  if (!isFinite(b)) b = a
  if (b < a) {
    var t = a
    a = b
    b = t
  }
  return a + Math.floor(Math.random() * (b - a + 1))
}

function chanceWeight(entry) {
  var w = Number(entry.chanceWeight != null ? entry.chanceWeight : entry.rollWeight)
  if (!isFinite(w) || w <= 0) w = 1
  return w
}

function loadWeight(entry) {
  var w = Number(entry.weight)
  if (!isFinite(w) || w <= 0) w = 1
  return w
}

function weightedPick(entries) {
  var i
  var total = 0
  var w
  for (i = 0; i < entries.length; i++) {
    w = chanceWeight(entries[i])
    total += w
  }
  if (total <= 0) return entries[0]
  var roll = Math.random() * total
  var acc = 0
  for (i = 0; i < entries.length; i++) {
    w = chanceWeight(entries[i])
    acc += w
    if (roll <= acc) return entries[i]
  }
  return entries[entries.length - 1]
}

function pickListByMaxWeight(sourceList, maxWeight, maxPicks) {
  var chosen = []
  var currentWeight = 0
  var picksLeft = Math.max(1, Math.floor(Number(maxPicks) || 128))
  var safeMaxWeight = Math.max(1, Math.floor(Number(maxWeight) || 1))
  var i
  var candidates
  var one
  while (picksLeft > 0 && currentWeight < safeMaxWeight) {
    candidates = []
    for (i = 0; i < sourceList.length; i++) {
      if (currentWeight + loadWeight(sourceList[i]) <= safeMaxWeight) candidates.push(sourceList[i])
    }
    if (candidates.length === 0) break
    one = weightedPick(candidates)
    chosen.push(one)
    currentWeight += loadWeight(one)
    picksLeft--
  }
  return chosen
}

function dimId(level) {
  try {
    if (level.dimension && level.dimension.location) return String(level.dimension.location())
  } catch (e) {}
  return 'minecraft:overworld'
}

function getServerLevel(server, dimension) {
  if (!server || !dimension) return null
  var levels = server.levels
  var n = levels.length
  var i
  var lv
  if (n == null || n === 0) {
    try {
      if (levels.size) n = levels.size()
    } catch (e0) {}
  }
  for (i = 0; i < n; i++) {
    lv = levels[i]
    if (lv == null && levels.get) {
      try {
        lv = levels.get(i)
      } catch (e1) {}
    }
    if (lv != null && dimId(lv) === String(dimension)) return lv
  }
  return null
}

function blockPosString(pos) {
  return Math.floor(Number(pos.x)) + ' ' + Math.floor(Number(pos.y)) + ' ' + Math.floor(Number(pos.z))
}

function runInDim(server, dimension, commandTail) {
  if (!server || !server.runCommandSilent) return
  server.runCommandSilent('execute in ' + dimension + ' run ' + commandTail)
}

function clearChest(server, dim, pos, size) {
  var slots = Math.max(1, Math.floor(Number(size) || 27))
  var i
  for (i = 0; i < slots; i++) {
    runInDim(server, dim, 'item replace block ' + blockPosString(pos) + ' container.' + i + ' with minecraft:air')
  }
}

function fillChestFromPool(server, chestCfg) {
  var pool = dungeonConfig.lootPools[chestCfg.pool]
  if (!pool || pool.length === 0) return
  var slots = Math.max(1, Math.floor(Number(chestCfg.slots) || 27))
  var maxWeight = Math.max(1, Math.floor(Number(chestCfg.maxWeight) || 10))
  var maxPicks = Math.max(1, Math.floor(Number(chestCfg.maxPicks) || slots))
  var used = {}
  var chosen = pickListByMaxWeight(pool, maxWeight, maxPicks)
  var i
  var slot
  var tries
  var pick
  var cnt
  clearChest(server, chestCfg.dimension, chestCfg.pos, slots)
  for (i = 0; i < chosen.length; i++) {
    tries = 0
    do {
      slot = rndInt(0, slots - 1)
      tries++
    } while (used[slot] === true && tries < 40)
    used[slot] = true
    pick = chosen[i]
    cnt = rndInt(pick.min != null ? pick.min : 1, pick.max != null ? pick.max : (pick.min != null ? pick.min : 1))
    runInDim(
      server,
      chestCfg.dimension,
      'item replace block ' +
        blockPosString(chestCfg.pos) +
        ' container.' +
        slot +
        ' with ' +
        String(pick.id) +
        ' ' +
        cnt
    )
  }
}

function refillResourceNode(server, nodeCfg) {
  runInDim(server, nodeCfg.dimension, 'setblock ' + blockPosString(nodeCfg.pos) + ' ' + String(nodeCfg.block) + ' replace')
}

function zoneTag(zone) {
  return 'dungeon_zone_' + String(zone.id).toLowerCase().replace(/[^a-z0-9_]/g, '_')
}

function zoneKey(id) {
  return String(id).toLowerCase().replace(/[^a-z0-9_]/g, '_')
}

function zoneWeightTag(zone) {
  return 'dungeon_zone_weight_' + zoneKey(zone.id)
}

function mobWeightTag(weight) {
  return 'dungeon_mob_weight_' + Math.max(1, Math.floor(Number(weight) || 1))
}

function mobLootTag(poolId) {
  return 'dungeon_mob_loot_' + String(poolId).toLowerCase().replace(/[^a-z0-9_]/g, '_')
}

function mobLootMaxTag(v) {
  return 'dungeon_mob_loot_max_' + Math.max(1, Math.floor(Number(v) || 1))
}

function mobLootPicksTag(v) {
  return 'dungeon_mob_loot_picks_' + Math.max(1, Math.floor(Number(v) || 1))
}

function zoneAABB(zone) {
  var minX = Math.floor(Number(zone.bounds.min.x))
  var minY = Math.floor(Number(zone.bounds.min.y))
  var minZ = Math.floor(Number(zone.bounds.min.z))
  var maxX = Math.floor(Number(zone.bounds.max.x))
  var maxY = Math.floor(Number(zone.bounds.max.y))
  var maxZ = Math.floor(Number(zone.bounds.max.z))
  if (maxX < minX) {
    var tx = minX
    minX = maxX
    maxX = tx
  }
  if (maxY < minY) {
    var ty = minY
    minY = maxY
    maxY = ty
  }
  if (maxZ < minZ) {
    var tz = minZ
    minZ = maxZ
    maxZ = tz
  }
  return {
    minX: minX,
    minY: minY,
    minZ: minZ,
    maxX: maxX,
    maxY: maxY,
    maxZ: maxZ,
    dx: maxX - minX,
    dy: maxY - minY,
    dz: maxZ - minZ
  }
}

function pointInsideAABB(x, y, z, box) {
  return x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY && z >= box.minZ && z <= box.maxZ
}

function anyPlayerInsideDungeonZone(server) {
  if (!server) return false
  var zones = dungeonConfig.mobZones || []
  if (zones.length === 0) return false
  var players = server.players
  var pi
  var zi
  var p
  var z
  var box
  for (pi = 0; pi < players.length; pi++) {
    p = players[pi]
    if (!p || p.fake) continue
    for (zi = 0; zi < zones.length; zi++) {
      z = zones[zi]
      if (String(dimId(p.level)) !== String(z.dimension)) continue
      box = zoneAABB(z)
      if (pointInsideAABB(Number(p.x), Number(p.y), Number(p.z), box)) return true
    }
  }
  return false
}

function applyPeacefulBypass(server) {
  var cfg = dungeonConfig.peacefulBypass || {}
  if (cfg.enabled !== true) return
  var shouldEnableDungeonDifficulty = anyPlayerInsideDungeonZone(server)
  if (shouldEnableDungeonDifficulty && !dungeonRuntime.peacefulOverrideActive) {
    server.runCommandSilent('difficulty ' + String(cfg.dungeonDifficulty))
    dungeonRuntime.peacefulOverrideActive = true
    if (dungeonConfig.debug) console.info('[dungeon_regen] difficulty -> ' + String(cfg.dungeonDifficulty))
  } else if (!shouldEnableDungeonDifficulty && dungeonRuntime.peacefulOverrideActive) {
    server.runCommandSilent('difficulty ' + String(cfg.fallbackDifficulty))
    dungeonRuntime.peacefulOverrideActive = false
    if (dungeonConfig.debug) console.info('[dungeon_regen] difficulty -> ' + String(cfg.fallbackDifficulty))
  }
}

function spawnZoneMobs(server, zoneCfg) {
  var mobs = zoneCfg.mobs || []
  if (mobs.length === 0) return
  var maxZoneWeight = Math.max(1, Math.floor(Number(zoneCfg.maxSpawnWeight) || 10))
  var currentZoneWeight = Number(dungeonRuntime.zoneAliveWeight[zoneKey(zoneCfg.id)] || 0)
  if (!isFinite(currentZoneWeight) || currentZoneWeight < 0) currentZoneWeight = 0
  var spawnBudget = maxZoneWeight - currentZoneWeight
  if (spawnBudget <= 0) return
  var maxPicks = Math.max(1, Math.floor(Number(zoneCfg.maxSpawnPicks) || 16))
  var chosen = pickListByMaxWeight(mobs, spawnBudget, maxPicks)
  var box = zoneAABB(zoneCfg)
  var i
  var pick
  var x
  var y
  var z
  var tag = zoneTag(zoneCfg)
  var zTag = zoneWeightTag(zoneCfg)
  var lootPool = zoneCfg.mobLootPool ? String(zoneCfg.mobLootPool) : ''
  var lootMaxWeight = Math.max(1, Math.floor(Number(zoneCfg.mobLootMaxWeight) || 4))
  var lootMaxPicks = Math.max(1, Math.floor(Number(zoneCfg.mobLootMaxPicks) || 8))
  for (i = 0; i < chosen.length; i++) {
    pick = chosen[i]
    x = rndInt(box.minX, box.maxX) + 0.5
    y = rndInt(box.minY, box.maxY)
    z = rndInt(box.minZ, box.maxZ) + 0.5
    var oneWeight = loadWeight(pick)
    var tagsNbt = '["' + tag + '","' + zTag + '","' + mobWeightTag(oneWeight) + '"'
    if (lootPool.length > 0) {
      tagsNbt +=
        ',"' + mobLootTag(lootPool) + '","' + mobLootMaxTag(lootMaxWeight) + '","' + mobLootPicksTag(lootMaxPicks) + '"'
    }
    tagsNbt += ']'
    runInDim(
      server,
      zoneCfg.dimension,
      'summon ' +
        String(pick.id) +
        ' ' +
        x +
        ' ' +
        y +
        ' ' +
        z +
        ' {PersistenceRequired:1b,DeathLootTable:"minecraft:empty",Tags:' +
        tagsNbt +
        '}'
    )
    currentZoneWeight += oneWeight
  }
  dungeonRuntime.zoneAliveWeight[zoneKey(zoneCfg.id)] = currentZoneWeight
}

function keepZoneMobsInside(server, zoneCfg) {
  var box = zoneAABB(zoneCfg)
  var centerX = Math.floor((box.minX + box.maxX) / 2) + 0.5
  var centerY = box.minY + 1
  var centerZ = Math.floor((box.minZ + box.maxZ) / 2) + 0.5
  var tag = zoneTag(zoneCfg)
  runInDim(
    server,
    zoneCfg.dimension,
    'execute as @e[tag=' +
      tag +
      '] at @s unless entity @s[x=' +
      box.minX +
      ',y=' +
      box.minY +
      ',z=' +
      box.minZ +
      ',dx=' +
      box.dx +
      ',dy=' +
      box.dy +
      ',dz=' +
      box.dz +
      '] run tp @s ' +
      centerX +
      ' ' +
      centerY +
      ' ' +
      centerZ
  )
}

function placeBarriers(server) {
  var barriers = dungeonConfig.barriers || []
  var i
  var b
  var x
  var y
  var z
  var blockId
  for (i = 0; i < barriers.length; i++) {
    b = barriers[i]
    // By default we do not place physical walls: they block players too.
    // Mob locking is handled by keepZoneMobsInside() in mobZones.
    if (b.enablePhysicalWall !== true) continue
    blockId = b.block ? String(b.block) : 'minecraft:barrier'
    for (x = Math.min(b.from.x, b.to.x); x <= Math.max(b.from.x, b.to.x); x++) {
      for (y = Math.min(b.from.y, b.to.y); y <= Math.max(b.from.y, b.to.y); y++) {
        for (z = Math.min(b.from.z, b.to.z); z <= Math.max(b.from.z, b.to.z); z++) {
          runInDim(server, b.dimension, 'setblock ' + x + ' ' + y + ' ' + z + ' ' + blockId + ' replace')
        }
      }
    }
  }
}

function resetDungeonRuntime() {
  var i
  dungeonRuntime.chestTimers = []
  dungeonRuntime.resourceTimers = []
  dungeonRuntime.mobTimers = []
  dungeonRuntime.zoneAliveWeight = {}
  dungeonRuntime.peacefulOverrideActive = false
  for (i = 0; i < dungeonConfig.chests.length; i++) {
    dungeonRuntime.chestTimers.push({ cfg: dungeonConfig.chests[i], timer: secToTicks(dungeonConfig.chests[i].respawnSeconds) })
  }
  for (i = 0; i < dungeonConfig.resources.length; i++) {
    dungeonRuntime.resourceTimers.push({ cfg: dungeonConfig.resources[i], timer: secToTicks(dungeonConfig.resources[i].respawnSeconds) })
  }
  for (i = 0; i < dungeonConfig.mobZones.length; i++) {
    dungeonRuntime.mobTimers.push({ cfg: dungeonConfig.mobZones[i], timer: secToTicks(dungeonConfig.mobZones[i].respawnSeconds) })
    dungeonRuntime.zoneAliveWeight[zoneKey(dungeonConfig.mobZones[i].id)] = 0
  }
}

function parseTagValue(tags, prefix) {
  var i
  var t
  for (i = 0; i < tags.length; i++) {
    t = String(tags[i])
    if (t.indexOf(prefix) === 0) return t.substring(prefix.length)
  }
  return ''
}

function dropCustomMobLoot(event, poolId, lootMaxWeight, lootMaxPicks) {
  var pool = dungeonConfig.mobLootPools[poolId]
  if (!pool || pool.length === 0) return
  var maxWeight = Math.max(1, Math.floor(Number(lootMaxWeight) || 4))
  var picks = pickListByMaxWeight(pool, maxWeight, Math.max(1, Math.floor(Number(lootMaxPicks) || 8)))
  var i
  var p
  var cnt
  var x = Number(event.entity.x) + 0.5
  var y = Number(event.entity.y) + 0.2
  var z = Number(event.entity.z) + 0.5
  for (i = 0; i < picks.length; i++) {
    p = picks[i]
    cnt = rndInt(p.min != null ? p.min : 1, p.max != null ? p.max : (p.min != null ? p.min : 1))
    runInDim(
      event.server,
      dimId(event.level),
      'summon minecraft:item ' + x + ' ' + y + ' ' + z + ' {Item:{id:"' + String(p.id) + '",count:' + cnt + 'b}}'
    )
  }
}

EntityEvents.death(function (event) {
  var entity = event.entity
  if (!entity || !entity.tags) return
  var tags = entity.tags.toArray ? entity.tags.toArray() : entity.tags
  var zoneName = parseTagValue(tags, 'dungeon_zone_weight_')
  var mobWeightRaw = parseTagValue(tags, 'dungeon_mob_weight_')
  var lootPool = parseTagValue(tags, 'dungeon_mob_loot_')
  var lootMaxWeight = parseTagValue(tags, 'dungeon_mob_loot_max_')
  var lootMaxPicks = parseTagValue(tags, 'dungeon_mob_loot_picks_')
  if (zoneName.length > 0 && mobWeightRaw.length > 0) {
    var alive = Number(dungeonRuntime.zoneAliveWeight[zoneName] || 0)
    var oneWeight = Math.max(1, Math.floor(Number(mobWeightRaw) || 1))
    alive -= oneWeight
    if (!isFinite(alive) || alive < 0) alive = 0
    dungeonRuntime.zoneAliveWeight[zoneName] = alive
  }
  if (lootPool.length > 0) {
    dropCustomMobLoot(event, lootPool, lootMaxWeight, lootMaxPicks)
  }
})

ServerEvents.loaded(function (event) {
  dungeonConfig = dungeonLoadConfig()
  resetDungeonRuntime()
  placeBarriers(event.server)
  console.info(
    '[dungeon_regen] Loaded. chests=' +
      dungeonConfig.chests.length +
      ', resources=' +
      dungeonConfig.resources.length +
      ', zones=' +
      dungeonConfig.mobZones.length
  )
})

ServerEvents.tick(function (event) {
  var tickStep = Math.max(1, Math.floor(Number(dungeonConfig.tickInterval) || 20))
  if (event.server.tickCount % tickStep !== 0) return

  applyPeacefulBypass(event.server)

  var i
  var job
  for (i = 0; i < dungeonRuntime.chestTimers.length; i++) {
    job = dungeonRuntime.chestTimers[i]
    job.timer -= tickStep
    if (job.timer <= 0) {
      fillChestFromPool(event.server, job.cfg)
      job.timer = secToTicks(job.cfg.respawnSeconds)
    }
  }

  for (i = 0; i < dungeonRuntime.resourceTimers.length; i++) {
    job = dungeonRuntime.resourceTimers[i]
    job.timer -= tickStep
    if (job.timer <= 0) {
      refillResourceNode(event.server, job.cfg)
      job.timer = secToTicks(job.cfg.respawnSeconds)
    }
  }

  for (i = 0; i < dungeonRuntime.mobTimers.length; i++) {
    job = dungeonRuntime.mobTimers[i]
    keepZoneMobsInside(event.server, job.cfg)
    job.timer -= tickStep
    if (job.timer <= 0) {
      spawnZoneMobs(event.server, job.cfg)
      job.timer = secToTicks(job.cfg.respawnSeconds)
    }
  }
})
