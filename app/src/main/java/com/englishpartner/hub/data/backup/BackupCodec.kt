package com.englishpartner.hub.data.backup

import com.englishpartner.hub.data.db.Bookmark
import com.englishpartner.hub.data.db.CourseDay
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.Note
import com.englishpartner.hub.data.db.VocabWord
import org.json.JSONArray
import org.json.JSONObject

/** Everything stored in Room, as one snapshot. */
data class BackupData(
    val days: List<CourseDay>,
    val materials: List<Material>,
    val bookmarks: List<Bookmark>,
    val notes: List<Note>,
    val vocabulary: List<VocabWord>
)

/**
 * Converts the database snapshot to and from JSON.
 * A versioned plain-JSON format survives future database schema changes better than copying the .db file.
 */
object BackupCodec {
    const val FORMAT_VERSION = 1

    fun encode(data: BackupData, createdAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("format", "english-partner-backup")
        root.put("version", FORMAT_VERSION)
        root.put("createdAt", createdAt)
        root.put("days", JSONArray().apply { data.days.forEach { put(dayToJson(it)) } })
        root.put("materials", JSONArray().apply { data.materials.forEach { put(materialToJson(it)) } })
        root.put("bookmarks", JSONArray().apply { data.bookmarks.forEach { put(bookmarkToJson(it)) } })
        root.put("notes", JSONArray().apply { data.notes.forEach { put(noteToJson(it)) } })
        root.put("vocabulary", JSONArray().apply { data.vocabulary.forEach { put(vocabToJson(it)) } })
        return root.toString()
    }

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        require(root.optString("format") == "english-partner-backup") { "This is not an English Partner backup file." }
        val version = root.optInt("version", 0)
        require(version in 1..FORMAT_VERSION) { "Backup version $version is not supported by this app version." }
        return BackupData(
            days = root.optJSONArray("days").objects().map(::dayFromJson),
            materials = root.optJSONArray("materials").objects().map(::materialFromJson),
            bookmarks = root.optJSONArray("bookmarks").objects().map(::bookmarkFromJson),
            notes = root.optJSONArray("notes").objects().map(::noteFromJson),
            vocabulary = root.optJSONArray("vocabulary").objects().map(::vocabFromJson)
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).map { getJSONObject(it) }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun dayToJson(d: CourseDay) = JSONObject().apply {
        put("id", d.id); put("dayNumber", d.dayNumber); put("title", d.title)
        put("completed", d.completed); put("createdAt", d.createdAt)
        put("completedAt", d.completedAt ?: JSONObject.NULL)
    }

    private fun dayFromJson(o: JSONObject) = CourseDay(
        id = o.getLong("id"),
        dayNumber = o.getInt("dayNumber"),
        title = o.optString("title", ""),
        completed = o.optBoolean("completed", false),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        completedAt = o.optLongOrNull("completedAt")
    )

    private fun materialToJson(m: Material) = JSONObject().apply {
        put("id", m.id); put("dayId", m.dayId); put("type", m.type)
        put("displayName", m.displayName); put("fileName", m.fileName)
        put("mimeType", m.mimeType); put("sizeBytes", m.sizeBytes)
        put("positionMs", m.positionMs); put("durationMs", m.durationMs)
        put("lastPage", m.lastPage); put("completed", m.completed)
        put("importedAt", m.importedAt)
        put("lastOpenedAt", m.lastOpenedAt ?: JSONObject.NULL)
    }

    private fun materialFromJson(o: JSONObject) = Material(
        id = o.getLong("id"),
        dayId = o.getLong("dayId"),
        type = o.optString("type", "OTHER"),
        displayName = o.optString("displayName", "file"),
        fileName = o.getString("fileName"),
        mimeType = o.optString("mimeType", ""),
        sizeBytes = o.optLong("sizeBytes", 0),
        positionMs = o.optLong("positionMs", 0),
        durationMs = o.optLong("durationMs", 0),
        lastPage = o.optInt("lastPage", 0),
        completed = o.optBoolean("completed", false),
        importedAt = o.optLong("importedAt", System.currentTimeMillis()),
        lastOpenedAt = o.optLongOrNull("lastOpenedAt")
    )

    private fun bookmarkToJson(b: Bookmark) = JSONObject().apply {
        put("id", b.id); put("materialId", b.materialId); put("position", b.position)
        put("label", b.label); put("createdAt", b.createdAt)
    }

    private fun bookmarkFromJson(o: JSONObject) = Bookmark(
        id = o.getLong("id"),
        materialId = o.getLong("materialId"),
        position = o.optLong("position", 0),
        label = o.optString("label", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun noteToJson(n: Note) = JSONObject().apply {
        put("id", n.id); put("dayId", n.dayId); put("text", n.text)
        put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
    }

    private fun noteFromJson(o: JSONObject) = Note(
        id = o.getLong("id"),
        dayId = o.getLong("dayId"),
        text = o.optString("text", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun vocabToJson(v: VocabWord) = JSONObject().apply {
        put("id", v.id); put("word", v.word); put("meaning", v.meaning)
        put("example", v.example); put("dayId", v.dayId ?: JSONObject.NULL)
        put("mastered", v.mastered); put("createdAt", v.createdAt)
    }

    private fun vocabFromJson(o: JSONObject) = VocabWord(
        id = o.getLong("id"),
        word = o.optString("word", ""),
        meaning = o.optString("meaning", ""),
        example = o.optString("example", ""),
        dayId = o.optLongOrNull("dayId"),
        mastered = o.optBoolean("mastered", false),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )
}
