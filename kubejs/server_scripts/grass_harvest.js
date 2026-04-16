// ПКМ по траве из списка: блок убирается, выдаётся предмет, через заданное время блок восстанавливается.
// Папку kubejs скопируйте в корень сервера (рядом с mods), установите KubeJS для NeoForge 1.21.1.
// Предметы (в т.ч. Executable Items для NeoForge): id из /kjs hand; несколько штук — массив items + itemPick.

const CONFIG_PATH = 'kubejs/config/grass_harvest.json'

/** Строковый id блока (KubeJS может отдавать ResourceLocation, не JS-строку). */
function kubeBlockId(block) {
  if (block == null) return ''
  var raw = block.id
  if (raw == null) return ''
  if (typeof raw === 'string') return raw
  try {
    if (raw.getNamespace && raw.getPath) {
      return raw.getNamespace() + ':' + raw.getPath()
    }
  } catch (e1) {}
  try {
    return String(raw.toString())
  } catch (e2) {}
  return String(raw)
}

function normalizeOneRewardItem(obj) {
  if (obj == null) return null
  var cnt = Math.max(1, obj.count | 0 || 1)
  // Executable Items: выдача командой ei give (см. eiGiveCommand в JSON)
  if (obj.eiGive != null && String(obj.eiGive).length > 0) {
    return { eiGive: String(obj.eiGive), count: cnt, id: null, components: null, stackFile: null }
  }
  if (obj.eiId != null && String(obj.eiId).length > 0) {
    return { eiGive: String(obj.eiId), count: cnt, id: null, components: null, stackFile: null }
  }
  // Полная строка предмета (SNBT) из файла — иначе длинные кавычки ломают JSON
  if (obj.stackFile) {
    return {
      stackFile: String(obj.stackFile),
      count: cnt,
      id: null,
      components: null
    }
  }
  if (!obj.id) return null
  return {
    id: String(obj.id),
    count: cnt,
    components: obj.components != null ? obj.components : null
  }
}

function normalizeRewardList(e) {
  var rewardItems = []
  var rewardPick = 'random'
  var ii
  var one
  if (e.items && e.items.length) {
    for (ii = 0; ii < e.items.length; ii++) {
      one = normalizeOneRewardItem(e.items[ii])
      if (one) rewardItems.push(one)
    }
    rewardPick = e.itemPick != null ? String(e.itemPick) : e.pick != null ? String(e.pick) : 'random'
  } else if (e.item) {
    rewardItems.push({
      id: String(e.item),
      count: Math.max(1, e.count | 0 || 1),
      components: e.components != null ? e.components : null
    })
  }
  if (rewardItems.length === 0) {
    rewardItems.push({ id: 'minecraft:wheat_seeds', count: 1, components: null })
  }
  if (rewardPick !== 'random' && rewardPick !== 'first' && rewardPick !== 'all') rewardPick = 'random'
  return { rewardItems: rewardItems, rewardPick: rewardPick }
}

/**
 * Таблица id блока → правило. Не Map и не for-of по entries: в Rhino/KubeJS итерация по
 * JsonIO/Java-спискам часто «залипает» на первой записи — тогда у всех блоков один regrowTicks.
 */
function buildLookup(config) {
  var map = {}
  var entries = config.entries
  if (!entries) return map
  var n = entries.length
  if (n == null || n === 0) {
    try {
      if (entries.size) n = entries.size()
    } catch (e0) {}
  }
  var ei
  var e
  var regrowTicks
  var regrowSecs
  var rw
  var blocks
  var blen
  var bi
  var b
  var key
  for (ei = 0; ei < n; ei++) {
    e = entries[ei]
    if (e == null && entries.get) {
      try {
        e = entries.get(ei)
      } catch (e1) {}
    }
    if (e == null) continue
    // Приоритет: regrowTicks (тики мира) > regrowMinutes > regrowSeconds. 20 мин = 1200 с или regrowMinutes: 20.
    if (e.regrowTicks != null && String(e.regrowTicks) !== '') {
      regrowTicks = Math.max(1, Math.floor(Number(e.regrowTicks)))
    } else {
      if (e.regrowMinutes != null && String(e.regrowMinutes) !== '') {
        regrowSecs = Number(e.regrowMinutes) * 60
      } else if (e.regrowSeconds != null && String(e.regrowSeconds) !== '') {
        regrowSecs = Number(e.regrowSeconds)
      } else {
        regrowSecs = 60
      }
      regrowTicks = Math.max(1, Math.floor(regrowSecs * 20))
    }
    rw = normalizeRewardList(e)
    blocks = e.blocks || []
    blen = blocks.length
    if (blen == null || blen === 0) {
      try {
        if (blocks.size) blen = blocks.size()
      } catch (e2) {}
    }
    for (bi = 0; bi < blen; bi++) {
      b = blocks[bi]
      if (b == null && blocks.get) {
        try {
          b = blocks.get(bi)
        } catch (e3) {}
      }
      if (b == null) continue
      key = String(b)
      if (map[key] !== undefined) continue
      map[key] = {
        rewardItems: rw.rewardItems,
        rewardPick: rw.rewardPick,
        regrowTicks: regrowTicks,
        blocks: e.blocks
      }
    }
  }
  return map
}

function pickRewardIndex(rule) {
  var n = rule.rewardItems.length
  if (n <= 1) return 0
  var pick = rule.rewardPick
  if (pick === 'first') return 0
  if (pick === 'random') return Math.floor(Math.random() * n)
  return 0
}

