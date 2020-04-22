/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.crypto.keyDerivation

import android.content.res.Resources
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.stream.bytes16ToUuid
import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.security.SecureRandom
import java.util.*

class Argon2Kdf internal constructor() : KdfEngine() {

    override val defaultParameters: KdfParameters
        get() {
            val p = KdfParameters(uuid!!)

            p.setParamUUID()
            p.setUInt32(PARAM_PARALLELISM, DEFAULT_PARALLELISM)
            p.setUInt64(PARAM_MEMORY, DEFAULT_MEMORY)
            p.setUInt64(PARAM_ITERATIONS, DEFAULT_ITERATIONS)
            p.setUInt32(PARAM_VERSION, MAX_VERSION)

            return p
        }

    override val defaultKeyRounds: Long
        get() = DEFAULT_ITERATIONS

    init {
        uuid = CIPHER_UUID
    }

    override fun getName(resources: Resources): String {
        return resources.getString(R.string.kdf_Argon2)
    }

    @Throws(IOException::class)
    override fun transform(masterKey: ByteArray, p: KdfParameters): ByteArray {

        val salt = p.getByteArray(PARAM_SALT)
        val parallelism = UnsignedInt(p.getUInt32(PARAM_PARALLELISM))
        val memory = UnsignedInt.fromLong(p.getUInt64(PARAM_MEMORY) / MEMORY_BLOCK_SIZE)
        val iterations = UnsignedInt.fromLong(p.getUInt64(PARAM_ITERATIONS))
        val version = UnsignedInt(p.getUInt32(PARAM_VERSION))
        val secretKey = p.getByteArray(PARAM_SECRET_KEY)
        val assocData = p.getByteArray(PARAM_ASSOC_DATA)

        return Argon2Native.transformKey(masterKey,
                salt,
                parallelism,
                memory,
                iterations,
                secretKey,
                assocData,
                version)
    }

    override fun randomize(p: KdfParameters) {
        val random = SecureRandom()

        val salt = ByteArray(32)
        random.nextBytes(salt)

        p.setByteArray(PARAM_SALT, salt)
    }

    override fun getKeyRounds(p: KdfParameters): Long {
        return p.getUInt64(PARAM_ITERATIONS)
    }

    override fun setKeyRounds(p: KdfParameters, keyRounds: Long) {
        p.setUInt64(PARAM_ITERATIONS, keyRounds)
    }

    override val minKeyRounds: Long
        get() = MIN_ITERATIONS

    override val maxKeyRounds: Long
        get() = MAX_ITERATIONS

    override fun getMemoryUsage(p: KdfParameters): Long {
        return p.getUInt64(PARAM_MEMORY)
    }

    override fun setMemoryUsage(p: KdfParameters, memory: Long) {
        p.setUInt64(PARAM_MEMORY, memory)
    }

    override val defaultMemoryUsage: Long
        get() = DEFAULT_MEMORY

    override val minMemoryUsage: Long
        get() = MIN_MEMORY

    override val maxMemoryUsage: Long
        get() = MAX_MEMORY

    override fun getParallelism(p: KdfParameters): Long {
        return UnsignedInt(p.getUInt32(PARAM_PARALLELISM)).toLong()
    }

    override fun setParallelism(p: KdfParameters, parallelism: Long) {
        p.setUInt32(PARAM_PARALLELISM, UnsignedInt.fromLong(parallelism))
    }

    override val defaultParallelism: Long
        get() = DEFAULT_PARALLELISM.toLong()

    override val minParallelism: Long
        get() = MIN_PARALLELISM

    override val maxParallelism: Long
        get() = MAX_PARALLELISM

    companion object {

        val CIPHER_UUID: UUID = bytes16ToUuid(
                byteArrayOf(0xEF.toByte(),
                        0x63.toByte(),
                        0x6D.toByte(),
                        0xDF.toByte(),
                        0x8C.toByte(),
                        0x29.toByte(),
                        0x44.toByte(),
                        0x4B.toByte(),
                        0x91.toByte(),
                        0xF7.toByte(),
                        0xA9.toByte(),
                        0xA4.toByte(),
                        0x03.toByte(),
                        0xE3.toByte(),
                        0x0A.toByte(),
                        0x0C.toByte()))

        private const val PARAM_SALT = "S" // byte[]
        private const val PARAM_PARALLELISM = "P" // UInt32
        private const val PARAM_MEMORY = "M" // UInt64
        private const val PARAM_ITERATIONS = "I" // UInt64
        private const val PARAM_VERSION = "V" // UInt32
        private const val PARAM_SECRET_KEY = "K" // byte[]
        private const val PARAM_ASSOC_DATA = "A" // byte[]

        private val MIN_VERSION = UnsignedInt(0x10)
        private val MAX_VERSION = UnsignedInt(0x13)

        private const val MIN_SALT = 8
        private val MAX_SALT = UnsignedInt.MAX_VALUE.toLong()

        private const val MIN_ITERATIONS: Long = 1L
        private const val MAX_ITERATIONS = 4294967295L

        private const val MIN_MEMORY = (1024 * 8).toLong()
        private val MAX_MEMORY = UnsignedInt.MAX_VALUE.toLong()
        private const val MEMORY_BLOCK_SIZE: Long = 1024L

        private const val MIN_PARALLELISM: Long = 1L
        private const val MAX_PARALLELISM: Long = ((1 shl 24) - 1).toLong()

        private const val DEFAULT_ITERATIONS: Long = 2L
        private const val DEFAULT_MEMORY = (1024 * 1024).toLong()
        private val DEFAULT_PARALLELISM = UnsignedInt(2)
    }
}
