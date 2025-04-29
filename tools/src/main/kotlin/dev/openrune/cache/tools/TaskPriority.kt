package dev.openrune.cache.tools

enum class TaskPriority(val priorityValue: Int) {
    NORMAL(0),
    END(1),
    VERY_LAST(2);
}