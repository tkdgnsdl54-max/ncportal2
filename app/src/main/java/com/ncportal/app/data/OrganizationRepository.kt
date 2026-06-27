package com.ncportal.app.data

import android.content.Context
import com.ncportal.app.model.Organization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists [Organization] list to [Context.filesDir]/organizations.json.
 * Same atomic-write and opt* deserialization patterns as [WorkTaskRepository].
 */
class OrganizationRepository(context: Context) {

    private val file    = File(context.filesDir, "organizations.json")
    private val tmpFile = File(context.filesDir, "organizations.json.tmp")

    suspend fun loadAll(): List<Organization> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val array = JSONArray(file.readText(Charsets.UTF_8))
            (0 until array.length()).map { array.getJSONObject(it).toOrganization() }
        }.getOrElse { emptyList() }
    }

    suspend fun saveAll(orgs: List<Organization>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        orgs.forEach { array.put(it.toJson()) }
        tmpFile.writeText(array.toString(), Charsets.UTF_8)
        tmpFile.renameTo(file)
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun Organization.toJson(): JSONObject = JSONObject().apply {
        put("id",      id)
        put("name",    name)
        put("contact", contact)
        put("memo",    memo)
    }

    private fun JSONObject.toOrganization(): Organization = Organization(
        id      = getString("id"),
        name    = getString("name"),
        contact = optString("contact", ""),
        memo    = optString("memo", ""),
    )
}
