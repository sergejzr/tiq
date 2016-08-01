package de.l3s.learnwebBeans;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.mt.model.Bucket;
import de.l3s.mt.model.ImagesInstance;

public class ApplicationBean {
	private transient Learnweb learnweb;
	private transient String sessionId;
	private long startTime;
	private static Logger logger = Logger.getLogger(ApplicationBean.class);

	public ApplicationBean() {
		/*
		 * if(isAjaxRequest()) return;
		 * 
		 * FacesContext facesContext = getFacesContext(); ExternalContext ext =
		 * facesContext.getExternalContext(); HttpServletRequest servletRequest
		 * = (HttpServletRequest) ext.getRequest(); UIViewRoot viewRoot =
		 * facesContext.getViewRoot();
		 * 
		 * if(null == viewRoot)
		 * System.err.println("ApplicationBean::viewRoot is null"); else {
		 * String request = viewRoot.getViewId(); String ip =
		 * servletRequest.getRemoteAddr();
		 * 
		 * System.out.println(request +" - "+ ip); }
		 */
		startTime = System.currentTimeMillis();
	}

	public String getSessionId() {
		if (null == sessionId) {
			HttpSession session = (HttpSession) getFacesContext()
					.getExternalContext().getSession(true);
			sessionId = session.getId();
		}
		return sessionId;
	}

	protected boolean isAjaxRequest() {
		if (null == FacesContext.getCurrentInstance())
			return false;

		return FacesContext.getCurrentInstance().isPostback();
	}

	protected FacesContext getFacesContext() {
		return FacesContext.getCurrentInstance();
	}

	/**
	 * Returns the http get paramater or null if not found
	 * 
	 * @param param
	 * @return
	 */
	protected String getParameter(String param) {
		String value = getFacesContext().getExternalContext()
				.getRequestParameterMap().get(param);

		if (null == value)
			return null;

		byte ptext[] = value.getBytes();
		String v2 = "fehler";
		try {
			v2 = new String(ptext, "UTF-8");// +" Kra Ðong";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		System.out.println(param + ": " + value + " oder " + v2);

		return v2;
	}

	/**
	 * Returns the http get parameter as int. Is null if not found or couldn't
	 * be parsed
	 * 
	 * @param param
	 * @return
	 */
	protected Integer getParameterInt(String param) {
		String value = getParameter(param);

		if (null == value)
			return null;

		Integer intValue;
		try {
			intValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			intValue = null;
		}

		return intValue;
	}

	/**
	 * Returns the currently logged in user.
	 * 
	 * @return null if not logged in
	 */
	protected User getUser() {
		return UtilBean.getUserBean().getUser();
	}

	protected Learnweb getLearnweb() {
		if (null == learnweb)
			learnweb = UtilBean.getLearnwebBean().getLearnweb();
		return learnweb;
	}

	/**
	 * get a message from the message property files depending on the currently
	 * used local
	 * 
	 * @param msgKey
	 * @param args
	 * @return
	 */
	public String getLocaleMessage(String msgKey, Object... args) {
		return UtilBean.getLocaleMessage(msgKey, args);
	}

	protected FacesMessage getFacesMessage(FacesMessage.Severity severity,
			String msgKey, Object... args) {
		return new FacesMessage(severity, getLocaleMessage(msgKey, args), null);
	}

	/**
	 * Call this method if you want to keep messages during a post-redirect-get. <br/>
	 * This value determines whether or not any FacesMessage instances queued in
	 * the current FacesContext must be preserved so they are accessible on the
	 * next traversal of the lifecycle on this session, regardless of the
	 * request being a redirect after post, or a normal postback.
	 */
	public void setKeepMessages() {
		getFacesContext().getExternalContext().getFlash().setKeepMessages(true);
	}

	/**
	 * adds a global message to the jsf context. this will be displayed by the
	 * p:messages component
	 * 
	 * @param severity
	 * @param msgKey
	 * @param args
	 */
	protected void addMessage(FacesMessage.Severity severity, String msgKey,
			Object... args) {
		getFacesContext()
				.addMessage(
						"message",
						new FacesMessage(severity, getLocaleMessage(msgKey,
								args), null));
	}

	/**
	 * adds a global message to the jsf context. this will be displayed for a
	 * minute by the p:growl component
	 * 
	 * @param severity
	 * @param msgKey
	 * @param args
	 */
	protected void addGrowl(FacesMessage.Severity severity, String msgKey,
			Object... args) {
		getFacesContext()
				.addMessage(
						null,
						new FacesMessage(severity, getLocaleMessage(msgKey,
								args), null));
	}

	/**
	 * returns the currently used template directory. By default this is "lw/"
	 * 
	 * @return
	 */
	protected String getTemplateDir() {
		String path = getFacesContext().getExternalContext()
				.getRequestServletPath();
		return path.substring(0, path.indexOf("/", 1));
	}

	/**
	 * retrieves an object that was previously set by setPreference()
	 * 
	 * @param key
	 * @return
	 */
	public Object getPreference(String key) {
		return UtilBean.getUserBean().getPreference(key);
	}

	/**
	 * returns defaultValue if no correspondig value is found for the key
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Object getPreference(String key, Object defaultValue) {
		Object obj = getPreference(key);
		return obj == null ? defaultValue : obj;
	}

	/**
	 * Stores an object in the session
	 * 
	 * @param key
	 * @param value
	 */
	public void setPreference(String key, Object value) {
		UtilBean.getUserBean().setPreference(key, value);
	}

	protected void addFatalMessage(Throwable e) {
		addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		e.printStackTrace();
	}

	protected void logImagesInput(int userId, ImagesInstance instance,
			Bucket bucket, boolean wasBonus) {

		try {
			getLearnweb().getLogManager().logImageInput(userId,
					instance.getImagesString(), instance.getEntityId(),
					instance.getRefImage().getId(),
					instance.getCorrectImage().getId(),
					instance.getPickedImage(), instance.isHoneyPot(),
					bucket.getBucketNumber(), bucket.getProgress(),
					getSessionId(), getRequestIp(), wasBonus);
		} catch (SQLException e) {
			logger.error("Error logging", e);
		}
	}

	protected String getRequestIp() {
		String ip = "";
		try {
			HttpServletRequest req = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			ip = req.getRemoteAddr();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ip;
	}

	protected void log(String action) {
		int executionTime = (int) (System.currentTimeMillis() - startTime);
		String ip = "";
		String userAgent = "";
		try {
			HttpServletRequest req = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			ip = req.getRemoteAddr();
			userAgent = req.getHeader("user-agent");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			getLearnweb().getLogManager().logUserInput(getUser(), action,
					getSessionId(), ip, userAgent, executionTime);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void nullUserRedirect() {
		logger.info("User is null, redirect to index");
		FacesContext.getCurrentInstance().getExternalContext()
				.invalidateSession();
		log("null-user-redirect");
		UtilBean.redirect(getLearnweb().getContextUrl()
				+ "/mt/index.jsf?faces-redirect=true");
	}

	protected void refreshPage() {
		logger.info("refresh page");
		FacesContext fc = getFacesContext();
		String refreshpage = fc.getViewRoot().getViewId();
		ViewHandler ViewH = fc.getApplication().getViewHandler();
		UIViewRoot UIV = ViewH.createView(fc, refreshpage);
		UIV.setViewId(refreshpage);
		fc.setViewRoot(UIV);
	}
}
