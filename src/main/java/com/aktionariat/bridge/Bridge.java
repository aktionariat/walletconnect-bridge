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
			} catch (WebsocketNotConnectedException e) {
				this.subscriber = null;
			}
		}
	}

	public synchronized void sub(WebSocket conn) {
		if (this.subscriber != null && this.subscriber != conn) {
			System.out.println("Replacing subscriber " + this.subscriber + " with  " + conn);
			this.subscriber.close(1000, "Replaced by new subscriber");
		}
		this.touch();
		this.subscriber = conn;
		for (String msg : messages) {
			subscriber.send(msg);
		}
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

	public void dispose() {
		if (this.subscriber != null) {
			this.subscriber.close();
			this.subscriber = null;
		}
	}

	@Override
	public String toString() {
		return "Bridge to " + subscriber + " since " + new Date(lastActivity);
	}

}
