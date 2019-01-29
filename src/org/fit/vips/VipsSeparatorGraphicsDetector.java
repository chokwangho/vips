/*
 * Tomas Popela, 2012
 * VIPS - Visual Internet Page Segmentation
 * Module - VipsSeparatorGraphicsDetector.java
 */

package org.fit.vips;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.TextBox;

/**
 * Separator detector with possibility of generating graphics output.
 * @author Tomas Popela
 *
 */
public class VipsSeparatorGraphicsDetector extends JPanel implements VipsSeparatorDetector {

	private static final long serialVersionUID = 5825509847374498L;

	Graphics2D _pool = null;
	BufferedImage _image = null;
	VipsBlock _vipsBlocks = null;
	List<VipsBlock> _visualBlocks = null;
	private List<Separator> _horizontalSeparators = null;
	private List<Separator> _verticalSeparators = null;

	private int _cleanSeparatorsTreshold = 0;

	/**
	 * Defaults constructor.
	 * @param width Pools width
	 * @param height Pools height
	 */
	public VipsSeparatorGraphicsDetector(int width, int height) {
		this._image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		this._horizontalSeparators = new ArrayList<Separator>();
		this._verticalSeparators = new ArrayList<Separator>();
		this._visualBlocks = new ArrayList<VipsBlock>();
		createPool();
	}

	/**
	 * Adds visual block to pool.
	 * 
	 * @param vipsBlock
	 *            Visual block
	 */
	public void addVisualBlock(VipsBlock vipsBlock)
	{
		Box elementBox = vipsBlock.getBox();

		Rectangle rect = new Rectangle(elementBox.getAbsoluteContentX(),
				elementBox.getAbsoluteContentY(), elementBox.getContentWidth(),
				elementBox.getContentHeight());

		_pool.draw(rect);
		_pool.fill(rect);
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(_image, 0, 0, null);
	}

	private void fillPoolWithBlocks(List<VipsBlock> visualBlocks)
	{
		for (VipsBlock block : visualBlocks)
		{
			addVisualBlock(block);
		}
	}

	private void fillPoolWithBlocks(VipsBlock vipsBlock)
	{
		if (vipsBlock.isVisualBlock())
		{
			addVisualBlock(vipsBlock);
			_visualBlocks.add(vipsBlock);
		}

		for (VipsBlock vipsBlockChild : vipsBlock.getChildren())
			fillPoolWithBlocks(vipsBlockChild);
	}

	/**
	 * Fills pool with all visual blocks from VIPS blocks.
	 * 
	 */
	@Override
	public void fillPool()
	{
		createPool();
		if (_vipsBlocks != null)
			fillPoolWithBlocks(_vipsBlocks);
		else
			fillPoolWithBlocks(_visualBlocks);
	}

	/**
	 * Creates pool
	 */
	private void createPool()
	{
		// set black as pool background color
		_pool = _image.createGraphics();
		_pool.setColor(Color.white);
		_pool.fillRect(0, 0, _image.getWidth(), _image.getHeight());
		// set drawing color back to white
		_pool.setColor(Color.black);
	}

	/**
	 * Sets VIPS block, that will be used for separators computing.
	 * @param vipsBlock Visual structure
	 */
	@Override
	public void setVipsBlock(VipsBlock vipsBlock) 
	{
		this._vipsBlocks = vipsBlock;
		_visualBlocks.clear();
		fillPoolWithBlocks(vipsBlock);
		createPool();
	}

	/**
	 * Gets VIPS block that is used for separators computing.
	 * @return Vips blocks
	 */
	@Override
	public VipsBlock getVipsBlock()
	{
		return _vipsBlocks;
	}

	/**
	 * Sets VIPS block, that will be used for separators computing.
	 * @param visualBlocks List of visual blocks
	 */
	@Override
	public void setVisualBlocks(List<VipsBlock> visualBlocks)
	{
		this._visualBlocks.clear();
		this._visualBlocks.addAll(visualBlocks);
	}

	/**
	 * Gets VIPS block that is used for separators computing.
	 * @return Visual structure
	 */
	@Override
	public List<VipsBlock> getVisualBlocks()
	{
		return _visualBlocks;
	}

