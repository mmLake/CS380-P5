

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;

public class UdpClient {

    private final static int totalPackets = 12;
    private final static int headerSize = 20;
    private final static String ipAddress = "18.221.102.182";
    private final static int port = 38005;
    private final static byte[] srcAddress = {127,0,0,1};

    public static void main(String[] args){
        UdpClient client = new UdpClient();
        client.main();
    }
    
    private byte[] getUDPPacket(byte[] destAddress, int portNum, int dataSize){
    	Random random = new Random();
    	int pseudoSize = 8;
    	byte[] pseudoPacket = new byte[headerSize + dataSize];
    	
    	pseudoPacket[4] = destAddress[0]; //destination Address
        pseudoPacket[5] = destAddress[1];
        pseudoPacket[6] = destAddress[2];
        pseudoPacket[7] = destAddress[3];
        pseudoPacket[9] = 0x11; //protocol
        pseudoPacket[10] = (byte)((pseudoSize + dataSize>>8) & 0xFF); //set len
        pseudoPacket[11] = (byte)(pseudoSize + dataSize & 0xFF);
        pseudoPacket[14] = (byte)((portNum>>8) & 0xFF); //set port num
        pseudoPacket[15] = (byte)(portNum & 0xFF);
        pseudoPacket[16] = pseudoPacket[10];
        pseudoPacket[17] = pseudoPacket[11];
        
        for (int i = 20; i < dataSize; i++){
        	pseudoPacket[i] = (byte)(random.nextInt());
        }
        
        short pseudoChecksum = checksum(pseudoPacket);
        
        byte[] udpPacket = new byte[pseudoSize + dataSize];
        udpPacket[2] = pseudoPacket[14]; //set port num
        udpPacket[3] = pseudoPacket[15];
        udpPacket[4] = (byte)((udpPacket.length>>8) & 0xFF); //set len
        udpPacket[5] = (byte)(udpPacket.length & 0xFF);
        udpPacket[6] = (byte)((pseudoChecksum>>8) & 0xFF); //set checksum
        udpPacket[7] = (byte)(pseudoChecksum & 0xFF);
        
        for (int i = pseudoSize; i<udpPacket.length; i++){
        	udpPacket[i] = pseudoPacket[i + 12];
        }
        
        return udpPacket;
    } 
    
    private byte[] getHandshake(byte[] destAddress){
    	int hdataSize = 4;
        int packetSize = headerSize + hdataSize;
        byte[] hardcodedData = {(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};
        
        byte[] hPacket = new byte[packetSize];

        hPacket[0] = 0x45; //version and hlen
        hPacket[1] = 0; //tos
        hPacket[2] = (byte)((packetSize>>8) & 0xFF); //size
        hPacket[3] = (byte)(packetSize & 0xFF);
        hPacket[4] = 0; //ident
        hPacket[5] = 0;
        hPacket[6] = 0x40; //flags
        hPacket[7] = 0; //offset
        hPacket[8] = 50; //ttl
        hPacket[9] = 17; //protocol
        hPacket[12] = srcAddress[0]; //source Address
        hPacket[13] = srcAddress[1];
        hPacket[14] = srcAddress[2];
        hPacket[15] = srcAddress[3];
        hPacket[16] = destAddress[0]; //destination Address
        hPacket[17] = destAddress[1];
        hPacket[18] = destAddress[2];
        hPacket[19] = destAddress[3];

        //calculate and replace handshake checksum values
        short hChecksum = checksum(hPacket);

        hPacket[10] = (byte) (hChecksum >>> 8);
        hPacket[11] = (byte) hChecksum;

        //enter hardcoded data
        for (int i=0; i<hPacket.length - headerSize;i++){
            hPacket[i+headerSize] = hardcodedData[i];
        }
        
        return hPacket;
    }

    public void main(){
        try (Socket socket = new Socket(ipAddress, port)) {

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            
            byte[] destAddress = socket.getInetAddress().getAddress();

            //send initial handshake packet
            byte[] hPacket = getHandshake(destAddress);
            os.write(hPacket);

            //print whether or not handshake is correct
            System.out.print("Handshake: ");;
            for(int j=0;j<4;j++){
            	byte value = (byte)is.read();
            	System.out.print(String.format("%02X", value));
            }
            System.out.println();
            
            //get port number
            byte[] portNumberArray = new byte[2];
            for(int k=0;k<2;k++){
            	portNumberArray[k] = (byte)is.read();
            }
            int portNumber = ((portNumberArray[0] <<8 & 0xFF00) | portNumberArray[1] & 0xFF);
         
            //send UDP packets
            for (int l=0; l< totalPackets; l++) {
            	int dataSize = (int)java.lang.Math.pow(2,(l+1));
            	byte[] udpPacket = getUDPPacket(destAddress, portNumber, dataSize);
            	
            	byte[] ipPacket = new byte[udpPacket.length + headerSize];

                ipPacket[0] = 0x45; //version and hlen
                ipPacket[1] = 0; //tos
                ipPacket[2] = (byte)((ipPacket.length>>8) & 0xFF); //size
                ipPacket[3] = (byte)(ipPacket.length & 0xFF);
                ipPacket[4] = 0; //ident
                ipPacket[5] = 0;
                ipPacket[6] = 0x40; //flags
                ipPacket[7] = 0; //offset
                ipPacket[8] = 50; //ttl
                ipPacket[9] = 17; //protocol
                ipPacket[12] = 0; //source Address
                ipPacket[13] = 0;
                ipPacket[14] = 0;
                ipPacket[15] = 0;
                ipPacket[16] = destAddress[0]; //destination Address
                ipPacket[17] = destAddress[1];
                ipPacket[18] = destAddress[2];
                ipPacket[19] = destAddress[3];

                //calculate and replace checksum values
                short ipChecksum = checksum(ipPacket);

                ipPacket[10] = (byte) (ipChecksum >>> 8);
                ipPacket[11] = (byte) ipChecksum;

                //enter data
                for (int i=0; i<udpPacket.length;i++){
                    ipPacket[i+headerSize] = udpPacket[i];
                }
            	
            	//send packet
                long tInitial = System.currentTimeMillis();
            	os.write(ipPacket);
            	
            	//print whether or not packet is correct
                for(int j=0;j<4;j++){
                	byte value = (byte)is.read();
                	System.out.print(String.format("%02X", value));
                }
                
                //print RTT
                System.out.println(" RTT: " + ((long)System.currentTimeMillis() - tInitial));
            }
        }catch(IOException e){
            System.out.println(e);
        }catch (Error e){
            System.out.println(e);
        }
    }
    
    
    public short checksum(byte[] b){
        int sum = 0;
        for (int i = 0; i < b.length; i+=2){
            //get first 2 halfs of values
            int firstHalf = b[i] << 8;
            firstHalf &= 0xFF00;
            int secondHalf = b[i+1] & 0xFF;

            sum += firstHalf + secondHalf;

            if ((sum & 0xFFFF0000) != 0){
                sum &= 0xFFFF;
                sum++;
            }
        }
        return (short)(~(sum & 0xFFFF));
    }
}
