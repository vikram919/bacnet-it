package uni.rostock.de.bacnet.it.coap.examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushButtonJob {

	private static final Logger LOG = LoggerFactory.getLogger(PushButtonJob.class);

	/* variables realated to OOB channel (push button related) */
	private static final String LEDDevicesBase = "/sys/class/gpio/gpio18";
	private static final String BUTTON_PATH = "/sys/class/gpio/gpio17";
	private Path ledValuePath;
	private Path ButtonValuePath;
	private long mTimestamp1;
	private long mTimestamp2;
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
		LOG.info("Press CTRL+c to exit");
		try {
			/* Flash led so it is ready to take input via OOB */
			Files.write(ledValuePath, String.valueOf(1).getBytes());
			Thread.sleep(375);
			Files.write(ledValuePath, String.valueOf(0).getBytes());
			Thread.sleep(375);
			while (i < 20) {
				if (new String(Files.readAllBytes(ButtonValuePath)).charAt(0) != '0') {
					System.out.println("button pressed!!!");
					mTimestamp1 = System.nanoTime();
					while (new String(Files.readAllBytes(ButtonValuePath)).charAt(0) != '0') {
						// Do nothing
					}
					mTimestamp2 = System.nanoTime();
					double delay = (double) ((mTimestamp2 - mTimestamp1) / 1000000000.0000);
					System.out.println("delay: " + delay);
					if (delay >= 0.5) {
						key += "0";
					} else {
						key += "1";
					}
					i++;
				}
				Thread.sleep(100);
			}

			/* Flash led as an indication that input is completed!!! */
			Files.write(ledValuePath, String.valueOf(1).getBytes());
			Thread.sleep(375);
			Files.write(ledValuePath, String.valueOf(0).getBytes());
			Thread.sleep(375);
			System.out.println("entered key: " + key);
		} catch (Exception e) {
			System.out.println("Exception occured: " + e.getMessage());
		}
	}

	public String getOOBKeyAsString() {
		return this.key;
	}
}
