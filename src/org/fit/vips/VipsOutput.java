/*
 * Tomas Popela, 2012
 * VIPS - Visual Internet Page Segmentation
 * Module - VipsOutput.java
 */

package org.fit.vips;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.Viewport;
import org.fit.vips.VipsBlock;
import org.fit.vips.VisualStructure;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
/**
 * Class, that handles output of VIPS algorithm.
 * @author Tomas Popela
 *
 */
public final class VipsOutput {

	private Document doc = null;
	private boolean _escapeOutput = true;
	private int _pDoC = 0;
	private int _order = 1;
	private String _filename = "VIPSResult";
	//static int _filenum=0;
	public VipsOutput() {
	}

	public VipsOutput(int pDoC) {
		this.setPDoC(pDoC);
	}

	/**
	 * Gets source code of visual structure nodes
	 * @param node Given node
	 * @return Source code
	 */
	private String getSource(Element node)
	{
		String content = "";
		try
		{
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			StringWriter buffer = new StringWriter();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(node), new StreamResult(buffer));
			content = buffer.toString().replaceAll("\n", "");
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return content;
	}

	/**
	 * Append node from given visual structure to parent node
	 * @param parentNode Given visual structure
	 * @param visualStructure Parent node
	 */
	private void writeVisualBlocks(Element parentNode, VisualStructure visualStructure)
	{
		Element layoutNode = doc.createElement("LayoutNode"); //DOM객체에 LayoutNode요소를 만들어주고
		//visualStruce에서 정보를 가져다 넣어준다.
		layoutNode.setAttribute("FrameSourceIndex", String.valueOf(visualStructure.getFrameSourceIndex()));
		layoutNode.setAttribute("SourceIndex", visualStructure.getSourceIndex());
		layoutNode.setAttribute("DoC", String.valueOf(visualStructure.getDoC()));
		layoutNode.setAttribute("ContainImg", String.valueOf(visualStructure.containImg()));
		layoutNode.setAttribute("IsImg", String.valueOf(visualStructure.isImg()));
		layoutNode.setAttribute("ContainTable", String.valueOf(visualStructure.containTable()));
		layoutNode.setAttribute("ContainP", String.valueOf(visualStructure.containP()));
		layoutNode.setAttribute("TextLen", String.valueOf(visualStructure.getTextLength()));
		layoutNode.setAttribute("LinkTextLen", String.valueOf(visualStructure.getLinkTextLength()));
		Box parentBox = visualStructure.getNestedBlocks().get(0).getBox().getParent();
		layoutNode.setAttribute("DOMCldNum", String.valueOf(parentBox.getNode().getChildNodes().getLength()));
		layoutNode.setAttribute("FontSize", String.valueOf(visualStructure.getFontSize()));
		layoutNode.setAttribute("FontWeight", String.valueOf(visualStructure.getFontWeight()));
		layoutNode.setAttribute("BgColor", visualStructure.getBgColor());
		layoutNode.setAttribute("ObjectRectLeft", String.valueOf(visualStructure.getX()));
		layoutNode.setAttribute("ObjectRectTop", String.valueOf(visualStructure.getY()));
		layoutNode.setAttribute("ObjectRectWidth", String.valueOf(visualStructure.getWidth()));
		layoutNode.setAttribute("ObjectRectHeight", String.valueOf(visualStructure.getHeight()));
		layoutNode.setAttribute("ID", visualStructure.getId());
		layoutNode.setAttribute("order", String.valueOf(_order));

		_order++;

		if (_pDoC >= visualStructure.getDoC())
		{
			// continue segmenting
			if (visualStructure.getChildrenVisualStructures().size() == 0)
			{
				if (visualStructure.getNestedBlocks().size() > 0)
				{
					String src = "";
					String content = "";
					for (VipsBlock block : visualStructure.getNestedBlocks())
					{
						ElementBox elementBox = block.getElementBox();

						if (elementBox == null)
							continue;

						if (!elementBox.getNode().getNodeName().equals("Xdiv") &&
								!elementBox.getNode().getNodeName().equals("Xspan"))
							src += getSource(elementBox.getElement());
						else
							src += elementBox.getText();

						content += elementBox.getText() + " ";

					}
					layoutNode.setAttribute("SRC", src);
					layoutNode.setAttribute("Content", content);
				}
			}

			parentNode.appendChild(layoutNode);

			for (VisualStructure child : visualStructure.getChildrenVisualStructures())
				writeVisualBlocks(layoutNode, child);
		}
		else
		{
			// "stop" segmentation
			if (visualStructure.getNestedBlocks().size() > 0)
			{
				String src = ""; //각 노드 속성들
				String content = ""; //속성 값들
				for (VipsBlock block : visualStructure.getNestedBlocks())
				{
					ElementBox elementBox = block.getElementBox(); 

					if (elementBox == null)
						continue;
					//각각의 노드들에서 xdiv, xspan있으면 xml에 넣어줘라. 여기가 찾아서 써주는 곳
					if (!elementBox.getNode().getNodeName().equals("Xdiv") &&
							!elementBox.getNode().getNodeName().equals("Xspan"))
						src += getSource(elementBox.getElement());
					else
						src += elementBox.getText();

					content += elementBox.getText() + " ";

				}//값을  layoutNode에 넣어준다.
				layoutNode.setAttribute("SRC", src);
				layoutNode.setAttribute("Content", content);
			}
			
			parentNode.appendChild(layoutNode);
			
		}
	}

