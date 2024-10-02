/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.exception.ApsSystemException;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.model.FormData;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.mail.IMailManager;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;

import static it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager.BEAN_ID;
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
	@Order(1)
	public void testListSearchFormByDateAfterBefore() throws ApsSystemException {

		Form form0=new Form();
		Form form1=new Form();
		Form form2=new Form();
		Form form3=new Form();
		Form form4=new Form();
		Form form5=new Form();
		Form form6=new Form();

		form0.setId(2678L);
		form0.setName("Romolo");
		form0.setSubmitted(LocalDateTime.parse("2024-05-09T05:28:15.000000")); //2024-05-09T05:28:15.000000
		form0.setDelivered(true);
		form0.setData(getFormDataForTest());

		form1.setId(2679L);
		form1.setName("Numa Pompilio");
		form1.setSubmitted(LocalDateTime.parse("2024-05-09T23:01:45.000000"));
		form1.setDelivered(true);
		form1.setData(getFormDataForTest());

		form2.setId(2680L);
		form2.setName("Tullo Ostilio");
		form2.setSubmitted(LocalDateTime.parse("2024-05-09T03:27:50.000000"));
		form2.setDelivered(false);
		form2.setData(getFormDataForTest());

		form3.setId(2681L);
		form3.setName("Anco Marzio");
		form3.setSubmitted(TODAY);
		form3.setDelivered(false);
		form3.setData(getFormDataForTest());

		form4.setId(2682L);
		form4.setName("Tarquinio Prisco");
		form4.setSubmitted(LocalDateTime.parse("2024-05-09T05:20:15.000000"));
		form4.setDelivered(true);
		form4.setData(getFormDataForTest());

		form5.setId(2683L);
		form5.setName("Servio Tullio");
		form5.setSubmitted(LocalDateTime.parse("2024-05-09T15:25:30.000000"));
		form5.setDelivered(false);
		form5.setData(getFormDataForTest());

		form6.setId(2684L);
		form6.setName("Tarquinio il superbo");
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



		List<Form> listSearchFormAfter1 = _formManager.searchByDateAfter(LDT_VERiFY, true);
		assertEquals(3,listSearchFormAfter1.size());

		List<Form> listSearchFormAfter2 = _formManager.searchByDateAfter(LDT_VERiFY, false);
		assertEquals(2,listSearchFormAfter2.size());

		List<Form> listSearchFormBefore1 = _formManager.searchByDateBefore(LDT_VERiFY, true);
		assertEquals(1,listSearchFormBefore1.size());

		List<Form> listSearchFormBefore2 = _formManager.searchByDateBefore(LDT_VERiFY, false);
		assertEquals(1,listSearchFormBefore2.size());

		_formManager.deleteForm(2678L);
		_formManager.deleteForm(2679L);
		_formManager.deleteForm(2680L);
		_formManager.deleteForm(2681L);
		_formManager.deleteForm(2682L);
		_formManager.deleteForm(2683L);
		_formManager.deleteForm(2684L);



	}

	@Test
	@Order(2)
	public void testGetForm() throws Exception {
		Form form = _formManager.getForm(2677L);
		assertNotNull(form);
		assertEquals(2677L, form.getId());
		assertEquals("Oettam", form.getName());
		testFormData(form.getData());
		assertEquals(true, form.getDelivered());
		assertEquals( LocalDateTime.of(2024, Month.SEPTEMBER,5,10,30), form.getSubmitted());


	}


	@Test
	@Order(3)
	public void testGetForms() throws Exception {
		List<Long> ids = _formManager.getForms();
		assertNotNull(ids);
		assertFalse(ids.isEmpty());
	}



	@Test
	@Order(4)
	public void testAddDeleteForm() throws Exception {
		Form form = new Form();

		form.setId(2678L);
		form.setName("Platone");
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
		testFormData(verify.getData());
		assertEquals(verify.getSubmitted().toLocalDate(), LocalDateTime.now().toLocalDate());
		assertEquals(true, verify.getDelivered());


		_formManager.deleteForm(2678L); //form.getId()
		verify = _formManager.getForm(2678L);//id
		assertNull(verify);


	}

	@Test
	@Order(5)
	public void testUpdateForm() throws ApsSystemException {

		Form form = new Form();

		form.setId(2678L);
		form.setName("UserUpdateFranco");
		form.setSubmitted(LocalDateTime.parse("2024-05-09T05:28:15.000000"));
		form.setDelivered(false);
		form.setData(getFormDataForTest());
		form.setRecipient("address@email.it");
		form.setCc("cc@email.it");
		form.setQualifiedName("Esq. John Doe");
		form.setSubject("mail subject");


		_formManager.addForm(form);
		assertEquals(false, form.getDelivered());


		_formManager.cronJob();

		Form formAfterUpdate =_formManager.getForm(2678L);
		assertEquals(true, formAfterUpdate.getDelivered());

		//System.out.println(formAfterUpdate.getId()+" "+formAfterUpdate.getName()+" "+formAfterUpdate.getDelivered()+" ");

		_formManager.deleteForm(2678L);

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

/*	public static Form getFormForTest() {
		Form form = new Form();

		form.setId(2677L);
		form.setName("Plinio");
		form.setSubmitted(TODAY);
		form.setDelivered(false);
		form.setData(getFormDataForTest());

		return form;
	}*/


	private IFormManager _formManager;
	private IMailManager _mailManager;
}
