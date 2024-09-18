/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.common.FieldSearchFilter;
import java.util.List;

public interface IFormDAO {

	public List<Integer> searchForms(FieldSearchFilter[] filters);
	
	public Form loadForm(long id);

	public List<Long> loadForms();

	public void removeForm(long id);
	
//	public void updateForm(Form form);

	public void insertForm(Form form);

    public int countForms(FieldSearchFilter[] filters);
}
