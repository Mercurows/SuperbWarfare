package com.atsuishio.superbwarfare.subweapon

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.data.gun.value.ReloadState
import com.atsuishio.superbwarfare.data.stack.GunStackStorage
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.BY_UUID
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.applyBaselineId
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.onReloadStarted
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime.tick
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 副武器的运行时。
 *
 * 「寄生 GunData」的全部要点都在这里：
 *
 * | 要素 | 做法 |
 * |---|---|
 * | 物品 | 副武器物品**自己**（`SubWeaponItem : GunItem, AttachmentProvider`） |
 * | 合成栈 | `ItemStack(subWeaponItem, 1, rootTag)` —— 根 tag 就是主武器 NBT 里那个附件子 tag 的**活引用** |
 * | 数据基线 | **配件定义说了算**：`SubWeaponInfo.Data` 非空就用它，否则用附件自己的注册 id（`sbw/guns/<id>.json` 与配件同名成对出现）。落成 tag 上的 `defaultDataId`，见 [applyBaselineId] |
 * | 状态 | 弹药/热量/换弹/耐久/revision 全部写在这份共享 tag 上 → **随主武器 NBT 持久化**，无新存档字段 |
 * | 实例身份 | 缓存表（见 [BY_UUID]）持有合成栈的强引用，否则会掉出 `GunData.DATA_CACHE`（weakKeys） |
 * | tick | 合成栈不在背包里，`GunItem.inventoryTick` 不会跑 → 主武器 gun tick 里顺带 tick（见 [tick]） |
 * | 开火/换弹/瞄准 | **四期起不再有任何副武器专用链路**：副武器被 G 切出来之后就是"当前操控的枪"（见 `ActiveGun`），开火走 `FireKeyMessage`、换弹走 `ReloadMessage`，与主武器**完全同一个入口** |
 *
 * **客户端与服务端用同一套装配规则**：`GunData` 的同步会把主武器整份 tag 推给客户端，
 * 附件子 tag 也在里面，所以两边都能解出同一份副武器状态。
 *
 * ## 三条不变量（这个类坏过的每一次都踩在这里）
 *
 * **① 一份 tag 只能有一个 [Instance] / 一个 `GunData`。**
 * `GunData.state` 是"解码一次就缓存"的镜像，`save()` 会把**整份**状态写回 tag；
 * 两个实例同时写同一份 tag 就会互相把对方的改动盖回去 ——
 * 表现是"装填走完提示了却没装进去""按一下 G 装填计时被打回满值""像是两条装填并行在跑"。
 * 所以缓存命中判定必须稳，重建必须罕见（见 ②）。
 *
 * **② 载体引用必须全程不变。**
 * `GunData` 构造时把根 tag 与它的子 compound 全部捕获成 `val`，所以 tag 一换，
 * 这个 [Instance] 就永久脱钩了。而主武器的 `GunData.rebind` 走 `clearTag + merge`：
 * 被清空的键会以 `tag.copy()` 重新落进去（`CompoundTag.merge` 对"原来不存在"的键是复制），
 * **每次 resync 之后附件子 tag 都是新实例**。
 * 现在遇到副本不再重建，而是把新内容折进手里那份再把手里那份**挂回槽位**
 * （`Attachment.setTag`），引用、`GunData`、[Instance] 三者全程不变。
 * **四期起这条不变量的判据从"`liveTag` 引用"升级成"载体令牌"**（[GunStackStorage.Carrier]），
 * 这样 1.21.1 分支换成 DataComponent 存储时这段逻辑不用重写。
 *
 * **③ 客户端和服务端各自一份缓存。**
 * 单人游戏里两边在同一个 JVM、同一份静态表，主武器 UUID 也相同；
 * 混用一张表会让两边轮流把对方的 [Instance] 挤掉（每次挤掉都重置下面的
 * [Instance.wasReloading]，于是装填音效又响一次）。所以缓存按 `clientSide` 分开。
 */
object SubWeaponRuntime {

