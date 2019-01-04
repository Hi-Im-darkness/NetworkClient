package com.forgetfilefast;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DownloadRunnable extends Thread {
	private String url;
	private byte[] storage;
	private int start;
	private int end;
	private CountDownLatch latch;
	private JProgressBar progressBar;
	
	DownloadRunnable() {}
	DownloadRunnable(String url, byte[] storage, int start, int end, CountDownLatch latch) {
		this.url = url;
		this.storage = storage;
		this.start = start;
		this.end = end;
		this.latch = latch;
		this.progressBar = null;
	}
	DownloadRunnable(String url, byte[] storage, int start, int end, CountDownLatch latch, JProgressBar progressBar) {
		this.url = url;
		this.storage = storage;
		this.start = start;
		this.end = end;
		this.latch = latch;
		this.progressBar = progressBar;
	}
		
	@Override
	public void run() {
		try {
			URL obj = new URL(this.url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			String range_str = String.format("bytes=%d-%d", this.start, this.end);
			con.setRequestProperty("Range", range_str);
			
			int responseCode = con.getResponseCode();
			if (responseCode == 416) {
				System.out.println("Requested Range Not Satisfiable");
			}
			InputStream in = con.getInputStream();
			in.read(this.storage, this.start, this.end - this.start + 1);
			if (this.progressBar != null) {
				synchronized (this.progressBar) {
					int val = this.progressBar.getValue();
					this.progressBar.setValue(val + 1);	
				}
			}
			latch.countDown();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
