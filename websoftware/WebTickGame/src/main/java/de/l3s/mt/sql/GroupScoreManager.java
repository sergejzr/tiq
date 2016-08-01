package de.l3s.mt.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.Group;
import de.l3s.mt.model.GroupScoreEntry;
import de.l3s.mt.model.ScoreEntry;

public class GroupScoreManager extends SQLManager {
	public final static String COLUMNS = " gs.`group_id` AS 'gs.group_id', gs.`groupname` AS 'gs.groupname', "
			+ " gs.`score` AS 'gs.score', gs.`rank` AS 'gs.rank', gs.`last_scored` AS 'gs.last_scored', "
			+ " gs.`marked` AS 'gs.marked' ";
	public String GROUPSCORETABLE;
	private String USERGROUPTABLE;

	private Learnweb learnweb;

	private static final Logger logger = Logger
			.getLogger(GroupScoreManager.class);

	public GroupScoreManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		GROUPSCORETABLE = learnweb.getTablePrefix() + "group_score";
		USERGROUPTABLE = learnweb.getTablePrefix() + "user_group";
	}

	public GroupScoreEntry getScoreEntryById(int groupId) throws SQLException {
		GroupScoreEntry entry = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + GROUPSCORETABLE
							+ "` AS gs " + "WHERE gs.`group_id` = ?");
			stmt.setInt(1, groupId);
			logger.debug("getScoreEntryById(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving entry for group_id " + groupId
								+ ".");
			if (!rs.next()) {
				Group group = learnweb.getGroupManager().getGroup(groupId);
				entry = new GroupScoreEntry(group);
				List<ScoreEntry> userScores = learnweb.getScoreManager()
						.getGroupUserEntries(groupId);
				int groupScore = 0;
				long maxScored = Long.MIN_VALUE;
				for (ScoreEntry userEntry : userScores) {
					groupScore += userEntry.getScore();
					if (userEntry.getLastScoredTime() < maxScored) {
						maxScored = userEntry.getLastScoredTime();
					}
				}
				entry.setScore(groupScore);
				entry.setLastScoredTime(maxScored);
				return insert(entry);
			}
			entry = new GroupScoreEntry(rs);
			return entry;
		} finally {
			close(rs, stmt);
		}
	}

	public List<GroupScoreEntry> getAllScores() throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<GroupScoreEntry> scores = new ArrayList<GroupScoreEntry>();
		try {
			stmt = learnweb.getConnection().prepareStatement(
					" SELECT " + COLUMNS + " FROM `" + GROUPSCORETABLE
							+ "` AS gs ORDER BY gs.`rank` ASC ");
			logger.debug("getAllScores() executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException("error while retrieving score entries");
			while (rs.next()) {
				scores.add(new GroupScoreEntry(rs));
			}
			return scores;
		} finally {
			close(rs, stmt);
		}
	}

	public List<GroupScoreEntry> getAllScoresWithGroupInfo()
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<GroupScoreEntry> scores = new ArrayList<GroupScoreEntry>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					" SELECT " + COLUMNS + ", " + learnweb.getGroupManager().GROUPCOLUMNS + 
					" FROM `" + GROUPSCORETABLE + "` AS gs " +
						" JOIN `" + learnweb.getGroupManager().GROUPTABLE + "` AS g ON gs.`group_id` = g.`group_id` " + 
					" ORDER BY gs.`rank` ASC ");
			//@formatter:on
			logger.debug("getAllScoresWithGroupInfo() executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException("error while retrieving score entries");
			while (rs.next()) {
				scores.add(new GroupScoreEntry(rs));
			}
			return scores;
		} finally {
			close(rs, stmt);
		}
	}

	public List<GroupScoreEntry> getTopKEntries(int k) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<GroupScoreEntry> scores = new ArrayList<GroupScoreEntry>();
		if (k < 1)
			return scores;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					" SELECT " + COLUMNS + " FROM `" + GROUPSCORETABLE
							+ "` AS gs " + " ORDER BY gs.`rank` ASC "
							+ " LIMIT ?");
			stmt.setInt(1, k);
			logger.debug("getTopKScores(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException("error while retrieving score entries");
			while (rs.next()) {
				scores.add(new GroupScoreEntry(rs));
			}
			return scores;
		} finally {
			close(rs, stmt);
		}
	}

	/**
	 * returns a list of group score entries with ranks >= startRank and <=
	 * endRank.
	 * 
	 * @throws SQLException
	 */
	public List<GroupScoreEntry> getScoresBetween(int startRank, int endRank)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<GroupScoreEntry> scores = new ArrayList<GroupScoreEntry>();
		if (startRank < 1)
			return scores;
		if (endRank < startRank)
			return scores;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + GROUPSCORETABLE
							+ "` AS gs "
							+ "WHERE gs.`rank` >= ? AND gs.`rank` <= ? "
							+ "ORDER BY gs.`rank` ASC ");
			stmt.setInt(1, startRank);
			stmt.setInt(2, endRank);
			logger.debug("getScoresBetween(int,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				return scores;

			while (rs.next()) {
				GroupScoreEntry entry = null;
				try {
					entry = new GroupScoreEntry(rs);
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

	/**
	 * calls stored procedure that increments the score of the group and
	 * recomputes the rank information of the whole group_score table.
	 * 
	 * @throws SQLException
	 */
	public boolean incrementScore(int groupId, int increment)
			throws SQLException {
		CallableStatement stmt = null;
		// ResultSet rs = null;
		boolean success = false;
		try {
			// CallableStatment
			stmt = learnweb.getConnection().prepareCall(
					"{call `" + learnweb.getTablePrefix()
							+ "inc_group_score`(?,?,?,?)}");
			stmt.setInt(1, groupId);
			stmt.setInt(2, increment);
			stmt.setLong(3, System.currentTimeMillis());
			stmt.registerOutParameter(4, java.sql.Types.BOOLEAN);
			logger.debug("incrementScore(int,int) executes: " + stmt.toString());
			stmt.executeUpdate();
			success = stmt.getBoolean("success");
			logger.debug("incrementScore() returned success");
			return success;
		} finally {
			close(null, stmt);
		}
	}

	/**
	 * Inserts a new GroupScoreEntry. This needs to be done one time upon
	 * creation of a group.
	 * 
	 * @throws SQLException
	 */
	public GroupScoreEntry insert(GroupScoreEntry entry) throws SQLException {
		PreparedStatement stmt = null;

		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"INSERT INTO "
									+ GROUPSCORETABLE
									+ " (group_id, groupname, score, rank, last_scored, marked) "
									+ "VALUES (?,?,?,?,?,?)");
			stmt.setInt(1, entry.getGroupId());
			stmt.setString(2, entry.getGroupname());
			stmt.setInt(3, entry.getScore());
			stmt.setInt(4, entry.getRank());
			stmt.setLong(5, entry.getLastScoredTime());
			stmt.setBoolean(6, entry.isMarked());
			logger.debug("insert(GroupScoreEntry) executes: " + stmt.toString());
			stmt.executeUpdate();
			return update(entry);
		} finally {
			close(null, stmt);
		}
	}

	/**
	 * Updates rank (all) information for a given group score entry instance.
	 * Currently this method updates the ranks in the whole group score table
	 * before retrieving updated information for the given entry. <br/>
	 * 
	 * @throws SQLException
	 */
	public GroupScoreEntry update(GroupScoreEntry entry) throws SQLException {
		// update all scores
		incrementScore(entry.getGroupId(), 0);
		// retrieve updated rank information for the entry
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + GROUPSCORETABLE
							+ "` AS gs " + "WHERE gs.`group_id` = ?");
			stmt.setInt(1, entry.getGroupId());
			logger.debug("update(GroupScoreEntry) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving entry with group_id "
								+ entry.getGroupId() + ".");
			if (!rs.next())
				throw new SQLException(
						"no result was returned for entry with group_id "
								+ entry.getGroupId() + ".");
			// update entry
			entry.update(rs);
			return entry;
		} finally {
			close(rs, stmt);
		}
	}

	/**
	 * deletes score entry for the given group id
	 * 
	 * @param groupId
	 * @throws SQLException
	 */
	public void deleteGroupScoreEntry(int groupId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"DELETE FROM `" + GROUPSCORETABLE
							+ "` WHERE `group_id` = ? ");
			stmt.setInt(1, groupId);
			logger.debug("deleteGroupScoreEntry(int) executes:"
					+ stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
	}

	public void setMarked(int groupId, boolean marked) throws SQLException {
		PreparedStatement stmt = null;
		try {
			//@formatter:off
			stmt = this.learnweb.getConnection().prepareStatement(
					" UPDATE `" + GROUPSCORETABLE + "` SET marked = ? WHERE group_id = ? ");
			//@formatter:on
			stmt.setBoolean(1, marked);
			stmt.setInt(2, groupId);
			logger.debug("setMarked(int,boolean) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
	}

}
