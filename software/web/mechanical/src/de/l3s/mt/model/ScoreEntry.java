package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.l3s.learnweb.User;

public class ScoreEntry implements Serializable {
	private static final long serialVersionUID = 7806957462561262369L;

	private int userId;
	private String username;
	private int score;
	private int rank;
	private long lastScoredTime;
	private int numTickets;

	// initial
	public ScoreEntry(User user) {
		userId = user.getId();
		username = user.getUsername();
		score = 0;
		numTickets = 0;
		// init rank to max int and then update it w/ stored procedure
		rank = 2147483647;
		lastScoredTime = System.currentTimeMillis();
	}

	public ScoreEntry(ResultSet rs) throws SQLException {
		update(rs);
	}

	public void update(ResultSet rs) throws SQLException {
		userId = rs.getInt("us.user_id");
		username = rs.getString("us.username");
		score = rs.getInt("us.score");
		rank = rs.getInt("us.rank");
		lastScoredTime = rs.getLong("us.last_scored");
		numTickets = rs.getInt("us.num_tickets");
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

	public int getNumTickets() {
		return this.numTickets;
	}

	public void setNumTickets(int numTickets) {
		this.numTickets = numTickets;
	}

	public String toString() {
		StringBuilder string = new StringBuilder();
		String SEPERATOR = ", ";

		string.append(this.getClass().getName() + " Object {");
		string.append("userId: " + getUserId() + SEPERATOR);
		string.append("username: " + getUsername() + SEPERATOR);
		string.append("score: " + getScore() + SEPERATOR);
		string.append("rank: " + getRank() + SEPERATOR);
		string.append("lastScoredTime:" + getLastScoredTime() + SEPERATOR);
		string.append("numTickets:" + getNumTickets());
		string.append("}");
		return string.toString();
	}
}
