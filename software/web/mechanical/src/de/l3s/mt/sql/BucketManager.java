package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.Bucket;

public class BucketManager extends SQLManager {
	private static Logger logger = Logger.getLogger(BucketManager.class);
	private String BUCKETTABLE;
	private String COLUMNS = " b.`user_id` AS 'b.user_id', b.`bucket_number` AS 'b.bucket_number', b.`progress` AS 'b.progress', "
			+ " b.`honey_pot_index` AS 'b.honey_pot_index', b.`honey_pot_person` AS 'b.honey_pot_person', b.`honey_pot_result` AS 'b.honey_pot_result' ";

	private Learnweb learnweb;

	public BucketManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		BUCKETTABLE = learnweb.getTablePrefix() + "bucket";
	}

	private Learnweb getLearnweb() {
		return learnweb;
	}

	/**
	 * retrieves bucket instance for the given user. If no bucket is found in
	 * the db, a new one is created, inserted and returned.
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public Bucket getBucket(int userId) throws SQLException {
		Bucket bucket;

		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = getLearnweb().getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + BUCKETTABLE
							+ "` b WHERE b.`user_id` = ? ");
			pStmt.setInt(1, userId);
			logger.debug("getBucket() executes: " + pStmt.toString());
			rs = pStmt.executeQuery();
			if (!rs.next()) {
				// user has no bucket yet
				bucket = new Bucket(userId);
				return insert(bucket);
			} else {
				bucket = new Bucket(rs);
			}
			return bucket;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	public Bucket insert(Bucket bucket) throws SQLException {
		PreparedStatement stmt = null;

		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"INSERT INTO `"
									+ BUCKETTABLE
									+ "` (user_id, bucket_number, progress, honey_pot_index, honey_pot_person, honey_pot_result) "
									+ "VALUES (?,?,?,?,?,?)");
			stmt.setInt(1, bucket.getId());
			stmt.setInt(2, bucket.getBucketNumber());
			stmt.setInt(3, bucket.getProgress());
			stmt.setInt(4, bucket.getHoneyPotIndex());
			stmt.setInt(5, bucket.getHoneyPotPerson());
			stmt.setBoolean(6, bucket.getHoneyPotResult());
			logger.debug("insert(Bucket) executes: " + stmt.toString());
			stmt.executeUpdate();
			return bucket;
		} finally {
			close(null, stmt);
		}
	}

	public Bucket save(Bucket bucket) throws SQLException {
		PreparedStatement stmt = null;

		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"UPDATE `"
									+ BUCKETTABLE
									+ "` SET bucket_number = ?, progress = ?, honey_pot_index = ?, "
									+ " honey_pot_person =?, honey_pot_result = ? WHERE user_id = ? ");
			stmt.setInt(1, bucket.getBucketNumber());
			stmt.setInt(2, bucket.getProgress());
			stmt.setInt(3, bucket.getHoneyPotIndex());
			stmt.setInt(4, bucket.getHoneyPotPerson());
			stmt.setBoolean(5, bucket.getHoneyPotResult());
			stmt.setInt(6, bucket.getId());
			logger.debug("update(Bucket) executes: " + stmt.toString());
			stmt.executeUpdate();
			return bucket;
		} finally {
			close(null, stmt);
		}
	}

}
