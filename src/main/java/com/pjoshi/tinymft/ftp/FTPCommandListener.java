package com.pjoshi.tinymft.ftp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.UUID;

import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DataType;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.message.MessageResourceFactory;
import org.apache.ftpserver.message.impl.DefaultMessageResource;
import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.HibernateUtil;
import com.pjoshi.tinymft.Settings;
import com.pjoshi.tinymft.xfer.Transfer;

public class FTPCommandListener extends DefaultFtplet {
	private static Logger logger = LoggerFactory.getLogger(FTPCommandListener.class);
	private static DefaultMessageResource messageresource = (DefaultMessageResource) new MessageResourceFactory().createMessageResource();
	
	private boolean isDataConnectionActive(FtpSession session, String command) throws FtpException {
		DataConnectionFactory connFactory = session.getDataConnection();
		if(connFactory instanceof IODataConnectionFactory) {
			InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();
            if (address == null)
            	throw new FtpException(String.valueOf(FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS));
		}
		
		return true;
	}
	
	private DataConnection getDataConnection(FtpSession session, String command) throws FtpException {
		DataConnection dataConnection = null;
		try {
            dataConnection = session.getDataConnection().openConnection();
        } catch (Exception e) {
        	logger.error("Exception getting the input data stream", e);
        	throw new FtpException(String.valueOf(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION));
        }
		
		return dataConnection;
	}
	
