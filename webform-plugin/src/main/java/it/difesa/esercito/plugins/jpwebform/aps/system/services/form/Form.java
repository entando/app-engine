/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.system.services.form;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.model.FormData;

import java.time.LocalDateTime;


public class Form {

	public String toJson() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();

		return mapper.writeValueAsString(this);
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getSubmitted() { //Date
		return submitted;
	}
	public void setSubmitted(LocalDateTime submitted) {
		this.submitted = submitted;
	}

	public FormData getData() {
		return data;
	}
	public void setData(FormData data) {
		this.data = data;
	}

	public String getQualifiedName() {
		return qualifiedName;
	}
	public void setQualifiedName(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	public String getCc() {
		return cc;
	}
	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Boolean getDelivered(){
		return delivered;
	}
	public void setDelivered(Boolean delivered){this.delivered=delivered;}

	public String getCampagna() {
		return campagna;
	}
	public void setCampagna(String campagna) {
		this.campagna = campagna;
	}

	private Long id;
	private String name;
	private LocalDateTime submitted;
	private FormData data;
	private String campagna;

	private String qualifiedName;
	private String cc;
	private String recipient;
	private String subject;
	private Boolean delivered;
}
