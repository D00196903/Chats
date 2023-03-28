
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author S05ad1
 * 
 */
public class Server {

    //this will keep track of id for every connection that is being made
    private static int uniqueId;
    // this will keep a list of all the connected clients
    private ArrayList<ClientThread> a;
    // this is being used to format the time of the messages when they are being sent
    private SimpleDateFormat sdf;
    // this is the port number that the server will listen to for in comming connections
    private int port;
    // this will check if the server is still running 
    private boolean keepGoing;
    //this notification will be used to let clients know when someone connects or disconnects
    private String notif = " *** ";

    ///this constructor for the class will take in the port number and initialize the simpledate format and the variables
    public Server(int port) {
        // this is the port 
        this.port = port;
        // this will display the time in hours, minutes and seconds 
        sdf = new SimpleDateFormat("HH:mm:ss"); // working
        // this will keep a list of all the clients 
        a = new ArrayList<ClientThread>();

    }

    // when the server is started it will create a socket and will then listen for incoming connection requests on a specified port. 
    // It then runs an infinite loop that will wait for any client connections. 
    //When a new client connects, it then  creates a new ClientThread object which handles
    //the communication with the client. Each client thread is started and added to an ArrayList of connected clients.
    // the loop will run until keepgoing variable is set to false which will then indicate that  the server should stop
    //start() method.  creates a dummy socket connection to the localhost on the specified port, 
    //which then unblocks the serverSocket.accept()
    public void start() {
        keepGoing = true;
        //create socket server and wait for connection requests 
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections ( till server is active )
            while (keepGoing) {
                display("Server waiting for Clients on port " + port + ".");

                // accept connection if requested from client
                Socket socket = serverSocket.accept();
                // break if server stoped
                if (!keepGoing) {
                    break;
                }
                // if client is connected, create its thread
                ClientThread t = new ClientThread(socket);
                //add this client to arraylist
                a.add(t);

                t.start();
            }
            // try to stop the server
            try {
                serverSocket.close();
                for (int i = 0; i < a.size(); ++i) {
                    ClientThread tc = a.get(i);
                    try {
                        // close all data streams and socket
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (Exception e) {
                display("closing the server: " + e);
            }
        } catch (IOException e) {
            String msg = sdf.format(new Date()) + " The exception is now on a new server: " + e + "\n";
            display(msg);
        }
    }

//this methos is used to stop the serverand it sets the keepgoing to fals which causes the server to exit the loop
    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
        }
    }

    // this  method is a helper method that is used to print messages to the console. 
    //It takes a string argument and the timestamp before printing it to the console.
    private void display(String msg) {
        String time = sdf.format(new Date()) + "" + msg;
        System.out.println(time);
    }

    // this method is used to send a message to all connected clients. 
    //It  then takes a string argument that represents the message to be sent. 
    //If the message is a private message, it is sent only to the selected user. 
    //If the message is a broadcast message, it is sent to all connected clients.
    private synchronized boolean broadcast(String message) {
        // here we are adding a  timestamp to the message
        String time = sdf.format(new Date());

        // to check if message is private ( meaning client to client)
        String[] w = message.split(" ", 3);

        boolean isPrivate = false;
        if (w[1].charAt(0) == '@') {
            isPrivate = true;
        }

        // here we are sending a message to a user who has been mentioned ( example: @mags)
        if (isPrivate == true) {
            String tocheck = w[1].substring(1, w[1].length());

            message = w[0] + w[2];
            String messageLf = time + " " + message + "\n";
            boolean found = false;
            // here we are finding any users that have been mentioned 
            for (int y = a.size(); --y >= 0;) {
                ClientThread ct1 = a.get(y);
                String check = ct1.getUsername();
                if (check.equals(tocheck)) {
                    // we are letting the user know that the message has failed
                    if (!ct1.writeMsg(messageLf)) {
                        a.remove(y);
                        display("this client is disconnected " + ct1.username + "  and the message has been removed from list.");
                    }
                    // the message has been delivered when the username has been found 
                    found = true;
                    break;
                }

            }
            // if the  mentioned user  was not found, return false
            if (found != true) {
                return false;
            }
        } //  here we are determing if the  message is a broadcast message ( meaning private or public)
        else {
            String messageLf = time + " " + message + "\n";
            //  here we are displaying the  message
            System.out.print(messageLf);
            // we arermoving the client from the list if they are diconnected
            for (int i = a.size(); --i >= 0;) {
                ClientThread ct = a.get(i);
                // try to write to the Client if it fails remove it from the list
                if (!ct.writeMsg(messageLf)) {
                    a.remove(i);
                    display(" this client is disconnected" + ct.username + "and the message has been removed from list.");
                }
            }
        }
        return true;

    }

