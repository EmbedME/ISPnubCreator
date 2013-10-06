ISPnubCreator
=============

Creates hex file for ISPnub from given programming instruction script.

Example of script (test.ispnub):
CONNECT 2000000                           ; connect with SCK = 2 MHz 
SPIVERIFY 0x30, 0x00, 0x00, 0x00, 0x1E    ; check signature byte 0x00 (0x1E = manufactured by Atmel)
SPIVERIFY 0x30, 0x00, 0x01, 0x00, 0x93    ; check signature byte 0x01 (0x93 = 8KB Flash memory)
SPIVERIFY 0x30, 0x00, 0x02, 0x00, 0x07    ; check signature byte 0x02 (0x07 = ATmega8 device)
SPIWRITE  0xAC, 0x80, 0x00, 0x00          ; chip erase
WAIT 2                                    ; wait 20 ms
FLASH ../test/main.hex, 0, 64             ; flash given hex file starting at flash address 0 with pagesize 64
DISCONNECT                                ; disconnect SPI
DECCOUNTER 10                             ; allow 10 programming cycles
END

Usage:
java -jar ISPnubCreator.jar test.ispnub ispnub.hex

Now the generated "ispnub.hex" can be flashed with your favorite programmer into the ISPnub module.

