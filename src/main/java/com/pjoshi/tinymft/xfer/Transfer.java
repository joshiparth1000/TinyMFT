package com.pjoshi.tinymft.xfer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.pjoshi.tinymft.jaas.Session;

@Entity
@Table(name = "Transfer")
public class Transfer {
	@ManyToOne
	@JoinColumn
	private Session session;
	
	@Id
	private String id;
	
	private String action;
	private String file;
	private String filename;
	private String state;
	private long transmittedbytes;
	private long filesize;
	private String lastevent;
	
	@Transient
	private List<TransferListener> listeners = new ArrayList<TransferListener>();
	
	@Transient
	public static final String STATE_STARTED = "STARTED";
	
	@Transient
	public static final String STATE_INPROGRESS = "INPROGRESS";
	
	@Transient
	public static final String STATE_ABORTED = "ABORTED";
	
	@Transient
	public static final String STATE_ENDED = "ENDED";
	
	@Transient
	public static final String ACTION_DOWNLOAD = "DOWNLOAD";
	
	@Transient
	public static final String ACTION_UPLOAD = "UPLOAD";
	
	public Transfer() {
		this.lastevent = "";
		this.addListener(new TransferListenerImpl());
	}
	
	public Session getSession() {
		return session;
	}
	public void setSession(Session session) {
		this.session = session;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
		
		if(File.separator.equals("\\"))
			this.file = file.replaceAll("/", "\\" + File.separator);
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
		
		for(TransferListener listener : listeners) {
			switch(this.state) {
			case STATE_STARTED:listener.onStart(this);break;
			case STATE_INPROGRESS:listener.Inprogress(this);break;
			case STATE_ABORTED:listener.onAbort(this);break;
			case STATE_ENDED:listener.onEnd(this);break;
			}
		}
	}
	public long getTransmittedbytes() {
		return transmittedbytes;
	}
	public void setTransmittedbytes(long transmittedbytes) {
		this.transmittedbytes = transmittedbytes;
		
		for(TransferListener listener : listeners)
			listener.Inprogress(this);
	}
	public String getLastevent() {
		return lastevent;
	}
	public void setLastevent(String lastevent) {
		this.lastevent = lastevent;
	}
	public long getFilesize() {
		return filesize;
	}
	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}
	public void addListener(TransferListener listener) {
		this.listeners.add(listener);
	}
	public void removeListener(TransferListener listener) {
		this.listeners.remove(listener);
	}
}
