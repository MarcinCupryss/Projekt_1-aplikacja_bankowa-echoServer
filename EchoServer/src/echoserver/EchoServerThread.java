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
                    System.out.println(threadName + " wants to login.");
                    boolean login_pass = false;
                    while (!login_pass) {
                        out.writeBytes("Login password nickname?" + "\r");
                        System.out.println(threadName + "| Line sent: " + "Login password nickname?");
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
                        System.out.println(threadName + "| Login and pass and nick: " + line);
                        s = line.split(" ");
                        login = line;
                        login_pass = true;
                        for (int i = 0; i < logins.size(); i++) {
                            if (logins.get(i).get(0).equals(s[0])) {
                                login_pass = false;
//                                out.writeBytes("Login already in use :(" + "\r");
                                System.out.println(threadName + "| Line sent: " + "Login already in use :(");
                                break;
                            }
                        }
                        if (login_pass) {
                            s = login.split(" ");
                            ArrayList<String> new_user = new ArrayList<>();
                            for (int i = 0; i < 3; i++) {
                                new_user.add(s[i]);
                            }
                            logins.add(new_user);

                            out.writeBytes("User registered temporarily" + "\r");
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
