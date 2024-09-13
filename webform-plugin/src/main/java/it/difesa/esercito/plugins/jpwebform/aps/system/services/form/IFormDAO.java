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
	
	public Form loadForm(int id);

	public List<Integer> loadForms();

	public void removeForm(int id);
	
//	public void updateForm(Form form);

	public void insertForm(Form form);

    public int countForms(FieldSearchFilter[] filters);
}
