package uni.rostock.de.bacnet.it.coap.examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LedFlash {

	private static final Logger LOG = LoggerFactory.getLogger(LedFlash.class);

	/* variables realated to OOB channel (push button related) */
	private static final String LEDDevicesBase = "/sys/class/gpio/gpio18";
	private static final String BUTTON_PATH = "/sys/class/gpio/gpio17";
	Path ledValuePath;
	Path ButtonValuePath;
	SecureRandom random = new SecureRandom();
	private String totalKey = "1010101";
	private String key = "";

	public void start() {

		int i = 0;

		if (Files.exists(Paths.get(LEDDevicesBase + "/value"))) {
			ledValuePath = Paths.get(LEDDevicesBase + "/value");
		} else {
			System.out.println("null path!!");
		}

		if (Files.exists(Paths.get(BUTTON_PATH + "/value"))) {
			ButtonValuePath = Paths.get(BUTTON_PATH + "/value");
		} else {
			System.out.println("null path!!");
		}
		for (int j = 0; j < 20; j++) {
			int bit = random.nextInt(2);
			key += bit;
		}
		totalKey+=key;
		System.out.println("entered key: " + totalKey);
		char[] bits = totalKey.toCharArray();
		System.out.println("Press CTRL+c to exit");
		try {

			/* Flash led so it is ready to take input via OOB */
			Files.write(ledValuePath, String.valueOf(1).getBytes());
			Thread.sleep(375);
			Files.write(ledValuePath, String.valueOf(0).getBytes());
			while (new String(Files.readAllBytes(ButtonValuePath)).charAt(0) == '0') {
				// wait for user initial press
				Thread.sleep(100);
			}
			Thread.sleep(1000);
			System.out.println("button pressed");
			while (i < 27) {
				Files.write(ledValuePath, String.valueOf(bits[i]).getBytes());
				Thread.sleep(100);
				Files.write(ledValuePath, String.valueOf(0).getBytes());
				Thread.sleep(100);
				i++;
			}
			System.out.println("entered key: " + key);
			/* wait for button press to start key exchange */
			while (new String(Files.readAllBytes(ButtonValuePath)).charAt(0) == '0') {
				// wait for user button press
			}
		} catch (Exception e) {
			System.out.println("Exception occured: " + e.getMessage());
		}
	}

	public String getOOBKeyAsString() {
		return this.key;
	}
}
