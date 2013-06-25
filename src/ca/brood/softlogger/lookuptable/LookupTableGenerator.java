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
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/*
 * Lookup table format:
 * Arbitrary number of bytes up to first '\n' can be used for misc data
 * (eg description, calibration values, etc)
 * The next line is an integer in base 10 followed by a '\n'.  This number
 * is the number 'n' of bytes per lookup in the file (must 4 or 8).
 * In other words it is the word length.
 * If n is 4, then each lookup is treated as a java float.
 * If n is 8, then each lookup is treated as a java double.
 * The data starts immediately following the '\n'.
 */

public class LookupTableGenerator {
	/** Generates a new lookup table.
	 * The format of the table is as follows:<br>
	 * The first line (arbitrary number of bytes up to first '\n' can be used for misc data
	 * (eg: description, calibration values, etc). <br>
	 * The next line is an integer in base 10 followed by a '\n'.  This number is
	 * the number 'n' of bytes per lookup in the file (must be 4 for float values or
	 * 8 for double values).  Aka Word Length.<br>
	 * The word length and the '\n' the data starts.  Each subsequent 'n' bytes represents
	 * a float (for n=4) or a double (for n=8) saved to the stream via {@link DataOutputStream#writeFloat(float)} and
	 * {@link DataOutputStream#writeDouble(double)} respectively.
	 * @param outFileName The name of the output file
	 * @param function The function to use for the data
	 * @param wordSize The wordsize for storage in the file.  Must be 4 for float or 8 for double values.
	 * @param description The description to put in the file.
	 * @param count The number of times to call the function.  The function gets called with each integer argument in [0, count-1].
	 * @throws IOException On error.
	 */
	public static void generate(String outFileName, GenerationFunction function, int wordSize, String description, int count) throws IOException {
		generateFile(outFileName, function, wordSize, description, count, false);
	}
	
	/** Generates a new lookup table for human debugging.  This is very similar to {@link #generate(String, GenerationFunction, int, String, int)}.
	 * The difference is that instead of encoding all the data in binary form, the debug lookup table encodes all the data as strings
	 * separated by newlines.  This makes it much easier to verify that a table is good, and also allows one to quickly view what
	 * the expected output for a particular lookup would be (by using the line numbers).
	 * @param outFileName The name of the output file
	 * @param function The function to use for the data
	 * @param wordSize The wordsize for storage in the file.  Must be 4 for float or 8 for double values.
	 * @param description The description to put in the file.
	 * @param count The number of times to call the function.  The function gets called with each integer argument in [0, count-1].
	 * @throws IOException On error.
	 */
	public static void generateDebug(String outFileName, GenerationFunction function, int wordSize, String description, int count) throws IOException {
		generateFile(outFileName, function, wordSize, description, count, true);
	}
	
	/** This takes a LUT generated with {@link #generate(String, GenerationFunction, int, String, int)} and converts it to
	 * what it would look like if it had been generated with {@link #generateDebug(String, GenerationFunction, int, String, int)}.
	 * That is, it takes a non-human-readable LUT and makes it human readable.
	 * @param tableIn The input LUT, must exist and be in correct format.
	 * @param tableOut The output LUT.
	 * @throws IOException On error.
	 */
	public static void decryptLUT(String tableIn, String tableOut) throws IOException {
		DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(tableIn)));
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tableOut)));
		
		try {
			
			int newlineCount = 0;
			
			while (newlineCount < 2) {
				int c = din.read();
				if (c == '\n')
					newlineCount++;
				dos.write(c);
			}
			while (din.available()>0) {
				float val = din.readFloat();
				dos.write(String.format("%f\n", val).getBytes());
			}
		} finally {
			din.close();
			dos.close();
		}
	}
	
	private static void generateFile(String outFileName, GenerationFunction function, int wordSize, String description, int count, boolean debug) throws IOException {
		if (wordSize != 4 && wordSize != 8) {
			throw new IOException("Invalid word size specified for file: "+wordSize);
		}
		File theFile = new File(outFileName);
		theFile.delete();
		theFile.createNewFile();
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(theFile)));
		try {
			dos.write((description+"\n").getBytes());
			dos.write((Integer.toString(wordSize)+"\n").getBytes());
			if (wordSize == 4) {
				for (int i = 0; i < count; i++) {
					float val = (float) function.process(i);
					if (debug)
						dos.write(String.format("%f\n", val).getBytes());
					else
						dos.writeFloat(val);
				}
			} else {
				for (int i = 0; i < count; i++) {
					double val = function.process(i);
					if (debug)
						dos.write(String.format("%f\n", val).getBytes());
					else
						dos.writeDouble(val);
				}
			}
		} finally {
			dos.close();
		}
	}
}
