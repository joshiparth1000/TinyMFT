package com.pjoshi.tinymft.ssh;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.DirectoryHandle;
import org.apache.sshd.server.subsystem.sftp.FileHandle;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.HibernateUtil;
import com.pjoshi.tinymft.Settings;
import com.pjoshi.tinymft.xfer.Transfer;

public class SSHDSFTPListenerImpl implements SftpEventListener {
	private static Logger logger = LoggerFactory.getLogger(SSHDSFTPListenerImpl.class);
	
	public void initialized(ServerSession session, int version) {
		logger.debug("SFTP initialized");
	}
	
	public void destroying(ServerSession session) {
		logger.debug("SFTP destroyed");
		
		com.pjoshi.tinymft.jaas.Session jaassession = 
				HibernateUtil.getSession(Base64.getEncoder().encodeToString(session.getSessionId()));
		
		List<Transfer> transfers = HibernateUtil.getTransfers(jaassession);
		
		for(Transfer transfer : transfers)
			transfer.setState(Transfer.STATE_ABORTED);
	}
	
	public void opening(ServerSession session, String remoteHandle, Handle localHandle) throws IOException {
		logger.debug("SFTP opening : " + localHandle.getFile());
	}
	
	public void open(ServerSession session, String remoteHandle, Handle localHandle) throws IOException {
		logger.debug("SFTP open : " + localHandle.getFile());
	}
	
	public void openFailed(ServerSession session, String remotePath, Path localPath, boolean isDirectory, Throwable thrown) throws IOException {
		logger.debug("SFTP open failed : " + localPath);
	}
	
	public void read(ServerSession session, String remoteHandle, DirectoryHandle localHandle, Map<String, Path> entries) throws IOException {
		logger.debug("SFTP file listing : " + localHandle.getFile());
	}
	
	public void reading(ServerSession session, String remoteHandle, FileHandle localHandle, 
			long offset, byte[] data, int dataOffset, int dataLen) throws IOException {
		logger.debug("SFTP reading");
		
		Transfer transfer = HibernateUtil.getTransfer(remoteHandle);
		if(transfer != null) {
			if(!transfer.getState().equals(Transfer.STATE_INPROGRESS)) {
				logger.debug(transfer.getAction() + " inprogress : " + transfer.getFile());
				transfer.setState(Transfer.STATE_INPROGRESS);
			}
		}
		else {
			com.pjoshi.tinymft.jaas.Session jaassession = 
					HibernateUtil.getSession(Base64.getEncoder().encodeToString(session.getSessionId()));
			
			transfer = new Transfer();
	        transfer.setId(remoteHandle);
	        transfer.setSession(jaassession);
	        transfer.setFilename(localHandle.getFile().toString());
	        transfer.setAction(Transfer.ACTION_DOWNLOAD);
	        transfer.setFile(Settings.ROOTFOLDER + File.separator + session.getUsername() + localHandle.getFile().toString());
			transfer.setFilesize(new File(transfer.getFile()).length());
			
			logger.debug("Starting " + transfer.getAction() + " : " + localHandle.getFile());
			transfer.setState(Transfer.STATE_STARTED);
		}
	}
	
	public void read(ServerSession session, String remoteHandle, FileHandle localHandle, 
			long offset, byte[] data, int dataOffset, int dataLen, int readLen, Throwable thrown) throws IOException {
		logger.debug("SFTP read2 session id : " + Base64.getEncoder().encodeToString(session.getSessionId()));
		logger.debug("SFTP read2 remoteHandle : " + remoteHandle);
		logger.debug("SFTP read2 localHandle : " + localHandle.getFile());
		logger.debug("SFTP read2 offset : " + offset);
		logger.debug("SFTP read2 data length : " + data.length);
		logger.debug("SFTP read2 dataOffset : " + dataOffset);
		logger.debug("SFTP read2 dataLen : " + dataLen);
		logger.debug("SFTP read2 readLen : " + readLen);
		
		Transfer transfer = HibernateUtil.getTransfer(remoteHandle);
		if(transfer != null && readLen != -1) {
			transfer.setTransmittedbytes(transfer.getTransmittedbytes() + readLen);
		}
	}
	
