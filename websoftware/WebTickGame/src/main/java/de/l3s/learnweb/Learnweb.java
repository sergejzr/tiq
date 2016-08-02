package de.l3s.learnweb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import de.l3s.mt.sql.ApplicationProperties;
import de.l3s.mt.sql.BonusEventManager;
import de.l3s.mt.sql.BucketManager;
import de.l3s.mt.sql.EntityManager;
import de.l3s.mt.sql.GroupManager;
import de.l3s.mt.sql.GroupMessageManager;
import de.l3s.mt.sql.GroupScoreManager;
import de.l3s.mt.sql.HoneypotInstanceManager;
import de.l3s.mt.sql.ImageManager;
import de.l3s.mt.sql.InvitationManager;
import de.l3s.mt.sql.LogManager;
import de.l3s.mt.sql.ScoreManager;
import de.l3s.mt.sql.TicketManager;
import de.l3s.mt.sql.UserCommentManager;
import de.l3s.mt.sql.UserManager;
import de.l3s.mt.sql.UserManagerInterface;

@ManagedBean
@ApplicationScoped
public class Learnweb extends Observable {
	public final static String salt1 = "ff4a9ff19306ee0407cf69d592";
	public final static String salt2 = "3a129713cc1b33650816d61450";

	private Connection dbConnection;

	private Properties properties;
	private String contextUrl;

	// Manager (Data Access Objects):
	private UserManagerInterface userManager;
	private LogManager logManager;
	private UserCommentManager userCommentManager;
	private ScoreManager scoreManager;
	private TicketManager ticketManager;
	private EntityManager entityManager;
	private ImageManager imageManager;
	private BucketManager bucketManager;
	private HoneypotInstanceManager honeypotInstanceManager;
	private GroupManager groupManager;
	private GroupScoreManager groupScoreManager;
	private GroupMessageManager groupMessageManager;
	private ApplicationProperties applicationProperties;
	private InvitationManager invitationManager;
	private BonusEventManager bonusEventManager;

	// table prefix
	private String tablePrefix;
	private String userTablePrefix;
	private Calendar start;
	private Calendar end;
	private Calendar endGroupFormation;
	private String cFApiKey;
	private Boolean planB;

	private static Learnweb learnweb = null;
	private static final Logger logger = Logger.getLogger(Learnweb.class);

	private HashMap<Integer, String> sessionMap;

	public static Learnweb getInstance(String contextUrl)
			throws ClassNotFoundException, SQLException, FileNotFoundException,
			IOException {
		if (learnweb == null) {
			learnweb = new Learnweb(contextUrl);
		}
		return learnweb;
	}

