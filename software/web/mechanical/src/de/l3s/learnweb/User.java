package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.l3s.mt.model.UserGroupRole;
import de.l3s.util.HasId;
import de.l3s.util.MD5;

public class User implements Comparable<User>, Serializable, HasId {
	private static final long serialVersionUID = 2482790243930271009L;

	public static final int GENDER_MALE = 1;
	public static final int GENDER_FEMALE = 2;

	private int id = -1;
	private Locale locale = null;
	private String username;
	private String email;
	private String password; // md5 hash

	private int gender;
	private Date dateofbirth;
	private String address;
	private String profession;
	private String additionalInformation;
	private String interest;
	private String phone;
	private Date registrationDate;

	private String age;
	private String country;
	private String facebook;

	private boolean destroyed = false;

	private TimeZone timeZone;

	private String mtToken;
	private boolean mailNotifications;
	private boolean active;
	private Integer workerId;
	private String channel;
	private int groupId;
	private UserGroupRole role;

	public User(ResultSet rs) throws SQLException {
		super();
		this.id = rs.getInt("user_id");
		this.username = rs.getString("username");
		this.email = rs.getString("email");
		this.password = rs.getString("password");

		this.gender = rs.getInt("gender");
		this.dateofbirth = rs.getDate("dateofbirth");
		this.address = rs.getString("address");
		this.profession = rs.getString("profession");
		this.additionalInformation = rs.getString("additionalinformation");
		this.interest = rs.getString("interest");
		this.phone = rs.getString("phone");
		this.registrationDate = rs.getDate("registration_date");
		this.timeZone = TimeZone.getTimeZone("Europe/Berlin");

		this.mtToken = rs.getString("mt_token");

		this.age = rs.getString("age");
		this.country = rs.getString("country");
		this.facebook = rs.getString("facebook");
		this.setMailNotifications(rs.getBoolean("mail_notifications"));
		this.active = rs.getBoolean("active");
		this.workerId = rs.getInt("worker_id");
		this.channel = rs.getString("channel");
	}

	public User() {
		this.timeZone = TimeZone.getTimeZone("Europe/Berlin");
		this.mailNotifications = true;
		this.active = false;
	}

	public void onDestroy() {
		if (destroyed)
			return;
		destroyed = true;
	}

	public String getAdditionalInformation() {
		return additionalInformation;
	}

	public String getAddress() {
		return address;
	}

	public Date getDateofbirth() {
		return dateofbirth;
	}

	public String getEmail() {
		return email;
	}

	public int getId() {
		return id;
	}

	/**
	 * Returns User.GENDER_MALE, User.GENDER_FEMALE or 0 if not set
	 * 
	 * @return
	 */
	public int getGender() {
		return gender;
	}

	public String getInterest() {
		return interest;
	}

	public String getPhone() {
		return phone;
	}

	public String getProfession() {
		return profession;
	}

	public String getUsername() {
		return username;
	}

	public void setAdditionalinformation(String additionalinformation) {
		this.additionalInformation = additionalinformation;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setDateofbirth(Date dateofbirth) {
		this.dateofbirth = dateofbirth;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public void setInterest(String interest) {
		this.interest = interest;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public int compareTo(User o) {
		return getUsername().compareTo(o.getUsername());
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * If the password is not encrypted (plain text) set isEncrypted to false
	 * 
	 * @param password
	 * @param isEncrypted
	 */
	public void setPassword(String password, boolean isEncrypted) {
		if (!isEncrypted)
			password = MD5.hash(password);
		this.password = password;
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public String getWelcomePage() {
		return "mt.jsf";
	}

	public String getMtToken() {
		return mtToken;
	}

	public void setMtToken(String mtToken) {
		this.mtToken = mtToken;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getFacebook() {
		return facebook;
	}

	public void setFacebook(String facebook) {
		this.facebook = facebook;
	}

	public boolean isMailNotifications() {
		return mailNotifications;
	}

	public void setMailNotifications(boolean mailNotifications) {
		this.mailNotifications = mailNotifications;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Integer getWorkerId() {
		return workerId;
	}

	public void setWorkerId(Integer workerId) {
		this.workerId = workerId;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public int getGroupId() {
		// TODO: fix
		return this.id;
		// return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public UserGroupRole getRole() {
		// TODO: fix
		return UserGroupRole.LEADER;
		// return role;
	}

	public void setRole(UserGroupRole role) {
		this.role = role;
	}
}