	private DefaultFtpReply getNegativeReply(FtpSession session, FtpException e) {
		DefaultFtpReply ftpreply = null;
		
		if(Integer.parseInt(e.getMessage()) == FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS)
			ftpreply = new DefaultFtpReply(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, 
					messageresource.getMessage(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
		                	e.getMessage(), session.getLanguage()));
		if(Integer.parseInt(e.getMessage()) == FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS)
			ftpreply = new DefaultFtpReply(FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                    "PORT or PASV must be issued first");
		if(Integer.parseInt(e.getMessage()) == FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN)
			ftpreply = new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, 
					messageresource.getMessage(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
		                	e.getMessage(), session.getLanguage()));
		if(Integer.parseInt(e.getMessage()) == FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION)
			ftpreply = new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, 
					messageresource.getMessage(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION,
							e.getMessage(), session.getLanguage()));
		if(Integer.parseInt(e.getMessage()) == FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED)
			ftpreply = new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, 
					messageresource.getMessage(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
							e.getMessage(), session.getLanguage()));
		if(Integer.parseInt(e.getMessage()) == FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN)
			ftpreply = new DefaultFtpReply(FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN, 
					messageresource.getMessage(FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
							e.getMessage(), session.getLanguage()));
		
		return ftpreply;
	}
	
	public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException {
		OutputStream outStream = null;
		Transfer transfer = null;
		String command = "STOR";
		
		try {
			long skipLen = session.getFileOffset();
			String fileName = request.getArgument();
			
			if (fileName == null)
				throw new FtpException(String.valueOf(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS));
			
			isDataConnectionActive(session, command);
			
			FtpFile file = null;
	        try {
	            file = session.getFileSystemView().getFile(fileName);
	        } catch (Exception ex) {
	            logger.error("Exception getting file object", ex);
	        }
	        
	        if (file == null)
	        	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
	        
	        com.pjoshi.tinymft.jaas.Session jaassession = HibernateUtil.getSession(session.getSessionId().toString());
	        transfer = new Transfer();
	        transfer.setId(UUID.randomUUID().toString());
	        transfer.setSession(jaassession);
	        transfer.setFilename(fileName);
	        transfer.setAction(Transfer.ACTION_UPLOAD);
	        transfer.setFile(Settings.ROOTFOLDER + File.separator + session.getUser().getName() + file.getAbsolutePath());	        
			transfer.setState(Transfer.STATE_STARTED);
	        
	        fileName = file.getAbsolutePath();
	        
	        if (!file.isWritable())
	        	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
	        
	        session.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, 
					messageresource.getMessage(FtpReply.REPLY_150_FILE_STATUS_OKAY,
		                	command, session.getLanguage())));
	        
	        DataConnection dataConnection = getDataConnection(session, command);
	        
	        boolean failure = false;
	        long transSz = 0L;
	        
	        try {
	        	transfer.setState(Transfer.STATE_INPROGRESS);
	        	outStream = file.createOutputStream(skipLen);
	        	transSz = dataConnection.transferFromClient(session, outStream);
	        
	        	if(outStream != null)
	        		outStream.close();
	        } catch (SocketException ex) {
	        	logger.error("Socket exception during data transfer", ex);
	            failure = true;
	            throw new FtpException(String.valueOf(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED));
	        } catch (IOException ex) {
	            logger.error("IOException during data transfer", ex);
	            failure = true;
	            throw new FtpException(String.valueOf(FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN));
	        }
	        
	        if (!failure) {
	            session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, 
	    				messageresource.getMessage(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
	    	                	command, session.getLanguage())));
	           
	            transfer.setFilesize(transSz);
	        	transfer.setTransmittedbytes(transSz);
	            transfer.setState(Transfer.STATE_ENDED);
	        }
		} catch(FtpException e) {
			DefaultFtpReply ftpreply = getNegativeReply(session, e);
			
			session.write(ftpreply);
			
			if(transfer != null) {
				File file = new File(transfer.getFile());
				if(file.exists()) {
					transfer.setTransmittedbytes(file.length());
					transfer.setFilesize(file.length());
				}
				transfer.setState(Transfer.STATE_ABORTED);
			}
		} finally {
			IoUtils.close(outStream);
			session.getDataConnection().closeDataConnection();
		}
        
		return FtpletResult.SKIP;
	}
	
	private FtpFile getUniqueFile(FtpSession session, FtpFile oldFile)
            throws FtpException {
        FtpFile newFile = oldFile;
        FileSystemView fsView = session.getFileSystemView();
        String fileName = newFile.getAbsolutePath();
        while (newFile.doesExist()) {
            newFile = fsView.getFile(fileName + '.'
                    + System.currentTimeMillis());
            if (newFile == null) {
                break;
            }
        }
        return newFile;
	}
	
	public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request) throws FtpException {
		OutputStream os = null;
		Transfer transfer = null;
		String command = "STOU";
		
		try {
			isDataConnectionActive(session, command);
			
			String pathName = request.getArgument();
			
			FtpFile file = null;
            try {
                String filePrefix;
                if (pathName == null) {
                    filePrefix = "ftp.dat";
                } else {
                    FtpFile dir = session.getFileSystemView().getFile(pathName);
                    if (dir.isDirectory())
                        filePrefix = pathName + "/ftp.dat";
                    else
                        filePrefix = pathName;
                }

                file = session.getFileSystemView().getFile(filePrefix);
                if (file != null)
                    file = getUniqueFile(session, file);
            } catch (Exception ex) {
                logger.error("Exception getting file object", ex);
            }
            
            if (file == null)
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            com.pjoshi.tinymft.jaas.Session jaassession = HibernateUtil.getSession(session.getSessionId().toString());
            String fileName = file.getAbsolutePath();
            transfer = new Transfer();
	        transfer.setId(UUID.randomUUID().toString());
	        transfer.setSession(jaassession);
	        transfer.setFilename(fileName);
	        transfer.setAction(Transfer.ACTION_UPLOAD);
	        transfer.setFile(Settings.ROOTFOLDER + File.separator + session.getUser().getName() + file.getAbsolutePath());
			transfer.setState(Transfer.STATE_STARTED);
            
            if (!file.isWritable())
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            session.write(new DefaultFtpReply(
            		FtpReply.REPLY_150_FILE_STATUS_OKAY, "FILE: " + fileName));
            
            boolean failure = false;
            
            DataConnection dataConnection = getDataConnection(session, command);
            
            long transSz = 0L;
            
            try {
            	transfer.setState(Transfer.STATE_INPROGRESS);
                os = file.createOutputStream(0L);
                transSz = dataConnection.transferFromClient(session, os);
                
                if(os != null) {
                    os.close();
                }
            } catch (SocketException ex) {
                logger.error("Socket exception during data transfer", ex);
                failure = true;
                throw new FtpException(String.valueOf(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED));
            } catch (IOException ex) {
                logger.error("IOException during data transfer", ex);
                failure = true;
                throw new FtpException(String.valueOf(FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN));
            }
            
            if (!failure) {
            	session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, 
	    				messageresource.getMessage(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
	    	                	command, session.getLanguage())));
            	
            	transfer.setFilesize(transSz);
	        	transfer.setTransmittedbytes(transSz);
	            transfer.setState(Transfer.STATE_ENDED);
            }
		} catch(FtpException e) {
			DefaultFtpReply ftpreply = getNegativeReply(session, e);
			
			session.write(ftpreply);
			
			if(transfer != null) {
				File file = new File(transfer.getFile());
				if(file.exists()) {
					transfer.setTransmittedbytes(file.length());
					transfer.setFilesize(file.length());
				}
				
				transfer.setState(Transfer.STATE_ABORTED);
			}
		} finally {
			IoUtils.close(os);
			session.getDataConnection().closeDataConnection();
		} 
		
		return FtpletResult.SKIP;
	}
	
	public FtpletResult onAppendStart(FtpSession session, FtpRequest request) throws FtpException {
		Transfer transfer = null;
		OutputStream os = null;
		long offset = 0L;
		String command = "APPE";
		
		try {
			String fileName = request.getArgument();
            if (fileName == null)
            	throw new FtpException(String.valueOf(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS));
            
            isDataConnectionActive(session, command);
			
			FtpFile file = null;
            try {
                file = session.getFileSystemView().getFile(fileName);
            } catch (Exception e) {
                logger.error("File system threw exception", e);
            }
            if (file == null)
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            com.pjoshi.tinymft.jaas.Session jaassession = HibernateUtil.getSession(session.getSessionId().toString());
            
            fileName = file.getAbsolutePath();
            
            transfer = new Transfer();
	        transfer.setId(UUID.randomUUID().toString());
	        transfer.setSession(jaassession);
	        transfer.setFilename(fileName);
	        transfer.setAction(Transfer.ACTION_UPLOAD);
	        transfer.setFile(Settings.ROOTFOLDER + File.separator + session.getUser().getName() + file.getAbsolutePath());
			transfer.setState(Transfer.STATE_STARTED);
            
            if (file.doesExist() && !file.isFile())
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            if (!file.isWritable())
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            session.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, 
					messageresource.getMessage(FtpReply.REPLY_150_FILE_STATUS_OKAY,
		                	command, session.getLanguage())));
            
            DataConnection dataConnection = getDataConnection(session, command);
            
            boolean failure = false;
            long transSz = 0L;
            
            try {
                if (file.doesExist())
                    offset = file.getSize();
                
                transfer.setState(Transfer.STATE_INPROGRESS);
                transfer.setFilesize(offset);
              
                os = file.createOutputStream(offset);
                transSz = dataConnection.transferFromClient(session, os);

                if(os != null)
                    os.close();
            } catch (SocketException e) {
                logger.error("SocketException during file upload", e);
                failure = true;
                throw new FtpException(String.valueOf(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED));
            } catch (IOException e) {
                logger.error("IOException during file upload", e);
                failure = true;
                throw new FtpException(String.valueOf(FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN));
            }
            
            if (!failure) {
            	session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, 
	    				messageresource.getMessage(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
	    	                	command, session.getLanguage())));
            	
            	transfer.setFilesize(new File(transfer.getFile()).length());
	        	transfer.setTransmittedbytes(transSz);
	            transfer.setState(Transfer.STATE_ENDED);
            }
		} catch(FtpException e) {
			DefaultFtpReply ftpreply = getNegativeReply(session, e);
			
			session.write(ftpreply);
			
			if(transfer != null) {
				File file = new File(transfer.getFile());
				if(file.exists()) {
					transfer.setTransmittedbytes(file.length() - offset);
					transfer.setFilesize(file.length());
				}
				transfer.setState(Transfer.STATE_ABORTED);
			}
		} finally {
			IoUtils.close(os);
			session.getDataConnection().closeDataConnection();
		}
		
		return FtpletResult.SKIP;
	}
	
	private InputStream openInputStream(FtpSession session, FtpFile file, long skipLen) throws IOException {
        InputStream in = null;
        if (session.getDataType() == DataType.ASCII) {
            int c;
            long offset = 0L;
            in = new BufferedInputStream(file.createInputStream(0L));
            while (offset++ < skipLen) {
                if ((c = in.read()) == -1) {
                    throw new IOException("Cannot skip");
                }
                if (c == '\n') {
                    offset++;
                }
            }
        } else {
            in = file.createInputStream(skipLen);
        }
        return in;
	}
	
	public FtpletResult onDownloadStart(FtpSession session, FtpRequest request) throws FtpException {
		String command = "RETR";
		InputStream is = null;
		Transfer transfer = null;
		
		try {
			long skipLen = session.getFileOffset();
			
			String fileName = request.getArgument();
            if (fileName == null)
            	throw new FtpException(String.valueOf(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS));
            
            FtpFile file = null;
            try {
                file = session.getFileSystemView().getFile(fileName);
            } catch (Exception ex) {
                logger.error("Exception getting file object", ex);
            }
            if (file == null)
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            com.pjoshi.tinymft.jaas.Session jaassession = HibernateUtil.getSession(session.getSessionId().toString());
            
            transfer = new Transfer();
	        transfer.setId(UUID.randomUUID().toString());
	        transfer.setSession(jaassession);
	        transfer.setFilename(fileName);
	        transfer.setAction(Transfer.ACTION_DOWNLOAD);
	        transfer.setFile(Settings.ROOTFOLDER + File.separator + session.getUser().getName() + file.getAbsolutePath());
			transfer.setState(Transfer.STATE_STARTED);
            
            fileName = file.getAbsolutePath();
            
            if (!file.doesExist())
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
           
            if (!file.isFile())
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            if (!file.isReadable())
            	throw new FtpException(String.valueOf(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN));
            
            isDataConnectionActive(session, command);
            
            session.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, 
					messageresource.getMessage(FtpReply.REPLY_150_FILE_STATUS_OKAY,
		                	command, session.getLanguage())));
            
            boolean failure = false;
            DataConnection dataConnection = getDataConnection(session, command);
            long transSz = 0L;
            
            try {
            	transfer.setState(Transfer.STATE_INPROGRESS);
                is = openInputStream(session, file, skipLen);

                transSz = dataConnection.transferToClient(session, is);
                if(is != null)
                    is.close();
            } catch (SocketException e) {
                logger.error("Socket exception during data transfer", e);
                failure = true;
                throw new FtpException(String.valueOf(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED));
            } catch (IOException e) {
                logger.error("IOException during data transfer", e);
                failure = true;
                throw new FtpException(String.valueOf(FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN));
            }
            
            if (!failure) {
            	session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, 
	    				messageresource.getMessage(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
	    	                	command, session.getLanguage())));
            	
            	transfer.setFilesize(transSz);
	        	transfer.setTransmittedbytes(transSz);
	            transfer.setState(Transfer.STATE_ENDED);
            }
		}  catch(FtpException e) {
			DefaultFtpReply ftpreply = getNegativeReply(session, e);
			
			session.write(ftpreply);
			
			if(transfer != null) {
				File file = new File(transfer.getFile());
				if(file.exists()) {
					transfer.setTransmittedbytes(file.length());
					transfer.setFilesize(file.length());
				}
				transfer.setState(Transfer.STATE_ABORTED);
			}
		} finally {
			IoUtils.close(is);
			session.getDataConnection().closeDataConnection();
		}
		
		return FtpletResult.SKIP;
	}
	
	public FtpletResult onLogin(FtpSession session, FtpRequest request) {
		InetAddress inetAddress = session.getClientAddress().getAddress();
		
		com.pjoshi.tinymft.jaas.Session jaassession = new com.pjoshi.tinymft.jaas.Session();
		jaassession.setId(session.getSessionId().toString());
		jaassession.setProtocol("ftp");
		jaassession.setUsername(session.getUser().getName());
		jaassession.setIp(inetAddress.getHostAddress());
		
		HibernateUtil.save(jaassession);
		
		return FtpletResult.DEFAULT;
	}
	
	public FtpletResult onDisconnect(FtpSession session) {
		com.pjoshi.tinymft.jaas.Session jaassession = HibernateUtil.getSession(session.getSessionId().toString());
		
		if(jaassession != null)
			HibernateUtil.delete(jaassession);
		
		return FtpletResult.DEFAULT;
	}
}
