package com.ganeshadigital.mlagaav

import android.content.Context
import java.util.TreeMap

/** district -> taluka -> gram panchayat -> villages (all sorted) */
typealias LocTree = TreeMap<String, TreeMap<String, TreeMap<String, MutableList<String>>>>

object LocationData {
    private var cachedState: String? = null
    private var cachedTree: LocTree = TreeMap()

    fun slug(state: String) = state.lowercase().replace(Regex("[^a-z0-9]+"), "_")

    fun states(ctx: Context): List<String> = try {
        ctx.assets.open("loc/states.txt").bufferedReader().readLines().filter { it.isNotBlank() }
    } catch (e: Exception) { emptyList() }

    fun load(ctx: Context, state: String): LocTree {
        if (state == cachedState) return cachedTree
        val tree: LocTree = TreeMap()
        try {
            ctx.assets.open("loc/${slug(state)}.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val p = line.split('\t')
                    if (p.size >= 4) {
                        tree.getOrPut(p[0]) { TreeMap() }
                            .getOrPut(p[1]) { TreeMap() }
                            .getOrPut(p[2]) { ArrayList() }
                            .add(p[3])
                    }
                }
            }
        } catch (e: Exception) { /* state file missing -> empty tree */ }
        cachedState = state
        cachedTree = tree
        return tree
    }
}
