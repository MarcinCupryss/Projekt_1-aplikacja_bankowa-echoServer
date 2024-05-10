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

    private String findUserSaldo(String login) {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 4 && userData[0].equals(login)) {
                    return userData[3]; 
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return "0.0"; 
    }

    public void run() {

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

        while (true) {
            String[] s;
            try {
                line = brinp.readLine();
                System.out.println(threadName + "| Line read: " + line);
                if ("login".equals(line)) {
                    System.out.println(threadName + " wants to login.");
                    boolean login_pass = false;
                    while (!login_pass) {
                        out.writeBytes("Enter login and password:" + "\r");
                        System.out.println(threadName + "| Line sent: " + "Enter login and password:");
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
                        System.out.println(threadName + "| Login and password: " + line);
                        String[] parts = line.split(", "); 
                
                        if (parts.length == 2) {
                            String login = parts[0];
                            String password = parts[1];
                
                            boolean userExists = false;
                            try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
                                String currentLine;
                                while ((currentLine = br.readLine()) != null) {
                                    String[] userData = currentLine.split(", ");
                                    if (userData.length >= 2 && userData[0].equals(login) && userData[1].equals(password)) {
                                        userExists = true;
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                System.err.println(threadName + "| Error reading file: " + e.getMessage());
                            }
                
                            if (userExists) {
                                out.writeBytes("Correct login and password. Logged in." + "\r");
                                System.out.println(threadName + "| Correct login and password. Logged in.");
                                login_pass = true;
                
                                while (login_pass) {
                                    try {
                                        line = brinp.readLine();
                                        if ("saldo".equals(line)) {
                                            String userSaldo = findUserSaldo(login);
                                            out.writeBytes("Saldo: " + userSaldo + "\r");
                                            System.out.println(threadName + "| Line sent: Saldo: " + userSaldo);
                                        } else if ("wplata".equals(line)) {
                                            try {
                                                out.writeBytes("Enter the amount to deposit:" + "\r");
                                                line = brinp.readLine();
                                                double depositAmount = Double.parseDouble(line);
                                                
                                                String userSaldo = findUserSaldo(login);
                                                double saldo = Double.parseDouble(userSaldo);
                                                
                                                saldo += depositAmount;
                                                try (BufferedReader file = new BufferedReader(new FileReader("users.txt"))) {
                                                    List<String> lines = new ArrayList<>();
                                                    String currentLine;
                                                    while ((currentLine = file.readLine()) != null) {
                                                        String[] userData = currentLine.split(", ");
                                                        if (userData.length >= 4 && userData[0].equals(login)) {
                                                            userData[3] = Double.toString(saldo);
                                                            currentLine = String.join(", ", userData);
                                                        }
                                                        lines.add(currentLine);
                                                    }
                                                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
                                                        for (String writeLine : lines) {
                                                            writer.write(writeLine + "\n");
                                                        }
                                                    }
                                                }
                                                out.writeBytes("Deposit successful. New balance: " + saldo + "\r");
                                                System.out.println(threadName + "| Deposit successful. New balance: " + saldo);
                                            } catch (NumberFormatException e) {
                                                out.writeBytes("Invalid amount format." + "\r");
                                                System.out.println(threadName + "| Invalid amount format.");
                                            } catch (IOException e) {
                                                System.out.println(threadName + "| Input-output error." + e);
                                                return;
                                            }
                                        
                                        } else if ("wyplata".equals(line)) {
                                            try {
                                                out.writeBytes("Enter the amount to withdraw:" + "\r");
                                                line = brinp.readLine();
                                                double withdrawAmount = Double.parseDouble(line);
                                                
                                                String userSaldo = findUserSaldo(login);
                                                double saldo = Double.parseDouble(userSaldo);
                                                
                                                if (withdrawAmount > saldo) {
                                                    out.writeBytes("Insufficient funds." + "\r");
                                                    System.out.println(threadName + "| Insufficient funds.");
                                                } else {
                                                    saldo -= withdrawAmount;
                                                    try (BufferedReader file = new BufferedReader(new FileReader("users.txt"))) {
                                                        List<String> lines = new ArrayList<>();
                                                        String currentLine;
                                                        while ((currentLine = file.readLine()) != null) {
                                                            String[] userData = currentLine.split(", ");
                                                            if (userData.length >= 4 && userData[0].equals(login)) {
                                                                userData[3] = Double.toString(saldo);
                                                                currentLine = String.join(", ", userData);
                                                            }
                                                            lines.add(currentLine);
                                                        }
                                                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
                                                            for (String writeLine : lines) {
                                                                writer.write(writeLine + "\n");
                                                            }
                                                        }
                                                    }
                                                    out.writeBytes("Withdrawal successful. New balance: " + saldo + "\r");
                                                    System.out.println(threadName + "| Withdrawal successful. New balance: " + saldo);
                                                }
                                            } catch (NumberFormatException e) {
                                                out.writeBytes("Invalid amount format." + "\r");
                                                System.out.println(threadName + "| Invalid amount format.");
                                            } catch (IOException e) {
                                                System.out.println(threadName + "| Input-output error." + e);
                                                return;
                                            }
                                        } else if ("przelew".equals(line)) {
                                            // Obsłuż operację przelewu
                                        } else if ("logout".equals(line)) {
                                            out.writeBytes("Logged out successfully.\r");
                                            System.out.println(threadName + "| Logged out successfully.");
                                            login_pass = true;
                                            break;
                                        } else {
                                            out.writeBytes("Invalid operation." + "\r");
                                            System.out.println(threadName + "| Invalid operation.");
                                        }
                                    } catch (IOException e) {
                                        System.out.println(threadName + "| Input-output error." + e);
                                        return;
                                    }
                                }
                            } else {
                                out.writeBytes("Incorrect login or password." + "\r");
                                System.out.println(threadName + "| Incorrect login or password.");
                            }
                        } else {
                            System.out.println(threadName + "| Invalid data format. Required: login, password.");
                            out.writeBytes("Invalid data format. Required: login, password" + "\r");
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
                                    pw.println(name + ", " + password + ", " + nickname + ", 0.0");
                                    pw.flush();
                                    System.out.println(threadName + "| User added to the file.");
                
                                    ArrayList<String> newUser = new ArrayList<>();
                                    newUser.add(name);
                                    newUser.add(password);
                                    newUser.add(nickname);
                                    newUser.add("0.0"); 
                                    logins.add(newUser);
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
                    try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
                        String currentLine;
                        StringBuilder userList = new StringBuilder();
                        while ((currentLine = br.readLine()) != null) {
                            String[] userData = currentLine.split(", ");
                            if (userData.length >= 1) {
                                userList.append(userData[0]).append(" ");
                            }
                        }
                        out.writeBytes(userList.toString().trim() + "\r");
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + e.getMessage());
                    }

                } else { 
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
