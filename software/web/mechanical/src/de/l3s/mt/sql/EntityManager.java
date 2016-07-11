package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.Entity;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;

public class EntityManager extends SQLManager {
	private String ENTITYTABLE;
	private String COLUMNS = " e.`entity_id` AS 'e.entity_id', e.`name` AS 'e.name' ";

	private Learnweb learnweb;
	private int cacheSize;

	private ICache<Entity> cache;

	public EntityManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		ENTITYTABLE = learnweb.getTablePrefix() + "entity";
		cacheSize = Integer.parseInt(getLearnweb().getProperties().getProperty(
				"ENTITY_CACHE"));

		this.cache = (cacheSize == 0) ? new DummyCache<Entity>()
				: new Cache<Entity>(cacheSize);
	}

	private Learnweb getLearnweb() {
		return learnweb;
	}

	public Entity getEntity(int id) throws SQLException {
		Entity entity;
		entity = cache.get(id);
		if (null != entity) {
			return entity;
		}

		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = getLearnweb().getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + ENTITYTABLE
							+ "` e WHERE e.`entity_id` = ? ");
			pStmt.setInt(1, id);
			rs = pStmt.executeQuery();
			if (!rs.next())
				return null;

			entity = new Entity(rs);
			cache.put(entity);
			return entity;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}
}
