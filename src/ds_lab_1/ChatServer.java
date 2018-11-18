package ds_lab_1;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class ChatServer {
    
    BufferedReader in;
    PrintWriter out;
    public static int PORT = 9016;
    
    static JFrame frame = new JFrame("Server");
    JTextField textField = new JTextField(40);
    private static JTextArea messageArea = new JTextArea(8, 40);
  
    private String clientName;
    private String[] allClients = {"All"};
    private boolean isUnicast;
    static DefaultListModel clientsModel = new DefaultListModel();
    JList clientList = new JList(clientsModel);

    
    private static String getPortAddress()
    {
        return JOptionPane.showInputDialog(
                frame,
                "Enter Port Address of the Server:",
                "Welcome to the Chat",
                JOptionPane.QUESTION_MESSAGE);    
    }
    
     /**
     * Constructs the client by laying out the GUI and registering a listener with the textfield so that pressing Return in the listener sends the textfield contents to the server. Note however that the textfield is initially NOT editable, and only becomes editable AFTER the client receives the NAMEACCEPTED message from the server.
     */
    public ChatServer() {

        // Layout GUI
        textField.setEditable(true);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "South");
        frame.getContentPane().add(clientList, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        textField.requestFocus();
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending the contents of the text field to the server. Then clear the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                //Here we must check whether user has selected a recipent from combo boc
                //If selected we must send the message to intended recipent only
                if(clientList.isSelectionEmpty()){
                    return;
                }
                
                int selectedItems[] = clientList.getSelectedIndices();
                for (int i = 0; i < selectedItems.length; i++) {
                    String selectedItem = clientsModel.get(selectedItems[i]).toString();
                    PrintWriter writer = writerMap.get(selectedItem);
                  
  
                    writer.println("Server -> " + selectedItem + ": " + textField.getText());
                    messageArea.append("Server ->" + selectedItem + ": " + textField.getText() + "\n");
                 
                }
                
                textField.setText("");
            }
        });
    }
            
            
    
    /**
     * The set of all names of clients in the chat room. Maintained so that we can check that new clients are not registering name already in use.
     */
    private static HashSet<String> names = new HashSet<String>();
    
    
    /**
     * The set of all the print writers for all the clients. This set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
    private static HashMap<String, PrintWriter> writerMap = new HashMap<String, PrintWriter>();
    
    /**
     * The appplication main method, which just listens on a port and spawns handler threads.
     */
    
    public static void main(String[] args) throws Exception {
        
        
        messageArea.append("The chat server is running." + "\n");
        PORT =  Integer.parseInt(getPortAddress());
            
        ServerSocket listener = new ServerSocket(PORT);
        ChatServer server = new ChatServer();
        server.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        server.frame.setVisible(true);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }
    
    /**
     * A handler thread class. Handlers are spawned from the listening loop and are responsible for a dealing with a single client and broadcasting its messages.
     */
    private static class Handler extends Thread {

        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
       
        /**
         * Constructs a handler thread, squirreling away the socket. All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }
        
        /**
         * Services this thread's client by repeatedly requesting a screen name until a unique one has been submitted, then acknowledges the name and registers the output stream for the client in a global set, then repeatedly gets inputs and broadcasts them.
         */
        @Override
        public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        
                        return;
                       
                    }
                     
                    if (name.startsWith("FILES"))
                    {
  
                        name = name.substring(4);
                        synchronized (names) {
                        if (!names.contains(name)) {
                          messageArea.append("New File Client Added: " + name + "\n");
                            names.add(name);
                            clientsModel.add(names.size() - 1, name);
                            writerMap.put(name, out);
                           
                             out.println("NAMEACCEPTED");
                             //out.println("");
                             writers.add(out);
                               //Now we must populate the ComboBoxes in client environments with the name of all the clients
                              updateClients();
                         
                              break;
                         }
                    }
                   }
                        
                        else
                        {
                        
                        synchronized (names) {
                        if (!names.contains(name)) {
                          messageArea.append("New Chat Client Added: " + name + "\n");
                            names.add(name);
                            clientsModel.add(names.size() - 1, name);
                            writerMap.put(name, out);
                            
                             out.println("NAMEACCEPTED");
                             writers.add(out);

                                //Now we must populate the ComboBoxes in client environments with the name of all the clients
                              updateClients();
                            
                            break;
                          }
                      }
        
                    }                  
                }
                
                
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
//                    if (isUnicastMessage(input)) {
//                        System.out.println("UNICAST MESSAGE");
//                        writerMap.get(name).println("MESSAGE " + name + ": " + input.replace(getUnicastRecipent(input) + ">>", ""));
//                        writerMap.get(getUnicastRecipent(input)).println("MESSAGE " + name + ": " + input.replace(getUnicastRecipent(input) + ">>", ""));
//                    } else {
//                        for (PrintWriter writer : writers) {
//                            writer.println(name + ": " + input);
                            messageArea.append(name + ": " + input + "\n");
                        //}
                   // }
                    if (input == null) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                writerMap.remove(name);
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                    updateClients();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        //Check if a message is intended for a specific client
        public boolean isUnicastMessage(String message) {
            for (String clientName : names) {
                String clientPrefix = clientName + ">>";
                if (message.contains(clientPrefix)) {
                    return true;
                }
            }
            return false;
        }

        //Extract only the message from a unicast message
        public String getUnicastRecipent(String message) {
            String unicastClient = null;
            for (String clientName : names) {
                String clientPrefix = clientName + ">>";
                if (message.contains(clientPrefix)) {
                    unicastClient = clientName;
                    break;
                }
            }
            return unicastClient;
        }
        
        
        //Removes specified client from the list
        public void removeClient(String clientName) {
            writerMap.remove(clientName);
            messageArea.append("Client Removed: " + clientName + "\n");
        }
        
        //Returns a list of clients to the client side
        public static String getAllClients() {
            String[] keys = writerMap.keySet().toArray(new String[0]);
            String clientArray = "All";
            for (int i = 0; i < keys.length; i++) {
                clientArray = clientArray + "," + keys[i];
            }
            return clientArray;
        }

        //Updates client list when a new client is added or removed
        public void updateClients() {
            for (PrintWriter writer : writers) {
                writer.println("CLIENTLIST" + getAllClients());
            }
        }
    }
}
