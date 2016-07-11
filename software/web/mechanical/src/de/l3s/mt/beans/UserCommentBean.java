package de.l3s.mt.beans;

import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.UserComment;

@ManagedBean
@RequestScoped
public class UserCommentBean extends ApplicationBean {

	private UserComment comment;
	@ManagedProperty(value = "#{imagesBean}")
	private ImagesBean imagesBean;

	public UserCommentBean() {
		setComment(new UserComment());
		getComment().setUserId(getUser().getId());
	}

	public UserComment getComment() {
		return this.comment;
	}

	public void setComment(UserComment comment) {
		this.comment = comment;
	}

	public void onSubmit() throws SQLException {
		this.comment.setInstance(this.imagesBean.getImagesInstance()
				.getImagesString());
		this.comment.setBucketNumber(this.imagesBean.getBucket()
				.getBucketNumber());
		this.comment
				.setProgressIndex(this.imagesBean.getBucket().getProgress());
		getLearnweb().getUserCommentManager().save(this.comment);

		getFacesContext().addMessage(
				null,
				new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Thank you for your comment.", ""));
		log("user comment submit");
	}

	public ImagesBean getImagesBean() {
		return this.imagesBean;
	}

	public void setImagesBean(ImagesBean imagesBean) {
		this.imagesBean = imagesBean;
	}
}
