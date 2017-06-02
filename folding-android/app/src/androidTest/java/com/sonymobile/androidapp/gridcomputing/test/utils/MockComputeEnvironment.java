/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
//
//package com.sonymobile.androidapp.gridcomputing.test.utils;
//
//import android.content.Context;
//
//import com.sonymobile.androidapp.gridcomputing.service.ClientStateListener;
//import com.sonymobile.androidapp.gridcomputing.service.ComputeEnvironment;
//import com.sonymobile.androidapp.gridcomputing.service.JobExecutionListener;
//import com.sonymobile.androidapp.gridcomputing.service.StateMachine;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.Reader;
//import java.io.UnsupportedEncodingException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Semaphore;
//
//public class MockComputeEnvironment extends ComputeEnvironment {
//
//    private MyReader reader;
//    private MyOutputStream os;
//    private String lastOutput;
//
//    public MockComputeEnvironment(Context context, JobStateListener jobStateListener,
//            ClientStateListener clientStateListener, boolean testmode) {
//        super(context, jobStateListener, clientStateListener, testmode);
//
//        try {
//            ProcessBuilder pb = new ProcessBuilder("ls");
//            reader = new MyReader(new InputStreamReader(pb.start().getInputStream(), "UTF-8"));
//            os = new MyOutputStream();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected BufferedReader getReader() throws UnsupportedEncodingException {
//        return reader;
//    }
//
//    @Override
//    protected Process startProcess() throws IOException {
//
//        try {
//            super.startProcess();
//        } catch (IOException e) {
//
//        }
//
//        ProcessBuilder pb = new ProcessBuilder("ls");
//        return pb.start();
//    }
//
//    public MyReader getMyReader() {
//        return reader;
//    }
//
//    public String getLastOutput() {
//        return lastOutput;
//    }
//
//    public String getKeyJSON() {
//        return getJsonKeyReply();
//    }
//
//    public String getKillClientJSON() {
//        return getJsonKillClient(false);
//    }
//
//    public StateMachine getMyStateMachine() {
//        return getStateMachine();
//    }
//
//    @Override
//    protected OutputStream getProcessOutputStream() {
//        return os;
//    }
//
//    @Override
//    public void stopJob(boolean immediately) {
//        super.stopJob(immediately);
//        setRunning(false);
//    }
//
//    public class MyOutputStream extends OutputStream {
//
//        @Override
//        public void write(int oneByte) throws IOException {
//
//        }
//
//        @Override
//        public void write(byte[] buffer) throws IOException {
//            lastOutput = new String(buffer);
//        }
//
//    }
//
//    public class MyReader extends BufferedReader {
//
//        private final List<String> values;
//        private final Semaphore available;
//
//        public MyReader(Reader in) {
//            super(in);
//            values = new ArrayList<String>();
//            available = new Semaphore(0, true);
//        }
//
//        @Override
//        public String readLine() throws IOException {
//            try {
//                available.acquire();
//                String val = values.get(0);
//                values.remove(0);
//                return val;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        public void setReaderVal(String str) {
//            values.add(str);
//            available.release();
//        }
//    }
//}
