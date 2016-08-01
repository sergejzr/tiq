package de.l3s.mt.beans;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnwebBeans.ApplicationBean;

@SessionScoped
@ManagedBean
public class RightNavigationBean extends ApplicationBean implements
		Serializable {

	private static final long serialVersionUID = 1028756563593111264L;
	private Page content;
	private static Logger logger = Logger.getLogger(RightNavigationBean.class);

	public RightNavigationBean() {
		// start at leaderboard
		this.content = Page.LEADERBOARD;
		logger.debug("RightNavigationBean: " + this.content);
	}

	public void goToProfile() {
		logger.debug("go to profile");
		this.content = Page.PROFILE;
		clearMsgs();
	}

	private void clearMsgs() {
		// prevent growl being shown twice
		Iterator<String> itIds = FacesContext.getCurrentInstance()
				.getClientIdsWithMessages();
		while (itIds.hasNext()) {
			List<FacesMessage> messageList = FacesContext.getCurrentInstance()
					.getMessageList(itIds.next());
			if (!messageList.isEmpty()) { // if empty, it will be unmodifiable
											// and throw
											// UnsupportedOperationException...
				messageList.clear();
			}
		}
	}

	public void goToLeaderboard() {
		logger.debug("go to leaderboard");
		this.content = Page.LEADERBOARD;
		clearMsgs();
	}

	public boolean includeProfile() {
		return this.content.equals(Page.PROFILE);
	}

	public boolean includeLeaderboard() {
		return this.content.equals(Page.LEADERBOARD);
	}

	public enum Page {
		LEADERBOARD, PROFILE;
	}

	public Page getContent() {
		return content;
	}

	public void setContent(Page content) {
		this.content = content;
	}

}
