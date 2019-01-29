/*
 * Tomas Popela, 2012
 * VIPS - Visual Internet Page Segmentation
 * Module - Vips.java
 */

package org.fit.vips;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.Viewport;
import org.fit.vips.VipsBlock;
import org.fit.vips.VipsOutput;
import org.fit.vips.VipsParser;
import org.fit.vips.VipsSeparatorGraphicsDetector;
import org.fit.vips.VisualStructureConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Vision-based Page Segmentation algorithm
 * @author Tomas Popela
 *
 */
public class Vips {
	private URL _url = null;
	private DOMAnalyzer _domAnalyzer = null;
	private BrowserCanvas _browserCanvas = null;
	private Viewport _viewport = null;

	private boolean _graphicsOutput = false;
	private boolean _outputToFolder = false;
	private boolean _outputEscaping = true;
	private int _pDoC = 11;
	private String _filename = "result"; //���Ⱑ �⺻ ����!! VipsOutPut�� _filename�� �ƴ�.
	
	
	//10�� 18�� �߰� : result 1~500���� �ѹ� vips�� ����ɶ����� +1�����ش�.
	private	int sizeTresholdWidth = 350;
	private	int sizeTresholdHeight = 400;

	private PrintStream originalOut = null;
	long startTime = 0;
	long endTime = 0;

	/**
	 * Default constructor
	 */
	public Vips()
	{
	}

	/**
	 * Enables or disables graphics output of VIPS algorithm.
	 * @param enable True for enable, otherwise false.
	 */
	public void enableGraphicsOutput(boolean enable)
	{
		_graphicsOutput = enable;
	}

	/**
	 * Enables or disables creation of new directory for every algorithm run.
	 * @param enable True for enable, otherwise false.
	 */
	public void enableOutputToFolder(boolean enable)
	{
		_outputToFolder = enable;
	}

	/**
	 * Enables or disables output XML character escaping.
	 * @param enable True for enable, otherwise false.
	 */
	public void enableOutputEscaping(boolean enable)
	{
		_outputEscaping = enable;
	}

	/**
	 * Sets permitted degree of coherence (pDoC) value.
	 * @param value pDoC value.
	 */
	public void setPredefinedDoC(int value)
	{
		if (value <= 0 || value > 11)
		{
			System.err.println("pDoC value must be between 1 and 11! Not " + value + "!");
			return;
		}
		else
		{
			_pDoC = value;
		}
	}

	/**
	 * Sets web page's URL
	 * @param url Url
	 * @throws MalformedURLException
	 */
	public void setUrl(String url)
	{
		try
		{
			if (url.startsWith("http://") || url.startsWith("https://"))
				_url = new URL(url);
			else
				_url = new URL("http://" + url);
		}
		catch (Exception e)
		{
			System.err.println("Invalid address: " + url);
		}
	}