function itemStackFromRewardEntry(entry) {
  var id = entry.id
  var count = entry.count
  var comp = entry.components
  var hasComp = comp != null && typeof comp === 'object'
  var raw
  var stack
  if (entry.stackFile) {
    try {
      raw = IO.read(String(entry.stackFile)).trim()
      if (raw.length >= 2 && raw.charAt(0) === "'" && raw.charAt(raw.length - 1) === "'") {
        raw = raw.substring(1, raw.length - 1)
      }
      stack = Item.of(raw)
      if (count > 1 && stack.withCount) {
        return stack.withCount(count)
      }
      return stack
    } catch (sfErr) {
      console.error('[grass_harvest] stackFile ' + entry.stackFile + ': ' + sfErr)
      throw sfErr
    }
  }
  if (hasComp) {
    try {
      return Item.of(id, count, comp)
    } catch (a) {
      try {
        return Item.of(id, comp).withCount(count)
      } catch (b) {
        try {
          return Item.of(id).withCount(count)
        } catch (c) {
          throw c
        }
      }
    }
  }
  return Item.of(id, count)
}

function playerNameForCommand(pl) {
  try {
    if (pl.name && pl.name.string) return pl.name.string
  } catch (e1) {}
  try {
    return String(pl.username)
  } catch (e2) {}
  return String(pl)
}

function applyEiGiveTemplate(tpl, playerName, eiId, count) {
  var s = String(tpl)
  s = s.split('{player}').join(playerName)
  s = s.split('{id}').join(eiId)
  s = s.split('{count}').join(String(count))
  return s
}

function giveOneRewardEntry(player, server, entry, commandTemplate) {
  var cnt
  var cmd
  if (entry.eiGive) {
    cnt = Math.max(1, entry.count | 0 || 1)
    cmd = applyEiGiveTemplate(commandTemplate, playerNameForCommand(player), String(entry.eiGive), cnt)
    try {
      server.runCommandSilent(cmd)
    } catch (e) {
      console.error('[grass_harvest] ' + cmd + ' -> ' + e)
    }
    return
  }
  player.give(itemStackFromRewardEntry(entry))
}

function prevalidateRuleRewards(rule) {
  var items = rule.rewardItems
  var pick = rule.rewardPick
  var idx
  var i
  var entry
  var toCheck = []
  if (!items || items.length === 0) return
  if (pick === 'all') {
    toCheck = items
  } else {
    idx = pickRewardIndex(rule)
    toCheck = [items[idx]]
  }
  for (i = 0; i < toCheck.length; i++) {
    entry = toCheck[i]
    if (entry.eiGive) continue
    itemStackFromRewardEntry(entry)
  }
}

function giveRuleRewards(player, server, rule) {
  var items = rule.rewardItems
  var pick = rule.rewardPick
  var tpl =
    grassConfig.eiGiveCommand != null && String(grassConfig.eiGiveCommand).length > 0
      ? String(grassConfig.eiGiveCommand)
      : 'ei give {player} {id} {count}'
  var i
  var idx
  if (!items || items.length === 0) return
  if (pick === 'all') {
    for (i = 0; i < items.length; i++) {
      giveOneRewardEntry(player, server, items[i], tpl)
    }
    return
  }
  idx = pickRewardIndex(rule)
  giveOneRewardEntry(player, server, items[idx], tpl)
}

function defaultGrassConfig() {
  return {
    entries: [
      {
        blocks: ['minecraft:short_grass', 'minecraft:fern'],
        items: [{ id: 'minecraft:wheat_seeds', count: 1 }],
        itemPick: 'random',
        regrowSeconds: 60
      },
      {
        blocks: ['minecraft:tall_grass', 'minecraft:large_fern'],
        items: [{ id: 'minecraft:wheat', count: 1 }],
        itemPick: 'first',
        regrowSeconds: 120
      }
    ],
    requireEmptyMainHand: false,
    debug: false,
    eiGiveCommand: 'ei give {player} {id} {count}',
    doublePlantBlockIds: [],
    showRegrowTimer: true,
    regrowTimerMode: 'text_display'
  }
}

function loadGrassConfig() {
  var data = null
  try {
    data = JsonIO.read(CONFIG_PATH)
  } catch (e) {
    console.error(
      '[grass_harvest] Невалидный JSON в ' +
        CONFIG_PATH +
        ': ' +
        e +
        ' Проверьте кавычки и запятые; id в кавычках. Используется конфиг по умолчанию.'
    )
    return defaultGrassConfig()
  }
  if (!data || !data.entries) {
    console.warn('[grass_harvest] Нет или пустой ' + CONFIG_PATH + ', используется встроенный минимум.')
    return defaultGrassConfig()
  }
  if (data.debug === undefined) data.debug = false
  if (data.eiGiveCommand === undefined) data.eiGiveCommand = 'ei give {player} {id} {count}'
  if (data.doublePlantBlockIds === undefined) data.doublePlantBlockIds = []
  if (data.showRegrowTimer === undefined) data.showRegrowTimer = true
  if (data.regrowTimerMode === undefined) data.regrowTimerMode = 'text_display'
  return data
}

