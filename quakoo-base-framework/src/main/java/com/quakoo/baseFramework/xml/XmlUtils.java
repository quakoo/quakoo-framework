package com.quakoo.baseFramework.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * @author lichao
 * @date 2016年4月30日
 */
public class XmlUtils {
	/**
	 * 获取根节点
	 * 
	 * @param doc
	 * @return
	 */
	public static Element getRootElement(Document doc) {
		if (doc == null) {
			return null;
		}
		return doc.getRootElement();
	}

	/**
	 * 获取节点eleName下的文本值，若eleName不存在则返回默认值defaultValue
	 * 
	 * @param eleName
	 * @param defaultValue
	 * @return
	 */
	public static String getElementValue(Element eleName, String defaultValue) {
		if (eleName == null) {
			return defaultValue == null ? "" : defaultValue;
		} else {
			return eleName.getTextTrim();
		}
	}

	public static String getElementValue(String eleName, Element parentElement) {
		if (parentElement == null) {
			return null;
		} else {
			Element element = parentElement.element(eleName);
			if (element != null) {
				return element.getTextTrim();
			} else {
				try {
					throw new Exception("找不到节点" + eleName);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}
	}

	/**
	 * 获取节点eleName下的文本值
	 * 
	 * @param eleName
	 * @return
	 */
	public static String getElementValue(Element eleName) {
		return getElementValue(eleName, null);
	}

	public static Document read(File file) {
		return read(file, null);
	}

	public static Document findCDATA(Document body, String path) {
		return stringToXml(getElementValue(path, body.getRootElement()));
	}

	/**
	 * 
	 * @param file
	 * @param charset
	 * @return
	 * @throws DocumentException
	 */
	public static Document read(File file, String charset) {
		if (file == null) {
			return null;
		}
		SAXReader reader = new SAXReader();
		if (StringUtils.isNotBlank(charset)) {
			reader.setEncoding(charset);
		}
		Document document = null;
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}

	public static Document read(URL url) {
		return read(url, null);
	}

	/**
	 * 
	 * @param url
	 * @param charset
	 * @return
	 * @throws DocumentException
	 */
	public static Document read(URL url, String charset) {
		if (url == null) {
			return null;
		}
		SAXReader reader = new SAXReader();
		if (StringUtils.isNotBlank(charset)) {
			reader.setEncoding(charset);
		}
		Document document = null;
		try {
			document = reader.read(url);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}

	/**
	 * 将文档树转换成字符串
	 * 
	 * @param doc
	 * @return
	 */
	public static String xmltoString(Document doc) {
		return xmltoString(doc, null);
	}

	/**
	 * 
	 * @param doc
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public static String xmltoString(Document doc, String charset) {
		if (doc == null) {
			return "";
		}
		if (StringUtils.isBlank(charset)) {
			return doc.asXML();
		}
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding(charset);
		StringWriter strWriter = new StringWriter();
		XMLWriter xmlWriter = new XMLWriter(strWriter, format);
		try {
			xmlWriter.write(doc);
			xmlWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strWriter.toString();
	}

	/**
	 * 持久化Document
	 * 
	 * @param doc
	 * @param charset
	 * @return
	 * @throws Exception
	 * @throws IOException
	 */
	public static void xmltoFile(Document doc, File file, String charset) throws Exception {
		if (doc == null) {
			throw new NullPointerException("doc cant not null");
		}
		if (StringUtils.isBlank(charset)) {
			throw new NullPointerException("charset cant not null");
		}
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding(charset);
		FileOutputStream os = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(os, charset);
		XMLWriter xmlWriter = new XMLWriter(osw, format);
		try {
			xmlWriter.write(doc);
			xmlWriter.close();
			if (osw != null) {
				osw.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param doc
	 * @param charset
	 * @return
	 * @throws Exception
	 * @throws IOException
	 */
	public static void xmltoFile(Document doc, String filePath, String charset) throws Exception {
		xmltoFile(doc, new File(filePath), charset);
	}

	/**
	 * 
	 * @param doc
	 * @param filePath
	 * @param charset
	 * @throws Exception
	 */
	public static void writeDocumentToFile(Document doc, String filePath, String charset) throws Exception {
		xmltoFile(doc, new File(filePath), charset);
	}

	public static Document stringToXml(String text) {
		try {
			return DocumentHelper.parseText(text);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Document createDocument() {
		return DocumentHelper.createDocument();
	}
}
