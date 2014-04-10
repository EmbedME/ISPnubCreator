package de.fischl.ispnub;

import java.io.*;
import java.util.Arrays;

/**
 * ISPnub script parser
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
public class ISPScript {   

    public static final byte CMD_CONNECT = 0x01;
    public static final byte CMD_DISCONNECT = 0x02;
    public static final byte CMD_SPIWRITE = 0x03;
    public static final byte CMD_SPIVERIFY = 0x04;
    public static final byte CMD_FLASH = 0x05;
    public static final byte CMD_WAIT = 0x06;
    public static final byte CMD_DECCOUNTER = 0x07;
    public static final byte CMD_EEPROM = 0x08;
    public static final byte CMD_END = (byte)0xff;

    public static final int[] SCK_OPTIONS = {2000000, 500000, 125000, 62500, 4000000, 1000000, 250000};
    
    
    public static int getIntVal(String s) {
        int base = 10;
        s = s.trim();

        if ((s.length() > 2) && (s.substring(0, 2).equals("0x"))) {
            base = 16;
            s = s.substring(2);
        }


        return Integer.parseInt(s, base);
    }

    public static byte getNearestSCKOption(int frequency) {

        int usediff = 8000000;
        byte useindex = 3;
        for (byte i = 0; i < SCK_OPTIONS.length; i++) {

            int diff = frequency - SCK_OPTIONS[i];
            if (diff < 0) continue;

            if (diff < usediff) {
                usediff = diff;
                useindex = i;
            }            
        }

        return useindex;
    }
        
    public static int parse(String scriptfilename, byte[] mem, int pos) throws IOException {
        
        BufferedReader br = new BufferedReader(new FileReader(scriptfilename));
        String line;
        int lineno = 0;
        while ((line = br.readLine()) != null) {

            lineno++;

            if (line.split(";").length == 0) continue;
            String linehead = line.split(";")[0].trim();
            if (linehead.equals("")) continue;

            String[] lineparts = linehead.split(" ");
            String cmd = lineparts[0].toUpperCase();
            String[] parameters = null;
            if (lineparts.length > 1) {
                parameters = linehead.substring(cmd.length()).split(",");
            }
            
            switch (cmd) {
                case "CONNECT":
                    mem[pos++] = CMD_CONNECT;
                    
                    mem[pos++] = getNearestSCKOption(getIntVal(parameters[0]));
                    //mem[pos++] = (byte)(getIntVal(parameters[0]));
                    break;
                case "DISCONNECT":
                    mem[pos++] = CMD_DISCONNECT;
                    break;
                case "SPIWRITE":
                    mem[pos++] = CMD_SPIWRITE;
                    mem[pos++] = (byte)(getIntVal(parameters[0]));
                    mem[pos++] = (byte)(getIntVal(parameters[1]));
                    mem[pos++] = (byte)(getIntVal(parameters[2]));
                    mem[pos++] = (byte)(getIntVal(parameters[3]));
                    break;
                case "SPIVERIFY":
                    mem[pos++] = CMD_SPIVERIFY;
                    mem[pos++] = (byte)(getIntVal(parameters[0]));
                    mem[pos++] = (byte)(getIntVal(parameters[1]));
                    mem[pos++] = (byte)(getIntVal(parameters[2]));
                    mem[pos++] = (byte)(getIntVal(parameters[3]));
                    mem[pos++] = (byte)(getIntVal(parameters[4]));
                    break;
                case "FLASH":
                case "EEPROM":
                    String filename = parameters[0].trim();
                    int pagesize = getIntVal(parameters[2]);
                    int startaddress = getIntVal(parameters[1]);
                    int blanksize = 64;
                    if (pagesize * 2 > blanksize) blanksize = pagesize * 2;
                    
                    byte[] hexmem = new byte[Creator.UC_MEMORY_SIZE];
                    Arrays.fill(hexmem, (byte)0xff);

                    int length = HexFile.read(new FileReader(filename), hexmem, 0);
                    
                    BinDataBlock block = new BinDataBlock(hexmem, length, blanksize);
                    
                    while (block.getNextBlock()) {
                        
                        /*
                        System.out.println("Block");
                        System.out.println("  first: " + block.getFirst());
                        System.out.println("  last: " + block.getLast());
                        System.out.println("  length: " + block.getLength());                    
                        */
                    
                        if (cmd.equals("FLASH")) {
                            mem[pos++] = CMD_FLASH;
                        } else {
                            mem[pos++] = CMD_EEPROM;
                        }
                        mem[pos++] = (byte)((startaddress + block.getFirst() >> 24) & 0xff);
                        mem[pos++] = (byte)((startaddress + block.getFirst() >> 16) & 0xff);
                        mem[pos++] = (byte)((startaddress + block.getFirst() >> 8) & 0xff);
                        mem[pos++] = (byte)((startaddress + block.getFirst()) & 0xff);
                        mem[pos++] = (byte)((block.getLength() >> 24) & 0xff);
                        mem[pos++] = (byte)((block.getLength() >> 16) & 0xff);
                        mem[pos++] = (byte)((block.getLength() >> 8) & 0xff);
                        mem[pos++] = (byte)((block.getLength()) & 0xff);
                        mem[pos++] = (byte)((pagesize >> 8) & 0xff);
                        mem[pos++] = (byte)(pagesize & 0xff);

                        System.arraycopy(hexmem, block.getFirst(), mem, pos, block.getLength());
                        
                        pos += block.getLength();
                    }
                    break;
                case "WAIT":
                    mem[pos++] = CMD_WAIT;
                    mem[pos++] = (byte)(getIntVal(parameters[0]));
                    break;
                case "DECCOUNTER":
                    mem[pos++] = CMD_DECCOUNTER;
                    int counter = getIntVal(parameters[0]);
                    mem[pos++] = (byte)((counter >> 8) & 0xff);
                    mem[pos++] = (byte)(counter & 0xff);
                    break;
                case "END":
                    mem[pos++] = CMD_END;
                    break;
                default:
                    System.out.println("Unknown command \"" + cmd + "\" in line " + lineno + ".");
                    break;
            }
        }

        br.close();

        return pos;
        
    }
}
