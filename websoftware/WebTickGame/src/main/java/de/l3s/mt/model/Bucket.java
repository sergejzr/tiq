package de.l3s.mt.model;

import java.io.Serializable;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.HasId;

public class Bucket implements Serializable, HasId {
	private static final long serialVersionUID = 5L;
	private static Logger logger = Logger.getLogger(Bucket.class);

	private static int NUM_POOLS;
	private static int BUCKET_SIZE = 100;
	private static Random generator;
	private static boolean initialized = false;

	// user_id
	private int id;
	private int bucketNumber;
	private int progress;
	private int honeyPotIndex = -1;
	private int honeyPotPerson = -1;
	private boolean honeyPotResult;

	private Bucket() {
		init();
	}

	/** create first Bucket for a user */
	public Bucket(int userId) {
		this();
		this.id = userId;
		this.bucketNumber = 1;
		this.progress = 1;
		initHoneyPot();
	}

	public Bucket(ResultSet rs) throws SQLException {
		this();
		this.id = rs.getInt("b.user_id");
		this.bucketNumber = rs.getInt("b.bucket_number");
		this.progress = rs.getInt("b.progress");
		this.honeyPotIndex = rs.getInt("b.honey_pot_index");
		this.honeyPotPerson = rs.getInt("b.honey_pot_person");
		this.honeyPotResult = rs.getBoolean("b.honey_pot_result");
	}

	/** randomly initializes honeypot values */
	private void initHoneyPot() {
		// decide on position of honeypot
		this.honeyPotIndex = Bucket.getGenerator().nextInt(Bucket.BUCKET_SIZE) + 1;

		// pick person for honeypot
		int lastPerson = this.honeyPotPerson;
		do {
			// assume person ids run from 1 to NUM_POOLS
			this.honeyPotPerson = Bucket.getGenerator().nextInt(
					Bucket.NUM_POOLS) + 1;
		} while (this.honeyPotPerson == lastPerson);

		this.honeyPotResult = false;
	}

	private void init() {
		if (!Bucket.initialized) {
			try {
				Bucket.NUM_POOLS = Integer.parseInt(Learnweb.getInstance()
						.getProperties().getProperty("NUM_POOLS"));
				logger.info("INIT: set NUM_POOLS to " + Bucket.NUM_POOLS);
			} catch (NumberFormatException e) {
				logger.error("INIT: Error retrieving property NUM_POOLS", e);
			}
			try {
				Bucket.BUCKET_SIZE = Integer.parseInt(Learnweb.getInstance()
						.getProperties().getProperty("BUCKET_SIZE"));
				logger.info("INIT: set BUCKET_SIZE to " + Bucket.BUCKET_SIZE);
			} catch (NumberFormatException e) {
				logger.error("INIT: Error retrieving property BUCKET_SIZE", e);
			}
			Bucket.getGenerator();
			Bucket.initialized = true;
		}
	}

	private static Random getGenerator() {
		if (null == Bucket.generator) {
			Bucket.generator = new SecureRandom();
		}
		return Bucket.generator;
	}

	/** increments progress by 1 */
	public void incProgress() {
		logger.debug("incrementing progress");
		this.progress += 1;
	}

	/**
	 * indicates whether current instance is the last (bucket needs to be
	 * initialized instead of incrementing the progress)
	 */
	public boolean isLastInstance() {
		return this.progress == Bucket.BUCKET_SIZE;
	}

	/**
	 * Indicates whether last instance was solved but bucket was not
	 * reInitialized yet.
	 */
	public boolean isFinished() {
		return this.progress == Bucket.BUCKET_SIZE + 1;
	}

	/** reinitializes bucket after it was completed */
	public void reInit() {
		this.bucketNumber += 1;
		this.progress = 1;
		initHoneyPot();
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getBucketNumber() {
		return bucketNumber;
	}

	public void setBucketNumber(int bucketNumber) {
		this.bucketNumber = bucketNumber;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public int getHoneyPotIndex() {
		return honeyPotIndex;
	}

	public void setHoneyPotIndex(int honeyPotIndex) {
		this.honeyPotIndex = honeyPotIndex;
	}

	public int getHoneyPotPerson() {
		return honeyPotPerson;
	}

	public void setHoneyPotPerson(int honeyPotPerson) {
		this.honeyPotPerson = honeyPotPerson;
	}

	public boolean getHoneyPotResult() {
		return honeyPotResult;
	}

	public void setHoneyPotResult(boolean honeyPotResult) {
		this.honeyPotResult = honeyPotResult;
	}

	public int getBucketSize() {
		return Bucket.BUCKET_SIZE;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		String SEPERATOR = ", ";

		string.append(this.getClass().getName() + " Object {");
		string.append("userId: " + getId() + SEPERATOR);
		string.append("bucketNumber:" + getBucketNumber() + SEPERATOR);
		string.append("progress:" + getProgress() + SEPERATOR);
		string.append("honeyPotIndex" + getHoneyPotIndex() + SEPERATOR);
		string.append("honeyPotPerson" + getHoneyPotPerson() + SEPERATOR);
		string.append("honeyPotResult" + getHoneyPotResult() + SEPERATOR);
		string.append("}");
		return string.toString();
	}
}
