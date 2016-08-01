package de.l3s.mt.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

public class CrowdFlowerAPIActivate {
	// read judgments from file
	public static void main(String[] args) throws SQLException {
		int jobId;
		if (args.length == 0) {
			System.out.println("ERROR: provide job id!");
			return;
			// jobId = 618780;
		} else {
			jobId = Integer.parseInt(args[0]);
		}
		Learnweb app = Learnweb.getInstance();
		CFJobManager jobs = new CFJobManager(app);
		CFJob job = jobs.getCFJob(jobId);
		if (null == job) {
			job = new CFJob(jobId);
		}
		/* we have to read in all judgments, as they are ordered by unit.
		 * as it turns out, even w/ 1 judgment per unit, it may happen that a unit 
		 * can receive a second judgment even days later */
		// int offset = (job.getUnitJudgmentsExamined() / 100) * 100;
		int offset = 0;
		int examined = 0;
		do {
			examined = activate(jobId, offset, app);
			offset += examined;
		} while (examined > 0);
		job.setUnitJudgmentsExamined(offset);
		jobs.save(job);
	}

	public static int activate(int jobId, int offset, Learnweb app) {
		URL url;
		String line;
		final StringBuilder builder = new StringBuilder(2048);
		int count = 0;
		try {
			url = new URL("https://api.crowdflower.com/v1/jobs/" + jobId
					+ "/judgments.json?key=" + app.getCFApiKey() + "&offset="
					+ offset);

			URLConnection urlc = url.openConnection();

			BufferedReader bfr = new BufferedReader(new InputStreamReader(
					urlc.getInputStream()));

			while ((line = bfr.readLine()) != null) {
				builder.append(line);
			}

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return 0;
		} catch (IOException e2) {
			e2.printStackTrace();
			return 0;
		}
		String activatedUsers = "";
		Map<String, User> users = null;
		try {
			users = app.getUserManager().getInactiveUsers();
		} catch (SQLException e) {
			System.out.println("ERROR: could not retrieve inactive users.");
			e.printStackTrace();
		}
		// convert response to JSON array
		try {
			// actually a JSONObject with an entry per j
			JSONObject units = new JSONObject(builder.toString());
			System.out
					.println("retrieved " + units.length()
							+ " unit judgments for job " + jobId + ", offset "
							+ offset);
			Iterator<?> keys = units.keys();

			User user = null;
			while (keys.hasNext()) {
				String key = (String) keys.next();
				JSONObject unitJudgments = (JSONObject) units.get(key);

				JSONArray tokens = unitJudgments.getJSONArray("user_code");
				for (int i = 0; i < tokens.length(); i++) {
					String token = tokens.getString(i);
					// if (i > 0)
					// System.out.println("got token " + token);
					user = users.get(token);
					if (user == null) {
						// System.out.println("user w/ token " + token
						// + " already active?");
						continue;
					}
					user.setActive(true);
					try {
						app.getUserManager().save(user);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					try {
						app.getLogManager().logUserInput(user, "api-activate",
								"-", "-", "-", 0);
					} catch (SQLException e) {
						e.printStackTrace();
					}

					System.out.println("(+) user id='" + user.getId()
							+ "' has been activated");
					activatedUsers += ((activatedUsers.length() > 0) ? "," : "")
							+ user.getId();
				}
				count += 1;
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		System.out.println("Activated users: " + activatedUsers);
		return count;
	}
}
