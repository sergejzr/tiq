package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotBlank;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@RequestScoped
public class LoginBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = 7980062591522267111L;
	@NotBlank
	private String username;
	@NotBlank
	private String password;

	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;

	private static Logger logger = Logger.getLogger(LoginBean.class);

	public LoginBean() {
		log("display login");
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String name) {
		this.username = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String login() throws SQLException {
		final User user = getLearnweb().getUserManager().getUser(username,
				password);

		if (null == user) {
			addMessage(FacesMessage.SEVERITY_ERROR,
					"wrong_username_or_password");
			return null;
		}

		login(user);
		// get groupBean to trigger growl notifications
		try {
			UtilBean.getManagedBean("groupBean");
		} catch (Throwable t) {
			logger.error("error retrieving group bean", t);
		}
		/*getFacesContext().getExternalContext().getFlash().setKeepMessages(true);
		getFacesContext().addMessage(
				null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "groupbean:"
						+ (null == UtilBean.getManagedBean("groupBean")), ""));
		*/
		// check if user is active
		if (getLearnweb().getPlanB() && !user.isActive()
				&& !getLearnweb().isAfterEnd()) {
			return getTemplateDir() + "/new.jsf?faces-redirect=true";
		}
		// if the user logs in from the start or the login page, redirect him to
		// the welcome page
		String viewId = getFacesContext().getViewRoot().getViewId();
		if (viewId.endsWith("/user/login.xhtml")
				|| viewId.endsWith("/index.xhtml")) {
			String page = "mt.jsf";
			return getTemplateDir() + "/" + page + "?faces-redirect=true";
		}

		// otherwise reload his last page
		return viewId + "?faces-redirect=true&includeViewParams=true";
	}

	public String logout() {
		// setKeepMessages();

		log("logout");
		UserBean uBean = UtilBean.getUserBean();
		if (null != uBean) {
			uBean.setUser(null);
		}

		// addMessage(FacesMessage.SEVERITY_INFO, "logout_success");
		FacesContext.getCurrentInstance().getExternalContext()
				.invalidateSession();
		return getTemplateDir() + "/index.xhtml?faces-redirect=true";
	}

	/**
	 * Performs necessary operations to log in the given user.
	 * 
	 * @param user
	 */
	public void login(User user) {
		UtilBean.getUserBean().setUser(user);
		// logs the user in
		// addMessage(FacesMessage.SEVERITY_INFO, "welcome_username",
		// user.getUsername());
		log("login");

		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext()
				.getSession(false);
		String sessionId = session.getId();
		logger.debug("login user " + user.getId() + ", session " + sessionId);
		Learnweb.getInstance().getSessionMap().put(user.getId(), sessionId);
		// log timezone
		logTimeZone();
	}

	/**
	 * Log the current timezone at login.
	 */
	public void logTimeZone() {
		if (getUserBean() == null) {
			logger.error("logTimeZone(): userBean is null!");
			return;
		}
		if (getUserBean().getTimeZoneOffset() == null) {
			logger.error("logTimeZone(): timeZoneOffset is null!");
			return;
		}
		log("timezone-offset=" + getUserBean().getTimeZoneOffset());
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}
}
