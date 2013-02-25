package ca.brood.softlogger.lookuptable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


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

public class LookupTableGenerator {
	public static boolean generate(String outFileName, GenerationFunction function, int size, String description, int count) throws IOException {
		if (size != 4 && size != 8) {
			return false;
		}
		File theFile = new File(outFileName);
		theFile.delete();
		theFile.createNewFile();
		FileOutputStream fo = new FileOutputStream(theFile);
		try {
			fo.write((description+"\n").getBytes());
			fo.write((Integer.toString(size)+"\n").getBytes());
			if (size == 4) {
				for (int i = 0; i < count; i++) {
					float val = (float) function.process(i);
					int byteVal = Float.floatToIntBits(val);
					write4Value(fo, byteVal);
				}
			} else {
				for (int i = 0; i < count; i++) {
					double val = function.process(i);
					long byteVal = Double.doubleToLongBits(val);
					write8Value(fo, byteVal);
				}
			}
		} finally {
			fo.close();
		}
		return true;
	}
	
	private static void write4Value(FileOutputStream stream, long val) throws IOException {
		stream.write((int) ((val>>24) & 0xFF));
		stream.write((int) ((val>>16) & 0xFF));
		stream.write((int) ((val>>8) & 0xFF));
		stream.write((int) ((val) & 0xFF));
	}
	
	private static void write8Value(FileOutputStream stream, long val) throws IOException {
		stream.write((int) ((val>>56) & 0xFF));
		stream.write((int) ((val>>48) & 0xFF));
		stream.write((int) ((val>>40) & 0xFF));
		stream.write((int) ((val>>32) & 0xFF));
		write4Value(stream, val);
	}

}