let grassConfig = loadGrassConfig()
let blockRuleLookup = buildLookup(grassConfig)
/** Очередь отрастаний: счётчик в ServerEvents.tick. После /kubejs reload server_scripts очередь сбрасывается — старые text_display могут остаться. */
var grassHarvestRegrowQueue = []
/** Повторная очистка тэгов завершённых таймеров (на случай выгруженного чанка в момент окончания). */
var grassHarvestTimerCleanupQueue = []
// Повторять cleanup долго: 24 часа с шагом 5 секунд.
var TIMER_CLEANUP_INTERVAL_TICKS = 100
var TIMER_CLEANUP_MAX_TRIES = 17280

function getDoublePlantIds() {
  var base = ['minecraft:tall_grass', 'minecraft:large_fern', 'minecraft:tall_seagrass']
  var extra = grassConfig.doublePlantBlockIds || []
  var out = []
  var i
  for (i = 0; i < base.length; i++) out.push(base[i])
  for (i = 0; i < extra.length; i++) {
    var s = String(extra[i])
    if (out.indexOf(s) < 0) out.push(s)
  }
  return out
}

/**
 * Двухблочные растения: снимаем обе половины, сохраняем состояние для восстановления.
 * @param {Internal.LevelBlock} block
 * @returns {{ snapshots: { pos: {x:number,y:number,z:number}, id: string, props: Object }[] } | null}
 */
function removeDoublePlant(block) {
  var id = kubeBlockId(block)
  var level = block.level
  var pos = block.pos
  var doubles = getDoublePlantIds()
  if (doubles.indexOf(id) < 0) return null

  var half = null
  var p = null
  try {
    p = block.properties
    if (p && p.half !== undefined) half = p.half
  } catch (ignored) {}
  var snapshots = []
  var otherY
  var main
  var otherBlock
  var otherId
  var other
  var ob
  var o2
  var dy
  if (half === 'lower' || half === 'upper') {
    otherY = half === 'lower' ? pos.y + 1 : pos.y - 1
    main = { pos: { x: pos.x, y: pos.y, z: pos.z }, id: id, props: safeProps(block) }
    otherBlock = level.getBlock(pos.x, otherY, pos.z)
    otherId = kubeBlockId(otherBlock)
    other = {
      pos: { x: pos.x, y: otherY, z: pos.z },
      id: otherId,
      props: safeProps(otherBlock)
    }
    if (otherId === id) {
      block.set('minecraft:air')
      otherBlock.set('minecraft:air')
      snapshots.push(main, other)
      return { snapshots: snapshots }
    }
  }
  for (dy = -1; dy <= 1; dy += 2) {
    ob = level.getBlock(pos.x, pos.y + dy, pos.z)
    if (kubeBlockId(ob) === id) {
      main = { pos: { x: pos.x, y: pos.y, z: pos.z }, id: id, props: safeProps(block) }
      o2 = { pos: { x: pos.x, y: pos.y + dy, z: pos.z }, id: kubeBlockId(ob), props: safeProps(ob) }
      block.set('minecraft:air')
      ob.set('minecraft:air')
      snapshots.push(main, o2)
      return { snapshots: snapshots }
    }
  }
  block.set('minecraft:air')
  snapshots.push({ pos: { x: pos.x, y: pos.y, z: pos.z }, id: id, props: safeProps(block) })
  return { snapshots: snapshots }
}

function safeProps(block) {
  var out = {}
  var p
  var keys
  var ki
  var it
  var e
  try {
    p = block.properties
    if (p == null) return out
    keys = Object.keys(p)
    for (ki = 0; ki < keys.length; ki++) {
      out[keys[ki]] = p[keys[ki]]
    }
    if (keys.length === 0 && p.entrySet) {
      try {
        it = p.entrySet().iterator()
        while (it.hasNext()) {
          e = it.next()
          out[String(e.getKey())] = String(e.getValue())
        }
      } catch (e2) {}
    }
  } catch (e1) {}
  return out
}

function normalizeHalfValue(v) {
  var s = String(v)
  if (s === 'LOWER' || s === 'lower') return 'lower'
  if (s === 'UPPER' || s === 'upper') return 'upper'
  return s
}

/** KubeJS 6: block.set не принимает строку вида id[prop=val] — только id или id + объект свойств. */
function normalizePropsForSet(props) {
  var out = {}
  var keys = Object.keys(props || {})
  var k
  var key
  var val
  for (k = 0; k < keys.length; k++) {
    key = keys[k]
    val = props[key]
    if (key === 'half') val = normalizeHalfValue(val)
    out[key] = val
  }
  return out
}

/** Пустой блок для отрастания: не только minecraft:air (часто cave_air / void_air). */
function isEmptyForRegrow(block) {
  try {
    if (block.isAir && block.isAir()) return true
  } catch (e0) {}
  var bid = kubeBlockId(block)
  return bid === 'minecraft:air' || bid === 'minecraft:cave_air' || bid === 'minecraft:void_air'
}

function isWaterForRegrow(block) {
  var bid = kubeBlockId(block)
  return bid === 'minecraft:water' || bid === 'minecraft:flowing_water' || bid === 'minecraft:bubble_column'
}

function canRegrowAtBlock(currentBlock, targetId) {
  if (isEmptyForRegrow(currentBlock)) return true
  // Водные растения должны уметь восстанавливаться в воде.
  if (targetId === 'minecraft:seagrass' || targetId === 'minecraft:tall_seagrass') {
    return isWaterForRegrow(currentBlock)
  }
  return false
}