    /**
     * 一把已装配的副武器。
     *
     * @param slot 它在主武器上的槽位（也是报文里用的标识：[slotName]）
     * @param attachmentId 配件数据 id（与 [stack] 的 tag 里的 `Id` 对应）
     * @param info 配件上的 `SubWeapon` 定义
     * @param clientSide 这份实例属于哪一侧（缓存键的一半，见不变式 ③）
     * @param stack 合成栈；持有强引用是**有意的**（见类的 KDoc）
     * @param carrier 合成栈根 tag 的**数据载体**（根 tag + 身份令牌）。
     *   **不能靠 `stack.tag` 反查**：缓存命中判定必须比较这一份我们亲手存下来的载体，
     *   否则 `ItemStack` 内部一旦不是"原样持有"（复制 / 规整 / 被 `GunData` 换过），
     *   判定就会永远为假 —— 表现为每 tick 重建一次 `GunData`，副武器状态机被反复清零。
     * @param data 合成栈对应的枪械数据
     */
    class Instance(
        val slot: AttachmentType,
        val attachmentId: ResourceLocation,
        val info: SubWeaponInfo,
        val clientSide: Boolean,
        val stack: ItemStack,
        val carrier: GunStackStorage.Carrier,
        val data: GunData,
    ) {
        /**
         * 上一 tick 是否在装填。
         *
         * 四期起它只用来捕捉"装填开始/结束"这**一瞬间**（完成音效）——
         * 自动装填已经删除，玩家自己按 R，与主武器完全一致。
         *
         * ⚠ 这仍然是**跨 tick 的记忆**，也是"实例必须复用"的另一个理由：实例一换，
         * 这个标记退回 `false`，新实例第一次 tick 看到的恰好是"正在装填"，
         * 于是又报一次"装填开始" —— 听感就是换弹音效一直响。
         */
        var wasReloading: Boolean = false

        /** 报文与冷却键里用的槽位标识 */
        val slotName: String get() = slot.name

        /**
         * 这份副武器实际使用的**枪数据 id**（`SubWeapon.Data`，不写就是附件自己的注册 id）。
         *
         * 它会在装配时写进合成栈的枪械状态（[applyBaselineId]），所以调试时对不上账就查这里：
         * 写的 id 在 `sbw/guns` 里不存在的话，`GunData` 会退回一份空基线（既打不出也装不上弹）。
         */
        val baselineId: String get() = baselineIdOf(attachmentId, info)

        /** 根 tag（活引用）；等价于老的 `liveTag` */
        val root: CompoundTag get() = carrier.root

        /** 载体令牌（不变式 ② 的判据） */
        val token: Long get() = carrier.token
    }

    /** 按 `(客户端?, 主武器 UUID)` 缓存；主武器还没拿到 UUID 时退回按 [GunData] 身份缓存 */
    private data class CacheKey(val clientSide: Boolean, val uuid: UUID)

    /**
     * 缓存键的一半是"哪一侧"，取值本身是线程安全的（单人游戏里客户端线程与服务端线程
     * 会同时读写这张表；桶内则只会被对应那一侧碰）。
     */
    private val BY_UUID = ConcurrentHashMap<CacheKey, MutableMap<AttachmentType, Instance>>()

    /** 没有 UUID 时的兜底表；同样按侧分开（单人游戏里客户端与服务端是两个 `GunData`） */
    private val BY_IDENTITY = arrayOf(
        WeakHashMap<GunData, MutableMap<AttachmentType, Instance>>(),
        WeakHashMap<GunData, MutableMap<AttachmentType, Instance>>(),
    )

    /** [BY_UUID] 的软上限：超过就整体丢弃（重建成本很低，比无界增长好） */
    private const val MAX_CACHED_GUNS = 256

    /** 已经吼过的"缺枪数据"id，避免每 tick 刷屏 */
    private val warnedMissingBaseline: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // ------------------------------------------------------------------ 装配

