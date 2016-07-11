package de.l3s.mt.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

public class CrowdFlowerResultsReader {
	// read judgments from file
	public static void main(String[] args) {
		Learnweb app = Learnweb.getInstance();
		File resultFile = null;
		if (args.length == 0) {
			resultFile = new File(
					"/media/data/uni/papers/mt-groups/testing/f618780.csv");
		} else {
			resultFile = new File("f" + args[0] + ".csv");
		}
		// do not check for each submitted token, if it exists in database and
		// is already active
		boolean activateOnly = true;
		if (args.length > 1) {
			activateOnly = Boolean.parseBoolean(args[1]);
		}
		String activatedUsers = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					resultFile));
			String line = reader.readLine();
			System.out.println("Read line: " + line);
			String[] fields = line.split(",");
			int workerIndex = getIndex(fields, "_worker_id");
			int tokenIndex = getIndex(fields, "user_code");
			int channelIndex = getIndex(fields, "_channel");
			System.out.println("column indexes: _worker_id: " + workerIndex
					+ ", user_code: " + tokenIndex + ", _channel:"
					+ channelIndex);
			Map<String, User> users = null;
			if (activateOnly) {
				try {
					users = app.getUserManager().getInactiveUsers();
				} catch (SQLException e) {
					System.out
							.println("ERROR: could not retrieve inactive users.");
					e.printStackTrace();
				}
			}
			for (line = reader.readLine(); line != null; line = reader
					.readLine()) {
				System.out.println("Read line: " + line);
				fields = line.split(",");
				String channel = fields[channelIndex];
				int workerId = Integer.parseInt(fields[workerIndex]);
				String token = fields[tokenIndex];
				// System.out.println("judgment information: workerId: "
				// + workerId + ", token: " + token + ", channel: "
				// + channel);
				User user = null;
				if (!activateOnly) {
					user = app.getUserManager().getUserByToken(token);
					if (user == null) {
						System.out.println("(N) user was null for token "
								+ token + " worker: " + workerId);
						continue;
					}
					if (user.isActive()) {
						System.out.println("(-) user " + user.getId()
								+ " already active.");
						continue;
					}
				} else {
					user = users.get(token);
					if (user == null)
						continue;
				}

				user.setActive(true);
				user.setWorkerId(workerId);
				user.setChannel(channel);
				app.getUserManager().save(user);
				try {
					app.getLogManager().logUserInput(user,
							"resultreader-activate", "-", "-", "-", 0);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				System.out.println("(+) user id='" + user.getId()
						+ "' has been activated");
				activatedUsers += ((activatedUsers.length() > 0) ? "," : "")
						+ user.getId();
			}
			System.out.println("Activated users: " + activatedUsers);
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int getIndex(String[] fields, String header) {
		int index = 0;
		boolean found = false;
		for (; index < fields.length; index++) {
			if (fields[index].equals(header)) {
				found = true;
				break;
			}
		}
		if (found)
			return index;
		System.out.println("ERROR: could not find index for header '" + header
				+ "'");
		return -1;
	}
}
