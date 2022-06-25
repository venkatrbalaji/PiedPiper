package com.example.mcproject.connection;

public class ClientThread {

    static Thread t;

    public static void start(){
        t = new Thread(new innerThread());
    }


    public static void stop(){
        
    }

    private static class innerThread implements  Runnable{

        @Override
        public void run() {
            while (true){
                //poll server
            }
        }
    }
}
