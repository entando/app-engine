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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormDAO extends AbstractSearcherDAO implements IFormDAO {

	private static final Logger logger =  LoggerFactory.getLogger(FormDAO.class);

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
    public List<Integer> searchForms(FieldSearchFilter[] filters) {
            List<Integer> formsId = new ArrayList<>();
        List<String> masterList = super.searchId(filters);
        masterList.stream().forEach(idString -> formsId.add(Integer.parseInt(idString)));
        return formsId;
        }


	@Override
	public List<Integer> loadForms() {
		List<Integer> formsId = new ArrayList<Integer>();
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet res = null;
		try {
			conn = this.getConnection();
			stat = conn.prepareStatement(LOAD_FORMS_ID);
			res = stat.executeQuery();
			while (res.next()) {
				int id = res.getInt("id");
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
	public void insertForm(Form form) {
		PreparedStatement stat = null;
		Connection conn  = null;
		try {
			long nextId = this.extractNextId(NEXT_ID, conn);
			conn = this.getConnection();
			conn.setAutoCommit(false);
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
			id = res.getLong(1) + 1; // N.B.: funziona anche per il primo record
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
		try {
			stat = conn.prepareStatement(ADD_FORM);
			int index = 1;
			stat.setLong(index++, form.getId());
 			stat.setString(index++, form.getName());
			if(null != form.getSubmitted()) {
				Timestamp submittedTimestamp = new Timestamp(form.getSubmitted().getTime());
				stat.setTimestamp(index++, submittedTimestamp);	
			} else {
				stat.setNull(index++, Types.DATE);
			}
  			stat.setString(index++, form.getData().toJson());
			stat.executeUpdate();
		} catch (Throwable t) {
			logger.error("Error on insert form",  t);
			throw new RuntimeException("Error on insert form", t);
		} finally {
			this.closeDaoResources(null, stat, null);
		}
	}

//	@Override
//	public void updateForm(Form form) {
//		PreparedStatement stat = null;
//		Connection conn = null;
//		try {
//			conn = this.getConnection();
//			conn.setAutoCommit(false);
//			this.updateForm(form, conn);
// 			conn.commit();
//		} catch (Throwable t) {
//			this.executeRollback(conn);
//			logger.error("Error updating form {}", form.getId(),  t);
//			throw new RuntimeException("Error updating form", t);
//		} finally {
//			this.closeDaoResources(null, stat, conn);
//		}
//	}

//	public void updateForm(Form form, Connection conn) {
//		PreparedStatement stat = null;
//		try {
//			stat = conn.prepareStatement(UPDATE_FORM);
//			int index = 1;
//
// 			stat.setString(index++, form.getName());
//			if(null != form.getSubmitted()) {
//				Timestamp submittedTimestamp = new Timestamp(form.getSubmitted().getTime());
//				stat.setTimestamp(index++, submittedTimestamp);
//			} else {
//				stat.setNull(index++, Types.DATE);
//			}
//  			stat.setString(index++, form.getData());
//			stat.setInt(index++, form.getId());
//			stat.executeUpdate();
//		} catch (Throwable t) {
//			logger.error("Error updating form {}", form.getId(),  t);
//			throw new RuntimeException("Error updating form", t);
//		} finally {
//			this.closeDaoResources(null, stat, null);
//		}
//	}

	@Override
	public void removeForm(int id) {
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
	
	public void removeForm(int id, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(DELETE_FORM);
			int index = 1;
			stat.setInt(index++, id);
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
			Timestamp submittedValue = res.getTimestamp("submitted");
			if (null != submittedValue) {
				form.setSubmitted(new Date(submittedValue.getTime()));
			}
			String json = res.getString("data");
			ObjectMapper mapper = new ObjectMapper();
			FormData data = mapper.readValue(json, FormData.class);
			form.setData(data);
		} catch (Throwable t) {
			logger.error("Error in buildFormFromRes", t);
		}
		return form;
	}

	private static final String ADD_FORM = "INSERT INTO jpwebform_form (id, name, submitted, data ) VALUES (?, ?, ?, ? )";

	private static final String UPDATE_FORM = "UPDATE jpwebform_form SET  name=?,  submitted=?, data=? WHERE id = ?";

	private static final String DELETE_FORM = "DELETE FROM jpwebform_form WHERE id = ?";
	
	private static final String LOAD_FORM = "SELECT id, name, submitted, \"data\"  FROM jpwebform_form WHERE id = ?";
	
	private static final String LOAD_FORMS_ID  = "SELECT id FROM jpwebform_form";

	private final String NEXT_ID = "SELECT MAX(id) FROM jpwebform_form";
	
}
