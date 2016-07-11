package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import de.l3s.util.HasId;

public class BonusEvent implements HasId, Serializable {
	private static final long serialVersionUID = 2L;
	private int id;
	/** start of event in unix time (milliseconds) */
	private long start;
	/** end of event in unix time (milliseconds) */
	private long end;
	/** factor by which score is multiplied */
	private int bonusFactor;
	/** number of batches for which bonus points are granted */
	private int bonusPoolSize;
	/** number of batches for which bonus points where claimed */
	private int bonusClaimed;

	public BonusEvent(int bonusFactor, int bonusPoolSize, long start) {
		this.setBonusFactor(bonusFactor);
		this.setBonusPoolSize(bonusPoolSize);
		this.setStart(start);
		this.setEnd(start + 3600000); // duration of 1 hour
		this.setBonusClaimed(0);
	}

	public BonusEvent(ResultSet rs) throws SQLException {
		setId(rs.getInt("be.event_id"));
		setBonusFactor(rs.getInt("be.bonus_factor"));
		setBonusPoolSize(rs.getInt("be.bonus_pool_size"));
		setBonusClaimed(rs.getInt("be.bonus_claimed"));
		setStart(rs.getTimestamp("be.start").getTime());
		setEnd(rs.getTimestamp("be.end").getTime());
	}

	public int getRemainingAnnotations() {
		return this.getBonusPoolSize() - this.getBonusClaimed();
	}

	public int getRemainingBatches() {
		// round up
		return (getRemainingAnnotations() + 99) / 100;
	}

	public boolean isPoolDepleted() {
		return this.getBonusPoolSize() <= this.getBonusClaimed();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getBonusFactor() {
		return bonusFactor;
	}

	public void setBonusFactor(int bonusFactor) {
		this.bonusFactor = bonusFactor;
	}

	public int getBonusPoolSize() {
		return bonusPoolSize;
	}

	public void setBonusPoolSize(int bonusPoolSize) {
		this.bonusPoolSize = bonusPoolSize;
	}

	public int getBonusClaimed() {
		return bonusClaimed;
	}

	public void setBonusClaimed(int bonusClaimed) {
		this.bonusClaimed = bonusClaimed;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	@Override
	public String toString() {
		return "BonusEvent [id=" + id + ", start=" + (new Date(start))
				+ ", end=" + (new Date(end)) + ", bonusFactor=" + bonusFactor
				+ ", bonusPoolSize=" + bonusPoolSize + ", bonusClaimed="
				+ bonusClaimed + "]";
	}
}