    /**
     * 解析主武器上装着的全部副武器（按槽位去重，顺序 = `AttachmentType.entries`）。
     *
     * 只有**同时满足**这三条的槽位才算副武器：
     * 1. 该槽位装了配件，且配件的定义能解析出来；
     * 2. 定义里有 [SubWeaponInfo]；
     * 3. 对应物品是 [SubWeaponItem]（否则拿不到 `GunData`）。
     *
     * @param client 调用方在哪一侧。**必须显式传**：单人游戏里客户端与服务端共享静态表，
     *   用错一侧的实例会让状态在两边互相覆盖（见类 KDoc 的不变式 ③）。
     */
    @JvmStatic
    fun installed(gun: GunData, client: Boolean): List<Instance> {
        val cache = cacheOf(gun, client)
        val found = LinkedHashMap<AttachmentType, Instance>()

        for (attachment in gun.attachment.installed()) {
            val info = attachment.definition.subWeapon ?: continue
            val item = BuiltInRegistries.ITEM.get(attachment.id) as? SubWeaponItem ?: continue

            val cached = cache[attachment.slot]

            // ⚠ **我们手里那份才是"活引用"，不能每帧去枪上重新materialize。**
            //
            // 四期踩过一次（症状：客户端每帧打印一条 `re-attached ... (folded=true)`，
            // 同时"能播开火动画但打不出去"）：
            //
            // `Attachment.getOrCreateTag` 对**字符串形式**的槽位内容会
            // `CompoundTag().apply { putString("Id", ...) }` —— **每次调用都返回一个新对象**。
            // 客户端的主手物品每个 tick 都会被服务端同步换成一份**新的栈对象**，
            // 而那份栈上的槽位内容在没被实体化之前一直是字符串 → 于是客户端每一帧都：
            // 拿到一个**游离的空 compound** → 判定"槽位不是我们那份" → `foldIncoming` 拿它**清空并 merge**
            // 我们缓存的那份 → 客户端手里的弹药/换弹状态被**每帧清空一次**。
            // 表现就是"动画照播、子弹永远打不出去"（服务端那份是好的，所以服务端没报错）。
            //
            // 正确做法：**只要手里那份还带着枪械状态，就以它为准**；
            // 只有枪上那份**确实带着不同的内容**时才折；而且**绝不用一份空 compound 去覆盖有内容的**。
            val cachedRoot = cached?.root
            val incoming: CompoundTag?
            if (cachedRoot != null && carriesGunState(cachedRoot)) {
                // 尽量复用：只有枪上那份本身就是 compound 时才读它（读操作没有副作用）
                incoming = gun.attachment.getTagIfCompound(attachment.slot)
            } else {
                // 还没装配过：这一次必须把它实体化（`getOrCreateTag` 会写回枪 NBT）
                incoming = gun.attachment.getOrCreateTag(attachment.slot)
            }

            // 装配规则只有一条：**只要还是同一个配件、而且我们手里那份 tag 里有枪械状态，
            // 就永远复用同一个 [Instance]**（必要时把手里那份挂回槽位）。
            // 其余情况都是"没得复用"：槽位从没装配过 / 换成了别的副武器 / 手里那份本来就没状态。
            val instance = if (cached != null && cached.attachmentId == attachment.id) {
                // 基线 id 以**配件定义**为准：数据包改了 `SubWeapon.Data` 之后这里要跟着换，
                // 而且必须换在**同一份 tag** 上（引用、`GunData`、[Instance] 三者都不能动）。
                // 正常情况下两边一致，`setDefaultDataId` 会自己跳过写入，所以这里几乎总是空操作。
                if (cached.data.defaultDataId.get() != baselineIdOf(attachment.id, info)) {
                    applyBaselineId(cached.stack, attachment.id, info)
                    // 状态是"构造时解码一次"的镜像（见 `GunData.pullFromTag`），写完要重新解码
                    cached.data.pullFromTag()
                    debug { "baseline of ${attachment.slot} -> ${cached.baselineId}" }
                }

                // 这次调用有没有真的把外面的内容折进手里那份。
                // 单独记一笔的原因见下面 revision 检查那段注释：折进来的内容 revision 可能与手里那份相等。
                var folded = false

                if (incoming !== null && incoming !== cached.root) {
                    // 枪上那份不是我们手里这份了：可能只是**换了个副本**（主武器 rebind 过，内容一样），
                    // 也可能是**服务端真的推了新内容过来**。两者都走同一条路：
                    //
                    // - 内容一致（`foldIncoming` 自己会早退）→ 什么都不做，**也不打印日志**；
                    // - 内容不同 → 折进手里那份，再把手里那份挂回槽位 ——
                    //   tag 引用、`GunData`、[Instance] 三者全程不变。
                    //
                    // ⚠ **绝不用空 compound 覆盖有内容的**：客户端每帧都可能拿到一份
                    // 刚 materialize 出来的空 compound（见上面那段注释），
                    // 拿它去 `clear + merge` 就等于每帧把客户端的副武器状态清空一次 ——
                    // 那正是"能播开火动画但打不出去 / 换弹两边不同步"的根因。
                    if (foldIncoming(cached.root, incoming, acceptEqualRevision = client)) {
                        // 折进来的内容 revision 可能与手里那份**相等**（客户端"宁可信服务端"那条路），
                        // 所以这次折必须显式记一笔，否则下面的 revision 检查会漏掉它。
                        folded = true
                    }
                    gun.attachment.setTag(attachment.slot, cached.root)
                } else if (incoming == null) {
                    // 枪上那把枪的槽位又变回字符串了（客户端每帧一份新栈就会这样）：
                    // 把手里这份**挂回去**（不折、也不打印日志）—— 手里那份才是活引用。
                    gun.attachment.setTag(attachment.slot, cached.root)
                }

                // ⚠⚠ **不管上面折不折，只要活 tag 上的 revision 跑在 `state` 前面就必须重新解码。**
                //
                // `GunData.state` 是**懒解码一次就缓存住**的镜像（`decodedState ?: fromTag(tag)`），
                // 之后只有 `update` / `updateLocal` / `pullFromTag` / `rebind` 会换掉它 ——
                // **没有任何机制能察觉"别人直接改了这份 tag"**。
                //
                // 而副武器的 tag 恰好就有第二个写入方：单人游戏里客户端与服务端共用静态缓存表，
                // 两边解析出来的 `root` 常常**就是同一个活 tag 对象**，服务端每 tick 往里写。
                // 这时上面那个 `incoming !== cached.root` 守卫直接跳过，`foldIncoming` 一次都不会
                // 被调用 —— 只靠它来触发重新解码就等于**永远不解码**。
                //
                // 症状（四期实际踩到）：客户端的副武器状态冻结在装配那一刻，而装配时弹匣是满的
                // → `canShoot` 恒为 true → 客户端在服务端换弹期间照播开火动画、照发包，
                // 服务端把这一发丢掉 → "能播开火动画但子弹打不出去 / 换弹两边不同步"。
                //
                // 判据用 `data.state.revision` 而不是另存一个字段：它就是"这个实例最后知道的
                // revision"，而 `GunData.persist` 在内容变化时会把这个数写进 tag。
                // 于是 `tag 上的 revision != 实例知道的 revision` 恰好等价于
                // **"有别人写了这份 tag，我落后了"**，而本实例自己写的那些 tick 不会误判
                // （批量写入把 revision 推迟到 `flush`，两边一起变，始终相等）。
                val revision = revisionOf(cached.root)
                if (folded || revision != cached.data.state.revision) {
                    cached.data.pullFromTag()
                }
                cached
            } else {
                // `incoming` 在"手里那份有状态"的分支里可能是 null（枪上还是字符串形式），
                // 走到这里说明没得复用，必须实体化一次。
                val root = incoming ?: gun.attachment.getOrCreateTag(attachment.slot)
                val assembled = assemble(attachment.slot, attachment.id, info, item, client, root)
                // 把栈自己那份活引用挂回槽位：两边从这一 tick 起就是同一个对象
                gun.attachment.setTag(attachment.slot, assembled.root)
                assembled
            }

            found[attachment.slot] = instance
        }

        cache.keys.retainAll(found.keys)
        cache.putAll(found)
        return found.values.toList()
    }

