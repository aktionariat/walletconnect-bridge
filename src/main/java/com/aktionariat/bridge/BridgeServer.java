/**
 * Created by Luzius Meisser on 2021-03-10
 * Copyright: Aktionariat AG, Zurich
 * Contact: luzius@aktionariat.com
 *
 * Feel free to reuse this code under the MIT License
 * https://opensource.org/licenses/MIT
 */
package com.aktionariat.bridge;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class BridgeServer extends WebSocketServer {

	private static final String NAME = "WalletConnect Bridge Java Edition 0.1";
	
	private HashMap<String, Bridge> bridges;
	private boolean startAttemptCompleted;
	private BindException error;

	public BridgeServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.startAttemptCompleted = false;
		this.bridges = new HashMap<String, Bridge>();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println(conn.getRemoteSocketAddress().getAddress() + " opened a connection to us");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn.getRemoteSocketAddress() + " closed the connection, status " + code);
	}

	@Override
	public void onWebsocketPong(WebSocket conn, Framedata f) {
		super.onWebsocketPong(conn, f);
		Bridge bridge = (Bridge) conn.getAttachment();
		if (bridge != null) {
			bridge.ack();
		}
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		try {
			System.out.println("Received " + message + " from " + conn.getRemoteSocketAddress().getAddress());
			WalletConnectMessage msg = WalletConnectMessage.parse(message);
			Bridge bridge = obtainBridge(msg.topic);
			switch (msg.type) {
			case "pub":
				bridge.push(message);
				break;
			case "sub":
				bridge.sub(conn);
				break;
			case "ack":
				bridge.ack();
				break;
			}
		} catch (IOException e) {
			System.out.println("Error: " + e);
			conn.close(200, "Error: " + e.getMessage());
		}
	}

	private synchronized Bridge obtainBridge(String topic) {
		Bridge b = bridges.get(topic);
		if (b == null) {
			b = new Bridge();
			bridges.put(topic, b);
		}
		return b;
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		onMessage(conn, new String(message.array()));
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		if (ex instanceof BindException) {
			synchronized (this) {
				this.startAttemptCompleted = true;
				this.error = (BindException) ex;
				this.notifyAll();
			}
		} else {
			ex.printStackTrace();
			if (conn != null) {
				conn.close(1011, ex.getMessage());
			}
		}
	}

	@Override
	public synchronized void onStart() {
		this.startAttemptCompleted = true;
		this.notifyAll();
	}

	public synchronized void waitForStart() throws BindException, InterruptedException {
		while (!startAttemptCompleted) {
			this.wait();
		}
		if (this.error != null) {
			throw error;
		}
	}

	public synchronized void purgeInactiveConnections() {
		long t0 = System.nanoTime();
		System.out.println("Purging inactive connections...");
		int count = 0;
		Iterator<Bridge> bridges = this.bridges.values().iterator();
		while (bridges.hasNext()) {
			Bridge bridge = bridges.next();
			if (bridge.isInactive()) {
				bridge.dispose();
				bridges.remove();
				count++;
			}
		}
		System.out.println("... purge of " + count + " completed in " + (System.nanoTime() - t0) / 1000 / 1000 + "ms.");
	}

	private static int findPort(String[] args) {
		int i = Arrays.asList(args).indexOf("-p");
		if (i >= 0) {
			return Integer.parseInt(args[i + 1]);
		} else {
			return 8887;
		}
	}
	
	public static BridgeServer startWithRetries(int port) throws UnknownHostException, InterruptedException {
		while (true) {
			try {
				BridgeServer server = new BridgeServer(port);
				server.start();
				server.waitForStart();
				return server;
			} catch (BindException e) {
				System.out.println("Port already in use, trying again in three seconds.");
				Thread.sleep(3000);
			}
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		BridgeServer s = startWithRetries(findPort(args));
		System.out.println(NAME + " started on port: " + s.getPort());

		try {
			while (true) {
				Thread.sleep(120 * 1000);
				s.purgeInactiveConnections();
			}
		} finally {
			s.stop(); // does not seem to work
		}
	}

}