package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.UserComment;

public class UserCommentManager extends SQLManager {

	private String USERCOMMENTTABLE;

	private Learnweb learnweb;

	public UserCommentManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;

		USERCOMMENTTABLE = learnweb.getTablePrefix() + "user_comment";
	}

	public UserComment save(UserComment comment) throws SQLException {
		ResultSet rs = null;
		PreparedStatement pStmt = null;
		try {
			pStmt = learnweb
					.getConnection()
					.prepareStatement(
							" REPLACE INTO `"
									+ USERCOMMENTTABLE
									+ "` (comment_id, user_id, text, instance, bucket_number, progress_index) "
									+ " VALUES (?, ?, ?, ?, ?, ?)",
							PreparedStatement.RETURN_GENERATED_KEYS);
			if (comment.getId() == -1) {
				pStmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				pStmt.setInt(1, comment.getId());
			}
			pStmt.setInt(2, comment.getUserId());
			pStmt.setString(3, comment.getText());
			pStmt.setString(4, comment.getInstance());
			pStmt.setInt(5, comment.getBucketNumber());
			pStmt.setInt(6, comment.getProgressIndex());
			pStmt.executeUpdate();

			if (comment.getId() < 0) // get the assigned id
			{
				rs = pStmt.getGeneratedKeys();
				if (!rs.next())
					throw new SQLException("database error: no id generated");
				comment.setId(rs.getInt(1));
			}
			return comment;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

}
