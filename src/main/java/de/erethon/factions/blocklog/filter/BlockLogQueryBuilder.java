package de.erethon.factions.blocklog.filter;

import de.erethon.factions.blocklog.dao.BlockChangeMapper;
import de.erethon.factions.blocklog.model.BlockChange;
import org.jdbi.v3.core.Handle;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Malfrador
 */
public class BlockLogQueryBuilder {

    private final BlockLogFilter filter;
    private final StringBuilder sql;
    private final List<Object[]> bindings; // each entry is [name, value]

    public BlockLogQueryBuilder(BlockLogFilter filter) {
        this.filter = filter;
        this.sql = new StringBuilder();
        this.bindings = new ArrayList<>();
    }

    /**
     * Builds and executes the query, returning results.
     */
    public List<BlockChange> execute(Handle handle) {
        buildQuery();
        var query = handle.createQuery(sql.toString());
        for (Object[] binding : bindings) {
            query.bind((String) binding[0], binding[1]);
        }
        return query.map(new BlockChangeMapper()).list();
    }

    /**
     * Builds and executes a COUNT query with the same filters (no limit/offset).
     */
    public long executeCount(Handle handle) {
        buildCountQuery();
        var query = handle.createQuery(sql.toString());
        for (Object[] binding : bindings) {
            query.bind((String) binding[0], binding[1]);
        }
        return query.mapTo(Long.class).one();
    }

    private void buildQuery() {
        sql.setLength(0);
        bindings.clear();

        boolean needPaletteJoin = !filter.blocks().isEmpty() || !filter.excludedBlocks().isEmpty();

        sql.append("SELECT bl.* FROM block_log bl");
        if (needPaletteJoin) {
            sql.append(" JOIN block_palette old_p ON bl.old_block_id = old_p.id");
            sql.append(" JOIN block_palette new_p ON bl.new_block_id = new_p.id");
        }
        sql.append(" WHERE 1=1");
        sql.append(" AND bl.reverted = FALSE");

        appendFilters(needPaletteJoin);

        // Ordering
        sql.append(" ORDER BY bl.timestamp ");
        sql.append(filter.ascending() ? "ASC" : "DESC");

        // Limit & offset
        sql.append(" LIMIT :limit OFFSET :offset");
        bind("limit", filter.limit());
        bind("offset", filter.offset());
    }

    private void buildCountQuery() {
        sql.setLength(0);
        bindings.clear();

        boolean needPaletteJoin = !filter.blocks().isEmpty() || !filter.excludedBlocks().isEmpty();

        sql.append("SELECT COUNT(*) FROM block_log bl");
        if (needPaletteJoin) {
            sql.append(" JOIN block_palette old_p ON bl.old_block_id = old_p.id");
            sql.append(" JOIN block_palette new_p ON bl.new_block_id = new_p.id");
        }
        sql.append(" WHERE 1=1");
        sql.append(" AND bl.reverted = FALSE");

        appendFilters(needPaletteJoin);
    }

