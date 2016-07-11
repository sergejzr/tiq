package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.GroupMessage;

public class GroupMessageManager extends SQLManager {
	private final static String COLUMNS = " gm.`message_id` AS 'gm.message_id', gm.`group_id` AS 'gm.group_id', "
			+ " gm.`user_id` AS 'gm.user_id', gm.`username` AS 'gm.username', gm.`message` as 'gm.message', "
			+ " gm.`timestamp` AS 'gm.timestamp' ";
	private String GROUPMESSAGETABLE;

	private Learnweb learnweb;

	private static final Logger logger = Logger
			.getLogger(GroupMessageManager.class);

	public GroupMessageManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		GROUPMESSAGETABLE = learnweb.getTablePrefix() + "group_message";
	}

	public GroupMessage insert(GroupMessage message) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"INSERT INTO "
									+ GROUPMESSAGETABLE
									+ " (message_id, group_id, user_id, username, message) "
									+ "VALUES (?,?,?,?,?)",
							Statement.RETURN_GENERATED_KEYS);
			// timestamp set by database
			if (message.getMessageId() < 1) {
				stmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(1, message.getMessageId());
			}
			stmt.setInt(2, message.getGroupId());
			stmt.setInt(3, message.getUserId());
			stmt.setString(4, message.getUsername());
			stmt.setString(
					5,
					message.getMessage().substring(0,
							Math.min(message.getMessage().length(), 255)));
			logger.debug("insert(GroupMessage) executes: " + stmt.toString());
			stmt.executeUpdate();
			if (message.getMessageId() < 1) {
				rs = stmt.getGeneratedKeys();
				if (!rs.next())
					throw new SQLException("database error: no id generated");
				message.setMessageId(rs.getInt(1));
			}
			return message;
		} finally {
			close(null, stmt);
		}
	}

	public List<GroupMessage> getGroupMessages(int groupId, int oldest)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<GroupMessage> messages = new ArrayList<GroupMessage>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" SELECT " + COLUMNS + 
				" FROM `" + GROUPMESSAGETABLE + "` AS gm " +
				" WHERE gm.`message_id` >= ? AND gm.`group_id` = ? ORDER BY gm.`message_id` DESC ");
			//@formatter:on
			stmt.setInt(1, oldest);
			stmt.setInt(2, groupId);
			logger.debug("getGroupMessages(int,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving group messages for group "
								+ groupId);

			while (rs.next()) {
				messages.add(new GroupMessage(rs));
			}
			return messages;
		} finally {
			close(rs, stmt);
		}
	}

	public int getNewestMessageId(int groupId) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			//@formatter:off
			stmt = this.learnweb.getConnection().prepareStatement(
					" SELECT max(`message_id`) " +
					" FROM `" + GROUPMESSAGETABLE + "` " +
					" WHERE `group_id` = ? ");
			//@formatter:on
			stmt.setInt(1, groupId);
			logger.debug("getNewestMessageId(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (rs.next())
				result = rs.getInt(1);

			return result;
		} finally {
			close(rs, stmt);
		}

	}
}
