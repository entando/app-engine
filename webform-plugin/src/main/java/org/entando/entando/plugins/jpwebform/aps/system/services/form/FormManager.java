/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.exception.ApsSystemException;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormManager extends AbstractService implements IFormManager {

	private static final Logger log =  LoggerFactory.getLogger(FormManager.class);
	private static final int MAX_AGE_HOURS = 6;
	private static final int MAX_NUMBER_HASH_CODE = 18;

	private File _formPath;
	private Integer _ageHours;

	@Override
	public void init() throws Exception {
		log.debug("looking for form folder");
		String tempDirPath = System.getProperty("java.io.tmpdir");
		_formPath = new File(tempDirPath + File.separator + "sme-form");
		if (!_formPath.exists()) {
			if (!_formPath.mkdir()) {
				log.error("could not create form directory");
				throw new RuntimeException("could not create form directory");
			}
			log.debug("fold directory created");
		}
		log.info("Form directory: " + _formPath.getAbsolutePath());
		log.debug("{} ready.", this.getClass().getName());
	}
 
	@Override
	public Form getForm(long id) throws ApsSystemException {
		try {
			log.debug("Loading form {}", id);
			return _formDAO.loadForm(id);
		} catch (Throwable t) {
			log.error("Error loading form with id '{}'", id,  t);
			throw new ApsSystemException("Error loading form with id: " + id, t);
		}
	}

	@Override
	public List<Long> getForms() throws ApsSystemException {
		List<Long> forms = new ArrayList<>();
		try {
			forms = _formDAO.loadForms();
		} catch (Throwable t) {
			log.error("Error loading Form list",  t);
			throw new ApsSystemException("Error loading Form ", t);
		}
		return forms;
	}

	@Override
	public void addForm(Form form) throws ApsSystemException {

		try {
			if (StringUtils.isBlank(form.getSerial())) {
				form.setSerial(RandomStringUtils.randomAlphanumeric(MAX_NUMBER_HASH_CODE));
				form.setHead(Boolean.TRUE);
			} else {
				form.setHead(Boolean.FALSE);
			}
			_formDAO.insertForm(form);

		} catch (Throwable t) {
			log.error("Error adding Form", t);
			throw new ApsSystemException("Error adding Form", t);
		}
	}

	@Override
	public void deleteForm(long id) throws ApsSystemException {

		try {
			_formDAO.removeForm(id);
		} catch (Throwable t) {
			log.error("Error deleting Form with id {}", id, t);
			throw new ApsSystemException("Error deleting Form with id:" + id, t);
		}
	}

	@Override
	public List<Form> getFormList() throws ApsSystemException {
		List<Form> listForm= new ArrayList<>();
		try {
			listForm= _formDAO.getFormList();
		} catch (Throwable t) {
			log.error("Error getting the list of forms",t);
			throw new ApsSystemException("Error getting the list of forms", t);
		}
		return listForm;
	}

	@Override
	public List<Form> searchByDateAfter(LocalDateTime data, Boolean delivered) throws ApsSystemException {
		List<Form> listForm= new ArrayList<>();
		try {
			listForm= _formDAO.searchByDateAfter(data, delivered);
		} catch (Throwable t) {
			log.error("Error getting date list form",t);
			throw new ApsSystemException("Error getting date list form", t);
		}
		return listForm;
	}

	@Override
	public List<Form> searchByDateBefore(LocalDateTime data, Boolean delivered) throws ApsSystemException {
		List<Form> listForm= new ArrayList<>();
		try {
			listForm= _formDAO.searchByDateBefore(data, delivered);
		} catch (Throwable t) {
			log.error("Error getting date list form",t);
			throw new ApsSystemException("Error getting date list form", t);
		}
		return listForm;
	}

	@Override
	public List<Long> search(FieldSearchFilter[] filter) {
		return _formDAO.searchForms(filter);
	}

	public Integer getAgeHours() {
		if (_ageHours == null) {
			_ageHours = MAX_AGE_HOURS;
		}
		return _ageHours;
	}

	@Override
	public void updateForm(Form form) {
		try {
			log.info("updating form ID ", form.getId());
			_formDAO.updateForm(form);
		} catch (Throwable t) {
			log.error("Error updating form",t);
			new ApsSystemException("Error updating form", t);
		}
	}

	@Override
	public void updateFormData(Form form) {
		try {
			log.info("updating payload of the form ID ", form.getId());
			_formDAO.updateFormData(form);
		} catch (Throwable t) {
			log.error("Error updating form payload",t);
			new ApsSystemException("Error updating form payload", t);
		}
	}

	public IFormDAO getFormDAO() {
		return _formDAO;
	}

	public void setFormDAO(IFormDAO formDAO) {
		this._formDAO = formDAO;
	}

	public void setAgeHours(Integer _ageHours) {
		this._ageHours = _ageHours;
	}

	private IFormDAO _formDAO;
}
