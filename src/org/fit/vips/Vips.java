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
	private String _filename = "result"; //여기가 기본 제목!! VipsOutPut의 _filename은 아님.
	
	
	//10월 18일 추가 : result 1~500까지 한번 vips가 실행될때마다 +1시켜준다.
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

		startTime = System.nanoTime(); //스케줄링이나 이벤트 처리를 위한 타이머 용도, 경과된 시간
		int numberOfIterations = 10; //반복횟수.? 설정 왜 설정해주지
		int pageWidth = _viewport.getWidth(); //페이지 화면 크기 얻어오기.
		int pageHeight = _viewport.getHeight();

		if (_graphicsOutput) //VIPS처리가 되었다면 이미지 추출해요
			exportPageToImage();

		VipsSeparatorGraphicsDetector detector; //이미지의 구분자를 찾아주는 detector 선언
		VipsParser vipsParser = new VipsParser(_viewport); // 웹페이지의 시각적으로 보이는 블록들을 찾아서 파싱해주는 vipsparser선언
		VisualStructureConstructor constructor = new VisualStructureConstructor(_pDoC); // vips 트리 구조를 만들어주는 constructor 선언
		constructor.setGraphicsOutput(_graphicsOutput); // 그래픽 출력을 비활성화한다.

		for (int iterationNumber = 1; iterationNumber < numberOfIterations+1; iterationNumber++) //11번 반복...? 어째서
		{
			detector = new VipsSeparatorGraphicsDetector(pageWidth, pageHeight); //구분자 초기값 설정. 처음 큰페이지의 구분자.

			//visual blocks detection
			vipsParser.setSizeTresholdHeight(sizeTresholdHeight); //임계 높이 값
			vipsParser.setSizeTresholdWidth(sizeTresholdWidth); // 임계 너비 값

			vipsParser.parse(); // 저장된 viewport의 블록과 그 블록들의 자식노드들을 나눌수있는지 블록들인지 체크하고 doc값 설정, visualblock의 카운트를 설정.

			VipsBlock vipsBlocks = vipsParser.getVipsBlocks(); //parser처리된 블록들을 가져온다.

			if (iterationNumber == 1) //반복횟수가 첫번째이면
			{//** 10월17일
				if (_graphicsOutput) // 그래픽 아웃풋을 true라고하면. 지금은 false 그래서 아래 기능을 수행하지않는다. **true로 변경해야지 이미지 추출 가능..
				{
					// in first round we'll export global separators
					detector.setVipsBlock(vipsBlocks); //구분자 처리하기 위한 vipsBlock들을 가져와서 지정.
					detector.fillPool(); //vipsBlock들을 Pool에 넣어준다.
					detector.saveToImage("blocks" + iterationNumber); //기본이미지 저장
					detector.setCleanUpSeparators(0); // 기본 구문자 섲렁
					detector.detectHorizontalSeparators(); //수평적 구분자 탐지
					detector.detectVerticalSeparators(); //수직적 구분자 탐지
					detector.exportHorizontalSeparatorsToImage(); // 수평적구분자 탐지한것을 이미지 추출
					detector.exportVerticalSeparatorsToImage(); //수직적 구분자 탐지한것을 이미지 추출
					detector.exportAllToImage(); //두개다 추출
				}

				// visual structure construction, 구분자를 찾은 뒤  구조 기본설정 지정.
				constructor.setVipsBlocks(vipsBlocks); //보이는 블록들만 찾아주고
				constructor.setPageSize(pageWidth, pageHeight);//페이지 크기 너비 설정
			}
			else //첫번재 반복이 아니면 파싱 된 비주얼 블록의 구조를 건설해준다.
			{
				vipsBlocks = vipsParser.getVipsBlocks();
				constructor.updateVipsBlocks(vipsBlocks);

				if (_graphicsOutput) //그리고 이미지출력이 on이면, 
				{
					detector.setVisualBlocks(constructor.getVisualBlocks());
					detector.fillPool();
					detector.saveToImage("blocks" + iterationNumber);
				}
			}

			// visual structure construction
			constructor.constructVisualStructure(); //여기서 진짜 비주얼블록들만 vips구조 만들어준다. ->그 변수가_visualStructure
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
		vipsOutput.setOutputFileName(_filename,pagenum); //output파일 이름 지정해줘야하는데없다.
//		System.out.println("33"+constructor.getVisualStructure());
		vipsOutput.writeXML(constructor.getVisualStructure(), _viewport);
		
		endTime = System.nanoTime();

		long diff = endTime - startTime;

		System.out.println("Execution time of VIPS: " + diff + " ns; " +
				(diff / 1000000.0) + " ms; " +
				(diff / 1000000000.0) + " sec");
		//10월 19일 추가 :뽑은 XML을 DB에다가 넣어준다.
		
		
	}
	
	private void performSegmentation()
	{

		startTime = System.nanoTime(); //스케줄링이나 이벤트 처리를 위한 타이머 용도, 경과된 시간
		int numberOfIterations = 10; //반복횟수.? 설정 왜 설정해주지
		int pageWidth = _viewport.getWidth(); //페이지 화면 크기 얻어오기.
		int pageHeight = _viewport.getHeight();

		if (_graphicsOutput) //VIPS처리가 되었다면 이미지 추출해요
			exportPageToImage();

		VipsSeparatorGraphicsDetector detector; //이미지의 구분자를 찾아주는 detector 선언
		VipsParser vipsParser = new VipsParser(_viewport); // 웹페이지의 시각적으로 보이는 블록들을 찾아서 파싱해주는 vipsparser선언
		VisualStructureConstructor constructor = new VisualStructureConstructor(_pDoC); // vips 트리 구조를 만들어주는 constructor 선언
		constructor.setGraphicsOutput(_graphicsOutput); // 그래픽 출력을 비활성화한다.

		for (int iterationNumber = 1; iterationNumber < numberOfIterations+1; iterationNumber++) //11번 반복...? 어째서
		{
			detector = new VipsSeparatorGraphicsDetector(pageWidth, pageHeight); //구분자 초기값 설정. 처음 큰페이지의 구분자.

			//visual blocks detection
			vipsParser.setSizeTresholdHeight(sizeTresholdHeight); //임계 높이 값
			vipsParser.setSizeTresholdWidth(sizeTresholdWidth); // 임계 너비 값

			vipsParser.parse(); // 저장된 viewport의 블록과 그 블록들의 자식노드들을 나눌수있는지 블록들인지 체크하고 doc값 설정, visualblock의 카운트를 설정.

			VipsBlock vipsBlocks = vipsParser.getVipsBlocks(); //parser처리된 블록들을 가져온다.

			if (iterationNumber == 1) //반복횟수가 첫번째이면
			{//** 10월17일
				if (_graphicsOutput) // 그래픽 아웃풋을 true라고하면. 지금은 false 그래서 아래 기능을 수행하지않는다. **true로 변경해야지 이미지 추출 가능..
				{
					// in first round we'll export global separators
					detector.setVipsBlock(vipsBlocks); //구분자 처리하기 위한 vipsBlock들을 가져와서 지정.
					detector.fillPool(); //vipsBlock들을 Pool에 넣어준다.
					detector.saveToImage("blocks" + iterationNumber); //기본이미지 저장
					detector.setCleanUpSeparators(0); // 기본 구문자 섲렁
					detector.detectHorizontalSeparators(); //수평적 구분자 탐지
					detector.detectVerticalSeparators(); //수직적 구분자 탐지
					detector.exportHorizontalSeparatorsToImage(); // 수평적구분자 탐지한것을 이미지 추출
					detector.exportVerticalSeparatorsToImage(); //수직적 구분자 탐지한것을 이미지 추출
					detector.exportAllToImage(); //두개다 추출
				}

				// visual structure construction, 구분자를 찾은 뒤  구조 기본설정 지정.
				constructor.setVipsBlocks(vipsBlocks); //보이는 블록들만 찾아주고
				constructor.setPageSize(pageWidth, pageHeight);//페이지 크기 너비 설정
			}
			else //첫번재 반복이 아니면 파싱 된 비주얼 블록의 구조를 건설해준다.
			{
				vipsBlocks = vipsParser.getVipsBlocks();
				constructor.updateVipsBlocks(vipsBlocks);

				if (_graphicsOutput) //그리고 이미지출력이 on이면, 
				{
					detector.setVisualBlocks(constructor.getVisualBlocks());
					detector.fillPool();
					detector.saveToImage("blocks" + iterationNumber);
				}
			}

			// visual structure construction
			constructor.constructVisualStructure(); //여기서 진짜 비주얼블록들만 vips구조 만들어준다. ->그 변수가_visualStructure
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
		vipsOutput.setOutputFileName(_filename); //output파일 이름 지정해줘야하는데없다.
//		System.out.println("33"+constructor.getVisualStructure());
		vipsOutput.writeXML(constructor.getVisualStructure(), _viewport);
		
		endTime = System.nanoTime();

		long diff = endTime - startTime;

		System.out.println("Execution time of VIPS: " + diff + " ns; " +
				(diff / 1000000.0) + " ms; " +
				(diff / 1000000000.0) + " sec");
		//10월 19일 추가 :뽑은 XML을 DB에다가 넣어준다.
		
		
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
			_url.openConnection(); // url 연결해서 url에 관한 인스턴스를 얻는다.

			redirectOut(); 

			getDomTree(_url); //url의 DOM tree얻어오기.
			startTime = System.nanoTime();
			getViewport(); //url의 페이지의 높이 ,길이, 확대값의 정보 가져오기
			restoreOut();

			String outputFolder = ""; //결과물 폴더명
			String oldWorkingDirectory = ""; //기존작업 폴더명
			String newWorkingDirectory = ""; //신규작업 폴더명

			if (_outputToFolder) //결과물을 저자할 폴더위치 지정. 있으면 새로 만들고 없으면 넘긴다.
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

			performSegmentation(pagenum);//블록 추출

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
			_url.openConnection(); // url 연결해서 url에 관한 인스턴스를 얻는다.

			redirectOut(); 

			getDomTree(_url); //url의 DOM tree얻어오기.
			startTime = System.nanoTime();
			getViewport(); //url의 페이지의 높이 ,길이, 확대값의 정보 가져오기
			restoreOut();

			String outputFolder = ""; //결과물 폴더명
			String oldWorkingDirectory = ""; //기존작업 폴더명
			String newWorkingDirectory = ""; //신규작업 폴더명

			if (_outputToFolder) //결과물을 저자할 폴더위치 지정. 있으면 새로 만들고 없으면 넘긴다.
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

			performSegmentation();//블록 추출

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
