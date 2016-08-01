package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.l3s.learnweb.Learnweb;

public class ImagesInstance implements Serializable {
	private static final long serialVersionUID = 1L;
	private static int NUM_IMAGES = 10;

	// entity we are looking for
	private int entityId;
	// reference image for entity
	private Image refImage;
	// correct image among the 10
	private Image correctImage;
	private int pickedImage;
	private boolean isHoneyPot;
	// list of images to display for the user to choose from
	private List<Image> images;

	public ImagesInstance() {
		images = new ArrayList<Image>(NUM_IMAGES);
	}

	/** constructor for use by honeypotInstanceManager */
	public ImagesInstance(ResultSet rs) throws SQLException {
		this.entityId = rs.getInt("hi.entity_id");
		this.refImage = Learnweb.getInstance().getImageManager()
				.getImage(rs.getInt("hi.ref_img"));
		this.correctImage = Learnweb.getInstance().getImageManager()
				.getImage(rs.getInt("hi.correct_img"));
		this.pickedImage = rs.getInt("hi.picked_img");
		this.isHoneyPot = true;
		this.images = new ArrayList<Image>();
		for (String str : rs.getString("hi.images").split(",")) {
			int imgId = Integer.parseInt(str);
			this.images.add(Learnweb.getInstance().getImageManager()
					.getImage(imgId));
		}
	}

	public int getEntityId() {
		return entityId;
	}

	public void setEntityId(int entityId) {
		this.entityId = entityId;
	}

	public boolean isHoneyPot() {
		return isHoneyPot;
	}

	public void setHoneyPot(boolean isHoneyPot) {
		this.isHoneyPot = isHoneyPot;
	}

	public Image getRefImage() {
		return refImage;
	}

	public void setRefImage(Image refImage) {
		this.refImage = refImage;
	}

	public Image getCorrectImage() {
		return correctImage;
	}

	public void setCorrectImage(Image correctImage) {
		this.correctImage = correctImage;
	}

	public List<Image> getImages() {
		return images;
	}

	public void setImages(List<Image> images) {
		this.images = images;
	}

	public String getImagesString() {
		StringBuilder iSB = new StringBuilder();
		for (Image img : this.getImages()) {
			iSB.append(img.getId() + ",");
		}
		iSB.deleteCharAt(iSB.length() - 1);
		return iSB.toString();
	}

	public int getPickedImage() {
		return pickedImage;
	}

	public void setPickedImage(int pickedImage) {
		this.pickedImage = pickedImage;
	}

	public boolean isCorrect() {
		return this.correctImage.getId() == this.pickedImage;
	}

	@Override
	public String toString() {
		return "ImagesInstance [entityId=" + entityId + ", refImage="
				+ refImage + ", correctImage=" + correctImage
				+ ", pickedImage=" + pickedImage + ", isHoneyPot=" + isHoneyPot
				+ ", images=" + images + "]";
	}
}
