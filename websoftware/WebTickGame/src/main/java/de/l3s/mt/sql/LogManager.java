package de.l3s.mt.sql;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

public class LogManager extends SQLManager implements Observer {

	private Learnweb learnweb;

	// image log
	private String IMAGELOGTABLE;
	private String IMAGELOGSUFFIXTABLE;
	private String IMAGELOGBASETABLE;
	private PreparedStatement imageLogStmt;
	private int imageLogBatchSize = 0;
	private int imageLogRowNum = 0;
	private static int MAXILOGROWNUM = 100000;

	// user log
	private String USERLOGTABLE;
	private String USERLOGSUFFIXTABLE;
	private String USERLOGBASETABLE;
	private PreparedStatement userLogStmt;
	private int userLogBatchSize = 0;
	private int userLogRowNum = 0;
	private static int MAXULOGROWNUM = 10000;

	private static Logger logger = Logger.getLogger(LogManager.class);

	public LogManager(Learnweb learnweb) throws SQLException {
		this.learnweb = learnweb;
		try {
			MAXULOGROWNUM = Integer.parseInt(learnweb.getProperties()
					.getProperty("MAXULOGROWNUM"));
		} catch (NumberFormatException e) {
			logger.error("error while getting maximum user log row number.", e);
		}
		try {
			MAXILOGROWNUM = Integer.parseInt(learnweb.getProperties()
					.getProperty("MAXILOGROWNUM"));
		} catch (NumberFormatException e) {
			logger.error("error while getting maximum image log row number.", e);
		}

		learnweb.addObserver(this);
		// init table (names)
		USERLOGSUFFIXTABLE = learnweb.getTablePrefix() + "user_log_suffixes";
		USERLOGBASETABLE = learnweb.getTablePrefix() + "user_log";
		initUserLogTable();
		IMAGELOGSUFFIXTABLE = learnweb.getTablePrefix() + "image_log_suffixes";
		IMAGELOGBASETABLE = learnweb.getTablePrefix() + "image_log";
		initImageLogTable();
		// init prepared statements
		initStmts();
		logger.info("Logmanager initialized. ULogTable: " + USERLOGTABLE
				+ ", ILogTable: " + IMAGELOGTABLE);
	}

	private void initStmts() throws SQLException {
		initUserLogStmt();
		initImageLogStmt();
	}

	// user log table management
	private void initUserLogTable() throws SQLException {
		USERLOGTABLE = getCurrentUserLogTable();
		userLogRowNum = getUserLogRowNum();
	}

	private int getUserLogRowNum() throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT COUNT(*) FROM " + USERLOGTABLE);
			rs = stmt.executeQuery();

