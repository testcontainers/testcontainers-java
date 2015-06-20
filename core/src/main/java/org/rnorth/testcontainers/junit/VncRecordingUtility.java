package org.rnorth.testcontainers.junit;

/**
 * Created by richardnorth on 13/06/15.
 */
public class VncRecordingUtility {

//    private static final Logger LOGGER = LoggerFactory.getLogger(VncRecordingUtility.class);
//    private static final Pattern VNC_URL_PATTERN = Pattern.compile("vnc://[^:]+:(?<password>[^@]+)@(?<host>[^:]+):(?<port>\\d+)");
//    private final String vncURL;
//    private final File outputFilename;
//    private VncRecordingSidekickContainer sidekickContainer;
//
//    public VncRecordingUtility(String vncURL, File outputFilename) {
//        this.vncURL = vncURL;
//        this.outputFilename = outputFilename;
//    }
//
//    public void start() {
//        Matcher matcher = VNC_URL_PATTERN.matcher(vncURL);
//        if (!matcher.matches()) {
//            throw new IllegalArgumentException("VNC URL could not be parsed! " + vncURL);
//        }
//
//        String host = matcher.group("host");
//        String port = matcher.group("port");
//        String password = matcher.group("password");
//        sidekickContainer = new VncRecordingSidekickContainer("recording.flv", password, host, port);
//        sidekickContainer.start();
//    }
//
//    public void stop(boolean discard) {
//        if (sidekickContainer == null) {
//            return;
//        }
//
//        sidekickContainer.stop(false);
//
//        if (!discard) {
//            try {
//                Files.move(sidekickContainer.getRecordingPath(), this.outputFilename.toPath());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
}