	/**
	 * Computes vertical visual separators
	 */
	private void findVerticalSeparators()
	{
		for (VipsBlock vipsBlock : _visualBlocks)
		{
			// add new visual block to pool
			addVisualBlock(vipsBlock);

			// block vertical coordinates
			int blockStart = vipsBlock.getBox().getAbsoluteContentX();
			int blockEnd = blockStart + vipsBlock.getBox().getContentWidth();

			// for each separator that we have in pool
			for (Separator separator : _verticalSeparators)
			{
				// find separator, that intersects with our visual block
				if (blockStart < separator.endPoint)
				{
					// next there are six relations that the separator and visual block can have

					// if separator is inside visual block
					if (blockStart < separator.startPoint && blockEnd >= separator.endPoint)
					{
						List<Separator> tempSeparators = new ArrayList<Separator>();
						tempSeparators.addAll(_verticalSeparators);

						//remove all separators, that are included in block
						for (Separator other : tempSeparators)
						{
							if (blockStart < other.startPoint && blockEnd > other.endPoint)
								_verticalSeparators.remove(other);
						}

						//find separator, that is on end of this block (if exists)
						for (Separator other : _verticalSeparators)
						{
							// and if it's necessary change it's start point
							if (blockEnd > other.startPoint && blockEnd < other.endPoint)
							{
								other.startPoint = blockEnd + 1;
								break;
							}
						}
						break;
					}
					// if block is inside another block -> skip it
					if (blockEnd < separator.startPoint)
						break;
					// if separator starts in the middle of block
					if (blockStart < separator.startPoint && blockEnd >= separator.startPoint)
					{
						// change separator start's point coordinate
						separator.startPoint = blockEnd+1;
						break;
					}
					// if block is inside the separator
					if (blockStart >= separator.startPoint && blockEnd <= separator.endPoint)
					{
						if (blockStart == separator.startPoint)
						{
							separator.startPoint = blockEnd+1;
							break;
						}
						if (blockEnd == separator.endPoint)
						{
							separator.endPoint = blockStart - 1;
							break;
						}
						// add new separator that starts behind the block
						_verticalSeparators.add(_verticalSeparators.indexOf(separator) + 1, new Separator(blockEnd + 1, separator.endPoint));
						// change end point coordinates of separator, that's before block
						separator.endPoint = blockStart - 1;
						break;
					}
					// if in one block is one separator ending and another one starting
					if (blockStart > separator.startPoint && blockStart < separator.endPoint)
					{
						// find the next one
						int nextSeparatorIndex =_verticalSeparators.indexOf(separator);

						// if it's not the last separator
						if (nextSeparatorIndex + 1 < _verticalSeparators.size())
						{
							Separator nextSeparator = _verticalSeparators.get(_verticalSeparators.indexOf(separator) + 1);

							// next separator is really starting before the block ends
							if (blockEnd > nextSeparator.startPoint && blockEnd < nextSeparator.endPoint)
							{
								// change separator start point coordinate
								separator.endPoint = blockStart - 1;
								nextSeparator.startPoint = blockEnd + 1;
								break;
							}
							else
							{
								List<Separator> tempSeparators = new ArrayList<Separator>();
								tempSeparators.addAll(_verticalSeparators);

								//remove all separators, that are included in block
								for (Separator other : tempSeparators)
								{
									if (blockStart < other.startPoint && other.endPoint < blockEnd)
									{
										_verticalSeparators.remove(other);
										continue;
									}
									if (blockEnd > other.startPoint && blockEnd < other.endPoint)
									{
										// change separator start's point coordinate
										other.startPoint = blockEnd+1;
										break;
									}
									if (blockStart > other.startPoint && blockStart < other.endPoint)
									{
										other.endPoint = blockStart-1;
										continue;
									}
								}
								break;
							}
						}
					}
					// if separator ends in the middle of block
					// change it's end point coordinate
					separator.endPoint = blockStart-1;
					break;
				}
			}
		}
	}

