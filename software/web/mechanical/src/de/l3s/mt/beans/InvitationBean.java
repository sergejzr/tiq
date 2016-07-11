package de.l3s.mt.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.Group;
import de.l3s.mt.model.GroupScoreEntry;
import de.l3s.mt.model.InvitationEvent;
import de.l3s.mt.model.InvitationMessage;

@ManagedBean
@ViewScoped
public class InvitationBean extends ApplicationBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(InvitationBean.class);

	private int targetGroup;
	private int targetEvent;
	private static boolean initialized = false;
	public static int GROUPLIMIT = 50;

	public InvitationBean() {
		initParameters();
		logger.debug("construct");
		this.targetGroup = -1;
		this.targetEvent = -1;
	}

	public void initParameters() {
		logger.debug("initialize parameters? " + !initialized);
		if (!initialized) {
			try {
				InvitationBean.GROUPLIMIT = Integer.parseInt(getLearnweb()
						.getProperties().getProperty("GROUP_LIMIT").trim());
			} catch (NumberFormatException e) {
				logger.error("error parsing parameter GROUP_LIMIT", e);
				InvitationBean.GROUPLIMIT = 50;
			}
			logger.info("INIT: GROUPLIMIT=" + InvitationBean.GROUPLIMIT);

			InvitationBean.initialized = true;
		}
	}

	public String doInvite() {
		log("invite");
		doInviatationEvent(InvitationEvent.Type.INVITATION);
		return getFacesContext().getViewRoot().getViewId()
				+ "?faces-redirect=true";
	}

	public String doOffer() {
		log("offer");
		doInviatationEvent(InvitationEvent.Type.OFFER);
		return getFacesContext().getViewRoot().getViewId()
				+ "?faces-redirect=true";
	}

	private void doInviatationEvent(InvitationEvent.Type type) {
		logger.debug("doInviatationEvent(" + type + ") with targetGroup "
				+ this.targetGroup);
		if (this.targetGroup < 1) {
			logger.error("doInviatationEvent(): targetGroup not valid:"
					+ this.targetGroup);
			return;
		}

		GroupBean groupBean = getGroupBean();
		int source = groupBean.getUserGroup().getGroupId();
		int sink = this.targetGroup;
		// check if team size limit would be exceeded
		try {
			Group sourceGroup = getLearnweb().getGroupManager()
					.getGroup(source);
			Group targetGroup = getLearnweb().getGroupManager().getGroup(sink);
			if (sourceGroup.getNumMembers() + targetGroup.getNumMembers() > getGroupLimit()) {
				getFacesContext()
						.addMessage(
								null,
								new FacesMessage(
										FacesMessage.SEVERITY_WARN,
										(type.equals(InvitationEvent.Type.INVITATION) ? "invitation"
												: "request for invitation")
												+ " cannot be send due to team size limit.",
										""));
				logger.info("Do not send " + type.name()
						+ " due to group size limit for (" + source + ","
						+ sink + ")");
				return;
			}
		} catch (SQLException e) {
			logger.error(
					"doInvitationEvent: error while checking group size limit",
					e);
			return;
		}

		// check if there is already an invitation with same type
		try {
			List<InvitationEvent> invitations = getLearnweb()
					.getInvitationManager().getInvitationEvents(source, sink);
			for (InvitationEvent inv : invitations) {
				if (inv.getType().equals(type)) {
					getFacesContext().getExternalContext().getFlash()
							.setKeepMessages(true);

					getFacesContext()
							.addMessage(
									null,
									new FacesMessage(
											FacesMessage.SEVERITY_WARN,
											"An "
													+ (type.equals(InvitationEvent.Type.INVITATION) ? "invitation"
															: "offer")
													+ " is already pending.",
											""));
					logger.debug("event with type " + type + " already exists");
					return;
				}
			}
			// check if events with different type in other direction exist
			invitations = getLearnweb().getInvitationManager()
					.getInvitationEvents(sink, source);
			for (InvitationEvent inv : invitations) {
				if (!inv.getType().equals(type)) {
					// offer/invitation in other direction exists
					if (type.equals(InvitationEvent.Type.OFFER)) {

						getFacesContext().getExternalContext().getFlash()
								.setKeepMessages(true);
						getFacesContext()
								.addMessage(
										null,
										new FacesMessage(
												FacesMessage.SEVERITY_WARN,
												"An invitation is already pending.",
												""));
						logger.debug("an invitation alread exists for event with type "
								+ type);
						return;
					}

					// if type == invitation: offer exists. remove offer and
					// proceed to invite group
					getLearnweb().getInvitationManager().removeEvent(inv,
							InvitationEvent.Result.ACCEPT);
				}
			}
		} catch (SQLException e) {
			logger.error(
					"doInviatationEvent(): error checking for existing events",
					e);
			return;
		}
		InvitationEvent invitation = new InvitationEvent();
		invitation.setSource(source);
		invitation.setSink(sink);
		invitation.setType(type);
		try {
			getLearnweb().getInvitationManager().insert(invitation);
			groupBean.getUserGroup().setNewestOwnInvEvent(invitation.getId());
			getLearnweb().getGroupManager().updateMsgStatistics(
					groupBean.getUserGroup());
		} catch (SQLException e) {
			logger.error("doInvite(): error inserting invitation", e);
			return;
		}

		getFacesContext().getExternalContext().getFlash().setKeepMessages(true);
		getFacesContext().addMessage(
				null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, (type
						.equals(InvitationEvent.Type.INVITATION) ? "invitation"
						: "request for invitation")
						+ " sent", ""));
		logger.debug("event with type " + type + " sent.");
	}

	public String doAccept() throws SQLException {
		logger.debug("doAccept()");
		try {
			InvitationEvent event = getLearnweb().getInvitationManager()
					.getEventById(this.targetEvent);
			event.setTarget(getLearnweb().getGroupScoreManager()
					.getScoreEntryById(event.getSink()));
			if (event.getType().equals(InvitationEvent.Type.OFFER)) {
				log("accept-offer");
				acceptOffer(event);
			} else {
				log("accept-invitation");
				acceptInvitation(event);
			}
		} catch (SQLException e) {
			logger.error("error while accepting event", e);
			throw e;
		}

		return getFacesContext().getViewRoot().getViewId()
				+ "?faces-redirect=true";
	}

	public void acceptOffer(InvitationEvent event) throws SQLException {
		if (null == event
				|| !event.getType().equals(InvitationEvent.Type.OFFER))
			return;
		// accept offer: -> doInvite for other group
		this.targetGroup = event.getSource();
		// delete offer
		getLearnweb().getInvitationManager().removeEvent(event,
				InvitationEvent.Result.ACCEPT);
		doInvite();
	}

	public void acceptInvitation(InvitationEvent event) throws SQLException {
		if (null == event
				|| !event.getType().equals(InvitationEvent.Type.INVITATION))
			return;
		// check if team size limit would be exceeded
		try {
			Group sourceGroup = getLearnweb().getGroupManager().getGroup(
					event.getSource());
			Group targetGroup = getLearnweb().getGroupManager().getGroup(
					event.getSink());
			if (sourceGroup.getNumMembers() + targetGroup.getNumMembers() > getGroupLimit()) {
				getFacesContext().getExternalContext().getFlash()
						.setKeepMessages(true);
				getFacesContext()
						.addMessage(
								null,
								new FacesMessage(
										FacesMessage.SEVERITY_WARN,
										"Can not accept invitation. Team size limit would be exceeded!",
										""));
				logger.info("Do not accept invitation due to group size limit for ("
						+ event.getSource() + "," + event.getSink() + ")");
				getLearnweb().getInvitationManager().removeEvent(event,
						InvitationEvent.Result.CANCEL);
				return;
			}
		} catch (SQLException e) {
			logger.error(
					"acceptInvitation: error while checking group size limit",
					e);
			return;
		}

		/* accept invitation:
		 * 0. check group size cap and delete event if it is exceeded
		 * 1. cancel all invitation events for own group and add notifications to other groups (case II and case IV)
		 * 2. for invitationEvents of the other group (the one we merge into):
		 *   - case I: cancel events and notify
		 *   - case III: notify about changes
		 * 3. merge
		 * 4. create merge invitationMessage
		 * 5. delete event
		 */
		List<InvitationEvent> sharedEvents = new ArrayList<InvitationEvent>();
		// we were target of invitation
		List<InvitationEvent> ownEvents = getLearnweb().getInvitationManager()
				.getInvitationEvents(event.getSink(), true);
		for (InvitationEvent ev : ownEvents) {
			// skip invitation itself
			if (ev.getId() == event.getId())
				continue;
			// collect all other events between A and B
			if (ev.getSink() == event.getSink()
					&& ev.getSource() == event.getSource()
					|| ev.getSource() == event.getSink()
					&& ev.getSink() == event.getSource()) {
				sharedEvents.add(ev);
				continue;
			}
			if (ev.getSource() == event.getSink()) {
				handleCase4(event, ev);
			}
			if (ev.getSink() == event.getSink()) {
				handleCase2(event, ev);
			}
		}
		// we merge into source of invitation
		List<InvitationEvent> otherEvents = getLearnweb()
				.getInvitationManager().getInvitationEvents(event.getSource(),
						true);
		for (InvitationEvent ev : otherEvents) {
			// skip all events between A and B (we have already collected them)
			if (ev.getSink() == event.getSink()
					&& ev.getSource() == event.getSource()
					|| ev.getSource() == event.getSink()
					&& ev.getSink() == event.getSource()) {
				continue;
			}
			if (ev.getSource() == event.getSource()) {
				handleCase3(event, ev);
			}
			if (ev.getSink() == event.getSource()) {
				handleCase1(event, ev);
			}
		}

		/* perform merge 
		 * - mark score of group B
		 * - transfer users of group B to group A (role MEMBER!)
		 * - compute current number of users for group A
		 * - retrieve current scorentry of group B and increment score of group A
		 * - delete scoreentry of group B (log verbose (info)!)
		 */
		// mark score entry of group B
		int groupB = event.getSink();
		getLearnweb().getGroupScoreManager().setMarked(groupB, true);
		// transfer users of groupB to groupA
		int groupA = event.getSource();
		getLearnweb().getGroupManager().mergeGroups(groupB, groupA,
				event.getId());
		// compute current number of users for group A
		Group A = getLearnweb().getGroupManager().getGroup(groupA);
		Group B = getLearnweb().getGroupManager().getGroup(groupB);
		A.setNumMembers(A.getNumMembers() + B.getNumMembers());
		getLearnweb().getGroupManager().save(A);
		// retrieve current scorentry of group B and increment score of group A
		GroupScoreEntry scoreGroupB = getLearnweb().getGroupScoreManager()
				.getScoreEntryById(groupB);
		getLearnweb().getGroupScoreManager().incrementScore(groupA,
				scoreGroupB.getScore());
		// delete scoreentry of group B (log verbose (info)!)
		logger.info("deleting group score entry: " + scoreGroupB);
		getLearnweb().getGroupScoreManager().deleteGroupScoreEntry(groupB);
		// update ranks
		getLearnweb().getGroupScoreManager().incrementScore(groupA, 0);
		// create merge message (for group A)
		Group group = getLearnweb().getGroupManager().getGroup(groupB);
		InvitationMessage message = new InvitationMessage();
		message.setGroupId(groupA);
		message.setMerge(true);
		message.setMessage(group.getGroupname() + " (" + group.getNumMembers()
				+ " member" + ((group.getNumMembers() > 1) ? "s" : "")
				+ ") has joined your team.");

		getLearnweb().getInvitationManager().insert(message);

		// remove event
		getLearnweb().getInvitationManager().removeEvent(event,
				InvitationEvent.Result.ACCEPT);
		// cancel other shared events
		for (InvitationEvent ev : sharedEvents) {
			getLearnweb().getInvitationManager().removeEvent(ev,
					InvitationEvent.Result.CANCEL);
		}

		// update notification to prevent growl
		GroupBean groupBean = getGroupBean();
		groupBean.getUserGroup().setNewestInvMsgNotification(
				message.getMessageId());
		getLearnweb().getGroupManager().updateMsgStatistics(
				groupBean.getUserGroup());

		getFacesContext().getExternalContext().getFlash().setKeepMessages(true);
		getFacesContext().addMessage(null,
				new FacesMessage("You have accepted the invitation.", ""));
	}

	public void handleCase1(InvitationEvent cause, InvitationEvent target)
			throws SQLException {
		logger.debug("handleCase1: cause:" + cause + ", target:" + target);
		// case I: group C has sent invitation/offer to group A
		int groupC = target.getSource();
		// do not need to notify sender of offer to be invited
		if (target.getType().equals(InvitationEvent.Type.OFFER))
			return;
		// cancel invitation and notify C about changes to group A
		// cancel invitation
		getLearnweb().getInvitationManager().removeEvent(target,
				InvitationEvent.Result.CANCEL);
		// notify c
		String groupAString = target.getTarget().getGroupname();
		String groupBString = cause.getTarget().getGroupname();
		InvitationMessage message = new InvitationMessage();
		message.setGroupId(groupC);
		message.setMessage(groupAString
				+ " has been joined by "
				+ groupBString
				+ ". Your invitation to "
				+ groupAString
				+ " has been canceled. You may send a new invitation after reviewing the changes.");
		getLearnweb().getInvitationManager().insert(message);
	}

	public void handleCase2(InvitationEvent cause, InvitationEvent target)
			throws SQLException {
		logger.debug("handleCase2: cause:" + cause + ", target:" + target);
		// case II: Group D has sent invitation/offer to Group B
		// cancel offer and notify D about B joining A
		// cancel invitation and notify D about B joining A
		int groupD = target.getSource();
		// cancel invitation/offer
		getLearnweb().getInvitationManager().removeEvent(target,
				InvitationEvent.Result.CANCEL);
		// notify D
		String groupAString = getLearnweb().getGroupManager()
				.getGroup(cause.getSource()).getGroupname();
		String groupBString = cause.getTarget().getGroupname();
		InvitationMessage message = new InvitationMessage();
		message.setGroupId(groupD);
		message.setMessage(groupBString
				+ " has joined "
				+ groupAString
				+ ". Your "
				+ ((target.getType().equals(InvitationEvent.Type.INVITATION)) ? "invitation"
						: "request for invitation") + " to " + groupBString
				+ " has been canceled. ");
		getLearnweb().getInvitationManager().insert(message);
	}

	public void handleCase3(InvitationEvent cause, InvitationEvent target)
			throws SQLException {
		logger.debug("handleCase3: cause:" + cause + ", target:" + target);
		// case III: Group E has received invitation/offer from group A
		// offer: do nothing
		// invitation: Notify E about changes to Group A, so E can review
		// changes
		int groupE = target.getSink();
		Group A = getLearnweb().getGroupManager().getGroup(cause.getSource());
		Group E = getLearnweb().getGroupManager().getGroup(groupE);
		String groupAString = A.getGroupname();
		String groupBString = cause.getTarget().getGroupname();
		InvitationMessage message = new InvitationMessage();
		message.setGroupId(groupE);
		if (A.getNumMembers() + E.getNumMembers() > getGroupLimit()) {
			message.setMessage(groupAString
					+ " has been joined by "
					+ groupBString
					+ ". The "
					+ ((target.getType()
							.equals(InvitationEvent.Type.INVITATION)) ? "invitation"
							: "request for invitation") + " from "
					+ groupAString + " has been canceled.");
			getLearnweb().getInvitationManager().removeEvent(target,
					InvitationEvent.Result.CANCEL);
		} else {
			if (target.getType().equals(InvitationEvent.Type.OFFER))
				return;
			message.setMessage(groupAString + " has been joined by "
					+ groupBString
					+ ". Please review changes to score and members of "
					+ groupAString + " before deciding on the invitation.");
		}
		getLearnweb().getInvitationManager().insert(message);
	}

	public void handleCase4(InvitationEvent cause, InvitationEvent target)
			throws SQLException {
		logger.debug("handleCase4: cause:" + cause + ", target:" + target);
		// case IV: group F has received invitation/offer by group B
		// offer: cancel invitation and notify F
		// invitation: cancel invitation and notify F
		getLearnweb().getInvitationManager().removeEvent(target,
				InvitationEvent.Result.CANCEL);
		int groupF = target.getSink();
		String groupAString = getLearnweb().getGroupManager()
				.getGroup(cause.getSource()).getGroupname();
		String groupBString = cause.getTarget().getGroupname();
		InvitationMessage message = new InvitationMessage();
		message.setGroupId(groupF);
		message.setMessage(groupBString
				+ " has joined "
				+ groupAString
				+ ". The "
				+ ((target.getType().equals(InvitationEvent.Type.INVITATION)) ? "invitation"
						: "request for invitation") + " by " + groupBString
				+ " has been canceled. ");
		getLearnweb().getInvitationManager().insert(message);
	}

	public String doDecline() {
		logger.debug("doDecline()");
		log("decline");
		try {
			InvitationEvent event = getLearnweb().getInvitationManager()
					.getEventById(this.targetEvent);
			Group source = getLearnweb().getGroupManager().getGroup(
					event.getSource());
			Group target = getLearnweb().getGroupManager().getGroup(
					event.getSink());
			// delete event and insert into deleted table
			getLearnweb().getInvitationManager().removeEvent(event,
					InvitationEvent.Result.DECLINE);
			// create message to notify other group
			InvitationMessage otherMessage = new InvitationMessage();
			otherMessage.setGroupId(source.getId());
			// message for own group history
			InvitationMessage ownMessage = new InvitationMessage();
			ownMessage.setGroupId(target.getId());
			getFacesContext().getExternalContext().getFlash()
					.setKeepMessages(true);

			if (event.getType().equals(InvitationEvent.Type.OFFER)) {
				otherMessage.setMessage(target.getGroupname()
						+ " has declined your request for an invitation.");
				ownMessage
						.setMessage("You have declined a request for an invitation by "
								+ source.getGroupname() + ".");
				getFacesContext().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_INFO,
								"Request for invitation by "
										+ source.getGroupname() + " declined.",
								""));
			} else {
				otherMessage.setMessage(target.getGroupname()
						+ " has declined your invitation to join your team.");
				ownMessage
						.setMessage("You have declined an invitation to join "
								+ source.getGroupname() + ".");
				getFacesContext().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_INFO,
								"Invitation by " + source.getGroupname()
										+ " declined.", ""));
			}
			getLearnweb().getInvitationManager().insert(otherMessage);
			getLearnweb().getInvitationManager().insert(ownMessage);
			GroupBean groupBean = getGroupBean();
			groupBean.getUserGroup().setNewestOwnInvMsg(
					ownMessage.getMessageId());
			getLearnweb().getGroupManager().updateMsgStatistics(
					groupBean.getUserGroup());
		} catch (SQLException e) {
			logger.error("doDecline()", e);
		}

		return getFacesContext().getViewRoot().getViewId()
				+ "?faces-redirect=true";
	}

	public GroupBean getGroupBean() {
		return (GroupBean) UtilBean.getManagedBean("groupBean");
	}

	public int getTargetGroup() {
		return targetGroup;
	}

	public void setTargetGroup(int targetGroup) {
		this.targetGroup = targetGroup;
	}

	public int getTargetEvent() {
		return targetEvent;
	}

	public void setTargetEvent(int targetEvent) {
		this.targetEvent = targetEvent;
	}

	public int getGroupLimit() {
		return InvitationBean.GROUPLIMIT;
	}
}
