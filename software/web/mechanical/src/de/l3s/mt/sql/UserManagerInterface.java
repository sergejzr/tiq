package de.l3s.mt.sql;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.User;

public interface UserManagerInterface {

	/**
	 * returns a list of all users
	 * 
	 * @return
	 * @throws SQLException
	 */
	public abstract List<User> getUsers() throws SQLException;

	/**
	 * get a user by username and password
	 * 
	 * @param Username
	 * @param Password
	 * @return null if user not found
	 * @throws SQLException
	 */
	public abstract User getUser(String username, String password)
			throws SQLException;

	/**
	 * Get a User by his id returns null if the user does not exist
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public abstract User getUser(int userId) throws SQLException;

	/**
	 * Get a User by his email returns null if the user does not exist
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public abstract User getUser(String email) throws SQLException;

	/**
	 * Get a User by his user token returns null if the user does not exist
	 * 
	 * @param token
	 * @return
	 * @throws SQLException
	 */
	public abstract User getUserByToken(String token) throws SQLException;

	/**
	 * Returns true if username is already in use
	 * 
	 * @param username
	 * @return
	 * @throws SQLException
	 */
	public abstract boolean isUsernameAlreadyTaken(String username)
			throws SQLException;

	/**
	 * Returns true if mail is already in use
	 * 
	 * @param email
	 * @return
	 * @throws SQLException
	 */
	public abstract boolean isMailAlreadyTaken(String email)
			throws SQLException;

	/**
	 * Saves the User to the database. If the User is not yet stored at the
	 * database, a new record will be created and the returned User contains the
	 * new id.
	 * 
	 * @param user
	 * @return
	 * @throws SQLException
	 */
	public abstract User save(User user) throws SQLException;

	// MT methods
	public User registerUser(String username, String password, String email,
			String age, String country, String facebook, int gender)
			throws Exception;

	public Map<String, User> getInactiveUsers() throws SQLException;

}