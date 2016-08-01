package de.l3s.mt.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.mt.model.Image;

public class ImageManager extends SQLManager {
	private static Logger logger = Logger.getLogger(ImageManager.class);
	private String IMAGETABLE;
	private String COLUMNS = " i.`image_id` AS 'i.image_id', i.`entity_id` AS 'i.entity_id', i.`honey_pot` AS 'i.honey_pot' ";

	private Learnweb learnweb;

	public ImageManager(Learnweb learnweb) throws SQLException {
		super();
		this.learnweb = learnweb;
		IMAGETABLE = learnweb.getTablePrefix() + "image";
	}

	private Learnweb getLearnweb() {
		return learnweb;
	}

	public Image getImage(int id) throws SQLException {
		Image image;

		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = getLearnweb().getConnection().prepareStatement(
					"SELECT " + COLUMNS + " FROM `" + IMAGETABLE
							+ "` i WHERE i.`image_id` = ? ");
			pStmt.setInt(1, id);
			logger.debug("getImage() executes: " + pStmt.toString());
			rs = pStmt.executeQuery();
			if (!rs.next())
				return null;

			image = new Image(rs);
			return image;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

	public List<Image> getImagesByEntity(int entityId, boolean honeyPot)
			throws SQLException {
		List<Image> images = null;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = getLearnweb()
					.getConnection()
					.prepareStatement(
							" SELECT "
									+ COLUMNS
									+ " FROM `"
									+ IMAGETABLE
									+ "` i WHERE "
									+ " i.`entity_id` = ? AND i.`honey_pot` = ? AND i.`deleted` = 0 ");
			pStmt.setInt(1, entityId);
			pStmt.setBoolean(2, honeyPot);
			logger.debug("getImagesByEntity() executes " + pStmt.toString());
			rs = pStmt.executeQuery();
			images = new ArrayList<Image>();
			while (rs.next()) {
				try {
					images.add(new Image(rs));
				} catch (SQLException e) {
					logger.error("failed to instantiate image", e);
				}
			}
			return images;
		} catch (SQLException e) {
			throw e;
		} finally {
			close(rs, pStmt);
		}
	}

}
