package de.l3s.mt.beans;

import java.io.Serializable;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.Bucket;
import de.l3s.mt.model.Entity;
import de.l3s.mt.model.Image;
import de.l3s.mt.model.ImagesInstance;
import de.l3s.mt.model.ScoreEntry;
import de.l3s.mt.model.UserGroup;

@ManagedBean(eager = false)
@SessionScoped
public class ImagesBean extends ApplicationBean implements Serializable {
	private static final long serialVersionUID = 9L;
	private static int HP_PENALTY;
	private static boolean ALLOW_DUPLICATE_PERSONS;
	private static boolean IMMEDIATE_FEEDBACK;
	private static int NUM_POOLS = 200;
	private static int NUM_CHOICES = 10;
	public static int BUCKETBONUS = 100;
	private static int GROUPSCORE_MAX_TRIALS = 10;
	private static int GROUPSCORE_BACKOFF = 50;
	private static boolean useGroups;
	private ImagesInstance imagesInstance;
	private Random generator;
	private ScoreEntry entry;
	private Bucket bucket;
	private int lastPerson = -1;
	private static boolean initialized = false;
	private static Logger logger = Logger.getLogger(ImagesBean.class);

	@ManagedProperty(value = "#{bonusBean}")
	private BonusBean bonusBean;

	public ImagesBean() throws SQLException {
		this.generator = new SecureRandom();
		initConstants();
		if (null == getUser()) {
			nullUserRedirect();
			return;
		}
		init();
	}

	private void init() throws SQLException {
		if (null == getUser()) {
			logger.error("init: user is null!");
		}

		initScoreEntry();
		initBucket();
		initializeImagesInstance();
	}

	public void initScoreEntry() throws SQLException {
		this.setEntry(getLearnweb().getScoreManager().getScoreEntryByUser(
				getUser()));
	}

	public void initBucket() throws SQLException {
		this.bucket = getLearnweb().getBucketManager().getBucket(
				getUser().getId());
	}

	private void initConstants() {
		if (!ImagesBean.initialized) {
			ImagesBean.HP_PENALTY = Integer.parseInt(getLearnweb()
					.getProperties().getProperty("HP_PENALTY"));
			logger.info("INIT: HP_PENALTY = " + ImagesBean.HP_PENALTY);
			ImagesBean.ALLOW_DUPLICATE_PERSONS = Boolean
					.parseBoolean(getLearnweb().getProperties().getProperty(
							"ALLOW_DUPLICATE_PERSONS"));
			logger.info("INIT: ALLOW_DUPLICATE_PERSONS = "
					+ ImagesBean.ALLOW_DUPLICATE_PERSONS);
			ImagesBean.IMMEDIATE_FEEDBACK = Boolean.parseBoolean(getLearnweb()
					.getProperties().getProperty("IMMEDIATE_FEEDBACK"));
			logger.info("INIT: IMMEDIATE_FEEDBACK = "
					+ ImagesBean.IMMEDIATE_FEEDBACK);
			ImagesBean.NUM_POOLS = Integer.parseInt(getLearnweb()
					.getProperties().getProperty("NUM_POOLS"));
			logger.info("INIT: NUM_POOLS = " + ImagesBean.NUM_POOLS);
			ImagesBean.NUM_CHOICES = Integer.parseInt(getLearnweb()
					.getProperties().getProperty("NUM_CHOICES"));
			logger.info("INIT: NUM_CHOICES = " + ImagesBean.NUM_CHOICES);
			ImagesBean.GROUPSCORE_MAX_TRIALS = Integer.parseInt(getLearnweb()
					.getProperties().getProperty("GROUPSCORE_MAX_TRIALS"));
			logger.info("INIT: GROUPSCORE_MAX_TRIALS = "
					+ ImagesBean.GROUPSCORE_MAX_TRIALS);
			ImagesBean.GROUPSCORE_BACKOFF = Integer.parseInt(getLearnweb()
					.getProperties().getProperty("GROUPSCORE_BACKOFF"));
			logger.info("INIT: GROUPSCORE_BACKOFF = "
					+ ImagesBean.GROUPSCORE_BACKOFF);
			try {
				ImagesBean.useGroups = !getLearnweb().getProperties()
						.getProperty("group_mode").equals("NONE");
			} catch (NullPointerException e) {
				logger.error("error retrieving parameter group_mode");
			}
			logger.info("INIT: useGroups = " + ImagesBean.useGroups);

			ImagesBean.initialized = true;
		}
	}

