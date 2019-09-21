import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Scanner;
import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.net.InetAddress;


class HandleClientRequest implements Runnable
{
  public HandleClientRequest()
  {

  }

  public void run()
  {
    ServerSocket server = null;
    int port = Node.nodePort;

    try
    {
      server = new ServerSocket(port);
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }

    while(true)
    {
      Socket client = null;
      InputStream in = null;
      OutputStream out = null;
      try
      {
        client = server.accept();
        System.out.println("*********Someone is requesting something*********");
        out = client.getOutputStream();
        in = client.getInputStream();

        byte[] buffer = new byte[1024];
        in.read(buffer);
        String request = new String(buffer);
        request = request.trim();

        String data[] = request.split(":");

        System.out.println("*********Requested message is "+request+"*********");

        if(data[0].equals("find_successor"))
        {
          int node_id_successor_to_be_found = Integer.parseInt(data[1]);
          int requesting_port = Integer.parseInt(data[2]);
          int flag = Integer.parseInt(data[3]);
          System.out.println("*********Finding the successor of the node id "+node_id_successor_to_be_found+"***************");

          String successor = Node.find_successor(node_id_successor_to_be_found, requesting_port, flag);
          System.out.println("*********Sending Response*********");
          out.write(successor.getBytes());
          out.close();
        }
        else if(data[0].equals("closest_preceding_finger"))
        {
            int id = Integer.parseInt(data[1]);
            int cPre = Finger.closest_preceding_finger(id);
            String cPreced = ""+cPre;
            System.out.println("*********Sending Response*********");
            out.write(cPreced.getBytes());
            out.close();
        }
        else if(data[0].equals("successor"))
        {
            int suc = Node.successorPort;
            int old = Node.successorID;
            int flag = Integer.parseInt(data[3]);
            int id = Integer.parseInt(data[2]);
            int p1 = Integer.parseInt(data[1]);
            int n = chord.SHA1("localhost:"+p1);

            boolean flag1 = false;
            if((id)%32<=(old-1+32)%32)
              flag1 = Node.isInterval((id)%32,(old-1+32)%32,n);
            else
            {
              flag1 = Node.isInterval((id)%32,31,n);
              if(flag1 == false)
                flag1 = Node.isInterval(0,(old-1+32)%32,n);
            }
            System.out.println("flag1 = "+flag1+" "+id+" "+ n+" "+old);
            if(flag1)
              suc = p1;

            if(id == Node.successorID)
              suc = Node.successorPort;

            if(flag == 0)
            {
              suc = Node.successorPort;
            }
            System.out.println("*********Sending Response*********");
            String p = ""+suc;
            out.write(p.getBytes());
            out.close();
        }
        else if(data[0].equals("predecessor"))
        {
           int pre = Node.predecessorPort;
          String p = ""+pre;
          System.out.println("*********Sending Response*********");
          int updPre = Integer.parseInt(data[1]);
          Node.predecessorPort = updPre;
          Node.predecessorID = chord.SHA1("localhost:"+updPre);
          if(data.length == 3 && Integer.parseInt(data[2]) != Node.nodePort)
            Request.makeRequest(Integer.parseInt(data[2]),"transfer_files:"+0+":"+32);
          out.write(p.getBytes());
          out.close();
        }

        else if(data[0].equals("update"))
        {
            int s = Integer.parseInt(data[1]);
            int i = Integer.parseInt(data[2]);
            int z = Finger.table[i][3];

            boolean flag1 = false;
            int k = 0;
            if((Node.nodeID+1)%32 <= (z-1+32)%32)
              flag1 = Node.isInterval((Node.nodeID+1)%32, (z-1+32)%32, s);

            else
            {
              flag1 = Node.isInterval((Node.nodeID+1)%32,31,s);
              if(flag1 == false)
                flag1 = Node.isInterval(0,(z-1+32)%32,s);
            }

            if(z == (Node.nodeID+1)%32)
              flag1 = false;

            if(flag1 && s != Node.nodeID && data.length == 4)
            {
              k = 1;
              if(data.length == 4)
              {
                Finger.table[i][3] = s;
                Finger.table[i][4] = Integer.parseInt(data[3]);
              }
              if(i == 0)
              {
                Node.successorID = s;
                Node.successorPort = Integer.parseInt(data[3]);
              }
              int prePort = Node.predecessorPort;
              if(prePort != Node.nodePort)
                Request.makeRequest(prePort, "update:"+s+":"+i+":"+data[3]);
            }

            if(s == z && data.length == 5)
            {
                Finger.table[i][3]=chord.SHA1("localhost:"+data[4]);
                Finger.table[i][4] = Integer.parseInt(data[4]);
                if(i==0)
                {
                  Node.successorID =chord.SHA1("localhost:"+data[4]);
                  Node.successorPort = Integer.parseInt(data[4]);
                }
                int prePort = Node.predecessorPort;
                if(prePort != Node.nodePort && prePort!=Integer.parseInt(data[3]))
                  Request.makeRequest(prePort,"update:"+s+":"+i+":"+data[3]+":"+data[4]);
            }
            System.out.println("*********Sending Response*********");
            out.write(" ".getBytes());
            out.close();
        }
        else if(data[0].equals("transfer_files"))
        {
            int start = Integer.parseInt(data[1]);
            int end = Integer.parseInt(data[2]);

            File folder = new File("./nodeFile");
            File[] allFiles = folder.listFiles();
            ArrayList<File> transferFiles = new ArrayList<>();
            int count = 0;
            for(File file: allFiles)
            {
              String filename = file.getName();
              int key = chord.SHA1(filename);
              boolean flag = false;
              if(start<=end)
                flag = Node.isInterval(start,end,key);
              else
              {
                  flag = Node.isInterval(start,31,key);
                  if(flag == false)
                    flag = Node.isInterval(0,end,key);
              }
              if(flag)
              {
                transferFiles.add(file);
                count++;
              }
            }
            System.out.println("Total file count : "+count);
            String s = "";
            for(int i = 0; i < count; i++)
            {
              File f = transferFiles.get(i);
              System.out.println("Sent file: "+f.getName());
              if(i < count-1)
                s = s+f.getName()+":";
              else
                s = s+f.getName();
              f.delete();
            }
            System.out.println("*********Sending Response*********");
            out.write(s.getBytes());
            out.close();
        }
      }
      catch(IOException e)
      {
          e.printStackTrace();
          return;
      }
      chord.display();
    }
  }
}

