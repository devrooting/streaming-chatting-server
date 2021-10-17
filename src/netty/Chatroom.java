package netty;

import io.netty.channel.Channel;

import java.util.HashMap;

public class Chatroom {


    public String streamer_id;
    public String streamer_nickname;
    public Channel streamer_channel;
    public long streamer_start_time;
    public HashMap<String, ChatClient> clients;

    public Chatroom() {
        clients = new HashMap<String, ChatClient>();
    }

    public void setStreamer_channel(Channel streamer_channel) {
        this.streamer_channel = streamer_channel;
    }

    public void setStreamer_id(String streamer_id) {
        this.streamer_id = streamer_id;
    }

    public void setClients(HashMap<String, ChatClient> clients) {
        this.clients = clients;
    }

    public void setStreamer_nickname(String streamer_nickname) {
        this.streamer_nickname = streamer_nickname;
    }

    public long getStreamer_start_time() {
        return streamer_start_time;
    }

    public void setStreamer_start_time(long streamer_start_time) {
        this.streamer_start_time = streamer_start_time;
    }


    public Channel getStreamer_channel() {
        return streamer_channel;
    }

    public String getStreamer_id() {
        return streamer_id;
    }

    public HashMap<String, ChatClient> getClients() {

        return clients;
    }

    public String getStreamer_nickname() {
        return streamer_nickname;
    }
}
