package com.aktionariat.bridge;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.slf4j.impl.SimpleLogger;

public class Launcher {
	
	private static void initLogger(){
		System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss.SSS");
	}
	
	public static void main(String[] args) throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, InterruptedException, IOException {
		initLogger();
		BridgeServer.main(args);
	}

}
