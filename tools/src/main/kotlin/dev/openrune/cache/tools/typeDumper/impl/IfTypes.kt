package dev.openrune.cache.tools.typeDumper.impl

import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.typeDumper.Language
import dev.openrune.cache.tools.typeDumper.TypeDumper
import dev.openrune.cache.util.Namer
import dev.openrune.cache.util.Namer.Companion.formatForClassName
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache

class IfTypes(
    private val typeDumper: TypeDumper,
    private val writeToJava: Boolean
) {

    private val namer = Namer()

    fun writeInterfacesAndComponents(group : GameValGroupTypes, cache: Cache) {
        val writtenInterfaces = mutableSetOf<String>()

        val ifTypes = GameValHandler.readGameVal(GameValGroupTypes.IFTYPES,cache)

        if (typeDumper.exportSettings.useGameVal!!.combineInterfaceAndComponents) {
            val (components, pathComponents) = typeDumper.generateWriter(group)

            ifTypes.forEach {
                val inf = it.elementAs<Interface>()
                val (interfaceName, interfaceID, _, _) = arrayOf(inf!!.name,inf.id.toString(),"","")
                if (!writtenInterfaces.contains(interfaceName)) {
                    writeComponentData(interfaceName, interfaceID, components, writeToJava)
                    writtenInterfaces.add(interfaceName)
                }
            }

            if (typeDumper.language == Language.RSCM) {

                ifTypes.forEach {
                    val inf = it.elementAs<Interface>()
                    val interfaceName = inf!!.name

                    inf.components.forEach { component ->
                        val comp = component.elementAs<Interface.InterfaceComponent>()
                        val finalComponentID = comp?.packed.toString()
                        val componentKey = namer.name("$interfaceName.${comp?.name}", finalComponentID, typeDumper.language, writeToJava)
                        typeDumper.write(components, typeDumper.fieldName(componentKey, finalComponentID))
                    }
                }
            } else {
                writeGroupedComponents(components, writeToJava,ifTypes)
            }

            typeDumper.endWriter(components, pathComponents)
        } else {
            val (interfaces, pathInterfaces) = typeDumper.generateWriter(override = "interfaces")
            val (components, pathComponents) = typeDumper.generateWriter(group)

            ifTypes.forEach {

                val inf = it.elementAs<Interface>()
                val interfaceName = inf!!.name
                val interfaceID = inf.id.toString()

                inf.components.forEach { comp ->
                    if (!writtenInterfaces.contains(interfaceName)) {
                        writeComponentData(interfaceName, interfaceID, interfaces, writeToJava)
                        writtenInterfaces.add(interfaceName)
                    }

                    val finalComponentID = comp.packed.toString()
                    val splitter = if (typeDumper.language == Language.RSCM) "." else "_"
                    val componentKey = namer.name("${interfaceName}${splitter}${comp.name}", finalComponentID, typeDumper.language, writeToJava)
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

    private fun writeGroupedComponents(components: StringBuilder, writeToJava: Boolean, ifTypes: List<GameValElement>) {
        val groupedComponents = mutableMapOf<String, MutableList<Pair<String, String>>>()

        ifTypes.forEach {
            val element = it.elementAs<Interface>()

            val interfaceName = it.name
            val interfaceID = it.id.toString()

            element?.components?.forEach { comp ->
                groupedComponents.computeIfAbsent("${interfaceName}:${interfaceID}") { mutableListOf() }.add(comp.name to comp.id.toString())
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
