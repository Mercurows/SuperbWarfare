package com.atsuishio.superbwarfare.data

import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.nbt.*
import net.minecraft.network.FriendlyByteBuf
import java.io.DataInput
import java.io.DataOutput

object IncrementalTagUpdater {

    fun test() {
        val result = listOf(
            // 0
            compare(CompoundTag().apply {
                putInt("aaa", 114)
                putInt("bbb", 114)
            }, CompoundTag().apply {
                putInt("aaa", 114)
                putInt("bbb", 114)
            }),

            // 1
            compare(CompoundTag().apply {
                putInt("aaa", 114)
                putInt("bbb", 114)
            }, CompoundTag().apply {
                putInt("aaa", 114)
                putInt("bbb", 514)
            }),

            // 2
            compare(CompoundTag().apply {
                putInt("aaa", 114)
            }, CompoundTag().apply {
                putInt("aaa", 114)
                putInt("bbb", 514)
            }),

            // 3
            compare(CompoundTag().apply {
                putInt("aaa", 114)
            }, CompoundTag().apply {
                putInt("aaa", 514)
                putInt("bbb", 1919)
            }),

            // 4
            compare(CompoundTag().apply {
                putInt("aaa", 114)
                putInt("bbb", 514)
            }, CompoundTag().apply {
                putInt("aaa", 114)
            }),

            // 5
            compare(CompoundTag().apply {
                putInt("aaa", 114)
            }, CompoundTag().apply {
                put("aaa", CompoundTag().apply {
                    putInt("bbb", 114)
                })
            }),

            // 6
            compare(CompoundTag().apply {
                put("aaa", CompoundTag().apply {
                    putInt("bbb", 514)
                })
            }, CompoundTag().apply {
                put("aaa", CompoundTag().apply {
                    putInt("bbb", 114)
                })
            }),

            // 7
            compare(CompoundTag().apply {
                put("aaa", CompoundTag().apply {
                    putInt("bbb", 514)
                })
            }, CompoundTag().apply {
                put("aaa", CompoundTag().apply {
                    putString("bbb", "diffType")
                })
            }),

            // patch apply test
            // 8
            compare(
                CompoundTag().apply {
                    put("aaa", CompoundTag().apply {
                        putInt("bbb", 514)
                    })
                }, updateTag(
                    CompoundTag(),
                    listOf(
                        Patch(Operation.ADD, ArrayDeque(), "aaa", CompoundTag().apply {
                            putInt("bbb", 514)
                        }),
                    )
                )
            ),

            // 9
            compare(
                CompoundTag().apply {
                    putInt("aaa", 1)
                    putInt("bbb", 2)
                    putInt("ddd", 4)
                }, updateTag(
                    CompoundTag().apply {
                        putInt("aaa", 1)
                        putInt("bbb", 2)
                        putInt("ccc", 3)
                        putInt("ddd", 4)
                    }, listOf(Patch(Operation.REMOVE, ArrayDeque(listOf(2))))
                )
            ),

            // 10
            compare(
                CompoundTag().apply {
                    putInt("aaa", 1)
                    putInt("bbb", 514)
                }, updateTag(
                    CompoundTag().apply {
                        putInt("aaa", 1)
                        putInt("bbb", 2)
                    }, listOf(Patch(Operation.UPDATE, ArrayDeque(listOf(1)), value = IntTag.valueOf(514)))
                )
            ),
        )

        println(result)

        fun testPatchSerialize(generatePatch: () -> TagPatch) {
            val buf = FriendlyByteBuf(Unpooled.buffer())

            val originalPatch = generatePatch()
            buf.writeTagPatch(originalPatch)
            val newPatch = FriendlyByteBuf(buf.copy()).readTagPatch()

            require(newPatch == originalPatch)
        }

        // t11
        testPatchSerialize {
            TagPatch(listOf(Patch(Operation.ADD, name = "aaa", value = CompoundTag().apply {
                putInt("bbb", 514)
            })))
        }

        // t12
        testPatchSerialize {
            TagPatch(listOf(Patch(Operation.UPDATE, ArrayDeque(listOf(11)), value = IntTag.valueOf(114))))
        }

        // t13
        testPatchSerialize {
            TagPatch(listOf(Patch(Operation.UPDATE, ArrayDeque(listOf(11)), value = ListTag().apply {
                add(IntTag.valueOf(114))
                add(IntTag.valueOf(514))
                add(IntTag.valueOf(1919))
            })))
        }

        // t14
        testPatchSerialize {
            TagPatch(listOf(Patch(Operation.REMOVE, ArrayDeque(listOf(11)))))
        }
    }

    fun compare(oldTag: CompoundTag, newTag: CompoundTag) = mutableListOf<Patch>()
        .apply {
            recursiveCompare(ArrayDeque(), this, oldTag, newTag)
        }.toList()

    private fun recursiveCompare(
        path: ArrayDeque<Int>,
        diffList: MutableList<Patch>,
        oldTag: CompoundTag,
        newTag: CompoundTag
    ) {
        val sortedOldKeys = oldTag.allKeys.toSortedSet()
        val sortedNewKeys = newTag.allKeys.toSortedSet()

        // 更新顺序：先更新已有键，再移除多余键，最后添加缺失键（这一步无需index）
        // 避免更新过程中修改原Tag键数量
        for (key in sortedNewKeys) {
            val newValue = newTag.get(key)!!
            val oldValue = oldTag.get(key) ?: continue  // 新增的键由后面单独处理
            if (oldValue == newValue) continue

            path += sortedNewKeys.indexOf(key)

            if (newValue is CompoundTag) {
                if (oldValue is CompoundTag) {
                    recursiveCompare(path, diffList, oldValue, newValue)
                } else {
                    diffList.add(Patch(Operation.UPDATE, ArrayDeque(path), value = newValue))
                }
            } else {
                diffList.add(Patch(Operation.UPDATE, ArrayDeque(path), value = newValue))
            }

            path.removeLast()
        }

        // 移除多余键
        (sortedOldKeys - sortedNewKeys).forEach { key ->
            path += sortedOldKeys.indexOf(key)
            diffList.add(Patch(Operation.REMOVE, ArrayDeque(path)))
            path.removeLast()
        }

        // 添加缺失键
        (sortedNewKeys - sortedOldKeys).forEach { key ->
            val value = newTag.get(key)!!
            diffList.add(Patch(Operation.ADD, ArrayDeque(path), name = key, value = value))
        }
    }

