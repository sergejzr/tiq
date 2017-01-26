package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

//import com.mysql.jdbc.Statement;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.Group;
import de.l3s.mt.model.GroupScoreEntry;
import de.l3s.mt.model.UserGroup;

public class GroupManager extends SQLManager {
	private static Logger logger = Logger.getLogger(GroupManager.class);
	public String GROUPTABLE;
	private String USERGROUPTABLE;
	public String GROUPCOLUMNS = " g.`group_id` AS 'g.group_id', g.`groupname` AS 'g.groupname', "
			+ " g.`num_members` AS 'g.num_members' ";
	private String USERGROUPCOLUMNS = " ug.`user_id` AS 'ug.user_id', ug.`group_id` AS 'ug.group_id', "
			+ " ug.`role` AS 'ug.role', ug.`newest_group_msg` AS 'ug.newest_group_msg', "
			+ " ug.`oldest_group_msg` AS 'ug.oldest_group_msg', ug.`newest_own_msg` AS 'ug.newest_own_msg', "
			+ " ug.`newest_group_msg_notification` AS 'ug.newest_group_msg_notification', "
			+ " ug.`newest_inv_event` AS 'ug.newest_inv_event', ug.`newest_inv_msg` AS 'ug.newest_inv_msg', "
			+ " ug.`newest_own_inv_event` AS 'ug.newest_own_inv_event', ug.`newest_own_inv_msg` AS 'ug.newest_own_inv_msg', "
			+ " ug.`newest_inv_event_notification` AS 'ug.newest_inv_event_notification', ug.`newest_inv_msg_notification` AS 'ug.newest_inv_msg_notification' ";
	private String MERGELOGTABLE;
	private String GROUPJOINTABLE;

	private Learnweb learnweb;