	/**
	 * Writes visual structure to output XML
	 * @param visualStructure Given visual structure
	 * @param pageViewport Page's viewport
	 */
	public void writeXML(VisualStructure visualStructure, Viewport pageViewport)
	{
		try
		{	//Document객체를 구하기 위해서는 DocumentBuilder가 필요하며, DocumentBuilder객체는 DocumentBuilderFactory에 의해 ㅁㄴ들어진다.
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			doc = docBuilder.newDocument(); //visualStructure를 넣어줄 빈 Document객체를 만들어준다.
			Element vipsElement = doc.createElement("VIPSPage"); //빈 Document객체형식의 VIPSPage라는 요소를 만들어주고 .

			String pageTitle = pageViewport.getRootElement().getOwnerDocument().getElementsByTagName("title").item(0).getTextContent();

			vipsElement.setAttribute("Url", pageViewport.getRootBox().getBase().toString());
			vipsElement.setAttribute("PageTitle", pageTitle);
			vipsElement.setAttribute("WindowWidth", String.valueOf(pageViewport.getContentWidth()));
			vipsElement.setAttribute("WindowHeight", String.valueOf(pageViewport.getContentHeight()));
			vipsElement.setAttribute("PageRectTop", String.valueOf(pageViewport.getAbsoluteContentY()));
			vipsElement.setAttribute("PageRectLeft", String.valueOf(pageViewport.getAbsoluteContentX()));
			vipsElement.setAttribute("PageRectWidth", String.valueOf(pageViewport.getContentWidth()));
			vipsElement.setAttribute("PageRectHeight", String.valueOf(pageViewport.getContentHeight()));
			vipsElement.setAttribute("neworder", "0");
			vipsElement.setAttribute("order", String.valueOf(pageViewport.getOrder()));

			doc.appendChild(vipsElement);//VIPSPage의 기본 설정을 빈 Document객체에 넣어준다. 

			writeVisualBlocks(vipsElement, visualStructure); //visualStructure의 vipsvisual트리들을 써준다. 그래서 xml의 형식을 바꾸고싶으면 여기를 바꿔야한다.
			//xml파일 만들기위해 변환 준비
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);

			if (_escapeOutput)//변환 하라면
			{
				StreamResult result = new StreamResult(new File(_filename + ".xml")); //
				transformer.transform(source, result); //xml파일 변환
				
			}
			else
			{
				StringWriter writer = new StringWriter();
				transformer.transform(source, new StreamResult(writer));
				String result = writer.toString();

				result = result.replaceAll("&gt;", ">");
				result = result.replaceAll("&lt;", "<");
				result = result.replaceAll("&quot;", "\"");

				FileWriter fstream = new FileWriter(_filename + ".xml");
				fstream.write(result);
				fstream.close();
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Enables or disables output escaping
	 * @param value
	 */
	public void setEscapeOutput(boolean value)
	{
		_escapeOutput = value;
	}

	/**
	 * Sets permitted degree of coherence pDoC
	 * @param pDoC pDoC value
	 */
	public void setPDoC(int pDoC)
	{
		if (pDoC <= 0 || pDoC> 11)
		{
			System.err.println("pDoC value must be between 1 and 11! Not " + pDoC + "!");
			return;
		}
		else
		{
			_pDoC = pDoC;
		}
	}

	/**
	 * Sets output filename
	 * @param filename Filename
	 */
	public void setOutputFileName(String filename,String pagenum)
	{
		if (!filename.equals(""))
		{	//10월 18일 추가 
			//여기서 filename의 result 1~500을 1씩 늘려준다.
			
			_filename = filename +pagenum ;
		}

	}
 
	public void setOutputFileName(String filename)
	{
		if (!filename.equals(""))
		{	//10월 18일 추가 
			//여기서 filename의 result 1~500을 1씩 늘려준다.
			
			_filename = filename;
		}

	}
}
