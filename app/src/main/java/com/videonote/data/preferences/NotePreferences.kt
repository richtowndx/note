package com.videonote.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.videonote.domain.model.NoteDirectory
import com.videonote.domain.model.TTSSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 笔记偏好设置管理器
 * 使用SharedPreferences存储笔记目录配置和全局TTS设置
 */
@Singleton
class NotePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * 保存笔记目录列表
     */
    fun saveNoteDirectories(directories: List<NoteDirectory>) {
        val json = directories.joinToString(SEPARATOR) { "${it.path}|${it.name}" }
        prefs.edit().putString(KEY_NOTE_DIRECTORIES, json).apply()
    }

    /**
     * 获取笔记目录列表
     */
    fun getNoteDirectories(): List<NoteDirectory> {
        val json = prefs.getString(KEY_NOTE_DIRECTORIES, null) ?: return emptyList()
        return json.split(SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    NoteDirectory(path = parts[0], name = parts[1])
                } else null
            }
    }

    /**
     * 添加笔记目录
     */
    fun addNoteDirectory(directory: NoteDirectory): Boolean {
        val current = getNoteDirectories().toMutableList()
        // 检查是否已存在
        if (current.any { it.path == directory.path }) {
            return false
        }
        current.add(directory)
        saveNoteDirectories(current)
        return true
    }

    /**
     * 删除笔记目录
     */
    fun removeNoteDirectory(path: String) {
        val current = getNoteDirectories().toMutableList()
        current.removeAll { it.path == path }
        saveNoteDirectories(current)
    }

    /**
     * 获取当前选中的目录路径
     */
    fun getSelectedDirectory(): String? {
        return prefs.getString(KEY_SELECTED_DIRECTORY, null)
    }

    /**
     * 设置当前选中的目录路径
     */
    fun setSelectedDirectory(path: String?) {
        prefs.edit().putString(KEY_SELECTED_DIRECTORY, path).apply()
    }

    /**
     * 保存全局TTS设置
     */
    fun saveGlobalTTSSettings(settings: TTSSettings) {
        prefs.edit()
            .putFloat(KEY_TTS_SPEECH_RATE, settings.speechRate)
            .putFloat(KEY_TTS_PITCH, settings.pitch)
            .putString(KEY_TTS_VOICE_ID, settings.voiceId)
            .apply()
    }

    /**
     * 获取全局TTS设置
     */
    fun getGlobalTTSSettings(): TTSSettings {
        return TTSSettings(
            speechRate = prefs.getFloat(KEY_TTS_SPEECH_RATE, TTSSettings.DEFAULT_RATE),
            pitch = prefs.getFloat(KEY_TTS_PITCH, TTSSettings.DEFAULT_PITCH),
            voiceId = prefs.getString(KEY_TTS_VOICE_ID, "") ?: ""
        )
    }

    companion object {
        private const val PREFS_NAME = "note_preferences"
        private const val KEY_NOTE_DIRECTORIES = "note_directories"
        private const val KEY_SELECTED_DIRECTORY = "selected_directory"
        private const val KEY_TTS_SPEECH_RATE = "tts_speech_rate"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_VOICE_ID = "tts_voice_id"
        private const val SEPARATOR = ";;;"
    }
}
