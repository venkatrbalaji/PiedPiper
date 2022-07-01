package com.example.mcproject.filemanagement;

import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyFileManager {

    //This class should manage the files partitions present in directory dir
    //all file parts will be stored in dir
    private static final String TAG = "PPFileManager";
//    private static final String dirName = "/my_files/";
    static public File directory;
//    static String dirName = "C:\\Users\\saura\\Desktop\\TestFol\\";

    public MyFileManager(File ppSysDir, String uploadDirectoryName){
        directory = new File(ppSysDir, "/" + uploadDirectoryName + "/");
    }
    public static void processFileDownload(String filename,OutputStream os){
        checkDir();
        try {

        } catch (Exception e) {
                Log.e(TAG,"Exception: ",e);
        }
    }

//    public static List<String> processFileUpload(String filename, InputStream inputStream){
//        checkDir();
//        try {
//            Log.i(TAG,"directory: "+directory.toString()+directory.exists());
//            File file = new File(filename);
//            Log.i(TAG,"directory: "+file);
//            OutputStream outputStream = new FileOutputStream(file);
//
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//
//            inputStream.close();
//            outputStream.close();
//            return shardBlob(filename,2);
//        }
//        catch (Exception e) {
//            Log.e(TAG,"Exception: ",e);
//        }
//        return null;
//    }


    public static List<String> processFileUpload(String filename){
        checkDir();
        try {
            Log.i(TAG,"directory: "+directory.toString()+ ",  Exists: " + directory.exists());
            return shardBlob(filename,2);
        }
        catch (Exception e) {
            Log.e(TAG,"Exception: ",e);
        }
        return null;
    }


    public static String reconstructShards(List<String> shardBlobIdList,String newName) throws IOException {
        checkDir();
        String reconsBlobID =  newName;
        File file = new File(directory,reconsBlobID);

        try(WritableByteChannel writer = Channels.newChannel(new FileOutputStream(file))){
            for (String blobId : shardBlobIdList) {
                InputStream inputStream =new FileInputStream(new File(directory,blobId));
                try(ReadableByteChannel reader = Channels.newChannel(inputStream )){
                    ByteBuffer bytes = ByteBuffer.allocate(1024);
                    while(reader.read(bytes)>0) {
                        bytes.flip();
                        writer.write(bytes);
                        bytes.clear();
                    }
                }
            }
        }

        Log.i(TAG,"reconstructShards completed : "+file.getName());

        return reconsBlobID;
    }

    private static List<String> shardBlob(String inFileName,int shardNumbers) throws IOException {
        checkDir();
        File inputFIle = new File(inFileName);
        FileInputStream inputStream = new FileInputStream(inputFIle);

        String shardPart = inputFIle.getName().split("\\.")[0];
        Log.i(TAG, "Shard PArt Name:" + shardPart);
        String shardPrefix = "shard_"+shardPart+"_"+String.valueOf(shardNumbers) ;
        long fileSize = inputFIle.length();//453013

        int chunkNumbers = shardNumbers; // number of shards to create
        int BUF_SIZE = 1024;
        long[] sizeList = new long[chunkNumbers];
        long sum  =0;
        for(int i =0;i<chunkNumbers-1;i++) {
            long shSize = fileSize/chunkNumbers;
            long extra = shSize%BUF_SIZE;
            long actualSize = shSize - extra;
            sizeList[i] = actualSize;
            sum +=sizeList[i];
        }

        sizeList[sizeList.length-1] = fileSize-sum;
        System.out.println("sizeList: "+Arrays.toString(sizeList));
        List<String> shardList = new ArrayList<>();
        try (ReadableByteChannel reader =  Channels.newChannel(inputStream)) {
            for(int i =0;i<chunkNumbers;i++) {
                String shardId = shardPrefix +"_"+  String.valueOf(i)+".shard";

                shardList.add(shardId);
                //Log.i(TAG,"ShardName: "+shardId);
                long shardSize = sizeList[i];
                long bytesRead =0;
                long bytesWritten = 0;


                OutputStream outS = new FileOutputStream(new File(directory,shardId));
                try(WritableByteChannel writer = Channels.newChannel(outS)){
                    ByteBuffer bytes = ByteBuffer.allocate(BUF_SIZE);

                    while (bytesWritten<shardSize) {
                        int curBytesRead = reader.read(bytes);

                        bytesRead+=curBytesRead;
                        bytes.flip();

                        bytesWritten = bytesWritten + writer.write(bytes);
                        //logger.info(" curBytesRead"+curBytesRead+",bytesWritten:"+bytesWritten);
                        bytes.clear();

                        if(curBytesRead<=0) {
                            Log.i(TAG,"file done bytesWritten"+bytesWritten);
                            break;
                        }
                    }
                }
                Log.i(TAG,"ShardName: "+shardId+", bytesRead: "+bytesRead+", bytesWritten: "+bytesWritten);
            }


        }
        return shardList;

    }
    private static void checkDir(){
//        File SDCardRoot = Environment.getExternalStorageDirectory();

//        directory = new File(SDCardRoot,"/pp_shard_files/");
        if (!directory.exists())
        {
            Log.i(TAG,"should create: "+directory.toString());
            directory.mkdir();
        }
    }
}