	/**
	 * make sure that Learnweb.getInstance(String contextUrl) was called before
	 */
	public static Learnweb getInstance() {
		try {
			return getInstance("http://learnweb.l3s.uni-hannover.de");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param contextUrl
	 *            The servername + contextpath. For the default installation
	 *            this is: http://learnweb.l3s.uni-hannover.de
	 * 
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private Learnweb(String contextUrl) throws ClassNotFoundException,
			SQLException, FileNotFoundException, IOException {
		this.contextUrl = contextUrl;

		try {
			PropertyConfigurator.configure(new Properties() {
				private static final long serialVersionUID = 3522475775881727293L;
				{
					load(getClass().getClassLoader().getResourceAsStream(
							"de/l3s/learnweb/config/log4j.properties"));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.properties = new Properties();
		String propertiesFileName = "mechanical";
		try {
		

			if ((new File("/home/markus")).exists()) // don't change this. Add
														// an elseif statement,
														// if you want to use
														// another properties
														// file
				propertiesFileName = "mt_local_markus";

			InputStream loader = getClass().getClassLoader().getResourceAsStream(
					"de/l3s/learnweb/config/" + propertiesFileName
					+ ".properties");
			
			properties.load(getClass().getClassLoader().getResourceAsStream(
					"de/l3s/learnweb/config/" + propertiesFileName
							+ ".properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// define start and end time
		initTimes();
		// connect db
		Class.forName("com.mysql.jdbc.Driver");
		connect();

		// init DAOs etc
		this.tablePrefix = properties.getProperty("table_prefix");
		this.userTablePrefix = properties.getProperty("user_table_prefix");
		this.cFApiKey = properties.getProperty("cf_api_key");
		this.planB = Boolean
				.parseBoolean(this.properties.getProperty("plan_b"));
		this.userManager = new UserManager(this);
		this.logManager = new LogManager(this);
		this.userCommentManager = new UserCommentManager(this);
		this.scoreManager = new ScoreManager(this);
		this.ticketManager = new TicketManager(this);
		this.bucketManager = new BucketManager(this);
		this.entityManager = new EntityManager(this);
		this.honeypotInstanceManager = new HoneypotInstanceManager(this);
		this.imageManager = new ImageManager(this);
		this.groupManager = new GroupManager(this);
		this.groupMessageManager = new GroupMessageManager(this);
		this.groupScoreManager = new GroupScoreManager(this);
		this.invitationManager = new InvitationManager(this);
		this.bonusEventManager = new BonusEventManager(this);
		this.sessionMap = new HashMap<Integer, String>();
		this.applicationProperties = new ApplicationProperties(this);
	}

	private void initTimes() {
		try {
			logger.info("Init start date");
			int sYear = Integer.parseInt(properties.getProperty("S_YEAR"));
			int sMonth = Integer.parseInt(properties.getProperty("S_MONTH"));
			int sDay = Integer.parseInt(properties.getProperty("S_DAY"));
			int sHour = Integer.parseInt(properties.getProperty("S_HOUR"));
			int sMinute = Integer.parseInt(properties.getProperty("S_MINUTE"));
			Calendar cal = GregorianCalendar.getInstance(TimeZone
					.getTimeZone("Europe/Berlin"));
			cal.clear();
			cal.set(sYear, sMonth - 1, sDay, sHour, sMinute);
			this.start = cal;
			logger.info("start date is " + cal.toString());

			logger.info("init end date");
			int eYear = Integer.parseInt(properties.getProperty("E_YEAR"));
			int eMonth = Integer.parseInt(properties.getProperty("E_MONTH"));
			int eDay = Integer.parseInt(properties.getProperty("E_DAY"));
			int eHour = Integer.parseInt(properties.getProperty("E_HOUR"));
			int eMinute = Integer.parseInt(properties.getProperty("E_MINUTE"));
			cal = GregorianCalendar.getInstance(TimeZone
					.getTimeZone("Europe/Berlin"));
			cal.clear();
			System.out.println(cal.getTime());
			cal.set(eYear, eMonth - 1, eDay, eHour, eMinute);
			
			System.out.println(cal.getTime());
			
			this.end = cal;
			logger.info("end date is: " + cal.toString());

			logger.info("init end group formation date");
			int eGFYear = Integer.parseInt(properties.getProperty("EGF_YEAR"));
			int eGFMonth = Integer
					.parseInt(properties.getProperty("EGF_MONTH"));
			int eGFDay = Integer.parseInt(properties.getProperty("EGF_DAY"));
			int eGFHour = Integer.parseInt(properties.getProperty("EGF_HOUR"));
			int eGFMinute = Integer.parseInt(properties
					.getProperty("EGF_MINUTE"));
			cal = GregorianCalendar.getInstance(TimeZone
					.getTimeZone("Europe/Berlin"));
			cal.clear();
			cal.set(eGFYear, eGFMonth - 1, eGFDay, eGFHour, eGFMinute);
			this.endGroupFormation = cal;
			logger.info("end date is: " + cal.toString());
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	public static void main(String[] args) {
		Calendar cal = GregorianCalendar.getInstance(TimeZone
				.getTimeZone("Europe/Berlin"));
		
		cal.set(2016, 7,15,2,10);
		
		System.out.println(cal.getTime());
	}

	/*
	 * public FedoraManager getFedoraManager() { return fedoraManager; }
	 */

	public UserManagerInterface getUserManager() {
		return userManager;
	}

	private void connect() throws SQLException {
		dbConnection = DriverManager.getConnection(
				properties.getProperty("mysql_url"),
				properties.getProperty("mysql_user"),
				properties.getProperty("mysql_password"));
		setChanged();
		notifyObservers();
	}

	private Long lastCheck = 0L;

	/**
	 * 
	 * @return true if new connection was established
	 * @throws SQLException
	 */
	public void checkConnection() throws SQLException {
		synchronized (lastCheck) {
			// exit if last check was one or less seconds ago
			if (lastCheck > System.currentTimeMillis() - 2000)
				return;

			if (!dbConnection.isValid(1)) {
				logger.error("Database connection invalid try to reconnect");

				try {
					dbConnection.close();
				} catch (SQLException e) {
				}
				try {
					// small backoff in case there is a problem with db server
					Thread.sleep(20L);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				try {
					connect();
				} catch (SQLException e) {
					logger.error(e.getLocalizedMessage(), e);
					// may be the result of missing driver
					DriverManager.registerDriver(new com.mysql.jdbc.Driver());
					connect();
				}
			}

			lastCheck = System.currentTimeMillis();
		}
	}

	public Properties getProperties() {
		return properties;
	}

	/**
	 * This method should be called before the system shuts down
	 */
	@PreDestroy
	public void onDestroy() {
		getLogManager().onDestroy();

		try {
			dbConnection.close();
		} catch (SQLException e) {
		} // ignore
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
				logger.info("deregistered driver: " + driver);
			} catch (SQLException e) {
				logger.error("error while deregistering driver: " + driver, e);
			}
		}
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
		for (Thread t : threadArray) {
			if (t.getName().contains("Abandoned connection cleanup thread")) {
				synchronized (t) {
					t.stop(); // don't complain, it works
				}
			}
		}
		org.apache.log4j.LogManager.shutdown();
	}

	/**
	 * Will be deprecated in the future
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnectionStatic() throws SQLException {
		Learnweb lw = getInstance();
		lw.checkConnection();

		return lw.dbConnection;
	}

	// should be used instead of the static method
	public Connection getConnection() throws SQLException {
		checkConnection();

		return dbConnection;
	}

	/**
	 * 
	 * @return Returns the servername + contextpath. For the default
	 *         installation this is: http://learnweb.l3s.uni-hannover.de
	 */
	public String getContextUrl() {
		if (null == contextUrl) {
			ExternalContext ext = FacesContext.getCurrentInstance()
					.getExternalContext();

			if (ext.getRequestServerPort() == 80
					|| ext.getRequestServerPort() == 443)
				contextUrl = ext.getRequestScheme() + "://"
						+ ext.getRequestServerName()
						+ ext.getRequestContextPath();
			else
				contextUrl = ext.getRequestScheme() + "://"
						+ ext.getRequestServerName() + ":"
						+ ext.getRequestServerPort()
						+ ext.getRequestContextPath();
			logger.info("context url is set to '" + contextUrl + "'");
		}
		return contextUrl; // because we don't use httpS we can cache the url,
							// change it if you want to use httpS too
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public Calendar getStart() {
		return this.start;
	}

	public Calendar getEnd() {
		return this.end;
	}

	public Calendar getEndGroupFormation() {
		return this.endGroupFormation;
	}

	public boolean isRunning() {
		return !isBeforeStart() && !isAfterEnd();
	}

	public boolean isBeforeStart() {
		long now = Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis();
		return (start.getTimeInMillis() - now) > 0;
	}

	public boolean isAfterEnd() {
		long now = Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis();
		return (end.getTimeInMillis() - now) < 0;
	}

	public boolean isAfterGroupFormation() {
		long now = Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis();
		return (endGroupFormation.getTimeInMillis() - now) < 0;
	}

	public boolean isGroupFormationPhase() {
		return !isBeforeStart() && !isAfterGroupFormation();
	}

	public BucketManager getBucketManager() {
		return this.bucketManager;
	}

	public BonusEventManager getBonusEventManager() {
		return this.bonusEventManager;
	}

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	public HoneypotInstanceManager getHoneypotInstanceManager() {
		return this.honeypotInstanceManager;
	}

	public ImageManager getImageManager() {
		return this.imageManager;
	}

	public LogManager getLogManager() {
		return this.logManager;
	}

	public ScoreManager getScoreManager() {
		return scoreManager;
	}

	public TicketManager getTicketManager() {
		return this.ticketManager;
	}

	public UserCommentManager getUserCommentManager() {
		return this.userCommentManager;
	}

	public HashMap<Integer, String> getSessionMap() {
		return sessionMap;
	}

	public ApplicationProperties getApplicationProperties() {
		return this.applicationProperties;
	}

	public String getUserTablePrefix() {
		return userTablePrefix;
	}

	public void setUserTablePrefix(String userTablePrefix) {
		this.userTablePrefix = userTablePrefix;
	}

	public String getCFApiKey() {
		return this.cFApiKey;
	}

	public Boolean getPlanB() {
		return this.planB;
	}

	public GroupManager getGroupManager() {
		return this.groupManager;
	}

	public GroupMessageManager getGroupMessageManager() {
		return this.groupMessageManager;
	}

	public GroupScoreManager getGroupScoreManager() {
		return this.groupScoreManager;
	}

	public InvitationManager getInvitationManager() {
		return invitationManager;
	}

}
