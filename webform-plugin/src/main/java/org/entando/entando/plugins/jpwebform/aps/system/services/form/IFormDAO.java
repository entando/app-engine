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

	List<Long> searchForms(FieldSearchFilter[] filters);
	
	Form loadForm(long id);

	List<Long> loadForms();

	void updateFormData(Form form);

	void removeForm(long id);

    boolean existsSerial(String serial);

    void updateForm(Form form);

	void insertForm(Form form);

    int countForms(FieldSearchFilter[] filters);

	List<Form>getFormList();

	List<Form> searchByDateAfter(LocalDateTime data, Boolean delivered);

	List<Form> searchByDateBefore(LocalDateTime data, Boolean delivered);

}
