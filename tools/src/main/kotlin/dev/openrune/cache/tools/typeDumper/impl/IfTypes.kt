package dev.openrune.cache.tools.typeDumper.impl

import dev.openrune.cache.tools.typeDumper.Language
import dev.openrune.cache.tools.typeDumper.TypeDumper
import dev.openrune.cache.util.Namer
import dev.openrune.cache.util.Namer.Companion.formatForClassName
import dev.openrune.definition.Js5GameValGroup

class IfTypes(
    private val typeDumper: TypeDumper,
    private val writeToJava: Boolean
) {

    private val namer = Namer()

    fun writeInterfacesAndComponents(group : Js5GameValGroup,data: StringBuilder) {
        val writtenInterfaces = mutableSetOf<String>()

        if (typeDumper.exportSettings.useGameVal!!.combineInterfaceAndComponents) {
            val (components, pathComponents) = typeDumper.generateWriter(group)

            data.lines().forEach { line ->
                val tokens = line.split("-")
                if (tokens.size == 4) {
                    val (interfaceName, interfaceID, _, _) = tokens
                    if (!writtenInterfaces.contains(interfaceName)) {
                        writeComponentData(interfaceName, interfaceID, components, writeToJava)
                        writtenInterfaces.add(interfaceName)
                    }
                }
            }

            if (typeDumper.language == Language.RSCM) {
                data.lines().forEach { line ->
                    val tokens = line.split("-")
                    if (tokens.size == 4) {
                        val (interfaceName, interfaceID, componentName, componentID) = tokens
                        val finalComponentID = getPackedComponentID(interfaceID, componentID)
                        val componentKey = namer.name("$interfaceName.$componentName", finalComponentID, typeDumper.language, writeToJava)
                        typeDumper.write(components, typeDumper.fieldName(componentKey, finalComponentID))
                    }
                }
            } else {
                writeGroupedComponents(data, components, writeToJava)
            }

            typeDumper.endWriter(components, pathComponents)
        } else {
            val (interfaces, pathInterfaces) = typeDumper.generateWriter(override = "interfaces")
            val (components, pathComponents) = typeDumper.generateWriter(group)

            data.lines().forEach { line ->
                val tokens = line.split("-")
                if (tokens.size == 4) {
                    val (interfaceName, interfaceID, componentName, componentID) = tokens
                    if (!writtenInterfaces.contains(interfaceName)) {
                        writeComponentData(interfaceName, interfaceID, interfaces, writeToJava)
                        writtenInterfaces.add(interfaceName)
                    }

                    val finalComponentID = getPackedComponentID(interfaceID, componentID)
                    val splitter = if (typeDumper.language == Language.RSCM) "." else "_"
                    val componentKey = namer.name("${interfaceName}${splitter}${componentName}", finalComponentID, typeDumper.language, writeToJava)
                    typeDumper.write(components, typeDumper.fieldName(componentKey, finalComponentID))
                }
            }

            typeDumper.endWriter(interfaces, pathInterfaces)
            typeDumper.endWriter(components, pathComponents)
        }
    }

    private fun writeComponentData(interfaceName: String, interfaceID: String, components: StringBuilder, writeToJava: Boolean) {
        val interfaceKey = namer.name(interfaceName, interfaceID, typeDumper.language, writeToJava)!!
        typeDumper.write(components, typeDumper.fieldName(interfaceKey, interfaceID))
    }

    private fun writeGroupedComponents(data: StringBuilder?, components: StringBuilder, writeToJava: Boolean) {
        val groupedComponents = mutableMapOf<String, MutableList<Pair<String, String>>>()

        data?.lines()?.forEach { line ->
            val tokens = line.split("-")
            if (tokens.size == 4) {
                val (interfaceName, interfaceID, componentName, componentID) = tokens
                groupedComponents.computeIfAbsent("${interfaceName}:${interfaceID}") { mutableListOf() }.add(componentName to componentID)
            }
        }

        components.appendLine()
        groupedComponents.forEach { (interfaceName, componentsList) ->
            val parts = interfaceName.split(":")
            if (parts.size == 2) {
                val classBuilder = StringBuilder()
                val classNameDec = if (typeDumper.language == Language.JAVA) "public static final class" else "object"
                classBuilder.append("$classNameDec ${formatForClassName(parts[0])} {")
                classBuilder.appendLine()

                namer.used.clear()
                componentsList.forEach { (componentName, componentID) ->
                    val finalComponentID = getPackedComponentID(parts[1], componentID)
                    val componentKey = namer.name(componentName, finalComponentID, typeDumper.language, writeToJava)
                    typeDumper.write(classBuilder, "\t${typeDumper.fieldName(componentKey, finalComponentID)}")
                }
                classBuilder.appendLine("\t}")
                typeDumper. write(components, classBuilder.toString())
            }
        }
    }

    private fun getPackedComponentID(interfaceID: String, componentID: String): String {
        return typeDumper.exportSettings.useGameVal!!.usePackedComponents.let {
            if (it) pack(interfaceID.toInt(), componentID) else componentID
        }.toString()
    }

    private fun pack(interfaceId: Int, componentId: String): Int {
        return (interfaceId and 0xFFFF) shl 16 or (componentId.toInt() and 0xFFFF)
    }


}
