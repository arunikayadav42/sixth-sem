import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class Server 
{
	static Vector<ClientHandler> ar = new Vector<>();
	static int i = 0;
	public static void main(String[] args)throws IOException
	{
		//Server is listening on port 5056
		ServerSocket ss = new ServerSocket(1234);
		Socket s;
		while(true)
		{
			s = ss.accept();
			System.out.println("New client request received: "+s);
			DataInputStream dis = new DataInputStream(s.getInputStream());
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());

			System.out.println("creating a new handler for this client");
			ClientHandler mtch = new ClientHandler(s, "client "+i, dis, dos);

			Thread t = new Thread(mtch);
			System.out.println("Adding this client to the list of active users");

			ar.add(mtch);
			t.start();

			i++;
	    }
	
}
}

class ClientHandler implements Runnable
{
	Scanner sc = new Scanner(System.in);
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;

    public ClientHandler(Socket s, String name, DataInputStream dis,DataOutputStream dos)
    {
    	this.dos = dos;
    	this.dis = dis;
    	this.name = name;
    	this.s = s;
    	this.isloggedin = true;
    }

    @Override
    public void run()
    {
    	String received;

    	while(true)
    	{
    		try
    	{
    		received = dis.readUTF();
    		System.out.println(received);
    		if(received.equals("logout"))
    		{
    			this.isloggedin = false;
    			this.s.close();
    			System.out.println("CLosing the connection");
    			break;
    		}
    		// StringTokenizer st = new StringTokenizer(received, "#");
    		// String msgToSend = st.nextToken();
    		// String recipient = st.nextToken();

    		// for(ClientHandler mc: Server.ar)
    		// {
    		// 	if(mc.name.equals(recipient) && mc.isloggedin==true)
    		// 	{
    		// 		mc.dos.writeUTF(this.name+" : "+msgToSend);
    		// 		break;
    		// 	}
    		// }
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    	}
    	try
    {
       this.dis.close();
       this.dos.close();
    }
    catch(IOException e){
    	e.printStackTrace();
    }
    }
    
}