	public void initializeImagesInstance() {
		logger.debug("init images instance");
		if (null != this.imagesInstance) {
			this.lastPerson = this.imagesInstance.getEntityId();
		}
		this.imagesInstance = new ImagesInstance();
		// ref and correct image pair will be drawn from this
		List<Image> targetImgs = null;
		// person we are looking for
		int targetPerson;
		if (this.bucket.getHoneyPotIndex() == this.bucket.getProgress()) {
			this.imagesInstance.setHoneyPot(true);
			// honeypot
			targetPerson = this.bucket.getHoneyPotPerson();
			try {
				Entity p = getLearnweb().getEntityManager().getEntity(
						targetPerson);
				targetImgs = new ArrayList<Image>(p.getHoneyPot());
			} catch (SQLException e) {
				logger.error(
						"Error while retrieving honeypot images for bucket "
								+ this.bucket.toString(), e);
			}
		} else {
			do {
				targetPerson = generator.nextInt(ImagesBean.NUM_POOLS) + 1;
			} while (targetPerson == this.lastPerson);
			try {
				Entity p = getLearnweb().getEntityManager().getEntity(
						targetPerson);
				targetImgs = new ArrayList<Image>(p.getImages());
			} catch (SQLException e) {
				logger.error("Error while retrieving images for bucket "
						+ this.bucket.toString() + " and person "
						+ targetPerson, e);
			}
		}
		logger.debug("initImagesInstance: target entity: " + targetPerson);
		this.imagesInstance.setEntityId(targetPerson);
		this.imagesInstance.setRefImage(targetImgs.remove(generator
				.nextInt(targetImgs.size())));
		this.imagesInstance.setCorrectImage(targetImgs.remove(generator
				.nextInt(targetImgs.size())));
		// get 9 other persons, specify order and retrieve an image for each
		int targetPosition = generator.nextInt(ImagesBean.NUM_CHOICES);
		List<Integer> persons = new ArrayList<Integer>(
				ImagesBean.NUM_CHOICES - 1);
		for (int i = 0; i < ImagesBean.NUM_CHOICES - 1; i++) {
			Integer newP;
			do {
				newP = generator.nextInt(ImagesBean.NUM_POOLS) + 1;
				if (ImagesBean.ALLOW_DUPLICATE_PERSONS)
					break;
			} while (!(newP != targetPerson && !persons.contains(newP)));
			persons.add(newP);
		}
		logger.debug("got persons. Now generating list of images.");
		for (int i = 0; i < ImagesBean.NUM_CHOICES; i++) {
			logger.debug("adding image at position " + (i + 1));
			if (i == targetPosition) {
				this.imagesInstance.getImages().add(
						this.imagesInstance.getCorrectImage());
			} else {
				try {
					Entity p = getLearnweb().getEntityManager().getEntity(
							persons.remove(0));
					logger.debug("person " + p.getId() + " (" + p.getName()
							+ ") has " + p.getImages().size() + " images.");
					int index = generator.nextInt(p.getImages().size());
					logger.debug("get image at index " + index);
					Image img = p.getImages().get(index);
					logger.debug("choose image " + img.getId());
					this.imagesInstance.getImages().add(img);

				} catch (SQLException e) {
					logger.error("Error retrieving person", e);
				}
			}
		}
		logger.debug("images instance initialized");
	}