	public GroupManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		GROUPTABLE = learnweb.getTablePrefix() + "group";
		USERGROUPTABLE = learnweb.getTablePrefix() + "user_group";
		MERGELOGTABLE = learnweb.getTablePrefix() + "group_merge_log";
		GROUPJOINTABLE = learnweb.getTablePrefix() + "group_join_log";
	}

	private Learnweb getLearnweb() {
		return learnweb;
	}

	public Group getGroup(int groupId) throws SQLException {
		Group bucket = null;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = getLearnweb().getConnection().prepareStatement(
					"SELECT " + GROUPCOLUMNS + " FROM `" + GROUPTABLE
							+ "` g WHERE g.`group_id` = ? ");
			pStmt.setInt(1, groupId);
			logger.debug("getGroup(int) executes: " + pStmt.toString());
			rs = pStmt.executeQuery();
			if (rs.next()) {
				bucket = new Group(rs);
			}
			return bucket;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	public List<Group> getAllGroups() throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<Group> groups = new ArrayList<Group>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					" SELECT " + GROUPCOLUMNS + 
					" FROM `" + GROUPTABLE + "` g");
			//@formatter:on
			logger.debug("getAllGroups() executes: " + stmt.toString());
			rs = stmt.executeQuery();
			while (rs.next()) {
				groups.add(new Group(rs));
			}
			return groups;
		} finally {
			close(rs, stmt);
		}
	}

	public Group save(Group group) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			/*
			stmt = learnweb.getConnection()
					.prepareStatement(
							"REPLACE INTO `" + GROUPTABLE
									+ "` (group_id, groupname, num_members) "
									+ "VALUES (?,?,?)",
							Statement.RETURN_GENERATED_KEYS);
							*/
			if (group.getId() < 0) // the User is not yet stored at the database
				stmt.setNull(1, java.sql.Types.INTEGER);
			else
				stmt.setInt(1, group.getId());
			stmt.setString(2, group.getGroupname());
			stmt.setInt(3, group.getNumMembers());
			logger.debug("save(Group) executes: " + stmt.toString());
			stmt.executeUpdate();
			if (group.getId() < 0) {
				rs = stmt.getGeneratedKeys();
				if (!rs.next())
					throw new SQLException("database error: no id generated");
				group.setId(rs.getInt(1));
			}
			return group;
		} finally {
			close(rs, stmt);
		}
	}

	public UserGroup updateMsgStatistics(UserGroup userGroup)
			throws SQLException {
		PreparedStatement stmt = null;
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" UPDATE `" + USERGROUPTABLE + "` " +
				" SET newest_group_msg = ?, oldest_group_msg = ?, " + 
				" 	  newest_own_msg = ?, newest_group_msg_notification = ?, " +
				"     newest_inv_event = ?, newest_inv_msg = ?, " +
				"	  newest_own_inv_event = ?, newest_own_inv_msg = ?, " +
				"     newest_inv_event_notification = ?, newest_inv_msg_notification = ? " +
				" WHERE user_id = ? ");
			//@formatter:on
			stmt.setInt(1, userGroup.getNewestGroupMsg());
			stmt.setInt(2, userGroup.getOldestGroupMsg());
			stmt.setInt(3, userGroup.getNewestOwnMsg());
			stmt.setInt(4, userGroup.getNewestGroupMsgNotification());
			stmt.setInt(5, userGroup.getNewestInvEvent());
			stmt.setInt(6, userGroup.getNewestInvMsg());
			stmt.setInt(7, userGroup.getNewestOwnInvEvent());
			stmt.setInt(8, userGroup.getNewestOwnInvMsg());
			stmt.setInt(9, userGroup.getNewestInvEventNotification());
			stmt.setInt(10, userGroup.getNewestInvMsgNotification());
			stmt.setInt(11, userGroup.getUserId());
			logger.debug("updateMsgStatistics(UserGroup) executes: "
					+ stmt.toString());
			stmt.executeUpdate();
			return userGroup;
		} finally {
			close(null, stmt);
		}
	}

	public UserGroup save(UserGroup userGroup) throws SQLException {
		PreparedStatement stmt = null;
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" REPLACE INTO `" + USERGROUPTABLE + "` " +
				"		(user_id, group_id, role, newest_group_msg, oldest_group_msg, " + 
						" newest_own_msg, newest_group_msg_notification, newest_inv_event, newest_inv_msg, " +
						" newest_own_inv_event, newest_own_inv_msg, newest_inv_event_notification, newest_inv_msg_notification) " + 
				" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
			//@formatter:on
			stmt.setInt(1, userGroup.getUserId());
			stmt.setInt(2, userGroup.getGroupId());
			stmt.setString(3, userGroup.getRole().toString());
			stmt.setInt(4, userGroup.getNewestGroupMsg());
			stmt.setInt(5, userGroup.getOldestGroupMsg());
			stmt.setInt(6, userGroup.getNewestOwnMsg());
			stmt.setInt(7, userGroup.getNewestGroupMsgNotification());
			stmt.setInt(8, userGroup.getNewestInvEvent());
			stmt.setInt(9, userGroup.getNewestInvMsg());
			stmt.setInt(10, userGroup.getNewestOwnInvEvent());
			stmt.setInt(11, userGroup.getNewestOwnInvMsg());
			stmt.setInt(12, userGroup.getNewestInvEventNotification());
			stmt.setInt(13, userGroup.getNewestInvMsgNotification());
			logger.debug("save(UserGroup) executes: " + stmt.toString());
			stmt.executeUpdate();
			return userGroup;
		} finally {
			close(null, stmt);
		}
	}

	public UserGroup getUserGroupEntry(int userId) throws SQLException {
		UserGroup ug = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT " + USERGROUPCOLUMNS + 
					" FROM `" + USERGROUPTABLE + "` ug " + 
					" WHERE ug.`user_id` = ? ");
			//@formatter:on
			stmt.setInt(1, userId);
			logger.debug("getUserGroupEntry(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (rs.next()) {
				ug = new UserGroup(rs);
			}
			return ug;
		} finally {
			close(rs, stmt);
		}

	}

	/**
	 * Merges usergroup B into usergroup A. Does not recompute group scores!
	 * 
	 * @param groupB
	 *            id of group to merge into other group
	 * @param groupA
	 *            id of group to merge into
	 * @throws SQLException
	 */
	public void mergeGroups(int groupB, int groupA, int eventId)
			throws SQLException {
		PreparedStatement stmt = null;
		int maxId = 0;
		int maxMergeId = 0;
		try {
			maxId = getLearnweb().getGroupMessageManager().getNewestMessageId(
					groupA);
			maxMergeId = getLearnweb().getInvitationManager()
					.getNewestMergeMessageId(groupA);
		} catch (SQLException e) {
			logger.error("mergeGroups(): error getting maxMessageId", e);
		}
		try {
			logMerge(groupB, groupA, eventId);
			logJoins(groupB, groupA);
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					" UPDATE `" + USERGROUPTABLE + "` " +
					" SET `group_id` = ?, `role` = 'MEMBER', " +
					" `newest_group_msg` = 0, `oldest_group_msg` = 1, " + // set message counters to defaults
					" `newest_own_msg` = 0, `newest_group_msg_notification` = ?, " +
					" `newest_inv_event` = 0, `newest_inv_msg` = 0," +
					" `newest_own_inv_event` = 0, `newest_own_inv_msg` = 0, " +
					" `newest_inv_event_notification` = 0, `newest_inv_msg_notification` = ? " +
					" WHERE `group_id` = ? ");
			//@formatter:on
			stmt.setInt(1, groupA);
			stmt.setInt(2, maxId);
			stmt.setInt(3, maxMergeId);
			stmt.setInt(4, groupB);
			logger.debug("mergeGroups(int,int) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
	}

	public void logMerge(int joining, int join, int eventId)
			throws SQLException {
		PreparedStatement stmt = null;
		try {
			GroupScoreEntry joiningGroup = learnweb.getGroupScoreManager()
					.getScoreEntryById(joining);
			GroupScoreEntry joinGroup = learnweb.getGroupScoreManager()
					.getScoreEntryById(join);
			if (null == joiningGroup || null == joinGroup)
				throw new SQLException("Could not retrieve score entries");

			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					" INSERT INTO `" + MERGELOGTABLE + "` " +
					" (`join_group_id`, `join_group_size`, `join_group_score`, `join_group_rank`," +
					"  `joining_group_id`, `joining_group_size`, `joining_group_score`, `joining_group_rank`, `event_id`) " +
					" VALUES (?,?,?,?,?,?,?,?,?)");
			//@formatter:on
			stmt.setInt(1, joinGroup.getGroupId());
			stmt.setInt(2, joinGroup.getGroup().getNumMembers());
			stmt.setInt(3, joinGroup.getScore());
			stmt.setInt(4, joinGroup.getRank());
			stmt.setInt(5, joiningGroup.getGroupId());
			stmt.setInt(6, joiningGroup.getGroup().getNumMembers());
			stmt.setInt(7, joiningGroup.getScore());
			stmt.setInt(8, joiningGroup.getRank());
			stmt.setInt(9, eventId);
			logger.debug("logMerge(int,int) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
	}

	public void logJoins(int joining, int join) throws SQLException {
		PreparedStatement stmt = null;
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					" INSERT INTO `" + GROUPJOINTABLE + "` " +
					" (`user_id`, `group_id`) " +
					" SELECT user_id, ? from `" + USERGROUPTABLE + "` WHERE group_id = ? ");
			//@formatter:on
			stmt.setInt(1, joining);
			stmt.setInt(2, join);
			logger.debug("logJoins(int,int) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
	}

	public void logJoin(int userId, int groupId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
					" INSERT INTO `" + GROUPJOINTABLE + "` " +
					" (`user_id`, `group_id`) " +
					" VALUES (?,?) ");
			//@formatter:on
			stmt.setInt(1, userId);
			stmt.setInt(2, groupId);
			logger.debug("logJoin(int,int) executes: " + stmt.toString());
			stmt.executeUpdate();
		} finally {
			close(null, stmt);
		}
	}
}
