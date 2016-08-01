package de.l3s.mt.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.Bucket;
import de.l3s.mt.model.Image;
import de.l3s.mt.model.ImagesInstance;

@ManagedBean(eager = false)
@RequestScoped
public class ImgResultsBean extends ApplicationBean implements Serializable {
	private static final long serialVersionUID = 5L;
	private static int HP_PENALTY;
	private ImagesInstance honeypotInstance;
	private Bucket bucket;
	private static boolean initialized = false;
	private static Logger logger = Logger.getLogger(ImgResultsBean.class);
	private List<String> imgStyles;

	private ImagesBean imagesBean;

	public ImgResultsBean() throws SQLException {
		if (null == getUser()) {
			logger.info("User is null");
			return;
		}
		this.imagesBean = (ImagesBean) UtilBean.getManagedBean("imagesBean");
		if (null == this.imagesBean) {
			logger.error("Error: imagesBean was not injected.");
			return;
		}
		initConstants();
		this.imagesBean.initBucket();
		this.bucket = this.imagesBean.getBucket();
		if (null == this.bucket) {
			logger.error("Bucket is null!");
			return;
		}

		if (this.bucket.isFinished()) {
			initializeImagesInstance();
		}
		initializeImageBorderStyles();
	}

	private void initializeImageBorderStyles() {
		if (null == this.honeypotInstance)
			return;

		this.setImgStyles(new ArrayList<String>(10));
		for (Image i : this.honeypotInstance.getImages()) {
			if (i.getId() == this.honeypotInstance.getCorrectImage().getId()) {
				getImgStyles().add("border: 5px solid green;");
			} else if (i.getId() == this.honeypotInstance.getPickedImage()) {
				getImgStyles().add("border: 5px solid red;");
			} else {
				getImgStyles().add("");
			}
		}
	}

	private void initConstants() {
		if (!ImgResultsBean.initialized) {
			ImgResultsBean.HP_PENALTY = Integer.parseInt(getLearnweb()
					.getProperties().getProperty("HP_PENALTY"));
			logger.info("INIT: HP_PENALTY = " + ImgResultsBean.HP_PENALTY);
			ImgResultsBean.initialized = true;
		}
	}

	private void initializeImagesInstance() {
		logger.debug("init images instance");
		try {
			this.honeypotInstance = getLearnweb().getHoneypotInstanceManager()
					.getHoneypotInstanceByUser(getUser().getId());
		} catch (SQLException e) {
			logger.error("Could not retrieve honeypotImagesInstance for user "
					+ getUser().getId());
		}
	}

	public String onContinue() {
		log("images_res_continue");
		if (this.bucket.isFinished()) {
			this.bucket.reInit();
			try {
				getLearnweb().getBucketManager().save(this.bucket);
			} catch (SQLException e) {
				logger.error("Error saving bucket.", e);
			}
		}
		this.imagesBean.initializeImagesInstance();
		return getTemplateDir() + "/mt.jsf?faces-redirect=true";
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
			result += days + ((days != 1) ? " days, " : " day, ") + hours
					+ ((hours != 1) ? " hours" : " hour") + " and " + minutes
					+ ((minutes != 1) ? " minutes" : " minute");
		} else {
			result += seconds + ((seconds != 1) ? "seconds" : "second");
		}
		return result;
	}

	public ImagesInstance getHoneypotInstance() {
		return honeypotInstance;
	}

	public void setHoneypotInstance(ImagesInstance honeypotInstance) {
		this.honeypotInstance = honeypotInstance;
	}

	public List<String> getImgStyles() {
		return imgStyles;
	}

	public void setImgStyles(List<String> imgStyles) {
		this.imgStyles = imgStyles;
	}

}
