package netty;

import io.netty.channel.Channel;

public class ChatClient {

    String user_id;
    String nickname;
    Channel user_channel;

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setUser_channel(Channel user_channel) {
        this.user_channel = user_channel;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getNickname() {
        return nickname;
    }

    public Channel getUser_channel() {
        return user_channel;
    }


}
