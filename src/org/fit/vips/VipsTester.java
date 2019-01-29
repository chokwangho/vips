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
		//db���� url�̾Ƽ� ���¹� ����
//		String url = "";
//		//10��18���߰� 
//		//���⼭ db���� ������ �޾ƿͼ� url�� �־��ְ� for������ ������ ������ŭ xml������ xml���� ����.
//		Connection connection = null;
//		Statement st = null;
//		ResultSet rs = null;
//		//int count=1; //�߰�
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
//			//VIPS���� 1�� �ּҸ��������� ���⸸ try catch�����. 
//				Vips vips = new Vips();
//				// disable graphics output
//				vips.enableGraphicsOutput(false);
//				// disable output to separate folder (no necessary, it's default value is false)
//				vips.enableOutputToFolder(false);
//				// set permitted degree of coherence
//				vips.setPredefinedDoC(8);
//				// start segmentation on page
//				vips.startSegmentation(url,pagenum);
//				System.out.println(pagenum+"����xml����"); //�߰�
//				//count+=1;//��
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
		//db���� url�̾Ƽ� ���¹� ��
		
		String url="https://www.naver.com";
		String pagenum="test";
	//VIPS���� 1�� �ּҸ��������� ���⸸ try catch�����. 
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
