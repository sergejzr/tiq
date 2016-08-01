package de.l3s.mt.model;

import java.io.Serializable;

import de.l3s.util.HasId;

public class UserComment implements Serializable, HasId {

	private static final long serialVersionUID = -6523154710921285091L;

	private int id;
	private int userId;
	private String text;
	private String instance;
	private int bucketNumber;
	private int progressIndex;

	public UserComment() {
		id = -1;
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public int getBucketNumber() {
		return bucketNumber;
	}

	public void setBucketNumber(int bucketNumber) {
		this.bucketNumber = bucketNumber;
	}

	public int getProgressIndex() {
		return progressIndex;
	}

	public void setProgressIndex(int progressIndex) {
		this.progressIndex = progressIndex;
	}
}
