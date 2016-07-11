package de.l3s.mt.rest;

import java.io.Serializable;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.mt.model.ScoreEntry;

@Path("/test")
public class ScoresTestServlet implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger
			.getLogger(ScoresTestServlet.class);
	private static boolean disabled = true;
	private Learnweb app;

	@Context
	HttpServletRequest request;

	public ScoresTestServlet() {
		this.app = Learnweb.getInstance();
	}

	@GET
	@Path("/groupscores")
	public String testGroupScoreMechanism(@QueryParam("user") Integer userId,
			@QueryParam("increment") Integer increment) {
		logger.debug("test group score - user: " + userId + ", increment: "
				+ increment);
		if (disabled)
			return "";
		User user = null;
		if (null == userId || null == increment)
			return "ERROR: parameters user or increment missing";
		try {
			user = app.getUserManager().getUser(userId);
		} catch (SQLException e) {
			logger.error(e.getLocalizedMessage(), e);
			return e.getMessage();
		}
		ScoreEntry userScore = null;
		try {
			userScore = app.getScoreManager().getScoreEntryByUser(user);
		} catch (SQLException e) {
			logger.error(e.getLocalizedMessage(), e);
			return e.getMessage();
		}
		userScore.setScore(userScore.getScore() + increment);
		try {
			app.getScoreManager().save(userScore);
		} catch (SQLException e) {
			logger.error(e.getLocalizedMessage(), e);
			return e.getMessage();
		}

		int trial = 1;
		boolean success = false;
		int groupId;
		do {
			long startTime = System.currentTimeMillis();
			// get current groupId
			try {
				groupId = app.getGroupManager().getUserGroupEntry(user.getId())
						.getGroupId();
			} catch (SQLException e) {
				logger.error(
						"error getting groupentry for user " + user.getId(), e);
				continue;
			}
			try {
				success = app.getGroupScoreManager().incrementScore(groupId,
						increment);
			} catch (SQLException e) {
				logger.error("error incrementing group score for group "
						+ groupId + " by " + increment, e);
			}
			if (success)
				break;
			long endTime = System.currentTimeMillis();
			try {
				Thread.sleep(Math.max(50L - (endTime - startTime), 0));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (trial < 10);

		return "ok";
	}
}
