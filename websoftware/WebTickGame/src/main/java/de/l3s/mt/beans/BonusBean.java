package de.l3s.mt.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.BonusEvent;

/**
 * Application scoped (singleton) bonus event bean that holds all bonus events
 * and handles accounting (e.g. of remaining bonus pool size) of the events.
 * 
 * Singleton ensures consistent state.
 * 
 * @author rokicki
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class BonusBean implements Serializable {
	private static final long serialVersionUID = 7L;
	private static final Logger logger = Logger.getLogger(BonusBean.class);

	@ManagedProperty(value = "#{learnwebBean.learnweb}")
	private Learnweb learnweb;

	private List<BonusEvent> events;
	private List<BonusEvent> pastEvents;
	private BonusEvent latestEvent;
	private BonusEvent nextEvent;
	// warning time in milliseconds
	private long warningTime;
	private boolean showSchedule;
	private boolean bonusFixed;
	private int bonusFactor;
	private boolean bonusLimited;
	private boolean bonusEventsEnabled;
	private int bonusLimit;
	private int bonusLimitBatches;

	private final Object latestEventLock = new Object();
	private final Object claimBonusLock = new Object();

	public BonusBean() {
		logger.debug("construct bonusBean");
		this.pastEvents = new ArrayList<BonusEvent>();
	}

	@PostConstruct
	public void init() {
		logger.info("init BonusBean");
		initParameters();
		try {
			setEvents(getLearnweb().getBonusEventManager().getEvents());
			if (getEvents() == null || getEvents().isEmpty()) {
				logger.warn("No bonus events were retrieved!");
			}
		} catch (SQLException e) {
			logger.error("Error while retrieving bonus events", e);
		}
	}

	/** init bonus event specific parameters */
	private void initParameters() {
		try {
			setBonusEventsEnabled(Boolean.parseBoolean(getLearnweb()
					.getProperties().getProperty("BONUS_EVENTS_ENABLED")));
			logger.info("bonusEventsEnabled set to " + isBonusEventsEnabled());
		} catch (Exception e) {
			logger.error("Error parsing BONUS_EVENTS_ENABLED", e);
		}
		try {
			setWarningTime(Integer.parseInt(getLearnweb().getProperties()
					.getProperty("EVENT_WARNING_TIME")) * 3600000);
			logger.info("warning time set to " + getWarningTime()
					+ " milliseconds.");
		} catch (NumberFormatException e) {
			logger.error("Error parsing EVENT_WARNING_TIME", e);
		}
		try {
			setBonusFactor(Integer.parseInt(getLearnweb().getProperties()
					.getProperty("BONUS_FACTOR")));
			logger.info("bonusFactor set to " + getBonusFactor());
		} catch (NumberFormatException e) {
			logger.error("Error parsing BONUS_FACTOR", e);
		}
		try {
			setShowSchedule(Boolean.parseBoolean(getLearnweb().getProperties()
					.getProperty("SHOW_EVENT_SCHEDULE")));
		} catch (Exception e) {
			logger.error("Error parsing SHOW_EVENT_SCHEDULE", e);
		}
		try {
			setBonusFixed(Boolean.parseBoolean(getLearnweb().getProperties()
					.getProperty("BONUS_FACTOR_FIXED")));
		} catch (Exception e) {
			logger.error("Error parsing BONUS_FACTOR_FIXED", e);
		}
		try {
			setBonusLimited(Boolean.parseBoolean(getLearnweb().getProperties()
					.getProperty("BONUS_LIMITED")));
		} catch (Exception e) {
			logger.error("Error parsing BONUS_LIMITED", e);
		}
		try {
			this.bonusLimit = Integer.parseInt(getLearnweb().getProperties()
					.getProperty("BONUS_LIMIT"));
			this.bonusLimitBatches = (this.bonusLimit + 99) / 100;
			logger.info("bonusLimit=" + this.bonusLimit);
		} catch (NumberFormatException e) {
			logger.error("Error parsing BONUS_LIMIT", e);
		}
	}

	public int claimBonus(int points) {
		int bonus = 0;
		// is active?
		if (isBonusActive()) {
			// event could change in the meantime as we use separate
			// latestEventLock (for
			// performance reasons)
			BonusEvent event = this.latestEvent;
			if (null == event) {
				return 0;
			}
			synchronized (claimBonusLock) {
				// claim bonus
				if (this.bonusLimited && !event.isPoolDepleted()) {
					if (points > 0) {
						event.setBonusClaimed(event.getBonusClaimed() + points);
					}
					// save latestEvent state
					try {
						getLearnweb().getBonusEventManager().save(event);
					} catch (SQLException e) {
						logger.error("Error saving BonusEvent " + event, e);
					}
				}
				// award bonus points regardless in case of concurrency
				bonus = points * (event.getBonusFactor() - 1);
			}
		}
		return bonus;
	}

	public BonusEvent getLatestEvent() {
		long now = System.currentTimeMillis();
		if (null == this.latestEvent || this.latestEvent.getEnd() < now
				|| (this.bonusLimited && this.latestEvent.isPoolDepleted())) {
			getNextEvent(now);
		}
		return this.latestEvent;
	}

	private void getNextEvent(long now) {
		logger.debug("get next event");
		synchronized (latestEventLock) {
			// handle last event
			if ((getEvents() == null || getEvents().isEmpty() || getEvents()
					.size() == 1)
					&& (this.latestEvent == null
							|| this.latestEvent.getEnd() < now || (this.bonusLimited && this.latestEvent
							.isPoolDepleted()))) {
				if (null != this.latestEvent) {
					logger.info("removing last event: " + this.latestEvent);
				}
				this.latestEvent = null;
				this.nextEvent = null;
				return;
			}
			// get next event if current one is expired
			while (!getEvents().isEmpty()
					&& (this.latestEvent == null
							|| this.latestEvent.getEnd() < now || (this.bonusLimited && this.latestEvent
							.isPoolDepleted()))) {
				if (this.latestEvent != null) {
					logger.info("moving event to past events: "
							+ this.latestEvent);
					this.pastEvents.add(getEvents().remove(0));
				}
				// get new current event
				if (!getEvents().isEmpty()) {
					this.latestEvent = getEvents().get(0);
					logger.info("new latestevent: " + this.latestEvent);
				} else {
					this.latestEvent = null;
					logger.debug("no more events");
				}
				// if there is another following, track it
				if (getEvents().size() >= 2) {
					this.nextEvent = getEvents().get(1);
					logger.info("new nextEvent is " + this.nextEvent);
				} else {
					this.nextEvent = null;
				}
			}
		}
	}

	public boolean showNextEventNotice() {
		if (getLatestEvent() == null || this.nextEvent == null)
			return false;
		// does next event directly follow latest event and is time to next
		// event < warning time?
		return this.isBonusActive()
				&& this.latestEvent.getEnd() == this.nextEvent.getStart()
				&& getNextEvent().getStart() - System.currentTimeMillis() <= getWarningTime();
	}

	public boolean isBonusActive() {
		long now = System.currentTimeMillis();
		return this.isBonusEventsEnabled() && getLatestEvent() != null
				&& this.latestEvent.getStart() <= now
				&& this.latestEvent.getEnd() >= now
				&& !(isBonusLimited() && this.latestEvent.isPoolDepleted());
	}

	/**
	 * show event if there is one and it starts in less than warning time
	 */
	public boolean showEvent() {
		if (getLatestEvent() == null)
			return false;
		return getLatestEvent().getStart() - System.currentTimeMillis() <= getWarningTime();
	}

	public Learnweb getLearnweb() {
		return learnweb;
	}

	public void setLearnweb(Learnweb learnweb) {
		this.learnweb = learnweb;
	}

	public List<BonusEvent> getEvents() {
		return events;
	}

	public void setEvents(List<BonusEvent> events) {
		this.events = events;
	}

	public void setLatestEvent(BonusEvent latestEvent) {
		this.latestEvent = latestEvent;
	}

	public long getWarningTime() {
		return warningTime;
	}

	public void setWarningTime(long warningTime) {
		this.warningTime = warningTime;
	}

	public boolean isShowSchedule() {
		return showSchedule;
	}

	public void setShowSchedule(boolean showSchedule) {
		this.showSchedule = showSchedule;
	}

	public boolean isBonusFixed() {
		return bonusFixed;
	}

	public void setBonusFixed(boolean bonusFixed) {
		this.bonusFixed = bonusFixed;
	}

	public boolean isBonusLimited() {
		return bonusLimited;
	}

	public void setBonusLimited(boolean bonusLimited) {
		this.bonusLimited = bonusLimited;
	}

	public boolean isBonusEventsEnabled() {
		return bonusEventsEnabled;
	}

	public void setBonusEventsEnabled(boolean bonusEventsEnabled) {
		this.bonusEventsEnabled = bonusEventsEnabled;
	}

	public int getBonusFactor() {
		return bonusFactor;
	}

	public void setBonusFactor(int bonusFactor) {
		this.bonusFactor = bonusFactor;
	}

	public List<BonusEvent> getPastEvents() {
		return pastEvents;
	}

	public void setPastEvents(List<BonusEvent> pastEvents) {
		this.pastEvents = pastEvents;
	}

	public BonusEvent getNextEvent() {
		return nextEvent;
	}

	public void setNextEvent(BonusEvent nextEvent) {
		this.nextEvent = nextEvent;
	}

	public int getBonusLimit() {
		return bonusLimit;
	}

	public void setBonusLimit(int bonusLimit) {
		this.bonusLimit = bonusLimit;
	}

	public int getBonusLimitBatches() {
		return bonusLimitBatches;
	}

	public void setBonusLimitBatches(int bonusLimitBatches) {
		this.bonusLimitBatches = bonusLimitBatches;
	}

}
