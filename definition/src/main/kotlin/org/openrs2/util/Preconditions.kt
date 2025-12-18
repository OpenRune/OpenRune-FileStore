package org.openrs2.util

public object Preconditions {
    private fun badPositionIndex(index: Int, size: Int, desc: String): String {
        if (index < 0) {
            return "$desc ($index) must not be negative"
        } else require(size >= 0) { "negative size: $size" }
        // index > size
        return "$desc ($index) must not be greater than size ($size)"
    }

    /**
     * Ensures that `start` and `end` specify a valid *positions* in an array, list
     * or string of size `size`, and are in order. A position index may range from zero to
     * `size`, inclusive.
     *
     * @param start a user-supplied index identifying a starting position in an array, list or string
     * @param end a user-supplied index identifying a ending position in an array, list or string
     * @param size the size of that array, list or string
     * @throws IndexOutOfBoundsException if either index is negative or is greater than `size`,
     * or if `end` is less than `start`
     * @throws IllegalArgumentException if `size` is negative
     */
    public fun checkPositionIndexes(start: Int, end: Int, size: Int) {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (start < 0 || end < start || end > size) {
            throw IndexOutOfBoundsException(badPositionIndexes(start, end, size))
        }
    }

    private fun badPositionIndexes(start: Int, end: Int, size: Int): String {
        if (start < 0 || start > size) {
            return badPositionIndex(start, size, "start index")
        }
        if (end < 0 || end > size) {
            return badPositionIndex(end, size, "end index")
        }
        // end < start
        return "end index ($end) must not be less than start index ($start)"
    }
}