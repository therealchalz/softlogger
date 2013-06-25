/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chache@brood.ca>. All rights reserved. 
 * 
 * This file is part of the softlogger project.
 * softlogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * softlogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with softlogger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache <chache@brood.ca> - initial API and implementation
 ******************************************************************************/
package ca.brood.softlogger.lookuptable;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/*
 * Lookup table format:
 * Arbitrary number of bytes up to first '\n' can be used for misc data
 * (eg description, calibration values, etc)
 * The next line is an integer in base 10 followed by a '\n'.  This number
 * is the word length/ number 'n' of bytes per lookup in the file (must 4 
 * or 8). If n is 4, then each lookup is treated as a float. If n is 8, then 
 * each lookup is treated as a double. The data starts immediately following 
 * the '\n'.
 * 
 * TODO: Optimize this.  Currently for every single lookup we open the file
 * and seek to the spot we want, then close the file.  Before I was keeping
 * the file up and using mark() and then I'd reset() before the next lookup,
 * but that stopped working when I tried to mark over 8000 bytes or so.
 */

public class LookupTable {
	private String filename;
	private String description;
	private int wordLength;
	private int introEndSeek;	//Points to the first character/byte of LUT data (after the preamble/intro stuff)
	
	public LookupTable(String filename) {
		this.filename = filename;
	}
	
	public synchronized Double lookup(int index) throws IOException {
		long seek = index*wordLength + introEndSeek;
		
		DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));

		din.skip(seek);
		
		double val;
		
		try {
			if (wordLength == 4)
				val = din.readFloat();
			else if (wordLength == 8)
				val = din.readDouble();
			else
				throw new IOException ("Invalid word length in LUT: "+filename);
		} finally {
			din.close();
		}
		
		return val;
	}
	
	public int getWordLength() {
		return wordLength;
	}
	
	public String getDescription() {
		return description;
	}
	
	private void parseWordLength(DataInputStream din) throws Exception {
		int s = din.read() - '0'; //must be 4 or 8
		if (s != 4 && s != 8) {
			throw new Exception("Bad size in lookup table: "+s);
		}
		int nl = din.read(); //skip the newline
		if (nl != '\n') {
			throw new Exception("Unexpected character in lookup table: "+nl);
		}
		
		introEndSeek += 2;	//the word length character + the newline
		
		this.wordLength = s;
	}
	
	private void parseDescription(DataInputStream din) throws IOException {
		StringBuilder desc = new StringBuilder();
		boolean reading = true;
		
		int readAheadSize = 32;
		byte[] buffer = new byte[readAheadSize];
		//Keep reading and appending bytes
		while (reading) {
			din.mark(readAheadSize);
			int bufSize = din.read(buffer);
			
			for (int i = 0; i < bufSize; i++) {
				
				introEndSeek++;	//Count each character and the newline
				
				if (buffer[i] == '\n') {
					//seek to the byte past the newline
					din.reset();
					din.skip(i+1);
					reading = false;
					break;
				}
				desc.append(String.format("%c", buffer[i]));
			}
		}
		
		this.description = desc.toString();
	}
	
	//Package private, for LookupTableManager
	void open() throws Exception {
		File tableFile = new File(filename);
		DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(tableFile)));
		
		introEndSeek = 0;
		
		parseDescription(din);
		parseWordLength(din);
	}
	
	//Package private, for LookupTableManager
	void close() throws Exception {
		//If we had something to clean up, we'd do it here
	}
}