	/**
	 * Parses a builds DOM tree from page source.
	 * @param urlStream Input stream with page source.
	 */
	private void getDomTree(URL urlStream)
	{
		DocumentSource docSource = null;
		try
		{
			docSource = new DefaultDocumentSource(urlStream);
			DOMSource parser = new DefaultDOMSource(docSource);

			Document domTree = parser.parse();
			_domAnalyzer = new DOMAnalyzer(domTree, _url);
			_domAnalyzer.attributesToStyles();
			_domAnalyzer.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT);
			_domAnalyzer.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT);
			_domAnalyzer.getStyleSheets();
		}
		catch (Exception e)
		{
			System.err.print(e.getMessage());
		}
	}

	/**
	 * Gets page's viewport
	 */
	private void getViewport()
	{
		_browserCanvas = new BrowserCanvas(_domAnalyzer.getRoot(),
				_domAnalyzer, new java.awt.Dimension(1200, 600), _url);
		_viewport = _browserCanvas.getViewport();
	}

	/**
	 * Exports rendered page to image.
	 */
	private void exportPageToImage()
	{
		try
		{
			BufferedImage page = _browserCanvas.getImage();
			String filename = System.getProperty("user.dir") + "/page-";
			ImageIO.write(page, "png", new File(filename));
		} catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Generates folder filename
	 * @return Folder filename
	 */
	private String generateFolderName()
	{
		String outputFolder = "";

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
		outputFolder += sdf.format(cal.getTime());
		outputFolder += "_";
		outputFolder += _url.getHost().replaceAll("\\.", "_").replaceAll("/", "_");

		return outputFolder;
	}

	/**
	 * Performs page segmentation.
	 */
	private void performSegmentation(String pagenum)
	{

		startTime = System.nanoTime(); //�����ٸ��̳� �̺�Ʈ ó���� ���� Ÿ�̸� �뵵, ����� �ð�
		int numberOfIterations = 10; //�ݺ�Ƚ��.? ���� �� ����������
		int pageWidth = _viewport.getWidth(); //������ ȭ�� ũ�� ������.
		int pageHeight = _viewport.getHeight();

		if (_graphicsOutput) //VIPSó���� �Ǿ��ٸ� �̹��� �����ؿ�
			exportPageToImage();

		VipsSeparatorGraphicsDetector detector; //�̹����� �����ڸ� ã���ִ� detector ����
		VipsParser vipsParser = new VipsParser(_viewport); // ���������� �ð������� ���̴� ��ϵ��� ã�Ƽ� �Ľ����ִ� vipsparser����
		VisualStructureConstructor constructor = new VisualStructureConstructor(_pDoC); // vips Ʈ�� ������ ������ִ� constructor ����
		constructor.setGraphicsOutput(_graphicsOutput); // �׷��� ����� ��Ȱ��ȭ�Ѵ�.

		for (int iterationNumber = 1; iterationNumber < numberOfIterations+1; iterationNumber++) //11�� �ݺ�...? ��°��
		{
			detector = new VipsSeparatorGraphicsDetector(pageWidth, pageHeight); //������ �ʱⰪ ����. ó�� ū�������� ������.

			//visual blocks detection
			vipsParser.setSizeTresholdHeight(sizeTresholdHeight); //�Ӱ� ���� ��
			vipsParser.setSizeTresholdWidth(sizeTresholdWidth); // �Ӱ� �ʺ� ��

			vipsParser.parse(); // ����� viewport�� ��ϰ� �� ��ϵ��� �ڽĳ����� �������ִ��� ��ϵ����� üũ�ϰ� doc�� ����, visualblock�� ī��Ʈ�� ����.

			VipsBlock vipsBlocks = vipsParser.getVipsBlocks(); //parseró���� ��ϵ��� �����´�.

			if (iterationNumber == 1) //�ݺ�Ƚ���� ù��°�̸�
			{//** 10��17��
				if (_graphicsOutput) // �׷��� �ƿ�ǲ�� true����ϸ�. ������ false �׷��� �Ʒ� ����� ���������ʴ´�. **true�� �����ؾ��� �̹��� ���� ����..
				{
					// in first round we'll export global separators
					detector.setVipsBlock(vipsBlocks); //������ ó���ϱ� ���� vipsBlock���� �����ͼ� ����.
					detector.fillPool(); //vipsBlock���� Pool�� �־��ش�.
					detector.saveToImage("blocks" + iterationNumber); //�⺻�̹��� ����
					detector.setCleanUpSeparators(0); // �⺻ ������ �A��
					detector.detectHorizontalSeparators(); //������ ������ Ž��
					detector.detectVerticalSeparators(); //������ ������ Ž��
					detector.exportHorizontalSeparatorsToImage(); // ������������ Ž���Ѱ��� �̹��� ����
					detector.exportVerticalSeparatorsToImage(); //������ ������ Ž���Ѱ��� �̹��� ����
					detector.exportAllToImage(); //�ΰ��� ����
				}

				// visual structure construction, �����ڸ� ã�� ��  ���� �⺻���� ����.
				constructor.setVipsBlocks(vipsBlocks); //���̴� ��ϵ鸸 ã���ְ�
				constructor.setPageSize(pageWidth, pageHeight);//������ ũ�� �ʺ� ����
			}
			else //ù���� �ݺ��� �ƴϸ� �Ľ� �� ���־� ����� ������ �Ǽ����ش�.
			{
				vipsBlocks = vipsParser.getVipsBlocks();
				constructor.updateVipsBlocks(vipsBlocks);

				if (_graphicsOutput) //�׸��� �̹�������� on�̸�, 
				{
					detector.setVisualBlocks(constructor.getVisualBlocks());
					detector.fillPool();
					detector.saveToImage("blocks" + iterationNumber);
				}
			}

			// visual structure construction
			constructor.constructVisualStructure(); //���⼭ ��¥ ���־��ϵ鸸 vips���� ������ش�. ->�� ������_visualStructure
			// prepare tresholds for next iteration
			if (iterationNumber <= 5 )
			{
				sizeTresholdHeight -= 50;
				sizeTresholdWidth -= 50;

			}
			if (iterationNumber == 6)
			{
				sizeTresholdHeight = 100;
				sizeTresholdWidth = 100;
			}
			if (iterationNumber == 7)
			{
				sizeTresholdHeight = 80;
				sizeTresholdWidth = 80;
			}
			if (iterationNumber == 8)
			{
				sizeTresholdHeight = 40;
				sizeTresholdWidth = 10;
			}
			if (iterationNumber == 9)
			{
				sizeTresholdHeight = 1;
				sizeTresholdWidth = 1;
			}

		}

		//		constructor.normalizeSeparatorsSoftMax();
		constructor.normalizeSeparatorsMinMax();

		VipsOutput vipsOutput = new VipsOutput(_pDoC);
		vipsOutput.setEscapeOutput(_outputEscaping);
		vipsOutput.setOutputFileName(_filename,pagenum); //output���� �̸� ����������ϴµ�����.
//		System.out.println("33"+constructor.getVisualStructure());
		vipsOutput.writeXML(constructor.getVisualStructure(), _viewport);
		
		endTime = System.nanoTime();

		long diff = endTime - startTime;

		System.out.println("Execution time of VIPS: " + diff + " ns; " +
				(diff / 1000000.0) + " ms; " +
				(diff / 1000000000.0) + " sec");
		//10�� 19�� �߰� :���� XML�� DB���ٰ� �־��ش�.
		
		
	}
	
	private void performSegmentation()
	{

		startTime = System.nanoTime(); //�����ٸ��̳� �̺�Ʈ ó���� ���� Ÿ�̸� �뵵, ����� �ð�
		int numberOfIterations = 10; //�ݺ�Ƚ��.? ���� �� ����������
		int pageWidth = _viewport.getWidth(); //������ ȭ�� ũ�� ������.
		int pageHeight = _viewport.getHeight();

		if (_graphicsOutput) //VIPSó���� �Ǿ��ٸ� �̹��� �����ؿ�
			exportPageToImage();

		VipsSeparatorGraphicsDetector detector; //�̹����� �����ڸ� ã���ִ� detector ����
		VipsParser vipsParser = new VipsParser(_viewport); // ���������� �ð������� ���̴� ��ϵ��� ã�Ƽ� �Ľ����ִ� vipsparser����
		VisualStructureConstructor constructor = new VisualStructureConstructor(_pDoC); // vips Ʈ�� ������ ������ִ� constructor ����
		constructor.setGraphicsOutput(_graphicsOutput); // �׷��� ����� ��Ȱ��ȭ�Ѵ�.

		for (int iterationNumber = 1; iterationNumber < numberOfIterations+1; iterationNumber++) //11�� �ݺ�...? ��°��
		{
			detector = new VipsSeparatorGraphicsDetector(pageWidth, pageHeight); //������ �ʱⰪ ����. ó�� ū�������� ������.

			//visual blocks detection
			vipsParser.setSizeTresholdHeight(sizeTresholdHeight); //�Ӱ� ���� ��
			vipsParser.setSizeTresholdWidth(sizeTresholdWidth); // �Ӱ� �ʺ� ��

			vipsParser.parse(); // ����� viewport�� ��ϰ� �� ��ϵ��� �ڽĳ����� �������ִ��� ��ϵ����� üũ�ϰ� doc�� ����, visualblock�� ī��Ʈ�� ����.

			VipsBlock vipsBlocks = vipsParser.getVipsBlocks(); //parseró���� ��ϵ��� �����´�.

			if (iterationNumber == 1) //�ݺ�Ƚ���� ù��°�̸�
			{//** 10��17��
				if (_graphicsOutput) // �׷��� �ƿ�ǲ�� true����ϸ�. ������ false �׷��� �Ʒ� ����� ���������ʴ´�. **true�� �����ؾ��� �̹��� ���� ����..
				{
					// in first round we'll export global separators
					detector.setVipsBlock(vipsBlocks); //������ ó���ϱ� ���� vipsBlock���� �����ͼ� ����.
					detector.fillPool(); //vipsBlock���� Pool�� �־��ش�.
					detector.saveToImage("blocks" + iterationNumber); //�⺻�̹��� ����
					detector.setCleanUpSeparators(0); // �⺻ ������ �A��
					detector.detectHorizontalSeparators(); //������ ������ Ž��
					detector.detectVerticalSeparators(); //������ ������ Ž��
					detector.exportHorizontalSeparatorsToImage(); // ������������ Ž���Ѱ��� �̹��� ����
					detector.exportVerticalSeparatorsToImage(); //������ ������ Ž���Ѱ��� �̹��� ����
					detector.exportAllToImage(); //�ΰ��� ����
				}

				// visual structure construction, �����ڸ� ã�� ��  ���� �⺻���� ����.
				constructor.setVipsBlocks(vipsBlocks); //���̴� ��ϵ鸸 ã���ְ�
				constructor.setPageSize(pageWidth, pageHeight);//������ ũ�� �ʺ� ����
			}
			else //ù���� �ݺ��� �ƴϸ� �Ľ� �� ���־� ����� ������ �Ǽ����ش�.
			{
				vipsBlocks = vipsParser.getVipsBlocks();
				constructor.updateVipsBlocks(vipsBlocks);

				if (_graphicsOutput) //�׸��� �̹�������� on�̸�, 
				{
					detector.setVisualBlocks(constructor.getVisualBlocks());
					detector.fillPool();
					detector.saveToImage("blocks" + iterationNumber);
				}
			}

			// visual structure construction
			constructor.constructVisualStructure(); //���⼭ ��¥ ���־��ϵ鸸 vips���� ������ش�. ->�� ������_visualStructure
			// prepare tresholds for next iteration
			if (iterationNumber <= 5 )
			{
				sizeTresholdHeight -= 50;
				sizeTresholdWidth -= 50;

			}
			if (iterationNumber == 6)
			{
				sizeTresholdHeight = 100;
				sizeTresholdWidth = 100;
			}
			if (iterationNumber == 7)
			{
				sizeTresholdHeight = 80;
				sizeTresholdWidth = 80;
			}
			if (iterationNumber == 8)
			{
				sizeTresholdHeight = 40;
				sizeTresholdWidth = 10;
			}
			if (iterationNumber == 9)
			{
				sizeTresholdHeight = 1;
				sizeTresholdWidth = 1;
			}

		}

		//		constructor.normalizeSeparatorsSoftMax();
		constructor.normalizeSeparatorsMinMax();

		VipsOutput vipsOutput = new VipsOutput(_pDoC);
		vipsOutput.setEscapeOutput(_outputEscaping);
		vipsOutput.setOutputFileName(_filename); //output���� �̸� ����������ϴµ�����.
//		System.out.println("33"+constructor.getVisualStructure());
		vipsOutput.writeXML(constructor.getVisualStructure(), _viewport);
		
		endTime = System.nanoTime();

		long diff = endTime - startTime;

		System.out.println("Execution time of VIPS: " + diff + " ns; " +
				(diff / 1000000.0) + " ms; " +
				(diff / 1000000000.0) + " sec");
		//10�� 19�� �߰� :���� XML�� DB���ٰ� �־��ش�.
		
		
	}

	/**
	 * Starts segmentation on given address
	 * @param url
	 */
	public void startSegmentation(String url,String pagenum)
	{
		setUrl(url);

		startSegmentation(pagenum);
	}
	
	

	/**
	 * Restores stdout
	 */
	private void restoreOut()
	{
		if (originalOut != null)
		{
			System.setOut(originalOut);
		}
	}

	/**
	 * Redirects stdout to nowhere
	 */
	private void redirectOut()
	{
		originalOut = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException
			{

			}
		}));
	}

	/**
	 * Starts visual segmentation of page
	 * @throws Exception
	 */
	public void startSegmentation(String pagenum)
	{
		try
		{	
			_url.openConnection(); // url �����ؼ� url�� ���� �ν��Ͻ��� ��´�.

			redirectOut(); 

			getDomTree(_url); //url�� DOM tree������.
			startTime = System.nanoTime();
			getViewport(); //url�� �������� ���� ,����, Ȯ�밪�� ���� ��������
			restoreOut();

			String outputFolder = ""; //����� ������
			String oldWorkingDirectory = ""; //�����۾� ������
			String newWorkingDirectory = ""; //�ű��۾� ������

			if (_outputToFolder) //������� ������ ������ġ ����. ������ ���� ����� ������ �ѱ��.
			{
				outputFolder = generateFolderName();

				if (!new File(outputFolder).mkdir())
				{
					System.err.println("Something goes wrong during directory creation!");
				}
				else
				{
					oldWorkingDirectory = System.getProperty("user.dir");
					newWorkingDirectory += oldWorkingDirectory + "/" + outputFolder + "/";
					System.setProperty("user.dir", newWorkingDirectory);
				}
			}

			performSegmentation(pagenum);//��� ����

			if (_outputToFolder)
				System.setProperty("user.dir", oldWorkingDirectory);
		}
		catch (Exception e)
		{
			System.err.println("Something's wrong!");
			e.printStackTrace();
		}
	}
	
	public void startSegmentation()
	{
		try
		{	
			_url.openConnection(); // url �����ؼ� url�� ���� �ν��Ͻ��� ��´�.

			redirectOut(); 

			getDomTree(_url); //url�� DOM tree������.
			startTime = System.nanoTime();
			getViewport(); //url�� �������� ���� ,����, Ȯ�밪�� ���� ��������
			restoreOut();

			String outputFolder = ""; //����� ������
			String oldWorkingDirectory = ""; //�����۾� ������
			String newWorkingDirectory = ""; //�ű��۾� ������

			if (_outputToFolder) //������� ������ ������ġ ����. ������ ���� ����� ������ �ѱ��.
			{
				outputFolder = generateFolderName();

				if (!new File(outputFolder).mkdir())
				{
					System.err.println("Something goes wrong during directory creation!");
				}
				else
				{
					oldWorkingDirectory = System.getProperty("user.dir");
					newWorkingDirectory += oldWorkingDirectory + "/" + outputFolder + "/";
					System.setProperty("user.dir", newWorkingDirectory);
				}
			}

			performSegmentation();//��� ����

			if (_outputToFolder)
				System.setProperty("user.dir", oldWorkingDirectory);
		}
		catch (Exception e)
		{
			System.err.println("Something's wrong!");
			e.printStackTrace();
		}
	}

	public void setOutputFileName(String filename)
	{   
		if (!filename.equals(""))
		{	
			_filename = filename;
		}
		else
		{
			System.out.println("Invalid filename!");
		}
	}
	
	
}
