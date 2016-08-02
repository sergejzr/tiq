package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.mt.model.ScoreEntry;

public class ScoreManager extends SQLManager {
	private final static String COLUMNS = " us.`user_id` AS 'us.user_id', us.`username` AS 'us.username', "
			+ " us.`rank` AS 'us.rank', us.`score` AS 'us.score', us.`last_scored` AS 'us.last_scored', "
			+ " us.`num_tickets` AS 'us.num_tickets' ";
	private String SCORETABLE;
	private String USERGROUPTABLE;

	private Learnweb learnweb;

	private static final Logger logger = Logger.getLogger(ScoreManager.class);

	public ScoreManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		SCORETABLE = learnweb.getTablePrefix() + "user_score";
		USERGROUPTABLE = learnweb.getTablePrefix() + "user_group";
	}

	/**
	 * Inserts a new ScoreEntry. This needs to be done one time upon
	 * registration of a user.
	 * 
	 * @throws SQLException
	 */
	public ScoreEntry insert(ScoreEntry entry) throws SQLException {
		PreparedStatement stmt = null;

		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"INSERT INTO "
									+ SCORETABLE
									+ " (user_id, username, score, rank, last_scored, num_tickets) "
									+ "VALUES (?,?,?,?,?,?)");
			stmt.setInt(1, entry.getUserId());
			stmt.setString(2, entry.getUsername());
			stmt.setInt(3, entry.getScore());
			stmt.setInt(4, entry.getRank());
			stmt.setLong(5, entry.getLastScoredTime());
			stmt.setInt(6, entry.getNumTickets());
			System.out.println("insert(ScoreEntry) executes: " + stmt.toString());
			logger.debug("insert(ScoreEntry) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
		return update(entry);
	}

	/**
	 * calls stored procedure that updates the rank information of the whole
	 * user_score table.
	 * 
	 * @throws SQLException
	 */
	public void updateScores() throws SQLException {
		
		//SZ:// need to add the procedure
		if(true) return;
		PreparedStatement stmt = null;
		try {
			// CallableStatment
			stmt = learnweb.getConnection()
					.prepareCall(
							"{call `" + learnweb.getTablePrefix()
									+ "update_scores`()}");
			logger.debug("update(ScoreEntry) executes: " + stmt.toString());
			stmt.executeQuery();
		} finally {
			close(null, stmt);
		}
	}

	/**
	 * Updates rank (all) information for a given score entry instance.
	 * Currently this method updates the ranks in the whole score table before
	 * retrieving updated information for the given entry. <br/>
	 * This method does <b>not</b> update the score of the entry!
	 * 
	 * @throws SQLException
	 */
	public ScoreEntry update(ScoreEntry entry) throws SQLException {
		// update all scores
		updateScores();
		// retrieve updated rank information for the entry
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + SCORETABLE + "` AS us "
							+ "WHERE us.`user_id` = ?");
			stmt.setInt(1, entry.getUserId());
			logger.debug("update(ScoreEntry) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving entry with user_id "
								+ entry.getUserId() + ".");
			if (!rs.next())
				throw new SQLException(
						"no result was returned for entry with user_id "
								+ entry.getUserId() + ".");
			// update entry
			entry.update(rs);
		} finally {
			close(rs, stmt);
		}
		return entry;
	}

	public ScoreEntry getScoreEntryByUser(User user) throws SQLException {
		ScoreEntry entry = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + SCORETABLE + "` AS us "
							+ "WHERE us.`user_id` = ?");
			stmt.setInt(1, user.getId());
			logger.debug("getScoreEntryById(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving entry for user_id "
								+ user.getId() + ".");
			if (!rs.next()) {
				// user has no bucket yet
				entry = new ScoreEntry(user);
				return insert(entry);
			} else {
				entry = new ScoreEntry(rs);
			}
		} finally {
			close(rs, stmt);
		}
		return entry;
	}

	/**
	 * updates existing entry's score and timestamp in database. rank
	 * information of the score table and the given score entry are subsequently
	 * updated by calling update(ScoreEntry).
	 * 
	 * @throws SQLException
	 */
	public ScoreEntry saveScore(ScoreEntry entry) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"UPDATE `"
									+ SCORETABLE
									+ "` AS us SET us.`score` = ?, us.`last_scored` = ?, us.`num_tickets` = ? "
									+ "WHERE us.`user_id` = ? ");
			stmt.setInt(1, entry.getScore());
			stmt.setLong(2, System.currentTimeMillis());
			stmt.setInt(3, entry.getNumTickets());
			stmt.setInt(4, entry.getUserId());
			logger.debug("saveScore(ScoreEntry) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
		return update(entry);
	}

	/**
	 * updates all information of the score entry in the data base.<br/>
	 * this method _does not_ check whether a row for the user already exists!<br/>
	 * 
	 * The rank information is not updated automatically by calling this method!
	 * 
	 * @param entry
	 * @return
	 * @throws SQLException
	 */
	public ScoreEntry save(ScoreEntry entry) throws SQLException {

		PreparedStatement update = null;
		try {
			update = learnweb
					.getConnection()
					.prepareStatement(
							"UPDATE `"
									+ SCORETABLE
									+ "` SET "
									+ " `username` = ?, `score` = ?, `rank` = ?, `last_scored` = ?, "
									+ " `num_tickets` = ? "
									+ " WHERE `user_id` = ? ");
			update.setString(1, entry.getUsername());
			update.setInt(2, entry.getScore());
			update.setInt(3, entry.getRank());
			update.setLong(4, entry.getLastScoredTime());
			update.setInt(5, entry.getNumTickets());
			update.setInt(6, entry.getUserId());
			logger.debug("save(ScoreEntry) executes: " + update.toString());
			update.executeUpdate();
		} finally {
			close(null, update);
		}
		return entry;
	}

	public List<ScoreEntry> getAllScores() throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<ScoreEntry> scores = new ArrayList<ScoreEntry>();
		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							" SELECT "
									+ COLUMNS
									+ " FROM `"
									+ SCORETABLE
									+ "` AS us ORDER BY us.`score` DESC, us.`last_scored` DESC ");
			logger.debug("getAllScores() executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException("error while retrieving score entries");
			while (rs.next()) {
				scores.add(new ScoreEntry(rs));
			}
		} finally {
			close(rs, stmt);
		}
		return scores;
	}

	public List<ScoreEntry> getTopKEntries(int k) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<ScoreEntry> scores = new ArrayList<ScoreEntry>();
		if (k < 1)
			return scores;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					" SELECT " + COLUMNS + " FROM `" + SCORETABLE + "` AS us "
							+ " ORDER BY us.`rank` ASC " + " LIMIT ?");
			stmt.setInt(1, k);
			logger.debug("getTopKScores(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException("error while retrieving score entries");
			while (rs.next()) {
				scores.add(new ScoreEntry(rs));
			}
		} finally {
			close(rs, stmt);
		}
		return scores;
	}

	/**
	 * returns a list of score entries with ranks >= startRank and <= endRank.
	 * 
	 * @throws SQLException
	 */
	public List<ScoreEntry> getScoresBetween(int startRank, int endRank)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<ScoreEntry> scores = new ArrayList<ScoreEntry>();
		if (startRank < 1)
			return scores;
		if (endRank < startRank)
			return scores;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + SCORETABLE + "` AS us "
							+ "WHERE us.`rank` >= ? AND us.`rank` <= ? "
							+ "ORDER BY us.`rank` ASC ");
			stmt.setInt(1, startRank);
			stmt.setInt(2, endRank);
			logger.debug("getScoresBetween(int,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				return scores;

			while (rs.next()) {
				ScoreEntry entry = null;
				try {
					entry = new ScoreEntry(rs);
				} catch (Exception e) {
				}
				if (null != entry) {
					scores.add(entry);
				}
			}
			return scores;
		} finally {
			close(rs, stmt);
		}
	}

	public List<ScoreEntry> getGroupUserEntries(int groupId)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<ScoreEntry> scores = new ArrayList<ScoreEntry>();
		if (groupId < 1)
			return scores;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					" SELECT " + COLUMNS + " FROM `" + USERGROUPTABLE + "` ug "
							+ " JOIN `" + SCORETABLE
							+ "` AS us ON ug.`user_id` = us.`user_id` "
							+ " WHERE ug.group_id = ? ORDER BY us.`rank` ASC ");
			stmt.setInt(1, groupId);
			logger.debug("getGroupUserEntries(int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving group user score entries");
			while (rs.next()) {
				scores.add(new ScoreEntry(rs));
			}
		} finally {
			close(rs, stmt);
		}
		return scores;
	}
}
