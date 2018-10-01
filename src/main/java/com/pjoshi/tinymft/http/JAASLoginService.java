package com.pjoshi.tinymft.http;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.HibernateUtil;
import com.pjoshi.tinymft.Settings;
import com.pjoshi.tinymft.jaas.AuthCallbackHandler;
import com.pjoshi.tinymft.jaas.Module;
import com.sun.security.auth.UserPrincipal;

public class JAASLoginService implements LoginService {
	private static Logger logger = LoggerFactory.getLogger(JAASLoginService.class);
	private IdentityService identityService=new DefaultIdentityService();
	
	@Override
	public IdentityService getIdentityService() {
		return identityService;
	}

	@Override
	public String getName() {
		return Settings.JAASDOMAIN;
	}

	@Override
	public UserIdentity login(String userName, Object info, ServletRequest request) {
		X509Certificate []x509certs = null;
		HttpServletRequest req = (HttpServletRequest) request;
		
		if(Settings.DUALAUTH)
			x509certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
		
		try {
			Credential credential = (info instanceof Credential)?(Credential)info:Credential.getCredential(info.toString());
			LoginContext lc = new LoginContext(Settings.JAASDOMAIN, new AuthCallbackHandler(userName, info.toString(), 
					null, Module.HTTP, x509certs));
			lc.login();
			
			com.pjoshi.tinymft.jaas.Session jaassession = new com.pjoshi.tinymft.jaas.Session();
			jaassession.setId(req.getSession().getId());
			jaassession.setProtocol("http");
			jaassession.setUsername(userName);
			jaassession.setIp(req.getRemoteAddr());
			
			HibernateUtil.save(jaassession);
			
			return getUserIdentity(userName, lc.getSubject(), credential);
		} catch (LoginException | CertificateException | IOException e) {
			logger.error("Error authenticating : ", e);
		}
		
		return null;
	}

	@Override
	public void logout(UserIdentity user) {
		
	}

	@Override
	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

	@Override
	public boolean validate(UserIdentity user) {
        return false;
	}
	
	public synchronized UserIdentity getUserIdentity(String userName, Subject subject, Credential credential)
    {
        Principal userPrincipal = new UserPrincipal(userName);       
        UserIdentity identity = identityService.newUserIdentity(subject, userPrincipal,new String[] {"user"});
        
        return identity;
    }
}
