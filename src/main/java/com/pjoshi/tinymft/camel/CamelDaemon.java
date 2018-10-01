package com.pjoshi.tinymft.camel;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.Settings;

public class CamelDaemon {
	private static CamelContext context = null;
	private static FileAlterationMonitor monitor = null;
	public static HashMap<String, List<RouteDefinition>> routes = new HashMap<String, List<RouteDefinition>>();
	private static Logger logger = LoggerFactory.getLogger(CamelDaemon.class);
	
	public static void init() {
		context = new DefaultCamelContext();
		monitor = new FileAlterationMonitor();
		
		FileAlterationObserver fao = new FileAlterationObserver(Settings.ROUTESFOLDER);
        fao.addListener(new RouteFolderListenerImpl());
        monitor.addObserver(fao);
	}
	
	public static void start() throws Exception {
		logger.info("Starting Camel context");
		context.start();
		
		for(File file : new File(Settings.ROUTESFOLDER).listFiles()) {
			if(!file.isFile())
				continue;
			
			try {
				RoutesDefinition rds = context.loadRoutesDefinition(new FileInputStream(file));
				context.addRouteDefinitions(rds.getRoutes());
				routes.put(file.getName(), rds.getRoutes());
			} catch (Exception e) {
				logger.error("Error loading routes : ", e);
			}
		}
		
		monitor.start();
	}
	
	public static void stop() throws Exception {
		logger.info("Stopping Camel context");
		monitor.stop();
		context.stop();
	}
	
	public static void sendBody(String target, String body) {
		context.createProducerTemplate().sendBody(target, body);
	}
	
	static class RouteFolderListenerImpl implements FileAlterationListener {
		private static Logger logger = LoggerFactory.getLogger(RouteFolderListenerImpl.class);

		@Override
		public void onDirectoryChange(File directory) {
			
		}

		@Override
		public void onDirectoryCreate(File directory) {

		}

		@Override
		public void onDirectoryDelete(File directory) {

		}

		@Override
		public void onFileChange(File file) {
			onFileDelete(file);
			onFileCreate(file);
		}

		@Override
		public void onFileCreate(File file) {
			try {
				logger.info("Loading routes in " + file.getName());
				
				RoutesDefinition rds = context.loadRoutesDefinition(new FileInputStream(file));
				context.addRouteDefinitions(rds.getRoutes());
				routes.put(file.getName(), rds.getRoutes());
			} catch (Exception e) {
				logger.error("Error loading routes : ", e);
			}
		}

		@Override
		public void onFileDelete(File file) {
			try {
				if(routes.get(file.getName()) != null) {
					for(RouteDefinition route : routes.get(file.getName())) {
						logger.info("Removing route " + route.getId() + " in " + file.getName());
						
						context.stopRoute(route.getId());
						context.removeRoute(route.getId());
					}
				
					routes.remove(file.getName());
				}
			} catch (Exception e) {
				logger.error("Error removing routes : ", e);
			}
		}

		@Override
		public void onStart(FileAlterationObserver arg0) {
			
		}

		@Override
		public void onStop(FileAlterationObserver arg0) {
			
		}
	}
}
