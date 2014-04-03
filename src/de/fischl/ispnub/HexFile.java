package de.fischl.ispnub;

import java.io.*;

/**
 * Hex file input and output functions
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
public class HexFile {

    public static final int RECORD_TYPE_DATA = 0x00;
    public static final int RECORD_TYPE_EOF = 0x01;
    public static final int RECORD_TYPE_EXT_SEGMENT_ADDRESS = 0x02;
    public static final int RECORD_TYPE_EXT_LINEAR_ADDRESS = 0x04;
    public static final int RADIX = 16;

    static int read(Reader r, byte[] membuffer, int offset) throws IOException {

        BufferedReader br = new BufferedReader(r);

        String record;
        int lineNum = 0;
        int extendedAddress = 0;
        int segmentAddress = 0;
        int endAddress = 0;

        while ((record = br.readLine()) != null) {

            lineNum++;

            int dataLength = Integer.parseInt(record.substring(1,3), RADIX);
            int address = Integer.parseInt(record.substring(3, 7), RADIX);
            int recordType = Integer.parseInt(record.substring(7,9), RADIX);

            // checksum
            byte sum = 0;
	    for (int i = 0; i < dataLength + 5; i++) {
		sum += (byte) (Integer.parseInt(record.substring(i * 2 + 1, (i * 2 + 3)), RADIX));
	    }
            if (sum != 0) throw new IllegalArgumentException("invalid checksum in line " + lineNum);

            switch (recordType) {
                case RECORD_TYPE_DATA:

                    for (int i = 0; i < dataLength; i++) {
                        byte value = (byte) (Integer.parseInt(record.substring(i * 2 + 9, (i * 2 + 11)), RADIX));
                        membuffer[offset + address + extendedAddress + segmentAddress] = value;
                        if (address + extendedAddress + segmentAddress > endAddress) endAddress = address + extendedAddress + segmentAddress;
                        address++;
                    }

                    break;
                case RECORD_TYPE_EOF:
                    break;
                case RECORD_TYPE_EXT_LINEAR_ADDRESS:

		    extendedAddress = Integer.parseInt(record.substring(9, 13), 16) << 16;
                    break;
                case RECORD_TYPE_EXT_SEGMENT_ADDRESS:
       		    segmentAddress = Integer.parseInt(record.substring(9, 13), 16) << 4;
                    break;
                default:
                    System.err.println("Unknown record type in line " + lineNum);
                    break;
            }

        }

        return endAddress + 1;
    }

    static void write(String filename, byte[] membuffer, int length) throws IOException {
        PrintWriter out = new PrintWriter(filename);

        int upperAddress = 0;
        int bytesleft = length;
        int position = 0;
        while (bytesleft > 0) {
            int len = bytesleft;
            if (len > 16) len = 16;

            if (upperAddress != (position >> 16)) {
                // printout upper address
                upperAddress = position >> 16;
                byte cs = (byte)(0xff - ((1 + 4 + (upperAddress >> 8) + (upperAddress & 0xff)) & 0xff));
                out.println(":02000004" + String.format("%04x", upperAddress) + String.format("%02x", cs));
            }

            boolean ignore = true;
            String outstring = ":";
            outstring += String.format("%02x", len).toUpperCase();
            outstring += String.format("%04x", position & 0xffff).toUpperCase() + "00";
            byte checksum = (byte)(len + ((position >> 8) & 0xff) + (position & 0xff));
            for (int i = 0; i < len; i++) {
                if (membuffer[position] != (byte)0xff) ignore = false;
                outstring += String.format("%02x", membuffer[position]).toUpperCase();
                checksum += membuffer[position];
                position++;
            }
            checksum = (byte) (-checksum);
            outstring += String.format("%02x", checksum).toUpperCase();
            if (!ignore) {
                out.println(outstring);
            }

            bytesleft = bytesleft - len;

        }
        out.println(":00000001FF");
        out.close();
    }
}
