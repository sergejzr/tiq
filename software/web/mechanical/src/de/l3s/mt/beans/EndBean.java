package de.l3s.mt.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.mt.model.LotteryTicket;

@ManagedBean
@RequestScoped
public class EndBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = 5326979217342170543L;

	private static boolean lottery;
	private static boolean initialized = false;
	private boolean winnersDrawn;
	private List<LotteryTicket> tickets;

	private static Logger logger = Logger.getLogger(EndBean.class);

	public EndBean() throws SQLException {
		init();
		initLottery();
	}

	private void initLottery() throws SQLException {
		if (!EndBean.lottery)
			return;

		this.winnersDrawn = Boolean.parseBoolean(getLearnweb()
				.getApplicationProperties().getProperty("WINNERS_DRAWN"));
		if (this.winnersDrawn) {
			tickets = getLearnweb().getTicketManager().getAllTickets();
		}
	}

	private void init() {
		if (!EndBean.initialized) {
			logger.info("initialize end bean");
			EndBean.lottery = Boolean.parseBoolean(getLearnweb()
					.getProperties().getProperty("LOTTERY"));
			logger.info("INIT: lottery = " + EndBean.lottery);

			EndBean.initialized = true;
		}
	}

	public boolean isWinnersDrawn() {
		return this.winnersDrawn;
	}

	public boolean isLottery() {
		return EndBean.lottery;
	}

	public List<LotteryTicket> getTickets() {
		return this.tickets;
	}
}
