/**
 * Created by Luzius Meisser on 2021-03-10
 * Copyright: Aktionariat AG, Zurich
 * Contact: luzius@aktionariat.com
 *
 * Feel free to reuse this code under the MIT License
 * https://opensource.org/licenses/MIT
 */
package com.aktionariat.bridge;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bridge {

	private static final Logger LOG = LoggerFactory.getLogger(Bridge.class);

	private static final long ACTIVE_PERIOD = TimeUnit.MINUTES.toNanos(30);

	private long lastActivity;

	private ArrayList<WebSocket> subscribers;
	private ArrayList<WalletConnectMessage> messages;

	public Bridge() {
		this.messages = new ArrayList<>();
		this.subscribers = new ArrayList<>();
		this.touch();
	}

	private void touch() {
		this.lastActivity = System.nanoTime();
	}

	public synchronized void push(WebSocket conn, WalletConnectMessage msg) {
		this.touch();
		LOG.info("Received " + msg);
		this.messages.add(msg);
		Iterator<WebSocket> subs = this.subscribers.iterator();
		while (subs.hasNext()) {
			WebSocket subscriber = subs.next();
			if (msg.shouldDeliverTo(subscriber)) {
				try {
					LOG.info("Deliviering " + msg + " from " + conn + " to " + subscriber.getRemoteSocketAddress());
					subscriber.send(msg.getJson());
//					subscriber.sendPing();
				} catch (WebsocketNotConnectedException e) {
					LOG.warn("Lost subscriber: " + subscriber.getRemoteSocketAddress());
					subs.remove();
				}
			}
		}
	}

	public synchronized void sub(WebSocket conn, WalletConnectMessage sub) {
		this.touch();
		try {
			LOG.info(conn.getRemoteSocketAddress() + " wants to know about " + sub.topic);
			for (WalletConnectMessage msg : messages) {
				if (msg.shouldDeliverTo(conn)) {
					LOG.info("Deliviering " + msg + " to " + conn.getRemoteSocketAddress());
					conn.send(msg.getJson());
				}
			}
			Set<Bridge> bridges = conn.getAttachment();
			if (bridges == null) {
				bridges = new HashSet<>();
				conn.setAttachment(bridges);
			}
			bridges.add(this);
//			conn.sendPing();
			this.subscribers.add(conn);
		} catch (WebsocketNotConnectedException e) {
			LOG.warn("Lost subscriber: " + conn.getRemoteSocketAddress());
		}
	}

	public synchronized void ack() {
		// problem: if there are multiple messages, the ack might have been meant for an
		// earlier one. this could be fixed by introducing sequence numbers
		this.touch();
		this.messages.clear();
	}

	public boolean isInactive() {
		return System.nanoTime() - this.lastActivity > ACTIVE_PERIOD;
	}

	public void dispose() {
		for (WebSocket socket : this.subscribers) {
			socket.close();
		}
		this.subscribers.clear();
	}

	@Override
	public String toString() {
		return "Bridge to " + this.subscribers.size() + " since " + new Date(lastActivity);
	}

}
