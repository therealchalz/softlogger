package ca.brood.softlogger.lookuptable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * Lookup table format:
 * Arbitrary number of bytes up to first '\n' can be used for misc data
 * (eg description, calibration values, etc)
 * The next line is an integer in base 10 followed by a '\n'.  This number
 * is the number 'n' of bytes per lookup in the file (must 4 or 8).
 * If n is 4, then each lookup is treated as a float.
 * If n is 8, then each lookup is treated as a double.
 * The data starts immediately following the '\n'.
 */

public class LookupTable {
	private String filename;
	private InputStream fos;
	private String description;
	private int size;
	
	public LookupTable(String filename) {
		this.filename = filename;
	}
	
	public Double lookup(int index) throws IOException {
		byte[] buffer = new byte[size];
		
		long seek = index*size;
		fos.mark((int) (seek + size));
		fos.skip(seek);
		fos.read(buffer,0,size);
		
		double val = parse(buffer);
		fos.reset();
		return val;
	}
	
	public int getSize() {
		return size;
	}
	
	public String getDescription() {
		return description;
	}
	
	private void readSize() throws Exception {
		int s = fos.read() - '0'; //must be 4 or 8
		if (s != 4 && s != 8) {
			throw new Exception("Bad size in lookup table: "+s);
		}
		int nl = fos.read(); //skip the newline
		if (nl != '\n') {
			throw new Exception("Unexpected character in lookup table: "+nl);
		}
		this.size = s;
	}
	
	private void readDescription() throws IOException {
		StringBuilder ret = new StringBuilder();
		boolean reading = true;
		//Keep reading and appending bytes
		while (reading) {
			int readAheadSize = 32;
			
			byte[] buffer = new byte[readAheadSize];
			
			fos.mark(readAheadSize);
			int bufSize = fos.read(buffer);
			
			for (int i = 0; i < bufSize; i++) {
				if (buffer[i] == '\n') {
					//seek to the byte past the newline
					fos.reset();
					fos.skip(i+1);
					reading = false;
					break;
				}
				ret.append(String.format("%c", buffer[i]));
			}
		}
		
		this.description = ret.toString();
	}
	
	public void open() throws Exception {
		File tableFile = new File(filename);
		fos = new BufferedInputStream(new FileInputStream(tableFile));
		readDescription();
		readSize();
	}
	
	public void close() throws Exception {
		fos.close();
	}
	
	private double parse(byte[] buff) {
		long val = 0;
		if (size == 4) {
			val += ((int)buff[0]&0xFF) << 24;
			val += ((int)buff[1]&0xFF) << 16;
			val += ((int)buff[2]&0xFF) << 8;
			val += ((int)buff[3]&0xFF);
			return (double) Float.intBitsToFloat((int)val);
		} else if (size == 8) {
			val += ((long)buff[0]&0xFF) << 56;
			val += ((long)buff[1]&0xFF) << 48;
			val += ((long)buff[2]&0xFF) << 40;
			val += ((long)buff[3]&0xFF) << 32;
			val += ((long)buff[4]&0xFF) << 24;
			val += ((long)buff[5]&0xFF) << 16;
			val += ((long)buff[6]&0xFF) << 8;
			val += ((long)buff[7]&0xFF);
			return Double.longBitsToDouble(val);
		}
		return 0.0;
		
	}
}