	/**
	 * Computes horizontal visual separators
	 */
	private void findHorizontalSeparators()
	{
		for (VipsBlock vipsBlock : _visualBlocks)
		{
			// add new visual block to pool
			addVisualBlock(vipsBlock);

			// block vertical coordinates
			int blockStart = vipsBlock.getBox().getAbsoluteContentY();
			int blockEnd = blockStart + vipsBlock.getBox().getContentHeight();

			// for each separator that we have in pool
			for (Separator separator : _horizontalSeparators)
			{
				// find separator, that intersects with our visual block
				if (blockStart < separator.endPoint)
				{
					// next there are six relations that the separator and visual block can have

					// if separator is inside visual block
					if (blockStart < separator.startPoint && blockEnd >= separator.endPoint)
					{
						List<Separator> tempSeparators = new ArrayList<Separator>();
						tempSeparators.addAll(_horizontalSeparators);

						//remove all separators, that are included in block
						for (Separator other : tempSeparators)
						{
							if (blockStart < other.startPoint && blockEnd > other.endPoint)
								_horizontalSeparators.remove(other);
						}

						//find separator, that is on end of this block (if exists)
						for (Separator other : _horizontalSeparators)
						{
							// and if it's necessary change it's start point
							if (blockEnd > other.startPoint && blockEnd < other.endPoint)
							{
								other.startPoint = blockEnd + 1;
								break;
							}
						}
						break;
					}
					// if block is inside another block -> skip it
					if (blockEnd < separator.startPoint)
						break;
					// if separator starts in the middle of block
					if (blockStart <= separator.startPoint && blockEnd >= separator.startPoint)
					{
						// change separator start's point coordinate
						separator.startPoint = blockEnd+1;
						break;
					}
					// if block is inside the separator
					if (blockStart >= separator.startPoint && blockEnd < separator.endPoint)
					{
						if (blockStart == separator.startPoint)
						{
							separator.startPoint = blockEnd+1;
							break;
						}
						if (blockEnd == separator.endPoint)
						{
							separator.endPoint = blockStart - 1;
							break;
						}
						// add new separator that starts behind the block
						_horizontalSeparators.add(_horizontalSeparators.indexOf(separator) + 1, new Separator(blockEnd + 1, separator.endPoint));
						// change end point coordinates of separator, that's before block
						separator.endPoint = blockStart - 1;
						break;
					}
					// if in one block is one separator ending and another one starting
					if (blockStart > separator.startPoint && blockStart < separator.endPoint)
					{
						// find the next one
						int nextSeparatorIndex =_horizontalSeparators.indexOf(separator);

						// if it's not the last separator
						if (nextSeparatorIndex + 1 < _horizontalSeparators.size())
						{
							Separator nextSeparator = _horizontalSeparators.get(_horizontalSeparators.indexOf(separator) + 1);

							// next separator is really starting before the block ends
							if (blockEnd > nextSeparator.startPoint && blockEnd < nextSeparator.endPoint)
							{
								// change separator start point coordinate
								separator.endPoint = blockStart - 1;
								nextSeparator.startPoint = blockEnd + 1;
								break;
							}
							else
							{
								List<Separator> tempSeparators = new ArrayList<Separator>();
								tempSeparators.addAll(_horizontalSeparators);

								//remove all separators, that are included in block
								for (Separator other : tempSeparators)
								{
									if (blockStart < other.startPoint && other.endPoint < blockEnd)
									{
										_horizontalSeparators.remove(other);
										continue;
									}
									if (blockEnd > other.startPoint && blockEnd < other.endPoint)
									{
										// change separator start's point coordinate
										other.startPoint = blockEnd+1;
										break;
									}
									if (blockStart > other.startPoint && blockStart < other.endPoint)
									{
										other.endPoint = blockStart-1;
										continue;
									}
								}
								break;
							}
						}
					}
					// if separator ends in the middle of block
					// change it's end point coordinate
					separator.endPoint = blockStart-1;
					break;
				}
			}
		}
	}