function floorBlockPos(pos) {
  return {
    x: Math.floor(Number(pos.x)),
    y: Math.floor(Number(pos.y)),
    z: Math.floor(Number(pos.z))
  }
}

function dimensionIdForExecute(level) {
  try {
    var d = level.dimension
    if (d && d.location) return String(d.location())
    return String(d)
  } catch (e) {
    return 'minecraft:overworld'
  }
}

/** Если b.set не оставил блок (часть модовых растений), пробуем setblock в том же измерении. */
function trySetblockFallback(level, snap, fx, fy, fz) {
  var srv = level.server
  if (!srv || !srv.runCommandSilent) return false
  if (grassConfig.regrowSetblockFallback === false) return false
  var dim = dimensionIdForExecute(level)
  var id = snap.id
  var norm = normalizePropsForSet(snap.props || {})
  var keys = Object.keys(norm)
  var candidates = []
  var bracket
  var ki
  if (keys.length > 0) {
    var parts = []
    for (ki = 0; ki < keys.length; ki++) {
      parts.push(keys[ki] + '=' + norm[keys[ki]])
    }
    bracket = id + '[' + parts.join(',') + ']'
    candidates.push(bracket)
  }
  candidates.push(id)
  var ci
  var cmd
  for (ci = 0; ci < candidates.length; ci++) {
    cmd = 'execute in ' + dim + ' run setblock ' + fx + ' ' + fy + ' ' + fz + ' ' + candidates[ci] + ' replace'
    try {
      srv.runCommandSilent(cmd)
    } catch (eCmd) {
      if (grassConfig.debug) console.error('[grass_harvest] setblock: ' + cmd + ' -> ' + eCmd)
      continue
    }
    if (!isEmptyForRegrow(level.getBlock(fx, fy, fz))) return true
  }
  return false
}

function applySnapshot(level, snap, snapIndex, allSorted) {
  var fp = floorBlockPos(snap.pos)
  var fx = fp.x
  var fy = fp.y
  var fz = fp.z
  var b = level.getBlock(fx, fy, fz)
  var id = snap.id
  if (!canRegrowAtBlock(b, id)) return
  var props = snap.props || {}
  var halfForced
  var norm
  var doubleIds = getDoublePlantIds()
  // Двухблоковые растения: half по порядку Y (нижний = lower).
  if (allSorted && allSorted.length === 2 && doubleIds.indexOf(id) >= 0) {
    halfForced = snapIndex === 0 ? 'lower' : 'upper'
    try {
      b.set(id, { half: halfForced })
      if (!isEmptyForRegrow(level.getBlock(fx, fy, fz))) return
    } catch (e0) {
      if (grassConfig.debug) console.error('[grass_harvest] tall half failed, пробуем сохранённые props: ' + e0)
    }
    try {
      norm = normalizePropsForSet(props)
      if (Object.keys(norm).length > 0) {
        b.set(id, norm)
      } else {
        b.set(id)
      }
    } catch (eFallback) {
      if (grassConfig.debug) console.error('[grass_harvest] tall plant fallback: ' + eFallback)
    }
    if (isEmptyForRegrow(level.getBlock(fx, fy, fz))) trySetblockFallback(level, snap, fx, fy, fz)
    return
  }
  norm = normalizePropsForSet(props)
  try {
    if (Object.keys(norm).length === 0) {
      b.set(id)
    } else {
      b.set(id, norm)
    }
  } catch (e2) {
    try {
      b.set(id)
    } catch (e3) {
      if (grassConfig.debug) console.error('[grass_harvest] applySnapshot: ' + e3)
    }
  }
  if (isEmptyForRegrow(level.getBlock(fx, fy, fz))) {
    if (grassConfig.debug) {
      console.warn('[grass_harvest] после b.set всё ещё пусто, id=' + id + ' @ ' + fx + ',' + fy + ',' + fz + ' — setblock fallback')
    }
    trySetblockFallback(level, snap, fx, fy, fz)
  }
}

/** Двухблоковые растения: сначала нижняя часть (меньший Y), иначе верх не ставится. */
function sortSnapshotsForRestore(list) {
  var copy = []
  var i
  for (i = 0; i < list.length; i++) copy.push(list[i])
  copy.sort(function (a, b) {
    return a.pos.y - b.pos.y
  })
  return copy
}

/**
 * Координаты блока(ов) для отображения таймера: одна точка над местом срыва.
 * Два снимка в один столбец (высокое растение) — только верхний блок, текст над ним.
 */
function timerDisplaySpots(snapshots) {
  var out = []
  var i
  var p
  var a
  var b
  var topY
  if (!snapshots || snapshots.length === 0) return out
  if (snapshots.length === 1) {
    p = floorBlockPos(snapshots[0].pos)
    out.push(p.x + ',' + p.y + ',' + p.z)
    return out
  }
  if (snapshots.length === 2) {
    a = floorBlockPos(snapshots[0].pos)
    b = floorBlockPos(snapshots[1].pos)
    if (a.x === b.x && a.z === b.z && Math.abs(a.y - b.y) === 1) {
      topY = a.y > b.y ? a.y : b.y
      out.push(a.x + ',' + topY + ',' + a.z)
      return out
    }
  }
  for (i = 0; i < snapshots.length; i++) {
    p = floorBlockPos(snapshots[i].pos)
    out.push(p.x + ',' + p.y + ',' + p.z)
  }
  return out
}

