package de.l3s.learnwebBeans;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean(eager = true)
@ApplicationScoped
public class LearnwebBean {

	private Learnweb learnweb;
	private LinkedList<LocaleContainer> supportedLocales = new LinkedList<LocaleContainer>();
	private String mechanicalUrl;

	private static Logger logger = Logger.getLogger(LearnwebBean.class);

	public LearnwebBean() throws IOException {
		// load supported languages. See WEB-INF/faces-config.xml
		Iterator<Locale> locales = FacesContext.getCurrentInstance()
				.getApplication().getSupportedLocales();
		while (locales.hasNext()) {
			supportedLocales.add(new LocaleContainer(locales.next()));
		}
		String contextUrl = null;
		/*
		ExternalContext ext = FacesContext.getCurrentInstance()
				.getExternalContext();

		if (ext.getRequestServerPort() == 80
				|| ext.getRequestServerPort() == 443)
			contextUrl = ext.getRequestScheme() + "://"
					+ ext.getRequestServerName() + ext.getRequestContextPath();
		else
			contextUrl = ext.getRequestScheme() + "://"
					+ ext.getRequestServerName() + ":"
					+ ext.getRequestServerPort() + ext.getRequestContextPath();
		*/
		try {
			learnweb = Learnweb.getInstance(contextUrl);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static void addMessage(String message) {
		FacesContext.getCurrentInstance().addMessage("message",
				new FacesMessage(FacesMessage.SEVERITY_FATAL, message, null));
	}

	/**
	 * 
	 * @return Returns the servername + contextpath. For the default
	 *         installation this is: http://learnweb.l3s.uni-hannover.de
	 */
	public String getContextUrl() {
		if (null == learnweb) {
			logger.error("getContextUrl: learnweb is null");
			return "";
		}
		return learnweb.getContextUrl(); // because we don't use httpS we can
											// cache the url,
		// change it if you want to use httpS too
	}

	/**
	 * 
	 * @return example for a local installation: http://localhost:8080/jlw/lw/
	 */
	public String getBaseUrl() {
		ExternalContext ext = FacesContext.getCurrentInstance()
				.getExternalContext();

		String path = ext.getRequestServletPath();
		path = path.substring(0, path.indexOf("/", 1) + 1);

		return getContextUrl() + path;
	}

	public Learnweb getLearnweb() {
		if (null == learnweb) {
			UtilBean.redirect(getBaseUrl() + "error.jsf");
		}
		return learnweb;
	}

	public void addGlobalErrorMessage() {
		if (null == learnweb) {
			addMessage("Could not start application");
		} else {
			addMessage("There was a problem with the application.");
		}

	}

	@PreDestroy
	public void onDestroy() {
		learnweb.onDestroy();
	}

	public LinkedList<LocaleContainer> getSupportedLocales() {
		return supportedLocales;
	}

	public class LocaleContainer {
		private Locale locale;
		private String countryCode;
		private String languageName;

		public LocaleContainer(Locale locale) {
			this.locale = locale;
			this.countryCode = locale.getCountry().toLowerCase();
			this.languageName = locale.getDisplayLanguage(locale);
		}

		public Locale getLocale() {
			return locale;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public String getLanguageName() {
			return languageName;
		}
	}

	/**
	 * get a message from the message property files depending on the currently
	 * used local
	 * 
	 * @param msgKey
	 * @param args
	 * @return
	 */
	public String getLocaleMessage(String msgKey) {
		return UtilBean.getLocaleMessage(msgKey);
	}

	public String getMechanicalUrl() {
		if (null == mechanicalUrl) {
			mechanicalUrl = getLearnweb().getProperties().getProperty("MT_URL");
			logger.info("mechanical url set to: '" + mechanicalUrl + "'");
		}
		return mechanicalUrl;
	}
}
