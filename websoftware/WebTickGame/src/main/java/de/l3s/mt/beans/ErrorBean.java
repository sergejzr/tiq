package de.l3s.mt.beans;

import java.io.Serializable;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.ViewExpiredException;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.learnwebBeans.UserBean;

@ManagedBean
@ApplicationScoped
public class ErrorBean extends ApplicationBean implements Serializable {
	private static final long serialVersionUID = 2L;
	Logger logger = Logger.getLogger(ErrorBean.class);

	public ErrorBean() {
		logger.info("ErrorBean constructed");
	}

	public void hit() {
		logger.info("error page displayed. invalidating session.");
		log("error-page-display");

		UserBean uBean = UtilBean.getUserBean();
		if (null != uBean) {
			uBean.setUser(null);
		}
		FacesContext.getCurrentInstance().getExternalContext()
				.invalidateSession();
	}

	public void handleError() {
		Throwable t = getThrowable();
		if (t instanceof ViewExpiredException) {
			logger.info("View was expired");
			getFacesContext().addMessage(
					"message",
					new FacesMessage(FacesMessage.SEVERITY_INFO,
							"View expired. Please log in again.", ""));
			return;
		} else {
			logger.error(t.getLocalizedMessage(), t);
		}
		if (null == getLearnweb()) {
			addMessage("Could not start application");
		} else {
			addMessage("There was a problem with the application.");
		}
	}

	private static void addMessage(String message) {
		FacesContext.getCurrentInstance().addMessage("message",
				new FacesMessage(FacesMessage.SEVERITY_FATAL, message, null));
	}

	private Throwable getThrowable() {
		FacesContext context = FacesContext.getCurrentInstance();
		Map requestMap = context.getExternalContext().getRequestMap();
		Throwable ex = (Throwable) requestMap
				.get("javax.servlet.error.exception");
		logger.debug("Got throwable  " + ex.getClass().getCanonicalName()
				+ ", message '" + ex.getLocalizedMessage() + "'");
		return ex;
	}
}