function grassHarvestDimensionId(level) {
  try {
    if (level.dimension && level.dimension.location) return String(level.dimension.location())
  } catch (e0) {}
  return 'minecraft:overworld'
}

/** Свежий Level по id измерения — ссылка из события ПКМ со временем «портится»; без этого regrow может не сработать. */
function findServerLevelByDimId(server, dimId) {
  if (!server || !dimId) return null
  var want = String(dimId)
  var levels
  var n
  var i
  var lv
  try {
    levels = server.levels
    if (!levels) return null
    n = levels.length
    if (n == null || n === 0) {
      try {
        if (levels.size) n = levels.size()
      } catch (eSz) {}
    }
    for (i = 0; i < n; i++) {
      lv = levels[i]
      if (lv == null && levels.get) {
        try {
          lv = levels.get(i)
        } catch (eG) {}
      }
      if (lv != null && grassHarvestDimensionId(lv) === want) return lv
    }
  } catch (e0) {}
  return null
}

/** Rhino/KubeJS: level.gameTime — часто java.lang.Long; без Number() вычитание даёт NaN → на табличке «0с». */
function levelGameTimeTicks(level) {
  try {
    var t = level.gameTime
    var n = Number(t)
    if (isFinite(n)) return n
  } catch (e0) {}
  return 0
}

/** Оставшееся время до отрастания — подпись над цветком (тики мира). */
function formatRegrowLabel(remainingTicks) {
  var rt = Number(remainingTicks)
  if (!isFinite(rt) || rt <= 0) return '00:00'
  var sec = ((rt + 19) / 20) | 0
  var m = (sec / 60) | 0
  var s = sec % 60
  var mm = m < 10 ? '0' + m : String(m)
  var ss = s < 10 ? '0' + s : String(s)
  return mm + ':' + ss
}

function timerTagCoordPart(v) {
  var n = Math.floor(Number(v))
  if (!isFinite(n)) n = 0
  return n < 0 ? 'n' + Math.abs(n) : 'p' + n
}

function timerTagDimPart(dim) {
  return String(dim || 'minecraft_overworld')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '_')
}

/** Стабильный тег таймера: один блок в одном измерении => один tag (без рандома). */
function makeTimerEntityTag(bx, by, bz, dim) {
  return 'ght_' + timerTagDimPart(dim) + '_' + timerTagCoordPart(bx) + '_' + timerTagCoordPart(by) + '_' + timerTagCoordPart(bz)
}

/** Ключ слота таймера: дедуп очереди и отмена старого КД при повторном срыве той же клетки. */
function timerSpotKey(dim, bx, by, bz) {
  return String(dim) + '|' + bx + ',' + by + ',' + bz
}

function buildTimerSpotMeta(level, snapshots) {
  var dim = grassHarvestDimensionId(level)
  var spots = timerDisplaySpots(snapshots)
  var keys = []
  var tags = []
  var si
  var parts
  var bx
  var by
  var bz
  var tag
  for (si = 0; si < spots.length; si++) {
    parts = spots[si].split(',')
    if (parts.length !== 3) continue
    bx = parseInt(parts[0], 10)
    by = parseInt(parts[1], 10)
    bz = parseInt(parts[2], 10)
    if (isNaN(bx) || isNaN(by) || isNaN(bz)) continue
    tag = makeTimerEntityTag(bx, by, bz, dim)
    keys.push(timerSpotKey(dim, bx, by, bz))
    tags.push(tag)
  }
  return { keys: keys, tags: tags, dim: dim }
}

function purgeCleanupForTags(tags) {
  if (!tags || tags.length === 0) return
  var cq = grassHarvestTimerCleanupQueue
  var ci
  for (ci = cq.length - 1; ci >= 0; ci--) {
    if (tags.indexOf(cq[ci].tag) >= 0) {
      try {
        cq.splice(ci, 1)
      } catch (eSp) {}
    }
  }
}

function cancelGrassHarvestJobsOverlappingKeys(server, keys) {
  if (!server || !keys || keys.length === 0) return
  var q = grassHarvestRegrowQueue
  var ji = q.length
  var job
  var jk
  var overlap
  while (ji--) {
    job = q[ji]
    if (!job || !job.timerKeys || job.timerKeys.length === 0) continue
    overlap = false
    for (jk = 0; jk < keys.length; jk++) {
      if (job.timerKeys.indexOf(keys[jk]) >= 0) {
        overlap = true
        break
      }
    }
    if (overlap) {
      try {
        removeRegrowTimerEntities(server, job.handles)
      } catch (eR) {}
      try {
        q.splice(ji, 1)
      } catch (eS) {}
    }
  }
}

/**
 * Сущности с подписью только на сервере: text_display (по умолчанию) или armor_stand.
 * Спавн через команды — без client_scripts.
 */
