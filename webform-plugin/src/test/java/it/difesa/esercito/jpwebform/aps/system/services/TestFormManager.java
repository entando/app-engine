/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.exception.ApsSystemException;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.IFormManager;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import org.entando.entando.plugins.jpwebform.aps.system.services.mail.IMailManager;
import org.junit.jupiter.api.*;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Date;
import java.util.List;

import static org.entando.entando.plugins.jpwebform.aps.system.services.form.IFormManager.BEAN_ID;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestFormManager extends BaseTestCase {

	//public static final ZoneId ZONE_ITALY = ZoneId.of("Europe/Rome");
	private static final LocalDateTime TODAY = LocalDateTime.now();

	private static LocalDateTime LDT_VERiFY = LocalDateTime.of(
			LocalDate.of(2024,5,9), LocalTime.of(23,01,45,000000)
	);


	@BeforeEach
	public void init() {
		this._mailManager = (IMailManager) this.getApplicationContext().getBean(IMailManager.BEAN_ID);
		this._formManager = (IFormManager) this.getService(BEAN_ID);
		assertNotNull(_formManager);
		DataSource dataSource = (DataSource) this.getApplicationContext().getBean("portDataSource");
	}


	@Test
	public void testListForm() throws ApsSystemException {

		Form form0 = new Form();
		Form form1 = new Form();
		Form form2 = new Form();
		Form form3 = new Form();
		Form form4 = new Form();
		Form form5 = new Form();
		Form form6 = new Form();

		try {


			form0.setName("Romolo");
			form0.setCampagna("Romolo");
			form0.setSubmitted(LocalDateTime.parse("2024-05-09T05:28:15.000000")); //2024-05-09T05:28:15.000000
			form0.setDelivered(true);
			form0.setData(getFormDataForTest());


			form1.setName("Numa Pompilio");
			form1.setCampagna("Numa Pompilio");
			form1.setSubmitted(LocalDateTime.parse("2024-05-09T23:01:45.000000"));
			form1.setDelivered(true);
			form1.setData(getFormDataForTest());


			form2.setName("Tullo Ostilio");
			form2.setCampagna("Tullo Ostilio");
			form2.setSubmitted(LocalDateTime.parse("2024-05-09T03:27:50.000000"));
			form2.setDelivered(false);
			form2.setData(getFormDataForTest());


			form3.setName("Anco Marzio");
			form3.setCampagna("Anco Marzio");
			form3.setSubmitted(TODAY);
			form3.setDelivered(false);
			form3.setData(getFormDataForTest());


			form4.setName("Tarquinio Prisco");
			form4.setCampagna("Tarquinio Prisco");
			form4.setSubmitted(LocalDateTime.parse("2024-05-09T05:20:15.000000"));
			form4.setDelivered(true);
			form4.setData(getFormDataForTest());


			form5.setName("Servio Tullio");
			form5.setCampagna("Servio Tullio");
			form5.setSubmitted(LocalDateTime.parse("2024-05-09T15:25:30.000000"));
			form5.setDelivered(false);
			form5.setData(getFormDataForTest());


			form6.setName("Tarquinio il superbo");
			form6.setCampagna("Tarquinio il superbo");
			form6.setSubmitted(LocalDateTime.parse("2024-05-09T08:40:14.000000"));
			form6.setDelivered(true);
			form6.setData(getFormDataForTest());


			_formManager.addForm(form0);
			_formManager.addForm(form1);
			_formManager.addForm(form2);
			_formManager.addForm(form3);
			_formManager.addForm(form4);
			_formManager.addForm(form5);
			_formManager.addForm(form6);

/*			_formManager.getFormList().forEach(form -> {
				System.out.println("\n"+form.getId()+" "+form.getName()+" "+form.getSeriale()+"\n=======================\n");
			});*/

		List<Form> listSearchFormAfter1 = _formManager.searchByDateAfter(LDT_VERiFY, true);
		assertEquals(3,listSearchFormAfter1.size());

		List<Form> listSearchFormAfter2 = _formManager.searchByDateAfter(LDT_VERiFY, false);
		assertEquals(2,listSearchFormAfter2.size());

		List<Form> listSearchFormBefore1 = _formManager.searchByDateBefore(LDT_VERiFY, true);
		assertEquals(1,listSearchFormBefore1.size());

		List<Form> listSearchFormBefore2 = _formManager.searchByDateBefore(LDT_VERiFY, false);
		assertEquals(1,listSearchFormBefore2.size());


		} finally {
			_formManager.deleteForm(form0.getId());
			_formManager.deleteForm(form2.getId());
			_formManager.deleteForm(form3.getId());
			_formManager.deleteForm(form4.getId());
			_formManager.deleteForm(form5.getId());
			_formManager.deleteForm(form6.getId());
		}
	}

	@Test
	public void testGetForm() throws Exception {
		Form form = _formManager.getForm(2677L);
		assertNotNull(form);
		assertEquals(2677L, form.getId());
		assertEquals("Oettam", form.getName());
		assertEquals("basic", form.getCampagna());
		testFormData(form.getData());
		assertEquals(true, form.getDelivered());
		assertEquals( LocalDateTime.of(2024, Month.SEPTEMBER,5,10,30), form.getSubmitted());
		assertEquals(form.getSeriale(), "AAAAbbAeAR4Akl1234");

	}


	@Test
	public void testGetForms() throws Exception {
		List<Long> ids = _formManager.getForms();
		assertNotNull(ids);
		assertFalse(ids.isEmpty());
	}



	@Test
	public void testAddDeleteForm() throws Exception {
		Form form = new Form();

		form.setName("Platone");
		form.setCampagna("basic");
		form.setSubmitted(TODAY);
		form.setDelivered(true);
		form.setData(getFormDataForTest());


		_formManager.addForm(form);

		assertNotNull(form.getId());

		Form verify = _formManager.getForm(form.getId());

		final Long id = verify.getId();

		assertNotNull(id);
		assertEquals(2678L, id);
		assertEquals("Platone", verify.getName());
		assertEquals("basic", verify.getCampagna());
		testFormData(verify.getData());
		assertEquals(verify.getSubmitted().toLocalDate(), LocalDateTime.now().toLocalDate());
		assertEquals(true, verify.getDelivered());


		_formManager.deleteForm(id);

		verify = _formManager.getForm(id);
		
		assertNull(verify);
	}


	private void testFormData(FormData data) {

		assertNotNull(data);

		assertEquals("setValore1", data.valore1);
		assertEquals("setValore2", data.valore2);
		assertEquals("setValore3", data.valore3);
		assertEquals("setValore4", data.valore4);
		assertEquals("setValore5", data.valore5);

		assertEquals("setTesto1", data.testo1);
		assertEquals("setTesto2", data.testo2);
		assertEquals("setTesto3", data.testo3);
		assertEquals("setTesto4", data.testo4);
		assertEquals("setTesto5", data.testo5);

		assertEquals("setEtichettaSel1", data.etichettaSel1);
		assertEquals("setEtichettaSel3", data.etichettaSel2);

		assertNull(data.etichettaSel3);

		assertEquals("setEtichettaSel4", data.etichettaSel4);
		assertEquals("setEtichettaSel5", data.etichettaSel5);

		assertEquals("setEtichetta1", data.etichetta1);
		assertEquals("setEtichetta2", data.etichetta2);
		assertEquals("setEtichetta3", data.etichetta3);
		assertEquals("setEtichetta4", data.etichetta4);
		assertEquals("setEtichetta5", data.etichetta5);

	}

	public static FormData getFormDataForTest() {
		FormData fd = new FormData();

		fd.setValore1("setValore1");
		fd.setValore2("setValore2");
		fd.setValore3("setValore3");
		fd.setValore4("setValore4");
		fd.setValore5("setValore5");

		fd.setTesto1("setTesto1");
		fd.setTesto2("setTesto2");
		fd.setTesto3("setTesto3");
		fd.setTesto4("setTesto4");
		fd.setTesto5("setTesto5");

		fd.setEtichettaSel1("setEtichettaSel1");
		fd.setEtichettaSel2("setEtichettaSel3");
		fd.setEtichettaSel3(null);
		fd.setEtichettaSel4("setEtichettaSel4");
		fd.setEtichettaSel5("setEtichettaSel5");

		fd.setEtichetta1("setEtichetta1");
		fd.setEtichetta2("setEtichetta2");
		fd.setEtichetta3("setEtichetta3");
		fd.setEtichetta4("setEtichetta4");
		fd.setEtichetta5("setEtichetta5");

		return fd;
	}

	@Test
	public void testFilter() throws Exception {
		final Date to = new Date();
		final LocalDate currentDate = LocalDate.now();
		final LocalDate fromLd = currentDate.minusYears(10);
		Date from = Date.from(fromLd.atStartOfDay(ZoneId.systemDefault()).toInstant());

		// search by date
		FieldSearchFilter dateFilter = new FieldSearchFilter("submitted", from, to);
		List<Long> dateRecords = _formManager.search(new FieldSearchFilter[]{dateFilter});
		assertNotNull(dateRecords);
		assertFalse(dateRecords.isEmpty());
		assertEquals(2677L, dateRecords.get(0));

		// search by name
		FieldSearchFilter nameFilter = new FieldSearchFilter("name", "Oettam", false);
		List<Long> nameRecords = _formManager.search(new FieldSearchFilter[]{nameFilter});
		assertNotNull(nameRecords);
		assertFalse(nameRecords.isEmpty());
		assertEquals(2677L, nameRecords.get(0));

		// search by delivered
		FieldSearchFilter deliveredFilter = new FieldSearchFilter("delivered", Boolean.TRUE, false);
		List<Long> deliveredRecords = _formManager.search(new FieldSearchFilter[]{deliveredFilter});
		assertNotNull(deliveredRecords);
		assertFalse(deliveredRecords.isEmpty());
		assertEquals(2677L, deliveredRecords.get(0));

	}

	@Test
	public void randomHashTest() throws ApsSystemException {

		Form form = _formManager.getForm(2677L);

		System.out.println(form.getSeriale());

		assertNotNull(form.getSeriale());



	}

/*	private Form getFormForTest() {
		Form form = new Form();

		form.setId(2677L);
		form.setName("Plinio");
		form.setCampagna("basic");
		form.setSubmitted(TODAY);
		form.setDelivered(false);
		form.setData(getFormDataForTest());
		form.setSeriale(this.generateRandomHash2());

		return form;
	}*/

/*	private String generateRandomHash2() {
		return RandomStringUtils.randomAlphanumeric(18);
	}*/

	private IFormManager _formManager;
	private IMailManager _mailManager;
}
