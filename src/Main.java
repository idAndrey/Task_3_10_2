import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;



public class Main {

    static class Transaction {
        public final int fromId;
        public final int toId;
        public final int amount;

        Transaction(int fromId, int toId, int amount) {
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
        }
    }

    static class User {
        private int user_id;
        private int balance;
        public ReentrantLock user_lock = new ReentrantLock();

        User(int user_id){
            this.user_id = user_id;
            this.balance = 0;
        }
        User(int user_id, int balance){
            this.user_id = user_id;
            this.balance = balance;
        }

        public void setUser_id(int user_id) {
            this.user_id = user_id;
        }

        public int getUser_id() {
            return user_id;
        }

        public void setBalance(int balance) {
            this.balance = balance;
        }

        public int getBalance() {
            return balance;
        }

        public ReentrantLock getUser_lock() {
            return user_lock;
        }
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        ReentrantLock[] locks;

        System.out.println("Задайте количество пользователей (n): ");
        System.out.print("==> ");

        int n = scanner.nextInt();

        if( n <= 1) {
            System.out.println("Количество пользователей должно быть больше двух!");
            return;
        }

        List<User> userList = new ArrayList<User>(n);
        locks = new ReentrantLock[n];

//        for (int i = 0; i < n; i++) {
//            userList.add(new User(i));
//            locks[i] = userList.get(i).getUser_lock();
//        }

        System.out.println();
        System.out.println("Задайте начальный баланс каждого пользователя через пробел.");
        System.out.print("==> ");

        for (int i = 0; i < n; i++) {

            int currentBalance = scanner.nextInt();
            User currentUser = new User(i, currentBalance);
            locks[i] = currentUser.getUser_lock();
            userList.add(currentUser);
        }

        System.out.println("Задайте количество транзакций (m): ");
        System.out.print("==> ");

        int m = scanner.nextInt();
        scanner.nextLine();

        Transaction[] transactions = new Transaction[m];

        System.out.println("Введите транзакции в формате: fromId - amount - toId, ");
        System.out.println("где fromId -  ID пользователя, с чьего счета списывать,");
        System.out.println("где amount -  кол-во денежных единиц,");
        System.out.println("где fromId -  ID пользователя, на чей счет перевести.");

        for (int j = 0; j < m; j++) {
            System.out.print("==> ");
            String[] parts = scanner.nextLine().split(" - ");
            int fromId = Integer.parseInt(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            int toId = Integer.parseInt(parts[2]);
            transactions[j] = new Transaction(fromId, toId, amount);
        }
        scanner.close();

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(m, 14));

        for (Transaction transaction : transactions) {
            executor.execute(() -> completeTransaction(transaction, userList, locks));
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        for (int i = 0; i < n; i++) {
            System.out.println("User " + i + " final balance: " + userList.get(i).getBalance());
        }
    }

    private static void completeTransaction(Transaction transaction, List<User> userList, ReentrantLock[] locks) {
        int fromId = transaction.fromId;
        int toId = transaction.toId;
        int amount = transaction.amount;

        if (fromId == toId) return; // Исключаем транзакции самому себе

        // Берем блокировки в одном порядке, чтобы избежать deadlock
        ReentrantLock firstLock = fromId < toId ? locks[fromId] : locks[toId];
        ReentrantLock secondLock = fromId < toId ? locks[toId] : locks[fromId];

        firstLock.lock();
        secondLock.lock();
        try {
            int balance_fromId = userList.get(fromId).getBalance();
            int balance_toId = userList.get(toId).getBalance();
            if ( balance_fromId >= amount) {
                userList.get(fromId).setBalance(balance_fromId - amount);
                userList.get(toId).setBalance(balance_toId + amount);
            }
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
    }
}