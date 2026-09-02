package org.telegram.tgnet;

import java.util.ArrayList;

/**
 * TLRPC - Official Type Language Definitions
 * Replicates DrKLO/Telegram TLRPC structures
 */
public class TLRPC {
    public static class TL_error extends TLObject {
        public int code;
        public String text;
    }

    public static class User extends TLObject {
        public long id;
        public String first_name;
        public String last_name;
        public String username;
        public String phone;
        public boolean premium;
        public boolean verified;
    }

    public static class Chat extends TLObject {
        public long id;
        public String title;
        public int participants_count;
        public boolean broadcast;
        public boolean megagroup;
    }

    public static class Dialog extends TLObject {
        public long id;
        public int top_message;
        public int read_inbox_max_id;
        public int read_outbox_max_id;
        public int unread_count;
        public boolean pinned;
        public int pinnedNum;
        public int folder_id;
    }

    public static class Message extends TLObject {
        public int id;
        public long from_id;
        public long peer_id;
        public int date;
        public String message;
        public boolean out;
        public boolean unread;
    }

    public static class messages_Dialogs extends TLObject {
        public ArrayList<Dialog> dialogs = new ArrayList<>();
        public ArrayList<Message> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int count;
    }

    public static class TL_auth_sendCode extends TLObject {
        public String phone_number;
        public int api_id;
        public String api_hash;
    }

    public static class TL_auth_sentCode extends TLObject {
        public String phone_code_hash;
        public int timeout;
    }

    public static class TL_auth_signIn extends TLObject {
        public String phone_number;
        public String phone_code_hash;
        public String phone_code;
    }

    public static class TL_auth_checkPassword extends TLObject {
        public byte[] password_hash;
    }

    public static class TL_auth_authorization extends TLObject {
        public User user;
    }

    public static class TL_auth_logOut extends TLObject {
    }

    public static class TL_messages_getDialogs extends TLObject {
        public int offset_date;
        public int offset_id;
        public int limit;
        public int folder_id;
    }

    public static class TL_messages_getHistory extends TLObject {
        public long peer_id;
        public int offset_id;
        public int offset_date;
        public int add_offset;
        public int limit;
        public int max_id;
        public int min_id;
    }

    public static class TL_updates_getDifference extends TLObject {
        public int pts;
        public int pts_total_limit;
        public int date;
        public int qts;
    }
}
