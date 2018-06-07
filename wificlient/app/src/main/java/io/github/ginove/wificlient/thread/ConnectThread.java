package io.github.ginove.wificlient.thread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import io.github.ginove.wificlient.MainActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import   java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 连接线程
 * Created by syhuang on 2016/9/7.
 */
public class ConnectThread extends Thread {

    private final Socket socket;
    private Handler handler;
    private InputStream inputStream;
    private OutputStream outputStream;



    SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd-HH:mm:ss");
    Date curDate =  new Date(System.currentTimeMillis());

    public ConnectThread(Socket socket, Handler handler) {
        setName("ConnectThread");
        Log.i("ConnectThread", "ConnectThread");
        this.socket = socket;
        this.handler = handler;

    }

    @Override
    public void run() {
/*        if(activeConnect){
//            socket.c
        }*/
        if (socket == null) {
            return;
        }
        handler.sendEmptyMessage(MainActivity.DEVICE_CONNECTED);
        try {
            //获取数据流
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            while (true) {
                try {


                        String fileName = curDate+".jpg";
                        File fs = new File(Environment.getExternalStorageDirectory()+"/msc/" + fileName);
                        FileOutputStream outputStream =new FileOutputStream(fs);


                        byte[] bytesA = new byte[1024];
                        int length = 0;
                        int progress = 0;

                        while ((length = inputStream.read(bytesA, 0, bytesA.length)) != -1) {
                            outputStream.write(bytesA, 0, length);
                            outputStream.flush();
                            progress += length;
                        }
                        outputStream.close();
                        Message message = Message.obtain();
                        message.what = MainActivity.GET_MSG;
                        Bundle bundle = new Bundle();
                        bundle.putString("MSG", new String("收到文件"));
                        message.setData(bundle);
                        handler.sendMessage(message);

                }catch (IOException e) {
                    e.printStackTrace();
                }



            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 格式化文件大小
     *
     * @param length
     * @return
     */

}
