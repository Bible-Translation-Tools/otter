package org.wycliffeassociates.otter.jvm.workbookapp.persistence.repositories

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.wycliffeassociates.otter.common.data.config.AudioPluginData
import org.wycliffeassociates.otter.common.data.config.IAudioPlugin
import org.wycliffeassociates.otter.common.persistence.IAppPreferences
import org.wycliffeassociates.otter.common.persistence.repositories.IAudioPluginRepository
import org.wycliffeassociates.otter.common.persistence.repositories.PluginType
import org.wycliffeassociates.otter.jvm.workbookapp.audioplugin.AudioPlugin
import org.wycliffeassociates.otter.jvm.workbookapp.persistence.AppPreferences
import org.wycliffeassociates.otter.jvm.workbookapp.persistence.database.AppDatabase
import org.wycliffeassociates.otter.jvm.workbookapp.persistence.repositories.mapping.AudioPluginDataMapper

class AudioPluginRepository(
    database: AppDatabase,
    private val preferences: IAppPreferences,
    private val mapper: AudioPluginDataMapper = AudioPluginDataMapper()
) : IAudioPluginRepository {
    private val logger = LoggerFactory.getLogger(AudioPluginRepository::class.java)

    private val audioPluginDao = database.audioPluginDao

    override fun insert(data: AudioPluginData): Single<Int> {
        return Single
            .fromCallable {
                audioPluginDao.insert(mapper.mapToEntity(data))
            }
            .doOnError { e ->
                logger.error("Error in insert with plugin data: $data", e)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun getAll(): Single<List<AudioPluginData>> {
        return Single
            .fromCallable {
                audioPluginDao
                    .fetchAll()
                    .map { mapper.mapFromEntity(it) }
            }
            .doOnError { e ->
                logger.error("Error in getAll", e)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun getAllPlugins(): Single<List<IAudioPlugin>> {
        return getAll()
            .map {
                it.map { AudioPlugin(it) }
            }
    }

    override fun update(obj: AudioPluginData): Completable {
        return Completable
            .fromAction {
                audioPluginDao.update(mapper.mapToEntity(obj))
            }
            .doOnError { e ->
                logger.error("Error in update for plugin data: $obj", e)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun delete(obj: AudioPluginData): Completable {
        return Completable
            .fromAction {
                obj.pluginFile?.let { if (it.exists()) it.delete() }
                audioPluginDao.delete(mapper.mapToEntity(obj))
            }
            // Update the preferences if necessary
            .andThen(preferences.pluginId(PluginType.RECORDER))
            .flatMapCompletable {
                if (it == obj.id)
                    return@flatMapCompletable preferences.setPluginId(PluginType.RECORDER, -1)
                else
                    return@flatMapCompletable Completable.complete()
            }
            .andThen(preferences.pluginId(PluginType.EDITOR))
            .flatMapCompletable {
                if (it == obj.id)
                    return@flatMapCompletable preferences.setPluginId(PluginType.EDITOR, -1)
                else
                    return@flatMapCompletable Completable.complete()
            }
            .doOnError { e ->
                logger.error("Error in delete for plugin data: $obj", e)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun initSelected(): Completable =
        Single
            .fromCallable {
                audioPluginDao.fetchAll()
            }
            .flatMapCompletable { allPlugins ->
                if (allPlugins.isEmpty()) {
                    Completable.complete()
                } else {
                    preferences.pluginId(PluginType.EDITOR)
                        .flatMapCompletable { editorId ->
                            val editPlugins = allPlugins.filter { it.edit == 1 }
                            if (editorId == AppPreferences.NO_ID && editPlugins.isNotEmpty()) {
                                preferences.setPluginId(PluginType.EDITOR, editPlugins.first().id)
                            } else {
                                Completable.complete()
                            }
                        }
                        .andThen(preferences.pluginId(PluginType.RECORDER))
                        .flatMapCompletable { recorderId ->
                            val recordPlugins = allPlugins.filter { it.record == 1 }
                            if (recorderId == AppPreferences.NO_ID && recordPlugins.isNotEmpty()) {
                                preferences.setPluginId(PluginType.RECORDER, recordPlugins.first().id)
                            } else {
                                Completable.complete()
                            }
                        }
                }
            }
            .doOnError { e ->
                logger.error("Error in initSelected", e)
            }
            .subscribeOn(Schedulers.io())

    override fun getPlugin(type: PluginType): Maybe<IAudioPlugin> {
        return getPluginData(type).map { AudioPlugin(it) }
    }

    override fun getPluginData(type: PluginType): Maybe<AudioPluginData> {
        return preferences.pluginId(type)
            .flatMapMaybe { pluginId ->
                if (pluginId == AppPreferences.NO_ID) {
                    return@flatMapMaybe Maybe.empty<AudioPluginData>()
                } else {
                    Maybe
                        .fromCallable {
                            mapper.mapFromEntity(audioPluginDao.fetchById(pluginId))
                        }
                        .onErrorComplete()
                        .subscribeOn(Schedulers.io())
                }
            }
            .doOnError { e ->
                logger.error("Error in getPluginData", e)
            }
    }

    override fun setPluginData(type: PluginType, default: AudioPluginData): Completable {
        return when (type) {
            PluginType.RECORDER -> {
                if (default.canRecord) preferences.setPluginId(type, default.id) else Completable.complete()
            }
            PluginType.EDITOR -> {
                if (default.canEdit) preferences.setPluginId(type, default.id) else Completable.complete()
            }
            PluginType.MARKER -> {
                if (default.canMark) preferences.setPluginId(type, default.id) else Completable.complete()
            }
        }
    }
}