    fun updateTag(tag: CompoundTag, patches: List<Patch>): CompoundTag {
        tailrec fun applyPatch(tag: CompoundTag, patch: Patch) {
            val orderedKeys = tag.allKeys.sorted()
            val path = patch.path
            val op = patch.operation

            // 递归解析path
            if (op == Operation.ADD && path.isNotEmpty() || path.size > 1) {
                val tagIndex = path.removeFirst()
                val tagName = orderedKeys.getOrNull(tagIndex) ?: error("Invalid path: $path")
                val newTag = tag.get(tagName) as? CompoundTag ?: error("Invalid path: $path")

                applyPatch(newTag, patch)
                return
            }

            // 添加
            if (op == Operation.ADD) {
                val key = patch.name!!
                tag.put(key, patch.value!!)
                return
            }

            val index = path.first()
            val key = orderedKeys[index]

            if (op == Operation.REMOVE) {
                tag.remove(key)
            } else {
                tag.put(key, patch.value!!)
            }
        }

        patches.sortedBy { it.operation.ordinal }.forEach {
            applyPatch(tag, it)
        }

        return tag
    }

    fun FriendlyByteBuf.writeTagPatch(patch: TagPatch) {
        writeVarInt(patch.patches.size)

        patch.patches.forEach {
            writeByte(it.operation.ordinal)
            writeIntIdList(IntList.of(*it.path.toIntArray()))

            if (it.operation == Operation.ADD) {
                writeUtf(it.name!!)
            }

            if (it.operation != Operation.REMOVE) {
                val tag = it.value!!
                writeByte(tag.id.toInt())
                tag.write(ByteBufWrapper(this))
            }
        }
    }

    fun FriendlyByteBuf.readTagPatch(): TagPatch {
        val size = readVarInt()

        return TagPatch(buildList {
            repeat(size) {
                val op = Operation.entries[readByte().toInt()]
                val path = readIntIdList()

                var name: String? = null
                var tag: Tag? = null

                if (op == Operation.ADD) {
                    name = readUtf()
                }

                if (op != Operation.REMOVE) {
                    val tagType = readByte().toInt()
                    tag = TagTypes.getType(tagType)
                        .load(ByteBufWrapper(this@readTagPatch), 0, NbtAccounter.UNLIMITED)
                }

                add(Patch(op, ArrayDeque(path), name, tag))
            }
        })
    }

    private class ByteBufWrapper(val buf: FriendlyByteBuf) : DataInput, DataOutput {
        override fun readFully(b: ByteArray) {
            repeat(b.size) {
                b[it] = readByte()
            }
        }

        override fun readFully(b: ByteArray, off: Int, len: Int) {
            repeat(len) {
                b[it + off] = readByte()
            }
        }

        override fun skipBytes(n: Int): Int {
            buf.skipBytes(n)
            return n
        }

        override fun readBoolean() = buf.readBoolean()
        override fun readByte() = buf.readByte()
        override fun readUnsignedByte() = buf.readUnsignedByte().toInt()
        override fun readShort() = buf.readShort()
        override fun readUnsignedShort() = buf.readUnsignedShort()
        override fun readChar() = buf.readChar()
        override fun readInt() = buf.readVarInt()
        override fun readLong() = buf.readVarLong()
        override fun readFloat() = buf.readFloat()
        override fun readDouble() = buf.readDouble()

        override fun readLine(): String {
            error("unsupported operation!")
        }

        override fun readUTF(): String = buf.readUtf()

        override fun write(b: Int) {
            buf.writeVarInt(b)
        }

        override fun write(b: ByteArray) {
            buf.writeByteArray(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            buf.writeByteArray(b.slice(off..<off + len).toByteArray())
        }

        override fun writeBoolean(v: Boolean) {
            buf.writeBoolean(v)
        }

        override fun writeByte(v: Int) {
            buf.writeByte(v)
        }

        override fun writeShort(v: Int) {
            buf.writeShort(v)
        }

        override fun writeChar(v: Int) {
            buf.writeChar(v)
        }

        override fun writeInt(v: Int) {
            buf.writeVarInt(v)
        }

        override fun writeLong(v: Long) {
            buf.writeVarLong(v)
        }

        override fun writeFloat(v: Float) {
            buf.writeFloat(v)
        }

        override fun writeDouble(v: Double) {
            buf.writeDouble(v)
        }

        override fun writeBytes(s: String) = writeUTF(s)
        override fun writeChars(s: String) = writeUTF(s)
        override fun writeUTF(s: String) {
            buf.writeUtf(s)
        }

    }

    enum class Operation {
        UPDATE,
        REMOVE,
        ADD,
    }

    data class TagPatch(
        val patches: List<Patch>,
    )

    data class Patch(
        val operation: Operation,
        val path: ArrayDeque<Int> = ArrayDeque(),
        val name: String? = null,
        val value: Tag? = null,
    )
}