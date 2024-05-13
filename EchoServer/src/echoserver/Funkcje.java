package echoserver;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Funkcje implements Runnable {

    protected Socket socket;
    CopyOnWriteArrayList<ArrayList<String>> logins;
    private String login;
    private boolean isLoggedIn = false;

    public Funkcje(Socket clientSocket) {
        this.socket = clientSocket;
        this.logins = new CopyOnWriteArrayList<>();
    }

    public void run() {
        BufferedReader brinp = null;
        DataOutputStream out = null;
        String threadName = Thread.currentThread().getName();

        //inicjalizacja strumieni
        try {
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println(threadName + "| Błąd przy tworzeniu strumieni " + e);
            return;
        }
        String line = null;
//        boolean isLoggedIn = false;

        // Główna pętla
        while (true) {
            try {
                line = brinp.readLine();
                System.out.println(threadName + "| Line read: " + line);

                // Obsługa komend dla niezalogowanych użytkowników
                if (isLoggedIn) {
                    // Obsługa komend dla zalogowanych użytkowników
                    if ("saldo".equals(line)) {
                        sendUserSaldo(out, threadName, login);
                    } else if ("wplata".equals(line)) {
                        handleDeposit(brinp, out, threadName, login);
                    } else if ("wyplata".equals(line)) {
                        handleWithdrawal(brinp, out, threadName, login);
                    } else if ("przelew".equals(line)) {
                        // Obsługa przelewu
                    } else if ("logout".equals(line)) {
                        logout(out, threadName);
                        login = null;
                        isLoggedIn = false;
                    } else {
                        out.writeBytes("Invalid operation.\r");
                        out.flush();
                        System.out.println(threadName + "| Invalid operation.");
                    }
                } else if ("login".equals(line)) {
                    handleLogin(brinp, out, threadName);
                } else if ("register".equals(line)) {
                    handleRegistration(brinp, out, threadName);
                } else if ("list".equals(line)) {
                    sendUserList(out);
                } else {
                    out.writeBytes("Invalid command.\r");
                    out.flush();
                    System.out.println(threadName + "| Invalid command.");
                }

            } catch (IOException e) {
                System.out.println(threadName + "| Błąd wejścia-wyjścia." + e);
                return;
            }
        }
    }

//    private boolean isLoggedIn(String line) {
//        return line.startsWith("login") || line.startsWith("register");
//    }

    private void handleLogin(BufferedReader brinp, DataOutputStream out, String threadName) throws IOException {
        System.out.println(threadName + " wants to login.");
        out.writeBytes("Enter login and password:" + "\r");
        out.flush();
        System.out.println(threadName + "| Line sent: " + "Enter login and password:");
        String line = "";
        while (line.length() < 1) {
            line = brinp.readLine();
            System.out.println("Line:" + line.length());
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
                    if (userData.length >= 2 && userData[0].equals(login) && userData[3].equals(password)) {
                        userExists = true;
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println(threadName + "| Error reading file: " + e.getMessage());
            }

            if (userExists) {
                out.writeBytes("Correct login and password. Logged in." + "\r");
                out.flush();
                System.out.println(threadName + "| Correct login and password. Logged in.");
                setLogin(login);
                isLoggedIn = true;
            } else {
                out.writeBytes("Incorrect login or password." + "\r");
                out.flush();
                System.out.println(threadName + "| Incorrect login or password.");
            }
        } else {
            System.out.println(threadName + "| Invalid data format. Required: login, password.");
            out.writeBytes("Invalid data format. Required: login, password" + "\r");
            out.flush();
        }
    }

    private void handleRegistration(BufferedReader brinp, DataOutputStream out, String threadName) throws IOException {
        out.writeBytes("Enter name, lastname, PESEL and password:" + "\r");
        out.flush();
        System.out.println(threadName + "| Line sent: " + "Enter name, lastname, PESEL and password:");
        String line = "";
        while (line.length() < 1) {
            line = brinp.readLine();
            System.out.println("Line:" + line.length());
        }
        System.out.println(threadName + "| Name, password, and PESEL: " + line);
        String[] parts = line.split(", ");

        if (parts.length == 4) {
            String name = parts[0];
            String lastname = parts[1];
            String pesel = parts[2];
            String password = parts[3];

            boolean userExists = false;
            try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
                String currentLine;
                while ((currentLine = br.readLine()) != null) {
                    String[] userData = currentLine.split(", ");
                    if (userData.length >= 4 && userData[2].equals(pesel)) {
                        userExists = true;
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println(threadName + "| Error reading file: " + e.getMessage());
            }

            if (userExists) {
                out.writeBytes("User with this PESEL already exists." + "\r");
                out.flush();
                System.out.println(threadName + "| User with this PESEL already exists.");
            } else if (!isValidPesel(pesel)) {
                out.writeBytes("Invalid PESEL format. PESEL must contain exactly 11 digits.\r");
                out.flush();
                System.out.println(threadName + "| Invalid PESEL format.");
            } else {
                try (FileWriter fw = new FileWriter("users.txt", true);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter pw = new PrintWriter(bw)) {
                    pw.println(name + ", " + lastname + ", " + pesel + ", " + password + ", 0.0");
                    pw.flush();
                    System.out.println(threadName + "| User added to the file.");

                    ArrayList<String> newUser = new ArrayList<>();
                    newUser.add(name);
                    newUser.add(lastname);
                    newUser.add(pesel);
                    newUser.add(password);
                    newUser.add("0.0");
                    logins.add(newUser); // Dodanie nowego użytkownika do listy zalogowanych
                } catch (IOException e) {
                    System.err.println(threadName + "| Error writing to file: " + e.getMessage());
                    return;
                }

                out.writeBytes("User registered temporarily" + "\r");
                out.flush();
                System.out.println(threadName + "| User registered temporarily.");
            }
        } else {
            System.out.println(threadName + "| Invalid data format. Required: name, lastname, PESEL, password");
            out.writeBytes("Invalid data format. Required: name, lastname, PESEL, password" + "\r");
            out.flush();
        }
    }

    private void sendUserList(DataOutputStream out) throws IOException {
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
            out.flush();
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private void sendUserSaldo(DataOutputStream out, String threadName, String login) throws IOException {

        String userSaldo = findUserSaldo(login);
        out.writeBytes("Saldo: " + userSaldo + "\r");
        out.flush();
        System.out.println(threadName + "| Line sent: Saldo: " + userSaldo);
    }

    private void handleDeposit(BufferedReader brinp, DataOutputStream out, String threadName, String login) throws IOException {
        out.writeBytes("Enter the amount to deposit:" + "\r");
        out.flush();
        System.out.println(threadName + "| Line sent: " + "Enter the amount to deposit:");
        String line = "";
        while (line.isEmpty()) {
            line = brinp.readLine();
        }
        double depositAmount = Double.parseDouble(line);

        String userSaldo = findUserSaldo(login);
        double saldo = Double.parseDouble(userSaldo);

        saldo += depositAmount;
        try (BufferedReader file = new BufferedReader(new FileReader("users.txt"))) {
            List<String> lines = new ArrayList<>();
            String currentLine;
            while ((currentLine = file.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 5 && userData[0].equals(login)) {
                    userData[4] = Double.toString(saldo);
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
        out.flush();
        System.out.println(threadName + "| Deposit successful. New balance: " + saldo);
    }

    private void handleWithdrawal(BufferedReader brinp, DataOutputStream out, String threadName, String login) throws IOException {
        out.writeBytes("Enter the amount to withdraw:" + "\r");
        out.flush();
        System.out.println(threadName + "| Line sent: " + "Enter the amount to withdraw:");
        String line = "";
        while (line.isEmpty()) {
            line = brinp.readLine();
        }
        double withdrawAmount = Double.parseDouble(line);

        String userSaldo = findUserSaldo(login);
        double saldo = Double.parseDouble(userSaldo);

        if (withdrawAmount > saldo) {
            out.writeBytes("Insufficient funds." + "\r");
            out.flush();
            System.out.println(threadName + "| Insufficient funds.");
        } else {
            saldo -= withdrawAmount;
            try (BufferedReader file = new BufferedReader(new FileReader("users.txt"))) {
                List<String> lines = new ArrayList<>();
                String currentLine;
                while ((currentLine = file.readLine()) != null) {
                    String[] userData = currentLine.split(", ");
                    if (userData.length >= 5 && userData[0].equals(login)) {
                        userData[4] = Double.toString(saldo);
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
            out.flush();
            System.out.println(threadName + "| Withdrawal successful. New balance: " + saldo);
        }
    }

    private void logout(DataOutputStream out, String threadName) throws IOException {
        out.writeBytes("Logged out successfully.\r");
        out.flush();
        System.out.println(threadName + "| Logged out successfully.");
    }

    private String findUserSaldo(String login) {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 5 && userData[0].equals(login)) {
                    return userData[4];
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return "0.0";
    }

    private boolean isValidPesel(String nip) {
        return nip.matches("\\d{11}");
    }

    private void setLogin(String login) {
        this.login = login;
    }

//    private String getLogin() {
//        return login;
//    }
}