    /** 按槽位找一把副武器；`null` = 这个槽位没装副武器 */
    @JvmStatic
    fun find(gun: GunData, slot: AttachmentType, client: Boolean): Instance? =
        installed(gun, client).firstOrNull { it.slot == slot }

    /** 按报文里的槽位标识找（`SUBWEAPON` 这样的枚举名） */
    @JvmStatic
    fun find(gun: GunData, slotName: String, client: Boolean): Instance? =
        installed(gun, client).firstOrNull { it.slotName == slotName }

    /**
     * 真正新建一份 [Instance]：合成栈的根 tag **就是** [root] 的内容，二者必须是同一个对象。
     *
     * `GunData` 构造时会把根 tag 与它的子 compound 全部**捕获成 `val`**，而 [Instance.carrier]
     * 也要拿同一份 —— 只要栈里挂的是副本，副武器的所有状态都会写进一个和主武器 NBT 无关的角落
     * （症状：能切出来、能瞄准，但装填与开火全部无效）。
     *
     * 所以先 [GunStackStorage.writeRoot] 把内容折进栈自己那份（必要时才创建），**再读回来**当载体：
     * 栈里那份才是活引用，[root] 只是内容来源。
     */
    private fun assemble(
        slot: AttachmentType,
        attachmentId: ResourceLocation,
        info: SubWeaponInfo,
        item: SubWeaponItem,
        client: Boolean,
        root: CompoundTag,
    ): Instance {
        val stack = ItemStack(item, 1)
        GunData.stackStorage.writeRoot(stack, root)

        val liveRoot = GunData.stackStorage.rootTag(stack)

        // ⚠ 必须在 `GunData.from(stack)` **之前**：`GunData` 构造时就把 `defaultDataId` 解码进状态
        applyBaselineId(stack, attachmentId, info)

        val carrier = GunStackStorage.Carrier(liveRoot, GunStackStorage.tokenOf(liveRoot))
        val instance = Instance(slot, attachmentId, info, client, stack, carrier, GunData.from(stack))
        warnIfNoBaseline(instance)

        return instance
    }

