package com.pjoshi.tinymft.xfer;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.Event;
import com.pjoshi.tinymft.HibernateUtil;

public class TransferListenerImpl implements TransferListener {
	private static Logger logger = LoggerFactory.getLogger(TransferListenerImpl.class);

	@Override
	public void onStart(Transfer transfer) {
		HibernateUtil.save(transfer);
		createEvent(transfer);
	}

	@Override
	public void Inprogress(Transfer transfer) {
		createEvent(transfer);
	}

	@Override
	public void onAbort(Transfer transfer) {
		HibernateUtil.delete(transfer);
		createEvent(transfer);
	}

	@Override
	public void onEnd(Transfer transfer) {
		HibernateUtil.delete(transfer);
		createEvent(transfer);
	}
	
	public void createEvent(Transfer transfer) {
		Event event = new Event();
		event.setLastevent(transfer.getLastevent());
		event.addProperty("session", transfer.getSession().getId());
		event.addProperty("id", transfer.getId());
		event.addProperty("action", transfer.getAction());
		event.addProperty("file", transfer.getFile());
		event.addProperty("username", transfer.getSession().getUsername());
		event.addProperty("filename", transfer.getFilename());
		event.addProperty("transmittedbytes", String.valueOf(transfer.getTransmittedbytes()));
		event.addProperty("filesize", String.valueOf(transfer.getFilesize()));
		event.addProperty("state", transfer.getState());

		try {
			transfer.setLastevent(event.send("seda:transferevent"));
		} catch (JAXBException e) {
			logger.error("Error writing event : ", e);
		}
	}

}