	/**
	 * Detects horizontal visual separators from Vips blocks.
	 */
	@Override
	public void detectHorizontalSeparators()
	{
		if (_visualBlocks.size() == 0)
		{
			System.err.println("I don't have any visual blocks!");
			return;
		}

		createPool();
		_horizontalSeparators.clear();
		_horizontalSeparators.add(new Separator(0, _image.getHeight()));

		findHorizontalSeparators();

		//remove pool borders
		List<Separator> tempSeparators = new ArrayList<Separator>();
		tempSeparators.addAll(_horizontalSeparators);

		for (Separator separator : tempSeparators)
		{
			if (separator.startPoint == 0)
				_horizontalSeparators.remove(separator);
			if (separator.endPoint == _image.getHeight())
				_horizontalSeparators.remove(separator);
		}

		if (_cleanSeparatorsTreshold != 0)
			cleanUpSeparators(_horizontalSeparators);

		computeHorizontalWeights();
		sortSeparatorsByWeight(_horizontalSeparators);
	}

	/**
	 * Detects vertical visual separators from Vips blocks.
	 */
	@Override
	public void detectVerticalSeparators()
	{
		if (_visualBlocks.size() == 0)
		{
			System.err.println("I don't have any visual blocks!");
			return;
		}

		createPool();
		_verticalSeparators.clear();
		_verticalSeparators.add(new Separator(0, _image.getWidth()));

		findVerticalSeparators();

		//remove pool borders
		List<Separator> tempSeparators = new ArrayList<Separator>();
		tempSeparators.addAll(_verticalSeparators);

		for (Separator separator : tempSeparators)
		{
			if (separator.startPoint == 0)
				_verticalSeparators.remove(separator);
			if (separator.endPoint == _image.getWidth())
				_verticalSeparators.remove(separator);
		}

		if (_cleanSeparatorsTreshold != 0)
			cleanUpSeparators(_verticalSeparators);
		computeVerticalWeights();
		sortSeparatorsByWeight(_verticalSeparators);
	}

	private void cleanUpSeparators(List<Separator> separators)
	{
		List<Separator> tempList = new ArrayList<Separator>();
		tempList.addAll(separators);

		for (Separator separator : tempList)
		{
			int width = separator.endPoint - separator.startPoint + 1;

			if (width < _cleanSeparatorsTreshold)
				separators.remove(separator);
		}
	}

	/**
	 * Sorts given separators by it's weight.
	 * @param separators Separators
	 */
	private void sortSeparatorsByWeight(List<Separator> separators)
	{
		Collections.sort(separators);
	}

	/**
	 * Computes weights for vertical separators.
	 */
	private void computeVerticalWeights()
	{
		for (Separator separator : _verticalSeparators)
		{
			ruleOne(separator);
			ruleTwo(separator, false);
			ruleThree(separator, false);
		}
	}

	/**
	 * Computes weights for horizontal separators.
	 */
	private void computeHorizontalWeights()
	{
		for (Separator separator : _horizontalSeparators)
		{
			ruleOne(separator);
			ruleTwo(separator, true);
			ruleThree(separator,true);
			ruleFour(separator);
			ruleFive(separator);
		}
	}

	/**
	 * The greater the distance between blocks on different
	 * side of the separator, the higher the weight. <p>
	 * For every 10 points of width we increase weight by 1 points.
	 * @param separator Separator
	 */
	private void ruleOne(Separator separator)
	{
		int width = separator.endPoint - separator.startPoint + 1;

		//separator.weight += width;

		if (width > 55 )
			separator.weight += 12;
		if (width > 45 && width <= 55)
			separator.weight += 10;
		if (width > 35 && width <= 45)
			separator.weight += 8;
		if (width > 25 && width <= 35)
			separator.weight += 6;
		else if (width > 15 && width <= 25)
			separator.weight += 4;
		else if (width > 8 && width <= 15)
			separator.weight += 2;
		else
			separator.weight += 1;

	}

	/**
	 * If a visual separator is overlapped with some certain HTML
	 * tags (e.g., the &lt;HR&gt; HTML tag), its weight is set to be higher.
	 * @param separator Separator
	 */
	private void ruleTwo(Separator separator, boolean horizontal)
	{
		List<VipsBlock> overlappedElements = new ArrayList<VipsBlock>();
		if (horizontal)
			findHorizontalOverlappedElements(separator, overlappedElements);
		else
			findVerticalOverlappedElements(separator, overlappedElements);

		if (overlappedElements.size() == 0)
			return;

		for (VipsBlock vipsBlock : overlappedElements)
		{
			if (vipsBlock.getBox().getNode().getNodeName().equals("hr"))
			{
				separator.weight += 2;
				break;
			}
		}
	}

