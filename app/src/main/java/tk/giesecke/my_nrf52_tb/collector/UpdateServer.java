package tk.giesecke.my_nrf52_tb.collector;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class UpdateServer extends NanoHTTPD {

    private FileInputStream fwStream;
    private long fwLen;

    UpdateServer() {
        super(12345);
        Log.i("WEB","\nRunning! Point your browsers to http://localhost:12345/ \n");
    }

    @Override
    public void start() throws IOException {
        if (fwStream != null) {
            try {
                fwStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fwStream = null;
        }
        File fwFile = new File(Environment.getExternalStorageDirectory() + "/SRC_Portal/" + CollectorActivity.updateFilename);
        fwLen = fwFile.length();
        if (fwLen != 0) {
            try {
                fwStream = new FileInputStream(Environment.getExternalStorageDirectory() + "/SRC_Portal/" + CollectorActivity.updateFilename);
            } catch (FileNotFoundException e) {
                fwStream = null;
            }
        } else {
            fwStream = null;
        }
        super.start();
    }

    @Override
    public void stop() {
        if (fwStream != null) {
            try {
                fwStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fwStream = null;
        }
        super.stop();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.i("WEB", session.getUri());

        FileInputStream fis;

        long fileLen;

            fis = fwStream;
            fileLen = fwLen;

        if (fis != null)
        {
            return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fis, fileLen);
        }
//        if ((session.getUri().equalsIgnoreCase("/v.json"))
//                || (session.getUri().equalsIgnoreCase("/fw.bin"))
//                || (session.getUri().equalsIgnoreCase("/spiffs.bin"))
//                || (session.getUri().equalsIgnoreCase("/ui.tft")))
//        {
//            try {
//                fileToSend = new File(Environment.getExternalStorageDirectory()
//                        + "/ScentControl" + session.getUri());
//                fileLen = fileToSend.length();
//                if (fileLen != 0) {
//                    fis = new FileInputStream(Environment.getExternalStorageDirectory()
//                            + "/ScentControl" + session.getUri());
//                    return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fis, fileLen);
//                }
//            } catch (FileNotFoundException ignore) {}
//        }
        // File not found
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/octet-stream", "Not Found");
//        return newFixedLengthResponse("<html><body><h1>File not found</h1>\n</body></html>\n");
    }

    /**
     * Get IP address from first non-localhost interface
     * @return  address or empty string
     */
    static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                            if (isIPv4)
                                return sAddr;
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}