package tk.giesecke.my_nrf52_tb.esp32_setup;

class XorCoding {
    static byte[] xorCode(String deviceName, byte[] dataBuffer, int count) {
        // Decode/Encode the data
        byte[] codeKey = deviceName.getBytes();
        int keyLen = codeKey.length;
        byte[] resultBuffer = new byte[count];
        int keyIndex = 0;
        for (int index = 0; index < count; index++) {
            resultBuffer[index] = (byte)(dataBuffer[index] ^ codeKey[keyIndex]);
            keyIndex++;
            if (keyIndex >= keyLen) keyIndex = 0;
        }
        return resultBuffer;
    }
}
