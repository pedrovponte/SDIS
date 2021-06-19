public class ThreadSendMessages implements Runnable {
    private byte[] message;
    private ChannelController channel;

    
    public ThreadSendMessages(ChannelController channel, byte[] message) {
        this.message = message;
        this.channel = channel;
    }


    @Override
    public void run() {
        this.channel.sendMessage(this.message);
    }

    
}
