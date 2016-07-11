package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * user_group relationship entry
 * 
 * @author markus
 */
public class UserGroup implements Serializable {

	private static final long serialVersionUID = 2L;
	private int userId;
	private int groupId;
	private UserGroupRole role;
	private int newestGroupMsg;
	private int oldestGroupMsg;
	private int newestOwnMsg;
	private int newestGroupMsgNotification;
	private int newestInvEvent;
	private int newestInvMsg;
	private int newestOwnInvEvent;
	private int newestOwnInvMsg;
	private int newestInvEventNotification;
	private int newestInvMsgNotification;

	public UserGroup(int userId, int groupId) {
		this.userId = userId;
		this.groupId = groupId;
		this.role = UserGroupRole.MEMBER;
		this.newestGroupMsg = 0;
		this.oldestGroupMsg = 1;
		this.newestOwnMsg = 0;
		this.newestGroupMsgNotification = 0;
		this.newestInvEvent = 0;
		this.newestInvMsg = 0;
		this.newestOwnInvEvent = 0;
		this.newestOwnInvMsg = 0;
		this.newestInvEventNotification = 0;
		this.newestInvMsgNotification = 0;
	}

	public UserGroup(ResultSet rs) throws SQLException {
		this.userId = rs.getInt("ug.user_id");
		this.groupId = rs.getInt("ug.group_id");
		this.role = UserGroupRole.valueOf(rs.getString("ug.role"));
		this.newestGroupMsg = rs.getInt("ug.newest_group_msg");
		this.oldestGroupMsg = rs.getInt("ug.oldest_group_msg");
		this.newestOwnMsg = rs.getInt("ug.newest_own_msg");
		this.newestGroupMsgNotification = rs
				.getInt("ug.newest_group_msg_notification");
		this.newestInvEvent = rs.getInt("ug.newest_inv_event");
		this.newestInvMsg = rs.getInt("ug.newest_inv_msg");
		this.newestOwnInvEvent = rs.getInt("ug.newest_own_inv_event");
		this.newestOwnInvMsg = rs.getInt("ug.newest_own_inv_msg");
		this.newestInvEventNotification = rs
				.getInt("ug.newest_inv_event_notification");
		this.newestInvMsgNotification = rs
				.getInt("ug.newest_inv_msg_notification");
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public UserGroupRole getRole() {
		return role;
	}

	public void setRole(UserGroupRole role) {
		this.role = role;
	}

	public int getNewestGroupMsg() {
		return newestGroupMsg;
	}

	public void setNewestGroupMsg(int newestGroupMsg) {
		this.newestGroupMsg = newestGroupMsg;
	}

	public int getOldestGroupMsg() {
		return oldestGroupMsg;
	}

	public void setOldestGroupMsg(int oldestGroupMsg) {
		this.oldestGroupMsg = oldestGroupMsg;
	}

	public int getNewestOwnMsg() {
		return newestOwnMsg;
	}

	public void setNewestOwnMsg(int newestOwnMsg) {
		this.newestOwnMsg = newestOwnMsg;
	}

	public int getNewestGroupMsgNotification() {
		return newestGroupMsgNotification;
	}

	public void setNewestGroupMsgNotification(int newestGroupMsgNotification) {
		this.newestGroupMsgNotification = newestGroupMsgNotification;
	}

	public int getNewestInvEvent() {
		return newestInvEvent;
	}

	public void setNewestInvEvent(int newestInvEvent) {
		this.newestInvEvent = newestInvEvent;
	}

	public int getNewestInvMsg() {
		return newestInvMsg;
	}

	public void setNewestInvMsg(int newestInvMsg) {
		this.newestInvMsg = newestInvMsg;
	}

	public int getNewestOwnInvEvent() {
		return newestOwnInvEvent;
	}

	public void setNewestOwnInvEvent(int newestOwnInvEvent) {
		this.newestOwnInvEvent = newestOwnInvEvent;
	}

	public int getNewestOwnInvMsg() {
		return newestOwnInvMsg;
	}

	public void setNewestOwnInvMsg(int newestOwnInvMsg) {
		this.newestOwnInvMsg = newestOwnInvMsg;
	}

	public int getNewestInvEventNotification() {
		return newestInvEventNotification;
	}

	public void setNewestInvEventNotification(int newestInvEventNotification) {
		this.newestInvEventNotification = newestInvEventNotification;
	}

	public int getNewestInvMsgNotification() {
		return newestInvMsgNotification;
	}

	public void setNewestInvMsgNotification(int newestInvMsgNotification) {
		this.newestInvMsgNotification = newestInvMsgNotification;
	}
}
