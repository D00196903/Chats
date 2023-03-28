
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author S05ad1
 */
public class Client {
    // notification

    private String notif = " *** ";
    // to read from the socket
    private ObjectInputStream sInput;
    // to write on the socket
    private ObjectOutputStream sOutput;
    private Socket socket;

    private String server, username;
    private int port;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    //start chat
    public boolean start() {
        //trying to connect to the server
        try {
            socket = new Socket(server, port);
        } // exception handler if it failed
        catch (Exception ec) {
            display("there isan error connecting to the server:" + ec);
            return false;
        }

        String msg = "the connection has been accepted" + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // this creates the Thread to listen from the server 
        new ListenFromServer().start();
        // this will send the username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage 
        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the client it has worked
        return true;
    }
// this sends a message to the console

    private void display(String msg) {

        System.out.println(msg);

    }
    // this sends a message to the server

    void sendMessage(ChatMessage msg) throws IOException {
        sOutput.writeObject(msg);
    }

    // when something goes wrong it will close the input adn output streams and it will then disconnect
    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (Exception e) {
        }
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (Exception e) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
        }

    }

    //If the portNumber is not specified 600 is used
    //If the serverAddress is not specified "localHost" is used
    // If the username is not specified "Anonymous" is used
    public static void main(String[] args) throws IOException {
        // default values if not entered
        int portNumber = 600;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the username: ");
        userName = scan.nextLine();

        // different case according to the length 
        switch (args.length) {
            case 3:
                // for Client username portNumber server address
                serverAddress = args[2];
            case 2:
                // for  Client username portnumber
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Use: Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 1:
                //  username
                userName = args[0];
            case 0:
                //  Client
                break;
            // if number of arguments are invalid
            default:
                System.out.println("Use: [username] [portNumber] [serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // trying to make a connection to the server and return if not connected
        if (!client.start()) {
            return;
        }
        
        System.out.println("***************************************************************************************");
        System.out.println(" welcome to the gamestop shop ");
        System.out.println("choose what you want to do");
        System.out.println("1. type the message to send a message to all active clients");
        System.out.println("2. Type '@username<space>yourmessage' to send a message to start a 1 on 1 chat");
        System.out.println("3. Type 'WHOISIN' see who is connected");
        System.out.println("4. Type 'LOGOUT' logout");
        System.out.println("***************************************************************************************");
        // infinite loop to get the input from the user
        while (true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            } // message to check who are connected
            else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            } // messages for everyone
            else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // close resource
        scan.close();
        // client completed its job. disconnect client.
        client.disconnect();
    }

    /*
	 * a class that waits for the message from the server
     */
    class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    // read the message form the input datastream
                    String msg = (String) sInput.readObject();
                    // print the message
                    System.out.println(msg);
                    System.out.print("> ");
                } catch (IOException e) {
                    display(notif + "Server has closed the connection: " + e + notif);
                    break;
                } catch (ClassNotFoundException e2) {
                }
            }
        }
    }
}
