package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;

public class GroupScoreEntry implements Serializable {
	private static final long serialVersionUID = 2;
	private static final Logger logger = Logger
			.getLogger(GroupScoreEntry.class);

	private int groupId;
	private String groupname;
	private int score;
	private int rank;
	private long lastScoredTime;
	private boolean marked;
	// not part of the entity itself
	private Group group;

	// initial
	public GroupScoreEntry(int groupId) {
		this.groupId = groupId;
		this.score = 0;
		this.rank = 0;
		this.lastScoredTime = System.currentTimeMillis();
		this.marked = false;
	}

	public GroupScoreEntry(Group group) {
		groupId = group.getId();
		groupname = group.getGroupname();
		score = 0;
		// init rank to max int and then update it w/ stored procedure
		rank = 2147483647;
		lastScoredTime = System.currentTimeMillis();
		marked = false;
		this.group = group;
	}

	public GroupScoreEntry(ResultSet rs) throws SQLException {
		update(rs);
	}

	public void update(ResultSet rs) throws SQLException {
		groupId = rs.getInt("gs.group_id");
		groupname = rs.getString("gs.groupname");
		score = rs.getInt("gs.score");
		rank = rs.getInt("gs.rank");
		lastScoredTime = rs.getLong("gs.last_scored");
		marked = rs.getBoolean("gs.marked");
		// instantiate group if information was retrieved only
		try {
			this.group = new Group(rs);
		} catch (SQLException e) {
		}
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public long getLastScoredTime() {
		return lastScoredTime;
	}

	public void setLastScoredTime(long lastScoredTime) {
		this.lastScoredTime = lastScoredTime;
	}

	public void updateTime() {
		lastScoredTime = System.currentTimeMillis();
	}

	public String toString() {
		StringBuilder string = new StringBuilder();
		String SEPERATOR = ", ";

		string.append(this.getClass().getName() + " Object {");
		string.append("groupId: " + getGroupId() + SEPERATOR);
		string.append("groupname: " + getGroupname() + SEPERATOR);
		string.append("score: " + getScore() + SEPERATOR);
		string.append("rank: " + getRank() + SEPERATOR);
		string.append("lastScoredTime:" + getLastScoredTime() + SEPERATOR);
		string.append("marked:" + isMarked() + SEPERATOR);
		string.append("}");
		return string.toString();
	}

	public boolean isMarked() {
		return marked;
	}

	public void setMarked(boolean marked) {
		this.marked = marked;
	}

	public Group getGroup() {
		if (null == this.group) {
			try {
				this.group = UtilBean.getLearnwebBean().getLearnweb()
						.getGroupManager().getGroup(this.groupId);
			} catch (SQLException e) {
				logger.error("getGroup(): could not retrieve group "
						+ this.groupId, e);
			}
		}
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}
}
