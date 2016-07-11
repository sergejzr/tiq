package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InvitationEvent implements Serializable {
	private static final long serialVersionUID = 2L;
	private int id;
	private int source;
	private int sink;
	private Type type;
	// initialized if possible
	private GroupScoreEntry target;
	private long created;

	public InvitationEvent() {
		this.id = -1;
		this.created = System.currentTimeMillis();
	}

	public InvitationEvent(ResultSet rs) throws SQLException {
		this.id = rs.getInt("ie.event_id");
		this.source = rs.getInt("ie.source_id");
		this.sink = rs.getInt("ie.sink_id");
		this.type = Type.valueOf(rs.getString("ie.type"));
		this.created = rs.getLong("ie.created");
		try {
			this.target = new GroupScoreEntry(rs);
		} catch (SQLException e) {
		}
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getSink() {
		return sink;
	}

	public void setSink(int sink) {
		this.sink = sink;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public GroupScoreEntry getTarget() {
		return target;
	}

	public void setTarget(GroupScoreEntry target) {
		this.target = target;
	}

	public enum Type {
		INVITATION, OFFER;
	}

	public enum Result {
		ACCEPT, DECLINE, CANCEL;
	}

	public String toString() {
		return "InvitationEvent [id=" + this.id + ",sink=" + this.sink
				+ ",source=" + this.source + ", type=" + this.type.name()
				+ ", created=" + this.created + "]";
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

}
