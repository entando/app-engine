/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.common.FieldSearchFilter;

import java.time.LocalDateTime;
import java.util.List;

public interface IFormDAO {

	public List<Long> searchForms(FieldSearchFilter[] filters);
	
	public Form loadForm(long id);

	public List<Long> loadForms();

	public void removeForm(long id);
	
	public void updateForm(Form form);

	public void insertForm(Form form);

    public int countForms(FieldSearchFilter[] filters);

	public List<Form>getFormList();

	public List<Form> searchByDateAfter(LocalDateTime data, Boolean delivered);
	public List<Form> searchByDateBefore(LocalDateTime data, Boolean delivered);

	public void cronJob();
}
