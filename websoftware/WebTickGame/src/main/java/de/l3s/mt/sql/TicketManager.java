package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;

//import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.LotteryTicket;

public class TicketManager extends SQLManager {
	private final static String COLUMNS = " lt.`ticket_id` AS 'lt.ticket_id', lt.`user_id` AS 'lt.user_id', "
			+ " lt.`ticket_code` AS 'lt.ticket_code', lt.`prize_cents` AS 'lt.prize_cents', lt.`anon_name` AS 'lt.anon_name' ";
	private String TICKETTABLE;

	private Learnweb learnweb;

	private static final Logger logger = Logger.getLogger(TicketManager.class);

	public TicketManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		TICKETTABLE = learnweb.getTablePrefix() + "lottery_ticket";
	}

	public LotteryTicket save(LotteryTicket ticket) throws SQLException {
		if (ticket.getId() < 1)
			return saveNew(ticket);

		PreparedStatement pStmt = null;
		try {
			pStmt = learnweb
					.getConnection()
					.prepareStatement(
							"UPDATE `"
									+ TICKETTABLE
									+ "` SET "
									+ " `user_id` = ?, `ticket_code` = ?, `prize_cents` = ?, `anon_name` = ? "
									+ " WHERE `ticket_id` = ?");
			pStmt.setInt(1, ticket.getUserId());
			pStmt.setString(2, ticket.getTicketCode());
			pStmt.setInt(3, ticket.getPrizeCents());
			pStmt.setString(4, ticket.getAnonName());
			pStmt.setInt(5, ticket.getId());
			logger.debug("save(LotteryTicket) executes: " + pStmt.toString());
			pStmt.execute();
		} finally {
			close(null, pStmt);
		}
		return ticket;
	}

	public LotteryTicket saveNew(LotteryTicket ticket) throws SQLException {
		/*
		if (ticket.getId() > 0)
			return save(ticket);

		boolean success = false;
		int tries = 0;
		while (!success && ++tries <= 20) {
			try {
				ticket.setTicketCode(generateTicketCode());
				trySaveNewTicket(ticket);
				success = true;
			} catch (MySQLIntegrityConstraintViolationException e) {
				logger.error("failed saving ticket (attempt " + tries + ") "
						+ ticket.toString(), e);
			}
		}*/
		return ticket;
	}

	private String generateTicketCode() throws SQLException {
		String code = "";
		do {
			code = RandomStringUtils.randomNumeric(8);
		} while (isTicketCodeTaken(code));
		return code;
	}

	private boolean isTicketCodeTaken(String code) throws SQLException {
		ResultSet rs = null;
		PreparedStatement select = null;
		try {

			select = learnweb.getConnection().prepareStatement(
					"SELECT 1 FROM `" + TICKETTABLE
							+ "` lt WHERE `ticket_code` = ?");
			select.setString(1, code);
			rs = select.executeQuery();
			boolean result = rs.next();
			return result;
		} finally {
			close(rs, select);
		}
	}

	private LotteryTicket trySaveNewTicket(LotteryTicket ticket)
			throws SQLException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;

		try {
			pStmt = learnweb
					.getConnection()
					.prepareStatement(
							" INSERT INTO `"
									+ TICKETTABLE
									+ "` (`user_id`, `ticket_code`, `prize_cents`, `anon_name`) "
									+ " VALUES (?,?,?,?) ",
							Statement.RETURN_GENERATED_KEYS);
			pStmt.setInt(1, ticket.getUserId());
			pStmt.setString(2, ticket.getTicketCode());
			pStmt.setInt(3, ticket.getPrizeCents());
			pStmt.setString(4, ticket.getAnonName());
			logger.debug("saveNew(LotteryTicket) executes: " + pStmt.toString());
			pStmt.execute();

			rs = pStmt.getGeneratedKeys();
			if (null != rs && rs.next())
				ticket.setId(rs.getInt(1));
		} finally {
			close(rs, pStmt);
		}
		return ticket;
	}

	public List<LotteryTicket> getUserTickets(int userId) throws SQLException {
		List<LotteryTicket> tickets = new ArrayList<LotteryTicket>();
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = learnweb.getConnection().prepareStatement(
					" SELECT " + COLUMNS + " FROM `" + TICKETTABLE
							+ "` lt WHERE lt.`user_id` = ? ");
			pStmt.setInt(1, userId);
			logger.debug("getUserTickets(int) executes: " + pStmt.toString());
			rs = pStmt.executeQuery();
			while (rs.next()) {
				try {
					tickets.add(new LotteryTicket(rs));
				} catch (SQLException e) {
					logger.error("Could not instantiate LotteryTicket", e);
				}
			}
		} finally {
			close(rs, pStmt);
		}

		return tickets;
	}

	/** returns number of lottery tickets in the game */
	public int getNumberOfTicketsAll() throws SQLException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			pStmt = learnweb.getConnection().prepareStatement(
					"SELECT count(*) from `" + TICKETTABLE + "`");
			logger.debug("getNumberOfTicketsAll() executes: "
					+ pStmt.toString());
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new SQLException(
						"Error while retrieving number of lottery tickets: No result was retrieved!");

			result = rs.getInt(1);
		} finally {
			close(rs, pStmt);
		}

		return result;
	}

	public List<LotteryTicket> getAllTickets() throws SQLException {
		ArrayList<LotteryTicket> tickets = new ArrayList<LotteryTicket>();
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = learnweb.getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + TICKETTABLE
							+ "` lt order by ticket_id asc");
			logger.debug("getAllTickets() executes: " + pStmt.toString());
			rs = pStmt.executeQuery();
			while (rs.next()) {
				try {
					tickets.add(new LotteryTicket(rs));
				} catch (Exception e) {
					logger.error("Error while instantiating lottery ticket.");
				}
			}
		} finally {
			close(rs, pStmt);
		}

		return tickets;
	}
}