    private void appendFilters(boolean needPaletteJoin) {
        // Player filter
        if (!filter.players().isEmpty()) {
            sql.append(" AND bl.cause_name IN (");
            for (int i = 0; i < filter.players().size(); i++) {
                if (i > 0) sql.append(", ");
                String param = "player_" + i;
                sql.append(":").append(param);
                bind(param, filter.players().get(i));
            }
            sql.append(")");
        }
        if (!filter.excludedPlayers().isEmpty()) {
            sql.append(" AND bl.cause_name NOT IN (");
            for (int i = 0; i < filter.excludedPlayers().size(); i++) {
                if (i > 0) sql.append(", ");
                String param = "excl_player_" + i;
                sql.append(":").append(param);
                bind(param, filter.excludedPlayers().get(i));
            }
            sql.append(")");
        }

        // Region filter
        if (filter.regionId() != null) {
            sql.append(" AND bl.region_id = :regionId");
            bind("regionId", filter.regionId());
        }

        // World filter
        if (filter.world() != null) {
            sql.append(" AND bl.world_name = :world");
            bind("world", filter.world());
        }

        // Time filters
        if (filter.since() != null) {
            sql.append(" AND bl.timestamp >= :since");
            bind("since", Timestamp.from(filter.since()));
        }
        if (filter.before() != null) {
            sql.append(" AND bl.timestamp <= :before");
            bind("before", Timestamp.from(filter.before()));
        }

        // Area filter (radius around a point)
        if (filter.radius() != null && filter.centerX() != null) {
            sql.append(" AND bl.world_name = :areaWorld");
            sql.append(" AND bl.x BETWEEN :areaMinX AND :areaMaxX");
            sql.append(" AND bl.y BETWEEN :areaMinY AND :areaMaxY");
            sql.append(" AND bl.z BETWEEN :areaMinZ AND :areaMaxZ");
            bind("areaWorld", filter.centerWorld());
            bind("areaMinX", filter.centerX() - filter.radius());
            bind("areaMaxX", filter.centerX() + filter.radius());
            bind("areaMinY", Math.max(-64, filter.centerY() - filter.radius()));
            bind("areaMaxY", Math.min(319, filter.centerY() + filter.radius()));
            bind("areaMinZ", filter.centerZ() - filter.radius());
            bind("areaMaxZ", filter.centerZ() + filter.radius());
        }

        // WorldEdit selection filter
        if (filter.selMinX() != null) {
            sql.append(" AND bl.world_name = :selWorld");
            sql.append(" AND bl.x BETWEEN :selMinX AND :selMaxX");
            sql.append(" AND bl.y BETWEEN :selMinY AND :selMaxY");
            sql.append(" AND bl.z BETWEEN :selMinZ AND :selMaxZ");
            bind("selWorld", filter.selWorld());
            bind("selMinX", filter.selMinX());
            bind("selMaxX", filter.selMaxX());
            bind("selMinY", filter.selMinY());
            bind("selMaxY", filter.selMaxY());
            bind("selMinZ", filter.selMinZ());
            bind("selMaxZ", filter.selMaxZ());
        }

        // Change type filter
        if (!filter.changeTypes().isEmpty()) {
            sql.append(" AND bl.change_type IN (");
            for (int i = 0; i < filter.changeTypes().size(); i++) {
                if (i > 0) sql.append(", ");
                String param = "changeType_" + i;
                sql.append(":").append(param);
                bind(param, filter.changeTypes().get(i).name());
            }
            sql.append(")");
        }

        // Block material filter (join to palette)
        if (!filter.blocks().isEmpty()) {
            List<String> materialKeys = filter.blocks().stream()
                    .map(m -> m.getKey().toString())
                    .toList();

            sql.append(" AND (old_p.material IN (");
            for (int i = 0; i < materialKeys.size(); i++) {
                if (i > 0) sql.append(", ");
                String param = "block_" + i;
                sql.append(":").append(param);
                bind(param, materialKeys.get(i));
            }
            sql.append(") OR new_p.material IN (");
            for (int i = 0; i < materialKeys.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(":block_").append(i); // reuse same params
            }
            sql.append("))");
        }

        if (!filter.excludedBlocks().isEmpty()) {
            List<String> excludedKeys = filter.excludedBlocks().stream()
                    .map(m -> m.getKey().toString())
                    .toList();

            sql.append(" AND old_p.material NOT IN (");
            for (int i = 0; i < excludedKeys.size(); i++) {
                if (i > 0) sql.append(", ");
                String param = "excl_block_" + i;
                sql.append(":").append(param);
                bind(param, excludedKeys.get(i));
            }
            sql.append(") AND new_p.material NOT IN (");
            for (int i = 0; i < excludedKeys.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(":excl_block_").append(i);
            }
            sql.append(")");
        }
    }

    private void bind(String name, Object value) {
        bindings.add(new Object[]{name, value});
    }
}

