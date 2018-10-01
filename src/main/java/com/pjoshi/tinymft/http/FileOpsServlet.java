package com.pjoshi.tinymft.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpMethod;
import org.h2.util.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pjoshi.tinymft.HibernateUtil;
import com.pjoshi.tinymft.Settings;
import com.pjoshi.tinymft.jaas.Session;
import com.pjoshi.tinymft.xfer.Transfer;

public class FileOpsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FileOpsServlet.class);
	private static DiskFileItemFactory factory = new DiskFileItemFactory();
	
	public void init(ServletConfig config) {
		ServletContext servletContext = config.getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		String path = Settings.ROOTFOLDER + File.separator + request.getRemoteUser() + request.getRequestURI();
		
		if(File.separator.equals("\\"))
			path = path.replaceAll("/", "\\" + File.separator);
		
		File file = new File(path);
		Session session = HibernateUtil.getSession(request.getSession().getId());
		
		if(file.exists()) {
			if(file.isFile()) {
				Transfer transfer = new Transfer();
		        transfer.setId(UUID.randomUUID().toString());
		        transfer.setSession(session);
		        transfer.setFilename(file.getName());
		        transfer.setAction(Transfer.ACTION_DOWNLOAD);
		        transfer.setFile(file.getAbsolutePath());
				transfer.setState(Transfer.STATE_STARTED);
				response.setContentType("application/octet-stream");
				try {
					transfer.setState(Transfer.STATE_INPROGRESS);
					
					FileUtils.copyFile(file, response.getOutputStream());
					
					transfer.setFilesize(file.length());
		        	transfer.setTransmittedbytes(file.length());
		            transfer.setState(Transfer.STATE_ENDED);
				} catch (IOException e) {
					logger.error("IOException during data transfer", e);
					transfer.setState(Transfer.STATE_ABORTED);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
			else if(file.isDirectory()) {
				JSONArray list = new JSONArray();
				
				for(File child : file.listFiles()) {
					JSONObject json = new JSONObject();
					json.put("name", child.getName());
					json.put("file", child.isFile());
					json.put("directory", child.isDirectory());
					json.put("size", child.length());
					json.put("lastmodified", child.lastModified());
					
					list.put(json);
				}
				
				response.setContentType("application/json");
				try {
					IOUtils.copy(new ByteArrayInputStream(list.toString().getBytes()), response.getOutputStream());
				} catch (IOException e) {
					logger.error("IOException during data transfer", e);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		}
		else
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}
	
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		String path = Settings.ROOTFOLDER + File.separator + request.getRemoteUser() + request.getRequestURI();
		
		if(File.separator.equals("\\"))
			path = path.replaceAll("/", "\\" + File.separator);
		
		File file = new File(path);
		
		if(file.exists()) {
			if(file.isFile()) {
				if(!FileUtils.deleteQuietly(file))
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			
			if(file.isDirectory() && request.getRequestURI().length() > 1) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					logger.error("Error deleting directory : " + file.getAbsolutePath(), e);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		}
		else
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		if(ServletFileUpload.isMultipartContent(request)) {
			ServletFileUpload upload = new ServletFileUpload(factory);
			List<FileItem> items = null;
			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e) {
				logger.error("Error parsing request : ", e);
			}
			
			if(items == null || items.size() > 2)
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			else {
				JSONObject json = null;
				long transSz = 0L;
				InputStream is = null;
								
				Iterator<FileItem> iter = items.iterator();
				while (iter.hasNext()) {
				    FileItem item = iter.next();
				    
				    if(item.getContentType().equals("application/json"))
				        json = new JSONObject(item.getString());
				    else if(item.getContentType().equals("application/octet-stream")) {
				        transSz = item.getSize();
				        try {
							is = item.getInputStream();
						} catch (IOException e) {
							logger.error("Unable to open input stream : ", e);
						}
				    }
				}
				
				if(json == null)
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				else {
					String filename = json.has("filename") ? json.getString("filename") : null;
					String newfilename = json.has("newfilename") ? json.getString("newfilename") : null;
					boolean directory = json.has("directory") ? json.getBoolean("directory") : false;
					boolean overwrite = json.has("overwrite") ? json.getBoolean("overwrite") : false;
					boolean append = json.has("append") ? json.getBoolean("append") : false;
					
					if(filename == null)
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					else {
						if(is == null) {
							if(directory) {
								File dir = new File(Settings.ROOTFOLDER + File.separator + request.getRemoteUser() + request.getRequestURI() + File.separator + filename);
								if(!dir.exists()) {
									try {
										Files.createDirectories(dir.toPath());
									} catch (IOException e) {
										logger.error("Error creating directory for " + dir.getAbsolutePath(), e);
										response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									}
								}
								else
									response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
							}
							else {
								if(newfilename == null && filename.equals(newfilename))
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								else {
									File oldfile = new File(Settings.ROOTFOLDER + File.separator + request.getRemoteUser() + request.getRequestURI() + File.separator + filename);
									File newfile = new File(Settings.ROOTFOLDER + File.separator + request.getRemoteUser() + request.getRequestURI() + File.separator + newfilename);
									
									if(oldfile.exists()) {
										try {
											if(oldfile.isFile())
												FileUtils.moveFile(oldfile, newfile);
											if(oldfile.isDirectory())
												FileUtils.moveDirectory(oldfile, newfile);
										} catch(IOException e) {
											logger.error("Error renaming " + filename + " to " + newfilename, e);
											response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										}
									}
									else
										response.setStatus(HttpServletResponse.SC_NOT_FOUND);
								}
							}
						}
						else {
							File file = new File(Settings.ROOTFOLDER + File.separator + request.getRemoteUser() + request.getRequestURI() + File.separator + filename);
							
							if(overwrite && append)
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							else if(file.exists() && !overwrite)
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							else if(!file.exists() && append)
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							else {
								Session session = HibernateUtil.getSession(request.getSession().getId());
								Transfer transfer = new Transfer();
								transfer.setId(UUID.randomUUID().toString());
								transfer.setSession(session);
								transfer.setFilename(file.getName());
								transfer.setAction(Transfer.ACTION_UPLOAD);
								transfer.setFile(file.getAbsolutePath());
								transfer.setState(Transfer.STATE_STARTED);
								
								try {
									transfer.setState(Transfer.STATE_INPROGRESS);
									transfer.setTransmittedbytes(transSz);
									
									FileOutputStream fout = new FileOutputStream(file, append);
									IOUtils.copy(is, fout);
									fout.close();
									is.close();
									
									transfer.setFilesize(file.length());
									transfer.setState(Transfer.STATE_ENDED);
								} catch (IOException e) {
									logger.error("Error writing stream to file : ", e);
									transfer.setState(Transfer.STATE_ABORTED);
									response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								}
							}
						}
					}
				}
			}
		}
		else
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	public void service(ServletRequest req, ServletResponse res) {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		
		try {
			if(request.getMethod().equalsIgnoreCase(HttpMethod.GET.toString()))
				doGet(request, response);
			else if(request.getMethod().equalsIgnoreCase(HttpMethod.POST.toString()))
				doPost(request, response);
			else if(request.getMethod().equalsIgnoreCase(HttpMethod.DELETE.toString()))
				doDelete(request, response);
			else
				res.getWriter().println(HttpServletResponse.SC_METHOD_NOT_ALLOWED + " " + 
						request.getMethod() + " method is unsupported.");
		} catch(IOException e) {
			logger.error("Error executing action : ", e);
		} finally {
			HibernateUtil.delete(HibernateUtil.getSession(request.getSession().getId()));
		}
	}
}
