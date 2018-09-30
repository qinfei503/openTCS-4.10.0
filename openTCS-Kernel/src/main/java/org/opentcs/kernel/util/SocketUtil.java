package org.opentcs.kernel.util;

import java.io.*;

public class SocketUtil {
	public static byte[] intToBytes(int num){
		byte[] bytes = new byte[4];
		bytes[0]=(byte)(0xff & (num >> 0));
		bytes[1]=(byte)(0xff & (num >> 8));
		bytes[2]=(byte)(0xff & (num >> 16));
		bytes[3]=(byte)(0xff & (num >> 24));
		return bytes;
		
	}
	
	public static int bytesToInt(byte[] bytes){
		int num =0;
		int temp;
		temp=(0x000000ff & (bytes[0])) <<0;
		num=num | temp;
		
		temp=(0x000000ff & (bytes[1])) <<8;
		num=num | temp;
		
		temp=(0x000000ff & (bytes[2])) <<16;
		num=num | temp;
		
		temp=(0x000000ff & (bytes[3])) <<24;
		num=num | temp;
		
		return num;
	}
	
	public static byte[] messageToBytesWithLength(String message) throws UnsupportedEncodingException {
		byte bytes[]=message.getBytes("UTF-8");
		byte dataLengthByte[]=intToBytes(bytes.length);
		byte[] sendBytes=new byte[bytes.length+4];
		System.arraycopy(dataLengthByte, 0, sendBytes, 0, 4);
		System.arraycopy(bytes, 0, sendBytes, 4, bytes.length);
		return sendBytes;
		
	}

	public static String readNextPacketData(InputStream in) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(in);
		byte[] head = new byte[4];
		while(bis.read(head)<0);
		int dataLength=bytesToInt(head);
		byte[] data=new byte[dataLength];
		if(bis.read(data)<0){
			return "";
		}
		return new String(data,"UTF-8");
	}

	public static void sendPacketData(OutputStream out,String message) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(out);
		bos.write(messageToBytesWithLength(message));
		bos.flush();
	}
}
