package de.l3s.mt.beans;

import java.io.Serializable;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.Bucket;
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
public class GroupBean extends ApplicationBean implements Serializable {
	private static final long serialVersionUID = 2L;
	private static final Logger logger = Logger.getLogger(GroupBean.class);

	private UserGroup userGroup;
	private GroupMode mode;
	private Random random;
	private int maxMsgId;
	private int maxInvMsgId;
	private int maxInvEventId;
	private boolean newMessages = false;
	private boolean groupMerged = false;
	private boolean newEvent = false;

	public GroupBean() {
		// shouldn't be constructed anyway if user is not logged in
		if (null == getUser())
			return;

		this.mode = GroupMode.valueOf(getLearnweb().getProperties()
				.getProperty("group_mode"));
		logger.debug("Contstruct: mode = " + mode.toString());

		if (this.mode.equals(GroupMode.NONE))
			return;
		try {
			this.userGroup = getLearnweb().getGroupManager().getUserGroupEntry(
					getUser().getId());

			if (null == this.userGroup) {
				if (this.mode.equals(GroupMode.RANDOM)) {
					getRandomGroup();
				}
				if (this.mode.equals(GroupMode.LEADER)) {
					getNewLeaderGroup();
				}
			}
			if (null != this.userGroup) {
				this.maxMsgId = getLearnweb().getGroupMessageManager()
						.getNewestMessageId(this.userGroup.getGroupId());
				if (this.userGroup.getRole().equals(UserGroupRole.MEMBER)) {
					this.maxInvMsgId = getLearnweb().getInvitationManager()
							.getNewestMergeMessageId(
									this.userGroup.getGroupId());
				} else {
					this.maxInvMsgId = getLearnweb().getInvitationManager()
							.getNewestMessageId(this.userGroup.getGroupId());
					this.maxInvEventId = getLearnweb().getInvitationManager()
							.getNewestEventId(this.userGroup.getGroupId());
					this.setNewEvent(this.maxInvEventId > Math.max(
							this.userGroup.getNewestInvEvent(),
							this.userGroup.getNewestOwnInvEvent()));
				}
				this.groupMerged = this.maxInvMsgId > Math.max(
						this.userGroup.getNewestInvMsg(),
						this.userGroup.getNewestOwnInvMsg());
			}
		} catch (SQLException e) {
			logger.error("failed to retrieve UserGroup entry for user "
					+ getUser().getId(), e);
		}

		this.newMessages = null != this.userGroup
				&& this.maxMsgId > Math.max(this.userGroup.getNewestGroupMsg(),
						this.userGroup.getNewestOwnMsg());
		if (null != this.userGroup) {
			init();
		}
	}

	// @PostConstruct
	public void init() {
		initNewMsgGrowl();
		initNewEventGrowl();
		initNewEventMsgGrowl();
		try {
			getLearnweb().getGroupManager().updateMsgStatistics(this.userGroup);
		} catch (SQLException e) {
			logger.error("init(): trouble with notifying about new messages", e);
		}
	}

