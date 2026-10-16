package com.example.flowmind.domain.models

import org.json.JSONArray

/**
 * Extension functions on [Workflow] for common derived computations.
 *
 * These are kept separate from the entity class so that Room does not
 * accidentally map them as database columns.
 */

/**
 * Parses [Workflow.nodesJson] and returns the ordered list of node title strings.
 * Returns an empty list if the JSON is blank or malformed.
 */
fun Workflow.nodeList(): List<String> {
    if (nodesJson.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(nodesJson)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())
}

/**
 * Returns the number of nodes in this workflow without fully deserialising the JSON.
 */
fun Workflow.nodeCount(): Int = nodeList().size

/**
 * Returns `true` when the workflow has been modified after its initial creation.
 * A 1-second tolerance is applied to account for clock resolution on some devices.
 */
fun Workflow.wasEdited(): Boolean = (updatedAt - createdAt) > 1_000L

/**
 * Returns a short human-readable status label suitable for list items and chips.
 */
fun Workflow.statusLabel(): String = if (isEnabled) "Active" else "Paused"

/**
 * Returns the age of the workflow in whole days since [createdAt].
 */
fun Workflow.ageInDays(): Long {
    val msPerDay = 24L * 60 * 60 * 1000
    return (System.currentTimeMillis() - createdAt) / msPerDay
}

/**
 * Returns `true` when the workflow was created within the last [days] days.
 */
fun Workflow.isRecent(days: Int = 7): Boolean = ageInDays() < days
