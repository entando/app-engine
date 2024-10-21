/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.exception.ApsSystemException;

import java.time.LocalDateTime;
import java.util.List;

public interface IFormManager {

	final String BEAN_ID = "jpwebformFormManager";


    /**
     * Load a file representing a form from the disk
     *
     * @param id
     * @return
     * @throws ApsSystemException
     */
	public Form getForm(long id) throws ApsSystemException;

	/**
     * Retrieve from disk the forms not expired
     *
     * @return
     * @throws ApsSystemException
     */
	public List<Long> getForms() throws ApsSystemException;

	/**
	 * Persist a form that couldn't be delivered
	 *
	 * @param form
	 * @throws ApsSystemException
	 */
	public void addForm(Form form) throws ApsSystemException;

	/**
	 * Delete a form from disk
	 *
	 * @param id the name of the form to delete
	 * @throws ApsSystemException in case the form file does not exist
	 */
	public void deleteForm(long id) throws ApsSystemException;


	public List<Form> getFormList() throws ApsSystemException;

	public List<Form> searchByDateAfter(LocalDateTime data, Boolean delivered) throws ApsSystemException;

	public List<Form> searchByDateBefore(LocalDateTime data, Boolean delivered) throws ApsSystemException;

	public void cronJob() throws ApsSystemException;

	public void updateForm(Form form);

	/**
	 * Search using entity filters
	 *
	 * @param filter the array containing search filters
	 * @return
	 */
	List<Long> search(FieldSearchFilter[] filter);
}
