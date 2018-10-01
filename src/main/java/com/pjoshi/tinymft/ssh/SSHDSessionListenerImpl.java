package com.pjoshi.tinymft.ssh;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Map;

import org.apache.sshd.common.kex.KexProposalOption;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.HibernateUtil;

public class SSHDSessionListenerImpl implements SessionListener {
	private static Logger logger = LoggerFactory.getLogger(SSHDSessionListenerImpl.class);
	
	public void sessionCreated(Session session) {
		logger.debug("Session created");
	}
	
	public void sessionNegotiationStart(Session session, Map<KexProposalOption,
			String> clientProposal, Map<KexProposalOption, String> serverProposal) {
		logger.debug("Session negotiation started");
	}
	
	public void sessionNegotiationEnd(Session session, Map<KexProposalOption, String> clientProposal,
			Map<KexProposalOption, String> serverProposal, 
			Map<KexProposalOption, String> negotiatedOptions, Throwable reason) {
		logger.debug("Session negotation ended");
	}
	
	public void sessionEvent(Session session, Event event) {
		logger.debug("Session event : " + event.toString());
		
		if(event == Event.Authenticated) {
			InetSocketAddress socketAddress = (InetSocketAddress) session.getIoSession().getRemoteAddress();
			InetAddress inetAddress = socketAddress.getAddress();
			
			com.pjoshi.tinymft.jaas.Session jaassession = new com.pjoshi.tinymft.jaas.Session();
			jaassession.setId(Base64.getEncoder().encodeToString(session.getSessionId()));
			jaassession.setProtocol("ssh");
			jaassession.setUsername(session.getUsername());
			jaassession.setIp(inetAddress.getHostAddress());
			
			HibernateUtil.save(jaassession);
		}
	}
	
	public void sessionException(Session session, Throwable t) {
		logger.debug("Session exception");
	}
	
	public void sessionClosed(Session session) {
		logger.debug("Session closed");
	}
}
