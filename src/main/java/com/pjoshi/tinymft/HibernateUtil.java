package com.pjoshi.tinymft;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.xfer.Transfer;

public class HibernateUtil {
	private static SessionFactory sessionFactory;
	private static StandardServiceRegistry registry;
	private static Configuration configuration;
	private static Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
	
	private static SessionFactory getSessionFactory() {
		if(sessionFactory == null) {
			logger.debug("Creating hibernate sessionfactory");
			
			configuration = new Configuration();
			configuration.setProperty("hibernate.hbm2ddl.auto", "update");
			configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
			configuration.setProperty("hibernate.connection.username", "sa");
			configuration.setProperty("hibernate.connection.password", "");
			configuration.setProperty("hibernate.hbm2ddl.auto", "update");
			configuration.setProperty("hibernate.default_schema", "PUBLIC");
			configuration.setProperty("hibernate.connection.autocommit","false");
			configuration.addAnnotatedClass(Transfer.class);
			configuration.addAnnotatedClass(com.pjoshi.tinymft.jaas.Session.class);
			
			StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
			registryBuilder.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			registryBuilder.applySetting("hibernate.connection.url", "jdbc:h2:mem:TinyMFT;DB_CLOSE_DELAY=-1");
			registryBuilder.applySetting("hibernate.show_sql", "false");
	        registry = registryBuilder.build();
	        
	        sessionFactory = configuration.buildSessionFactory(registry);
		}
		
		return sessionFactory;
	}
	
	@SuppressWarnings("rawtypes")
	public static List<Transfer> getTransfers(com.pjoshi.tinymft.jaas.Session jaassession) {
		logger.debug("Fetching transfers for session: " + jaassession.getId());
		
		Session session = getSessionFactory().openSession();
		Criteria criteria = session.createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("session", jaassession));
		
		List result = criteria.list();
		session.close();
		
		List<Transfer> transfers = new ArrayList<Transfer>();
		
		for(Object obj : result)
			transfers.add((Transfer) obj);
		
		return transfers;
	}
	
	
	@SuppressWarnings("rawtypes")
	public static com.pjoshi.tinymft.jaas.Session getSession(String id) {
		logger.debug("Fetching session : "  + id);
		
		Session session = getSessionFactory().openSession();
		Criteria criteria = session.createCriteria(com.pjoshi.tinymft.jaas.Session.class);
		criteria.add(Restrictions.eq("id", id));
		List result = criteria.list();
		session.close();
		
		if(result.isEmpty())
			return null;
		
		return (com.pjoshi.tinymft.jaas.Session) result.get(0);
	}
	
	@SuppressWarnings("rawtypes")
	public static Transfer getTransfer(String id) {
		logger.debug("Fetching transfer : "  + id);
		
		Session session = getSessionFactory().openSession();
		Criteria criteria = session.createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("id", id));
		List result = criteria.list();
		session.close();
		
		if(result.isEmpty())
			return null;
		
		return (Transfer) result.get(0);
	}
	
	public static void save(Object obj) {
		logger.debug("Saving object of type " + obj.getClass().getName());
		
		Session session = getSessionFactory().openSession();
		session.beginTransaction();
		session.save(obj);
		session.getTransaction().commit();
		session.close();
	}
	
	public static void update(Object obj) {
		logger.debug("Updating object of type " + obj.getClass().getName());
		
		Session session = getSessionFactory().openSession();
		session.beginTransaction();
		session.update(obj);
		session.getTransaction().commit();
		session.close();
	}
	
	public static void delete(Object obj) {
		logger.debug("Deleting object of type " + obj.getClass().getName());
		
		Session session = getSessionFactory().openSession();
		session.beginTransaction();
		session.delete(obj);
		session.getTransaction().commit();
		session.close();
	}
	
	public static void shutdown() {
		if(registry != null) {
			logger.debug("Shutting down hibernate");
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}
}
