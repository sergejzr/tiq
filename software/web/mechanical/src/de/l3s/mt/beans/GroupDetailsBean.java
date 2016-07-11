package de.l3s.mt.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.Group;
import de.l3s.mt.model.GroupMessage;
import de.l3s.mt.model.GroupMode;
import de.l3s.mt.model.GroupScoreEntry;
import de.l3s.mt.model.InvitationEvent;
import de.l3s.mt.model.InvitationMessage;
import de.l3s.mt.model.ScoreEntry;
import de.l3s.mt.model.UserGroup;
import de.l3s.mt.model.UserGroupRole;

@RequestScoped
@ManagedBean
public class GroupDetailsBean extends ApplicationBean implements Serializable {
	private static final long serialVersionUID = -8222650784991554794L;
	private Group group;
	private List<ScoreEntry> scores;
	private GroupScoreEntry groupScoreEntry;
	private UserGroup userGroup;
	private List<GroupMessage> groupMessages;
	private GroupMessage message;
	private int oldest;
	private int newest;
	private List<InvitationEvent> invitationEvents;
	private int newestEvent = Integer.MAX_VALUE; // TODO: track
	private List<InvitationMessage> invitationMessages;
	private int newestInvitationMessage = Integer.MAX_VALUE; // TODO: track
	private GroupMode mode;

	private Logger logger = Logger.getLogger(GroupDetailsBean.class);

	public GroupDetailsBean() throws SQLException {

	}

	/** wait until groupBean is available */
	@PostConstruct
	public void init() {
		try {
			GroupBean groupBean = UtilBean.getGroupBean();
			this.mode = groupBean.getMode();
			this.group = getLearnweb().getGroupManager().getGroup(
					groupBean.getUserGroup().getGroupId());
			this.groupScoreEntry = getLearnweb().getGroupScoreManager()
					.getScoreEntryById(this.group.getId());
			this.scores = getLearnweb().getScoreManager().getGroupUserEntries(
					this.group.getId());
			this.userGroup = groupBean.getUserGroup();
			this.oldest = userGroup.getOldestGroupMsg();
			this.newest = userGroup.getNewestGroupMsg();
			this.newestEvent = userGroup.getNewestInvEvent();
			this.newestInvitationMessage = userGroup.getNewestInvMsg();

			// init messages for group chat
			initMessages();

			if (this.mode.equals(GroupMode.LEADER)) {
				initLeaderMode();
			}
		} catch (SQLException e) {
			logger.error("init()", e);
		}
	}

	private void initLeaderMode() throws SQLException {
		if (null == this.group) {
			logger.error("initLeaderMode: group was null!");
			return;
		}
		if (this.userGroup.getRole().equals(UserGroupRole.LEADER)) {
			this.invitationEvents = getLearnweb().getInvitationManager()
					.getInvitationEvents(this.group.getId(), false);
			this.invitationMessages = getLearnweb().getInvitationManager()
					.getInvitationMessages(this.group.getId(), 0);
			if (!this.invitationEvents.isEmpty()) {
				this.userGroup.setNewestInvEvent(this.invitationEvents.get(0)
						.getId());
			}
		} else {
			this.invitationMessages = getLearnweb().getInvitationManager()
					.getMergeMessages(this.group.getId(), 0);
		}

		if (!this.invitationMessages.isEmpty()) {
			userGroup.setNewestInvMsg(this.invitationMessages.get(0)
					.getMessageId());
		}
		// store information about which invitation messages and events have
		// been seen by user
		if (this.newestInvitationMessage != this.userGroup.getNewestInvMsg()
				|| this.newestEvent != this.userGroup.getNewestInvEvent()) {
			getLearnweb().getGroupManager().updateMsgStatistics(userGroup);
		}
	}

	private void initMessages() throws SQLException {
		if (null == this.group) {
			logger.error("initMessages: group was null!");
			return;
		}
		if (null == this.userGroup) {
			logger.error("initMessages: userGroup was null!");
			return;
		}
		// retrieve messages
		this.groupMessages = getLearnweb().getGroupMessageManager()
				.getGroupMessages(this.group.getId(),
						this.userGroup.getOldestGroupMsg());
		this.message = new GroupMessage();
		this.message.setGroupId(this.group.getId());
		this.message.setUserId(getUser().getId());
		this.message.setUsername(getUser().getUsername());
		this.message.setMessage("");

		// store information about which messages have been seen by user
		if (!this.groupMessages.isEmpty()) {
			userGroup.setOldestGroupMsg(this.groupMessages.get(
					this.groupMessages.size() - 1).getMessageId());
			userGroup.setNewestGroupMsg(this.groupMessages.get(0)
					.getMessageId());
			if (this.oldest != this.userGroup.getOldestGroupMsg()
					|| this.newest != this.userGroup.getNewestGroupMsg()) {
				getLearnweb().getGroupManager().updateMsgStatistics(userGroup);
			}
		}
	}