function summonOneRegrowTimerEntity(server, dim, useStand, tag, bx, by, bz, labelText) {
  if (!server || !server.runCommandSilent) return
  var x = bx + 0.5
  var y = by + 1.22
  var z = bz + 0.5
  var textJson = JSON.stringify({ text: labelText })
  var inner = textJson.replace(/\\/g, '\\\\').replace(/'/g, "\\'")
  var cmd
  try {
    // Удаляем возможные дубликаты от прошлых сессий/крашей до нового summon.
    server.runCommandSilent('execute in ' + dim + ' run kill @e[type=minecraft:text_display,tag=' + tag + ']')
    server.runCommandSilent('execute in ' + dim + ' run kill @e[type=minecraft:armor_stand,tag=' + tag + ']')
    // Любые чужие/старые подписи ровно над клеткой (другой тег, битый merge и т.д.)
    server.runCommandSilent(
      'execute in ' + dim + ' positioned ' + x + ' ' + y + ' ' + z + ' run kill @e[type=minecraft:text_display,distance=..0.25]'
    )
    server.runCommandSilent(
      'execute in ' + dim + ' positioned ' + x + ' ' + y + ' ' + z + ' run kill @e[type=minecraft:armor_stand,distance=..0.25]'
    )
    if (useStand) {
      cmd =
        'execute in ' +
        dim +
        ' run summon minecraft:armor_stand ' +
        x +
        ' ' +
        y +
        ' ' +
        z +
        ' {Tags:["' +
        tag +
        '"],Marker:1b,Invisible:1b,NoGravity:1b,Small:1b,CustomNameVisible:1b,CustomName:\'' +
        inner +
        "'}"
    } else {
      cmd =
        'execute in ' +
        dim +
        ' run summon minecraft:text_display ' +
        x +
        ' ' +
        y +
        ' ' +
        z +
        ' {Tags:["' +
        tag +
        '"],text:\'' +
        inner +
        '\',billboard:"center",see_through:1b}'
    }
    server.runCommandSilent(cmd)
  } catch (spErr) {
    if (grassConfig.debug) console.error('[grass_harvest] summon timer: ' + spErr)
  }
}

function spawnServerRegrowTimers(server, level, snapshots, regrowTicks) {
  if (grassConfig.showRegrowTimer === false) return []
  if (!server || !server.runCommandSilent) return []
  var rticks = Number(regrowTicks)
  if (!isFinite(rticks) || rticks < 1) rticks = 1
  var spots = timerDisplaySpots(snapshots)
  if (!spots || spots.length === 0) return []
  var useStand = false
  var dim = grassHarvestDimensionId(level)
  var label0 = formatRegrowLabel(rticks)
  var handles = []
  var si
  var parts
  var bx
  var by
  var bz
  var tag
  for (si = 0; si < spots.length; si++) {
    parts = spots[si].split(',')
    if (parts.length !== 3) continue
    bx = parseInt(parts[0], 10)
    by = parseInt(parts[1], 10)
    bz = parseInt(parts[2], 10)
    if (isNaN(bx) || isNaN(by) || isNaN(bz)) continue
    tag = makeTimerEntityTag(bx, by, bz, dim)
    summonOneRegrowTimerEntity(server, dim, useStand, tag, bx, by, bz, label0)
    handles.push({ tag: tag, dim: dim, stand: useStand, bx: bx, by: by, bz: bz })
  }
  return handles
}

function mergeTimerEntityLabel(server, h, label) {
  if (!h || !h.tag || !server.runCommandSilent) return
  var textJson = JSON.stringify({ text: label })
  var inner = textJson.replace(/\\/g, '\\\\').replace(/'/g, "\\'")
  var sel
  var cmd
  try {
    if (h.stand) {
      sel = '@e[type=minecraft:armor_stand,tag=' + h.tag + ',limit=1]'
      cmd = 'execute in ' + h.dim + ' run data merge entity ' + sel + " {CustomName:'" + inner + "'}"
    } else {
      sel = '@e[type=minecraft:text_display,tag=' + h.tag + ',limit=1]'
      cmd = 'execute in ' + h.dim + ' run data merge entity ' + sel + " {text:'" + inner + "'}"
    }
    server.runCommandSilent(cmd)
  } catch (eM) {
    if (grassConfig.debug) console.error('[grass_harvest] data merge timer: ' + eM)
  }
}

function removeRegrowTimerEntities(server, handles) {
  if (!handles || handles.length === 0) return
  var hi
  var h
  var type
  var cmd
  for (hi = 0; hi < handles.length; hi++) {
    h = handles[hi]
    if (!h || !h.tag) continue
    type = h.stand ? 'minecraft:armor_stand' : 'minecraft:text_display'
    try {
      cmd = 'execute in ' + h.dim + ' run kill @e[type=' + type + ',tag=' + h.tag + ']'
      server.runCommandSilent(cmd)
    } catch (eK) {}
  }
}

function enqueueTimerCleanup(server, handles) {
  if (!server || !handles || handles.length === 0) return
  var i
  var h
  var j
  var found
  for (i = 0; i < handles.length; i++) {
    h = handles[i]
    if (!h || !h.tag || !h.dim) continue
    found = false
    for (j = 0; j < grassHarvestTimerCleanupQueue.length; j++) {
      if (grassHarvestTimerCleanupQueue[j].tag === h.tag && grassHarvestTimerCleanupQueue[j].dim === h.dim) {
        grassHarvestTimerCleanupQueue[j].triesLeft = TIMER_CLEANUP_MAX_TRIES
        found = true
        break
      }
    }
    if (!found) {
      grassHarvestTimerCleanupQueue.push({
        server: server,
        tag: h.tag,
        dim: h.dim,
        stand: !!h.stand,
        triesLeft: TIMER_CLEANUP_MAX_TRIES
      })
    }
  }
}

ServerEvents.tick(function (event) {
  var q = grassHarvestRegrowQueue
  var ji = q.length
  var job
  var elapsed
  var label
  var hi
  var cleanup = grassHarvestTimerCleanupQueue
  var ci
  var c
  var ctype
  var ccmd
  while (ji--) {
    job = q[ji]
    if (!job || job.remainingTicks == null || job.totalTicks == null) {
      try {
        q.splice(ji, 1)
      } catch (eSp) {}
      continue
    }
    job.remainingTicks--
    elapsed = job.totalTicks - job.remainingTicks
    if (job.handles && job.handles.length > 0 && job.server) {
      if (job.remainingTicks > 0) {
        var doMerge = false
        if (elapsed > 0 && elapsed % 20 === 0) doMerge = true
        else if (job.totalTicks < 20 && elapsed > 0) doMerge = true
        if (doMerge) {
          label = formatRegrowLabel(job.remainingTicks)
          for (hi = 0; hi < job.handles.length; hi++) {
            // Обновляем только текст существующей сущности без пересоздания.
            mergeTimerEntityLabel(job.server, job.handles[hi], label)
          }
        }
      }
    }
    if (job.remainingTicks <= 0) {
      var dimKey = job.dimensionId != null ? job.dimensionId : job.handles && job.handles[0] ? job.handles[0].dim : null
      var lvlRegrow = findServerLevelByDimId(job.server, dimKey) || job.level
      if (!lvlRegrow) {
        job.remainingTicks = 1
      } else {
        try {
          // Таймер оставляем всегда: после окончания КД он показывает 00:00.
          for (hi = 0; hi < job.handles.length; hi++) {
            mergeTimerEntityLabel(job.server, job.handles[hi], '00:00')
          }
          if (grassConfig.debug) {
            console.info('[grass_harvest] regrow (тик): ' + job.blockIdForLog + ', снимков: ' + job.restoreList.length)
          }
          for (hi = 0; hi < job.restoreList.length; hi++) {
            applySnapshot(lvlRegrow, job.restoreList[hi], hi, job.restoreList)
          }
        } catch (eR) {
          console.error('[grass_harvest] regrow: ' + eR)
        }
        try {
          q.splice(ji, 1)
        } catch (eSp2) {}
      }
    }
  }
  // Периодически повторяем kill завершённых таймеров:
  // если чанк был выгружен в момент regrow, сущность удалится после следующей прогрузки.
  var runCleanup = false
  try {
    if (event.server && event.server.tickCount != null) {
      runCleanup = Number(event.server.tickCount) % TIMER_CLEANUP_INTERVAL_TICKS === 0
    }
  } catch (eTc) {}
  if (event.server && runCleanup) {
    for (ci = cleanup.length - 1; ci >= 0; ci--) {
      c = cleanup[ci]
      if (!c || !c.server || !c.tag || !c.dim || c.triesLeft <= 0) {
        cleanup.splice(ci, 1)
        continue
      }
      ctype = c.stand ? 'minecraft:armor_stand' : 'minecraft:text_display'
      try {
        ccmd = 'execute in ' + c.dim + ' run kill @e[type=' + ctype + ',tag=' + c.tag + ']'
        c.server.runCommandSilent(ccmd)
      } catch (eC) {}
      c.triesLeft--
      if (c.triesLeft <= 0) cleanup.splice(ci, 1)
    }
  }
})

/** Вторая рука: отсекаем, иначе срабатывание удваивается. MAIN_HAND не должен содержать подстроку OFF как ложное срабатывание. */
function isOffHandInteraction(event) {
  try {
    var ghHand = event.hand
    if (ghHand == null) return false
    var s = String(ghHand)
    if (s.indexOf('OFF_HAND') >= 0) return true
    if (s.toLowerCase().indexOf('off_hand') >= 0) return true
  } catch (e) {}
  return false
}

function collectBlockIdsFromConfig(cfg) {
  var ids = []
  var entries = cfg.entries || []
  var n = entries.length
  if (n == null || n === 0) {
    try {
      if (entries.size) n = entries.size()
    } catch (e0) {}
  }
  var ei
  var e
  var blocks
  var blen
  var bi
  var b
  var s
  for (ei = 0; ei < n; ei++) {
    e = entries[ei]
    if (e == null && entries.get) {
      try {
        e = entries.get(ei)
      } catch (e1) {}
    }
    if (e == null) continue
    blocks = e.blocks || []
    blen = blocks.length
    if (blen == null || blen === 0) {
      try {
        if (blocks.size) blen = blocks.size()
      } catch (e2) {}
    }
    for (bi = 0; bi < blen; bi++) {
      b = blocks[bi]
      if (b == null && blocks.get) {
        try {
          b = blocks.get(bi)
        } catch (e3) {}
      }
      if (b == null) continue
      s = String(b)
      if (ids.indexOf(s) < 0) ids.push(s)
    }
  }
  return ids
}

function handleGrassRightClick(event) {
  // Rhino/KubeJS: не использовать const/let в этом обработчике (конфликт с контекстом события и for-of).
  var ghPlayer = event.player
  var ghBlock = event.block
  var ghServer = event.server
  var ghLevel = event.level
  var ghRule
  var ghSnapshots
  var ghDouble
  var ghBid
  var ghToRestore
  var ghRegrowTicks

  try {
    if (!ghPlayer || ghPlayer.fake) return
    if (isOffHandInteraction(event)) return

    ghBid = kubeBlockId(ghBlock)
    if (grassConfig.debug) {
      console.info('[grass_harvest] ПКМ по блоку id="' + ghBid + '" hand=' + event.hand)
    }
    ghRule = blockRuleLookup[String(ghBid)]
    if (!ghRule) {
      if (grassConfig.debug) {
        console.info('[grass_harvest] ПКМ по блоку не из списка: "' + ghBid + '" (добавьте в grass_harvest.json)')
      }
      return
    }

    if (grassConfig.requireEmptyMainHand && !ghPlayer.mainHandItem.isEmpty()) return

    ghRegrowTicks = ghRule.regrowTicks
    try {
      ghRegrowTicks = Math.max(1, Math.floor(Number(ghRegrowTicks)))
    } catch (eRt) {}
    if (grassConfig.debug) {
      console.info(
        '[grass_harvest] КД для ' +
          ghBid +
          ': ' +
          ghRegrowTicks +
          ' тиков ≈ ' +
          (ghRegrowTicks / 20).toFixed(1) +
          ' с ≈ ' +
          (ghRegrowTicks / 20 / 60).toFixed(2) +
          ' мин (проверьте regrowSeconds/regrowMinutes в JSON)'
      )
    }
    try {
      prevalidateRuleRewards(ghRule)
    } catch (ghErr) {
      console.error('[grass_harvest] Неверный предмет в конфиге: ' + ghErr)
      return
    }

    ghDouble = removeDoublePlant(ghBlock)
    if (ghDouble) {
      ghSnapshots = ghDouble.snapshots
    } else {
      ghSnapshots = [{ pos: floorBlockPos(ghBlock.pos), id: ghBid, props: safeProps(ghBlock) }]
      ghBlock.set('minecraft:air')
    }

    giveRuleRewards(ghPlayer, ghServer, ghRule)

    // Важно: event.cancel() в KubeJS бросает EventExit — код ПОСЛЕ него не выполняется. Сначала планируем regrow, потом отменяем событие.
    ghToRestore = sortSnapshotsForRestore(ghSnapshots)
    var spotMeta = buildTimerSpotMeta(ghLevel, ghToRestore)
    cancelGrassHarvestJobsOverlappingKeys(ghServer, spotMeta.keys)
    purgeCleanupForTags(spotMeta.tags)
    var ghTimerHandles = spawnServerRegrowTimers(ghServer, ghLevel, ghToRestore, ghRegrowTicks)
    if (grassConfig.debug) {
      console.info(
        '[grass_harvest] КД в очереди: ' +
          ghRegrowTicks +
          ' серверных тиков (~' +
          (ghRegrowTicks / 20).toFixed(1) +
          ' с), блок ' +
          ghBid
      )
    }
    grassHarvestRegrowQueue.push({
      totalTicks: ghRegrowTicks,
      remainingTicks: ghRegrowTicks,
      server: ghServer,
      level: ghLevel,
      dimensionId: grassHarvestDimensionId(ghLevel),
      timerKeys: spotMeta.keys,
      restoreList: ghToRestore,
      handles: ghTimerHandles,
      blockIdForLog: ghBid
    })

    event.cancel()
  } catch (ghOuter) {
    // Не глотать EventExit от cancel(), если он когда-либо вылетит из другого места
    var ghMsg = String(ghOuter)
    if (ghMsg.indexOf('EventExit') >= 0) return
    console.error('[grass_harvest] ' + ghOuter)
  }
}

// Подписка по каждому id (как в доке KubeJS). Сравнение id — kubeBlockId + строковые ключи в blockRuleLookup.
;(function registerGrassHandlers() {
  var ids = collectBlockIdsFromConfig(grassConfig)
  var bi
  if (ids.length === 0) {
    console.warn('[grass_harvest] В конфиге нет блоков — добавьте id в kubejs/config/grass_harvest.json')
    return
  }
  for (bi = 0; bi < ids.length; bi++) {
    BlockEvents.rightClicked(ids[bi], handleGrassRightClick)
  }
  console.info('[grass_harvest] ПКМ зарегистрирован для блоков: ' + ids.join(', '))
})()

ServerEvents.loaded(() => {
  grassConfig = loadGrassConfig()
  blockRuleLookup = buildLookup(grassConfig)
  var ids = collectBlockIdsFromConfig(grassConfig)
  var ei
  var ent
  var bi
  var blk
  var secs
  console.info('[grass_harvest] Конфиг перезагружен, блоки в таблице: ' + ids.join(', '))
  for (ei = 0; ei < (grassConfig.entries || []).length; ei++) {
    ent = grassConfig.entries[ei]
    secs = ent.regrowSeconds != null ? Number(ent.regrowSeconds) : 60
    for (bi = 0; bi < (ent.blocks || []).length; bi++) {
      blk = ent.blocks[bi]
      console.info(
        '[grass_harvest]   "' +
          blk +
          '" → отрастание через ' +
          secs +
          ' с реального времени (~' +
          (secs / 60).toFixed(2) +
          ' мин)'
      )
    }
  }
  if (grassConfig.debug) {
    for (bi = 0; bi < ids.length; bi++) {
      blk = ids[bi]
      var rule = blockRuleLookup[blk]
      if (rule) {
        console.info(
          '[grass_harvest] таблица [' +
            blk +
            '] regrowTicks=' +
            rule.regrowTicks +
            ' (~' +
            rule.regrowTicks / 20 +
            ' с)'
        )
      }
    }
  }
  // Новые id блоков в JSON подхватываются в таблице; чтобы подписать на них ПКМ, выполните /kubejs reload server_scripts
})
