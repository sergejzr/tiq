package de.l3s.mt.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.Group;
import de.l3s.mt.model.GroupMode;
import de.l3s.mt.model.GroupScoreEntry;
import de.l3s.mt.model.LotteryTicket;
import de.l3s.mt.model.ScoreEntry;
import de.l3s.mt.model.UserGroup;
import de.l3s.mt.model.UserGroupRole;

@ManagedBean
@RequestScoped
public class LeaderboardBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = 7172233208683129499L;

	// single user fields
	private boolean userInTopK;
	private List<ScoreEntry> scores;
	private ScoreEntry userEntry;
	// group fields
	private boolean groupInTopK;
	private List<GroupScoreEntry> groupScores;
	private GroupScoreEntry groupEntry;

	// config
	// number of entries to display
	private static boolean initialized = false;
	private static int k;
	private static boolean showAll;
	private static int numSurrounding;
	private static int policy = 0;
	private static boolean showRank;
	private static boolean isLottery;
	private static int ticketThreshold;
	private static boolean showSingleInformation;
	// group parameters
	private static boolean useGroups;
	private static int groupsNumSurrounding;
	private static int groupJoinProgress;
	private static GroupMode mode;
	// lottery information
	private int numTicketsAll;
	private int pointsToNextTicket;
	private List<LotteryTicket> userTickets;
	private boolean winnersDrawn;

	@ManagedProperty(value = "#{groupBean}")
	private GroupBean groupBean;

	private static Logger logger = Logger.getLogger(LeaderboardBean.class);

	public LeaderboardBean() throws SQLException {
		// TODO: add parameter for whether single user leaderboard should be
		// shown!
		initParameters();
	}

	@PostConstruct
	public void init() {
		try {
			if (LeaderboardBean.policy == 1 && !LeaderboardBean.isLottery) {
				initUserOpenPolicyNormal();
			}
			if (LeaderboardBean.policy == 2 && !LeaderboardBean.isLottery) {
				initUserMediumPolicyNormal();
			}
			if (LeaderboardBean.policy == 3 && !LeaderboardBean.isLottery) {
				initUserRestrictedPolicyNormal();
			}
			if (LeaderboardBean.isLottery) {
				initUserLottery();
			}
			if (LeaderboardBean.useGroups
					&& LeaderboardBean.mode.equals(GroupMode.RANDOM)) {
				initGroupsMedium();
			}
			if (LeaderboardBean.useGroups
					&& LeaderboardBean.mode.equals(GroupMode.LEADER)) {
				initGroupsLeader();
			}
		} catch (SQLException e) {
			logger.error("error initalizing leaderboard(s)", e);
			throw new IllegalStateException(e);
		}
	}

	private void initParameters() {
		logger.debug("initialize parameters? " + !initialized);
		if (!initialized) {
			String policy = getLearnweb().getProperties().getProperty(
					"INFO_POLICY");
			logger.info("INIT: Information policy will be set to " + policy);
			if (policy.equals("open")) {
				LeaderboardBean.policy = 1;
			}
			if (policy.equals("medium")) {
				LeaderboardBean.policy = 2;
			}
			if (policy.equals("restricted")) {
				LeaderboardBean.policy = 3;
			}
			if (LeaderboardBean.policy < 1 || LeaderboardBean.policy > 3) {
				LeaderboardBean.policy = 1;
				logger.error("error setting information policy!");
			}
			try {
				k = Integer.parseInt(getLearnweb().getProperties()
						.getProperty("NUM_DISPLAYED").trim());
			} catch (NumberFormatException e) {
				logger.error("error parsing parameter NUM_DISPLAYED", e);
				k = 10;
			}

			logger.info("INIT: k = " + LeaderboardBean.k);
			try {
				showAll = Boolean.parseBoolean(getLearnweb().getProperties()
						.getProperty("SHOW_ALL_SCORES").trim());
			} catch (Exception e) {
				logger.error("error parsing parameter SHOW_ALL_SCORES", e);
				showAll = false;
			}
			logger.info("INIT: showAll = " + showAll);
			try {
				numSurrounding = Integer.parseInt(getLearnweb().getProperties()
						.getProperty("NUM_SURROUNDING").trim());
			} catch (NumberFormatException e) {
				logger.error("error parsing parameter NUM_SURROUNDING");
			}
			logger.info("INIT: numSurrounding = " + numSurrounding);

			try {
				LeaderboardBean.showRank = Boolean.parseBoolean(getLearnweb()
						.getProperties().getProperty("SHOW_RANK").trim());
			} catch (Exception e) {
				logger.error("error parsing parameter SHOW_RANK", e);
				LeaderboardBean.showRank = true;
			}
			logger.info("INIT: showRank = " + LeaderboardBean.showRank);

			try {
				LeaderboardBean.isLottery = Boolean.parseBoolean(getLearnweb()
						.getProperties().getProperty("LOTTERY"));
			} catch (Exception e) {
				logger.error("error parsing parameter LOTTERY", e);
				LeaderboardBean.isLottery = false;
			}
			logger.info("INIT: isLottery = " + LeaderboardBean.isLottery);

			try {
				LeaderboardBean.ticketThreshold = Integer
						.parseInt(getLearnweb().getProperties().getProperty(
								"TICKETTHRES"));
			} catch (NumberFormatException e) {
				logger.error("error parsing parameter TICKETTHRES", e);
				LeaderboardBean.ticketThreshold = 200;
			}
			logger.info("INIT: ticketThreshold = "
					+ LeaderboardBean.ticketThreshold);

			try {
				LeaderboardBean.useGroups = !getLearnweb().getProperties()
						.getProperty("group_mode").equals("NONE");
			} catch (NullPointerException e) {
				logger.error("error retrieving parameter group_mode");
			}
			logger.info("INIT: useGroups = " + LeaderboardBean.useGroups);

			try {
				groupsNumSurrounding = Integer.parseInt(getLearnweb()
						.getProperties().getProperty("GROUPS_NUM_SURROUNDING")
						.trim());
			} catch (NumberFormatException e) {
				logger.error("error parsing parameter GROUPS_NUM_SURROUNDING");
			}
			logger.info("INIT: groupsNumSurrounding = " + groupsNumSurrounding);
			try {
				LeaderboardBean.showSingleInformation = Boolean
						.parseBoolean(getLearnweb().getProperties()
								.getProperty("SHOW_SINGLE_INFORMATION").trim());
			} catch (Exception e) {
				logger.error("error parsing parameter SHOW_SINGLE_INFORMATION",
						e);
				LeaderboardBean.showSingleInformation = true;
			}
			logger.info("INIT: showSingleLeaderboard = "
					+ LeaderboardBean.showSingleInformation);
			try {
				LeaderboardBean.groupJoinProgress = Integer
						.parseInt(getLearnweb().getProperties().getProperty(
								"group_join_progress"));
			} catch (NumberFormatException e) {
				logger.error("error parsing parameter group_join_progress", e);
			}
			logger.info("INIT: groupJoinProgress = "
					+ LeaderboardBean.groupJoinProgress);

			try {
				LeaderboardBean.mode = GroupMode.valueOf(getLearnweb()
						.getProperties().getProperty("group_mode"));
			} catch (IllegalArgumentException e) {
				logger.error("error parsing parameter group_mode", e);
			}
			logger.info("INIT: mode = " + mode.toString());

			initialized = true;
		}
	}

	/**
	 * initialize userEntry for restricted policy
	 * 
	 * @throws SQLException
	 */
	private void initUserRestrictedPolicyNormal() throws SQLException {
		if (getLearnweb().isAfterEnd()) {
			// display the whole leaderboard when the game has ended
			LeaderboardBean.policy = 1;
			initUserOpenPolicyNormal();
			return;
		}
		userEntry = getLearnweb().getScoreManager().getScoreEntryByUser(
				getUser());
	}

	/**
	 * initialize scores list according to medium restricted policy.
	 * 
	 * @throws SQLException
	 */
	private void initUserMediumPolicyNormal() throws SQLException {
		logger.debug("Initialize leaderboard for medium information policy");
		if (getLearnweb().isAfterEnd()) {
			// display the whole leaderboard when the game has ended
			LeaderboardBean.policy = 1;
			initUserOpenPolicyNormal();
			return;
		}
		userEntry = getLearnweb().getScoreManager().getScoreEntryByUser(
				getUser());
		logger.debug("retrieving scores for entry " + userEntry);
		// default values
		int minRank = 1;
		int maxRank = Integer.MAX_VALUE;

		if (null != userEntry) {
			userInTopK = true; // user will be displayed
			// display 2*numSurrounding + 1 entries (unless there are less than
			// numSurrounding entries after userEntry
			minRank = Math.max(userEntry.getRank() - numSurrounding, 1);
			maxRank = userEntry.getRank() + numSurrounding;
		}
		scores = getLearnweb().getScoreManager().getScoresBetween(minRank,
				maxRank);
	}

	/**
	 * initialize scores list according to open policy
	 * 
	 * @throws SQLException
	 */
	private void initUserOpenPolicyNormal() throws SQLException {
		// change visibility after game has ended
		if (getLearnweb().isAfterEnd()) {
			// showAll = true;
		}
		userEntry = getLearnweb().getScoreManager().getScoreEntryByUser(
				getUser());
		userInTopK = false;

		if (showAll) {
			scores = getLearnweb().getScoreManager().getAllScores();
			userInTopK = true;
		} else {
			scores = getLearnweb().getScoreManager().getTopKEntries(k);
			if (null != userEntry) {
				userInTopK = userEntry.getRank() <= k;
			}
		}

		if (!userInTopK && null != userEntry) {
			scores.add(new ScoreEntry(new User()));
			scores.add(userEntry);
		}
	}

	private void initUserLottery() throws SQLException {
		// TODO: handle end of game

		// are the winners drawn?
		this.winnersDrawn = Boolean.parseBoolean(getLearnweb()
				.getApplicationProperties().getProperty("WINNERS_DRAWN"));

		if (LeaderboardBean.policy == 1) {
			this.numTicketsAll = getLearnweb().getTicketManager()
					.getNumberOfTicketsAll();
		}
		// user entry contains score and number of tickets
		this.userEntry = getLearnweb().getScoreManager().getScoreEntryByUser(
				getUser());
		// get users lottery tickets
		this.userTickets = getLearnweb().getTicketManager().getUserTickets(
				getUser().getId());
		// compute how many points left to next ticket
		if (null != this.userEntry) {
			this.pointsToNextTicket = Math.max(
					(this.userEntry.getNumTickets() + 1)
							* LeaderboardBean.ticketThreshold
							- this.userEntry.getScore(), 0);
		} else {
			this.pointsToNextTicket = LeaderboardBean.ticketThreshold;
		}
	}

	/**
	 * initialize scores list according to medium restricted policy.
	 * 
	 * @throws SQLException
	 */
	private void initGroupsMedium() throws SQLException {
		logger.debug("Initialize  group leaderboard (medium information policy)");
		// TODO: account for group formation phase -> check if leader etc..
		UserGroup userGroup = getGroupBean().getUserGroup();
		if (null == userGroup)
			return;
		groupEntry = getLearnweb().getGroupScoreManager().getScoreEntryById(
				userGroup.getGroupId());
		if (getLearnweb().isAfterEnd()) {
			groupInTopK = false;

			groupScores = getLearnweb().getGroupScoreManager()
					.getTopKEntries(k);

			if (null != groupEntry) {
				groupInTopK = groupEntry.getRank() <= k;
			}

			if (!groupInTopK && null != groupEntry) {
				if (groupEntry.getRank() > k + 1) {
					groupScores.add(new GroupScoreEntry(new Group()));
				}
				groupScores.add(groupEntry);
			}
			insertGroupShareEntry();
			return;
		}

		logger.debug("retrieving scores for entry " + groupEntry);
		// default values
		int minRank = 1;
		int maxRank = Integer.MAX_VALUE;

		if (null != groupEntry) {
			setGroupInTopK(true); // user will be displayed
			// display 2*numSurrounding + 1 entries (unless there are less than
			// numSurrounding entries before userEntry
			minRank = Math.max(groupEntry.getRank() - groupsNumSurrounding, 1);
			maxRank = groupEntry.getRank() + groupsNumSurrounding;
		}
		groupScores = getLearnweb().getGroupScoreManager().getScoresBetween(
				minRank, maxRank);
		insertGroupShareEntry();
	}

	/**
	 * initialize scores list according for group leader in group formation
	 * phase
	 * 
	 * @throws SQLException
	 */
	private void initGroupsLeader() throws SQLException {
		UserGroup userGroup = getGroupBean().getUserGroup();
		if (null == userGroup)
			return;

		if (userGroup.getRole().equals(UserGroupRole.MEMBER)
				|| !getLearnweb().isGroupFormationPhase()) {
			initGroupsMedium();
			return;
		}

		logger.debug("Initialize  group leaderboard for leader mode");

		groupEntry = getLearnweb().getGroupScoreManager().getScoreEntryById(
				userGroup.getGroupId());
		groupInTopK = false;
		groupScores = getLearnweb().getGroupScoreManager()
				.getAllScoresWithGroupInfo();

		insertGroupShareEntry();
		return;
	}

	private void insertGroupShareEntry() {
		int pos = 0;
		for (GroupScoreEntry entry : groupScores) {
			if (entry.getGroupId() == groupEntry.getGroupId()) {
				break;
			}
			pos += 1;
		}
		GroupScoreEntry shareEntry = new GroupScoreEntry(
				groupEntry.getGroupId());
		shareEntry.setRank(-1);
		groupScores.add(pos + 1, shareEntry);
	}

	public boolean isUserInTopK() {
		return userInTopK;
	}

	public void setUserInTopK(boolean userInTopK) {
		this.userInTopK = userInTopK;
	}

	public int getUserPosition() throws SQLException {
		return userEntry.getRank();
	}

	public List<ScoreEntry> getScores() {
		return scores;
	}

	public void setScores(List<ScoreEntry> scores) {
		this.scores = scores;
	}

	public ScoreEntry getUserEntry() {
		return userEntry;
	}

	public void setUserEntry(ScoreEntry entry) {
		userEntry = entry;
	}

	public boolean showTable() {
		return LeaderboardBean.policy != 3 && !getLearnweb().isBeforeStart()
				&& !LeaderboardBean.isLottery
				&& LeaderboardBean.showSingleInformation;
	}

	public boolean showRank() {
		return LeaderboardBean.showRank
				&& LeaderboardBean.showSingleInformation;
	}

	public boolean showRankInformation() {
		return LeaderboardBean.policy == 3 && !LeaderboardBean.isLottery
				&& !getLearnweb().isBeforeStart()
				&& LeaderboardBean.showSingleInformation;
	}

	/**
	 * show single user score when groups are used and single user leaderboard
	 * is not shown
	 */
	public boolean showSingleUserScore() {
		return !LeaderboardBean.showSingleInformation
				&& LeaderboardBean.useGroups && !getLearnweb().isBeforeStart();
	}

	public boolean showUserTickets1() {
		return LeaderboardBean.isLottery && !getLearnweb().isBeforeStart()
				&& (!this.winnersDrawn || !getLearnweb().isAfterEnd());
	}

	public boolean showUserTickets2() {
		return LeaderboardBean.isLottery && !getLearnweb().isBeforeStart()
				&& this.winnersDrawn && getLearnweb().isAfterEnd();
	}

	public boolean showLotteryInfo() {
		return LeaderboardBean.isLottery && !getLearnweb().isBeforeStart()
				&& LeaderboardBean.showSingleInformation;
	}

	public boolean showNumTicketsAll1() {
		return LeaderboardBean.isLottery && LeaderboardBean.policy == 1
				&& !getLearnweb().isBeforeStart() && !this.winnersDrawn
				&& LeaderboardBean.showSingleInformation;
	}

	public boolean showNumTicketsAll2() {
		return LeaderboardBean.isLottery && LeaderboardBean.policy == 1
				&& !getLearnweb().isBeforeStart() && this.winnersDrawn
				&& LeaderboardBean.showSingleInformation;
	}

	public boolean showPointsRequired() {
		return LeaderboardBean.isLottery && !getLearnweb().isAfterEnd()
				&& LeaderboardBean.showSingleInformation;
	}

	public boolean showGroupLeaderboard() {
		return LeaderboardBean.useGroups && !getLearnweb().isBeforeStart()
				&& (null != this.groupEntry) && !showGroupLeaderLeaderboard();
	}

	public boolean showGroupLeaderLeaderboard() {
		return LeaderboardBean.useGroups
				&& LeaderboardBean.mode.equals(GroupMode.LEADER)
				&& getLearnweb().isGroupFormationPhase()
				&& (null != this.groupEntry)
				&& (getGroupBean().getUserGroup() != null)
				&& getGroupBean().getUserGroup().getRole()
						.equals(UserGroupRole.LEADER);
	}

	public boolean showNoGroupYetMessageRandom() {
		return LeaderboardBean.useGroups
				&& LeaderboardBean.mode.equals(GroupMode.RANDOM)
				&& !getLearnweb().isBeforeStart() && (null == this.groupEntry)
				&& LeaderboardBean.groupJoinProgress > 1;
	}

	public boolean showNoGroupYetMessageLeader() {
		return LeaderboardBean.useGroups
				&& LeaderboardBean.mode.equals(GroupMode.LEADER)
				&& !getLearnweb().isBeforeStart() && (null == this.groupEntry)
				&& LeaderboardBean.groupJoinProgress > 1;
	}

	public boolean showGroupControls() {
		// TODO: show only when mode = LEADER ?
		return LeaderboardBean.useGroups && !getLearnweb().isBeforeStart()
				&& (null != this.groupEntry);
	}

	public int getNumTicketsAll() {
		return numTicketsAll;
	}

	public void setNumTicketsAll(int numTicketsAll) {
		this.numTicketsAll = numTicketsAll;
	}

	public int getPointsToNextTicket() {
		return pointsToNextTicket;
	}

	public void setPointsToNextTicket(int pointsToNextTicket) {
		this.pointsToNextTicket = pointsToNextTicket;
	}

	public List<LotteryTicket> getUserTickets() {
		return userTickets;
	}

	public void setUserTickets(List<LotteryTicket> userTickets) {
		this.userTickets = userTickets;
	}

	public boolean isWinnersDrawn() {
		return this.winnersDrawn;
	}

	public GroupBean getGroupBean() {
		return groupBean;
	}

	public void setGroupBean(GroupBean groupBean) {
		this.groupBean = groupBean;
	}

	public List<GroupScoreEntry> getGroupScores() {
		return groupScores;
	}

	public void setGroupScores(List<GroupScoreEntry> groupScores) {
		this.groupScores = groupScores;
	}

	public GroupScoreEntry getGroupEntry() {
		return groupEntry;
	}

	public void setGroupEntry(GroupScoreEntry groupEntry) {
		this.groupEntry = groupEntry;
	}

	public boolean isGroupInTopK() {
		return groupInTopK;
	}

	public void setGroupInTopK(boolean groupInTopK) {
		this.groupInTopK = groupInTopK;
	}

	public String getUserShareString() {
		if (!LeaderboardBean.useGroups)
			return "";

		if (this.groupEntry.getScore() == 0)
			return "100";
		logger.debug("user:"
				+ this.userEntry.getScore()
				+ " group:"
				+ this.groupEntry.getScore()
				+ "double share: "
				+ (100 * (double) this.userEntry.getScore() / (double) this.groupEntry
						.getScore()));
		BigDecimal share = new BigDecimal(100
				* (double) this.userEntry.getScore()
				/ (double) this.groupEntry.getScore());
		return share.setScale(1, RoundingMode.HALF_UP).toPlainString();
	}

	public int getGroupJoinProgress() {
		return LeaderboardBean.groupJoinProgress;
	}

	public void setGroupJoinProgress(int groupJoinProgress) {
		LeaderboardBean.groupJoinProgress = groupJoinProgress;
	}
}