class Finger
{
  static int table[][];

  public Finger()
  {
     table = new int[5][5];

     for(int i = 0; i < 5; i++)
     {
      //start column of the finger table
      table[i][0] = (Node.nodeID + (int)Math.pow(2, i));
      //start of the interval of the row of the i+1 th row of the finger table
      table[i][1] = table[i][0];
      //end of the interval of the row of the i+1 th row of the finger table
      table[i][2] = (table[i][0] + (int)Math.pow(2, i));
      //the id of the successor node of the i+1 th row of the finger table
      table[i][3] = Node.nodeID;
      //the corresponding port of the above id of the node successor of the i+1 th row of the finger table
      table[i][4] = Node.nodePort;
     }
     System.out.println("*********Finger table initiated for this node*********");
  }

  public Finger(int known_port)
  {
     table = new int[5][5];
     table[0][0] = (Node.nodeID + (int)Math.pow(2, 0))%32;
     table[0][1] = table[0][0];
     table[0][2] = (table[0][0] + (int)Math.pow(2, 0)%32);
     String predSuccessor = Request.makeRequest(known_port, "find_successor:"+table[0][0]+":"+Node.nodePort+":"+1);
     String data[] = predSuccessor.split(":");

     int suc = Integer.parseInt(data[0]);
     int pred = Integer.parseInt(data[1]);

     table[0][4] = suc;
     String k = "localhost:"+suc;
     int sucID = chord.SHA1(k);

     Node.successorPort = table[0][4];
     Node.successorID = sucID;
     Node.predecessorID = chord.SHA1("localhost:"+pred);
     Node.predecessorPort = pred;

     table[0][3] = sucID;

     System.out.println("*********Found its Successor using Known node i.e "+chord.SHA1("localhost:"+known_port)+"*********");
     System.out.println("*********It's Successor is "+sucID+"*********");

     int sucPre = Integer.parseInt(Request.makeRequest(suc, "predecessor:"+Node.nodePort));
     
     for(int i = 1; i < 5; i++)
     {
       table[i][0] = (Node.nodeID + (int)Math.pow(2, i))%32;
       table[i][1] = table[i][0];
       table[i][2] = (table[i][1] + (int)Math.pow(2, i))%32;

       int x = table[i-1][3];
       int y = table[i][0];

       boolean flag = false;

       if((Node.nodeID)%32 <= (x-1+32)%32)
        flag = Node.isInterval((Node.nodeID)%32, (x-1+32)%32, y);
      else
      {
        flag = Node.isInterval((Node.nodeID)%32, 31, y);
        if(flag == false)
        flag = Node.isInterval(0, (x-1+32)%32, y);
      }
      if(flag)
      {
        table[i][3] = table[i-1][3];
        table[i][4] = table[i-1][4];
      }
      else
      {
        String val = Request.makeRequest(known_port, "find_successor:"+table[i][0]+":"+Node.nodePort+":1");
        String data1[] = val.split(":");
        table[i][3] = chord.SHA1("localhost:"+data1[0]);
        table[i][4] = Integer.parseInt(data1[0]); 
      }
     }
     System.out.println("*********Finger table intialized for this node*********");
   }

