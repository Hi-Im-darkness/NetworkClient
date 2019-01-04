package com.forgetfilefast;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CountDownLatch;


public class DownloadMission {
	public static final int READY = 1;
	public static final int DOWNLOADING = 2;
	public static final int ERROR = 3;
	public static final int FINISHED = 4;
	
	public static final int DEFAULT_THREAD_COUNT = 6;
	public static final String DEFAULT_DIRECTORY = "./root/";
	
	private String IS_url;
	private String save_filename;
	private String hash;
	private String save_directory;
	private long content_size;
	private int status;
	private int thread_count;
	private CountDownLatch latch;
	private JProgressBar progressBar;
	
	private ArrayList <String> MS_url;
	
	public DownloadMission(String IS_url, String filename, String save_directory, JProgressBar progressBar) {
		this.IS_url = IS_url;
		String[] tmp = filename.split("/");
		this.save_filename = tmp[0];
		this.hash = tmp[1];
		this.save_directory = save_directory;
		this.status = READY;
		this.thread_count = DEFAULT_THREAD_COUNT;
		this.MS_url = new ArrayList<String>();
		this.progressBar = progressBar;
		init();
	}
	
	public DownloadMission(String IS_url, String filename, JProgressBar progressBar) {
		this.IS_url = IS_url;
		String[] tmp = filename.split("/");
		this.save_filename = tmp[0];
		this.hash = tmp[1];
		this.save_directory = DEFAULT_DIRECTORY;
		this.status = READY;
		this.thread_count = DEFAULT_THREAD_COUNT;
		this.MS_url = new ArrayList<String>();
		this.progressBar = progressBar;
		init();
	}

	public DownloadMission(String IS_url, String filename, String save_directory, int thread_count, JProgressBar progressBar) {
		this.IS_url = IS_url;
		String[] tmp = filename.split("/");
		this.save_filename = tmp[0];
		this.hash = tmp[1];
		this.save_directory = save_directory;
		this.status = READY;
		this.thread_count = thread_count;
		this.MS_url = new ArrayList<String>();
		this.progressBar = progressBar;
		init();
	}
	
	public DownloadMission(String IS_url, String filename, int thread_count, JProgressBar progressBar) {
		this.IS_url = IS_url;
		String[] tmp = filename.split("/");
		this.save_filename = tmp[0];
		this.hash = tmp[1];
		this.save_directory = DEFAULT_DIRECTORY;
		this.status = READY;
		this.thread_count = thread_count;
		this.MS_url = new ArrayList<String>();
		this.progressBar = progressBar;
		init();
	}
	
	public void init() {
		try {
			String parameter = String.format("/get?f=%s", this.save_filename + '/' + this.hash);
//			String parameter = String.format("/get?f=%s", this.save_filename);
			URL obj = new URL(this.IS_url + parameter);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				System.out.println("Internal Error");
				this.status = ERROR;
				return;
			}

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			StringBuffer response = new StringBuffer();

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.isEmpty())
					break;
				inputLine += String.format("dl?filename=%s", this.save_filename);
				this.MS_url.add(inputLine);
			}
			in.close();
			this.content_size = getContentLength();	
			System.out.println(this.content_size);
		} catch (Exception ex) {
			ex.printStackTrace();
		}	
	}
	
	private long getContentLength() {
		int idx = 0;
		long res = -1;
		try {
			for (; idx < this.MS_url.size(); idx++) {
				URL obj = new URL(this.MS_url.get(idx));
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				String range_str = String.format("bytes=0-1");
				con.setRequestProperty("Range", range_str);
				
				int responseCode = con.getResponseCode();
				if (res == -1) {
					String raw = con.getHeaderField("Content-range");
					res =  Long.valueOf(raw.split("/")[1]);					
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			this.MS_url.remove(idx);
		}
		return res;
	}
	
	public void start() {
		if (this.content_size == -1) {
			this.status = ERROR;
			return;
		}
		latch = new CountDownLatch(this.thread_count);
		long end = -1, start, range_size = this.content_size / this.thread_count + 1;
		for (int i = 0; i < this.thread_count; i++) {
			start = end + 1;
			end = start + range_size;
			if (end >= this.content_size)
				end = this.content_size - 1;
			String url = this.MS_url.get(i % this.MS_url.size());
			Thread dr = new DownloadRunnable(url, this.save_directory, this.save_filename, start, end, latch, this.progressBar);
			dr.start();
		}
		this.status = DOWNLOADING;
		wait2finish();
	}
	
	private void wait2finish() {
		try {
			this.latch.await();
			finish();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void finish() {
		try {
			this.status = FINISHED;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	 
	    JProgressBar aJProgressBar = new JProgressBar(0, 50);
	    aJProgressBar.setStringPainted(true);

	    JButton aJButton = new JButton("Start");
	    ActionListener actionListener = new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	          aJButton.setEnabled(false);
	          aJProgressBar.setMaximum(6);
	          DownloadMission m = new DownloadMission("http://127.0.0.1:9498", "test/dummy", aJProgressBar);
	  		  m.start();
	        }
	    };
	    aJButton.addActionListener(actionListener);
	    frame.add(aJProgressBar, BorderLayout.NORTH);
	    frame.add(aJButton, BorderLayout.SOUTH);
	    frame.setSize(300, 200);
	    frame.setVisible(true);
	}
}