			if (!rs.next())
				return 0;
			int rows = rs.getInt(1);
			return rows;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, stmt);
		}
	}

	private void initNextUserLogTable() throws SQLException {
		USERLOGTABLE = getNextUserLogTable();
		userLogRowNum = 0;
	}

	private boolean hasULogEnoughRows() {
		return userLogRowNum >= MAXULOGROWNUM;
	}

	private String getCurrentUserLogTable() throws SQLException {
		int suff = -1;
		try {
			suff = getCurrentULogSuffix();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		if (suff < 1) { // first run maybe?
			return getNextUserLogTable();
		}
		return getUserLogTableName(suff);
	}

	private String getNextUserLogTable() throws SQLException {
		logger.info("creating next user log table");
		int suff = getNextULogSuffix();
		createUserLogTable(suff);
		return getUserLogTableName(suff);
	}

	private String getUserLogTableName(int suffix) {
		return USERLOGBASETABLE + "_p" + suffix;
	}

	private void createUserLogTable(int suffix) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"CREATE TABLE IF NOT EXISTS " + getUserLogTableName(suffix)
							+ " LIKE " + USERLOGBASETABLE);
			stmt.execute();
		} catch (SQLException e) {
			throw e;
		} finally {
			close(null, stmt);
		}
	}

	private int getNextULogSuffix() throws SQLException {
		int number = 0;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = learnweb.getConnection()
					.prepareStatement(
							"INSERT INTO " + USERLOGSUFFIXTABLE
									+ " VALUES(NULL, NULL)",
							Statement.RETURN_GENERATED_KEYS);
			pStmt.executeUpdate();
			rs = pStmt.getGeneratedKeys();
			if (!rs.next())
				throw new SQLException(
						"Problem while retrieving new ULog table suffix.");
			number = rs.getInt(1);
			return number;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	private int getCurrentULogSuffix() throws SQLException {
		int number = 0;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = learnweb.getConnection().prepareStatement(
					"SELECT MAX(id) from " + USERLOGSUFFIXTABLE);
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new SQLException(
						"Problem while retrieving current ULog table suffix.");
			number = rs.getInt(1);
			return number;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	// image log table management
	private void initImageLogTable() throws SQLException {
		IMAGELOGTABLE = getCurrentImageLogTable();
		imageLogRowNum = getImageLogRowNum();
	}

	private int getImageLogRowNum() throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"SELECT COUNT(*) FROM " + IMAGELOGTABLE);
			rs = stmt.executeQuery();

			if (!rs.next())
				return 0;
			int rows = rs.getInt(1);
			return rows;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, stmt);
		}
	}

	private void initNextImageLogTable() throws SQLException {
		IMAGELOGTABLE = getNextImageLogTable();
		imageLogRowNum = 0;
	}

	private boolean hasILogEnoughRows() {
		return imageLogRowNum >= MAXILOGROWNUM;
	}

	private String getCurrentImageLogTable() throws SQLException {
		int suff = -1;
		try {
			suff = getCurrentILogSuffix();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		if (suff < 1) { // first run maybe?
			return getNextImageLogTable();
		}
		return getImageLogTableName(suff);
	}

	private String getNextImageLogTable() throws SQLException {
		logger.info("creating next image log table");
		int suff = getNextILogSuffix();
		createImageLogTable(suff);
		return getImageLogTableName(suff);
	}

	private String getImageLogTableName(int suffix) {
		return IMAGELOGBASETABLE + "_p" + suffix;
	}

	private void createImageLogTable(int suffix) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = learnweb.getConnection().prepareStatement(
					"CREATE TABLE IF NOT EXISTS "
							+ getImageLogTableName(suffix) + " LIKE "
							+ IMAGELOGBASETABLE);
			stmt.execute();
		} catch (SQLException e) {
			throw e;
		} finally {
			close(null, stmt);
		}
	}

	private int getNextILogSuffix() throws SQLException {
		int number = 0;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = learnweb.getConnection().prepareStatement(
					"INSERT INTO " + IMAGELOGSUFFIXTABLE
							+ " VALUES(NULL, NULL)",
					Statement.RETURN_GENERATED_KEYS);
			pStmt.executeUpdate();

			rs = pStmt.getGeneratedKeys();
			if (!rs.next())
				throw new SQLException(
						"Problem while retrieving new ILog table suffix.");
			number = rs.getInt(1);
			return number;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	private int getCurrentILogSuffix() throws SQLException {
		int number = 0;
		ResultSet rs = null;
		PreparedStatement pStmt = null;
		try {

			pStmt = learnweb.getConnection().prepareStatement(
					"SELECT MAX(id) from " + IMAGELOGSUFFIXTABLE);
			rs = pStmt.executeQuery();

			if (!rs.next())
				throw new SQLException(
						"Problem while retrieving current ILog table suffix.");
			number = rs.getInt(1);
			return number;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	// usual logic

	public void initUserLogStmt() throws SQLException {
		if (null != userLogStmt && !userLogStmt.isClosed()) {
			userLogStmt.close();
		}
		userLogStmt = learnweb
				.getConnection()
				.prepareStatement(
						"INSERT DELAYED INTO `"
								+ USERLOGTABLE
								+ "` "
								+ "(`user_id`, `action`, `session_id`, `request_ip`, `user-agent`, `execution_time`) VALUES (?, ?, ?, ?, ?, ?)");

	}

	private void initImageLogStmt() throws SQLException {
		if (null != imageLogStmt && !imageLogStmt.isClosed()) {
			imageLogStmt.close();
		}
		imageLogStmt = learnweb
				.getConnection()
				.prepareStatement(
						"INSERT DELAYED INTO `"
								+ IMAGELOGTABLE
								+ "` "
								+ "(`user_id`, `instance`, `entity_id`, `ref_img`, `cor_img`, `picked_img`, `honey_pot`, `bucket_number`, `progress_index`, `session_id`, `request_ip`, `bonus_claimed`) "
								+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

	}

	public void logImageInput(int userId, String instance, int entityId,
			int refImg, int corImg, int pickedImg, boolean isHoneyPot,
			int bucketNumber, int progressIndex, String sessionId,
			String requestIp, boolean bonusClaimed) throws SQLException {

		synchronized (imageLogStmt) {
			try {
				learnweb.checkConnection();

				imageLogStmt.setInt(1, userId);
				imageLogStmt
						.setString(
								2,
								instance.substring(0,
										Math.min(instance.length(), 100)));
				imageLogStmt.setInt(3, entityId);
				imageLogStmt.setInt(4, refImg);
				imageLogStmt.setInt(5, corImg);
				imageLogStmt.setInt(6, pickedImg);
				imageLogStmt.setBoolean(7, isHoneyPot);
				imageLogStmt.setInt(8, bucketNumber);
				imageLogStmt.setInt(9, progressIndex);
				imageLogStmt.setString(10, sessionId.substring(0,
						Math.min(sessionId.length(), 32)));
				imageLogStmt.setString(11, requestIp.substring(0,
						Math.min(requestIp.length(), 45)));
				imageLogStmt.setBoolean(12, bonusClaimed);
				imageLogStmt.addBatch();

				imageLogBatchSize++;

				if (imageLogBatchSize > 0) {
					int[] cnts = imageLogStmt.executeBatch();
					for (int cnt : cnts) {
						imageLogRowNum += cnt;
					}
					imageLogBatchSize = 0;
				}
				if (hasILogEnoughRows()) {
					initNextImageLogTable();
					initImageLogStmt();
				}
			} catch (BatchUpdateException e) {
				logger.error(e);
			} catch (SQLException e) {
				initImageLogStmt();
				logger.error(e);
			}
		}
	}

	public void logUserInput(User user, String action, String sessionId,
			String requestIp, String userAgent, int executionTime)
			throws SQLException {

		synchronized (userLogStmt) {
			try {
				learnweb.checkConnection();

				if (null != user) {
					userLogStmt.setInt(1, user.getId());
				} else {
					userLogStmt.setNull(1, java.sql.Types.INTEGER);
				}
				userLogStmt.setString(2, action);
				userLogStmt.setString(3, sessionId);
				userLogStmt.setString(4, requestIp);
				userLogStmt.setString(
						5,
						userAgent.substring(0,
								Math.min(userAgent.length(), 150)));
				userLogStmt.setInt(6, executionTime);

				userLogStmt.addBatch();

				userLogBatchSize++;

				if (userLogBatchSize > 0) {
					int[] cnts = userLogStmt.executeBatch();
					for (int cnt : cnts) {
						userLogRowNum += cnt;
					}
					userLogBatchSize = 0;
				}
				if (hasULogEnoughRows()) {
					initNextUserLogTable();
					initUserLogStmt();
				}
			} catch (SQLException e) {
				initUserLogStmt();
				e.printStackTrace();
			}
		}
	}

	@PreDestroy
	public void onDestroy() {
		try {
			if (userLogBatchSize > 0) {
				userLogStmt.executeBatch();
				userLogBatchSize = 0;
				userLogStmt.close();
			}
			if (imageLogBatchSize > 0) {
				imageLogStmt.executeBatch();
				imageLogBatchSize = 0;
				imageLogStmt.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		try {
			initStmts();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