  public static int closest_preceding_finger(int id)
  {
    System.out.println("*********In closest preceding finger, finding that for id: "+id);
    for(int i = 4; i>= 0; i--)
    {
      //get the successor, i.e, the finger table entry
      int x = table[i][3];
      boolean flag = false;
      if((Node.nodeID+1)%32 <= (id-1+32)%32)
      {
        flag = Node.isInterval((Node.nodeID)%32, (id-1+32)%32, x);
      }
      else
      {
        flag = Node.isInterval((Node.nodeID)%32, 31, x);
        if(flag == false)
          flag = Node.isInterval(0, (id-1+32)%32, x);
      }
      if(flag)
        return table[i][4];
    }

    return Node.nodePort;
  }
  
}

//class to serve as a client
class Request
{
  public static String makeRequest(int port, String msg)
  {
    Socket clientSocket = null;
    String ip = "localhost";

    InputStream in = null;
    OutputStream out = null;

    try
    {
           clientSocket = new Socket(ip, port);
           out = clientSocket.getOutputStream();
           in = clientSocket.getInputStream();

           String ask = "";
           ask = msg;

           out.write(ask.getBytes());
           byte[] buf = new byte[1024];
           in.read(buf);

           String p = new String(buf);

           p = p.trim();
           System.out.println("*********Message request "+ask+" to node "+chord.SHA1("localhost:"+port)+"*********");
           System.out.println("*********Response is "+p+"*********");

           if(msg.contains("transfer_files"))
           {
             String name[] = p.split(":");
             System.out.println("*********Receiving files*********");
             System.out.println("*********File count: "+name.length+"*********");
             for(int i = 0; i < name.length; i++)
             {
              System.out.println("File received: "+name[i]);
              File f = new File("./nodeFile/"+name[i]);
              f.createNewFile();
             }
           }
           return p;
    }

    catch(Exception e)
    {
      e.printStackTrace();
    }
    return "";
  }
}

class Node
{
  static int predecessorID;
  static int successorID;
  static int predecessorPort;
  static int successorPort;
  static int nodeID;
  static int nodePort;
  static HashMap<Integer, Integer> hmap = new HashMap<Integer, Integer>();
    
    public Node(int id, int port)
    {
      nodeID = id;
      predecessorID = id;
      successorID = id;
      nodePort = port;
      successorPort = port;
      predecessorPort = port;
    }


    static boolean isInterval(int start, int end, int key)
    {
      if(key >= start && key <= end)
        return true;

      return false;
    }

