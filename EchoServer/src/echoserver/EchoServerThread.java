/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package echoserver;

/**
 *
 * @author dzelazny
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServerThread implements Runnable {

    protected Socket socket;
    CopyOnWriteArrayList<ArrayList<String>> logins;

    public EchoServerThread(Socket clientSocket, CopyOnWriteArrayList<ArrayList<String>> logins) {
        this.socket = clientSocket;
        this.logins = logins;
    }

    public void run() {
        //Deklaracje zmiennych

        BufferedReader brinp = null;
        DataOutputStream out = null;
        String threadName = Thread.currentThread().getName();

        //inicjalizacja strumieni
        try {
            brinp = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()
                    )
            );
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println(threadName + "| Błąd przy tworzeniu strumieni " + e);
            return;
        }
        String line = null;
        String login = null;
        //pętla główna
        while (true) {
            String[] s;
            try {
                line = brinp.readLine();
                System.out.println(threadName + "| Line read: " + line);
                if ("login".equals(line)) {
                    System.out.println(threadName + " wants to login.");
                    boolean login_pass = false;
                    while (!login_pass) {
                        out.writeBytes("Login and password?" + "\r");
                        System.out.println(threadName + "| Line sent: " + "Login and password?");
                        line = "";
                        while (line.length() < 1) {
                            try {
                                line = brinp.readLine();
                                System.out.println("Line:" + line.length());
                            } catch (IOException e) {
                                System.out.println(threadName + "| Błąd wejścia-wyjścia." + e);
                                return;
                            }
                        }
                        System.out.println(threadName + "| Login and pass: " + line);
                        s = line.split(" ");
                        login = line;
                        for (int i = 0; i < logins.size(); i++) {
                            if (logins.get(i).get(0).equals(s[0]) && logins.get(i).get(1).equals(s[1])) {
                                login_pass = true;
                                out.writeBytes("Correct login and password" + "\r");
                                System.out.println(threadName + "| Line sent: " + "Correct login and password");
                                //menu 
                                while (login_pass){
                                    line = brinp.readLine();
                                    if ("saldo".equals(line)) {
                                    out.writeBytes("Saldo: " + logins.get(i).get(3) +  "\r");
                                    System.out.println(threadName + "| Line sent: " + logins.get(i).get(3));

                                    } else if ("wplata".equals(line)) {
                                    
                                    } else if ("wyplata".equals(line)) {
                                    
                                    } else if ("przelew".equals(line)) {
                                        
                                    } else if ("logout".equals(line)) {
                                        login_pass = false;
                                        break;
                                    } 
                                }
                            }
                        }
                    }
                } else if ("register".equals(line)) {
                    System.out.println(threadName + " wants to register.");
                    boolean login_pass = false;
                    while (!login_pass) {
                        out.writeBytes("Enter name, password, and nickname:" + "\r");
                        System.out.println(threadName + "| Line sent: " + "Enter name, password, and nickname:");
                        line = "";
                        while (line.length() < 1) {
                            try {
                                line = brinp.readLine();
                                System.out.println("Line:" + line.length());
                            } catch (IOException e) {
                                System.out.println(threadName + "| Input-output error." + e);
                                return;
                            }
                        }
                        System.out.println(threadName + "| Name, password, and nickname: " + line);
                        String[] parts = line.split(", ");
                
                        if (parts.length == 3) {
                            String name = parts[0];
                            String password = parts[1];
                            String nickname = parts[2];
                        
                            boolean userExists = false;
                            try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
                                String currentLine;
                                while ((currentLine = br.readLine()) != null) {
                                    String[] userData = currentLine.split(", ");
                                    if (userData.length >= 3 && userData[0].equals(name)) {
                                        userExists = true;
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                System.err.println(threadName + "| Error reading file: " + e.getMessage());
                            }
                            
                            if (userExists) {
                                out.writeBytes("User with this login already exists." + "\r");
                                System.out.println(threadName + "| User with this login already exists.");
                            } else {
                                try (FileWriter fw = new FileWriter("users.txt", true);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        PrintWriter pw = new PrintWriter(bw)) {
                                    pw.println(name + ", " + password + ", " + nickname + ", 0.0"); // Dodajemy użytkownika z saldem 0
                                    pw.flush();
                                    System.out.println(threadName + "| User added to the file.");
                                } catch (IOException e) {
                                    System.err.println(threadName + "| Error writing to file: " + e.getMessage());
                                    return;
                                }
                
                                out.writeBytes("User registered temporarily" + "\r");
                                login_pass = true;
                            }
                        } else {
                            System.out.println(threadName + "| Invalid data format. Required: name, password, nickname.");
                            out.writeBytes("Invalid data format. Required: name, password, nickname" + "\r");
                        }
                    }
            
                } else if ("list".equals(line)) {
                    String user_list = "";
                    for (int i = 0; i < logins.size(); i++) {
                        user_list += (logins.get(i).get(0) + " ");
                    }
                    out.writeBytes(user_list + "\r");

                } else { //odesłanie danych do klienta
                    out.writeBytes(line + "\r");
                    System.out.println(threadName + "| Line sent: " + line);
                    line = brinp.readLine();
                }

            } catch (IOException e) {
                System.out.println(threadName + "| Błąd wejścia-wyjścia." + e);
                return;
            }
        }
    }
}
