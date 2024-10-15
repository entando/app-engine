/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.common.AbstractSearcherDAO;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.model.FormData;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.mail.IMailManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FormDAO extends AbstractSearcherDAO implements IFormDAO {

	private static final Logger logger =  LoggerFactory.getLogger(FormDAO.class);
	private IMailManager _mailManager;
	private static final Integer MAX_HOURS = 12;

	@Override
	public int countForms(FieldSearchFilter[] filters) {
		Integer forms = null;
		try {
			forms = super.countId(filters);
		} catch (Throwable t) {
			logger.error("error in count forms", t);
			throw new RuntimeException("error in count forms", t);
		}
		return forms;
	}

	@Override
	protected String getTableFieldName(String metadataFieldKey) {
		return metadataFieldKey;
	}
	
	@Override
	protected String getMasterTableName() {
		return "jpwebform_form";
	}
	
	@Override
	protected String getMasterTableIdFieldName() {
		return "id";
	}

	@Override
	public List<Long> searchForms(FieldSearchFilter[] filters) {
		List<Long> formsId = new ArrayList<>();
		List<String> masterList = super.searchId(filters);
		masterList.stream().forEach(idString -> formsId.add(Long.parseLong(idString)));
		return formsId;
	}


	@Override
	public List<Long> loadForms() {
		List<Long> formsId = new ArrayList<Long>();
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet res = null;
		try {
			conn = this.getConnection();
			stat = conn.prepareStatement(LOAD_FORMS_ID);
			res = stat.executeQuery();
			while (res.next()) {
				long id = res.getLong("id");
				formsId.add(id);
			}
		} catch (Throwable t) {
			logger.error("Error loading Form list",  t);
			throw new RuntimeException("Error loading Form list", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return formsId;
	}
	
	@Override
	public List<Form> getFormList(){
		List<Form> formlist = new ArrayList<Form>();
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet res = null;

		try {
			conn = this.getConnection();
			stat = conn.prepareStatement(ALL_FORM);
			res = stat.executeQuery();
			while (res.next()) {
				long id = res.getLong("id");
				formlist.add(this.loadForm(id));
			}

		} catch (Throwable t) {
			logger.error("Error loading Form list",  t);
			throw new RuntimeException("Error loading Form list", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return formlist;
	}

	@Override
	public void insertForm(Form form) {
		PreparedStatement stat = null;
		Connection conn  = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			long nextId = this.extractNextId(NEXT_ID, conn);
			form.setId(nextId);
			this.insertForm(form, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			logger.error("Error on insert form",  t);
			throw new RuntimeException("Error on insert form", t);
		} finally {
			this.closeDaoResources(null, stat, conn);
		}
	}

	protected long extractNextId(String query, Connection conn) {
		long id = 0;
		Statement stat = null;
		ResultSet res = null;
		try {
			stat = conn.createStatement();
			res = stat.executeQuery(query);
			res.next();
			id = res.getLong(1) + 1;
		} catch (Throwable t) {
			logger.error("Error extracting next id", t);
			throw new RuntimeException("Error extracting next id", t);
		} finally {
			closeDaoResources(res, stat);
		}
		return id;
	}

	public void insertForm(Form form, Connection conn) {
		PreparedStatement stat = null;
		int index = 1;

		try {
			stat = conn.prepareStatement(ADD_FORM);

			stat.setLong(index++, form.getId());
			stat.setString(index++, form.getName());
			stat.setString(index++, form.getCampagna());
			Timestamp submittedTimestamp = Timestamp.valueOf(form.getSubmitted());
			stat.setTimestamp(index++, submittedTimestamp);
			stat.setBoolean(index++, form.getDelivered());
			stat.setString(index++, form.getData().toJson());
			stat.setString(index, form.getSeriale());
			stat.executeUpdate();
		} catch (Throwable t) {
			logger.error("Error on insert form",  t);
			throw new RuntimeException("Error on insert form", t);
		} finally {
			this.closeDaoResources(null, stat, null);
		}
	}

	@Override
	public void updateForm(Form form) {
		PreparedStatement stat = null;
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.updateForm(form, conn);
 			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			logger.error("Error updating form {}", form.getId(),  t);
			throw new RuntimeException("Error updating form", t);
		} finally {
			this.closeDaoResources(null, stat, conn);
		}
	}

	public void updateForm(Form form, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(UPDATE_FORM);
			int index = 1;

			form.setDelivered(true);//<=======cambio lo stato da false a true
			stat.setBoolean(index++, form.getDelivered());//<=======

			stat.setLong(index++, form.getId());//<======
			stat.executeUpdate();
		} catch (Throwable t) {
			logger.error("Error updating form {}", form.getId(),  t);
			throw new RuntimeException("Error updating form", t);
		} finally {
			this.closeDaoResources(null, stat, null);
		}
	}

	@Override
	public void removeForm(long id) {
		PreparedStatement stat = null;
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.removeForm(id, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			logger.error("Error deleting form {}", id, t);
			throw new RuntimeException("Error deleting form", t);
		} finally {
			this.closeDaoResources(null, stat, conn);
		}
	}

	public void removeForm(long id, Connection conn) {
		PreparedStatement stat = null;

		try {
			stat = conn.prepareStatement(DELETE_FORM);
			int index = 1;
			stat.setLong(index++, id);
			stat.executeUpdate();
		} catch (Throwable t) {
			logger.error("Error deleting form {}", id, t);
			throw new RuntimeException("Error deleting form", t);
		} finally {
			this.closeDaoResources(null, stat, null);
		}
	}

	public Form loadForm(long id) {
		Form form = null;
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet res = null;

		try {
			conn = this.getConnection();
			form = this.loadForm(id, conn);
		} catch (Throwable t) {
			logger.error("Error loading form with id {}", id, t);
			throw new RuntimeException("Error loading form with id " + id, t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return form;
	}



	public Form loadForm(long id, Connection conn) {
		Form form = null;
		PreparedStatement stat = null;
		ResultSet res = null;

		try {
			stat = conn.prepareStatement(LOAD_FORM);
			int index = 1;
			stat.setLong(index++, id);
			res = stat.executeQuery();
			if (res.next()) {
				form = this.buildFormFromRes(res);
			}
		} catch (Throwable t) {
			logger.error("Error loading form with id {}", id, t);
			throw new RuntimeException("Error loading form with id " + id, t);
		} finally {
			closeDaoResources(res, stat, null);
		}
		return form;
	}

	protected Form buildFormFromRes(ResultSet res) {
		Form form = null;

		try {
			form = new Form();
			form.setId(res.getLong("id"));
			form.setName(res.getString("name"));
			form.setCampagna(res.getString("campagna"));
			Timestamp submittedValue = res.getTimestamp("submitted");
			form.setDelivered(res.getBoolean("delivered"));//<==========
			if (null != submittedValue) {
				//form.setSubmitted(new Date(submittedValue.getTime()));
				form.setSubmitted(submittedValue.toLocalDateTime());
			}
			String json = res.getString("data");
			ObjectMapper mapper = new ObjectMapper();
			FormData data = mapper.readValue(json, FormData.class);
			form.setData(data);
			form.setSeriale(res.getString("seriale")); //<========
		} catch (Throwable t) {
			logger.error("Error in buildFormFromRes", t);
		}
		return form;
	}



	/**/
	public List<Form> searchByDateAfter(LocalDateTime data, Boolean delivered){

		List<Form> formList= getFormList();

		if(formList == null || formList.isEmpty()){
			return Collections.EMPTY_LIST;
		}

		return	formList.stream()
					.filter(form->data.isAfter(form.getSubmitted()) && delivered.equals(form.getDelivered()))
				.collect(Collectors.toList());
	}

	/**/
	public List<Form> searchByDateBefore(LocalDateTime data, Boolean delivered){

		List<Form> formList= getFormList();

		if(formList == null || formList.isEmpty()){
			return Collections.EMPTY_LIST;
		}

		return	formList.stream()
				.filter(form->data.isBefore(form.getSubmitted()) && delivered.equals(form.getDelivered()))
				.collect(Collectors.toList());
	}

	@Override
	@Scheduled(cron="* */2 * * * *")
	public void cronJob() {

		List<Form> formListExpired = searchByDateAfter(LocalDateTime.now().minus(MAX_HOURS, ChronoUnit.HOURS), false);

		if (formListExpired != null && !formListExpired.isEmpty()) {
			formListExpired.forEach(form -> {
				try {
					updateForm(form);

					_mailManager.sendMail(form);

					logger.info("updated form {} after delivery (or expiration!)", form.getId());


				} catch (Exception e) {
					logger.error("Unexpected error trying to update the form", form.getId(), e);
				}
			});
		}
	}


	private static final String ADD_FORM = "INSERT INTO jpwebform_form (id, name, campagna, submitted, delivered, \"data\", seriale) VALUES (?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_FORM = "UPDATE jpwebform_form SET  delivered=? WHERE id = ?";

	private static final String DELETE_FORM = "DELETE FROM jpwebform_form WHERE id = ?";

	private static final String LOAD_FORM = "SELECT id, name, campagna, submitted, delivered, \"data\", seriale  FROM jpwebform_form WHERE id = ?";

	private static final String LOAD_FORMS_ID  = "SELECT id FROM jpwebform_form";

	private final String NEXT_ID = "SELECT MAX(id) FROM jpwebform_form";

	private final String ALL_FORM ="SELECT * FROM jpwebform_form";

	private final String SEARCH_BY_DATE_AFTER ="SELECT id, name, submitted, delivered, \"data\", seriale FROM jpwebform_form WHERE submitted >= ? AND delivered = ?";//<========

	private final String SEARCH_BY_DATE_BEFORE ="SELECT id, name, submitted, delivered, \"data\", seriale FROM jpwebform_form WHERE submitted <= ? AND delivered = ?";//<========
}
