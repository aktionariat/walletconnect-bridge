package com.aktionariat.bridge;

import java.util.ArrayList;
import java.util.Date;

import org.java_websocket.WebSocket;

public class Bridge {
	
	private long creation = System.currentTimeMillis();
	
	private WebSocket subscriber; // assume only one subscriber for simplicity
	private ArrayList<String> messages;

	public Bridge() {
		this.messages = new ArrayList<String>();
	}

	public synchronized void push(String msg) {
		this.messages.add(msg);
		if (this.subscriber != null) {
			this.subscriber.send(msg);
		}
	}

	public synchronized void sub(WebSocket conn) {
		if (this.subscriber != null && this.subscriber != conn) {
			System.out.println("Replacing subscriber " + this.subscriber + " with  " + conn);
			this.subscriber.close(200, "Replaced by new subscriber");
		}
		this.subscriber = conn;
		for (String msg: messages) {
			subscriber.send(msg);
		}
	}

	public synchronized void ack() {
		// problem: if there are multiple messages, the ack might have been meant for an earlier one
		// this could be fixed by introducing sequence numbers
		this.messages.clear(); 
	}
	
	@Override
	public String toString() {
		return "Bridge to " + subscriber + " since " + new Date(creation);
	}

}
