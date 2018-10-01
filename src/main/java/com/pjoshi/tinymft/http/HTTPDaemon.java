package com.pjoshi.tinymft.http;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.Settings;

public class HTTPDaemon {
	private static Server server = null;
	private static Logger logger = LoggerFactory.getLogger(HTTPDaemon.class);
	
	public static void init() {
		server = new Server();
		
		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(Settings.KEYSTORE);
		sslContextFactory.setKeyStorePassword(Settings.KEYPASSWORD);
		sslContextFactory.setKeyManagerPassword(Settings.KEYSTOREPASSWORD);
		sslContextFactory.setTrustStorePath(Settings.TRUSTSTORE);
		sslContextFactory.setTrustStorePassword(Settings.TRUSTSTOREPASSWORD);
		
		if(Settings.DUALAUTH)
			sslContextFactory.setNeedClientAuth(true);
		else
			sslContextFactory.setWantClientAuth(true);

		ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"),
		        new HttpConnectionFactory(https));
		sslConnector.setPort(Settings.HTTPPORT);
		sslConnector.setHost(Settings.BINDIP);

		server.setConnectors(new Connector[] {sslConnector});
		
		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ctx.setContextPath("/");
		
		Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");
		ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
		csh.setAuthenticator(new BasicAuthenticator());
		csh.setRealmName(Settings.JAASDOMAIN);
		csh.addConstraintMapping(cm);
		csh.setLoginService(new JAASLoginService());
		ctx.setSecurityHandler(csh);
		
		ServletHandler servletHandler = new ServletHandler();
		ServletHolder holder=new ServletHolder(new FileOpsServlet());
		servletHandler.addServletWithMapping(holder, "/");
		ctx.setServletHandler(servletHandler);
		
        server.setHandler(ctx);
	}
	
	public static void start() {
		logger.info("Starting HTTP daemon");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			logger.error("Unable to start HTTP daemon : ", e);
		}
	}
	
	public static void stop() {
		if(server.isRunning()) {
			logger.info("Stopping HTTP daemon");
			try {
				server.stop();
			} catch (Exception e) {
				logger.error("Unable to stop HTTP daemon : ", e);
			}
		}
	}
	
	public static boolean isStarted() {
		return server.isStarted();
	}
	
	public static boolean isStopped() {
		return server.isStopped();
	}
}
