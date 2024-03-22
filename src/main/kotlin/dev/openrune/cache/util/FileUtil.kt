package dev.openrune.cache.util

import dev.openrune.cache.tools.CacheTool
import java.io.File

object FileUtil {

    fun getTemp() : File {
        val file = File(CacheTool.builder.cacheLocation,"/temp/")
        if(!file.exists()) file.mkdirs()
        return file
    }

    fun getTempDir(dir : String) : File {
        val file = File(CacheTool.builder.cacheLocation,"/temp/${dir}/")
        if(!file.exists()) file.mkdirs()
        return file
    }

}