	/**
	 * Finds elements that are overlapped with horizontal separator.
	 * @param separator Separator, that we look at
	 * @param vipsBlock Visual block corresponding to element
	 * @param result Elements, that we found
	 */
	private void findHorizontalOverlappedElements(Separator separator, List<VipsBlock> result)
	{
		for (VipsBlock vipsBlock : _visualBlocks)
		{
			int topEdge = vipsBlock.getBox().getAbsoluteContentY();
			int bottomEdge = topEdge + vipsBlock.getBox().getContentHeight();

			// two upper edges of element are overlapped with separator
			if (topEdge > separator.startPoint && topEdge < separator.endPoint && bottomEdge > separator.endPoint)
			{
				result.add(vipsBlock);
			}

			// two bottom edges of element are overlapped with separator
			if (topEdge < separator.startPoint && bottomEdge > separator.startPoint && bottomEdge < separator.endPoint)
			{
				result.add(vipsBlock);
			}

			// all edges of element are overlapped with separator
			if (topEdge >= separator.startPoint && bottomEdge <= separator.endPoint)
			{
				result.add(vipsBlock);
			}

		}
	}

	/**
	 * Finds elements that are overlapped with vertical separator.
	 * @param separator Separator, that we look at
	 * @param vipsBlock Visual block corresponding to element
	 * @param result Elements, that we found
	 */
	private void findVerticalOverlappedElements(Separator separator, List<VipsBlock> result)
	{
		for (VipsBlock vipsBlock : _visualBlocks)
		{
			int leftEdge = vipsBlock.getBox().getAbsoluteContentX();
			int rightEdge = leftEdge + vipsBlock.getBox().getContentWidth();

			// two left edges of element are overlapped with separator
			if (leftEdge > separator.startPoint && leftEdge < separator.endPoint && rightEdge > separator.endPoint)
			{
				result.add(vipsBlock);
			}

			// two right edges of element are overlapped with separator
			if (leftEdge < separator.startPoint && rightEdge > separator.startPoint && rightEdge < separator.endPoint)
			{
				result.add(vipsBlock);
			}

			// all edges of element are overlapped with separator
			if (leftEdge >= separator.startPoint && rightEdge <= separator.endPoint)
			{
				result.add(vipsBlock);
			}
		}
	}

	/**
	 * If background colors of the blocks on two sides of the separator
	 * are different, the weight will be increased.
	 * @param separator Separator
	 */
	private void ruleThree(Separator separator, boolean horizontal)
	{
		// for vertical is represents elements on left side
		List<VipsBlock> topAdjacentElements = new ArrayList<VipsBlock>();
		// for vertical is represents elements on right side
		List<VipsBlock> bottomAdjacentElements = new ArrayList<VipsBlock>();
		if (horizontal)
			findHorizontalAdjacentBlocks(separator, topAdjacentElements, bottomAdjacentElements);
		else
			findVerticalAdjacentBlocks(separator, topAdjacentElements, bottomAdjacentElements);

		if (topAdjacentElements.size() < 1 || bottomAdjacentElements.size() < 1)
			return;

		boolean weightIncreased = false;

		for (VipsBlock top : topAdjacentElements)
		{
			for (VipsBlock bottom : bottomAdjacentElements)
			{
				if (!top.getBgColor().equals(bottom.getBgColor()))
				{
					separator.weight += 2;
					weightIncreased = true;
					break;
				}
			}
			if (weightIncreased)
				break;
		}
	}

	/**
	 * Finds elements that are adjacent to horizontal separator.
	 * @param separator Separator, that we look at
	 * @param vipsBlock Visual block corresponding to element
	 * @param resultTop Elements, that we found on top side of separator
	 * @param resultBottom Elements, that we found on bottom side side of separator
	 */
	private void findHorizontalAdjacentBlocks(Separator separator, List<VipsBlock> resultTop, List<VipsBlock> resultBottom)
	{
		for (VipsBlock vipsBlock : _visualBlocks)
		{
			int topEdge = vipsBlock.getBox().getAbsoluteContentY();
			int bottomEdge = topEdge + vipsBlock.getBox().getContentHeight();

			// if box is adjancent to separator from bottom
			if (topEdge == separator.endPoint + 1 && bottomEdge > separator.endPoint + 1)
			{
				resultBottom.add(vipsBlock);
			}

			// if box is adjancent to separator from top
			if (bottomEdge == separator.startPoint - 1 && topEdge < separator.startPoint - 1)
			{
				resultTop.add(0, vipsBlock);
			}
		}
	}

