package de.erethon.factions.blocklog.dao;

import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.model.ChangeType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Row mapper for the {@link BlockChange} record.
 * Maps database columns to the record's constructor parameters.
 *
 * @author Malfrador
 */
public class BlockChangeMapper implements RowMapper<BlockChange> {

    @Override
    public BlockChange map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new BlockChange(
                rs.getLong("id"),
                rs.getInt("region_id"),
                rs.getInt("x"),
                rs.getShort("y"),
                rs.getInt("z"),
                rs.getString("world_name"),
                rs.getInt("old_block_id"),
                rs.getInt("new_block_id"),
                ChangeType.valueOf(rs.getString("change_type")),
                (UUID) rs.getObject("cause_uuid"),
                rs.getString("cause_name"),
                rs.getTimestamp("timestamp"),
                rs.getInt("baseline_version")
        );
    }
}

