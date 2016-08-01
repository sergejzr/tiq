package de.l3s.mt.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.util.HasId;

public class Group implements HasId, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Group.class);
	private int id;
	private String groupname;
	// retrieved by joining tables; needs to be stored separately!
	// role of the retrieving user!
	private int numMembers;
	private List<User> members;

	public Group() {
		this.id = -1;
		this.setNumMembers(0);
	}

	public Group(ResultSet rs) throws SQLException {
		this.id = rs.getInt("g.group_id");
		this.setGroupname(rs.getString("g.groupname"));
		this.setNumMembers(rs.getInt("g.num_members"));
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getNumMembers() {
		return numMembers;
	}

	public void setNumMembers(int numMembers) {
		this.numMembers = numMembers;
	}

	public List<User> getMembers() {
		return members;
	}

	public void setMembers(List<User> members) {
		this.members = members;
		if (null != members) {
			this.numMembers = members.size();
		}
	}

	public String getName() {
		return getGroupname();
	}

	public void setName(String name) {
		this.setGroupname(name);
	}

	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}
}
