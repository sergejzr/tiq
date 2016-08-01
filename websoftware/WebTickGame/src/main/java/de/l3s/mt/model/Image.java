package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.HasId;
import de.l3s.util.MD5;

public class Image implements Serializable, HasId {
	private static final long serialVersionUID = 1L;
	private int id;
	private int entityId;
	private boolean honeyPot;
	private String url;

	public Image() {
		this.id = -1;
		this.setEntityId(-1);
	}

	public Image(ResultSet rs) throws SQLException {
		this.id = rs.getInt("i.image_id");
		this.entityId = rs.getInt("i.entity_id");
		this.honeyPot = rs.getBoolean("i.honey_pot");
	}

	@Override
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getEntityId() {
		return entityId;
	}

	public void setEntityId(int entityId) {
		this.entityId = entityId;
	}

	public String getUrl() {
		if (null == this.url) {
			this.url = Learnweb.getInstance().getProperties()
					.getProperty("IMAGE_URL")
					+ MD5.hash("" + this.id);
		}
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isHoneyPot() {
		return honeyPot;
	}

	public void setHoneyPot(boolean honeyPot) {
		this.honeyPot = honeyPot;
	}

}
