package net.qihoo.guessthepattern.utils;

import com.alibaba.fastjson.JSON;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.List;

public class TempUtils {
	public static String getRandom() {
		List<String> resList = new ClassPathResourceReader("data/temp.txt").getContent();
		int random = (int) Math.floor(Math.random() * resList.size());
		return resList.get(random);
	}

	public static List<String> getAll() {
		List<String> resList = new ClassPathResourceReader("data/temp.txt").getContent();
		return resList;
	}

	/**
	 * 读取json文件，返回json串
	 *
	 * @param fileName
	 * @return
	 */
	public static String readJsonFile(String fileName) {
		String jsonStr = "";
		try {
			File jsonFile = ResourceUtils.getFile("classpath:"+fileName);
			FileReader fileReader = new FileReader(jsonFile);
			Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
			int ch = 0;
			StringBuffer sb = new StringBuffer();
			while ((ch = reader.read()) != -1) {
				sb.append((char) ch);
			}
			fileReader.close();
			reader.close();
			jsonStr = sb.toString();
			return jsonStr;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
