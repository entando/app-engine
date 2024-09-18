/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.exception.ApsSystemException;
import java.util.List;

public interface IFormManager {

	final String BEAN_ID = "jpwebformFormManager";


    /**
	 * Load a file representing a form from the disk
	 * @param file
	 * @return
	 * @throws ApsSystemException
	 */
	public Form getForm(long file) throws ApsSystemException;

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

}