    /** 副武器实际使用的枪数据 id：`SubWeapon.Data` 非空就用它，否则回落到附件自己的注册 id */
    @JvmStatic
    fun baselineIdOf(attachmentId: ResourceLocation, info: SubWeaponInfo): String =
        info.data?.takeIf { it.isNotBlank() } ?: attachmentId.toString()

    /**
     * 把枪数据 id 写进合成栈（`SubWeaponInfo.Data` 的落地）。
     *
     * 走 [GunData.setDefaultDataId]：它把 id 盖在枪械状态子 tag 上，之后 `GunData.getDefault()`
     * 就按这个 id 去 `CustomData.GUN_DATA`（`sbw/guns/<id>.json`）里取基线 ——
     * 与载具武器共用同一个物品 id 时那一套机制完全相同，不新增任何存档字段。
     *
     * 那份子 tag **就是主武器 NBT 里的附件子 tag**，所以 id 随主武器持久化、也随同步到达客户端，
     * 两边解出的是同一份基线（否则客户端与服务器的弹药/弹道会各算各的）。
     */
    private fun applyBaselineId(stack: ItemStack, attachmentId: ResourceLocation, info: SubWeaponInfo) {
        GunData.setDefaultDataId(stack, baselineIdOf(attachmentId, info))
    }

