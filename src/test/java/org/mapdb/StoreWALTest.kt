package org.mapdb

import org.junit.Assert.*
import org.junit.Test
import org.mapdb.StoreAccess.volume
import java.io.File
import java.io.RandomAccessFile

/**
 * Created by jan on 3/22/16.
 */
class StoreWALTest: StoreDirectAbstractTest() {

    override fun openStore(file: File): StoreWAL {
        return StoreWAL.make(file=file.path)
    }

    override fun openStore(): StoreWAL {
        return StoreWAL.make()
    }


    @Test override fun delete_after_close(){
        val dir = TT.tempDir()
        val store = StoreWAL.make(dir.path+"/aa",deleteFilesAfterClose = true)
        store.put(11, Serializer.INTEGER)
        store.commit()
        store.put(11, Serializer.INTEGER)
        store.commit()
        assertNotEquals(0, dir.listFiles().size)
        store.close()
        assertEquals(0, dir.listFiles().size)
    }

    @Test(expected=DBException.WrongConfiguration::class)
    fun checksum_disabled(){
        StoreWAL.make(checksum=true)
    }

    @Test fun no_head_checksum(){
        var store = StoreWAL.make(checksumHeader = false)
        assertEquals(0, store.volume.getInt(16)) //features
        assertEquals(0, store.volume.getInt(20)) //checksum

        store = StoreWAL.make(checksumHeader = true)
        assertEquals(1, store.volume.getInt(16)) //features
        assertNotEquals(0, store.volume.getInt(20)) //checksum

    }

    @Test fun headers2(){
        val f = TT.tempFile()
        val store = openStore(f)
        store.put(TT.randomByteArray(1000000),Serializer.BYTE_ARRAY)

        val raf = RandomAccessFile(f.path, "r");
        raf.seek(0)
        assertEquals(CC.FILE_HEADER.toInt(), raf.readUnsignedByte())
        assertEquals(CC.FILE_TYPE_STOREDIRECT.toInt(), raf.readUnsignedByte())
        assertEquals(0, raf.readChar().toInt())
        raf.close()

        val wal = RandomAccessFile(f.path + ".wal.0", "r");
        wal.seek(0)
        assertEquals(CC.FILE_HEADER.toInt(), wal.readUnsignedByte())
        assertEquals(CC.FILE_TYPE_STOREWAL_WAL.toInt(), wal.readUnsignedByte())
        assertEquals(0, wal.readChar().toInt())
        wal.close()
        f.delete()
    }

}