	public String submitMessage() {
		String path = getTemplateDir() + "/group.jsf?faces-redirect=true";
		if (this.message.getMessage().isEmpty())
			return path;

		try {
			getLearnweb().getGroupMessageManager().insert(message);
			// this.userGroup.setNewestGroupMsg(this.message.getMessageId());
			this.userGroup.setNewestOwnMsg(this.message.getMessageId());
			getLearnweb().getGroupManager().updateMsgStatistics(this.userGroup);
		} catch (SQLException e) {
			logger.error("submitMessage: error inserting message", e);
		}
		return path;
	}

	public String getShareString(String userScoreString) {
		int userScore = 0;
		try {
			userScore = Integer.parseInt(userScoreString);
		} catch (NumberFormatException e) {
			logger.error("getShareString", e);
		}
		if (this.groupScoreEntry.getScore() == 0)
			return "100";
		BigDecimal share = new BigDecimal(100 * (double) userScore
				/ (double) this.groupScoreEntry.getScore());
		return share.setScale(1, RoundingMode.HALF_UP).toPlainString();
	}

	public String goToAnnotation() {
		logger.debug("go to annotation");
		log("go_to_annotation");
		return getTemplateDir() + "/mt.jsf";
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public List<ScoreEntry> getScores() {
		return scores;
	}

	public void setScores(List<ScoreEntry> scores) {
		this.scores = scores;
	}

	public GroupScoreEntry getGroupScoreEntry() {
		return groupScoreEntry;
	}

	public void setGroupScoreEntry(GroupScoreEntry groupScoreEntry) {
		this.groupScoreEntry = groupScoreEntry;
	}

	public List<GroupMessage> getGroupMessages() {
		return groupMessages;
	}

	public void setGroupMessages(List<GroupMessage> groupMessages) {
		this.groupMessages = groupMessages;
	}

	public GroupMessage getMessage() {
		return message;
	}

	public void setMessage(GroupMessage message) {
		this.message = message;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	public int getOldest() {
		return oldest;
	}

	public void setOldest(int oldest) {
		this.oldest = oldest;
	}

	public int getNewest() {
		return newest;
	}

	public void setNewest(int newest) {
		this.newest = newest;
	}

	public GroupMode getMode() {
		return mode;
	}

	public void setMode(GroupMode mode) {
		this.mode = mode;
	}

	public List<InvitationEvent> getInvitationEvents() {
		return invitationEvents;
	}

	public void setInvitationEvents(List<InvitationEvent> invitationEvents) {
		this.invitationEvents = invitationEvents;
	}

	public List<InvitationMessage> getInvitationMessages() {
		return invitationMessages;
	}

	public void setInvitationMessages(
			List<InvitationMessage> invitationMesssages) {
		this.invitationMessages = invitationMesssages;
	}

	public Boolean showInvitationEvents() {
		return this.mode.equals(GroupMode.LEADER)
				&& this.userGroup.getRole().equals(UserGroupRole.LEADER)
				&& getLearnweb().isGroupFormationPhase();
	}

	// show general history also in annotation phase
	public Boolean showInvitationMessages() {
		return this.mode.equals(GroupMode.LEADER);
	}

	public Boolean showAllInvitationMessages() {
		return showInvitationMessages()
				&& this.userGroup.getRole().equals(UserGroupRole.LEADER)
				&& getLearnweb().isGroupFormationPhase();
	}

	public int getNewestEvent() {
		return newestEvent;
	}

	public void setNewestEvent(int newestEvent) {
		this.newestEvent = newestEvent;
	}

	public int getNewestInvitationMessage() {
		return newestInvitationMessage;
	}

	public void setNewestInvitationMessage(int newestInvitationMessage) {
		this.newestInvitationMessage = newestInvitationMessage;
	}

}
