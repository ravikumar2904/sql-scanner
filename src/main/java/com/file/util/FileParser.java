package com.file.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.file.util.vo.Box;
import com.file.util.vo.BoxChild;
import com.file.util.vo.KshChild;

/**
 * @author
 *
 */
public class FileParser {

	public static String SAMPLE_XLSX_FILE_PATH = "." + File.separatorChar;
	private static final String SH_DIRECTORY = "." + File.separatorChar + "shfiles";

	public static void main(String[] args) throws IOException, InvalidFormatException {
		if (args.length == 0) {
			throw new RuntimeException("Required input xslx file");
		}
		SAMPLE_XLSX_FILE_PATH = SAMPLE_XLSX_FILE_PATH + args[0];

		File shDirectory = new File(SH_DIRECTORY);
		if (!shDirectory.exists()) {
			shDirectory.mkdir();
		}
		Map<String, List<String>> vapAndBoxListMap = WorkBookHelper.getVapNameAndBoxList(SAMPLE_XLSX_FILE_PATH);

		Map<String, List<Box>> resultVapAndBoxList = processVaps(vapAndBoxListMap);

		WorkBookHelper.writeResultToExcel(resultVapAndBoxList);

	}

	/**
	 * Iterates the vap and list of boxs for the vap.
	 * 
	 * @param vapAndBoxListMap.
	 * @return
	 */
	private static Map<String, List<Box>> processVaps(Map<String, List<String>> vapAndBoxListMap) {
		Map<String, List<Box>> resultVapAndBoxList = new LinkedHashMap<>();

		for (Entry<String, List<String>> entry : vapAndBoxListMap.entrySet()) {
			List<Box> boxs = new ArrayList<>();
			resultVapAndBoxList.put(entry.getKey(), boxs);
			Set<String> cfgFiles = new LinkedHashSet<>();

			for (String txtFileTobeProcessed : entry.getValue()) {
				Box box = new Box();
				box.setName(txtFileTobeProcessed);

				List<BoxChild> boxChilds = getBoxChilds(txtFileTobeProcessed,cfgFiles);

				box.setBoxChilds(boxChilds);
				boxs.add(box);
			}
		}
		return resultVapAndBoxList;
	}

	/**
	 * Extracts the child's of the box .Each job being the child of box.
	 * Recursively iterates the scripts to extract the sql and params invoked in
	 * the script.
	 * 
	 * @param txtFileTobeProcessed
	 * @param cfgFiles 
	 * @return
	 */
	private static List<BoxChild> getBoxChilds(String txtFileTobeProcessed, Set<String> cfgFiles) {
		List<BoxChild> jobs = JobExtractor.getJobs(txtFileTobeProcessed);
		for (BoxChild boxChild : jobs) {
			List<String> files = boxChild.getFilesTobeScanned();
			List<KshChild> childs = new ArrayList<>();
			for (String fileTobeProcessed : files) {
				// To remove duplicate and recurive parsing
				HashSet<String> fileNames = new HashSet<>();
				fileNames.add(fileTobeProcessed);
				scanFile(fileTobeProcessed, childs, fileNames, boxChild.getCmdParams(),cfgFiles);
			} 
			boxChild.setKshChilds(childs);

		}
		return jobs;
	}

	/**
	 * Recursively parses the files extracts tge sqls and params.
	 * @param fileTobeProcessed
	 * @param childs
	 * @param fileNames
	 * @param cmdParams
	 * @param cfgFiles 
	 */
	private static void scanFile(String fileTobeProcessed, List<KshChild> childs, HashSet<String> fileNames,
			Map<String, String> cmdParams, Set<String> cfgFiles) {

		List<String> childScripts = GetScriptsInTheFile.getScripts(fileTobeProcessed, fileNames,cfgFiles);
		if (!childScripts.isEmpty()) {
			for (String filename : childScripts) {
				scanFile(filename, childs, fileNames, cmdParams,cfgFiles);
			}
		} 

		KshChild boxChild = SqlExtractor.exctractSqlCommands(fileTobeProcessed, cmdParams);
		childs.add(boxChild);

	}

}