    public static int find_predecessor(int id)
    {
       int currentPredecessor = Node.nodeID;
       int currentPredecessorPort = Node.nodePort;

       int successor = Node.successorID;

       boolean flag = false;

       if((currentPredecessor+1)%32 < successor)
       {
          flag = isInterval((currentPredecessor+1)%32, successor, id);
       }
       else
       {
         flag = isInterval((currentPredecessor+1)%32, 31, id);
         if(flag == false)
         {
          flag = isInterval(0, successor, id);
         }
       }
       while(!(flag))
       {
          if(currentPredecessor == Node.nodeID)
          {
             currentPredecessorPort = Finger.closest_preceding_finger(id);
             String k = "localhost:"+currentPredecessorPort;
             currentPredecessor = chord.SHA1(k);

             successor = (currentPredecessor == Node.nodeID)?Node.successorID:Integer.parseInt(Request.makeRequest(currentPredecessorPort,"successor:1222"+":"+id%32+":0"));
             
             if((currentPredecessor+1)%32<=successor)
                flag = Node.isInterval((currentPredecessor+1)%32,successor,id);
             else
              {
                flag = Node.isInterval((currentPredecessor+1)%32,31,id);
                if(flag == false)
                  flag = Node.isInterval(0,successor,id);
              }

          }
          else
          {
              currentPredecessorPort = Integer.parseInt(Request.makeRequest(currentPredecessorPort,"closest_preceeding:"+(id)));
              String k = "localhost:"+currentPredecessorPort;
              currentPredecessor = chord.SHA1(k);
              successor = (currentPredecessor==Node.nodeID)?Node.successorID:Integer.parseInt(Request.makeRequest(currentPredecessorPort,"successor:1222"+":"+id%32+":0"));
                    if(successor>32)
                      successor = chord.SHA1("localhost:"+successor);
                    if((currentPredecessor+1)%32<=successor)
                flag = Node.isInterval((currentPredecessor+1)%32,successor,id);
              else
              {
                flag = Node.isInterval((currentPredecessor+1)%32,31,id);
                if(flag == false)
                flag = Node.isInterval(0,successor,id);
              }
          }
       }
       System.out.println("find_predecessor(): *********Predecessor of node :"+id+" is "+chord.SHA1("localhost:"+currentPredecessorPort)+"*********");
       return currentPredecessorPort;
    }

    public static String find_successor(int id, int requesting_port, int flag)
    {
        int pred = find_predecessor(id);
        System.out.println("find_successor(): *********Predecessor of node :"+id+" is "+chord.SHA1("localhost:"+pred)+"*********");

        if(pred == Node.nodePort)
        {
          int oldSuccessorPort = Node.successorPort;
          int oldSuccessor = Node.successorID;

          int n = chord.SHA1("localhost:"+requesting_port);
          boolean flag1 = false;

          if((id)%32 <= (oldSuccessor-1)%32)
            flag1 = Node.isInterval((id)%32, (oldSuccessor-1)%32, n);
          else
          {
            flag1 = Node.isInterval((id)%32, 31, n);
            if(flag1 == false)
              flag1 = Node.isInterval(0, (oldSuccessor-1)%32, n);
          }
          if(flag1)
          {
            oldSuccessor = n;
            oldSuccessorPort = requesting_port;
          }
          if(id == Node.successorID)
          {
            oldSuccessor = Node.successorID;
            oldSuccessorPort = Node.successorPort;
          }

          System.out.println("*********Successor of its predecessor , i.e, "+Node.nodeID+" is"+oldSuccessor);
          return oldSuccessorPort+":"+pred;
        }
        int predSuc = Integer.parseInt(Request.makeRequest(pred,"successor:"+requesting_port+":"+id+":"+flag));
        System.out.println("*********Successor Of it's Predecessor i.e"+chord.SHA1("localhost:"+pred)+" is "+predSuc+"*********");
        return predSuc+":"+pred ;
    }
}

public class chord
{
  public static int SHA1(String text)
  {
     MessageDigest md;

     try
     {
      String sha1 = "";
      MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
      msdDigest.update(text.getBytes("UTF-8"), 0, text.length());
      sha1 = DatatypeConverter.printHexBinary(msdDigest.digest());
      int key = Integer.parseInt(new BigInteger(sha1, 16).toString(2).substring(95, 100), 2);
      return key;
     }

     catch(Exception e)
     {
       e.printStackTrace();
     }
     return 0;
  }

