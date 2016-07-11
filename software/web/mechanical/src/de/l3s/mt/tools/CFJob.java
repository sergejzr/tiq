package de.l3s.mt.tools;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.l3s.util.HasId;

public class CFJob implements HasId, Serializable {

	private static final long serialVersionUID = 1L;
	private int id;
	private int unitJudgmentsExamined;

	public CFJob(int id) {
		this.id = id;
		this.unitJudgmentsExamined = 0;
	}

	public CFJob(ResultSet rs) throws SQLException {
		this.id = rs.getInt("j.job_id");
		this.unitJudgmentsExamined = rs.getInt("j.unit_judgments_examined");
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUnitJudgmentsExamined() {
		return unitJudgmentsExamined;
	}

	public void setUnitJudgmentsExamined(int unitJudgmentsExamined) {
		this.unitJudgmentsExamined = unitJudgmentsExamined;
	}

}
