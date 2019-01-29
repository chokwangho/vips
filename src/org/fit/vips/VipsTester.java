/*
 * Tomas Popela, 2012
 * VIPS - Visual Internet Page Segmentation
 * Module - VipsTester.java
 */

package org.fit.vips;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



/**
 * VIPS API example application.
 * @author Tomas Popela
 *
 */
public class VipsTester {
	
	/**
	 * Main function
	 * @param args Internet address of web page.
	 */
	public static void main(String args[])
	{
		// we've just one argument - web address of page
//		if (args.length != 1)
//		{
//			System.err.println("We've just only one argument - web address of page!");
//			System.exit(0);
//		}
		//db에서 url뽑아서 쓰는법 시작
//		String url = "";
//		//10월18일추가 
//		//여기서 db에서 페이지 받아와서 url에 넣어주고 for문으로 페이지 개수만큼 xml돌려서 xml파일 생성.
//		Connection connection = null;
//		Statement st = null;
//		ResultSet rs = null;
//		//int count=1; //추가
//		String pagenum="";
//		try{
//			
//			Class.forName("com.mysql.cj.jdbc.Driver");
//			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test_vips?serverTimezone=UTC&useSSL=false", "root","cho2128ho!");
//	
//			st= connection.createStatement();
//			
//			String sql;
//			sql = "SELECT page_number,page_url FROM pageurl";
//	
//			rs= st.executeQuery(sql);
//			
//			while(rs.next()){
//				url=rs.getString("page_url");
//				pagenum = rs.getString("page_number");
//			//VIPS실행 1개 주소만돌리려면 여기만 try catch씌운다. 
//				Vips vips = new Vips();
//				// disable graphics output
//				vips.enableGraphicsOutput(false);
//				// disable output to separate folder (no necessary, it's default value is false)
//				vips.enableOutputToFolder(false);
//				// set permitted degree of coherence
//				vips.setPredefinedDoC(8);
//				// start segmentation on page
//				vips.startSegmentation(url,pagenum);
//				System.out.println(pagenum+"번쨰xml생성"); //추가
//				//count+=1;//츄가
//			}
//			}catch(SQLException se1){
//				se1.printStackTrace();
//			}catch(Exception ex){
//				ex.printStackTrace();
//			}finally{
//				try{
//					if(st!=null){
//						st.close();
//					}
//				}catch(SQLException se2){
//					
//				}
//				try{
//					if(connection!= null)
//						connection.close();
//				}catch(SQLException se){
//					se.printStackTrace();
//				}
//				
//			}
//		
		//db에서 url뽑아서 쓰는법 끝
		
		String url="https://www.naver.com";
		String pagenum="test";
	//VIPS실행 1개 주소만돌리려면 여기만 try catch씌운다. 
		Vips vips = new Vips();
		// disable graphics output
		vips.enableGraphicsOutput(false);
		// disable output to separate folder (no necessary, it's default value is false)
		vips.enableOutputToFolder(false);
		// set permitted degree of coherence
		vips.setPredefinedDoC(8);
		// start segmentation on page
		vips.startSegmentation(url,pagenum);
//		try
//		{	while(rs.next()){
//				url=rs.getString("page_url");
//			System.out.println(rs.getString("page_url"));
//
//				Vips vips = new Vips();
//				// disable graphics output
//				vips.enableGraphicsOutput(false);
//				// disable output to separate folder (no necessary, it's default value is false)
//				vips.enableOutputToFolder(false);
//				// set permitted degree of coherence
//				vips.setPredefinedDoC(8);
//				// start segmentation on page
//				vips.startSegmentation(url);
//			}
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//		}
	}
}
