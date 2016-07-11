package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InvitationMessage implements Serializable {

	private static final long serialVersionUID = 2L;
	private int messageId;
	private int groupId;
	private String message;
	private boolean merge;

	public InvitationMessage() {
		this.messageId = -1;
		this.merge = false;
	}

	public InvitationMessage(ResultSet rs) throws SQLException {
		this.messageId = rs.getInt("im.message_id");
		this.groupId = rs.getInt("im.group_id");
		this.message = rs.getString("im.message");
		this.merge = rs.getBoolean("im.merge");
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isMerge() {
		return merge;
	}

	public void setMerge(boolean merge) {
		this.merge = merge;
	}

}
