package de.l3s.mt.tools;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.sql.SQLManager;

public class CFJobManager extends SQLManager {
	private static Logger logger = Logger.getLogger(CFJobManager.class);
	private String CFJOBTABLE;
	private String COLUMNS = " j.`job_id` AS 'j.job_id', j.`unit_judgments_examined` AS 'j.unit_judgments_examined' ";

	private Learnweb learnweb;

	public CFJobManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		CFJOBTABLE = learnweb.getUserTablePrefix() + "cf_job";
	}

	private Learnweb getLearnweb() {
		return learnweb;
	}

	public CFJob getCFJob(int id) throws SQLException {
		CFJob job;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = getLearnweb().getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + CFJOBTABLE
							+ "` j WHERE j.`job_id` = ? ");
			pStmt.setInt(1, id);
			rs = pStmt.executeQuery();
			if (!rs.next())
				return null;

			job = new CFJob(rs);
			return job;
		} finally {
			close(rs, pStmt);
		}
	}

	public CFJob save(CFJob job) throws SQLException {
		PreparedStatement replace = null;
		ResultSet rs = null;
		try {
			//@formatter:off
			replace = learnweb.getConnection().prepareStatement(
					"REPLACE INTO `" + CFJOBTABLE + "` (job_id, unit_judgments_examined) " +
							"VALUES (?,?)");
			//@formatter:off
			replace.setInt(1, job.getId());
			replace.setInt(2, job.getUnitJudgmentsExamined());
			logger.debug("save(CFJob) executes: " + replace.toString());
			replace.executeUpdate();
			return job;
		} finally {
			close(rs, replace);
		}

	}
}
