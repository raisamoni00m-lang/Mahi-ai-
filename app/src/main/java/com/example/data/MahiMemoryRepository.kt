package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class MemoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TrainedTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val steps: Int,
    val subtitle: String,
    val isBuiltIn: Boolean = true,
    val promptTemplate: String = ""
)

data class FavoriteContact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val isSos: Boolean = false
)

class MahiMemoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mahi_memories_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MEMORIES = "stored_memories"
        private const val KEY_TASKS = "stored_tasks"
        private const val KEY_CONTACTS = "stored_contacts"
        private const val KEY_CHATS = "stored_chat_history"
    }

    // ================= MEMORIES =================
    fun getMemories(): List<MemoryItem> {
        val jsonStr = prefs.getString(KEY_MEMORIES, null) ?: return defaultMemories()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<MemoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MemoryItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        category = obj.getString("category"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) defaultMemories() else list
        } catch (_: Exception) {
            defaultMemories()
        }
    }

    fun addMemory(title: String, category: String = "General") {
        val current = getMemories().toMutableList()
        current.add(0, MemoryItem(title = title, category = category))
        saveMemories(current)
    }

    fun deleteMemory(id: String) {
        val current = getMemories().filterNot { it.id == id }
        saveMemories(current)
    }

    fun saveMemories(list: List<MemoryItem>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("category", item.category)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_MEMORIES, arr.toString()).apply()
    }

    private fun defaultMemories(): List<MemoryItem> {
        return listOf(
            MemoryItem(title = "Prefers clear, concise responses without fluff", category = "Preference"),
            MemoryItem(title = "Working on Android AI assistant app development", category = "Project"),
            MemoryItem(title = "Favorite study music: Lo-Fi and ambient soundtracks", category = "Music")
        )
    }

    // ================= TRAINED TASKS =================
    fun getTrainedTasks(): List<TrainedTask> {
        val jsonStr = prefs.getString(KEY_TASKS, null) ?: return defaultTrainedTasks()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<TrainedTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TrainedTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        steps = obj.getInt("steps"),
                        subtitle = obj.getString("subtitle"),
                        isBuiltIn = obj.optBoolean("isBuiltIn", true),
                        promptTemplate = obj.optString("promptTemplate", "")
                    )
                )
            }
            if (list.isEmpty()) defaultTrainedTasks() else list
        } catch (_: Exception) {
            defaultTrainedTasks()
        }
    }

    fun addCustomTask(title: String, steps: Int, subtitle: String, prompt: String) {
        val current = getTrainedTasks().toMutableList()
        current.add(
            TrainedTask(
                title = title,
                steps = steps,
                subtitle = "$steps steps • custom",
                isBuiltIn = false,
                promptTemplate = prompt
            )
        )
        saveTrainedTasks(current)
    }

    fun deleteCustomTask(id: String) {
        val current = getTrainedTasks().filterNot { it.id == id }
        saveTrainedTasks(current)
    }

    fun updateCustomTask(id: String, title: String, steps: Int, prompt: String) {
        val current = getTrainedTasks().map { task ->
            if (task.id == id) {
                task.copy(
                    title = title,
                    steps = steps,
                    subtitle = "$steps steps • custom",
                    promptTemplate = prompt
                )
            } else {
                task
            }
        }
        saveTrainedTasks(current)
    }

    fun saveTrainedTasks(list: List<TrainedTask>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("steps", item.steps)
                put("subtitle", item.subtitle)
                put("isBuiltIn", item.isBuiltIn)
                put("promptTemplate", item.promptTemplate)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    private fun defaultTrainedTasks(): List<TrainedTask> {
        return listOf(
            TrainedTask(
                title = "gemini se poochho",
                steps = 8,
                subtitle = "8 steps • sawaal • built-in",
                isBuiltIn = true,
                promptTemplate = "Answer with clear step-by-step guidance in Hindi/English mix as requested."
            ),
            TrainedTask(
                title = "chatgpt se poochho",
                steps = 8,
                subtitle = "8 steps • sawaal • built-in",
                isBuiltIn = true,
                promptTemplate = "Compare answers and synthesize a concise perspective."
            ),
            TrainedTask(
                title = "gemini video",
                steps = 16,
                subtitle = "16 steps • video prompt • built-in",
                isBuiltIn = true,
                promptTemplate = "Analyze video concept, write 16 key scenes, transitions and script."
            ),
            TrainedTask(
                title = "gemini image",
                steps = 17,
                subtitle = "17 steps • image prompt • built-in",
                isBuiltIn = true,
                promptTemplate = "Generate high-detail photorealistic prompt breakdowns with camera lighting."
            ),
            TrainedTask(
                title = "chatgpt image",
                steps = 14,
                subtitle = "14 steps • image prompt • built-in",
                isBuiltIn = true,
                promptTemplate = "Construct detailed DALL-E / generative image creative art direction."
            )
        )
    }

    // ================= CONTACTS =================
    fun getFavoriteContacts(): List<FavoriteContact> {
        val jsonStr = prefs.getString(KEY_CONTACTS, null) ?: return defaultContacts()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<FavoriteContact>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FavoriteContact(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        phone = obj.getString("phone"),
                        isSos = obj.optBoolean("isSos", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultContacts()
        }
    }

    private fun defaultContacts(): List<FavoriteContact> {
        return emptyList()
    }

    // ================= CHATS =================
    fun getConversationCount(): Int {
        val jsonStr = prefs.getString(KEY_CHATS, null) ?: return 1
        return try {
            val arr = JSONArray(jsonStr)
            maxOf(arr.length(), 1)
        } catch (_: Exception) {
            1
        }
    }

    // ================= BACKUP & RESTORE =================
    /**
     * Generates sanitized JSON backup.
     * Excludes API keys, license keys, and voice print/biometrics.
     */
    fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", "Mahi AI")
        root.put("version", "1.0")
        root.put("createdAt", System.currentTimeMillis())
        root.put("creator", "Mahi AI (Made by Mizan)")

        // Memories
        val memoriesArr = JSONArray()
        for (m in getMemories()) {
            memoriesArr.put(JSONObject().apply {
                put("title", m.title)
                put("category", m.category)
                put("timestamp", m.timestamp)
            })
        }
        root.put("memories", memoriesArr)

        // Trained tasks
        val tasksArr = JSONArray()
        for (t in getTrainedTasks().filterNot { it.isBuiltIn }) {
            tasksArr.put(JSONObject().apply {
                put("title", t.title)
                put("steps", t.steps)
                put("subtitle", t.subtitle)
                put("promptTemplate", t.promptTemplate)
            })
        }
        root.put("customTasks", tasksArr)

        // Contacts
        val contactsArr = JSONArray()
        for (c in getFavoriteContacts()) {
            contactsArr.put(JSONObject().apply {
                put("name", c.name)
                put("phone", c.phone)
                put("isSos", c.isSos)
            })
        }
        root.put("contacts", contactsArr)

        return root.toString(2)
    }

    /**
     * Merges imported data without duplicate overwrites.
     * Returns Pair(importedMemoriesCount, importedTasksCount).
     */
    fun restoreFromJson(jsonContent: String): Pair<Int, Int> {
        val root = JSONObject(jsonContent)
        val memoriesArr = root.optJSONArray("memories")
        val currentMemories = getMemories().toMutableList()
        val existingTitles = currentMemories.map { it.title.trim().lowercase() }.toSet()
        var addedMemories = 0

        if (memoriesArr != null) {
            for (i in 0 until memoriesArr.length()) {
                val obj = memoriesArr.getJSONObject(i)
                val title = obj.getString("title")
                if (!existingTitles.contains(title.trim().lowercase())) {
                    currentMemories.add(
                        MemoryItem(
                            title = title,
                            category = obj.optString("category", "General"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                    addedMemories++
                }
            }
            saveMemories(currentMemories)
        }

        val customTasksArr = root.optJSONArray("customTasks")
        val currentTasks = getTrainedTasks().toMutableList()
        val existingTasks = currentTasks.map { it.title.trim().lowercase() }.toSet()
        var addedTasks = 0

        if (customTasksArr != null) {
            for (i in 0 until customTasksArr.length()) {
                val obj = customTasksArr.getJSONObject(i)
                val title = obj.getString("title")
                if (!existingTasks.contains(title.trim().lowercase())) {
                    currentTasks.add(
                        TrainedTask(
                            title = title,
                            steps = obj.optInt("steps", 8),
                            subtitle = obj.optString("subtitle", "Imported task"),
                            isBuiltIn = false,
                            promptTemplate = obj.optString("promptTemplate", "")
                        )
                    )
                    addedTasks++
                }
            }
            saveTrainedTasks(currentTasks)
        }

        return Pair(addedMemories, addedTasks)
    }
}
