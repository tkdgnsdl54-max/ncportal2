package com.ncportal.app.data

import android.content.Context
import com.ncportal.app.model.LogAction
import com.ncportal.app.model.TaskStatus
import com.ncportal.app.model.WorkLogEntry
import com.ncportal.app.model.WorkTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists [WorkTask] list to [Context.filesDir]/work_tasks.json.
 *
 * Write strategy: serialise to work_tasks.json.tmp first, then rename over
 * work_tasks.json. On Android's Linux kernel, rename(2) is atomic — a crash
 * mid-write always leaves either the old or new complete file, never a partial.
 */
class WorkTaskRepository(context: Context) {

    private val file    = File(context.filesDir, "work_tasks.json")
    private val tmpFile = File(context.filesDir, "work_tasks.json.tmp")

    suspend fun loadAll(): List<WorkTask> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val array = JSONArray(file.readText(Charsets.UTF_8))
            (0 until array.length()).map { array.getJSONObject(it).toWorkTask() }
        }.getOrElse { emptyList() }
    }

    suspend fun saveAll(tasks: List<WorkTask>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        tasks.forEach { array.put(it.toJson()) }
        tmpFile.writeText(array.toString(), Charsets.UTF_8)
        tmpFile.renameTo(file)
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun WorkTask.toJson(): JSONObject = JSONObject().apply {
        put("id",              id)
        put("title",           title)
        put("organizationId",  organizationId)
        put("requester",       requester)
        put("contact",         contact)
        put("scheduledDate",   scheduledDate)
        put("status",          status.name)
        put("workDescription", workDescription)
        put("notes",           notes)
        put("createdAt",       createdAt)
        put("updatedAt",       updatedAt)
        put("workLogs", JSONArray().also { arr ->
            workLogs.forEach { arr.put(it.toJson()) }
        })
    }

    private fun JSONObject.toWorkTask(): WorkTask = WorkTask(
        id              = getString("id"),
        title           = getString("title"),
        organizationId  = optString("organizationId", ""),
        requester       = optString("requester", ""),
        contact         = optString("contact", ""),
        scheduledDate   = optLong("scheduledDate", 0L),
        status          = runCatching { TaskStatus.valueOf(getString("status")) }
                              .getOrDefault(TaskStatus.PENDING),
        workDescription = optString("workDescription", ""),
        notes           = optString("notes", ""),
        createdAt       = optLong("createdAt", System.currentTimeMillis()),
        updatedAt       = optLong("updatedAt", System.currentTimeMillis()),
        workLogs        = optJSONArray("workLogs")?.let { arr ->
                              (0 until arr.length()).map { arr.getJSONObject(it).toWorkLogEntry() }
                          } ?: emptyList(),
    )

    private fun WorkLogEntry.toJson(): JSONObject = JSONObject().apply {
        put("id",          id)
        put("timestamp",   timestamp)
        put("action",      action.name)
        put("description", description)
    }

    private fun JSONObject.toWorkLogEntry(): WorkLogEntry = WorkLogEntry(
        id          = getString("id"),
        timestamp   = optLong("timestamp", System.currentTimeMillis()),
        action      = runCatching { LogAction.valueOf(getString("action")) }
                          .getOrDefault(LogAction.NOTE_ADDED),
        description = optString("description", ""),
    )
}
