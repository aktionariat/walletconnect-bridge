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
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

public class Bridge {

	private static final long ACTIVE_PERIOD = TimeUnit.MINUTES.toNanos(30);

	private long lastActivity;

	private WebSocket subscriber; // assume only one subscriber for simplicity
	private ArrayList<String> messages;

	public Bridge() {
		this.messages = new ArrayList<String>();
		this.touch();
	}

	private void touch() {
		this.lastActivity = System.nanoTime();
	}

	public synchronized void push(String msg) {
		this.touch();
		this.messages.add(msg);
		if (this.subscriber != null) {
			try {
				this.subscriber.send(msg);
				this.subscriber.sendPing();
			} catch (WebsocketNotConnectedException e) {
				this.subscriber = null;
			}
		}
	}

	public synchronized WebSocket sub(WebSocket conn) {
		WebSocket replaced = this.subscriber;
		if (replaced != null && replaced != conn) {
			System.out.println("Replacing subscriber " + this.subscriber + " with  " + conn.getRemoteSocketAddress());
			replaced.close(1000, "Replaced by new subscriber");
		}
		this.touch();
		this.subscriber = conn;
		this.subscriber.setAttachment(this);
		for (String msg : messages) {
			subscriber.send(msg);
		}
		this.subscriber.sendPing();
		return replaced;
	}

	public synchronized void ack() {
		// problem: if there are multiple messages, the ack might have been meant for an earlier one
		// this could be fixed by introducing sequence numbers
		this.touch();
		this.messages.clear();
	}

	public boolean isInactive() {
		return System.nanoTime() - this.lastActivity > ACTIVE_PERIOD;
	}

	public WebSocket dispose() {
		WebSocket disposed = this.subscriber;
		if (disposed != null) {
			disposed.close();
			this.subscriber = null;
		}
		return disposed;
	}

	@Override
	public String toString() {
		return "Bridge to " + subscriber + " since " + new Date(lastActivity);
	}

}
