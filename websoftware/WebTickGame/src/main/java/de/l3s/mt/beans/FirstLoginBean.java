package de.l3s.mt.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class FirstLoginBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = 2L;
	private Logger logger = Logger.getLogger(FirstLoginBean.class);

	public String onProceed() throws Exception {
		// logging
		logger.debug("onProceed called");
		if (getLearnweb().getPlanB()) {
			return planB();
		} else {
			return planA();
		}
	}

	public String planA() throws Exception {
		User user = null;
		user = getLearnweb().getUserManager().getUser(getUser().getId());
		if (null == user)
			throw new SQLException("user is null");
		if (user.isActive()) {
			UtilBean.getUserBean().setUser(user);
			log("firstlogin_proceed_success");
			getFacesContext()
					.addMessage(
							null,
							new FacesMessage(
									FacesMessage.SEVERITY_INFO,
									"Your account has been successfully activated.",
									""));
			return getTemplateDir() + "/mt.jsf?faces-redirect=true";
		}
		getFacesContext()
				.addMessage(
						null,
						new FacesMessage(
								FacesMessage.SEVERITY_INFO,
								"Your account has not yet been activated. Please submit your token or try again later.",
								""));
		log("firstlogin_proceed_denied");
		return getTemplateDir() + "/new.jsf";
	}

	public String planB() {
		log("firstlogin_proceed_success");
		return getTemplateDir() + "/mt.jsf?faces-redirect=true";
	}
}
