package com.pjoshi.tinymft.jaas;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.pjoshi.tinymft.xfer.Transfer;

@Entity
@Table(name = "Session")
public class Session {
	@Id
	private String id;
	
	private String username;
	private String ip;
	private String protocol;
	
	@OneToMany(mappedBy="session", cascade={CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<Transfer> transfers;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public Set<Transfer> getTransfers() {
		return transfers;
	}
	public void setTransfers(Set<Transfer> transfers) {
		this.transfers = transfers;
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof Session) {
			Session session = (Session) obj;
			return this.id.equals(session.getId());
		}
		
		return false;
	}
	
	public int hashCode() {
		int hashcode = 0;
		
		for(int i=0;i<id.length();i++)
			hashcode += Character.getNumericValue(id.charAt(i));
		
		return hashcode;
	}
}
