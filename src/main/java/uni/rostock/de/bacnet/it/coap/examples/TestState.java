package uni.rostock.de.bacnet.it.coap.examples;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

public class TestState {

	public static void main(String[] args) {
		System.out.println(readState("State.txt"));
		writeState("State.txt");
		System.out.println(readState("State.txt"));
	}

	public static String readState(String filePath) {
		File file = new File(filePath);
		String state = null;
		try {
			state = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return state;
	}
	public static void writeState(String filePath) {
		File file = new File(filePath);
		try {
			FileUtils.writeStringToFile(file, "IMPRINTED", StandardCharsets.UTF_8, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