  public static void display()
  {
    System.out.println("Enter the choice");
    System.out.println("1. Self IP Address and ID");
    System.out.println("2. The IP Address and ID of the successor and the predecessor");
    System.out.println("3. The file key IDs it contains");
    System.out.println("4. Self finger table");
    System.out.println("5. Want to leave the network");
  }
  public static void main(String[] args)throws UnknownHostException
  {
    Finger fingertable;
    Scanner in = new Scanner(System.in);

    int port = 6500;
    String inetAddress = "localhost:"+port;
        
        //call sha1 for id of node
        int id = SHA1(inetAddress);
        System.out.println("Node joined in network with id "+id);
        Node host = new Node(id, port);
        
        new Thread(new HandleClientRequest()).start();
        
        System.out.println("Enter port number if you know a node in the network else enter -1");
        int input = in.nextInt();

        if(input == -1)
          fingertable = new Finger();
        else
        {
          //initialize finger table
          System.out.println("Node :"+id+" knows the node with port : "+input);
          fingertable = new Finger(input);

          //update other nodes
          for(int i = 0; i < 5; i++)
          {
            int x = Node.nodeID - (int)Math.pow(2, i)+1;

            if(x < 0)
              x += 32;

            int p = Node.find_predecessor(x);

            if(p == Node.nodePort)
            {
              break;
            }
            Request.makeRequest(p, "update:"+Node.nodeID+":"+i+":"+Node.nodePort);
          }
          //Transfer File
          int start = Node.predecessorID+1;
          int end = Node.nodeID;
          File dir = new File("./nodeFile");
          if(!dir.exists())
            dir.mkdir();
          else
          {
            String[] entries = dir.list();
            for(String s: entries)
              new File(dir,s).delete();
          }
          Request.makeRequest(Node.successorPort,"transfer_files:"+start+":"+end);
          
        }

        while(true)
        {
            display();
            int choice = in.nextInt();

            if(choice == 1)
            {
                //InetAddress address = InetAddress.getByName("localhost");   
                System.out.println("Host Ip is: " +InetAddress.getLocalHost().getHostAddress()+":"+Node.nodePort+" and SHA1 id is "+Node.nodeID);
            }
            else if(choice == 2)
            {
                System.out.println("PREDECESSOR--Ip is " +InetAddress.getLocalHost().getHostAddress()+":"+Node.predecessorPort+" SHA1 Id is "+Node.predecessorID);
                System.out.println("SUCCESSOR--Ip is "+ InetAddress.getLocalHost().getHostAddress()+":"+Node.successorPort+" SHA1 Id is "+Node.successorID);
            }
            else if(choice == 3)
            {
                File folder = new File("./nodeFile");
                File[] allFiles = folder.listFiles();

                int count = 0;
                System.out.println("File Id: Key");
                for(File file: allFiles)
                {
                  String filename = file.getName();
                  int key = chord.SHA1(filename);
                  System.out.println(filename+" "+key);
                }
                System.out.println("DONE____________");
            }
            else if(choice == 4)
            {
                System.out.println("Finger table of node "+Node.nodeID);
                System.out.println("start      startInterval     endInterval     successor");
                for(int i=0;i<5;i++)
                 {
                   System.out.println(Finger.table[i][0]+"   "+Finger.table[i][1]+"   "+Finger.table[i][2]+"   "+Finger.table[i][3]);
                 }
            }
            else if(choice == 5)
            {
                System.out.println("Node "+Node.nodeID+" about to leave");
                int successorPort = Node.successorPort;

                for(int i = 0; i < 5; i++)
                {
                  int x = Node.nodeID - (int)Math.pow(2, i)+1;

                  if(x < 0)
                    x += 32;

                  int p = Node.find_predecessor(x);

                  if(p == Node.nodePort)
                    break;
                  Request.makeRequest(p, "update:"+Node.nodeID+":"+i+":"+Node.nodePort+":"+successorPort);
                }
                Request.makeRequest(successorPort,"predecessor:"+Node.predecessorPort+":"+Node.nodePort);
                System.out.println("Well wishes to fellow node from node "+Node.nodeID);
                System.exit(0);
            }
        }
  }
}