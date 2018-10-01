package com.pjoshi.tinymft;

import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.camel.CamelDaemon;

@XmlRootElement(name = "Event")
@XmlAccessorType(XmlAccessType.FIELD)
public class Event {
	private String id = null;
	private Date date = null;
	private String lastevent = null;
	private HashMap<String, String> properties = null;
	
	@XmlTransient
	private static Logger logger = LoggerFactory.getLogger(Event.class);
	
	@XmlTransient
	private static JAXBContext jaxbContext = null;
	
	@XmlTransient
	private static Marshaller jaxbMarshaller = null;
	
	static {
		try {
			jaxbContext = JAXBContext.newInstance(Event.class);
			jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		} catch (JAXBException e) {
			logger.error("Error creating JAXBContent : ", e);
		}
	}
	
	public Event() {
		id = UUID.randomUUID().toString();
		date = new Date();
		properties = new HashMap<String, String>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public HashMap<String, String> getProperties() {
		return properties;
	}

	public void setProperties(HashMap<String, String> properties) {
		this.properties = properties;
	}
	
	public String getLastevent() {
		return lastevent;
	}

	public void setLastevent(String lastevent) {
		this.lastevent = lastevent;
	}

	public void addProperty(String key, String value) {
		properties.put(key, value);
	}
	
	public void removeProperty(String key) {
		properties.remove(key);
	}
	
	public String send(String target) throws JAXBException {
		StringWriter sw = new StringWriter();
		jaxbMarshaller.marshal(this, sw);
		
		CamelDaemon.sendBody(target, sw.toString());
		
		return this.id;
	}
}
