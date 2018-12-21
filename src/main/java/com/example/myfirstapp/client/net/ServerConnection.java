package com.example.myfirstapp.client.net;

import android.os.AsyncTask;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import com.example.myfirstapp.common.MsgType;
import com.example.myfirstapp.common.MessageSplitter;

/**
 * Manages all communication with the server. All operations are non-blocking.
 */
public class ServerConnection extends AsyncTask<Void, Void, String> implements Runnable {
    private final ByteBuffer msgFromServer = ByteBuffer.allocateDirect(2018);
    private final Queue<ByteBuffer> messagesToSend = new ArrayDeque<>();
    private final MessageSplitter msgSplitter = new MessageSplitter();
    private final List<CommunicationListener> listeners = new ArrayList<>();
    private InetSocketAddress serverAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private boolean connected;
    private volatile boolean timeToSend = false;
    String response;
    TextView textResponse;
    SelectionKey key2;

    /*
    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }
    */

    @Override
    public void run() {
    //
        try {
            System.out.println("In server run");
            initConnection();
            System.out.println("In server run after initConn\n");
            initSelector();
            System.out.println("In server run after initSel\n");

            while(connected || !messagesToSend.isEmpty()){
                System.out.println("In server run mess to send\n");
                if(selector == null)
                    System.out.println("Selector does not exist after anymore.");
                else
                    System.out.println("Selector still exist after");
                if(timeToSend){
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    timeToSend = false;
                }
                System.out.println("In server before select");
                selector.select();
                System.out.println("In server after select");
                for(SelectionKey key : selector.selectedKeys()){
                    selector.selectedKeys().remove(key);
                    if(!key.isValid()){
                        continue;
                    }
                    if(key.isConnectable()){
                        System.out.println("In server after select connect");
                        completeConnection(key);
                    } else if(key.isReadable()){
                        System.out.println("In server after select read");
                        //recvFromServer(key);
                        key2=key;
                        doInBackground();
                    } else if(key.isWritable()){
                        System.out.println("In server after select write");
                        try {
                        sendToServer(key);
                        } catch(IOException ioex) {
                            System.err.println("COULD NOT SEND REQUEST TO SERVER, TRY AGAIN");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("LOST CONNECTION");
        } try {
            doDisconnect();
        } catch(IOException ex){
            System.err.println("COULD NOT DISCONNECT, WILL LEAVE UNGRACEFULLY!");
        }
    }

    /**
     * Creates a new instance and connects to the specified server. Also starts a listener thread
     * receiving broadcast messages from server.
     *
     * @param host             Host name or IP address of server.
     * @param port             Server's port number.
     */
    public void connect(String host, int port, TextView view){
        System.out.println("IN CONNECT");
        textResponse = view;
       serverAddress = new InetSocketAddress(host, port);
        System.out.println("AFTER ADDRESS");
       new Thread(this).start();
    }
    
    private void initSelector() throws IOException{
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        if(selector == null)
            System.out.println("Selector does not exist after init..");
        else
            System.out.println("Selector exist after init!");
    }
    
    private void initConnection() throws IOException{
        System.out.println("IN INITCON\n");
        socketChannel = socketChannel.open();
        System.out.println("AFTER OPEN\n");
        socketChannel.configureBlocking(false);
        System.out.println("AFTER CONF\n");
        socketChannel.connect(serverAddress);
        System.out.println("AFTER CONN\n");
        connected = true;
    }
    
    private void completeConnection(SelectionKey key) throws IOException{
        socketChannel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);
        try{
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            notifyConnectionDone(remoteAddress);
        }catch(IOException ioe){
            notifyConnectionDone(serverAddress);
        }
    }
    
    /**
     * Closes the connection with the server and stops the broadcast listener thread.
     *
     * @throws IOException If failed to close socket.
     */
    public void disconnect() throws IOException {
        connected = false;
        sendMsg(MsgType.DISCONNECT.toString(), null);
    }
    
    private void doDisconnect() throws IOException{
        socketChannel.close();
        socketChannel.keyFor(selector).cancel();
        notifyDisconnectionDone();
    }

    /**
     * Sends the user's username to the server. That username will be prepended to all messages
     * originating from this client, until a new username is specified.
     *
     * @param username The current user's username.
     */
    public void sendUsername(String username) { System.out.println("In server, join username: " + username);
        sendMsg(MsgType.USER.toString(), username); System.out.println("In server, join");
    }
    
    public void sendPlay(String choice) {
        sendMsg(MsgType.PLAY.toString(), choice);
    }

    public void sendJoin() {
        sendMsg(MsgType.JOIN.toString());
    }

    public void sendMsg(String... parts) {
        StringJoiner joiner = new StringJoiner("##");
        for (String part : parts) {
            joiner.add(part);
        }
        System.out.println("IN sendMsg");
        String messageWithLengthHeader = MessageSplitter.prependLengthHeader(joiner.toString());
        synchronized (messagesToSend) {
            System.out.println("IN sendMsg, add to buffer");
            messagesToSend.add(ByteBuffer.wrap(messageWithLengthHeader.getBytes()));
        }
        timeToSend = true;
        System.out.println("IN sendMsg, before wakeup");
        if (selector == null)
            System.out.println("selector finns ej..");
        selector.wakeup();
        System.out.println("IN sendMsg, after wakeup");
    }
    
    private void sendToServer(SelectionKey key) throws IOException {
        ByteBuffer msg;
        synchronized (messagesToSend) {
            while ((msg = messagesToSend.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return;
                }
                messagesToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    @Override
    protected String doInBackground(Void... voids) {//}
    //private void recvFromServer(SelectionKey key) throws IOException {
        msgFromServer.clear();
        int numOfReadBytes = 0;
        try {
            numOfReadBytes = socketChannel.read(msgFromServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (numOfReadBytes == -1) {
            try {
                throw new IOException("LOST CONNECTION");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String recvdString = extractMessageFromBuffer();


        msgSplitter.appendRecvdString(recvdString);
        String msg = "";
        while (msgSplitter.hasNext()) {
            msg = msgSplitter.nextMsg();
            notifyMsgReceived(MessageSplitter.bodyOf(msg));
        }
        response = MessageSplitter.bodyOf(msg);
        System.out.println("in doInBack response: " + response);
        //return response;
        //textResponse.setText(response);
        return response;
    }


    @Override
    protected void onPostExecute(String result) {
        System.out.println("IN ON POST EXECUTE############################");
        textResponse.setText(response);
        super.onPostExecute(result);
    }
    private String extractMessageFromBuffer() {
        msgFromServer.flip();
        byte[] bytes = new byte[msgFromServer.remaining()];
        msgFromServer.get(bytes);
        return new String(bytes);
    }
    
    private void notifyConnectionDone(InetSocketAddress connectedAddress) {
        Executor pool = ForkJoinPool.commonPool();
        int j;
        final InetSocketAddress connectedAddress2 = connectedAddress;

        for (j =0; j < listeners.size(); j++) {
            final int i = j;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    listeners.get(i).connected(connectedAddress2);
                }
            });
        }
    }
    
    private void notifyDisconnectionDone() {
        int j;
        Executor pool = ForkJoinPool.commonPool();

        for (j =0; j < listeners.size(); j++) {
            final int i=j;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    listeners.get(i).disconnected();
                }
            });
        }
    }
    
    private void notifyMsgReceived(String msg) {
        final String msg2 = msg;
        int j;
        Executor pool = ForkJoinPool.commonPool();
        for (j =0; j < listeners.size(); j++) {
            final int i=j;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    listeners.get(i).recvdMsg(msg2);
                }
            });
        }
    }
    
    public void addCommunicationListener(CommunicationListener listener){
        listeners.add(listener);
    }


}
