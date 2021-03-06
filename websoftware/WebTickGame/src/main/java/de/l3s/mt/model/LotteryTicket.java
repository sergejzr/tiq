package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.util.HasId;

public class LotteryTicket implements Serializable, HasId {
	private static final long serialVersionUID = 7594883479337703548L;
	private int id;
	private int userId;
	private String ticketCode;
	private int prizeCents;
	private String anonName;

	private static Logger logger = Logger.getLogger(LotteryTicket.class);

	public LotteryTicket(int userId) {
		this.id = -1;
		this.userId = userId;
		prizeCents = 0;
		ticketCode = ""; // generated by dao
		anonName = "";
	}

	public LotteryTicket(ResultSet rs) throws SQLException {
		this.id = rs.getInt("lt.ticket_id");
		this.userId = rs.getInt("lt.user_id");
		this.ticketCode = rs.getString("lt.ticket_code");
		this.prizeCents = rs.getInt("lt.prize_cents");
		this.anonName = rs.getString("lt.anon_name");
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getTicketCode() {
		return ticketCode;
	}

	public void setTicketCode(String ticketCode) {
		this.ticketCode = ticketCode;
	}

	public int getPrizeCents() {
		return prizeCents;
	}

	public void setPrizeCents(int prizeCents) {
		this.prizeCents = prizeCents;
	}

	public void setAnonName(String anonName) {
		this.anonName = anonName;
	}

	public String getAnonName() {
		return this.anonName;
	}

	public String getPrizeDollars() {
		if (this.prizeCents > 0 && this.prizeCents < 100) {
			return (new Double(this.prizeCents / 100.0)).toString();
		} else {
			return (new Integer(this.prizeCents / 100)).toString();
		}
	}
}
