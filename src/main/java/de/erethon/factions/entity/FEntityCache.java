package de.erethon.factions.entity;

import de.erethon.bedrock.misc.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author Fyreum
 */
public abstract class FEntityCache<E extends FLegalEntity> implements Iterable<E> {

    protected final File folder;
    protected final Map<Integer, E> cache = new HashMap<>();

    public FEntityCache(@NotNull File folder) {
        this.folder = folder;
        this.folder.mkdir();
        initializeAll();
    }

    public void initializeAll() {
        for (File file : FileUtil.getFilesForFolder(folder)) {
            initialize(file);
        }
    }

    public void initialize(@NotNull File file) {
        E entity = create(file);
        if (entity == null) {
            return;
        }
        cache.put(entity.getId(), entity);
    }

    public void loadAll() {
        for (E entity : cache.values()) {
            entity.load();
        }
    }

    public void saveAll() {
        for (E entity : cache.values()) {
            entity.saveData();
        }
    }

    /* Abstracts */

    protected abstract @Nullable E create(@NotNull File file);

    /* Getters */

    public @NotNull File getFolder() {
        return folder;
    }

    public @NotNull Map<Integer, E> getCache() {
        return cache;
    }

    public @Nullable E getById(int id) {
        return cache.get(id);
    }

    public @Nullable E getByName(@NotNull String name) {
        for (E entity : cache.values()) {
            if (entity.matchingName(name)) {
                return entity;
            }
        }
        return null;
    }

    /* Iterable */

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return cache.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        cache.values().forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return cache.values().spliterator();
    }
}