	/**
	 * Finds elements that are adjacent to vertical separator.
	 * @param separator Separator, that we look at
	 * @param vipsBlock Visual block corresponding to element
	 * @param resultLeft Elements, that we found on left side of separator
	 * @param resultRight Elements, that we found on right side side of separator
	 */
	private void findVerticalAdjacentBlocks(Separator separator, List<VipsBlock> resultLeft, List<VipsBlock> resultRight)
	{
		for (VipsBlock vipsBlock : _visualBlocks)
		{
			int leftEdge = vipsBlock.getBox().getAbsoluteContentX() + 1;
			int rightEdge = leftEdge + vipsBlock.getBox().getContentWidth();

			// if box is adjancent to separator from right
			if (leftEdge == separator.endPoint + 1 && rightEdge > separator.endPoint + 1)
			{
				resultRight.add(vipsBlock);
			}

			// if box is adjancent to separator from left
			if (rightEdge == separator.startPoint - 1 && leftEdge < separator.startPoint - 1)
			{
				resultLeft.add(0, vipsBlock);
			}
		}
	}

	/**
	 * For horizontal separators, if the differences of font properties
	 * such as font size and font weight are bigger on two
	 * sides of the separator, the weight will be increased.
	 * Moreover, the weight will be increased if the font size of the block
	 * above the separator is smaller than the font size of the block
	 * below the separator.
	 * @param separator Separator
	 */
	private void ruleFour(Separator separator)
	{
		List<VipsBlock> topAdjacentElements = new ArrayList<VipsBlock>();
		List<VipsBlock> bottomAdjacentElements = new ArrayList<VipsBlock>();

		findHorizontalAdjacentBlocks(separator, topAdjacentElements, bottomAdjacentElements);

		if (topAdjacentElements.size() < 1 || bottomAdjacentElements.size() < 1)
			return;

		boolean weightIncreased = false;

		for (VipsBlock top : topAdjacentElements)
		{
			for (VipsBlock bottom : bottomAdjacentElements)
			{
				int diff = Math.abs(top.getFontSize() - bottom.getFontSize());
				if (diff != 0)
				{
					separator.weight += 2;
					weightIncreased = true;
					break;
				}
				else
				{
					if (!top.getFontWeight().equals(bottom.getFontWeight()))
					{
						separator.weight += 2;
					}
				}
			}
			if (weightIncreased)
				break;
		}

		weightIncreased = false;

		for (VipsBlock top : topAdjacentElements)
		{
			for (VipsBlock bottom : bottomAdjacentElements)
			{
				if (top.getFontSize() < bottom.getFontSize())
				{
					separator.weight += 2;
					weightIncreased = true;
					break;
				}
			}
			if (weightIncreased)
				break;
		}
	}

	/**
	 * For horizontal separators, when the structures of the blocks on the two
	 * sides of the separator are very similar (e.g. both are text),
	 * the weight of the separator will be decreased.
	 * @param separator Separator
	 */
	private void ruleFive(Separator separator)
	{
		List<VipsBlock> topAdjacentElements = new ArrayList<VipsBlock>();
		List<VipsBlock> bottomAdjacentElements = new ArrayList<VipsBlock>();

		findHorizontalAdjacentBlocks(separator, topAdjacentElements, bottomAdjacentElements);

		if (topAdjacentElements.size() < 1 || bottomAdjacentElements.size() < 1)
			return;

		boolean weightDecreased = false;

		for (VipsBlock top : topAdjacentElements)
		{
			for (VipsBlock bottom : bottomAdjacentElements)
			{
				if (top.getBox() instanceof TextBox &&
						bottom.getBox() instanceof TextBox)
				{
					separator.weight -= 2;
					weightDecreased = true;
					break;
				}
			}
			if (weightDecreased)
				break;
		}
	}