    /**
     * 把 [source] 折进 [target]（先清空再 merge，与 `GunData.reloadTagFrom` 同一套做法）。
     *
     * @param acceptEqualRevision `true` = 客户端：两边 revision **一样**时也接受同步过来的内容。
     *
     *   为什么需要它：`GunState.revision` **只在枪械状态变化时推进**。副武器的装填计时器每 tick 都在动，
     *   但 `persist` 只在内容真的变了时 bump —— 两边时序不同步时很容易出现
     *   "服务端 revision 已经 +1、客户端还是旧值"之外的**相反**情况：客户端手里那份 revision 更大
     *   （它自己接受过更新的同步内容），于是服务端的正常同步被当成"旧快照"**全部丢掉**，
     *   客户端永远停在那一刻 —— 表现就是"换弹进度两边不同步 / 打不出去"。
     *
     *   所以客户端的方向是"**宁可信服务端**"：只要不是**明确更旧**（revision 更小），就折。
     *   服务端反过来（`acceptEqualRevision = false`）：手里那份才是权威，只在副本确实更新时才折。
     *
     * @return 是否真的折了内容（折了的话调用方**必须**让对应的 `GunData` 重新解码状态，
     *   见 `GunData.pullFromTag`）。⚠ 返回 `false` **不等于"没有变化"** —— 上面每一条早退
     *   都可能是"内容其实一样"也可能是"这份来源不该采信"，而"同一个活 tag 被对面写了"
     *   这种最常见的情况根本走不到这里。所以调用方判断要不要重新解码**不能只看这个返回值**，
     *   见 `installed` 里那段 revision 检查。
     */
    private fun foldIncoming(
        target: CompoundTag,
        source: CompoundTag,
        acceptEqualRevision: Boolean,
    ): Boolean {
        if (target === source) return false
        if (target == source) return false

        // ⚠ **绝不用空/无状态的来源覆盖有内容的自己**。
        //
        // 客户端每帧都可能拿到一份刚 materialize 出来的空 compound（见 `installed` 里的长注释），
        // 一旦折进来就等于把客户端的副武器状态（弹药/换弹计时器）**清空**，
        // 表现是"开火动画照播、子弹永远打不出去"。服务端手里那份是权威，
        // 客户端手里那份同样是**已经从同步内容解出来的完整视图**，两者都不该被空快照冲掉。
        if (!target.isEmpty && !carriesGunState(source)) return false

        val sourceRevision = revisionOf(source)
        val targetRevision = revisionOf(target)
        if (sourceRevision < targetRevision) return false
        if (!acceptEqualRevision && !GunState.isNewerRevision(sourceRevision, targetRevision)) return false

        // ⚠⚠ **只能在保住对象身份的前提下合并** —— 不能先把键删光，也不能直接用 `target.merge`。
        //
        // 这里原本是 `for (key in target.allKeys) target.remove(key)` 再 `merge` —— 看着像
        // "先清空再合并"，实际上是个**对象身份杀手**：vanilla 的 `CompoundTag.merge` 在
        // **两边都有**这个键时是**递归合并**（保住嵌套 compound 的对象），只有键不存在时才
        // `put(key, value.copy())`。先把键删光，就让**每一个**键都落进后面那条分支 ——
        // `root.GunData` 被整整换成一个新副本。
        //
        // 为什么致命：`GunData` 构造时把根 tag 与它的子 compound 全部**捕获成 `val`**
        // （`tag` / `gunDataTag` / `perkTag` / `attachmentTag`），子数据处理器也都握着这些引用。
        // 子 compound 一被换掉，`cached.data` 就与活 tag **脱钩**：
        //
        // - `revisionOf(cached.root)` 读的是**新**副本 → 一路涨；
        // - `cached.data.pullFromTag()` 解码的是**旧**那个 → `state` 冻死。
        //
        // 四期实测（双端探针日志）：客户端 `root rev` 从 21 稳定跟到 69、与服务端完全一致，
        // 而 `stateRev` 冻在 20 不动 —— tag 同步一直是好的，是**解码解码错了对象**。
        // 表现就是弹药永不消耗（"能播开火动画但打不出去"）、换弹进度两边不同步。
        //
        // 但也不能图省事直接用 `target.merge(source)`：它**只增不删**，而 `GunState.writeInto`
        // 恰恰用"键消失"表达"这个字段回到默认值"（`encodeDefaults = false`）——
        // 少了删除这一步，`Ammo` 从 5 回到 0 时会留在 target 里变成 5。
        // 所以两件事都要做，且删除必须**递归到每一层**。
        GunStackStorage.mergePreservingIdentity(target, source)

        return true
    }

    /** 根 tag 里的枪械状态子 tag；`null` = 这个槽位还没被装配过 */
    private fun gunStateOf(tag: CompoundTag): CompoundTag? =
        if (tag.contains(GunState.KEY_GUN_DATA, Tag.TAG_COMPOUND.toInt())) {
            tag.getCompound(GunState.KEY_GUN_DATA)
        } else {
            null
        }

    /** 根 tag 里记录的枪械状态 revision（没有状态就是 0） */
    private fun revisionOf(tag: CompoundTag): Long =
        gunStateOf(tag)?.getLong(GunState.KEY_REVISION) ?: 0L

    private fun carriesGunState(tag: CompoundTag): Boolean = gunStateOf(tag) != null

    // ------------------------------------------------------------------ tick

