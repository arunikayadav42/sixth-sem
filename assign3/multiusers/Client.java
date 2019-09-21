import java.net.*; 
import java.io.*; 
import java.util.Scanner;
  
public class Client 
{ 
    final static int ServerPort = 1234;
    // initialize socket and input output streams 
    public static void main(String args[])throws UnknownHostException, IOException
    {
        Scanner scn = new Scanner(System.in);
        InetAddress ip = InetAddress.getByName("localhost");

        Socket s = new Socket(ip, ServerPort);

        BufferedReader dis = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
       Thread sendMessage = new Thread(new Runnable()
       {
        @Override
        public void run(){
            while(true)
            {
                String msg = scn.nextLine();
                try{
                    dos.writeUTF(msg);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
       });
      sendMessage.start();
    }
} 