	/**
	 * Saves everything (separators + block) to image.
	 */
	public void exportAllToImage()
	{
		createPool();
		fillPool();
		drawVerticalSeparators();
		drawHorizontalSeparators();
		saveToImage("all");
	}

	/**
	 * Saves everything (separators + block) to image with given suffix.
	 */
	public void exportAllToImage(int suffix)
	{
		createPool();
		fillPoolWithBlocks(_visualBlocks);
		drawVerticalSeparators();
		drawHorizontalSeparators();
		saveToImage("iteration" + suffix);
	}

	/**
	 * Adds all detected vertical separators to pool
	 */
	private void drawVerticalSeparators()
	{
		_pool.setColor(Color.red);
		for (Separator separator : _verticalSeparators)
		{
			Rectangle rect;
			if (separator.leftUp != null)
				rect = new Rectangle(separator.leftUp, new Dimension(
						(int) (separator.rightDown.getX() - separator.leftUp.getX()),
						(int) (separator.rightDown.getY() - separator.leftUp.getY())));
			else
				rect = new Rectangle(separator.startPoint, 0, separator.endPoint - separator.startPoint, _image.getHeight());

			_pool.draw(rect);
			_pool.fill(rect);
		}
	}

	/**
	 * Saves vertical separators to image.
	 */
	public void exportVerticalSeparatorsToImage()
	{
		createPool();
		drawVerticalSeparators();
		saveToImage("verticalSeparators");
	}

	/**
	 * Saves vertical separators to image.
	 */
	public void exportVerticalSeparatorsToImage(int suffix)
	{
		createPool();
		drawVerticalSeparators();
		saveToImage("verticalSeparators" + suffix);
	}

	/**
	 * Adds all detected horizontal separators to pool
	 */
	private void drawHorizontalSeparators()
	{
		_pool.setColor(Color.blue);
		for (Separator separator : _horizontalSeparators)
		{
			Rectangle rect;
			if (separator.leftUp != null)
				rect = new Rectangle(separator.leftUp, new Dimension(
						(int) (separator.rightDown.getX() - separator.leftUp.getX()),
						(int) (separator.rightDown.getY() - separator.leftUp.getY())));
			else
				rect = new Rectangle(0, separator.startPoint, _image.getWidth(), separator.endPoint - separator.startPoint);

			_pool.draw(rect);
			_pool.fill(rect);
		}
	}

	/**
	 * Saves horizontal separators to image.
	 */
	public void exportHorizontalSeparatorsToImage()
	{
		createPool();
		drawHorizontalSeparators();
		saveToImage("horizontalSeparators");
	}

	/**
	 * Saves horizontal separators to image.
	 */
	public void exportHorizontalSeparatorsToImage(int suffix)
	{
		createPool();
		drawHorizontalSeparators();
		saveToImage("horizontalSeparators" + suffix);
	}

	/**
	 * Saves pool to image
	 */
	public void saveToImage(String filename)
	{
		filename = System.getProperty("user.dir") + "/" + filename + ".png";
		try
		{
			ImageIO.write(_image, "png", new File(filename));
		} catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Saves pool to image
	 */
	public void saveToImage(String filename, String folder)
	{
		if (folder.equals(""))
			return;

		filename = folder + "/" + filename + ".png";

		try
		{
			ImageIO.write(_image, "png", new File(filename));
		} catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * @return the _horizontalSeparators
	 */
	@Override
	public List<Separator> getHorizontalSeparators()
	{
		return _horizontalSeparators;
	}

	@Override
	public void setHorizontalSeparators(List<Separator> separators)
	{
		_horizontalSeparators.clear();
		_horizontalSeparators.addAll(separators);
	}

	@Override
	public void setVerticalSeparators(List<Separator> separators)
	{
		_verticalSeparators.clear();
		_verticalSeparators.addAll(separators);
	}

	/**
	 * @return the _verticalSeparators
	 */
	@Override
	public List<Separator> getVerticalSeparators()
	{
		return _verticalSeparators;
	}

	@Override
	public void setCleanUpSeparators(int treshold)
	{
		this._cleanSeparatorsTreshold = treshold;
	}

	@Override
	public boolean isCleanUpEnabled()
	{
		if (_cleanSeparatorsTreshold == 0)
			return true;

		return false;
	}
}