    /**
     * 主武器 tick 时顺带 tick 全部副武器。
     *
     * 三个前提都必须成立：
     * - **只在服务端**推进（客户端的副武器状态跟着主武器 tag 同步过来，自己再推会打架）；
     * - 主武器自己**不是副武器**（否则一把副武器上再装副武器会无限递归）；
     * - 主武器身上**确实有附件**（便宜的前置过滤，绝大多数枪直接跳过装配流程）。
     *
     * **装填与栓动的进度只在"这把副武器正被操控"时推进** —— 与主武器完全同一条口径：
     * 主武器的换弹计时器也在 `gunTickInternal` 的 `inMainHand` 分支里，切枪时
     * `LivingEventHandler` 还会把计时器与状态一起清掉。副武器照做：没被切出来/宿主枪不在主手时
     * **直接中断装填**（进度归零），切回来从头装。
     *
     * 与持有无关的（热量、冷却、perk、各种计时器）照常推进：把它们也绑在 `inMainHand` 上，
     * 副武器的状态机就会在那段判定为假时被整段冻住（换弹计时器停在同一 tick、`canShoot` 永远 false）。
     *
     * @param deployedSlot 这次 tick 期间**被切出来**的槽位（`ActiveGun` 解析结果）；
     *   `null` = 这把主武器当前操控的是它自己
     * @param inMainHand 宿主枪是不是正在主手（`GunEventHandler.gunTickInternal` 的入参）
     */
    @JvmStatic
    @JvmOverloads
    fun tick(shooter: Entity?, gun: GunData, deployedSlot: AttachmentType? = null, inMainHand: Boolean = true) {
        if (shooter == null) return
        if (shooter.level().isClientSide) return
        if (gun.item is SubWeaponItem) return
        if (gun.attachmentTag.isEmpty) return

        val instances = installed(gun, client = false)
        if (instances.isEmpty()) return

        for (instance in instances) {
            val sub = instance.data
            val reloadingBefore = instance.wasReloading

            // 这把副武器是不是"当前操控的枪"：宿主枪在手上 **且** 切出来的就是它。
            // 只有这时装填/栓动的进度才往前走 —— 与主武器 `if (inMainHand)` 完全同一条口径。
            val operated = inMainHand && deployedSlot == instance.slot

            // ⓪ 没被操控：**中断装填**（进度归零），而不是让它在背包里自己走完。
            if (!operated) {
                interruptReload(sub)
            }

            // ① 推进状态机：入参就是"这把副武器有没有被操控" —— 装填/栓动只在被操控时走
            GunEventHandler.gunTick(shooter, sub, inMainHand = operated)

            // ② 处理"开始 / 结束"这两个跳变（被中断的那一次不算"完成"）
            val reloadingNow = sub.reloading()
            when {
                !operated && reloadingBefore ->
                    debug { "reload of ${instance.slotName} interrupted (no longer the active gun)" }

                !reloadingBefore && reloadingNow -> onReloadStarted(shooter, instance)
                reloadingBefore && !reloadingNow -> onReloadFinished(shooter, instance)
            }
            instance.wasReloading = reloadingNow
        }
    }

    /**
     * 中断装填：换弹状态与计时器清干净，**栓动计时器一起清**。
     *
     * 与主武器切枪时 `LivingEventHandler` 做的是同一件事（`reload.setTime(0)` +
     * `NOT_RELOADING` + 单发装填的各阶段计时器 + `bolt.actionTimer.reset()`），
     * 于是"切走再切回来"是从 0 重新装，而不是接着上次的进度。
     *
     * `reloadStarter` 不用手动清：它只在"标记了、但还没被 `gunTick` 消费"的那一 tick 里为真，
     * 留着它反而正好让切回来的第一 tick 就重新起步。
     *
     * 没有任何东西在走时直接返回 —— 背包里躺着不动的枪每 tick 都会走到这里。
     */
    @JvmStatic
    fun interruptReload(data: GunData) {
        if (!data.reloading() && data.bolt.actionTimer.get() == 0) return

        data.reload.setTime(0)
        data.reload.setState(ReloadState.NOT_RELOADING)

        // 单发装填（`ReloadTypes: ["Iterative"]`）自己的阶段计时器也要一起清，否则会留下半截状态
        if (data.get(GunProp.ITERATIVE_TIME) != 0) {
            data.stopped.set(false)
            data.forceStop.set(false)
            data.reload.setStage(0)
            data.reload.prepareTimer.reset()
            data.reload.prepareLoadTimer.reset()
            data.reload.iterativeLoadTimer.reset()
            data.reload.finishTimer.reset()
        }

        if (data.get(GunProp.BOLT_ACTION_TIME) > 0) {
            data.bolt.actionTimer.reset()
        }

        data.invalidateProperties()
    }

