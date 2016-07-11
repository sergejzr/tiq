package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GroupMessage implements Serializable {

	private static final long serialVersionUID = 3L;
	private int messageId;
	private int groupId;
	private int userId;
	private String username;
	private String message;
	private long timestamp;

	public GroupMessage() {
		this.messageId = -1;
	}

	public GroupMessage(ResultSet rs) throws SQLException {
		this.messageId = rs.getInt("gm.message_id");
		this.groupId = rs.getInt("gm.group_id");
		this.userId = rs.getInt("gm.user_id");
		this.username = rs.getString("gm.username");
		this.message = rs.getString("gm.message");
		this.timestamp = rs.getTimestamp("gm.timestamp").getTime();
	}

	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
