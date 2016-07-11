package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.HasId;

public class Entity implements Serializable, HasId {
	private static final long serialVersionUID = 4820518424799242305L;

	// db fields
	private int id;
	private String name;
	// helper fields
	private List<Image> images;
	private List<Image> honeyPot;

	public Entity() {
		this.id = -1;
	}

	public Entity(ResultSet rs) throws SQLException {
		this.id = rs.getInt("e.entity_id");
		this.name = rs.getString("e.name");
	}

	@Override
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Image> getImages() throws SQLException {
		if (null == this.images) {
			this.images = Learnweb.getInstance().getImageManager()
					.getImagesByEntity(this.id, false);
		}
		return this.images;
	}

	public void setImages(List<Image> images) {
		this.images = images;
	}

	public List<Image> getHoneyPot() throws SQLException {
		if (null == this.honeyPot) {
			this.honeyPot = Learnweb.getInstance().getImageManager()
					.getImagesByEntity(this.id, true);
		}
		return this.honeyPot;
	}

	public void setHoneyPot(List<Image> honeyPot) {
		this.honeyPot = honeyPot;
	}

}
