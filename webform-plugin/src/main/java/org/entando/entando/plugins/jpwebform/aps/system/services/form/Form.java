/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.system.services.form;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormPayload;

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

	public FormPayload getFormPayload() {
		return this.data;
	}

	public void setFormPayload(FormPayload data) {
		this.data = data;
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

	public String getSeriale() {
		return seriale;
	}

	public void setSeriale(String seriale) {
		this.seriale = seriale;
	}

	private Long id;
	private String name;
	private LocalDateTime submitted;
	private FormPayload data;
	private String campagna;
	private Boolean delivered;
	private String seriale;

}
