package com.quakoo.baseFramework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 
 * @author liyongbiao
 *
 */
public class LZMAUtil {

	public static byte[] zip(byte[] data) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		java.io.BufferedInputStream inStream = null;
		java.io.BufferedOutputStream outStream = null;
		try {
			inStream = new java.io.BufferedInputStream(new ByteArrayInputStream(data));
			outStream = new java.io.BufferedOutputStream(bos);

			boolean eos = false;
			SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
			encoder.SetEndMarkerMode(eos);
			encoder.WriteCoderProperties(outStream);
			long fileSize;
			if (eos)
				fileSize = -1;
			else
				fileSize = data.length;
			for (int i = 0; i < 8; i++)
				outStream.write((int) (fileSize >>> (8 * i)) & 0xFF);
			encoder.Code(inStream, outStream, -1, -1, null);
		} catch (Exception e) {
			throw e;
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
		}
		return bos.toByteArray();
	}

	public static byte[] unzip(byte[] bs) throws Exception {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		java.io.BufferedInputStream inStream = null;
		java.io.BufferedOutputStream outStream = null;
		try {
			inStream = new java.io.BufferedInputStream(new ByteArrayInputStream(bs));
			outStream = new java.io.BufferedOutputStream(bos);

			int propertiesSize = 5;
			byte[] properties = new byte[propertiesSize];
			if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
				throw new Exception("input .lzma file is too short");
			SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
			if (!decoder.SetDecoderProperties(properties))
				throw new Exception("Incorrect stream properties");
			long outSize = 0;
			for (int i = 0; i < 8; i++) {
				int v = inStream.read();
				if (v < 0)
					throw new Exception("Can't read stream size");
				outSize |= ((long) v) << (8 * i);
			}
			if (!decoder.Code(inStream, outStream, outSize))
				throw new Exception("Error in data stream");
		} catch (Exception e) {
			throw e;
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
		}
		return bos.toByteArray();
	}

	public static void zipFile(String inFilePath, String outFilePath) throws Exception {
		java.io.File inFile = new java.io.File(inFilePath);
		java.io.File outFile = new java.io.File(outFilePath);

		java.io.BufferedInputStream inStream = null;
		java.io.BufferedOutputStream outStream = null;
		try {
			inStream = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
			outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));

			boolean eos = false;
			SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
			encoder.SetEndMarkerMode(eos);
			encoder.WriteCoderProperties(outStream);
			long fileSize;
			if (eos)
				fileSize = -1;
			else
				fileSize = inFile.length();
			for (int i = 0; i < 8; i++)
				outStream.write((int) (fileSize >>> (8 * i)) & 0xFF);
			encoder.Code(inStream, outStream, -1, -1, null);
		} catch (Exception e) {
			throw e;
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
		}
	}

	public static void unzipFile(String inFilePath, String outFilePath) throws Exception {
		java.io.File inFile = new java.io.File(inFilePath);
		java.io.File outFile = new java.io.File(outFilePath);

		java.io.BufferedInputStream inStream = null;
		java.io.BufferedOutputStream outStream = null;
		try {
			inStream = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
			outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));

			int propertiesSize = 5;
			byte[] properties = new byte[propertiesSize];
			if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
				throw new Exception("input .lzma file is too short");
			SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
			if (!decoder.SetDecoderProperties(properties))
				throw new Exception("Incorrect stream properties");
			long outSize = 0;
			for (int i = 0; i < 8; i++) {
				int v = inStream.read();
				if (v < 0)
					throw new Exception("Can't read stream size");
				outSize |= ((long) v) << (8 * i);
			}
			if (!decoder.Code(inStream, outStream, outSize))
				throw new Exception("Error in data stream");
		} catch (Exception e) {
			throw e;
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
		}
	}
	
	
	
	
	public static void main(String[] args) throws Exception {
		
	}

}