	public String onCheck() throws SQLException {
		initBucket();
		// check answers (we assume correct answer always and then penalize at
		// the end of bucket
		boolean scoreChanged = false;
		boolean wasBonus = false;
		int pointsDifference = 0;
		getFacesContext().getExternalContext().getFlash().setKeepMessages(true);

		// update honeypot instance information
		if (this.imagesInstance.isHoneyPot()) {
			if (this.imagesInstance.isCorrect()) {
				this.bucket.setHoneyPotResult(true);
			}
			try {
				getLearnweb().getHoneypotInstanceManager()
						.saveHoneypotInstance(getUser().getId(),
								this.imagesInstance);
			} catch (SQLException e) {
				logger.error("Error saving honeypotInstance for user "
						+ getUser().getId() + ": " + imagesInstance.toString(),
						e);
			}
		}

		if (ImagesBean.IMMEDIATE_FEEDBACK) {
			// SETTING NOT USED; CODE SEEMS TO BE WRONG
			int newPoints = 1;
			initScoreEntry();
			if (null != this.getEntry()) {
				this.getEntry()
						.setScore(this.getEntry().getScore() + newPoints);
				scoreChanged = true;
				pointsDifference = newPoints;
			}
			// output feedback for user
			infoGrowl("+ " + newPoints + " point"
					+ ((newPoints == 1) ? "" : "s") + "!");
		}

		// advance bucket
		if (this.bucket.isLastInstance()) {
			initScoreEntry();
			// penalize if honeypot wrong
			if (!bucket.getHoneyPotResult()) {
				int penalty = ImagesBean.HP_PENALTY;
				if (!ImagesBean.IMMEDIATE_FEEDBACK) {
					penalty -= ImagesBean.BUCKETBONUS;
				}

				if (null != this.getEntry()) {
					penalty = Math.min(this.getEntry().getScore(), penalty);
					pointsDifference = -penalty;
					int bonus = 0;
					if (getBonusBean() != null
							&& getBonusBean().isBonusActive()
							&& pointsDifference != 0) {
						bonus = getBonusBean().claimBonus(pointsDifference);
						if (bonus != 0)
							wasBonus = true;
					}
					pointsDifference += bonus;
					this.getEntry().setScore(
							this.getEntry().getScore() + pointsDifference);
					scoreChanged = true;
					penalty = -pointsDifference;
				}
				warnGrowl("Honeypot wrong!");
				if (penalty > 0) {
					infoGrowl("- " + penalty + " point"
							+ ((penalty == 1) ? "" : "s") + "!");
				}
			} else {
				infoGrowl("Congratulations! You have solved the test instance correctly.");
				if (!ImagesBean.IMMEDIATE_FEEDBACK) {
					pointsDifference = ImagesBean.BUCKETBONUS;
					int bonus = 0;
					if (getBonusBean() != null
							&& getBonusBean().isBonusActive()) {
						bonus = getBonusBean().claimBonus(pointsDifference);
						if (bonus > 0)
							wasBonus = true;
					}
					pointsDifference += bonus;
					if (null != this.getEntry()) {
						this.getEntry().setScore(
								this.getEntry().getScore() + pointsDifference);
						scoreChanged = true;
					}
					// output feedback for user
					infoGrowl("+ " + pointsDifference + " point"
							+ ((pointsDifference == 1) ? "" : "s") + "!");
				}
			}
		}

		// log input and state
		logImagesInput(getUser().getId(), this.imagesInstance, this.bucket,
				wasBonus);
		// advance bucket progress
		bucket.incProgress();
		// save bucket
		try {
			getLearnweb().getBucketManager().save(this.bucket);
		} catch (SQLException e) {
			logger.error("Error saving bucket " + bucket.toString(), e);
		}
		// save score
		if (scoreChanged) {
			incrementGroupScore(pointsDifference);
			saveScore();
		}
		// init instance
		initializeImagesInstance();

		log("images_submit");

		return getTemplateDir() + "/mt.jsf?faces-redirect=true";
	}

	/** display simple growl with SEVERITY_INFO and given text */
	private void infoGrowl(String text) {
		textGrowl(FacesMessage.SEVERITY_INFO, text);
	}

	private void warnGrowl(String text) {
		textGrowl(FacesMessage.SEVERITY_WARN, text);
	}

	private void textGrowl(Severity severity, String text) {
		getFacesContext()
				.addMessage(null, new FacesMessage(severity, text, ""));
	}

	private void saveScore() {
		if (null == this.getEntry())
			return;
		try {
			getLearnweb().getScoreManager().saveScore(getEntry());
		} catch (SQLException e) {
			logger.error("error while saving score ", e);
		}
	}

	public void incrementGroupScore(int increment) {
		if (!ImagesBean.useGroups)
			return;

		int trial = 1;
		boolean success = false;
		do {
			long startTime = System.currentTimeMillis();
			// get current groupId
			UserGroup userGroup = UtilBean.getGroupBean().getUserGroup();
			if (null == userGroup)
				break;
			int groupId = userGroup.getGroupId();
			if (trial > 1) {
				try {
					groupId = getLearnweb().getGroupManager()
							.getUserGroupEntry(getUser().getId()).getGroupId();
				} catch (SQLException e) {
					logger.error("error getting groupentry for user "
							+ getUser().getId(), e);
					continue;
				}
			}
			try {
				success = getLearnweb().getGroupScoreManager().incrementScore(
						groupId, increment);
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
		} while (trial < ImagesBean.GROUPSCORE_MAX_TRIALS);
	}

	public String getTimeToEndString() {
		long now = Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis();
		long start = getLearnweb().getEnd().getTimeInMillis();
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

	public ImagesInstance getImagesInstance() {
		return imagesInstance;
	}

	public void setImagesInstance(ImagesInstance imagesInstance) {
		this.imagesInstance = imagesInstance;
	}

	public Bucket getBucket() {
		if (null == this.bucket) {
			logger.info("bucket was null! call init.");
			try {
				init();
				refreshPage();
			} catch (SQLException e) {
			}
		}
		return this.bucket;
	}

	public int getNumChoices() {
		return ImagesBean.NUM_CHOICES;
	}

	public static boolean isUseGroups() {
		return useGroups;
	}

	public static void setUseGroups(boolean useGroups) {
		ImagesBean.useGroups = useGroups;
	}

	public ScoreEntry getEntry() {
		return entry;
	}

	public void setEntry(ScoreEntry entry) {
		this.entry = entry;
	}

	public BonusBean getBonusBean() {
		return bonusBean;
	}

	public void setBonusBean(BonusBean bonusBean) {
		this.bonusBean = bonusBean;
	}
}
