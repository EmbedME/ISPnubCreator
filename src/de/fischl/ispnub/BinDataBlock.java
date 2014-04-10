package de.fischl.ispnub;

/**
 * Binary data block functions. Filter out blocks with 0xff from binary memory
 * image.
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
public class BinDataBlock {
    
    protected int first;
    protected int last;
    protected int adr;
    protected int length;
    protected byte[] buffer;
    protected int bound;
    
    public BinDataBlock(byte[] buffer, int length, int blanksize) {
        this.adr = 0;
        this.length = length;
        this.buffer = buffer;
        this.bound = blanksize;
    }
    
    public int getFirst() { return first; }
    public int getLast() { return last; }
    public int getLength() { return last - first + 1; }
    
    public boolean getNextBlock() {
        
        first = -1;
        last = -1;
        
        while (adr < length) {            
            
            if (buffer[adr] != (byte)0xff) {

                if (first < 0) first = adr;
                last = adr;
                
            } else {
                
                if ((last > 0) && ((adr - last) > bound)) {
                    return true;
                }
                                
            }
            
            adr++;  
        }
        
        return (last > 0) || (adr < length);
    }

}
