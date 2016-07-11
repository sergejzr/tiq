package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.InvitationEvent;
import de.l3s.mt.model.InvitationMessage;

public class InvitationManager extends SQLManager {
	private final static String MESSAGECOLUMNS = " im.`message_id` AS 'im.message_id', im.`group_id` AS 'im.group_id', "
			+ " im.`message` as 'im.message', im.`merge` AS 'im.merge' ";

	// TODO implement basics for invitation_event
	private final static String EVENTCOLUMNS = " ie.`event_id` AS 'ie.event_id', ie.`source_id` AS 'ie.source_id', "
			+ " ie.`sink_id` AS 'ie.sink_id', ie.`type` AS 'ie.type', ie.`created` AS 'ie.created' ";
	private String INVITATIONMESSAGETABLE;
	private String INVITATIONEVENTTABLE;

	private Learnweb learnweb;

	private static final Logger logger = Logger
			.getLogger(InvitationManager.class);

	public InvitationManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		INVITATIONMESSAGETABLE = learnweb.getTablePrefix()
				+ "invitation_message";
		INVITATIONEVENTTABLE = learnweb.getTablePrefix() + "invitation_event";
	}

	public InvitationMessage insert(InvitationMessage message)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"INSERT INTO " + INVITATIONMESSAGETABLE
							+ " (message_id, group_id, message,merge) "
							+ "VALUES (?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			if (message.getMessageId() < 1) {
				stmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(1, message.getMessageId());
			}
			stmt.setInt(2, message.getGroupId());
			stmt.setString(
					3,
					message.getMessage().substring(0,
							Math.min(message.getMessage().length(), 255)));
			stmt.setBoolean(4, message.isMerge());
			logger.debug("insert(InvitationMessage) executes: "
					+ stmt.toString());
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

	public InvitationEvent insert(InvitationEvent event) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"INSERT INTO " + INVITATIONEVENTTABLE
							+ " (event_id, source_id, sink_id, type, created) "
							+ "VALUES (?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			if (event.getId() < 1) {
				stmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(1, event.getId());
			}
			stmt.setInt(2, event.getSource());
			stmt.setInt(3, event.getSink());
			stmt.setString(4, event.getType().name());
			stmt.setLong(5, event.getCreated());
			logger.debug("insert(InvitationMessage) executes: "
					+ stmt.toString());
			stmt.executeUpdate();
			if (event.getId() < 1) {
				rs = stmt.getGeneratedKeys();
				if (!rs.next())
					throw new SQLException("database error: no id generated");
				event.setId(rs.getInt(1));
			}
			return event;
		} finally {
			close(null, stmt);
		}
	}

	public List<InvitationEvent> getInvitationEvents(int source, int sink)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<InvitationEvent> events = new ArrayList<InvitationEvent>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" SELECT " + EVENTCOLUMNS +
				" FROM `" + INVITATIONEVENTTABLE + "` AS ie " +
				" WHERE ie.`source_id` = ? AND ie.`sink_id` = ? ORDER BY ie.`event_id` DESC ");
			//@formatter:on
			stmt.setInt(1, source);
			stmt.setInt(2, sink);
			logger.debug("getInvitationEvents(int,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving invitation events for source "
								+ source + ", sink " + sink);

			while (rs.next()) {
				events.add(new InvitationEvent(rs));
			}
			return events;
		} finally {
			close(rs, stmt);
		}
	}

	/**
	 * retrieves all pending events for given group. if sinkAsTarget is set,
	 * target property of events contains sink. otherwise, target contains the
	 * group other than the given group.
	 * 
	 * @param groupId
	 * @param sinkAsTarget
	 * @return
	 * @throws SQLException
	 */
	public List<InvitationEvent> getInvitationEvents(int groupId,
			boolean sinkAsTarget) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<InvitationEvent> events = new ArrayList<InvitationEvent>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" SELECT " + EVENTCOLUMNS + ", " + GroupScoreManager.COLUMNS +  
				" FROM `" + INVITATIONEVENTTABLE + "` AS ie " +
				" JOIN `" +  learnweb.getGroupScoreManager().GROUPSCORETABLE + "` AS gs ON " +
					((sinkAsTarget) ? " ie.sink_id = gs.group_id " : 
						" (ie.source_id <> ? AND ie.source_id = gs.group_id) OR (ie.sink_id <> ? AND ie.sink_id = gs.group_id) ") +
				" JOIN `" + learnweb.getGroupManager().GROUPTABLE + "` AS g ON gs.group_id = g.group_id " +
				"WHERE ie.`source_id` = ? OR ie.`sink_id` = ? ORDER BY ie.`event_id` DESC ");
			//@formatter:on
			stmt.setInt(1, groupId);
			stmt.setInt(2, groupId);
			if (!sinkAsTarget) {
				stmt.setInt(3, groupId);
				stmt.setInt(4, groupId);
			}
			logger.debug("getInvitationEvents(int,boolean) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving invitation events for group "
								+ groupId);

			while (rs.next()) {
				events.add(new InvitationEvent(rs));
			}
			return events;
		} finally {
			close(rs, stmt);
		}
	}

	/**
	 * retrieves all pending events for given group. if sinkAsTarget is set,
	 * target property of events contains sink. otherwise, target contains the
	 * group other than the given group.
	 * 
	 * @param groupId
	 * @param sinkAsTarget
	 * @param oldest
	 *            oldest event to retrieve
	 * @return
	 * @throws SQLException
	 */
	public List<InvitationEvent> getInvitationEvents(int groupId,
			boolean sinkAsTarget, int oldest) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<InvitationEvent> events = new ArrayList<InvitationEvent>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" SELECT " + EVENTCOLUMNS + ", " + GroupScoreManager.COLUMNS +  
				" FROM `" + INVITATIONEVENTTABLE + "` AS ie " +
				" JOIN `" +  learnweb.getGroupScoreManager().GROUPSCORETABLE + "` AS gs ON " +
					((sinkAsTarget) ? " ie.sink_id = gs.group_id " : 
						" (ie.source_id <> ? AND ie.source_id = gs.group_id) OR (ie.sink_id <> ? AND ie.sink_id = gs.group_id) ") +
				" JOIN `" + learnweb.getGroupManager().GROUPTABLE + "` AS g ON gs.group_id = g.group_id " +
				"WHERE (ie.`source_id` = ? OR ie.`sink_id` = ?) AND ie.`event_id` >= ? ORDER BY ie.`event_id` DESC ");
			//@formatter:on
			stmt.setInt(1, groupId);
			stmt.setInt(2, groupId);
			if (!sinkAsTarget) {
				stmt.setInt(3, groupId);
				stmt.setInt(4, groupId);
				stmt.setInt(5, oldest);
			}
			stmt.setInt(3, oldest);
			logger.debug("getInvitationEvents(int,boolean,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving invitation events for group "
								+ groupId);

			while (rs.next()) {
				events.add(new InvitationEvent(rs));
			}
			return events;
		} finally {
			close(rs, stmt);
		}
	}

	public List<InvitationMessage> getInvitationMessages(int groupId, int oldest)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<InvitationMessage> messages = new ArrayList<InvitationMessage>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" SELECT " + MESSAGECOLUMNS + 
				" FROM `" + INVITATIONMESSAGETABLE + "` AS im " +
				" WHERE im.`message_id` >= ? AND im.`group_id` = ? ORDER BY im.`message_id` DESC ");
			//@formatter:on
			stmt.setInt(1, oldest);
			stmt.setInt(2, groupId);
			logger.debug("getInvitationMessages(int,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving invitation messages for group "
								+ groupId);

			while (rs.next()) {
				messages.add(new InvitationMessage(rs));
			}
			return messages;
		} finally {
			close(rs, stmt);
		}
	}

	public List<InvitationMessage> getMergeMessages(int groupId, int oldest)
			throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<InvitationMessage> messages = new ArrayList<InvitationMessage>();
		try {
			//@formatter:off
			stmt = learnweb.getConnection().prepareStatement(
				" SELECT " + MESSAGECOLUMNS + 
				" FROM `" + INVITATIONMESSAGETABLE + "` AS im " +
				" WHERE im.`message_id` >= ? AND im.`group_id` = ? and im.`merge` = 1 ORDER BY im.`message_id` DESC ");
			//@formatter:on
			stmt.setInt(1, oldest);
			stmt.setInt(2, groupId);
			logger.debug("getInvitationMergeMessages(int,int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (null == rs)
				throw new SQLException(
						"error while retrieving invitation messages for group "
								+ groupId);

			while (rs.next()) {
				messages.add(new InvitationMessage(rs));
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
					" FROM `" + INVITATIONMESSAGETABLE + "` " +
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

	public int getNewestEventId(int groupId) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			//@formatter:off
			stmt = this.learnweb.getConnection().prepareStatement(
					" SELECT max(`event_id`) " +
					" FROM `" + INVITATIONEVENTTABLE + "` " +
					" WHERE `source_id` = ? OR `sink_id` = ? ");
			//@formatter:on
			stmt.setInt(1, groupId);
			stmt.setInt(2, groupId);
			logger.debug("getNewestEventId(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (rs.next())
				result = rs.getInt(1);

			return result;
		} finally {
			close(rs, stmt);
		}
	}

	public int getNewestMergeMessageId(int groupId) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			//@formatter:off
			stmt = this.learnweb.getConnection().prepareStatement(
					" SELECT max(`message_id`) " +
					" FROM `" + INVITATIONMESSAGETABLE + "` " +
					" WHERE `group_id` = ? and `merge` = 1 ");
			//@formatter:on
			stmt.setInt(1, groupId);
			logger.debug("getNewestMergeMessageId(int) executes: "
					+ stmt.toString());
			rs = stmt.executeQuery();
			if (rs.next())
				result = rs.getInt(1);

			return result;
		} finally {
			close(rs, stmt);
		}
	}

	public InvitationEvent insertDeleted(InvitationEvent event,
			InvitationEvent.Result result) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb
					.getConnection()
					.prepareStatement(
							"INSERT INTO `"
									+ INVITATIONEVENTTABLE
									+ "_deleted` "
									+ " (event_id, source_id, sink_id, type, result, created) "
									+ "VALUES (?,?,?,?,?,?)",
							Statement.RETURN_GENERATED_KEYS);
			if (event.getId() < 1) {
				stmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(1, event.getId());
			}
			stmt.setInt(2, event.getSource());
			stmt.setInt(3, event.getSink());
			stmt.setString(4, event.getType().name());
			stmt.setString(5, result.name());
			stmt.setLong(6, event.getCreated());
			logger.debug("insertDeleted(InvitationMessage) executes: "
					+ stmt.toString());
			stmt.executeUpdate();
			if (event.getId() < 1) {
				rs = stmt.getGeneratedKeys();
				if (!rs.next())
					throw new SQLException("database error: no id generated");
				event.setId(rs.getInt(1));
			}
			return event;
		} finally {
			close(null, stmt);
		}
	}

	public InvitationEvent removeEvent(InvitationEvent event,
			InvitationEvent.Result result) throws SQLException {
		PreparedStatement stmt = null;
		try {
			insertDeleted(event, result);
			//@formatter:off
			stmt = this.learnweb.getConnection().prepareStatement(
					" DELETE FROM `" + INVITATIONEVENTTABLE + "`" +
					" WHERE event_id = ? ");
			//@formatter:on
			stmt.setInt(1, event.getId());
			logger.debug("removeEvent(InvitationEvent) executes: "
					+ stmt.toString());
			stmt.executeUpdate();
			return event;
		} finally {
			close(null, stmt);
		}

	}

	public InvitationEvent getEventById(int eventId) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			//@formatter:off
			stmt = this.learnweb.getConnection().prepareStatement(
					" SELECT " + EVENTCOLUMNS + 
					" FROM `" + INVITATIONEVENTTABLE + "` AS ie " +
					" WHERE ie.event_id = ? ");
			//@formatter:on
			stmt.setInt(1, eventId);
			logger.debug("getEventById(int) executes: " + stmt.toString());
			rs = stmt.executeQuery();
			if (!rs.next()) {
				logger.error("no invitationEvent with id " + eventId);
				return null;
			}
			return new InvitationEvent(rs);
		} finally {
			close(rs, stmt);
		}

	}
}