	public void writing(ServerSession session, String remoteHandle, FileHandle localHandle, 
			long offset, byte[] data, int dataOffset, int dataLen) throws IOException {
		logger.debug("SFTP writing");
		
		Transfer transfer = HibernateUtil.getTransfer(remoteHandle);
		if(transfer != null) {
			if(!transfer.getState().equals(Transfer.STATE_INPROGRESS)) {
				logger.debug(transfer.getAction() + " inprogress : " + transfer.getFile());
				transfer.setState(Transfer.STATE_INPROGRESS);
			}
		}
		else {
			com.pjoshi.tinymft.jaas.Session jaassession = 
					HibernateUtil.getSession(Base64.getEncoder().encodeToString(session.getSessionId()));
			
			transfer = new Transfer();
	        transfer.setId(remoteHandle);
	        transfer.setSession(jaassession);
	        transfer.setFilename(localHandle.getFile().toString());
	        transfer.setAction(Transfer.ACTION_UPLOAD);
	        transfer.setFile(Settings.ROOTFOLDER + File.separator + session.getUsername() + localHandle.getFile().toString());
			
			logger.debug("Starting " + transfer.getAction() + " : " + localHandle.getFile());
			transfer.setState(Transfer.STATE_STARTED);
		}
	}
	
	public void written(ServerSession session, String remoteHandle, FileHandle localHandle, 
			long offset, byte[] data, int dataOffset, int dataLen, Throwable thrown) throws IOException {
		logger.debug("SFTP written session id : " + Base64.getEncoder().encodeToString(session.getSessionId()));
		logger.debug("SFTP written remoteHandle : " + remoteHandle);
		logger.debug("SFTP written localHandle : " + localHandle.getFile());
		logger.debug("SFTP written offset : " + offset);
		logger.debug("SFTP written data length : " + data.length);
		logger.debug("SFTP written dataOffset : " + dataOffset);
		logger.debug("SFTP written dataLen : " + dataLen);
		
		Transfer transfer = HibernateUtil.getTransfer(remoteHandle);
		if(transfer != null) {
			transfer.setTransmittedbytes(transfer.getTransmittedbytes() + dataLen);
			transfer.setFilesize(transfer.getFilesize() + dataLen);
		}
	}
	
	public void blocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask) throws IOException {
		logger.debug("SFTP blocking");
	}
	
	public void blocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask, Throwable thrown) throws IOException {
		logger.debug("SFTP blocked");
	}
	
	public void unblocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length) throws IOException {
		logger.debug("SFTP unblocking");
	}
	
	public void unblocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, Throwable thrown) throws IOException {
		logger.debug("SFTP unblocked");
	}
	
	public void closing(ServerSession session, String remoteHandle, Handle localHandle) {
		logger.debug("SFTP closing : " + localHandle.getFile());
	}
	
	public void closed(ServerSession session, String remoteHandle, Handle localHandle, Throwable thrown) {
		Transfer transfer = HibernateUtil.getTransfer(remoteHandle);
		if(transfer != null) {
			if(transfer.getAction().equals(Transfer.ACTION_DOWNLOAD) && 
					transfer.getFilesize() != transfer.getTransmittedbytes()) {
				logger.debug(transfer.getAction() + " aborted : " + transfer.getFile());
				transfer.setState(Transfer.STATE_ABORTED);
			}
			else {
				logger.debug(transfer.getAction() + " completed : " + transfer.getFile());
				transfer.setState(Transfer.STATE_ENDED);
			}
		}
		
		logger.debug("SFTP closed : " + localHandle.getFile());
	}
	
	public void creating(ServerSession session, Path path, Map<String, ?> attrs) throws IOException {
		logger.debug("SFTP creating");
	}
	
	public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) throws IOException {
		logger.debug("SFTP created : " + path);
	}
	
	public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts) throws IOException {
		logger.debug("SFTP moving");
	}
	
	public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) throws IOException {
		logger.debug("SFTP moved");
	}
	
	public void removing(ServerSession session, Path path) throws IOException {
		logger.debug("SFTP removing");
	}
	
	public void removed(ServerSession session, Path path, Throwable thrown) throws IOException {
		logger.debug("SFTP removed : " + path);
	}
	
	public void linking(ServerSession session, Path source, Path target, boolean symLink) throws IOException {
		logger.debug("SFTP linking");
	}
	
	public void linked(ServerSession session, Path source, Path target, boolean symLink, Throwable thrown) throws IOException {
		logger.debug("SFTP linked");
	}
	
	public void modifyingAttributes(ServerSession session, Path path, Map<String, ?> attrs) throws IOException {
		logger.debug("SFTP modifying attributes");
	}
	
	public void modifiedAttributes(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) throws IOException {
		logger.debug("SFTP modified attributes");
	}
}
