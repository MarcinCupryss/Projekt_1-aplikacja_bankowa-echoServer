package echoserver;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServerThread implements Runnable {

    protected Socket socket;
    CopyOnWriteArrayList<ArrayList<String>> logins;
    private String login;
    private boolean isLoggedIn = false;

    public EchoServerThread(Socket clientSocket) {
        this.socket = clientSocket;
        this.logins = new CopyOnWriteArrayList<>();
    }

    public void run() {
        BufferedReader brinp = null;
        BufferedWriter writer = null;
        String threadName = Thread.currentThread().getName();

        try {
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println(threadName + "| Błąd przy tworzeniu strumieni " + e);
            return;
        }
        String line = null;

        while (true) {
            try {
                line = brinp.readLine();
                System.out.println(threadName + "| Line read: " + line);

                if (isLoggedIn) {
                    if ("saldo".equals(line)) {
                        sendUserBalance(writer, threadName, login);
                    } else if ("wplata".equals(line)) {
                        handleDeposit(brinp, writer, threadName, login);
                    } else if ("wyplata".equals(line)) {
                        handleWithdrawal(brinp, writer, threadName, login);
                    } else if ("przelew".equals(line)) {
                        // Obsługa przelewu
                    } else if ("wyloguj".equals(line)) {
                        logout(writer, threadName);
                    } else if ("komendy".equals(line)) {
                        getCommands(writer);
                        System.out.println(threadName + "| User " + login + " got commands list.");
                    } else {
                        writer.write("Niepoprawna komenda!" + System.lineSeparator());
                        writer.flush();
                        System.out.println(threadName + "| Invalid operation.");
                    }
                } else if ("zaloguj".equals(line)) {
                    handleLogin(brinp, writer, threadName);
                } else if ("rejestracja".equals(line)) {
                    handleRegistration(brinp, writer, threadName);
                } else if ("lista".equals(line)) {
                    sendUserList(writer);
                } else if ("komendy".equals(line)) {
                    getCommands(writer);
                    System.out.println(threadName + "| Sending commands list.");
                } else {
                    writer.write("Niepoprawna komenda!" + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| Invalid command.");
                }

            } catch (IOException e) {
                System.out.println(threadName + "| Input/output error." + e);
                return;
            }
        }
    }

    private void getCommands(BufferedWriter writer) throws IOException {
        String commands = "";
        if (isLoggedIn) {
            commands = "Komendy do wyboru: saldo, wplata, wyplata, przelew, komendy, wyloguj";
        } else {
            commands = "Komendy do wyboru: zaloguj, rejestracja, lista, komendy";
        }
        writer.write(commands + System.lineSeparator());
        writer.flush();
    }

    private void handleLogin(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {
        boolean logLoop = false;
        System.out.println(threadName + "| User wants to log in.");
        writer.write("Wprowadź login oraz hasło rozdzielone \", \"." + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Message sent: " + "Enter login and password:");
        while (logLoop == false) {
            String line = brinp.readLine();
            String[] parts = line.split(", ");

            if (parts.length == 2) {
                String login = parts[0];
                String password = parts[1];

                boolean userExists = false;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
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
                    writer.write("Zalogowano!" + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User " + login + " has logged in.");
                    setLogin(login);
                    isLoggedIn = true;
                    logLoop = true;
                } else {
                    writer.write("Niepoprawnie wpisano login/hasło, wprowadź login oraz hasło rozdzielone \", \"." + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User has put incorrect login and/or password.");
                }
            } else {
                System.out.println(threadName + "| Invalid data format. Required: login, password.");
                writer.write("Niepoprawny format danych, wprowadź login oraz hasło rozdzielone \", \"." + System.lineSeparator());
                writer.flush();
            }
        }

    }

    private void handleRegistration(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {
        boolean registerLoop = false;
        writer.write("Podaj imię, nazwisko, PESEL, hasło rozdzielone \", \":" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Line sent: " + "Enter name, lastname, PESEL and password:");
        while (registerLoop == false) {
            String line = "";
            while (line.length() < 1) {
                line = brinp.readLine();
            }
            System.out.println(threadName + "| Trying to register: " + line);
            String[] parts = line.split(", ");

            if (parts.length == 4) {
                String name = parts[0];
                String lastname = parts[1];
                String pesel = parts[2];
                String password = parts[3];

                boolean userExists = false;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
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
                    writer.write("Użytkownik z tym numerem PESEL już istnieje." + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User with this PESEL already exists.");
                } else if (!isValidPesel(pesel)) {
                    writer.write("Niepoprawnie wpisano PESEL. PESEL powinien zawierać 11 cyfr." + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| Invalid PESEL format.");
                } else {
                    try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("users.txt", true), StandardCharsets.UTF_8);
                         BufferedWriter bw = new BufferedWriter(osw);
                         PrintWriter pw = new PrintWriter(bw)) {
                        pw.println(name + ", " + lastname + ", " + pesel + ", " + password + ", 0.0");
                        pw.flush();
                        System.out.println(threadName + "| User " + name + " added to the file.");

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

                    writer.write("Użytkownik " + name + " " + lastname + " został zarejestrowany." + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User registered.");
                    registerLoop = true;
                }
            } else {
                System.out.println(threadName + "| Invalid data format. Required: name, lastname, PESEL, password");
                writer.write("Niepoprawne dane. Wymagane: imię, nazwisko, PESEL, hasło" + System.lineSeparator());
                writer.flush();
            }
        }
    }

    private void sendUserList(BufferedWriter writer) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            StringJoiner userList = new StringJoiner(", ");
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 1 && !userData[0].isEmpty()) {
                    userList.add(userData[0]);
                }
            }
            String userListString = userList.toString();
            if (!userListString.isEmpty()) {
                writer.write("Lista użytkowników: " + userListString + System.lineSeparator());
                writer.flush();
            } else {
                writer.write("Lista użytkowników jest pusta." + System.lineSeparator());
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private void sendUserBalance(BufferedWriter writer, String threadName, String login) throws IOException {
        String userSaldo = findUserSaldo(login);
        writer.write("Środki na koncie: " + userSaldo + " PLN" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| User " + login + " has checked account balance. Balance: " + userSaldo + " PLN");
    }

    private void handleDeposit(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        writer.write("Wprowadź wartość do wpłacenia:" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Message sent: " + "Enter the amount to deposit:");
        String line = "";
        while (line.isEmpty()) {
            line = brinp.readLine();
        }
        double depositAmount = Double.parseDouble(line);

        String userSaldo = findUserSaldo(login);
        double saldo = Double.parseDouble(userSaldo);

        saldo += depositAmount;
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
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
            try (BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
                for (String writeLine : lines) {
                    fileWriter.write(writeLine + System.lineSeparator());
                }
            }
        }
        writer.write("Na konto wpłynęło " + depositAmount + ". Środki na koncie: " + saldo + " PLN" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Deposit successful. New balance: " + saldo + " PLN");
    }

    private void handleWithdrawal(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        writer.write("Wprowadź wartość do wypłaty." + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Message sent: " + "Enter the amount to withdraw:");
        String line = "";
        while (line.isEmpty()) {
            line = brinp.readLine();
        }
        double withdrawAmount = Double.parseDouble(line);

        String userSaldo = findUserSaldo(login);
        double saldo = Double.parseDouble(userSaldo);

        if (withdrawAmount > saldo) {
            writer.write("Przekroczono ilość środków na koncie!" + System.lineSeparator());
            writer.flush();
            System.out.println(threadName + "| + User " + login + " has insufficient funds to withdraw + " + withdrawAmount + " PLN");
        } else {
            saldo -= withdrawAmount;
            try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
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
                try (BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
                    for (String writeLine : lines) {
                        fileWriter.write(writeLine + System.lineSeparator());
                    }
                }
            }
            writer.write("Wypłacono " + withdrawAmount + ". Pozostałe środki na koncie: " + saldo + " PLN" + System.lineSeparator());
            writer.flush();
            System.out.println(threadName + "| Withdrawal successful. New balance: " + saldo + " PLN");
        }
    }

    private void logout(BufferedWriter writer, String threadName) throws IOException {
        writer.write("Wylogowano!" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Logged out successfully.");
        login = null;
        isLoggedIn = false;
    }

    private String findUserSaldo(String login) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
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

}