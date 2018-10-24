import java.io.*;
import java.net.*;
import java.util.*;

public class BeatBoxServer{
  ArrayList<ObjectOutputStream> clientOutputStreams = new ArrayList<ObjectOutputStream>();

  public static void main(String[] args){
    BeatBoxServer server = new BeatBoxServer();
    server.go();
  }

  public void go(){
    try{
      ServerSocket serverSock = new ServerSocket(4242);
      while(true){
        Socket clientSocket = serverSock.accept();
        ClientHandler handler = new ClientHandler(clientSocket);
        Thread t = new Thread(handler);
        t.start();
        System.out.println("Open one thread for a client.");
      }
    }catch(Exception ex){
      ex.printStackTrace();
    }
  }

  class ClientHandler implements Runnable{
    Socket clientSock;
    ObjectInputStream in;
    ObjectOutputStream out;

    public ClientHandler(Socket sock){
      clientSock = sock;
      try{
        in = new ObjectInputStream(sock.getInputStream());
        out = new ObjectOutputStream(sock.getOutputStream());
        clientOutputStreams.add(out);
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }

    public void run(){
      Object obj1;
      Object obj2;
      try{
        while((obj1 = in.readObject()) != null){
          obj2 = in.readObject();
          tellEveryone(obj1, obj2);
        }
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }

    private void tellEveryone(Object obj1, Object obj2){
      try{
        for(ObjectOutputStream out : clientOutputStreams){
          out.writeObject(obj1);
          out.writeObject(obj2);
        }
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
  }


}
