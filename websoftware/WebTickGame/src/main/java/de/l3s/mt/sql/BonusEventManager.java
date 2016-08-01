package de.l3s.mt.sql;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.BonusEvent;

public class BonusEventManager extends SQLManager {
	private static Logger logger = Logger.getLogger(BonusEventManager.class);
	private String BONUSTABLE;
	private String COLUMNS = " be.`event_id` AS 'be.event_id', be.`start` AS 'be.start', "
			+ " be.`end` AS 'be.end', be.`bonus_factor` AS 'be.bonus_factor', "
			+ " be.`bonus_pool_size` AS 'be.bonus_pool_size', be.`bonus_claimed` AS 'be.bonus_claimed' ";

	private Random random;
	// params
	private boolean bonusFixed;
	private int bonusFactor;
	private boolean bonusLimited;
	private int bonusLimit;
	private int numEvents;

	public BonusEventManager(Learnweb application) throws SQLException {
		super(application);
		initParameters();
		checkState();

	}

	private void initParameters() {
		BONUSTABLE = this.application.getTablePrefix() + "bonus_event";
		try {
			this.bonusFixed = Boolean.parseBoolean(application.getProperties()
					.getProperty("BONUS_FACTOR_FIXED"));
			logger.info("bonusFixed=" + this.bonusFixed);
		} catch (Exception e) {
			logger.error("Error parsing BONUS_FACTOR_FIXED", e);
		}

		try {
			this.bonusFactor = Integer.parseInt(application.getProperties()
					.getProperty("BONUS_FACTOR"));
			logger.info("bonusFactor=" + this.bonusFactor);
		} catch (NumberFormatException e) {
			logger.error("Error parsing BONUS_FACTOR", e);
		}

		try {
			this.bonusLimited = Boolean.parseBoolean(application
					.getProperties().getProperty("BONUS_LIMITED"));
			logger.info("bonusLimited=" + this.bonusLimited);
		} catch (Exception e) {
			logger.error("Error parsing BONUS_LIMITED", e);
		}

		try {
			this.bonusLimit = Integer.parseInt(application.getProperties()
					.getProperty("BONUS_LIMIT"));
			logger.info("bonusLimit=" + this.bonusLimit);
		} catch (NumberFormatException e) {
			logger.error("Error parsing BONUS_LIMIT", e);
		}

		try {
			this.numEvents = Integer.parseInt(application.getProperties()
					.getProperty("NUM_EVENTS"));
			logger.info("numEvents=" + this.numEvents);
		} catch (NumberFormatException e) {
			logger.error("Error parsing NUM_EVENTS", e);
		}
	}

	/**
	 * check if database state is consistent with parameters. if it is not, set
	 * up database contents.
	 * 
	 * @throws SQLException
	 */
	private void checkState() throws SQLException {
		logger.info("CHECKING BONUS EVENT STATE...");
		// check if table exists
		boolean ok = true;
		if (!existsTable(BONUSTABLE)) {
			ok = false;
			logger.error("Bonus table does not exist!");
		}
		List<BonusEvent> events = getEvents();
		if (events.size() < this.numEvents) {
			logger.info("Bonus Events are not set up.");
			initEvents(events);
			for (BonusEvent event : events) {
				save(event);
			}
		}
		// TODO: check if enough bonus events are defined
		if (ok) {
			logger.info("BonusEvent state OK");
		}
	}

	/**
	 * Initialize BonusEvents
	 */
	private void initEvents(List<BonusEvent> events) {
		logger.info("initialize bonus events");
		long start = application.getStart().getTimeInMillis();
		long end = application.getEnd().getTimeInMillis();
		int hours = (int) (end - start) / 3600000;
		logger.debug("hours:" + hours);

		// track hours that already have bonus event
		Set<Integer> blockedHours = new HashSet<Integer>();
		for (BonusEvent event : events) {
			int hour = (int) (event.getStart() - start) / 3600000;
			logger.debug("found existing event for hour " + hour);
			if (blockedHours.contains(hour)) {
				logger.error("Multiple events scheduled for hour " + hour
						+ " of the competition!");
				throw new IllegalStateException("Duplicate bonus events");
			}
			blockedHours.add(hour);
		}
		while (events.size() < this.numEvents) {
			// add an event
			// pick hour
			int hour;
			do {
				hour = getRandom().nextInt(hours);
			} while (blockedHours.contains(hour));
			// track hour
			blockedHours.add(hour);
			// set bonus factor
			int bonusFactor = this.bonusFactor;
			if (this.bonusFixed) {
				// TODO: implement if this setting is used for additional
				// experiment
			}
			logger.debug("new event hour: " + hour);
			events.add(new BonusEvent(bonusFactor, this.bonusLimit, start
					+ (long) hour * 3600000L));
		}

	}

	private Random getRandom() {
		if (this.random == null) {
			this.random = new SecureRandom();
		}
		return this.random;
	}

	private Learnweb getApplication() {
		return this.application;
	}

	/**
	 * Get all Bonus event ordered by start time (in asc order, i.e. earliest
	 * one is first)
	 */
	public List<BonusEvent> getEvents() throws SQLException {
		ResultSet rs = null;
		PreparedStatement pStmt = null;
		List<BonusEvent> result = new ArrayList<BonusEvent>();
		try {
			pStmt = getApplication().getConnection().prepareStatement(
					" SELECT " + COLUMNS + " FROM `" + BONUSTABLE
							+ "` be ORDER BY be.`start` ASC");
			logger.debug("getEvents() executes: " + pStmt.toString());
			rs = pStmt.executeQuery();
			while (rs.next()) {
				result.add(new BonusEvent(rs));
			}
			return result;
		} finally {
			close(rs, pStmt);
		}
	}

	public BonusEvent save(BonusEvent event) throws SQLException {
		ResultSet rs = null;
		PreparedStatement pStmt = null;
		try {
			pStmt = getApplication()
					.getConnection()
					.prepareStatement(
							" REPLACE INTO `"
									+ BONUSTABLE
									+ "` (event_id, start, end, bonus_factor, bonus_pool_size, bonus_claimed) "
									+ " VALUES (?, ?, ?, ?, ?, ?)",
							PreparedStatement.RETURN_GENERATED_KEYS);
			if (event.getId() == -1) {
				pStmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				pStmt.setInt(1, event.getId());
			}
			pStmt.setTimestamp(2, new Timestamp(event.getStart()));
			pStmt.setTimestamp(3, new Timestamp(event.getEnd()));
			pStmt.setInt(4, event.getBonusFactor());
			pStmt.setInt(5, event.getBonusPoolSize());
			pStmt.setInt(6, event.getBonusClaimed());
			logger.debug("save(BonusEvent) executes: " + pStmt.toString());
			pStmt.executeUpdate();

			if (event.getId() < 0) // get the assigned id
			{
				rs = pStmt.getGeneratedKeys();
				if (!rs.next())
					throw new SQLException("database error: no id generated");
				event.setId(rs.getInt(1));
			}
			return event;
		} finally {
			close(rs, pStmt);
		}
	}
}