    /**
     * 换弹开始的那一瞬间。
     *
     * 音效走**配件数据自己的** `ReloadSound`。
     *
     * 四期它为什么**曾经**是唯一来源：换弹动画归副武器自己的资源
     * （`sbw/guns/<id>.json` 的 `Animation.Reload`，见设计文档 §9.8.7），
     * 而当时那条附件动画播放链路**不接 `sound_effects` 关键帧** ——
     * 写在副武器动画里的音效不会响。
     *
     * **五期起这条理由不再成立**：附件播放链路（[GeoGunAnimationInstance.subWeaponReloadRunner]）
     * 现在也收 `sound_effects` 了，和主武器同一支 `collectSoundEvents`（只在**主手**实例上收，
     * 见其构造参数 `hand`）。于是这里降级为**兜底**：`ReloadSound` 在服务端也会响、
     * 不依赖动画进度，而动画里的关键帧对得上时间轴。**两边都写会响两遍**，
     * 见 [SubWeaponInfo.reloadSound] 的说明。
     *
     * 这个回调只应该在"真的开始了一次装填"时响一次 —— 它重复触发意味着实例/状态被重建过
     * （见类 KDoc 的不变式 ①②）。
     */
    private fun onReloadStarted(shooter: Entity?, instance: Instance) {
        debug { "reload started: ${instance.slotName}" }

        val player = shooter as? ServerPlayer ?: return
        val sound = instance.info.reloadSound ?: return
        player.playLocalSound(sound, RELOAD_SOUND_VOLUME, 1f)
    }

    /**
     * 换弹结束的那一瞬间。
     *
     * 四期起**不再**发完成提示：副武器就是"当前操控的枪"，
     * 它的弹药变化由常规 HUD（弹药条/弹药数）直接体现，与其它枪完全一致 ——
     * 三期那三条动作栏提示（`info.superbwarfare.subweapon.*`）连同自动装填一起删除了。
     *
     * 完成音效仍然保留，理由与 [onReloadStarted] 同一条。
     */
    private fun onReloadFinished(shooter: Entity?, instance: Instance) {
        val sub = instance.data
        val ammo = sub.ammo.get()

        debug { "reload finished: ${instance.slotName} ammo=$ammo/${sub.get(GunProp.MAGAZINE)}" }

        val player = shooter as? ServerPlayer ?: return
        instance.info.reloadEndSound?.let { player.playLocalSound(it, RELOAD_SOUND_VOLUME, 1f) }
    }

    /** 副武器换弹音效的音量（本地音，只有射手听得到） */
    private const val RELOAD_SOUND_VOLUME = 1.0f

    /**
     * 副武器**必须**能按 [Instance.baselineId] 解析到 `sbw/guns/<id>.json`。
     *
     * 解析不到时 [GunData.getDefault] 会退回一份空的 `DefaultGunData`：
     * `Magazine = 0` → `useBackpackAmmo()` 为真 → `tryStartReload` 第一行就返回（**永远装不了弹**），
     * 同时 `ProjectileAmount = 0` → `canShoot` 恒为假（**永远开不了火**）。
     * 表现是"切过去之后完全没反应"，所以这里必须吼一声，不能让它静默。
     */
    private fun warnIfNoBaseline(instance: Instance) {
        if (!instance.data.getDefault().isDefaultData) return
        if (!warnedMissingBaseline.add(instance.baselineId)) return

        Mod.LOGGER.error(
            "[SubWeapon] '{}' has no matching gun data at sbw/guns/{}.json; GunData fell back to an empty " +
                    "baseline (Magazine=0, ProjectileAmount=0), so it can neither fire nor reload. " +
                    "Either ship that file or point SubWeapon.Data at an existing gun data id.",
            instance.attachmentId, instance.baselineId.substringAfter(':'),
        )
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }

    @JvmStatic
    fun debugEnabled(): Boolean = DisplayConfig.MELEE_DEBUG_LOG.get()

    // ------------------------------------------------------------------ 缓存

    private fun cacheOf(gun: GunData, client: Boolean): MutableMap<AttachmentType, Instance> {
        val uuid = gun.uuid
        if (uuid == null) {
            return BY_IDENTITY[if (client) 1 else 0].getOrPut(gun) { HashMap() }
        }

        if (BY_UUID.size > MAX_CACHED_GUNS) {
            BY_UUID.clear()
        }
        return BY_UUID.computeIfAbsent(CacheKey(client, uuid)) { HashMap() }
    }

    /** 主武器丢失/换枪/卸载配件时清掉它的条目（目前只在调试与资源重载时用得上） */
    @JvmStatic
    fun clear(gun: GunData) {
        val uuid = gun.uuid
        if (uuid != null) {
            BY_UUID.remove(CacheKey(false, uuid))
            BY_UUID.remove(CacheKey(true, uuid))
        }
        BY_IDENTITY.forEach { it.remove(gun) }
    }

    @JvmStatic
    fun clearAll() {
        BY_UUID.clear()
        BY_IDENTITY.forEach { it.clear() }
    }
}
