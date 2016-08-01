package de.l3s.mt.rest.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement(name = "userstatus")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserStatus extends XMLResponse {

	@XmlAttribute(name = "exists")
	boolean exists;

	@XmlAttribute(name = "score")
	int score;

	@XmlAttribute(name = "position")
	private int position;

	public boolean isExists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

}
