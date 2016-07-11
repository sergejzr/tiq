package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.ImagesInstance;

public class HoneypotInstanceManager extends SQLManager {
	private static Logger logger = Logger
			.getLogger(HoneypotInstanceManager.class);
	private Learnweb learnweb;
	private String HPITABLE;
	private String HPICOLUMNS = " hi.`user_id` AS 'hi.user_id', hi.`entity_id` AS 'hi.entity_id', "
			+ " hi.`ref_img` AS 'hi.ref_img', hi.`correct_img` AS 'hi.correct_img', "
			+ " hi.`picked_img` AS 'hi.picked_img', hi.`images` AS 'hi.images' ";

	public HoneypotInstanceManager(Learnweb learnweb) {
		super();
		this.learnweb = learnweb;
		HPITABLE = learnweb.getTablePrefix() + "honeypot_instance";
	}

	public ImagesInstance getHoneypotInstanceByUser(int userId)
			throws SQLException {
		ImagesInstance instance = null;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = learnweb.getConnection().prepareStatement(
					" SELECT " + HPICOLUMNS + " FROM `" + HPITABLE
							+ "` hi WHERE hi.`user_id` = ? ");
			pStmt.setInt(1, userId);
			logger.debug("getHPInstanceByUser executes: " + pStmt.toString());
			rs = pStmt.executeQuery();

			if (rs.next()) {
				try {
					instance = new ImagesInstance(rs);
				} catch (SQLException e) {
					logger.error(
							"Error instantiating honeypot ImagesInstance.", e);
				}
			}
			return instance;
		} finally {
			close(rs, pStmt);
		}
	}

	public ImagesInstance saveHoneypotInstance(int userId,
			ImagesInstance instance) throws SQLException {
		PreparedStatement pStmt = null;
		try {
			pStmt = learnweb
					.getConnection()
					.prepareStatement(
							" REPLACE INTO `"
									+ HPITABLE
									+ "` (`user_id`, `entity_id`, `ref_img`, `correct_img`, `picked_img`, `images`) "
									+ "VALUES (?,?,?,?,?,?)");
			pStmt.setInt(1, userId);
			pStmt.setInt(2, instance.getEntityId());
			pStmt.setInt(3, instance.getRefImage().getId());
			pStmt.setInt(4, instance.getCorrectImage().getId());
			pStmt.setInt(5, instance.getPickedImage());
			pStmt.setString(6, instance.getImagesString());
			logger.debug("saveHoneypotInstance executes: " + pStmt.toString());
			pStmt.executeUpdate();
			return instance;
		} finally {
			close(null, pStmt);
		}
	}
}