	private void initNewMsgGrowl() {
		// growl notification about new message(s)
		if (this.newMessages
				&& this.maxMsgId > Math.max(Math.max(
						this.userGroup.getNewestGroupMsg(),
						this.userGroup.getNewestOwnMsg()), this.userGroup
						.getNewestGroupMsgNotification())) {
			try {
				List<GroupMessage> messages = getLearnweb()
						.getGroupMessageManager()
						.getGroupMessages(
								this.userGroup.getGroupId(),
								Math.max(
										this.userGroup.getNewestGroupMsg(),
										this.userGroup
												.getNewestGroupMsgNotification()) + 1);
				if (!messages.isEmpty()) {
					logger.debug("Notify user about new " + messages.size()
							+ " messages");
					getFacesContext().getExternalContext().getFlash()
							.setKeepMessages(true);

					getFacesContext()
							.addMessage(
									null,
									new FacesMessage(
											FacesMessage.SEVERITY_INFO,
											messages.get(0).getUsername()
													+ ": "
													+ messages.get(0)
															.getMessage(), ""));
					if (messages.size() == 2) {
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_INFO,
												"There is one additional new team message.",
												""));
					}
					if (messages.size() > 2) {
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_INFO,
												"There are "
														+ (messages.size() - 1)
														+ " additional new team messages.",
												""));
					}
					this.userGroup.setNewestGroupMsgNotification(messages
							.get(0).getMessageId());
				}
			} catch (SQLException e) {
				logger.error(
						"initNewMsgGrowl(): trouble with notifying about new messages",
						e);
			}
		}
	}

	private void initNewEventGrowl() {
		if (this.userGroup == null
				|| this.userGroup.getRole().equals(UserGroupRole.MEMBER))
			return;
		// growl notification about new invitation events
		if (this.newEvent
				&& this.maxInvEventId > Math.max(Math.max(
						this.userGroup.getNewestInvEvent(),
						this.userGroup.getNewestOwnInvEvent()), this.userGroup
						.getNewestInvEventNotification())) {
			try {
				List<InvitationEvent> invitationEvents = getLearnweb()
						.getInvitationManager()
						.getInvitationEvents(
								this.userGroup.getGroupId(),
								false,
								Math.max(
										this.userGroup.getNewestInvEvent(),
										this.userGroup
												.getNewestInvEventNotification()) + 1);
				if (!invitationEvents.isEmpty()) {
					logger.debug("Notify leader about new "
							+ invitationEvents.size() + " invitation events");
					getFacesContext().getExternalContext().getFlash()
							.setKeepMessages(true);

					String message = "";
					InvitationEvent event = invitationEvents.get(0);
					if (event.getSource() == this.getUserGroup().getGroupId()) {
						if (event.getType() == InvitationEvent.Type.OFFER) {
							message = "You asked "
									+ event.getTarget().getGroupname()
									+ " (score:"
									+ event.getTarget().getScore()
									+ ", members:"
									+ event.getTarget().getGroup()
											.getNumMembers()
									+ ") to invite you to join their team.";
						} else {
							message = "You have invited "
									+ event.getTarget().getGroupname()
									+ " (score:"
									+ event.getTarget().getScore()
									+ ", members:"
									+ event.getTarget().getGroup()
											.getNumMembers()
									+ ") to join your team.";
						}
					} else {
						if (event.getType() == InvitationEvent.Type.OFFER) {
							message = "You where asked by "
									+ event.getTarget().getGroupname()
									+ " (score:"
									+ event.getTarget().getScore()
									+ ", members:"
									+ event.getTarget().getGroup()
											.getNumMembers()
									+ ") to invite them to join your team.";
						} else {
							message = "You have been invited by "
									+ event.getTarget().getGroupname()
									+ " (score:"
									+ event.getTarget().getScore()
									+ ", members:"
									+ event.getTarget().getGroup()
											.getNumMembers()
									+ ") to join their team.";
						}
					}

					getFacesContext().addMessage(
							null,
							new FacesMessage(FacesMessage.SEVERITY_INFO,
									message, ""));

					if (invitationEvents.size() == 2) {
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_INFO,
												"There is one additional new invitation or request for invitation.",
												""));
					}
					if (invitationEvents.size() > 2) {
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_INFO,
												"There are "
														+ (invitationEvents
																.size() - 1)
														+ "  additional new invitations or requests for invitation.",
												""));
					}
					this.userGroup
							.setNewestInvEventNotification(invitationEvents
									.get(0).getId());
				}
			} catch (SQLException e) {
				logger.error(
						"initNewEventGrowl(): trouble with notifying about new messages",
						e);
			}
		}
	}

	private void initNewEventMsgGrowl() {
		// growl notification about new message(s)
		if (this.groupMerged
				&& this.maxInvMsgId > Math.max(Math.max(
						this.userGroup.getNewestInvMsg(),
						this.userGroup.getNewestOwnInvMsg()), this.userGroup
						.getNewestInvMsgNotification())) {
			try {
				List<InvitationMessage> messages = null;
				if (this.userGroup.getRole().equals(UserGroupRole.MEMBER)) {
					messages = getLearnweb()
							.getInvitationManager()
							.getMergeMessages(
									this.userGroup.getGroupId(),
									Math.max(
											this.userGroup.getNewestInvMsg(),
											this.userGroup
													.getNewestInvMsgNotification()) + 1);
				} else {
					messages = getLearnweb()
							.getInvitationManager()
							.getInvitationMessages(
									this.userGroup.getGroupId(),
									Math.max(
											this.userGroup.getNewestInvMsg(),
											this.userGroup
													.getNewestInvMsgNotification()) + 1);
				}
				if (null != messages && !messages.isEmpty()) {
					logger.debug("Notify user about new " + messages.size()
							+ " invitation/merge messages");
					getFacesContext().getExternalContext().getFlash()
							.setKeepMessages(true);

					getFacesContext().addMessage(
							null,
							new FacesMessage(FacesMessage.SEVERITY_INFO,
									messages.get(0).getMessage(), ""));

					if (messages.size() == 2) {
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_INFO,
												"There is one additional new team history message.",
												""));
					}
					if (messages.size() > 2) {
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_INFO,
												"There are "
														+ (messages.size() - 1)
														+ " additional new team history messages.",
												""));
					}
					this.userGroup.setNewestInvMsgNotification(messages.get(0)
							.getMessageId());
				}
			} catch (SQLException e) {
				logger.error(
						"initNewMsgGrowl(): trouble with notifying about new invitation messages",
						e);
			}
		}
	}

	private void getRandomGroup() throws SQLException {
		// check progress
		if (!checkProgress())
			return;
		// get random group
		List<Group> groups = getLearnweb().getGroupManager().getAllGroups();
		if (groups.isEmpty())
			return;
		// select a group
		// reduce candidate groups to those with lowest number of members
		int minMembers = Integer.MAX_VALUE;
		for (Group g : groups) {
			if (g.getNumMembers() < minMembers) {
				minMembers = g.getNumMembers();
			}
		}

		for (Iterator<Group> it = groups.iterator(); it.hasNext();) {
			Group g = it.next();
			if (g.getNumMembers() > minMembers) {
				it.remove();
			}
		}
		// assign randomly to one of those groups
		Group group = groups.get(getRandom().nextInt(groups.size()));

		// join group as member
		// create user group entry
		this.userGroup = new UserGroup(getUser().getId(), group.getId());
		this.userGroup.setRole(UserGroupRole.MEMBER);
		int maxId = 0;
		try {
			maxId = getLearnweb().getGroupMessageManager().getNewestMessageId(
					group.getId());
		} catch (SQLException e) {
			logger.error("getRandomGroup(): error getting maxMessageId", e);
		}
		this.userGroup.setNewestGroupMsgNotification(maxId);
		// save user group entry
		getLearnweb().getGroupManager().save(this.userGroup);
		// in reality this is consistent enough
		group.setNumMembers(group.getNumMembers() + 1);
		getLearnweb().getGroupManager().save(group);
		getLearnweb().getGroupManager().logJoin(getUser().getId(),
				group.getId());

		// increment groupscore by own score
		// create score entry for usergroup
		ScoreEntry scoreEntry = getLearnweb().getScoreManager()
				.getScoreEntryByUser(getUser());
		getLearnweb().getGroupScoreManager().incrementScore(group.getId(),
				scoreEntry.getScore());

		log("getgroup");
		logger.info("user " + this.userGroup.getUserId() + " joined group "
				+ group.getId());
	}

	/** returns true if user exceeds progress necessary to join group */
	public boolean checkProgress() {
		int joinProgress = Integer.parseInt(getLearnweb().getProperties()
				.getProperty("group_join_progress"));
		if (joinProgress > 1) {
			ImagesBean imagesBean = (ImagesBean) UtilBean
					.getManagedBean("imagesBean");
			if (null == imagesBean) {
				logger.error("getRandomGroup(): could not retrieve imagesBean!");
				return false;
			}
			Bucket userBucket = imagesBean.getBucket();
			if (null == userBucket) {
				logger.error("getRandomGroup(): user bucket was null!");
				return false;
			}
			if (userBucket.getBucketNumber() == 1
					&& userBucket.getProgress() < joinProgress)
				return false;
			// user progress far enough to be assigned to group
			if (joinProgress == 101) { // special case; check if batch was
										// correct
				// user has score = BUCKETBONUS immediately after completing
				// first correct batch
				if (imagesBean.getEntry().getScore() < ImagesBean.BUCKETBONUS)
					return false;
			}
		}
		return true;
	}

	private void getNewLeaderGroup() throws SQLException {
		// check progress
		if (!checkProgress())
			return;
		// create group
		Group group = new Group();
		group.setGroupname("Team " + getUser().getUsername());
		group.setNumMembers(1);
		// save group
		getLearnweb().getGroupManager().save(group);

		// create user group entry
		this.userGroup = new UserGroup(getUser().getId(), group.getId());
		this.userGroup.setRole(UserGroupRole.LEADER);
		// save user group entry
		getLearnweb().getGroupManager().save(this.userGroup);
		getLearnweb().getGroupManager().logJoin(getUser().getId(),
				group.getId());

		// create score entry for usergroup
		ScoreEntry scoreEntry = getLearnweb().getScoreManager()
				.getScoreEntryByUser(getUser());
		GroupScoreEntry groupScoreEntry = new GroupScoreEntry(group);
		groupScoreEntry.setScore(scoreEntry.getScore());
		// will be overwritten on update of ranks following insert!
		groupScoreEntry.setLastScoredTime(scoreEntry.getLastScoredTime());
		getLearnweb().getGroupScoreManager().insert(groupScoreEntry);

		log("getgroup");
		logger.info("created group " + this.userGroup.getGroupId()
				+ " with leader " + this.userGroup.getUserId());
	}

	public String goToGroup() {
		logger.debug("go to group");
		log("go_to_group");
		this.newMessages = false;
		this.newEvent = false;
		this.groupMerged = false;
		// to late to prevent growl here. alternative?
		return getTemplateDir() + "/group.jsf";
	}

	public String getTimeToEndGroupFormationString() {
		long now = Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis();
		long start = getLearnweb().getEndGroupFormation().getTimeInMillis();
		long diff = start - now;
		String result = "";

		long days = (long) Math.floor(diff / (1000 * 60 * 60 * 24));
		Double remainder = Math.floor(diff % (1000 * 60 * 60 * 24));
		long hours = (long) Math.floor(remainder / (1000 * 60 * 60));
		remainder = Math.floor(remainder % (1000 * 60 * 60));
		long minutes = (long) Math.floor(remainder / (1000 * 60));
		remainder = Math.floor(remainder % (1000 * 60));
		long seconds = (long) Math.floor(remainder / (1000));

		if (diff >= 1000 * 60) {
			result += ((days > 0) ? days + ((days != 1) ? " days, " : " day, ")
					: "")
					+ ((days > 0 || hours > 0) ? hours
							+ ((hours != 1) ? " hours" : " hour") + " and "
							: "")
					+ minutes
					+ ((minutes != 1) ? " minutes" : " minute");
		} else {
			result += seconds + ((seconds != 1) ? "seconds" : "second");
		}
		return result;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	public GroupMode getMode() {
		return mode;
	}

	public void setMode(GroupMode mode) {
		this.mode = mode;
	}

	public Random getRandom() {
		if (null == random) {
			this.random = new SecureRandom();
		}
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public boolean useGroups() {
		return !this.mode.equals(GroupMode.NONE);
	}

	public int getMaxMsgId() {
		return maxMsgId;
	}

	public void setMaxMsgId(int maxMsgId) {
		this.maxMsgId = maxMsgId;
	}

	public boolean isNewMessages() {
		return newMessages;
	}

	public void setNewMessages(boolean newMessages) {
		this.newMessages = newMessages;
	}

	public boolean isGroupMerged() {
		return groupMerged;
	}

	public void setGroupMerged(boolean groupMerged) {
		this.groupMerged = groupMerged;
	}

	public boolean isNewEvent() {
		return newEvent;
	}

	public void setNewEvent(boolean newEvent) {
		this.newEvent = newEvent;
	}
}