    // this method is used to remove a client from the list of connected clients.
    //It takes an integer argument that represents the id of the client to be removed.
    //When a client is removed, the method broadcasts a message to all remaining clients
    //to inform them that the client is disconnected.
    synchronized void remove(int id) {
        String disconnectedClient = "";
        ///checking the list for the id we keep looking until we find it
        for (int i = 0; i < a.size(); ++i) {
            ClientThread ct = a.get(i);
            // if found remove it
            if (ct.id == id) {
                disconnectedClient = ct.getUsername();
                a.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " this user is not connected to the server" + notif);
    }

    // this  method starts the server and sets the port number to 600, but can be overridden by a command line argument.
    public static void main(String[] args) {
        //starting the server and giving a port number if one wasnt given
        int portNumber = 600;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]); /// this line throws an error if i use int instead of integer
                } catch (Exception e) {
                    System.out.println("the port number is invalid");
                    System.out.println("use: [portNumber] ");
                }
            case 2:
                break;
            default:
                System.out.println("use: [portNumber]");
        }
        Server server = new Server(portNumber);
        server.start();
    }

    // this method receives the username of the client and broadcasts a message to all 
    //connected clients that a new user has connected to the server
// it also handles two other types of messages: LOGOUT and WHOISIN. 
    //When a client sends a LOGOUT message, the server broadcasts a message to all 
    //connected clients that the user has disconnected and then removes the client 
    //from the list of connected clients. 
    //When a client sends a WHOISIN message, the server sends a list of all connected
    //clients to the client who sent the message.. 
    class ClientThread extends Thread {

        // the socket to get messages from client
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        //unique id  this will be easier for deconnecting
        int id;
        // the Username of the Client
        String username;
        // to recieve the message 
        ChatMessage cm;
        // timestamp
        String date;

        // Constructor
        ClientThread(Socket socket) {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            // input and out
            System.out.println("the thread is creating an input and output");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                // reading the username
                username = (String) sInput.readObject();
                broadcast(notif + username + " has just come on." + notif);
            } catch (IOException e) {
                display("Exception creating new Input/output: " + e);
                return;
            } catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void run() {
            ///we are looping until we are logged out
            boolean keepGoing = true;
            while (keepGoing) {
                /// here we are reading in the string 
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + "Exception reading:" + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;

                }
                String message = cm.getMessage();
                switch (cm.getType()) {
                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + message);
                        if (confirmation == false) {
                            String msg = notif + "this user does not exsist" + notif;
                            writeMsg(msg);
                        }
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // send list of active clients
                        for (int i = 0; i < a.size(); ++i) {
                            ClientThread ct = a.get(i);
                            writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }
            //when you are not looping then you will be discconected and removed from the list
            remove(id);
            close();
        }

        /// this method is responsible for The sOutput and sInput are the output and input 
        //while the socket is for the socket connection. it checks if the sOutput, sInput, 
        //and socket are not null before trying to close them. If they are null then this method wont be
        //called. the catch blocks any that are empty which means that any exception 
        //that happens during the closingprocess will be ignored
        private void close() {
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (Exception e) {
            }
            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (Exception e) {
            };
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
        }
        // this method takes a string as a parameter and will return it as a boolean value.
        // the method will check if the socket is still connected and it it is not then it will 
        // be closed/ the close method will return false otherwise the writeObject
        //method is called on the sOutput stream to send the message. 
        //If an exception occurs during this process, the display method is 
        //called to display an error message with the exception details, and the method returns false.

        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                display(notif + "there is a problem sending the message to" + username + notif);
                display(e.toString());
            }
            return true;
        }
    }
}
