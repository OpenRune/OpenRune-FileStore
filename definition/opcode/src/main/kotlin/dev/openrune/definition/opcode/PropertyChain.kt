package dev.openrune.definition.opcode

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class PropertyChain<T, R>(
    internal val properties: List<KProperty1<*, *>>,
    private val autoInitializers: List<() -> Any> = emptyList()
) {

    fun getter(): (T) -> R? = { root ->
        var current: Any? = root
        for (prop in properties) {
            if (current == null) {
                current = null
                break
            }
            @Suppress("UNCHECKED_CAST")
            current = (prop as KProperty1<Any, Any?>).get(current)
        }
        current as R?
    }

    fun setter(): (T, R) -> Unit = { root, value ->
        var current: Any? = root

        for (i in 0 until properties.lastIndex) {
            val prop = properties[i]
            @Suppress("UNCHECKED_CAST")
            val mutable = prop as? KMutableProperty1<Any, Any?>

            var next = (prop as KProperty1<Any, Any?>).get(current!!)
            if (next == null) {
                if (mutable == null) error("Property '${prop.name}' is not mutable and is null")
                val initializer = if (autoInitializers.isNotEmpty()) {
                    autoInitializers.getOrNull(i)
                        ?: error("Missing auto-initializer for '${prop.name}'")
                } else {
                    error("No auto-initializers provided for '${prop.name}'")
                }
                next = initializer()
                mutable.set(current, next)
            }

            current = next
        }

        @Suppress("UNCHECKED_CAST")
        val last = properties.last() as KMutableProperty1<Any, R?>
        last.set(current!!, value)
    }

    fun toGetterSetter(): Pair<(T) -> R?, (T, R) -> Unit> {
        val inits: List<() -> Any> = if (autoInitializers.isEmpty()) {
            properties.dropLast(1).map { prop ->
                val propType = prop.returnType.classifier as? KClass<*>
                if (propType == null) {
                    {
                        println("Warning: Cannot infer class for property '${prop.name}', no auto-init possible.")
                        error("No auto-init for property '${prop.name}'")
                    }
                } else {
                    {
                        try {
                            val ctor = propType.constructors.firstOrNull { it.parameters.isEmpty() }
                            if (ctor != null) {
                                ctor.call()
                            } else {
                                propType.java.getDeclaredConstructor().newInstance()
                            }
                        } catch (ex: Exception) {
                            println("Warning: No no-arg constructor for '${prop.name}' of type ${propType.simpleName}. Provide manual initializer.")
                            error("No auto-init for property '${prop.name}'")
                        }
                    }
                }
            }
        } else autoInitializers

        val chainWithInit = if (inits.isNotEmpty()) {
            PropertyChain<T, R>(properties, inits)
        } else {
            this
        }

        return chainWithInit.getter() to chainWithInit.setter()
    }
}

/**
 * Create a deep property chain for nested nullable access.
 * E.g., propertyChain(A::b, B::c, C::d)
 */
fun <T, R> propertyChain(
    vararg props: KProperty1<*, *>
): PropertyChain<T, R> = PropertyChain(props.toList())

/**
 * Manually provide auto-initializers for intermediate properties.
 * Overrides the automatic no-arg constructor approach.
 */
fun <T, R> PropertyChain<T, R>.withAutoInit(
    vararg initializers: () -> Any
): Pair<(T) -> R?, (T, R) -> Unit> = PropertyChain<T, R>(
    properties,
    autoInitializers = initializers.toList()
).toGetterSetter()
