package com.forgetfilefast;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DownloadRunnable extends Thread {
	private String url;
	private String save_directory;
	private String save_filename;
	private long start;
	private long end;
	private CountDownLatch latch;
	private JProgressBar progressBar;
	
	private static int BUFFER_SIZE = 1024;
	
	DownloadRunnable() {}
	DownloadRunnable(String url, String save_directory, String save_filename, long start, long end, CountDownLatch latch) {
		this.url = url;
		this.start = start;
		this.end = end;
		this.latch = latch;
		this.save_directory = save_directory;
		this.save_filename = save_filename;
		this.progressBar = null;
	}
	DownloadRunnable(String url, String save_directory, String save_filename, long start, long end, CountDownLatch latch, JProgressBar progressBar) {
		this.url = url;
		this.start = start;
		this.end = end;
		this.latch = latch;
		this.save_directory = save_directory;
		this.save_filename = save_filename;
		this.progressBar = progressBar;
	}
		
	@Override
	public void run() {
		synchronized (this) {
			File targetFile = new File(this.save_directory + "/" + this.save_filename);
			if (targetFile.exists() == false) {
				try {
					targetFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			BufferedInputStream bufferedInputStream = null;
			RandomAccessFile randomAccessFile = null;
			byte[] buf = new byte[BUFFER_SIZE];
			try {
				URL obj = new URL(this.url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				String range_str = String.format("bytes=%d-%d", this.start, this.end);
				con.setRequestProperty("Range", range_str);
				randomAccessFile = new RandomAccessFile(targetFile, "rw");
				randomAccessFile.seek(this.start);
				bufferedInputStream = new BufferedInputStream(
						con.getInputStream());
				while (this.start < this.end) {
					int len = bufferedInputStream.read(buf, 0, BUFFER_SIZE);
					if (len == -1)
						break;
					else {
						randomAccessFile.write(buf, 0, len);
						this.start += len;
					}
				}
				bufferedInputStream.close();
				randomAccessFile.close();
				latch.countDown();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (this.progressBar != null) {
			synchronized (this.progressBar) {
				int val = this.progressBar.getValue();
				this.progressBar.setValue(val + 1);	
			}
		}
	}

}
