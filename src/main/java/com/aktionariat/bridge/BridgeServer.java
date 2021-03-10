package com.aktionariat.bridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class BridgeServer extends WebSocketServer {

	private HashMap<String, Bridge> bridges;

	public BridgeServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.bridges = new HashMap<String, Bridge>();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println(conn.getRemoteSocketAddress().getAddress() + " opened a connection to us");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn.getRemoteSocketAddress().getAddress() + " closed the connection.");
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
		ex.printStackTrace();
		if (conn != null) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		BridgeServer s = new BridgeServer(8887);
		s.start();
		System.out.println("WalletConnect bridge started on port: " + s.getPort());

		try {
			while (true) {
				Thread.sleep(1000);
			}
		} finally {
			s.stop(500);
		}
	}

}