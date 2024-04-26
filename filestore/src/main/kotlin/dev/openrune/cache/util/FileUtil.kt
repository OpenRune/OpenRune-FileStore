package dev.openrune.cache.util

import dev.openrune.cache.Constants.builder
import java.io.File

object FileUtil {

    fun getTemp() : File {
        val file = File(builder.cacheLocation,"/temp/")
        if(!file.exists()) file.mkdirs()
        return file
    }

    fun getTempDir(dir : String) : File {
        val file = File(builder.cacheLocation,"/temp/${dir}/")
        if(!file.exists()) file.mkdirs()
        return file
    }

}