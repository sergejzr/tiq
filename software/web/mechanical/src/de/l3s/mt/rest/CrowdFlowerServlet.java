package de.l3s.mt.rest;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

@Path("/crowdflower")
public class CrowdFlowerServlet implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger
			.getLogger(CrowdFlowerServlet.class);

	private Learnweb app;

	@Context
	HttpServletRequest request;

	public CrowdFlowerServlet() {
		this.app = Learnweb.getInstance();
	}

	@POST
	@Path("/processhook")
	public void processHook(String jsonString)
			throws UnsupportedEncodingException {
		logger.debug("weebhook call received.");
		/* receives:
		 * received: signal=unit_complete&payload=%7B%22id%22%3A556879929%2C%22data%22%3A%7B%22slot%22%3A%221%22%7D%2C%22difficulty%22%3A0%2C%22judgments_count%22%3A0%2C%22state%22%3A%22judgable%22%2C%22agreement%22%3Anull%2C%22missed_count%22%3A0%2C%22gold_pool%22%3Anull%2C%22created_at%22%3A%222014-09-24T14%3A06%3A09%2B00%3A00%22%2C%22updated_at%22%3A%222014-09-24T14%3A06%3A09%2B00%3A00%22%2C%22job_id%22%3A618780%2C%22results%22%3A%7B%22judgments%22%3A%5B%5D%7D%7D&signature=93263df69cbd70d1793623f5047e2ff6d83c05df
		 */
		String[] json = jsonString.split("&");
		String signal = json[0].replaceFirst("signal=", "");
		logger.info("signal: '" + signal + "' received");
		// only interested in unit_complete for now. ignore others
		if (!signal.equals("unit_complete"))
			return;
		String payload = java.net.URLDecoder.decode(
				json[1].replaceFirst("payload=", ""), "UTF-8");
		logger.debug("payload:" + payload);
		// check signature
		// "signature = sha1_encrypt(payload + api_key)"
		String sig = DigestUtils.shaHex(payload + app.getCFApiKey());
		String signature = json[2].replaceFirst("signature=", "");
		logger.debug("signature:" + signature);
		if (!signature.equals(sig)) {
			logger.error("signature does not match (should be " + sig
					+ "). JSON: \""
					+ java.net.URLDecoder.decode(jsonString, "UTF-8") + "\"");
			return;
		}
		// process payload
		try {
			JSONObject pl = new JSONObject(payload);
			JSONObject results = pl.getJSONObject("results");
			JSONArray judgments = results.getJSONArray("judgments");
			// check number of judgments (should be 1)
			if (judgments.length() < 1) {
				logger.error("no judgments received:\""
						+ java.net.URLDecoder.decode(jsonString, "UTF-8")
						+ "\"");
				return;
			}
			for (int i = 0; i < judgments.length(); i++) {
				JSONObject judgment = judgments.getJSONObject(i);
				int workerId = judgment.getInt("worker_id");
				String token = judgment.getJSONObject("data").getString(
						"user_code");
				String channel = judgment.getString("external_type");
				logger.debug("judgment information: workerId: " + workerId
						+ ", token: " + token + ", channel: " + channel);
				// (re-)activate user account
				User user = app.getUserManager().getUserByToken(token);
				if (user == null) {
					logger.error("user was null for token " + token);
					continue;
				}
				user.setActive(true);
				user.setWorkerId(workerId);
				user.setChannel(channel);
				app.getUserManager().save(user);
				try {
					app.getLogManager().logUserInput(user, "hook-activate",
							"-", "-", "-", 0);
				} catch (SQLException e) {
					logger.error(
							"error loging activation for user " + user.getId(),
							e);
				}
			}
		} catch (JSONException e) {
			logger.error("processHook: could not process JSON: \""
					+ java.net.URLDecoder.decode(jsonString, "UTF-8") + "\"", e);
		} catch (SQLException e) {
			logger.error(
					"processHook: db error while retrieving/storing user. JSON:\""
							+ java.net.URLDecoder.decode(jsonString, "UTF-8")
							+ "\"", e);
		} catch (Exception e) {
			logger.error("processHook: user null. JSON:\""
					+ java.net.URLDecoder.decode(jsonString, "UTF-8") + "\"", e);
		}

	}
}
