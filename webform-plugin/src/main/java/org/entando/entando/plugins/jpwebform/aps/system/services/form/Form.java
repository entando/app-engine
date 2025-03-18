/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.system.services.form;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.DeliveryData;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormConfiguration;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;


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

	public Boolean getDelivered(){
		return delivered;
	}
	public void setDelivered(Boolean delivered){this.delivered=delivered;}

	public String getCampaign() {
		return campaign;
	}
	public void setCampaign(String campaign) {
		this.campaign = campaign;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public FormConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(FormConfiguration configuration) {
		this.configuration = configuration;
	}

	public Boolean getHead() {
		return isHead;
	}

	public void setHead(Boolean head) {
		isHead = head;
	}

	public DeliveryData getDelivery() {
		return delivery;
	}

	public void setDelivery(DeliveryData delivery) {
		this.delivery = delivery;
	}

	public FormData getData() {
		return data;
	}

	public void setData(FormData data) {
		this.data = data;
	}

	private Long id;
	private String name;
	private LocalDateTime submitted;
	private FormData data;
	private String campaign;
	private Boolean delivered;
	private String serial;
	private FormConfiguration configuration;
	private Boolean isHead;
	private DeliveryData delivery;
	// TODO aggiungere gruppo!

}
