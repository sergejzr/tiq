package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.User;

@ManagedBean
@RequestScoped
public class UnsubscribeBean extends ApplicationBean implements Serializable {
	private static Logger logger = Logger.getLogger(UnsubscribeBean.class);
	private static final long serialVersionUID = 0L;

	private String parameter;
	private User user = null;

	public UnsubscribeBean() throws SQLException {
		UnsubscribeBean.logger.debug("construct unsubscribeBean");
		if (parameter == null || parameter.equals(""))
			parameter = getFacesContext().getExternalContext()
					.getRequestParameterMap().get("u");

		if (parameter == null) {
			addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
			return;
		}

		String[] splits = parameter.split("_");
		if (splits.length != 2) {
			addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
			return;
		}
		int userId = Integer.parseInt(splits[0]);
		String hash = splits[1];

		user = getLearnweb().getUserManager().getUser(userId);
		log("unsubscribe");
		if (null == user || !hash.equals(PasswordBean.createHash(user))) {
			addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
			return;
		}

		this.user.setMailNotifications(false);
		getLearnweb().getUserManager().save(this.user);
		UnsubscribeBean.logger.info("unsubscribed user:" + user.getUsername()
				+ " id " + user.getId());
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public User getUser() {
		return this.user;
	}
}