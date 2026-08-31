/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.tgnet;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.json.TLJsonBuilder;
import org.telegram.tgnet.json.TLJsonParser;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_aicompose;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.tgnet.tl.TL_communities;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.tgnet.tl.legacy.TL_legacy_message;
import org.telegram.tgnet.tl.TL_payments;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stats;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.Components.poll.PollAttachedMediaPack;
import org.telegram.ui.Stories.MessageMediaStoryFull;
import org.telegram.ui.Stories.MessageMediaStoryFull_old;
import org.telegram.ui.community.CommunityUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class TLRPC {

    //public static final int MESSAGE_FLAG_UNREAD             = 0x00000001;
    //public static final int MESSAGE_FLAG_OUT                = 0x00000002;
    public static final int MESSAGE_FLAG_FWD                = 0x00000004;
    public static final int MESSAGE_FLAG_REPLY              = 0x00000008;
    //public static final int MESSAGE_FLAG_MENTION            = 0x00000010;
    //public static final int MESSAGE_FLAG_CONTENT_UNREAD     = 0x00000020;
    public static final int MESSAGE_FLAG_HAS_MARKUP         = 0x00000040;
    public static final int MESSAGE_FLAG_HAS_ENTITIES       = 0x00000080;
    public static final int MESSAGE_FLAG_HAS_FROM_ID        = 0x00000100;
    public static final int MESSAGE_FLAG_HAS_MEDIA          = 0x00000200;
    public static final int MESSAGE_FLAG_HAS_VIEWS          = 0x00000400;
    public static final int MESSAGE_FLAG_HAS_BOT_ID         = 0x00000800;
    public static final int MESSAGE_FLAG_EDITED             = 0x00008000;

    public static final int LAYER = 229;

    public static abstract class EmailVerifyPurpose extends TLObject {

        public static EmailVerifyPurpose TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            EmailVerifyPurpose result = null;
            switch (constructor) {
                case TL_emailVerifyPurposeLoginSetup.constructor:
                    result = new TL_emailVerifyPurposeLoginSetup();
                    break;
                case TL_emailVerifyPurposeLoginChange.constructor:
                    result = new TL_emailVerifyPurposeLoginChange();
                    break;
                case TL_emailVerifyPurposePassport.constructor:
                    result = new TL_emailVerifyPurposePassport();
                    break;
            }
            return TLdeserialize(EmailVerifyPurpose.class, result, stream, constructor, exception);
        }
    }

    public static class TL_emailVerifyPurposeLoginSetup extends EmailVerifyPurpose {
        public static final int constructor = 0x4345be73;

        public String phone_number;
        public String phone_code_hash;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            phone_code_hash = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(phone_code_hash);
        }
    }

    public static class TL_inputReplyToMonoForum extends InputReplyTo {
        public static final int constructor = 0x69d66c45;

        public void readParams(InputSerializedData stream, boolean exception) {
            monoforum_peer_id = InputPeer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            monoforum_peer_id.serializeToStream(stream);
        }
    }

    public static class TL_emailVerifyPurposeLoginChange extends EmailVerifyPurpose {
        public static final int constructor = 0x527d22eb;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_emailVerifyPurposePassport extends EmailVerifyPurpose {
        public static final int constructor = 0xbbf51685;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class EmailVerification extends TLObject {

        public static EmailVerification TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            EmailVerification result = null;
            switch (constructor) {
                case TL_emailVerificationCode.constructor:
                    result = new TL_emailVerificationCode();
                    break;
                case TL_emailVerificationGoogle.constructor:
                    result = new TL_emailVerificationGoogle();
                    break;
                case TL_emailVerificationApple.constructor:
                    result = new TL_emailVerificationApple();
                    break;
            }
            return TLdeserialize(EmailVerification.class, result, stream, constructor, exception);
        }
    }

    public static class TL_emailVerificationCode extends EmailVerification {
        public static final int constructor = 0x922e55a9;

        public String code;

        public void readParams(InputSerializedData stream, boolean exception) {
            code = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(code);
        }
    }

    public static class TL_emailVerificationGoogle extends EmailVerification {
        public static final int constructor = 0xdb909ec2;

        public String token;

        public void readParams(InputSerializedData stream, boolean exception) {
            token = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(token);
        }
    }

    public static class TL_emailVerificationApple extends EmailVerification {
        public static final int constructor = 0x96d074fd;

        public String token;

        public void readParams(InputSerializedData stream, boolean exception) {
            token = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(token);
        }
    }

    public static class TL_chatBannedRights extends TLObject {
        public static final int constructor = 0x9f120418;

        public int flags;
        public boolean view_messages;
        public boolean send_messages;
        public boolean send_media;
        public boolean send_stickers;
        public boolean send_gifs;
        public boolean send_games;
        public boolean send_inline;
        public boolean embed_links;
        public boolean send_polls;
        public boolean change_info;
        public boolean invite_users;
        public boolean pin_messages;
        public boolean manage_topics;
        public boolean send_photos;
        public boolean send_videos;
        public boolean send_roundvideos;
        public boolean send_audios;
        public boolean send_voices;
        public boolean send_docs;
        public boolean send_plain;
        public boolean edit_rank;
        public boolean send_reactions;
        public boolean manage_linked_peers;
        public int until_date;

        public static TL_chatBannedRights TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_chatBannedRights result = TL_chatBannedRights.constructor != constructor ? null : new TL_chatBannedRights();
            return TLdeserialize(TL_chatBannedRights.class, result, stream, constructor, exception);
        }

        public static TL_chatBannedRights clone(TL_chatBannedRights rights) {
            if (rights == null) return null;
            SerializedData data = new SerializedData(rights.getObjectSize());
            rights.serializeToStream(data);
            data = new SerializedData(data.toByteArray());
            return TLdeserialize(data, data.readInt32(false), false);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TL_chatBannedRights)) return false;
            return this.flags == ((TL_chatBannedRights) obj).flags;
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            view_messages = hasFlag(flags, FLAG_0);
            send_messages = hasFlag(flags, FLAG_1);
            send_media = hasFlag(flags, FLAG_2);
            send_stickers = hasFlag(flags, FLAG_3);
            send_gifs = hasFlag(flags, FLAG_4);
            send_games = hasFlag(flags, FLAG_5);
            send_inline = hasFlag(flags, FLAG_6);
            embed_links = hasFlag(flags, FLAG_7);
            send_polls = hasFlag(flags, FLAG_8);
            change_info = hasFlag(flags, FLAG_10);
            invite_users = hasFlag(flags, FLAG_15);
            pin_messages = hasFlag(flags, FLAG_17);
            manage_topics = hasFlag(flags, FLAG_18);
            send_photos = hasFlag(flags, FLAG_19);
            send_videos = hasFlag(flags, FLAG_20);
            send_roundvideos = hasFlag(flags, FLAG_21);
            send_audios = hasFlag(flags, FLAG_22);
            send_voices = hasFlag(flags, FLAG_23);
            send_docs = hasFlag(flags, FLAG_24);
            send_plain = hasFlag(flags, FLAG_25);
            edit_rank = hasFlag(flags, FLAG_26);
            send_reactions = hasFlag(flags, FLAG_27);
            manage_linked_peers = hasFlag(flags, FLAG_28);
            if (send_media) {
                send_photos = true;
                send_videos = true;
                send_roundvideos = true;
                send_audios = true;
                send_voices = true;
                send_docs = true;
            }
            until_date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            if (ApplicationLoader.isAndroidTestEnvironment()) {
                stream.writeInt32(flags);
                stream.writeInt32(until_date);
                return;
            }

            if (send_photos && send_videos && send_roundvideos && send_audios && send_voices && send_docs) {
                send_media = true;
            } else {
                send_media = false;
            }
            if (send_plain && send_media && send_stickers) {
                send_messages = true;
            } else {
                send_messages = false;
            }
            flags = setFlag(flags, FLAG_0, view_messages);
            flags = setFlag(flags, FLAG_1, send_messages);
            flags = setFlag(flags, FLAG_2, send_media);
            flags = setFlag(flags, FLAG_3, send_stickers);
            flags = setFlag(flags, FLAG_4, send_gifs);
            flags = setFlag(flags, FLAG_5, send_games);
            flags = setFlag(flags, FLAG_6, send_inline);
            flags = setFlag(flags, FLAG_7, embed_links);
            flags = setFlag(flags, FLAG_8, send_polls);
            flags = setFlag(flags, FLAG_10, change_info);
            flags = setFlag(flags, FLAG_15, invite_users);
            flags = setFlag(flags, FLAG_17, pin_messages);
            flags = setFlag(flags, FLAG_18, manage_topics);
            flags = setFlag(flags, FLAG_19, send_photos);
            flags = setFlag(flags, FLAG_20, send_videos);
            flags = setFlag(flags, FLAG_21, send_roundvideos);
            flags = setFlag(flags, FLAG_22, send_audios);
            flags = setFlag(flags, FLAG_23, send_voices);
            flags = setFlag(flags, FLAG_24, send_docs);
            flags = setFlag(flags, FLAG_25, send_plain);
            flags = setFlag(flags, FLAG_26, edit_rank);
            flags = setFlag(flags, FLAG_27, send_reactions);
            flags = setFlag(flags, FLAG_28, manage_linked_peers);
            stream.writeInt32(flags);
            stream.writeInt32(until_date);
        }
    }

    public static class TL_stickers_suggestedShortName extends TLObject {
        public static final int constructor = 0x85fea03f;

        public String short_name;

        public static TL_stickers_suggestedShortName TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_stickers_suggestedShortName result = TL_stickers_suggestedShortName.constructor != constructor ? null : new TL_stickers_suggestedShortName();
            return TLdeserialize(TL_stickers_suggestedShortName.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            short_name = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(short_name);
        }
    }

    public static abstract class DraftMessage extends TLObject {

        public int flags;
        public boolean no_webpage;
        public boolean invert_media;
        public InputReplyTo reply_to;
        public String message;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public InputMedia media;
        public SuggestedPost suggested_post;
        public int date;
        public long effect;
        public TL_iv.RichMessage rich_message;

        public static DraftMessage TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            DraftMessage result = null;
            switch (constructor) {
                case TL_draftMessageEmpty.constructor:
                    result = new TL_draftMessageEmpty();
                    break;
                case TL_draftMessageEmpty_layer81.constructor:
                    result = new TL_draftMessageEmpty_layer81();
                    break;
                case TL_draftMessage.constructor:
                    result = new TL_draftMessage();
                    break;
                case TL_draftMessage_layer226.constructor:
                    result = new TL_draftMessage_layer226();
                    break;
                case TL_draftMessage_layer205.constructor:
                    result = new TL_draftMessage_layer205();
                    break;
                case TL_draftMessage_layer181.constructor:
                    result = new TL_draftMessage_layer181();
                    break;
                case TL_draftMessage_layer165.constructor:
                    result = new TL_draftMessage_layer165();
                    break;
            }
            return TLdeserialize(DraftMessage.class, result, stream, constructor, exception);
        }
    }

    public static class TL_draftMessageEmpty extends DraftMessage {
        public static final int constructor = 0x1b0c841a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(date);
            }
        }
    }

    public static class TL_draftMessageEmpty_layer81 extends TL_draftMessageEmpty {
        public static final int constructor = 0xba4baec5;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_draftMessage extends DraftMessage {
        public static final int constructor = 0x60fe3294;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_7)) {
                effect = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post = SuggestedPost.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                rich_message = TL_iv.RichMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            flags = setFlag(flags, FLAG_8, suggested_post != null);
            flags = setFlag(flags, FLAG_9, rich_message != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt64(effect);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                rich_message.serializeToStream(stream);
            }
        }
    }

    public static class TL_draftMessage_layer226 extends TL_draftMessage {
        public static final int constructor = 0x96eaa5eb;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_7)) {
                effect = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post = SuggestedPost.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            flags = setFlag(flags, FLAG_8, suggested_post != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt64(effect);
            }
            if (hasFlag(flags, FLAG_8)) {
                suggested_post.serializeToStream(stream);
            }
        }
    }

    public static class TL_draftMessage_layer205 extends DraftMessage {
        public static final int constructor = 0x2d65321f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_7)) {
                effect = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt64(effect);
            }
        }
    }

    public static class TL_draftMessage_layer181 extends TL_draftMessage {
        public static final int constructor = 0x3fccf7ef;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            invert_media = hasFlag(flags, FLAG_6);
            if (hasFlag(flags, FLAG_4)) {
                reply_to = InputReplyTo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            flags = setFlag(flags, FLAG_6, invert_media);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_4)) {
                reply_to.serializeToStream(stream);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                media.serializeToStream(stream);
            }
            stream.writeInt32(date);
        }
    }

    public static class TL_draftMessage_layer165 extends TL_draftMessage {
        public static final int constructor = 0xfd8e711f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            no_webpage = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_0)) {
                TL_inputReplyToMessage reply_to = new TL_inputReplyToMessage();
                reply_to.flags |= 16;
                reply_to.reply_to_msg_id = stream.readInt32(exception);
                this.reply_to = reply_to;
            }
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, no_webpage);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(reply_to instanceof TLRPC.TL_inputReplyToMessage ? ((TL_inputReplyToMessage) reply_to).reply_to_msg_id : 0);
            }
            stream.writeString(message);
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, entities);
            }
            stream.writeInt32(date);
        }
    }

    public static abstract class ChatPhoto extends TLObject {

        public int flags;
        public boolean has_video;
        public FileLocation photo_small;
        public FileLocation photo_big;
        public byte[] stripped_thumb;
        public int dc_id;
        public long photo_id;
        public BitmapDrawable strippedBitmap;

        public static ChatPhoto TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(stream, constructor, exception, true);
        }

        public static ChatPhoto TLdeserialize(InputSerializedData stream, int constructor, boolean exception,  boolean allowStripedThumb) {
            ChatPhoto result = null;
            switch (constructor) {
                case 0x1c6e1c11:
                    result = new TL_chatPhoto();
                    break;
                case 0x475cdbd5:
                    result = new TL_chatPhoto_layer115();
                    break;
                case 0x37c1011c:
                    result = new TL_chatPhotoEmpty();
                    break;
                case 0x6153276a:
                    result = new TL_chatPhoto_layer97();
                    break;
                case 0xd20b9f3c:
                    result = new TL_chatPhoto_layer126();
                    break;
                case 0x4790ee05:
                    result = new TL_chatPhoto_layer127();
                    break;
            }
            return TLdeserialize(ChatPhoto.class, result, stream, constructor, exception);
        }
    }

    public static class TL_chatPhoto extends ChatPhoto {
        public static final int constructor = 0x1c6e1c11;

        public void readParams(InputSerializedData stream, boolean exception) {
            readParams(stream, exception, true);
        }

        public void readParams(InputSerializedData stream, boolean exception, boolean allowStripedThumbs) {
            flags = stream.readInt32(exception);
            has_video = hasFlag(flags, FLAG_0);
            photo_id = stream.readInt64(exception);
            if (hasFlag(flags, FLAG_1)) {
                stripped_thumb = stream.readByteArray(exception);
            }
            dc_id = stream.readInt32(exception);
            photo_small = new TL_fileLocationToBeDeprecated();
            photo_small.volume_id = -photo_id;
            photo_small.local_id = 'a';
            photo_big = new TL_fileLocationToBeDeprecated();
            photo_big.volume_id = -photo_id;
            photo_big.local_id = 'c';

            if (allowStripedThumbs && stripped_thumb != null) {
                try {
                    strippedBitmap = new BitmapDrawable(ImageLoader.getStrippedPhotoBitmap(stripped_thumb, "b"));
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, has_video);
            stream.writeInt32(flags);
            stream.writeInt64(photo_id);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeByteArray(stripped_thumb);
            }
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_chatPhoto_layer115 extends TL_chatPhoto {
        public static final int constructor = 0x475cdbd5;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_chatPhotoEmpty extends ChatPhoto {
        public static final int constructor = 0x37c1011c;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_chatPhoto_layer97 extends TL_chatPhoto {
        public static final int constructor = 0x6153276a;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
        }
    }

    public static class TL_chatPhoto_layer126 extends TL_chatPhoto {
        public static final int constructor = 0xd20b9f3c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            has_video = hasFlag(flags, FLAG_0);
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, has_video);
            stream.writeInt32(flags);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_chatPhoto_layer127 extends TL_chatPhoto {
        public static final int constructor = 0x4790ee05;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            has_video = hasFlag(flags, FLAG_0);
            photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_1)) {
                stripped_thumb = stream.readByteArray(exception);
                try {
                    strippedBitmap = new BitmapDrawable(ImageLoader.getStrippedPhotoBitmap(stripped_thumb, "b"));
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, has_video);
            stream.writeInt32(flags);
            photo_small.serializeToStream(stream);
            photo_big.serializeToStream(stream);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeByteArray(stripped_thumb);
            }
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_help_termsOfService extends TLObject {
        public static final int constructor = 0x780a0310;

        public int flags;
        public boolean popup;
        public TL_dataJSON id;
        public String text;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public int min_age_confirm;

        public static TL_help_termsOfService TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_help_termsOfService result = TL_help_termsOfService.constructor != constructor ? null : new TL_help_termsOfService();
            return TLdeserialize(TL_help_termsOfService.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            popup = hasFlag(flags, FLAG_0);
            id = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
            text = stream.readString(exception);
            entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_1)) {
                min_age_confirm = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, popup);
            stream.writeInt32(flags);
            id.serializeToStream(stream);
            stream.writeString(text);
            Vector.serialize(stream, entities);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(min_age_confirm);
            }
        }
    }

    public static class PaymentReceipt extends TLObject {
        public int flags;
        public int date;
        public long bot_id;
        public long provider_id;
        public String title;
        public String description;
        public WebDocument photo;
        public TL_invoice invoice;
        public TL_paymentRequestedInfo info;
        public TL_shippingOption shipping;
        public long tip_amount;
        public String currency;
        public long total_amount;
        public String credentials_title;
        public ArrayList<User> users = new ArrayList<>();
        public String transaction_id;

        public static PaymentReceipt TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PaymentReceipt result = null;
            switch (constructor) {
                case TL_payments_paymentReceipt.constructor:
                    result = new TL_payments_paymentReceipt();
                    break;
                case TL_payments_paymentReceiptStars.constructor:
                    result = new TL_payments_paymentReceiptStars();
                    break;
            }
            return TLdeserialize(PaymentReceipt.class, result, stream, constructor, exception);
        }
    }

    public static class TL_payments_paymentReceipt extends PaymentReceipt {
        public static final int constructor = 0x70c4fe03;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            date = stream.readInt32(exception);
            bot_id = stream.readInt64(exception);
            provider_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_2)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                info = TL_paymentRequestedInfo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                shipping = TL_shippingOption.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                tip_amount = stream.readInt64(exception);
            }
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            credentials_title = stream.readString(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(date);
            stream.writeInt64(bot_id);
            stream.writeInt64(provider_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_2)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                info.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                shipping.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt64(tip_amount);
            }
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(credentials_title);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_payments_paymentReceiptStars extends PaymentReceipt {
        public static final int constructor = 0xdabbf83a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            date = stream.readInt32(exception);
            bot_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_2)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            transaction_id = stream.readString(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(date);
            stream.writeInt64(bot_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_2)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(transaction_id);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class NotifyPeer extends TLObject {

        public static NotifyPeer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(NotifyPeer.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static NotifyPeer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_notifyForumTopic.constructor:
                    return new TL_notifyForumTopic();
                case TL_notifyBroadcasts.constructor:
                    return new TL_notifyBroadcasts();
                case TL_notifyChats.constructor:
                    return new TL_notifyChats();
                case TL_notifyUsers.constructor:
                    return new TL_notifyUsers();
                case TL_notifyPeer.constructor:
                    return new TL_notifyPeer();
                case TL_notifyCommunity.constructor:
                    return new TL_notifyCommunity();
                default:
                    return null;
            }
        }
    }

    public static class TL_notifyForumTopic extends NotifyPeer {
        public static final int constructor = 0x226e6308;

        public Peer peer;
        public int top_msg_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            top_msg_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
            stream.writeInt32(top_msg_id);
        }
    }

    public static class TL_notifyCommunity extends NotifyPeer {
        public static final int constructor = 0xBE376999;

        public long community_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            community_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(community_id);
        }
    }

    public static class TL_notifyBroadcasts extends NotifyPeer {
        public static final int constructor = 0xd612e8ef;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notifyChats extends NotifyPeer {
        public static final int constructor = 0xc007cec3;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notifyUsers extends NotifyPeer {
        public static final int constructor = 0xb4c83b4c;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notifyPeer extends NotifyPeer {
        public static final int constructor = 0x9fd40bd8;

        public Peer peer;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
        }
    }

    public static class TL_emojiKeywordsDifference extends TLObject {
        public static final int constructor = 0x5cc761bd;

        public String lang_code;
        public int from_version;
        public int version;
        public ArrayList<EmojiKeyword> keywords = new ArrayList<>();

        public static TL_emojiKeywordsDifference TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_emojiKeywordsDifference result = TL_emojiKeywordsDifference.constructor != constructor ? null : new TL_emojiKeywordsDifference();
            return TLdeserialize(TL_emojiKeywordsDifference.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            lang_code = stream.readString(exception);
            from_version = stream.readInt32(exception);
            version = stream.readInt32(exception);
            keywords = Vector.deserialize(stream, EmojiKeyword::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(lang_code);
            stream.writeInt32(from_version);
            stream.writeInt32(version);
            Vector.serialize(stream, keywords);
        }
    }

    public static abstract class messages_SentEncryptedMessage extends TLObject {
        public int date;
        public EncryptedFile file;

        public static messages_SentEncryptedMessage TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_SentEncryptedMessage result = null;
            switch (constructor) {
                case 0x560f8935:
                    result = new TL_messages_sentEncryptedMessage();
                    break;
                case 0x9493ff32:
                    result = new TL_messages_sentEncryptedFile();
                    break;
            }
            return TLdeserialize(messages_SentEncryptedMessage.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_sentEncryptedMessage extends messages_SentEncryptedMessage {
        public static final int constructor = 0x560f8935;

        public void readParams(InputSerializedData stream, boolean exception) {
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(date);
        }
    }

    public static class TL_messages_sentEncryptedFile extends messages_SentEncryptedMessage {
        public static final int constructor = 0x9493ff32;

        public void readParams(InputSerializedData stream, boolean exception) {
            date = stream.readInt32(exception);
            file = EncryptedFile.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(date);
            file.serializeToStream(stream);
        }
    }

    public static class TL_premiumSubscriptionOption extends TLObject {
        public static final int constructor = 0x5f2d1df2;

        public int flags;
        public boolean current;
        public String transaction;
        public boolean can_purchase_upgrade;
        public int months;
        public String currency;
        public long amount;
        public String bot_url;
        public String store_product;

        public static TL_premiumSubscriptionOption TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_premiumSubscriptionOption result = null;
            switch (constructor) {
                case 0x5f2d1df2:
                    result = new TL_premiumSubscriptionOption();
                    break;
                case 0xb6f11ebe:
                    result = new TL_premiumSubscriptionOption_layer151();
                    break;
            }
            return TLdeserialize(TL_premiumSubscriptionOption.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            current = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_3)) {
                transaction = stream.readString(exception);
            }
            can_purchase_upgrade = hasFlag(flags, FLAG_2);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            bot_url = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, current);
            flags = setFlag(flags, FLAG_2, can_purchase_upgrade);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(transaction);
            }
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            stream.writeString(bot_url);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_premiumSubscriptionOption_layer151 extends TL_premiumSubscriptionOption {
        public static final int constructor = 0xb6f11ebe;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            bot_url = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            stream.writeString(bot_url);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_premiumGiftOption extends TLObject {
        public static final int constructor = 0x79c059f7;

        public int flags;
        public int months;
        public String currency;
        public long amount;
        public String bot_url;
        public String store_product;

        public static TL_premiumGiftOption TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_premiumGiftOption result = null;
            switch (constructor) {
                case TL_premiumGiftOption.constructor:
                    result = new TL_premiumGiftOption();
                    break;
                case TL_premiumGiftOption_layer199.constructor:
                    result = new TL_premiumGiftOption_layer199();
                    break;
            }
            return TLdeserialize(TL_premiumGiftOption.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            if (hasFlag(flags, FLAG_1)) {
                bot_url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(bot_url);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_premiumGiftOption_layer199 extends TL_premiumGiftOption {
        public static final int constructor = 0x74c34319;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            months = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
            bot_url = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                store_product = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(months);
            stream.writeString(currency);
            stream.writeInt64(amount);
            stream.writeString(bot_url);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(store_product);
            }
        }
    }

    public static class TL_error extends TLObject {
        public static final int constructor = 0xc4b9f9bb;

        public int code;
        public String text;

        public static TL_error TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_error result = TL_error.constructor != constructor ? null : new TL_error();
            return TLdeserialize(TL_error.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            code = stream.readInt32(exception);
            text = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(code);
            stream.writeString(text);
        }
    }

    public static abstract class UrlAuthResult extends TLObject {

        public static UrlAuthResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            UrlAuthResult result = null;
            switch (constructor) {
                case TL_urlAuthResultDefault.constructor:
                    result = new TL_urlAuthResultDefault();
                    break;
                case TL_urlAuthResultRequest.constructor:
                    result = new TL_urlAuthResultRequest();
                    break;
                case TL_urlAuthResultAccepted.constructor:
                    result = new TL_urlAuthResultAccepted();
                    break;
            }
            return TLdeserialize(UrlAuthResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_urlAuthResultDefault extends UrlAuthResult {
        public static final int constructor = 0xa9d6db1f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_checkUrlAuthMatchCode extends TLMethod<Bool> {
        public static final int constructor = 0xC9A47B0B;

        public String url;
        public String match_code;

        @Override
        public Bool deserializeResponseT(InputSerializedData stream, int constructor, boolean exception) {
            return Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeString(match_code);
        }
    }

    public static class TL_urlAuthResultRequest extends UrlAuthResult {
        public static final int constructor = 0x3cd623ec;

        public int flags;
        public boolean request_write_access;
        public boolean request_phone_number;
        public boolean match_codes_first;
        public boolean is_app;
        public User bot;
        public String domain;
        public String browser;
        public String platform;
        public String ip;
        public String region;
        public ArrayList<String> match_codes = new ArrayList<>();
        public long user_id_hint;
        public String verified_app_name;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            request_write_access = hasFlag(flags, FLAG_0);
            request_phone_number = hasFlag(flags, FLAG_1);
            match_codes_first = hasFlag(flags, FLAG_5);
            is_app = hasFlag(flags, FLAG_6);
            bot = User.TLdeserialize(stream, stream.readInt32(exception), exception);
            domain = stream.readString(exception);
            if (hasFlag(flags, FLAG_2)) {
                browser = stream.readString(exception);
                platform = stream.readString(exception);
                ip = stream.readString(exception);
                region = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                match_codes = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                user_id_hint = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                verified_app_name = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, request_write_access);
            flags = setFlag(flags, FLAG_1, request_phone_number);
            flags = setFlag(flags, FLAG_5, match_codes_first);
            flags = setFlag(flags, FLAG_6, is_app);
            stream.writeInt32(flags);
            bot.serializeToStream(stream);
            stream.writeString(domain);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(browser);
                stream.writeString(platform);
                stream.writeString(ip);
                stream.writeString(region);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serializeString(stream, match_codes);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(user_id_hint);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeString(verified_app_name);
            }
        }
    }

    public static class TL_urlAuthResultAccepted extends UrlAuthResult {
        public static final int constructor = 0x623a8fa0;

        public int flags;
        public String url;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                url = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(url);
            }
        }
    }

    public static class TL_messages_chatFull extends TLObject {
        public static final int constructor = 0xe5d7d19c;

        public ChatFull full_chat;
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_messages_chatFull TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_chatFull result = TL_messages_chatFull.constructor != constructor ? null : new TL_messages_chatFull();
            return TLdeserialize(TL_messages_chatFull.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            full_chat = ChatFull.TLdeserialize(stream, stream.readInt32(exception), exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            full_chat.serializeToStream(stream);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class DocumentAttribute extends TLObject {
        public String alt;
        public InputStickerSet stickerset;
        public double duration;
        public int flags;
        public TL_maskCoords mask_coords;
        public boolean round_message;
        public boolean supports_streaming;
        public String file_name;
        public int w;
        public int h;
        public boolean mask;
        public String title;
        public String performer;
        public boolean voice;
        public byte[] waveform;
        public int preload_prefix_size;
        public double video_start_ts;
        public boolean nosound;
        public String video_codec;

        public static DocumentAttribute TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            DocumentAttribute result = null;
            switch (constructor) {
                case 0x3a556302:
                    result = new TL_documentAttributeSticker_layer55();
                    break;
                case 0xef02ce6:
                    result = new TL_documentAttributeVideo_layer159();
                    break;
                case 0x51448e5:
                    result = new TL_documentAttributeAudio_old();
                    break;
                case 0x6319d612:
                    result = new TL_documentAttributeSticker();
                    break;
                case 0x11b58939:
                    result = new TL_documentAttributeAnimated();
                    break;
                case 0x15590068:
                    result = new TL_documentAttributeFilename();
                    break;
                case TL_documentAttributeVideo.constructor:
                    result = new TL_documentAttributeVideo();
                    break;
                case TL_documentAttributeVideo_layer187.constructor:
                    result = new TL_documentAttributeVideo_layer187();
                    break;
                case TL_documentAttributeVideo_layer184.constructor:
                    result = new TL_documentAttributeVideo_layer184();
                    break;
                case 0x5910cccb:
                    result = new TL_documentAttributeVideo_layer65();
                    break;
                case 0xded218e0:
                    result = new TL_documentAttributeAudio_layer45();
                    break;
                case 0xfb0a5727:
                    result = new TL_documentAttributeSticker_old();
                    break;
                case 0x9801d2f7:
                    result = new TL_documentAttributeHasStickers();
                    break;
                case 0x994c9882:
                    result = new TL_documentAttributeSticker_old2();
                    break;
                case 0x6c37c15c:
                    result = new TL_documentAttributeImageSize();
                    break;
                case 0x9852f9c6:
                    result = new TL_documentAttributeAudio();
                    break;
                case 0xfd149899:
                    result = new TL_documentAttributeCustomEmoji();
                    break;
            }
            return TLdeserialize(DocumentAttribute.class, result, stream, constructor, exception);
        }
    }

    public static class TL_documentAttributeSticker_layer55 extends TL_documentAttributeSticker {
        public static final int constructor = 0x3a556302;

        public void readParams(InputSerializedData stream, boolean exception) {
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
        }
    }

    public static class TL_documentAttributeAudio_old extends TL_documentAttributeAudio {
        public static final int constructor = 0x51448e5;

        public void readParams(InputSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) duration);
        }
    }

    public static class TL_documentAttributeSticker extends DocumentAttribute {
        public static final int constructor = 0x6319d612;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            mask = hasFlag(flags, FLAG_1);
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords = TL_maskCoords.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, mask);
            stream.writeInt32(flags);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords.serializeToStream(stream);
            }
        }
    }

    public static class TL_documentAttributeAnimated extends DocumentAttribute {
        public static final int constructor = 0x11b58939;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeFilename extends DocumentAttribute {
        public static final int constructor = 0x15590068;

        public void readParams(InputSerializedData stream, boolean exception) {
            file_name = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(file_name);
        }
    }

    public static class TL_documentAttributeVideo extends DocumentAttribute {
        public static final int constructor = 0x43c57c48;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            nosound = hasFlag(flags, FLAG_3);
            duration = stream.readDouble(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                preload_prefix_size = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                video_start_ts = stream.readDouble(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                video_codec = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            flags = setFlag(flags, FLAG_3, nosound);
            stream.writeInt32(flags);
            stream.writeDouble(duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(preload_prefix_size);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeDouble(video_start_ts);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeString(video_codec);
            }
        }
    }

    public static class TL_documentAttributeVideo_layer187 extends TL_documentAttributeVideo {
        public static final int constructor = 0x17399fad;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            nosound = hasFlag(flags, FLAG_3);
            duration = stream.readDouble(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                preload_prefix_size = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                video_start_ts = stream.readDouble(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            flags = setFlag(flags, FLAG_3, nosound);
            stream.writeInt32(flags);
            stream.writeDouble(duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(preload_prefix_size);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeDouble(video_start_ts);
            }
        }
    }

    public static class TL_documentAttributeVideo_layer184 extends TL_documentAttributeVideo {
        public static final int constructor = 0xd38ff1c2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            nosound = hasFlag(flags, FLAG_3);
            duration = stream.readDouble(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                preload_prefix_size = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            flags = setFlag(flags, FLAG_3, nosound);
            stream.writeInt32(flags);
            stream.writeDouble(duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(preload_prefix_size);
            }
        }
    }

    public static class TL_documentAttributeVideo_layer159 extends TL_documentAttributeVideo {
        public static final int constructor = 0xef02ce6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            round_message = hasFlag(flags, FLAG_0);
            supports_streaming = hasFlag(flags, FLAG_1);
            duration = stream.readInt32(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, round_message);
            flags = setFlag(flags, FLAG_1, supports_streaming);
            stream.writeInt32(flags);
            stream.writeInt32((int) duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeVideo_layer65 extends TL_documentAttributeVideo {
        public static final int constructor = 0x5910cccb;

        public void readParams(InputSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) duration);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeAudio_layer45 extends TL_documentAttributeAudio {
        public static final int constructor = 0xded218e0;

        public void readParams(InputSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
            title = stream.readString(exception);
            performer = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) duration);
            stream.writeString(title);
            stream.writeString(performer);
        }
    }

    public static class TL_documentAttributeSticker_old extends TL_documentAttributeSticker {
        public static final int constructor = 0xfb0a5727;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeHasStickers extends DocumentAttribute {
        public static final int constructor = 0x9801d2f7;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeSticker_old2 extends TL_documentAttributeSticker {
        public static final int constructor = 0x994c9882;

        public void readParams(InputSerializedData stream, boolean exception) {
            alt = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(alt);
        }
    }

    public static class TL_documentAttributeImageSize extends DocumentAttribute {
        public static final int constructor = 0x6c37c15c;

        public void readParams(InputSerializedData stream, boolean exception) {
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeAudio extends DocumentAttribute {
        public static final int constructor = 0x9852f9c6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            voice = hasFlag(flags, FLAG_10);
            duration = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                performer = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                waveform = stream.readByteArray(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_10, voice);
            stream.writeInt32(flags);
            stream.writeInt32((int) duration);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(performer);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeByteArray(waveform);
            }
        }
    }

    public static class TL_documentAttributeCustomEmoji extends DocumentAttribute {
        public static final int constructor = 0xfd149899;

        public boolean free;
        public boolean text_color;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            free = hasFlag(flags, FLAG_0);
            text_color = hasFlag(flags, FLAG_1);
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, free);
            flags = setFlag(flags, FLAG_1, text_color);
            stream.writeInt32(flags);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
        }
    }

    public static class TL_statsURL extends TLObject {
        public static final int constructor = 0x47a971e0;

        public String url;

        public static TL_statsURL TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_statsURL result = TL_statsURL.constructor != constructor ? null : new TL_statsURL();
            return TLdeserialize(TL_statsURL.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
        }
    }

    public static class TL_popularContact extends TLObject {
        public static final int constructor = 0x5ce14175;

        public long client_id;
        public int importers;

        public static TL_popularContact TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_popularContact result = TL_popularContact.constructor != constructor ? null : new TL_popularContact();
            return TLdeserialize(TL_popularContact.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            client_id = stream.readInt64(exception);
            importers = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(client_id);
            stream.writeInt32(importers);
        }
    }

    public static class TL_messages_botCallbackAnswer extends TLObject {
        public static final int constructor = 0x36585ea4;

        public int flags;
        public boolean alert;
        public boolean has_url;
        public boolean native_ui;
        public String message;
        public String url;
        public int cache_time;

        public static TL_messages_botCallbackAnswer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_botCallbackAnswer result = TL_messages_botCallbackAnswer.constructor != constructor ? null : new TL_messages_botCallbackAnswer();
            return TLdeserialize(TL_messages_botCallbackAnswer.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            alert = hasFlag(flags, FLAG_1);
            has_url = hasFlag(flags, FLAG_3);
            native_ui = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_0)) {
                message = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                url = stream.readString(exception);
            }
            cache_time = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, alert);
            flags = setFlag(flags, FLAG_3, has_url);
            flags = setFlag(flags, FLAG_4, native_ui);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(message);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(url);
            }
            stream.writeInt32(cache_time);
        }
    }

    public static class TL_contactStatus extends TLObject {
        public static final int constructor = 0x16d9703b;

        public long user_id;
        public UserStatus status;

        public static TL_contactStatus TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_contactStatus result = TL_contactStatus.constructor != constructor ? null : new TL_contactStatus();
            return TLdeserialize(TL_contactStatus.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
            status = UserStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
            status.serializeToStream(stream);
        }
    }

    public static abstract class GroupCall extends TLObject {

        public int flags;
        public boolean join_muted;
        public boolean can_change_join_muted;
        public boolean join_date_asc;
        public boolean schedule_start_subscribed;
        public boolean can_start_video;
        public boolean record_video_active;
        public boolean conference;
        public boolean rtmp_stream;
        public boolean listeners_hidden;
        public boolean creator;
        public long id;
        public long access_hash;
        public int participants_count;
        public String title;
        public int stream_dc_id;
        public int record_start_date;
        public int schedule_date;
        public int unmuted_video_count;
        public int unmuted_video_limit;
        public int version;
        public int duration;
        public long conference_from_call;
        public String invite_link;
        public boolean messages_enabled;
        public boolean can_change_messages_enabled;
        public boolean min;
        public long send_paid_messages_stars;
        public TLRPC.Peer default_send_as;

        public static GroupCall TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(GroupCall.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static GroupCall fromConstructor(int constructor) {
            switch (constructor) {
                case TL_groupCallDiscarded.constructor:     return new TL_groupCallDiscarded();
                case TL_groupCall.constructor:              return new TL_groupCall();
                case TL_groupCall_layer216.constructor:     return new TL_groupCall_layer216();
                case TL_groupCall_layer201_2.constructor:   return new TL_groupCall_layer201_2();
                case TL_groupCall_layer201.constructor:     return new TL_groupCall_layer201();
            }
            return null;
        }
    }

    public static class TL_groupCallDiscarded extends GroupCall {
        public static final int constructor = 0x7780bcb4;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            duration = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(duration);
        }
    }

    public static class TL_groupCall extends GroupCall {
        public static final int constructor = 0xefb2b617;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            creator = hasFlag(flags, FLAG_15);
            messages_enabled = hasFlag(flags, FLAG_17);
            can_change_messages_enabled = hasFlag(flags, FLAG_18);
            min = hasFlag(flags, FLAG_19);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_16)) {
                invite_link = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_20)) {
                send_paid_messages_stars = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_21)) {
                default_send_as = TLRPC.Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            flags = setFlag(flags, FLAG_15, creator);
            flags = setFlag(flags, FLAG_17, messages_enabled);
            flags = setFlag(flags, FLAG_18, can_change_messages_enabled);
            flags = setFlag(flags, FLAG_19, min);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(invite_link);
            }
            if (hasFlag(flags, FLAG_20)) {
                stream.writeInt64(send_paid_messages_stars);
            }
            if (hasFlag(flags, FLAG_21)) {
                default_send_as.serializeToStream(stream);
            }
        }
    }

    public static class TL_groupCall_layer216 extends TL_groupCall {
        public static final int constructor = 0x553b0ba1;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            creator = hasFlag(flags, FLAG_15);
            messages_enabled = hasFlag(flags, FLAG_17);
            can_change_messages_enabled = hasFlag(flags, FLAG_18);
            min = hasFlag(flags, FLAG_19);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_16)) {
                invite_link = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            flags = setFlag(flags, FLAG_15, creator);
            flags = setFlag(flags, FLAG_17, messages_enabled);
            flags = setFlag(flags, FLAG_18, can_change_messages_enabled);
            flags = setFlag(flags, FLAG_19, min);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(invite_link);
            }
        }
    }

    public static class TL_groupCall_layer201_2 extends TL_groupCall {
        public static final int constructor = 0xd597650c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            creator = hasFlag(flags, FLAG_15);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_16)) {
                invite_link = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            flags = setFlag(flags, FLAG_15, creator);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(invite_link);
            }
        }
    }

    public static class TL_groupCall_layer201 extends TL_groupCall {
        public static final int constructor = 0xcdf8d3e3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            join_muted = hasFlag(flags, FLAG_1);
            can_change_join_muted = hasFlag(flags, FLAG_2);
            join_date_asc = hasFlag(flags, FLAG_6);
            schedule_start_subscribed = hasFlag(flags, FLAG_8);
            can_start_video = hasFlag(flags, FLAG_9);
            record_video_active = hasFlag(flags, FLAG_11);
            rtmp_stream = hasFlag(flags, FLAG_12);
            listeners_hidden = hasFlag(flags, FLAG_13);
            conference = hasFlag(flags, FLAG_14);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream_dc_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                record_start_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                schedule_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                unmuted_video_count = stream.readInt32(exception);
            }
            unmuted_video_limit = stream.readInt32(exception);
            version = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_14)) {
                conference_from_call = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, join_muted);
            flags = setFlag(flags, FLAG_2, can_change_join_muted);
            flags = setFlag(flags, FLAG_6, join_date_asc);
            flags = setFlag(flags, FLAG_8, schedule_start_subscribed);
            flags = setFlag(flags, FLAG_9, can_start_video);
            flags = setFlag(flags, FLAG_11, record_video_active);
            flags = setFlag(flags, FLAG_12, rtmp_stream);
            flags = setFlag(flags, FLAG_13, listeners_hidden);
            flags = setFlag(flags, FLAG_14, conference);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(stream_dc_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(record_start_date);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeInt32(schedule_date);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(unmuted_video_count);
            }
            stream.writeInt32(unmuted_video_limit);
            stream.writeInt32(version);
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(conference_from_call);
            }
        }
    }

    public static class TL_channelBannedRights_layer92 extends TLObject {
        public static final int constructor = 0x58cf4249;

        public int flags;
        public boolean view_messages;
        public boolean send_messages;
        public boolean send_media;
        public boolean send_stickers;
        public boolean send_gifs;
        public boolean send_games;
        public boolean send_inline;
        public boolean embed_links;
        public int until_date;

        public static TL_channelBannedRights_layer92 TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channelBannedRights_layer92 result = TL_channelBannedRights_layer92.constructor != constructor ? null : new TL_channelBannedRights_layer92();
            return TLdeserialize(TL_channelBannedRights_layer92.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            view_messages = hasFlag(flags, FLAG_0);
            send_messages = hasFlag(flags, FLAG_1);
            send_media = hasFlag(flags, FLAG_2);
            send_stickers = hasFlag(flags, FLAG_3);
            send_gifs = hasFlag(flags, FLAG_4);
            send_games = hasFlag(flags, FLAG_5);
            send_inline = hasFlag(flags, FLAG_6);
            embed_links = hasFlag(flags, FLAG_7);
            until_date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, view_messages);
            flags = setFlag(flags, FLAG_1, send_messages);
            flags = setFlag(flags, FLAG_2, send_media);
            flags = setFlag(flags, FLAG_3, send_stickers);
            flags = setFlag(flags, FLAG_4, send_gifs);
            flags = setFlag(flags, FLAG_5, send_games);
            flags = setFlag(flags, FLAG_6, send_inline);
            flags = setFlag(flags, FLAG_7, embed_links);
            stream.writeInt32(flags);
            stream.writeInt32(until_date);
        }
    }

    public static abstract class DialogPeer extends TLObject {

        public static DialogPeer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(DialogPeer.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static DialogPeer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_dialogPeer.constructor:
                    return new TL_dialogPeer();
                case TL_dialogPeerFolder.constructor:
                    return new TL_dialogPeerFolder();
                case TL_dialogPeerCommunity.constructor:
                    return new TL_dialogPeerCommunity();
                default:
                    return null;
            }
        }
    }

    public static class TL_dialogPeer extends DialogPeer {
        public static final int constructor = 0xe56dbf05;

        public Peer peer;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
        }
    }

    public static class TL_dialogPeerFolder extends DialogPeer {
        public static final int constructor = 0x514519e2;

        public int folder_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            folder_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(folder_id);
        }
    }

    public static class TL_dialogPeerCommunity extends DialogPeer {
        public static final int constructor = 0x2F65C8E4;

        public long community_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            community_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(community_id);
        }
    }

    public static abstract class MessagePeerReaction extends TLObject {

        public int flags;
        public boolean big;
        public boolean unread;
        public Peer peer_id;
        public Reaction reaction;
        public int date;
        public boolean dateIsSeen; //custom

        public static MessagePeerReaction TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            MessagePeerReaction result = null;
            switch (constructor) {
                case 0x8c79b63c:
                    result = new TL_messagePeerReaction();
                    break;
                case 0xb156fe9c:
                    result = new TL_messagePeerReaction_layer154();
                    break;
                case 0x51b67eff:
                    result = new TL_messagePeerReaction_layer144();
                    break;
                case 0x932844fa:
                    result = new TL_messagePeerReaction_layer137();
                    break;
            }
            return TLdeserialize(MessagePeerReaction.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messagePeerReaction extends MessagePeerReaction {
        public static final int constructor = 0x8c79b63c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            big = hasFlag(flags, FLAG_0);
            unread = hasFlag(flags, FLAG_1);
            peer_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            date = stream.readInt32(exception);
            reaction = Reaction.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, big);
            flags = setFlag(flags, FLAG_1, unread);
            stream.writeInt32(flags);
            peer_id.serializeToStream(stream);
            stream.writeInt32(date);
            reaction.serializeToStream(stream);
        }
    }

    public static class TL_messagePeerReaction_layer154 extends MessagePeerReaction {
        public static final int constructor = 0xb156fe9c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            big = hasFlag(flags, FLAG_0);
            unread = hasFlag(flags, FLAG_1);
            peer_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            reaction = Reaction.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, big);
            flags = setFlag(flags, FLAG_1, unread);
            stream.writeInt32(flags);
            peer_id.serializeToStream(stream);
            reaction.serializeToStream(stream);
        }
    }

    public static class TL_messagePeerReaction_layer144 extends MessagePeerReaction {
        public static final int constructor = 0x51b67eff;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            big = hasFlag(flags, FLAG_0);
            unread = hasFlag(flags, FLAG_1);
            peer_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            reaction = new TL_reactionEmoji();
            ((TL_reactionEmoji) reaction).emoticon = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, big);
            flags = setFlag(flags, FLAG_1, unread);
            stream.writeInt32(flags);
            peer_id.serializeToStream(stream);
            if (reaction instanceof TL_reactionEmoji)
                stream.writeString(((TL_reactionEmoji) reaction).emoticon);
            else
                stream.writeString("");
        }
    }

    public static class TL_messagePeerReaction_layer137 extends MessagePeerReaction {
        public static final int constructor = 0x932844fa;

        public long user_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
            reaction = new TL_reactionEmoji();
            ((TL_reactionEmoji) reaction).emoticon = stream.readString(exception);
            peer_id = new TL_peerUser();
            peer_id.user_id = user_id;
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
            if (reaction instanceof TL_reactionEmoji)
                stream.writeString(((TL_reactionEmoji) reaction).emoticon);
            else
                stream.writeString("");
        }
    }

    public static abstract class auth_Authorization extends TLObject {

        public static auth_Authorization TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            auth_Authorization result = null;
            switch (constructor) {
                case 0x44747e9a:
                    result = new TL_auth_authorizationSignUpRequired();
                    break;
                case 0x33fb7bb8://TODO old constructor need remove
                    result = new TL_auth_authorization();
                    break;
                case 0x2ea2c0d4:
                    result = new TL_auth_authorization();
                    break;
            }
            return TLdeserialize(auth_Authorization.class, result, stream, constructor, exception);
        }
    }

    public static class TL_auth_authorizationSignUpRequired extends auth_Authorization {
        public static final int constructor = 0x44747e9a;

        public int flags;
        public TL_help_termsOfService terms_of_service;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                terms_of_service = TL_help_termsOfService.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                terms_of_service.serializeToStream(stream);
            }
        }
    }

    public static class TL_auth_authorization extends auth_Authorization {
        public static final int constructor = 0x2ea2c0d4;

        public int flags;
        public boolean setup_password_required;
        public int otherwise_relogin_days;
        public int tmp_sessions;
        public byte[] future_auth_token;
        public User user;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            setup_password_required = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_1)) {
                otherwise_relogin_days = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                tmp_sessions = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                future_auth_token = stream.readByteArray(exception);
            }
            user = User.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, setup_password_required);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(otherwise_relogin_days);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(tmp_sessions);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeByteArray(future_auth_token);
            }
            user.serializeToStream(stream);
        }
    }

    public static class PollAnswer extends TLObject {
        public int flags;
        public TL_textWithEntities text = new TL_textWithEntities();
        public byte[] option;
        public MessageMedia media;
        public InputMedia input_media;
        public TLRPC.Peer added_by;
        public int date;
        
        public int unshuffled_index; // custom
        public long shuffle_hash; // custom

        private static PollAnswer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_pollAnswer.constructor:
                    return new TL_pollAnswer();
                case TL_pollAnswer_layer223.constructor:
                    return new TL_pollAnswer_layer223();
                case TL_pollAnswer_layer178.constructor:
                    return new TL_pollAnswer_layer178();
                case TL_inputPollAnswer.constructor:
                    return new TL_inputPollAnswer();
            }
            return null;
        }

        public static PollAnswer TLdeserialize(InputSerializedData stream, boolean exception) {
            return TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public static PollAnswer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(PollAnswer.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_inputPollAnswer extends PollAnswer {
        public static final int constructor = 0x199FED96;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                input_media = InputMedia.TLdeserialize(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, input_media != null);
            stream.writeInt32(flags);
            text.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                input_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_pollAnswer extends PollAnswer {
        public static final int constructor = 0x4B7D786A;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            option = stream.readByteArray(exception);
            if (hasFlag(flags, FLAG_0)) {
                media = MessageMedia.TLdeserialize(stream, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                added_by = TLRPC.Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
                date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, media != null);
            flags = setFlag(flags, FLAG_1, added_by != null);
            stream.writeInt32(flags);
            text.serializeToStream(stream);
            stream.writeByteArray(option);
            if (hasFlag(flags, FLAG_0)) {
                media.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                added_by.serializeToStream(stream);
                stream.writeInt32(date);
            }
        }
    }

    public static class TL_pollAnswer_layer223 extends TL_pollAnswer {
        public static final int constructor = 0xff16e2ca;

        public void readParams(InputSerializedData stream, boolean exception) {
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            option = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeByteArray(option);
        }
    }

    public static class TL_pollAnswer_layer178 extends TL_pollAnswer {
        public static final int constructor = 0x6ca9c2e9;

        public void readParams(InputSerializedData stream, boolean exception) {
            text = new TL_textWithEntities();
            text.text = stream.readString(exception);
            option = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(text == null ? "" : text.text);
            stream.writeByteArray(option);
        }
    }

    public static abstract class JSONValue extends TLObject {

        public static JSONValue TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            JSONValue result = null;
            switch (constructor) {
                case 0xc7345e6a:
                    result = new TL_jsonBool();
                    break;
                case 0x3f6d7b68:
                    result = new TL_jsonNull();
                    break;
                case 0xb71e767a:
                    result = new TL_jsonString();
                    break;
                case 0xf7444763:
                    result = new TL_jsonArray();
                    break;
                case 0x99c1d49d:
                    result = new TL_jsonObject();
                    break;
                case 0x2be0dfa4:
                    result = new TL_jsonNumber();
                    break;
            }
            return TLdeserialize(JSONValue.class, result, stream, constructor, exception);
        }
    }

    public static class TL_jsonBool extends JSONValue {
        public static final int constructor = 0xc7345e6a;

        public boolean value;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readBool(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeBool(value);
        }
    }

    public static class TL_jsonNull extends JSONValue {
        public static final int constructor = 0x3f6d7b68;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_jsonString extends JSONValue {
        public static final int constructor = 0xb71e767a;

        public String value;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(value);
        }
    }

    public static class TL_jsonArray extends JSONValue {
        public static final int constructor = 0xf7444763;

        public ArrayList<JSONValue> value = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            value = Vector.deserialize(stream, JSONValue::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, value);
        }
    }

    public static class TL_jsonObject extends JSONValue {
        public static final int constructor = 0x99c1d49d;

        public ArrayList<TL_jsonObjectValue> value = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            value = Vector.deserialize(stream, TL_jsonObjectValue::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, value);
        }
    }

    public static class TL_jsonNumber extends JSONValue {
        public static final int constructor = 0x2be0dfa4;

        public double value;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readDouble(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeDouble(value);
        }
    }

    public static abstract class InputWallPaper extends TLObject {

        public static InputWallPaper TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputWallPaper result = null;
            switch (constructor) {
                case 0xe630b979:
                    result = new TL_inputWallPaper();
                    break;
                case 0x967a462e:
                    result = new TL_inputWallPaperNoFile();
                    break;
                case 0x72091c80:
                    result = new TL_inputWallPaperSlug();
                    break;
            }
            return TLdeserialize(InputWallPaper.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputWallPaper extends InputWallPaper {
        public static final int constructor = 0xe630b979;

        public long id;
        public long access_hash;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_inputWallPaperNoFile extends InputWallPaper {
        public static final int constructor = 0x967a462e;

        public long id;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_inputWallPaperSlug extends InputWallPaper {
        public static final int constructor = 0x72091c80;

        public String slug;

        public void readParams(InputSerializedData stream, boolean exception) {
            slug = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(slug);
        }
    }

    public static class TL_messages_historyImportParsed extends TLObject {
        public static final int constructor = 0x5e0fb7b9;

        public int flags;
        public boolean pm;
        public boolean group;
        public String title;

        public static TL_messages_historyImportParsed TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_historyImportParsed result = TL_messages_historyImportParsed.constructor != constructor ? null : new TL_messages_historyImportParsed();
            return TLdeserialize(TL_messages_historyImportParsed.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            pm = hasFlag(flags, FLAG_0);
            group = hasFlag(flags, FLAG_1);
            if (hasFlag(flags, FLAG_2)) {
                title = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, pm);
            flags = setFlag(flags, FLAG_1, group);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(title);
            }
        }
    }

    public static class TL_folder extends TLObject {
        public static final int constructor = 0xff544e65;

        public int flags;
        public boolean autofill_new_broadcasts;
        public boolean autofill_public_groups;
        public boolean autofill_new_correspondents;
        public int id;
        public String title;
        public ChatPhoto photo;

        public static TL_folder TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_folder result = TL_folder.constructor != constructor ? null : new TL_folder();
            return TLdeserialize(TL_folder.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            autofill_new_broadcasts = hasFlag(flags, FLAG_0);
            autofill_public_groups = hasFlag(flags, FLAG_1);
            autofill_new_correspondents = hasFlag(flags, FLAG_2);
            id = stream.readInt32(exception);
            title = stream.readString(exception);
            if (hasFlag(flags, FLAG_3)) {
                photo = ChatPhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, autofill_new_broadcasts);
            flags = setFlag(flags, FLAG_1, autofill_public_groups);
            flags = setFlag(flags, FLAG_2, autofill_new_correspondents);
            stream.writeInt32(flags);
            stream.writeInt32(id);
            stream.writeString(title);
            if (hasFlag(flags, FLAG_3)) {
                photo.serializeToStream(stream);
            }
        }
    }

    public static abstract class messages_Messages extends TLObject {
        public ArrayList<Message> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public ArrayList<TL_forumTopic> topics = new ArrayList<>();
        public int flags;
        public boolean inexact;
        public int pts;
        public int count;
        public int next_rate;
        public int offset_id_offset;
        public ArrayList<Document> animatedEmoji;
        public SearchPostsFlood search_flood;

        public static messages_Messages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_Messages result = null;
            switch (constructor) {
                case TL_messages_messagesSlice.constructor:
                    result = new TL_messages_messagesSlice();
                    break;
                case TL_messages_messagesSlice_layer215.constructor:
                    result = new TL_messages_messagesSlice_layer215();
                    break;
                case TL_messages_messagesSlice_layer210.constructor:
                    result = new TL_messages_messagesSlice_layer210();
                    break;
                case TL_messages_messages.constructor:
                    result = new TL_messages_messages();
                    break;
                case TL_messages_messages_layer215.constructor:
                    result = new TL_messages_messages_layer215();
                    break;
                case TL_messages_channelMessages.constructor:
                    result = new TL_messages_channelMessages();
                    break;
                case TL_messages_messagesNotModified.constructor:
                    result = new TL_messages_messagesNotModified();
                    break;
            }
            return TLdeserialize(messages_Messages.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_messagesSlice extends messages_Messages {
        public static final int constructor = 0x5F206716;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                next_rate = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood = SearchPostsFlood.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            topics = Vector.deserialize(stream, TL_forumTopic::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(next_rate);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood.serializeToStream(stream);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, topics);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messagesSlice_layer215 extends  TL_messages_messagesSlice {
        public static final int constructor = 0x762b263d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                next_rate = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood = SearchPostsFlood.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(next_rate);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            if (hasFlag(flags, FLAG_3)) {
                search_flood.serializeToStream(stream);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messagesSlice_layer210 extends TL_messages_messagesSlice {
        public static final int constructor = 0x3a54685e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                next_rate = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(next_rate);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messages extends messages_Messages {
        public static final int constructor = 0x1D73E7EA;

        public void readParams(InputSerializedData stream, boolean exception) {
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            topics = Vector.deserialize(stream, TL_forumTopic::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, messages);
            Vector.serialize(stream, topics);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messages_layer215 extends messages_Messages {
        public static final int constructor = 0x8c718e87;

        public void readParams(InputSerializedData stream, boolean exception) {
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_channelMessages extends messages_Messages {
        public static final int constructor = 0xc776ba4e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            inexact = hasFlag(flags, FLAG_1);
            pts = stream.readInt32(exception);
            count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                offset_id_offset = stream.readInt32(exception);
            }
            messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            topics = Vector.deserialize(stream, TL_forumTopic::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, inexact);
            stream.writeInt32(flags);
            stream.writeInt32(pts);
            stream.writeInt32(count);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(offset_id_offset);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, topics);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_messagesNotModified extends messages_Messages {
        public static final int constructor = 0x74535f21;

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
        }
    }

    public static class PaymentForm extends TLObject {
        public int flags;
        public boolean can_save_credentials;
        public boolean password_missing;
        public long form_id;
        public long bot_id;
        public String title;
        public String description;
        public WebDocument photo;
        public TL_invoice invoice;
        public long provider_id;
        public String url;
        public String native_provider;
        public TL_dataJSON native_params;
        public ArrayList<TL_paymentFormMethod> additional_methods = new ArrayList<>();
        public TL_paymentRequestedInfo saved_info;
        public ArrayList<TL_paymentSavedCredentialsCard> saved_credentials = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static PaymentForm TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PaymentForm result = null;
            switch (constructor) {
                case TL_payments_paymentForm.constructor:
                    result = new TL_payments_paymentForm();
                    break;
                case TL_payments_paymentFormStars.constructor:
                    result = new TL_payments_paymentFormStars();
                    break;
                case TL_payments_paymentFormStarGift.constructor:
                    result = new TL_payments_paymentFormStarGift();
                    break;
            }
            return TLdeserialize(PaymentForm.class, result, stream, constructor, exception);
        }
    }

    public static class TL_payments_paymentForm extends PaymentForm {
        public static final int constructor = 0xa0058751;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_save_credentials = hasFlag(flags, FLAG_2);
            password_missing = hasFlag(flags, FLAG_3);
            form_id = stream.readInt64(exception);
            bot_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_5)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            provider_id = stream.readInt64(exception);
            url = stream.readString(exception);
            if (hasFlag(flags, FLAG_4)) {
                native_provider = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                native_params = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                additional_methods = Vector.deserialize(stream, TL_paymentFormMethod::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                saved_info = TL_paymentRequestedInfo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                saved_credentials = Vector.deserialize(stream, TL_paymentSavedCredentialsCard::TLdeserialize, exception);
            }
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, can_save_credentials);
            flags = setFlag(flags, FLAG_3, password_missing);
            stream.writeInt32(flags);
            stream.writeInt64(form_id);
            stream.writeInt64(bot_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_5)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            stream.writeInt64(provider_id);
            stream.writeString(url);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(native_provider);
            }
            if (hasFlag(flags, FLAG_4)) {
                native_params.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_6)) {
                Vector.serialize(stream, additional_methods);
            }
            if (hasFlag(flags, FLAG_0)) {
                saved_info.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, saved_credentials);
            }
            Vector.serialize(stream, users);
        }
    }

    public static class TL_payments_paymentFormStars extends PaymentForm {
        public static final int constructor = 0x7bf6b15c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            form_id = stream.readInt64(exception);
            bot_id = stream.readInt64(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_5)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, can_save_credentials);
            flags = setFlag(flags, FLAG_3, password_missing);
            stream.writeInt32(flags);
            stream.writeInt64(form_id);
            stream.writeInt64(bot_id);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_5)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_payments_paymentFormStarGift extends PaymentForm {
        public static final int constructor = 0xb425cfe1;

        public void readParams(InputSerializedData stream, boolean exception) {
            form_id = stream.readInt64(exception);
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(form_id);
            invoice.serializeToStream(stream);
        }
    }

    public static class TL_paymentFormMethod extends TLObject {
        public static final int constructor = 0x88f8f21b;

        public String url;
        public String title;

        public static TL_paymentFormMethod TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_paymentFormMethod result = TL_paymentFormMethod.constructor != constructor ? null : new TL_paymentFormMethod();
            return TLdeserialize(TL_paymentFormMethod.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
            title = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeString(title);
        }
    }

    public static abstract class ContactLink_layer101 extends TLObject {

        public static ContactLink_layer101 TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ContactLink_layer101 result = null;
            switch (constructor) {
                case 0xfeedd3ad:
                    result = new TL_contactLinkNone();
                    break;
                case 0xd502c2d0:
                    result = new TL_contactLinkContact();
                    break;
                case 0x5f4f9247:
                    result = new TL_contactLinkUnknown();
                    break;
            }
            return TLdeserialize(ContactLink_layer101.class, result, stream, constructor, exception);
        }
    }

    public static class TL_contactLinkNone extends ContactLink_layer101 {
        public static final int constructor = 0xfeedd3ad;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contactLinkContact extends ContactLink_layer101 {
        public static final int constructor = 0xd502c2d0;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contactLinkUnknown extends ContactLink_layer101 {
        public static final int constructor = 0x5f4f9247;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contacts_link_layer101 extends TLObject {
        public static final int constructor = 0x3ace484c;

        public ContactLink_layer101 my_link;
        public ContactLink_layer101 foreign_link;
        public User user;

        public static TL_contacts_link_layer101 TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_contacts_link_layer101 result = TL_contacts_link_layer101.constructor != constructor ? null : new TL_contacts_link_layer101();
            return TLdeserialize(TL_contacts_link_layer101.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            my_link = ContactLink_layer101.TLdeserialize(stream, stream.readInt32(exception), exception);
            foreign_link = ContactLink_layer101.TLdeserialize(stream, stream.readInt32(exception), exception);
            user = User.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            my_link.serializeToStream(stream);
            foreign_link.serializeToStream(stream);
            user.serializeToStream(stream);
        }
    }

    public static abstract class EncryptedFile extends TLObject {
        public long id;
        public long access_hash;
        public long size;
        public int dc_id;
        public int key_fingerprint;

        public static EncryptedFile TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            EncryptedFile result = null;
            switch (constructor) {
                case 0xa8008cd8:
                    result = new TL_encryptedFile();
                    break;
                case 0x4a70994c:
                    result = new TL_encryptedFile_layer142();
                    break;
                case 0xc21f497e:
                    result = new TL_encryptedFileEmpty();
                    break;
            }
            return TLdeserialize(EncryptedFile.class, result, stream, constructor, exception);
        }
    }

    public static class TL_encryptedFile extends EncryptedFile {
        public static final int constructor = 0xa8008cd8;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt64(exception);
            dc_id = stream.readInt32(exception);
            key_fingerprint = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt64(size);
            stream.writeInt32(dc_id);
            stream.writeInt32(key_fingerprint);
        }
    }

    public static class TL_encryptedFile_layer142 extends EncryptedFile {
        public static final int constructor = 0x4a70994c;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
            key_fingerprint = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) size);
            stream.writeInt32(dc_id);
            stream.writeInt32(key_fingerprint);
        }
    }

    public static class TL_encryptedFileEmpty extends EncryptedFile {
        public static final int constructor = 0xc21f497e;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class Peer extends TLObject {

        public long user_id;
        public long chat_id;
        public long channel_id;

        public static Peer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            Peer result = null;
            switch (constructor) {
                case 0xa2a5371e:
                    result = new TL_peerChannel();
                    break;
                case 0xbddde532:
                    result = new TL_peerChannel_layer131();
                    break;
                case 0x59511722:
                    result = new TL_peerUser();
                    break;
                case 0x9db1bc6d:
                    result = new TL_peerUser_layer131();
                    break;
                case 0x36c6019a:
                    result = new TL_peerChat();
                    break;
                case 0xbad0e5bb:
                    result = new TL_peerChat_layer131();
                    break;
            }
            return TLdeserialize(Peer.class, result, stream, constructor, exception);
        }
    }

    public static class TL_peerChannel_layer131 extends TL_peerChannel {
        public static final int constructor = 0xbddde532;

        public void readParams(InputSerializedData stream, boolean exception) {
            channel_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) channel_id);
        }
    }

    public static class TL_peerUser extends Peer {
        public static final int constructor = 0x59511722;

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
        }
    }

    public static class TL_peerChannel extends Peer {
        public static final int constructor = 0xa2a5371e;

        public void readParams(InputSerializedData stream, boolean exception) {
            channel_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(channel_id);
        }
    }

    public static class TL_peerChat extends Peer {
        public static final int constructor = 0x36c6019a;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(chat_id);
        }
    }

    public static class TL_peerUser_layer131 extends TL_peerUser {
        public static final int constructor = 0x9db1bc6d;

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) user_id);
        }
    }

    public static class TL_peerChat_layer131 extends TL_peerChat {
        public static final int constructor = 0xbad0e5bb;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32((int) chat_id);
        }
    }

    public static class TL_labeledPrice extends TLObject {
        public static final int constructor = 0xcb296bf8;

        public String label;
        public long amount;

        public static TL_labeledPrice TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_labeledPrice result = TL_labeledPrice.constructor != constructor ? null : new TL_labeledPrice();
            return TLdeserialize(TL_labeledPrice.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            label = stream.readString(exception);
            amount = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(label);
            stream.writeInt64(amount);
        }
    }

    public static class TL_messages_exportedChatInvites extends TLObject {
        public static final int constructor = 0xbdc62dcc;

        public int count;
        public ArrayList<ExportedChatInvite> invites = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_messages_exportedChatInvites TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_exportedChatInvites result = TL_messages_exportedChatInvites.constructor != constructor ? null : new TL_messages_exportedChatInvites();
            return TLdeserialize(TL_messages_exportedChatInvites.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            invites = Vector.deserialize(stream, ExportedChatInvite::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, invites);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_inputStickerSetItem extends TLObject {
        public static final int constructor = 0xffa0a496;

        public int flags;
        public InputDocument document;
        public String emoji;
        public TL_maskCoords mask_coords;

        public static TL_inputStickerSetItem TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_inputStickerSetItem result = TL_inputStickerSetItem.constructor != constructor ? null : new TL_inputStickerSetItem();
            return TLdeserialize(TL_inputStickerSetItem.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            document = InputDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            emoji = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords = TL_maskCoords.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            document.serializeToStream(stream);
            stream.writeString(emoji);
            if (hasFlag(flags, FLAG_0)) {
                mask_coords.serializeToStream(stream);
            }
        }
    }

    public static class TL_langPackDifference extends TLObject {
        public static final int constructor = 0xf385c1f6;

        public String lang_code;
        public int from_version;
        public int version;
        public ArrayList<LangPackString> strings = new ArrayList<>();

        public static TL_langPackDifference TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_langPackDifference result = TL_langPackDifference.constructor != constructor ? null : new TL_langPackDifference();
            return TLdeserialize(TL_langPackDifference.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            lang_code = stream.readString(exception);
            from_version = stream.readInt32(exception);
            version = stream.readInt32(exception);
            strings = Vector.deserialize(stream, LangPackString::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(lang_code);
            stream.writeInt32(from_version);
            stream.writeInt32(version);
            Vector.serialize(stream, strings);
        }
    }

    public static abstract class help_DeepLinkInfo extends TLObject {

        public static help_DeepLinkInfo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_DeepLinkInfo result = null;
            switch (constructor) {
                case 0x66afa166:
                    result = new TL_help_deepLinkInfoEmpty();
                    break;
                case 0x6a4ee832:
                    result = new TL_help_deepLinkInfo();
                    break;
            }
            return TLdeserialize(help_DeepLinkInfo.class, result, stream, constructor, exception);
        }
    }

    public static class TL_help_deepLinkInfoEmpty extends help_DeepLinkInfo {
        public static final int constructor = 0x66afa166;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_help_deepLinkInfo extends help_DeepLinkInfo {
        public static final int constructor = 0x6a4ee832;

        public int flags;
        public boolean update_app;
        public String message;
        public ArrayList<MessageEntity> entities = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            update_app = hasFlag(flags, FLAG_0);
            message = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, update_app);
            stream.writeInt32(flags);
            stream.writeString(message);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, entities);
            }
        }
    }

    public static class TL_chatAdminRights extends TLObject {
        public static final int constructor = 0x5fb224d5;

        public int flags;
        public boolean change_info;
        public boolean post_messages;
        public boolean edit_messages;
        public boolean delete_messages;
        public boolean ban_users;
        public boolean invite_users;
        public boolean pin_messages;
        public boolean add_admins;
        public boolean anonymous;
        public boolean manage_call;
        public boolean other;
        public boolean manage_topics;
        public boolean post_stories;
        public boolean edit_stories;
        public boolean delete_stories;
        public boolean manage_direct_messages;
        public boolean manage_ranks;
        public boolean manage_linked_peers;
        public boolean manage_welcome_messages;

        public static TL_chatAdminRights TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_chatAdminRights result = TL_chatAdminRights.constructor != constructor ? null : new TL_chatAdminRights();
            return TLdeserialize(TL_chatAdminRights.class, result, stream, constructor, exception);
        }

        public static TL_chatAdminRights clone(TL_chatAdminRights rights) {
            if (rights == null) return null;
            SerializedData data = new SerializedData(rights.getObjectSize());
            rights.serializeToStream(data);
            data = new SerializedData(data.toByteArray());
            return TLdeserialize(data, data.readInt32(false), false);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TL_chatAdminRights)) return false;
            return this.flags == ((TL_chatAdminRights) obj).flags;
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            change_info = hasFlag(flags, FLAG_0);
            post_messages = hasFlag(flags, FLAG_1);
            edit_messages = hasFlag(flags, FLAG_2);
            delete_messages = hasFlag(flags, FLAG_3);
            ban_users = hasFlag(flags, FLAG_4);
            invite_users = hasFlag(flags, FLAG_5);
            pin_messages = hasFlag(flags, FLAG_7);
            add_admins = hasFlag(flags, FLAG_9);
            anonymous = hasFlag(flags, FLAG_10);
            manage_call = hasFlag(flags, FLAG_11);
            other = hasFlag(flags, FLAG_12);
            manage_topics = hasFlag(flags, FLAG_13);
            post_stories = hasFlag(flags, FLAG_14);
            edit_stories = hasFlag(flags, FLAG_15);
            delete_stories = hasFlag(flags, FLAG_16);
            manage_direct_messages = hasFlag(flags, FLAG_17);
            manage_ranks = hasFlag(flags, FLAG_18);
            manage_linked_peers = hasFlag(flags, FLAG_19);
            manage_welcome_messages = hasFlag(flags, FLAG_20);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, change_info);
            flags = setFlag(flags, FLAG_1, post_messages);
            flags = setFlag(flags, FLAG_2, edit_messages);
            flags = setFlag(flags, FLAG_3, delete_messages);
            flags = setFlag(flags, FLAG_4, ban_users);
            flags = setFlag(flags, FLAG_5, invite_users);
            flags = setFlag(flags, FLAG_7, pin_messages);
            flags = setFlag(flags, FLAG_9, add_admins);
            flags = setFlag(flags, FLAG_10, anonymous);
            flags = setFlag(flags, FLAG_11, manage_call);
            flags = setFlag(flags, FLAG_12, other);
            flags = setFlag(flags, FLAG_13, manage_topics);
            flags = setFlag(flags, FLAG_14, post_stories);
            flags = setFlag(flags, FLAG_15, edit_stories);
            flags = setFlag(flags, FLAG_16, delete_stories);
            flags = setFlag(flags, FLAG_17, manage_direct_messages);
            flags = setFlag(flags, FLAG_18, manage_ranks);
            flags = setFlag(flags, FLAG_19, manage_linked_peers);
            flags = setFlag(flags, FLAG_20, manage_welcome_messages);
            stream.writeInt32(flags);
        }
    }

    public static abstract class PollResults extends TLObject {

        public int flags;
        public boolean min;
        public ArrayList<PollAnswerVoters> results = new ArrayList<>();
        public int total_voters;
        public ArrayList<Peer> recent_voters = new ArrayList<>();
        public String solution;
        public ArrayList<MessageEntity> solution_entities = new ArrayList<>();
        public MessageMedia solution_media;
        public boolean has_unread_votes;
        public boolean can_view_stats;

        private static PollResults fromConstructor(int constructor) {
            switch (constructor) {
                case TL_pollResults_layer108.constructor:
                    return new TL_pollResults_layer108();
                case TL_pollResults_layer111.constructor:
                    return new TL_pollResults_layer111();
                case TL_pollResults_layer131.constructor:
                    return new TL_pollResults_layer131();
                case TL_pollResults_layer158.constructor:
                    return new TL_pollResults_layer158();
                case TL_pollResults_layer223.constructor:
                    return new TL_pollResults_layer223();
                case TL_pollResults.constructor:
                    return new TL_pollResults();
            }
            return null;
        }

        public static PollResults TLdeserialize(InputSerializedData stream, boolean exception) {
            return TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public static PollResults TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(PollResults.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_pollResults_layer108 extends TL_pollResults {
        public static final int constructor = 0x5755785a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
        }
    }

    public static class TL_pollResults_layer111 extends PollResults {
        public static final int constructor = 0xc87024a2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = VectorLegacy.deserialize_IntUserIdAsPeer(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                VectorLegacy.serialize_PeerAsIntUserId(stream, recent_voters);
            }
        }
    }

    public static class TL_pollResults_layer131 extends TL_pollResults {
        public static final int constructor = 0xbadcc1a3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = VectorLegacy.deserialize_IntUserIdAsPeer(stream, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                VectorLegacy.serialize_PeerAsIntUserId(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
                Vector.serialize(stream, solution_entities);
            }
        }
    }

    public static class TL_pollResults extends PollResults {
        public static final int constructor = 0xba7bb15e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            has_unread_votes = hasFlag(flags, FLAG_6);
            can_view_stats = hasFlag(flags, FLAG_7);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                solution_media = MessageMedia.TLdeserialize(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            flags = setFlag(flags, FLAG_5, solution_media != null);
            flags = setFlag(flags, FLAG_6, has_unread_votes);
            flags = setFlag(flags, FLAG_7, can_view_stats);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
            }
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, solution_entities);
            }
            if (hasFlag(flags, FLAG_5)) {
                solution_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_pollResults_layer223 extends TL_pollResults {
        public static final int constructor = 0x7adf2420;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
            }
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, solution_entities);
            }
        }
    }

    public static class TL_pollResults_layer158 extends PollResults {
        public static final int constructor = 0xdcb82ea3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            min = hasFlag(flags, FLAG_0);
            if (hasFlag(flags, FLAG_1)) {
                results = Vector.deserialize(stream, PollAnswerVoters::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                total_voters = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                recent_voters = VectorLegacy.deserialize_LongUserIdAsPeer(stream, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                solution = stream.readString(exception);
                solution_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, min);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, results);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(total_voters);
            }
            if (hasFlag(flags, FLAG_3)) {
                VectorLegacy.serialize_PeerAsLongUserId(stream, recent_voters);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(solution);
                Vector.serialize(stream, solution_entities);
            }
        }
    }

    public static abstract class SecureFile extends TLObject {

        public static SecureFile TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            SecureFile result = null;
            switch (constructor) {
                case 0x64199744:
                    result = new TL_secureFileEmpty();
                    break;
                case 0x7d09c27e:
                    result = new TL_secureFile();
                    break;
            }
            return TLdeserialize(SecureFile.class, result, stream, constructor, exception);
        }
    }

    public static class TL_secureFileEmpty extends SecureFile {
        public static final int constructor = 0x64199744;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_secureFile extends SecureFile {
        public static final int constructor = 0x7d09c27e;

        public long id;
        public long access_hash;
        public long size;
        public int dc_id;
        public int date;
        public byte[] file_hash;
        public byte[] secret;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt64(exception);
            dc_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            file_hash = stream.readByteArray(exception);
            secret = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt64(size);
            stream.writeInt32(dc_id);
            stream.writeInt32(date);
            stream.writeByteArray(file_hash);
            stream.writeByteArray(secret);
        }
    }

    public static class TL_secureFile_layer142 extends TL_secureFile {
        public static final int constructor = 0xe0277a62;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            file_hash = stream.readByteArray(exception);
            secret = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) size);
            stream.writeInt32(dc_id);
            stream.writeInt32(date);
            stream.writeByteArray(file_hash);
            stream.writeByteArray(secret);
        }
    }

    public static class TL_messages_affectedMessages extends TLObject {
        public static final int constructor = 0x84d19185;

        public int pts;
        public int pts_count;

        public static TL_messages_affectedMessages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_affectedMessages result = TL_messages_affectedMessages.constructor != constructor ? null : new TL_messages_affectedMessages();
            return TLdeserialize(TL_messages_affectedMessages.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            pts = stream.readInt32(exception);
            pts_count = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(pts);
            stream.writeInt32(pts_count);
        }
    }

    public static class TL_messages_chatInviteImporters extends TLObject {
        public static final int constructor = 0x81b6b00a;

        public int count;
        public ArrayList<TL_chatInviteImporter> importers = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_messages_chatInviteImporters TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_chatInviteImporters result = TL_messages_chatInviteImporters.constructor != constructor ? null : new TL_messages_chatInviteImporters();
            return TLdeserialize(TL_messages_chatInviteImporters.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            importers = Vector.deserialize(stream, TL_chatInviteImporter::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, importers);
            Vector.serialize(stream, users);
        }
    }

    public static class PollAnswerVoters extends TLObject {
        public int flags;
        public boolean chosen;
        public boolean correct;
        public byte[] option;
        public int voters;
        public @Nullable ArrayList<Peer> recent_voters;

        private static PollAnswerVoters fromConstructor(int constructor) {
            switch (constructor) {
                case TL_pollAnswerVoters.constructor:
                    return new TL_pollAnswerVoters();
                case TL_pollAnswerVoters_layer223.constructor:
                    return new TL_pollAnswerVoters_layer223();
            }
            return null;
        }

        public static PollAnswerVoters TLdeserialize(InputSerializedData stream, boolean exception) {
            return TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public static PollAnswerVoters TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(PollAnswerVoters.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_pollAnswerVoters extends PollAnswerVoters {
        public static final int constructor = 0x3645230a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            chosen = hasFlag(flags, FLAG_0);
            correct = hasFlag(flags, FLAG_1);
            option = stream.readByteArray(exception);
            if (hasFlag(flags, FLAG_2)) {
                voters = stream.readInt32(exception);
                recent_voters = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, chosen);
            flags = setFlag(flags, FLAG_1, correct);
            stream.writeInt32(flags);
            stream.writeByteArray(option);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(voters);
                Vector.serialize(stream, recent_voters);
            }
        }
    }

    public static class TL_pollAnswerVoters_layer223 extends TL_pollAnswerVoters {
        public static final int constructor = 0x3b6ddad2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            chosen = hasFlag(flags, FLAG_0);
            correct = hasFlag(flags, FLAG_1);
            option = stream.readByteArray(exception);
            voters = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, chosen);
            flags = setFlag(flags, FLAG_1, correct);
            stream.writeInt32(flags);
            stream.writeByteArray(option);
            stream.writeInt32(voters);
        }
    }

    public static class TL_channels_channelParticipant extends TLObject {
        public static final int constructor = 0xdfb80317;

        public ChannelParticipant participant;
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_channels_channelParticipant TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channels_channelParticipant result = TL_channels_channelParticipant.constructor != constructor ? null : new TL_channels_channelParticipant();
            return TLdeserialize(TL_channels_channelParticipant.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            participant = ChannelParticipant.TLdeserialize(stream, stream.readInt32(exception), exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            participant.serializeToStream(stream);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_authorization extends TLObject {
        public static final int constructor = 0xad01d61d;

        public int flags;
        public boolean current;
        public boolean official_app;
        public boolean password_pending;
        public boolean encrypted_requests_disabled;
        public boolean call_requests_disabled;
        public boolean unconfirmed;
        public long hash;
        public String device_model;
        public String platform;
        public String system_version;
        public int api_id;
        public String app_name;
        public String app_version;
        public int date_created;
        public int date_active;
        public String ip;
        public String country;
        public String region;

        public static TL_authorization TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_authorization result = TL_authorization.constructor != constructor ? null : new TL_authorization();
            return TLdeserialize(TL_authorization.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            current = hasFlag(flags, FLAG_0);
            official_app = hasFlag(flags, FLAG_1);
            password_pending = hasFlag(flags, FLAG_2);
            encrypted_requests_disabled = hasFlag(flags, FLAG_3);
            call_requests_disabled = hasFlag(flags, FLAG_4);
            unconfirmed = hasFlag(flags, FLAG_5);
            hash = stream.readInt64(exception);
            device_model = stream.readString(exception);
            platform = stream.readString(exception);
            system_version = stream.readString(exception);
            api_id = stream.readInt32(exception);
            app_name = stream.readString(exception);
            app_version = stream.readString(exception);
            date_created = stream.readInt32(exception);
            date_active = stream.readInt32(exception);
            ip = stream.readString(exception);
            country = stream.readString(exception);
            region = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, current);
            flags = setFlag(flags, FLAG_1, official_app);
            flags = setFlag(flags, FLAG_2, password_pending);
            flags = setFlag(flags, FLAG_3, encrypted_requests_disabled);
            flags = setFlag(flags, FLAG_4, call_requests_disabled);
            flags = setFlag(flags, FLAG_5, unconfirmed);
            stream.writeInt32(flags);
            stream.writeInt64(hash);
            stream.writeString(device_model);
            stream.writeString(platform);
            stream.writeString(system_version);
            stream.writeInt32(api_id);
            stream.writeString(app_name);
            stream.writeString(app_version);
            stream.writeInt32(date_created);
            stream.writeInt32(date_active);
            stream.writeString(ip);
            stream.writeString(country);
            stream.writeString(region);
        }
    }

    public static abstract class updates_Difference extends TLObject {
        public ArrayList<Message> new_messages = new ArrayList<>();
        public ArrayList<EncryptedMessage> new_encrypted_messages = new ArrayList<>();
        public ArrayList<Update> other_updates = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public TL_updates_state state;
        public TL_updates_state intermediate_state;
        public int pts;
        public int date;
        public int seq;

        public static updates_Difference TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            updates_Difference result = null;
            switch (constructor) {
                case 0xf49ca0:
                    result = new TL_updates_difference();
                    break;
                case 0xa8fb1981:
                    result = new TL_updates_differenceSlice();
                    break;
                case 0x4afe8f6d:
                    result = new TL_updates_differenceTooLong();
                    break;
                case 0x5d75a138:
                    result = new TL_updates_differenceEmpty();
                    break;
            }
            return TLdeserialize(updates_Difference.class, result, stream, constructor, exception);
        }
    }

    public static class TL_updates_difference extends updates_Difference {
        public static final int constructor = 0xf49ca0;

        public void readParams(InputSerializedData stream, boolean exception) {
            new_messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            new_encrypted_messages = Vector.deserialize(stream, EncryptedMessage::TLdeserialize, exception);
            other_updates = Vector.deserialize(stream, Update::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
            state = TL_updates_state.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, new_messages);
            Vector.serialize(stream, new_encrypted_messages);
            Vector.serialize(stream, other_updates);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
            state.serializeToStream(stream);
        }
    }

    public static class TL_updates_differenceSlice extends updates_Difference {
        public static final int constructor = 0xa8fb1981;

        public void readParams(InputSerializedData stream, boolean exception) {
            new_messages = Vector.deserialize(stream, Message::TLdeserialize, exception);
            new_encrypted_messages = Vector.deserialize(stream, EncryptedMessage::TLdeserialize, exception);
            other_updates = Vector.deserialize(stream, Update::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
            intermediate_state = TL_updates_state.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, new_messages);
            Vector.serialize(stream, new_encrypted_messages);
            Vector.serialize(stream, other_updates);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
            intermediate_state.serializeToStream(stream);
        }
    }

    public static class TL_updates_differenceTooLong extends updates_Difference {
        public static final int constructor = 0x4afe8f6d;

        public void readParams(InputSerializedData stream, boolean exception) {
            pts = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(pts);
        }
    }

    public static class TL_updates_differenceEmpty extends updates_Difference {
        public static final int constructor = 0x5d75a138;

        public void readParams(InputSerializedData stream, boolean exception) {
            date = stream.readInt32(exception);
            seq = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(date);
            stream.writeInt32(seq);
        }
    }

    public static abstract class PrivacyKey extends TLObject {

        public static PrivacyKey TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PrivacyKey result = null;
            switch (constructor) {
                case TL_privacyKeyStatusTimestamp.constructor:
                    result = new TL_privacyKeyStatusTimestamp();
                    break;
                case TL_privacyKeyPhoneP2P.constructor:
                    result = new TL_privacyKeyPhoneP2P();
                    break;
                case TL_privacyKeyChatInvite.constructor:
                    result = new TL_privacyKeyChatInvite();
                    break;
                case TL_privacyKeyAddedByPhone.constructor:
                    result = new TL_privacyKeyAddedByPhone();
                    break;
                case TL_privacyKeyVoiceMessages.constructor:
                    result = new TL_privacyKeyVoiceMessages();
                    break;
                case TL_privacyKeyAbout.constructor:
                    result = new TL_privacyKeyAbout();
                    break;
                case TL_privacyKeyPhoneCall.constructor:
                    result = new TL_privacyKeyPhoneCall();
                    break;
                case TL_privacyKeyForwards.constructor:
                    result = new TL_privacyKeyForwards();
                    break;
                case TL_privacyKeyPhoneNumber.constructor:
                    result = new TL_privacyKeyPhoneNumber();
                    break;
                case TL_privacyKeyProfilePhoto.constructor:
                    result = new TL_privacyKeyProfilePhoto();
                    break;
                case TL_privacyKeyBirthday.constructor:
                    result = new TL_privacyKeyBirthday();
                    break;
                case TL_privacyKeyStarGiftsAutoSave.constructor:
                    result = new TL_privacyKeyStarGiftsAutoSave();
                    break;
                case TL_privacyKeyNoPaidMessages.constructor:
                    result = new TL_privacyKeyNoPaidMessages();
                    break;
                case TL_privacyKeySavedMusic.constructor:
                    result = new TL_privacyKeySavedMusic();
                    break;
            }
            return TLdeserialize(PrivacyKey.class, result, stream, constructor, exception);
        }
    }

    public static class TL_privacyKeyStatusTimestamp extends PrivacyKey {
        public static final int constructor = 0xbc2eab30;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyPhoneP2P extends PrivacyKey {
        public static final int constructor = 0x39491cc8;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyChatInvite extends PrivacyKey {
        public static final int constructor = 0x500e6dfa;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyPhoneCall extends PrivacyKey {
        public static final int constructor = 0x3d662b7b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyAddedByPhone extends PrivacyKey {
        public static final int constructor = 0x42ffd42b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyAbout extends PrivacyKey {
        public static final int constructor = 0xa486b761;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyVoiceMessages extends PrivacyKey {
        public static final int constructor = 0x697f414;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyForwards extends PrivacyKey {
        public static final int constructor = 0x69ec56a3;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyPhoneNumber extends PrivacyKey {
        public static final int constructor = 0xd19ae46d;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyProfilePhoto extends PrivacyKey {
        public static final int constructor = 0x96151fed;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyBirthday extends PrivacyKey {
        public static final int constructor = 0x2000a518;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyStarGiftsAutoSave extends PrivacyKey {
        public static final int constructor = 0x2ca4fdf8;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeyNoPaidMessages extends PrivacyKey {
        public static final int constructor = 0x17d348d2;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyKeySavedMusic extends PrivacyKey {
        public static final int constructor = 0xff7a571b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class GeoPoint extends TLObject {
        public int flags;
        public double _long;
        public double lat;
        public int accuracy_radius;
        public long access_hash;

        public static GeoPoint TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            GeoPoint result = null;
            switch (constructor) {
                case 0x296f104:
                    result = new TL_geoPoint_layer119();
                    break;
                case 0x2049d70c:
                    result = new TL_geoPoint_layer81();
                    break;
                case 0x1117dd5f:
                    result = new TL_geoPointEmpty();
                    break;
                case 0xb2a2f663:
                    result = new TL_geoPoint();
                    break;
            }
            return TLdeserialize(GeoPoint.class, result, stream, constructor, exception);
        }
    }

    public static class TL_geoPoint_layer119 extends TL_geoPoint {
        public static final int constructor = 0x296f104;

        public void readParams(InputSerializedData stream, boolean exception) {
            _long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeDouble(_long);
            stream.writeDouble(lat);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_geoPoint_layer81 extends TL_geoPoint {
        public static final int constructor = 0x2049d70c;

        public void readParams(InputSerializedData stream, boolean exception) {
            _long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeDouble(_long);
            stream.writeDouble(lat);
        }
    }

    public static class TL_geoPointEmpty extends GeoPoint {
        public static final int constructor = 0x1117dd5f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_geoPoint extends GeoPoint {
        public static final int constructor = 0xb2a2f663;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            _long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
            access_hash = stream.readInt64(exception);
            if (hasFlag(flags, FLAG_0)) {
                accuracy_radius = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeDouble(_long);
            stream.writeDouble(lat);
            stream.writeInt64(access_hash);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(accuracy_radius);
            }
        }
    }

    public static abstract class ChatInvite extends TLObject {

        public int flags;
        public boolean channel;
        public boolean broadcast;
        public boolean isPublic;
        public boolean megagroup;
        public boolean request_needed;
        public String title;
        public String about;
        public Photo photo;
        public int participants_count;
        public ArrayList<User> participants = new ArrayList<>();
        public Chat chat;
        public int expires;
        public boolean verified;
        public boolean scam;
        public boolean fake;
        public boolean can_refulfill_subscription;
        public int color;
        public TL_stars.TL_starsSubscriptionPricing subscription_pricing;
        public long subscription_form_id;
        public TL_bots.botVerification bot_verification;

        public static ChatInvite TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ChatInvite result = null;
            switch (constructor) {
                case TL_chatInvite.constructor:
                    result = new TL_chatInvite();
                    break;
                case TL_chatInvite_layer195.constructor:
                    result = new TL_chatInvite_layer195();
                    break;
                case TL_chatInvite_layer185.constructor:
                    result = new TL_chatInvite_layer185();
                    break;
                case TL_chatInvite_layer165.constructor:
                    result = new TL_chatInvite_layer165();
                    break;
                case 0x61695cb0:
                    result = new TL_chatInvitePeek();
                    break;
                case 0x5a686d7c:
                    result = new TL_chatInviteAlready();
                    break;
            }
            return TLdeserialize(ChatInvite.class, result, stream, constructor, exception);
        }
    }

    public static class TL_chatInvite extends ChatInvite {
        public static final int constructor = 0x5c9d3702;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            can_refulfill_subscription = hasFlag(flags, FLAG_11);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing = TL_stars.TL_starsSubscriptionPricing.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                subscription_form_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                bot_verification = TL_bots.botVerification.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            flags = setFlag(flags, FLAG_11, can_refulfill_subscription);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            stream.writeInt32(color);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt64(subscription_form_id);
            }
            if (hasFlag(flags, FLAG_13)) {
                bot_verification.serializeToStream(stream);
            }
        }
    }

    public static class TL_chatInvite_layer195 extends TL_chatInvite {
        public static final int constructor = 0xfe65389d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            can_refulfill_subscription = hasFlag(flags, FLAG_11);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing = TL_stars.TL_starsSubscriptionPricing.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                subscription_form_id = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            flags = setFlag(flags, FLAG_11, can_refulfill_subscription);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            stream.writeInt32(color);
            if (hasFlag(flags, FLAG_10)) {
                subscription_pricing.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt64(subscription_form_id);
            }
        }
    }

    public static class TL_chatInvite_layer185 extends TL_chatInvite {
        public static final int constructor = 0xcde0ec40;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            color = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            stream.writeInt32(color);
        }
    }

    public static class TL_chatInvite_layer165 extends ChatInvite {
        public static final int constructor = 0x300c44c1;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            channel = hasFlag(flags, FLAG_0);
            broadcast = hasFlag(flags, FLAG_1);
            isPublic = hasFlag(flags, FLAG_2);
            megagroup = hasFlag(flags, FLAG_3);
            request_needed = hasFlag(flags, FLAG_6);
            verified = hasFlag(flags, FLAG_7);
            scam = hasFlag(flags, FLAG_8);
            fake = hasFlag(flags, FLAG_9);
            title = stream.readString(exception);
            boolean hasAbout = hasFlag(flags, FLAG_5);
            if (hasAbout) {
                about = stream.readString(exception);
            }
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            participants_count = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_4)) {
                participants = Vector.deserialize(stream, User::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                color = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, channel);
            flags = setFlag(flags, FLAG_1, broadcast);
            flags = setFlag(flags, FLAG_2, isPublic);
            flags = setFlag(flags, FLAG_3, megagroup);
            flags = setFlag(flags, FLAG_5, about != null);
            flags = setFlag(flags, FLAG_6, request_needed);
            flags = setFlag(flags, FLAG_7, verified);
            flags = setFlag(flags, FLAG_8, scam);
            flags = setFlag(flags, FLAG_9, fake);
            stream.writeInt32(flags);
            stream.writeString(title);
            if (about != null) {
                stream.writeString(about);
            }
            photo.serializeToStream(stream);
            stream.writeInt32(participants_count);
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, participants);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(color);
            }
        }
    }

    public static class TL_chatInvitePeek extends ChatInvite {
        public static final int constructor = 0x61695cb0;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
            expires = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            chat.serializeToStream(stream);
            stream.writeInt32(expires);
        }
    }

    public static class TL_chatInviteAlready extends ChatInvite {
        public static final int constructor = 0x5a686d7c;

        public void readParams(InputSerializedData stream, boolean exception) {
            chat = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            chat.serializeToStream(stream);
        }
    }

    public static class InputGroupCall extends TLObject {

        public long id;
        public long access_hash;
        public String slug;
        public int msg_id;

        public static InputGroupCall TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputGroupCall result = null;
            switch (constructor) {
                case TL_inputGroupCall.constructor:
                    result = new TL_inputGroupCall();
                    break;
                case TL_inputGroupCallSlug.constructor:
                    result = new TL_inputGroupCallSlug();
                    break;
                case TL_inputGroupCallInviteMessage.constructor:
                    result = new TL_inputGroupCallInviteMessage();
                    break;
            }
            return TLdeserialize(InputGroupCall.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputGroupCall extends InputGroupCall {
        public static final int constructor = 0xd8aa840f;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_inputGroupCallSlug extends InputGroupCall {
        public static final int constructor = 0xfe06823f;

        public void readParams(InputSerializedData stream, boolean exception) {
            slug = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(slug);
        }
    }

    public static class TL_inputGroupCallInviteMessage extends InputGroupCall {
        public static final int constructor = 0x8c10603f;

        public void readParams(InputSerializedData stream, boolean exception) {
            msg_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(msg_id);
        }
    }

    public static abstract class help_AppUpdate extends TLObject {

        public static help_AppUpdate TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_AppUpdate result = null;
            switch (constructor) {
                case 0xccbbce30:
                    result = new TL_help_appUpdate();
                    break;
                case 0xc45a6536:
                    result = new TL_help_noAppUpdate();
                    break;
            }
            return TLdeserialize(help_AppUpdate.class, result, stream, constructor, exception);
        }
    }

    @Keep
    public static class TL_help_appUpdate extends help_AppUpdate {
        public static final int constructor = 0xccbbce30;

        public int flags;
        public boolean can_not_skip;
        public int id;
        public String version;
        public String text;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public Document document;
        public String url;
        public Document sticker;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_not_skip = hasFlag(flags, FLAG_0);
            id = stream.readInt32(exception);
            version = stream.readString(exception);
            text = stream.readString(exception);
            entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_1)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                sticker = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, can_not_skip);
            stream.writeInt32(flags);
            stream.writeInt32(id);
            stream.writeString(version);
            stream.writeString(text);
            Vector.serialize(stream, entities);
            if (hasFlag(flags, FLAG_1)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(url);
            }
            if (hasFlag(flags, FLAG_3)) {
                sticker.serializeToStream(stream);
            }
        }
    }

    public static class TL_help_noAppUpdate extends help_AppUpdate {
        public static final int constructor = 0xc45a6536;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_affectedFoundMessages extends TLObject {
        public static final int constructor = 0xef8d3e6c;

        public int pts;
        public int pts_count;
        public int offset;
        public ArrayList<Integer> messages = new ArrayList<>();

        public static TL_messages_affectedFoundMessages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_affectedFoundMessages result = TL_messages_affectedFoundMessages.constructor != constructor ? null : new TL_messages_affectedFoundMessages();
            return TLdeserialize(TL_messages_affectedFoundMessages.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            pts = stream.readInt32(exception);
            pts_count = stream.readInt32(exception);
            offset = stream.readInt32(exception);
            messages = Vector.deserializeInt(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(pts);
            stream.writeInt32(pts_count);
            stream.writeInt32(offset);
            Vector.serializeInt(stream, messages);
        }
    }

    public static class TL_channelAdminLogEvent extends TLObject {
        public static final int constructor = 0x1fad68cd;

        public long id;
        public int date;
        public long user_id;
        public ChannelAdminLogEventAction action;

        public static TL_channelAdminLogEvent TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channelAdminLogEvent result = TL_channelAdminLogEvent.constructor != constructor ? null : new TL_channelAdminLogEvent();
            return TLdeserialize(TL_channelAdminLogEvent.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            date = stream.readInt32(exception);
            user_id = stream.readInt64(exception);
            action = ChannelAdminLogEventAction.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt32(date);
            stream.writeInt64(user_id);
            action.serializeToStream(stream);
        }
    }

    public static abstract class messages_FavedStickers extends TLObject {
        public long hash;
        public ArrayList<TL_stickerPack> packs = new ArrayList<>();
        public ArrayList<Document> stickers = new ArrayList<>();

        public static messages_FavedStickers TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_FavedStickers result = null;
            switch (constructor) {
                case 0x9e8fa6d3:
                    result = new TL_messages_favedStickersNotModified();
                    break;
                case 0x2cb51097:
                    result = new TL_messages_favedStickers();
                    break;
            }
            return TLdeserialize(messages_FavedStickers.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_favedStickersNotModified extends messages_FavedStickers {
        public static final int constructor = 0x9e8fa6d3;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_favedStickers extends messages_FavedStickers {
        public static final int constructor = 0x2cb51097;

        public void readParams(InputSerializedData stream, boolean exception) {
            hash = stream.readInt64(exception);
            packs = Vector.deserialize(stream, TL_stickerPack::TLdeserialize, exception);
            stickers = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(hash);
            Vector.serialize(stream, packs);
            Vector.serialize(stream, stickers);
        }
    }

    public static class TL_langPackLanguage extends TLObject {
        public static final int constructor = 0xeeca5ce3;

        public int flags;
        public boolean official;
        public boolean rtl;
        public String name;
        public String native_name;
        public String lang_code;
        public String base_lang_code;
        public String plural_code;
        public int strings_count;
        public int translated_count;
        public String translations_url;

        public static TL_langPackLanguage TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_langPackLanguage result = TL_langPackLanguage.constructor != constructor ? null : new TL_langPackLanguage();
            return TLdeserialize(TL_langPackLanguage.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            official = hasFlag(flags, FLAG_0);
            rtl = hasFlag(flags, FLAG_2);
            name = stream.readString(exception);
            native_name = stream.readString(exception);
            lang_code = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                base_lang_code = stream.readString(exception);
            }
            plural_code = stream.readString(exception);
            strings_count = stream.readInt32(exception);
            translated_count = stream.readInt32(exception);
            translations_url = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, official);
            flags = setFlag(flags, FLAG_2, rtl);
            stream.writeInt32(flags);
            stream.writeString(name);
            stream.writeString(native_name);
            stream.writeString(lang_code);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(base_lang_code);
            }
            stream.writeString(plural_code);
            stream.writeInt32(strings_count);
            stream.writeInt32(translated_count);
            stream.writeString(translations_url);
        }
    }

    public static class TL_chatInviteImporter extends TLObject {
        public static final int constructor = 0x8c5adfd9;

        public int flags;
        public boolean requested;
        public long user_id;
        public int date;
        public String about;
        public long approved_by;
        public boolean via_chatlist;

        public static TL_chatInviteImporter TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_chatInviteImporter result = TL_chatInviteImporter.constructor != constructor ? null : new TL_chatInviteImporter();
            return TLdeserialize(TL_chatInviteImporter.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            requested = hasFlag(flags, FLAG_0);
            via_chatlist = hasFlag(flags, FLAG_3);
            user_id = stream.readInt64(exception);
            date = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_2)) {
                about = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                approved_by = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, requested);
            flags = setFlag(flags, FLAG_3, via_chatlist);
            stream.writeInt32(flags);
            stream.writeInt64(user_id);
            stream.writeInt32(date);
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(about);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt64(approved_by);
            }
        }
    }

    public static abstract class SendMessageAction extends TLObject {
        public int progress;

        public static SendMessageAction TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(SendMessageAction.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static SendMessageAction fromConstructor(int constructor) {
            switch (constructor) {
                case TL_sendMessageTextDraftAction.constructor:
                    return new TL_sendMessageTextDraftAction();
                case TL_sendMessageRichMessageDraftAction.constructor:
                    return new TL_sendMessageRichMessageDraftAction();
                case TL_sendMessageStopDraftAction.constructor:
                    return new TL_sendMessageStopDraftAction();
                case TL_sendMessageGamePlayAction.constructor:
                    return new TL_sendMessageGamePlayAction();
                case TL_sendMessageRecordAudioAction.constructor:
                    return new TL_sendMessageRecordAudioAction();
                case TL_sendMessageUploadVideoAction_old.constructor:
                    return new TL_sendMessageUploadVideoAction_old();
                case TL_sendMessageUploadAudioAction_old.constructor:
                    return new TL_sendMessageUploadAudioAction_old();
                case TL_sendMessageUploadAudioAction.constructor:
                    return new TL_sendMessageUploadAudioAction();
                case TL_sendMessageUploadPhotoAction.constructor:
                    return new TL_sendMessageUploadPhotoAction();
                case TL_sendMessageUploadDocumentAction_old.constructor:
                    return new TL_sendMessageUploadDocumentAction_old();
                case TL_sendMessageUploadVideoAction.constructor:
                    return new TL_sendMessageUploadVideoAction();
                case TL_sendMessageCancelAction.constructor:
                    return new TL_sendMessageCancelAction();
                case TL_sendMessageGeoLocationAction.constructor:
                    return new TL_sendMessageGeoLocationAction();
                case TL_sendMessageChooseContactAction.constructor:
                    return new TL_sendMessageChooseContactAction();
                case TL_sendMessageChooseStickerAction.constructor:
                    return new TL_sendMessageChooseStickerAction();
                case TL_sendMessageRecordRoundAction.constructor:
                    return new TL_sendMessageRecordRoundAction();
                case TL_sendMessageUploadRoundAction.constructor:
                    return new TL_sendMessageUploadRoundAction();
                case TL_sendMessageUploadRoundAction_layer66.constructor:
                    return new TL_sendMessageUploadRoundAction_layer66();
                case TL_sendMessageTypingAction.constructor:
                    return new TL_sendMessageTypingAction();
                case TL_sendMessageHistoryImportAction.constructor:
                    return new TL_sendMessageHistoryImportAction();
                case TL_sendMessageUploadPhotoAction_old.constructor:
                    return new TL_sendMessageUploadPhotoAction_old();
                case TL_sendMessageUploadDocumentAction.constructor:
                    return new TL_sendMessageUploadDocumentAction();
                case TL_speakingInGroupCallAction.constructor:
                    return new TL_speakingInGroupCallAction();
                case TL_sendMessageRecordVideoAction.constructor:
                    return new TL_sendMessageRecordVideoAction();
                case TL_sendMessageEmojiInteraction.constructor:
                    return new TL_sendMessageEmojiInteraction();
                case TL_sendMessageEmojiInteractionSeen.constructor:
                    return new TL_sendMessageEmojiInteractionSeen();
                default:
                    return null;
            }
        }
    }

    public static class TL_sendMessageTextDraftAction extends SendMessageAction {
        public static final int constructor = 0x3630b85a;

        public boolean can_stop;
        public boolean keep_on_stop;
        public long random_id;
        public TL_textWithEntities text;

        public int flags;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_stop = hasFlag(flags, FLAG_0);
            keep_on_stop = hasFlag(flags, FLAG_1);
            random_id = stream.readInt64(exception);
            text = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, can_stop);
            flags = setFlag(flags, FLAG_1, keep_on_stop);
            stream.writeInt32(flags);
            stream.writeInt64(random_id);
            text.serializeToStream(stream);
        }
    }

    public static class TL_sendMessageRichMessageDraftAction extends SendMessageAction {
        public static final int constructor = 0x52564893;

        public boolean can_stop;
        public boolean keep_on_stop;
        public long random_id;
        public TL_iv.RichMessage rich_message;

        public int flags;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_stop = hasFlag(flags, FLAG_0);
            keep_on_stop = hasFlag(flags, FLAG_1);
            random_id = stream.readInt64(exception);
            rich_message = TL_iv.RichMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, can_stop);
            flags = setFlag(flags, FLAG_1, keep_on_stop);
            stream.writeInt32(flags);
            stream.writeInt64(random_id);
            rich_message.serializeToStream(stream);
        }
    }

    public static class TL_sendMessageStopDraftAction extends SendMessageAction {
        public static final int constructor = 0xfbf902b0;

        public long random_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            random_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(random_id);
        }
    }

    public static class TL_sendMessageGamePlayAction extends SendMessageAction {
        public static final int constructor = 0xdd6a8f48;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageRecordAudioAction extends SendMessageAction {
        public static final int constructor = 0xd52f73f7;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadVideoAction_old extends TL_sendMessageUploadVideoAction {
        public static final int constructor = 0x92042ff7;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageEmojiInteraction extends SendMessageAction {
        public static final int constructor = 0x25972bcb;

        public String emoticon;
        public int msg_id;
        public TL_dataJSON interaction;

        public void readParams(InputSerializedData stream, boolean exception) {
            emoticon = stream.readString(exception);
            msg_id = stream.readInt32(exception);
            interaction = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(emoticon);
            stream.writeInt32(msg_id);
            interaction.serializeToStream(stream);
        }
    }

    public static class TL_sendMessageUploadAudioAction_old extends TL_sendMessageUploadAudioAction {
        public static final int constructor = 0xe6ac8a6f;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadAudioAction extends SendMessageAction {
        public static final int constructor = 0xf351d7ab;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadPhotoAction extends SendMessageAction {
        public static final int constructor = 0xd1d34a26;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadDocumentAction_old extends TL_sendMessageUploadDocumentAction {
        public static final int constructor = 0x8faee98e;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadVideoAction extends SendMessageAction {
        public static final int constructor = 0xe9763aec;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageCancelAction extends SendMessageAction {
        public static final int constructor = 0xfd5ec8f5;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageGeoLocationAction extends SendMessageAction {
        public static final int constructor = 0x176f8ba1;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageChooseContactAction extends SendMessageAction {
        public static final int constructor = 0x628cbc6f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageChooseStickerAction extends SendMessageAction {
        public static final int constructor = 0xb05ac6b1;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageRecordRoundAction extends SendMessageAction {
        public static final int constructor = 0x88f27fbc;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadRoundAction extends SendMessageAction {
        public static final int constructor = 0x243e1c66;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadRoundAction_layer66 extends TL_sendMessageUploadRoundAction {
        public static final int constructor = 0xbb718624;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageEmojiInteractionSeen extends SendMessageAction {
        public static final int constructor = 0xb665902e;

        public String emoticon;

        public void readParams(InputSerializedData stream, boolean exception) {
            emoticon = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(emoticon);
        }
    }

    public static class TL_sendMessageTypingAction extends SendMessageAction {
        public static final int constructor = 0x16bf744e;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageHistoryImportAction extends SendMessageAction {
        public static final int constructor = 0xdbda9246;

        public int progress;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_sendMessageUploadPhotoAction_old extends TL_sendMessageUploadPhotoAction {
        public static final int constructor = 0x990a3c1a;

        public void readParams(InputSerializedData stream, boolean exception) {
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageUploadDocumentAction extends SendMessageAction {
        public static final int constructor = 0xaa0cd9e4;

        public void readParams(InputSerializedData stream, boolean exception) {
            progress = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(progress);
        }
    }

    public static class TL_speakingInGroupCallAction extends SendMessageAction {
        public static final int constructor = 0xd92c2285;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_sendMessageRecordVideoAction extends SendMessageAction {
        public static final int constructor = 0xa187d66f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class auth_SentCodeType extends TLObject {
        public int flags;
        public String url;
        public int length;
        public String pattern;
        public String prefix;
        public boolean apple_signin_allowed;
        public boolean google_signin_allowed;
        public String email_pattern;
        public int next_phone_login_date;
        public byte[] nonce;
        public long play_integrity_project_id;
        public byte[] play_integrity_nonce;
        public String receipt;
        public int push_timeout;
        public int reset_available_period;
        public int reset_pending_date;
        public String beginning;
        public boolean verifiedFirebase; //custom

        public static auth_SentCodeType TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            auth_SentCodeType result = null;
            switch (constructor) {
                case 0x3dbb5986:
                    result = new TL_auth_sentCodeTypeApp();
                    break;
                case 0x5353e5a7:
                    result = new TL_auth_sentCodeTypeCall();
                    break;
                case 0xf450f59b:
                    result = new TL_auth_sentCodeTypeEmailCode();
                    break;
                case 0xa5491dea:
                    result = new TL_auth_sentCodeTypeSetUpEmailRequired();
                    break;
                case 0xab03c6d9:
                    result = new TL_auth_sentCodeTypeFlashCall();
                    break;
                case 0x82006484:
                    result = new TL_auth_sentCodeTypeMissedCall();
                    break;
                case 0xc000bba2:
                    result = new TL_auth_sentCodeTypeSms();
                    break;
                case 0xd9565c39:
                    result = new TL_auth_sentCodeTypeFragmentSms();
                    break;
                case 0x9fd736:
                    result = new TL_auth_sentCodeTypeFirebaseSms();
                    break;
                case 0xa416ac81:
                    result = new TL_auth_sentCodeTypeSmsWord();
                    break;
                case 0xb37794af:
                    result = new TL_auth_sentCodeTypeSmsPhrase();
                    break;
            }
            return TLdeserialize(auth_SentCodeType.class, result, stream, constructor, exception);
        }
    }

    public static class TL_auth_sentCodeTypeApp extends auth_SentCodeType {
        public static final int constructor = 0x3dbb5986;

        public void readParams(InputSerializedData stream, boolean exception) {
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeCall extends auth_SentCodeType {
        public static final int constructor = 0x5353e5a7;

        public void readParams(InputSerializedData stream, boolean exception) {
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeEmailCode extends auth_SentCodeType {
        public static final int constructor = 0xf450f59b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            apple_signin_allowed = hasFlag(flags, FLAG_0);
            google_signin_allowed = hasFlag(flags, FLAG_1);
            email_pattern = stream.readString(exception);
            length = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                reset_available_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                reset_pending_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, apple_signin_allowed);
            flags = setFlag(flags, FLAG_1, google_signin_allowed);
            stream.writeInt32(flags);
            stream.writeString(email_pattern);
            stream.writeInt32(length);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(reset_available_period);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(reset_pending_date);
            }
        }
    }

    public static class TL_auth_sentCodeTypeSetUpEmailRequired extends auth_SentCodeType {
        public static final int constructor = 0xa5491dea;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            apple_signin_allowed = hasFlag(flags, FLAG_0);
            google_signin_allowed = hasFlag(flags, FLAG_1);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, apple_signin_allowed);
            flags = setFlag(flags, FLAG_1, google_signin_allowed);
            stream.writeInt32(flags);
        }
    }

    public static class TL_auth_sentCodeTypeFlashCall extends auth_SentCodeType {
        public static final int constructor = 0xab03c6d9;

        public void readParams(InputSerializedData stream, boolean exception) {
            pattern = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(pattern);
        }
    }

    public static class TL_auth_sentCodeTypeMissedCall extends auth_SentCodeType {
        public static final int constructor = 0x82006484;

        public void readParams(InputSerializedData stream, boolean exception) {
            prefix = stream.readString(exception);
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(prefix);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeSms extends auth_SentCodeType {
        public static final int constructor = 0xc000bba2;

        public void readParams(InputSerializedData stream, boolean exception) {
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeFragmentSms extends auth_SentCodeType {
        public static final int constructor = 0xd9565c39;

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeFirebaseSms extends auth_SentCodeType {
        public static final int constructor = 0x9fd736;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                nonce = stream.readByteArray(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                play_integrity_project_id = stream.readInt64(exception);
                play_integrity_nonce = stream.readByteArray(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                receipt = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                push_timeout = stream.readInt32(exception);
            }
            length = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeByteArray(nonce);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt64(play_integrity_project_id);
                stream.writeByteArray(play_integrity_nonce);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(receipt);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(push_timeout);
            }
            stream.writeInt32(length);
        }
    }

    public static class TL_auth_sentCodeTypeSmsWord extends auth_SentCodeType {
        public static final int constructor = 0xa416ac81;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                beginning = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(beginning);
            }
        }
    }

    public static class TL_auth_sentCodeTypeSmsPhrase extends auth_SentCodeType {
        public static final int constructor = 0xb37794af;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                beginning = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(beginning);
            }
        }
    }

    public static abstract class messages_StickerSetInstallResult extends TLObject {
        public ArrayList<StickerSetCovered> sets = new ArrayList<>();

        public static messages_StickerSetInstallResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_StickerSetInstallResult result = null;
            switch (constructor) {
                case 0x38641628:
                    result = new TL_messages_stickerSetInstallResultSuccess();
                    break;
                case 0x35e410a8:
                    result = new TL_messages_stickerSetInstallResultArchive();
                    break;
            }
            return TLdeserialize(messages_StickerSetInstallResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_stickerSetInstallResultSuccess extends messages_StickerSetInstallResult {
        public static final int constructor = 0x38641628;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_stickerSetInstallResultArchive extends messages_StickerSetInstallResult {
        public static final int constructor = 0x35e410a8;

        public void readParams(InputSerializedData stream, boolean exception) {
            sets = Vector.deserialize(stream, StickerSetCovered::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, sets);
        }
    }

    public static class PeerSettings extends TLObject {

        public int flags;
        public boolean report_spam;
        public boolean add_contact;
        public boolean block_contact;
        public boolean share_contact;
        public boolean need_contacts_exception;
        public boolean report_geo;
        public boolean autoarchived;
        public boolean invite_members;
        public boolean request_chat_broadcast;
        public int geo_distance;
        public String request_chat_title;
        public int request_chat_date;
        public boolean business_bot_paused;
        public boolean business_bot_can_reply;
        public long business_bot_id;
        public String business_bot_manage_url;
        public long charge_paid_message_stars;
        public String registration_month;
        public String phone_country;
        public int name_change_date;
        public int photo_change_date;

        public static PeerSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PeerSettings result = null;
            switch (constructor) {
                case TL_peerSettings.constructor:
                    result = new TL_peerSettings();
                    break;
                case TL_peerSettings_layer199.constructor:
                    result = new TL_peerSettings_layer199();
                    break;
                case TL_peerSettings_layer176.constructor:
                    result = new TL_peerSettings_layer176();
                    break;
            }
            return TLdeserialize(PeerSettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_peerSettings extends PeerSettings {
        public static final int constructor = 0xf47741f7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            report_spam = hasFlag(flags, FLAG_0);
            add_contact = hasFlag(flags, FLAG_1);
            block_contact = hasFlag(flags, FLAG_2);
            share_contact = hasFlag(flags, FLAG_3);
            need_contacts_exception = hasFlag(flags, FLAG_4);
            report_geo = hasFlag(flags, FLAG_5);
            autoarchived = hasFlag(flags, FLAG_7);
            invite_members = hasFlag(flags, FLAG_8);
            request_chat_broadcast = hasFlag(flags, FLAG_10);
            business_bot_paused = hasFlag(flags, FLAG_11);
            business_bot_can_reply = hasFlag(flags, FLAG_12);
            if (hasFlag(flags, FLAG_6)) {
                geo_distance = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_manage_url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                charge_paid_message_stars = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                registration_month = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                phone_country = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                name_change_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                photo_change_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, report_spam);
            flags = setFlag(flags, FLAG_1, add_contact);
            flags = setFlag(flags, FLAG_2, block_contact);
            flags = setFlag(flags, FLAG_3, share_contact);
            flags = setFlag(flags, FLAG_4, need_contacts_exception);
            flags = setFlag(flags, FLAG_5, report_geo);
            flags = setFlag(flags, FLAG_7, autoarchived);
            flags = setFlag(flags, FLAG_8, invite_members);
            flags = setFlag(flags, FLAG_10, request_chat_broadcast);
            flags = setFlag(flags, FLAG_11, business_bot_paused);
            flags = setFlag(flags, FLAG_12, business_bot_can_reply);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(geo_distance);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeString(request_chat_title);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(request_chat_date);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt64(business_bot_id);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeString(business_bot_manage_url);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(charge_paid_message_stars);
            }
            if (hasFlag(flags, FLAG_15)) {
                stream.writeString(registration_month);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(phone_country);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(name_change_date);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(photo_change_date);
            }
        }
    }

    public static class TL_peerSettings_layer199 extends TL_peerSettings {
        public static final int constructor = 0xacd66c5e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            report_spam = hasFlag(flags, FLAG_0);
            add_contact = hasFlag(flags, FLAG_1);
            block_contact = hasFlag(flags, FLAG_2);
            share_contact = hasFlag(flags, FLAG_3);
            need_contacts_exception = hasFlag(flags, FLAG_4);
            report_geo = hasFlag(flags, FLAG_5);
            autoarchived = hasFlag(flags, FLAG_7);
            invite_members = hasFlag(flags, FLAG_8);
            request_chat_broadcast = hasFlag(flags, FLAG_10);
            business_bot_paused = hasFlag(flags, FLAG_11);
            business_bot_can_reply = hasFlag(flags, FLAG_12);
            if (hasFlag(flags, FLAG_6)) {
                geo_distance = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                business_bot_manage_url = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, report_spam);
            flags = setFlag(flags, FLAG_1, add_contact);
            flags = setFlag(flags, FLAG_2, block_contact);
            flags = setFlag(flags, FLAG_3, share_contact);
            flags = setFlag(flags, FLAG_4, need_contacts_exception);
            flags = setFlag(flags, FLAG_5, report_geo);
            flags = setFlag(flags, FLAG_7, autoarchived);
            flags = setFlag(flags, FLAG_8, invite_members);
            flags = setFlag(flags, FLAG_10, request_chat_broadcast);
            flags = setFlag(flags, FLAG_11, business_bot_paused);
            flags = setFlag(flags, FLAG_12, business_bot_can_reply);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(geo_distance);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeString(request_chat_title);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(request_chat_date);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt64(business_bot_id);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeString(business_bot_manage_url);
            }
        }
    }

    public static class TL_peerSettings_layer176 extends TL_peerSettings {
        public static final int constructor = 0xa518110d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            report_spam = hasFlag(flags, FLAG_0);
            add_contact = hasFlag(flags, FLAG_1);
            block_contact = hasFlag(flags, FLAG_2);
            share_contact = hasFlag(flags, FLAG_3);
            need_contacts_exception = hasFlag(flags, FLAG_4);
            report_geo = hasFlag(flags, FLAG_5);
            autoarchived = hasFlag(flags, FLAG_7);
            invite_members = hasFlag(flags, FLAG_8);
            request_chat_broadcast = hasFlag(flags, FLAG_10);
            if (hasFlag(flags, FLAG_6)) {
                geo_distance = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                request_chat_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, report_spam);
            flags = setFlag(flags, FLAG_1, add_contact);
            flags = setFlag(flags, FLAG_2, block_contact);
            flags = setFlag(flags, FLAG_3, share_contact);
            flags = setFlag(flags, FLAG_4, need_contacts_exception);
            flags = setFlag(flags, FLAG_5, report_geo);
            flags = setFlag(flags, FLAG_7, autoarchived);
            flags = setFlag(flags, FLAG_8, invite_members);
            flags = setFlag(flags, FLAG_10, request_chat_broadcast);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(geo_distance);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeString(request_chat_title);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(request_chat_date);
            }
        }
    }

    public static class TL_readParticipantDate extends TLObject {
        public static final int constructor = 0x4a4ff172;

        public long user_id;
        public int date;

        public static TL_readParticipantDate TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_readParticipantDate result = TL_readParticipantDate.constructor != constructor ? null : new TL_readParticipantDate();
            return TLdeserialize(TL_readParticipantDate.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            user_id = stream.readInt64(exception);
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(user_id);
            stream.writeInt32(date);
        }
    }

    public static abstract class InputDialogPeer extends TLObject {

        public static InputDialogPeer TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(InputDialogPeer.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static InputDialogPeer fromConstructor(int constructor) {
            switch (constructor) {
                case TL_inputDialogPeer.constructor:
                    return new TL_inputDialogPeer();
                case TL_inputDialogPeerFolder.constructor:
                    return new TL_inputDialogPeerFolder();
                case TL_inputDialogPeerCommunity.constructor:
                    return new TL_inputDialogPeerCommunity();
                default:
                    return null;
            }
        }
    }

    public static class TL_inputDialogPeer extends InputDialogPeer {
        public static final int constructor = 0xfcaafeb7;

        public InputPeer peer;

        public void readParams(InputSerializedData stream, boolean exception) {
            peer = InputPeer.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
        }
    }

    public static class TL_inputDialogPeerFolder extends InputDialogPeer {
        public static final int constructor = 0x64600527;

        public int folder_id;

        public void readParams(InputSerializedData stream, boolean exception) {
            folder_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(folder_id);
        }
    }

    public static class TL_inputDialogPeerCommunity extends InputDialogPeer {
        public static final int constructor = 0x69EF72C4;

        public InputChannel community;

        public void readParams(InputSerializedData stream, boolean exception) {
            community = InputChannel.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            community.serializeToStream(stream);
        }
    }

    public static abstract class payments_PaymentResult extends TLObject {

        public static payments_PaymentResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            payments_PaymentResult result = null;
            switch (constructor) {
                case 0xd8411139:
                    result = new TL_payments_paymentVerificationNeeded();
                    break;
                case 0x4e5f810d:
                    result = new TL_payments_paymentResult();
                    break;
            }
            return TLdeserialize(payments_PaymentResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_payments_paymentVerificationNeeded extends payments_PaymentResult {
        public static final int constructor = 0xd8411139;

        public String url;

        public void readParams(InputSerializedData stream, boolean exception) {
            url = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
        }
    }

    public static class TL_payments_paymentResult extends payments_PaymentResult {
        public static final int constructor = 0x4e5f810d;

        public Updates updates;

        public void readParams(InputSerializedData stream, boolean exception) {
            updates = Updates.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            updates.serializeToStream(stream);
        }
    }

    public static class TL_channels_adminLogResults extends TLObject {
        public static final int constructor = 0xed8af74d;

        public ArrayList<TL_channelAdminLogEvent> events = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_channels_adminLogResults TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_channels_adminLogResults result = TL_channels_adminLogResults.constructor != constructor ? null : new TL_channels_adminLogResults();
            return TLdeserialize(TL_channels_adminLogResults.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            events = Vector.deserialize(stream, TL_channelAdminLogEvent::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, events);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_inputPhoneContact extends TLObject {
        public static final int constructor = 0x6a1dc4be;

        public int flags;
        public long client_id;
        public String phone;
        public String first_name;
        public String last_name;
        public TL_textWithEntities note;

        public static TL_inputPhoneContact TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_inputPhoneContact result = TL_inputPhoneContact.constructor != constructor ? null : new TL_inputPhoneContact();
            return TLdeserialize(TL_inputPhoneContact.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            client_id = stream.readInt64(exception);
            phone = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                note = TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(client_id);
            stream.writeString(phone);
            stream.writeString(first_name);
            stream.writeString(last_name);
            if (hasFlag(flags, FLAG_0)) {
                note.serializeToStream(stream);
            }
        }
    }

    public static abstract class ThemeSettings extends TLObject {

        public int flags;
        public boolean message_colors_animated;
        public BaseTheme base_theme;
        public int accent_color;
        public int outbox_accent_color;
        public ArrayList<Integer> message_colors = new ArrayList<>();
        public WallPaper wallpaper;

        public static ThemeSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ThemeSettings result = null;
            switch (constructor) {
                case 0xfa58b6d4:
                    result = new TL_themeSettings();
                    break;
                case 0x8db4e76c:
                    result = new TL_themeSettings_layer132();
                    break;
                case 0x9c14984a:
                    result = new TL_themeSettings_layer131();
                    break;
            }
            return TLdeserialize(ThemeSettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_themeSettings extends ThemeSettings {
        public static final int constructor = 0xfa58b6d4;

        public static TL_themeSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_themeSettings result = TL_themeSettings.constructor != constructor ? null : new TL_themeSettings();
            return TLdeserialize(TL_themeSettings.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            message_colors_animated = hasFlag(flags, FLAG_2);
            base_theme = BaseTheme.TLdeserialize(stream, stream.readInt32(exception), exception);
            accent_color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_3)) {
                outbox_accent_color = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                message_colors = Vector.deserializeInt(stream, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper = WallPaper.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, message_colors_animated);
            stream.writeInt32(flags);
            base_theme.serializeToStream(stream);
            stream.writeInt32(accent_color);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(outbox_accent_color);
            }
            if (hasFlag(flags, FLAG_0)) {
                Vector.serializeInt(stream, message_colors);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper.serializeToStream(stream);
            }
        }
    }

    public static class TL_themeSettings_layer132 extends ThemeSettings {
        public static final int constructor = 0x8db4e76c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            message_colors_animated = hasFlag(flags, FLAG_2);
            base_theme = BaseTheme.TLdeserialize(stream, stream.readInt32(exception), exception);
            accent_color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                message_colors = Vector.deserializeInt(stream, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper = WallPaper.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, message_colors_animated);
            stream.writeInt32(flags);
            base_theme.serializeToStream(stream);
            stream.writeInt32(accent_color);
            if (hasFlag(flags, FLAG_0)) {
                Vector.serializeInt(stream, message_colors);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper.serializeToStream(stream);
            }
        }
    }

    public static class TL_themeSettings_layer131 extends ThemeSettings {
        public static final int constructor = 0x9c14984a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            base_theme = BaseTheme.TLdeserialize(stream, stream.readInt32(exception), exception);
            accent_color = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                int color = stream.readInt32(exception);
                if (color != 0) {
                    message_colors.add(color);
                }
            }
            if (hasFlag(flags, FLAG_0)) {
                int color = stream.readInt32(exception);
                if (color != 0) {
                    message_colors.add(0, color);
                }
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper = WallPaper.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            base_theme.serializeToStream(stream);
            stream.writeInt32(accent_color);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(message_colors.size() > 1 ? message_colors.get(1) : 0);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(message_colors.size() > 0 ? message_colors.get(0) : 0);
            }
            if (hasFlag(flags, FLAG_1)) {
                wallpaper.serializeToStream(stream);
            }
        }
    }

    public static abstract class PrivacyRule extends TLObject {

        public static PrivacyRule TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PrivacyRule result = null;
            switch (constructor) {
                case 0xf888fa1a:
                    result = new TL_privacyValueDisallowContacts();
                    break;
                case 0xe4621141:
                    result = new TL_privacyValueDisallowUsers();
                    break;
                case 0x6b134e8e:
                    result = new TL_privacyValueAllowChatParticipants();
                    break;
                case 0x41c87565:
                    result = new TL_privacyValueDisallowChatParticipants();
                    break;
                case 0x65427b82:
                    result = new TL_privacyValueAllowAll();
                    break;
                case 0x8b73e763:
                    result = new TL_privacyValueDisallowAll();
                    break;
                case 0xb8905fb2:
                    result = new TL_privacyValueAllowUsers();
                    break;
                case 0xfffe1bac:
                    result = new TL_privacyValueAllowContacts();
                    break;
                case 0xf7e8d89b:
                    result = new TL_privacyValueAllowCloseFriends();
                    break;
                case 0xece9814b:
                    result = new TL_privacyValueAllowPremium();
                    break;
                case TL_privacyValueAllowBots.constructor:
                    result = new TL_privacyValueAllowBots();
                    break;
                case TL_privacyValueDisallowBots.constructor:
                    result = new TL_privacyValueDisallowBots();
                    break;
            }
            return TLdeserialize(PrivacyRule.class, result, stream, constructor, exception);
        }
    }

    public static class TL_privacyValueDisallowContacts extends PrivacyRule {
        public static final int constructor = 0xf888fa1a;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueDisallowUsers extends PrivacyRule {
        public static final int constructor = 0xe4621141;

        public ArrayList<Long> users = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            users = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, users);
        }
    }

    public static class TL_privacyValueAllowChatParticipants extends PrivacyRule {
        public static final int constructor = 0x6b134e8e;

        public ArrayList<Long> chats = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            chats = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, chats);
        }
    }

    public static class TL_privacyValueDisallowChatParticipants extends PrivacyRule {
        public static final int constructor = 0x41c87565;

        public ArrayList<Long> chats = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            chats = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, chats);
        }
    }

    public static class TL_privacyValueAllowAll extends PrivacyRule {
        public static final int constructor = 0x65427b82;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueDisallowAll extends PrivacyRule {
        public static final int constructor = 0x8b73e763;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowUsers extends PrivacyRule {
        public static final int constructor = 0xb8905fb2;

        public ArrayList<Long> users = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            users = Vector.deserializeLong(stream, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serializeLong(stream, users);
        }
    }

    public static class TL_privacyValueAllowContacts extends PrivacyRule {
        public static final int constructor = 0xfffe1bac;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowCloseFriends extends PrivacyRule {
        public static final int constructor = 0xf7e8d89b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowPremium extends PrivacyRule {
        public static final int constructor = 0xece9814b;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueAllowBots extends PrivacyRule {
        public static final int constructor = 0x21461b5d;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_privacyValueDisallowBots extends PrivacyRule {
        public static final int constructor = 0xf6a5f82f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messageMediaUnsupported_old extends TL_messageMediaUnsupported {
        public static final int constructor = 0x29632a36;

        public void readParams(InputSerializedData stream, boolean exception) {
            bytes = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(bytes);
        }
    }

    public static class TL_messageMediaAudio_layer45 extends MessageMedia {
        public static final int constructor = 0xc6b68300;

        public void readParams(InputSerializedData stream, boolean exception) {
            audio_unused = Audio.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            audio_unused.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaPhoto_old extends TL_messageMediaPhoto {
        public static final int constructor = 0xc8c45a2a;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo.serializeToStream(stream);
        }
    }

    public static class MessageExtendedMedia extends TLObject {

        public String attachPath; // custom
        public float downloadProgress, uploadProgress; // custom

        public static MessageExtendedMedia TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            MessageExtendedMedia result = null;
            switch (constructor) {
                case 0xad628cc8:
                    result = new TL_messageExtendedMediaPreview();
                    break;
                case 0xee479c64:
                    result = new TL_messageExtendedMedia();
                    break;
            }
            return TLdeserialize(MessageExtendedMedia.class, result, stream, constructor, exception);
        }

    }

    public static class TL_messageExtendedMediaPreview extends MessageExtendedMedia {
        public static final int constructor = 0xad628cc8;

        public int flags;
        public int w;
        public int h;
        public PhotoSize thumb;
        public int video_duration;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                w = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                h = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                video_duration = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(w);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(h);
            }
            if (hasFlag(flags, FLAG_1)) {
                thumb.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(video_duration);
            }
        }
    }

    public static class TL_messageExtendedMedia extends MessageExtendedMedia {
        public static final int constructor = 0xee479c64;

        public MessageMedia media;

        public void readParams(InputSerializedData stream, boolean exception) {
            media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            media.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaInvoice extends MessageMedia {
        public static final int constructor = 0xf6a548d3;

        public WebDocument webPhoto;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            shipping_address_requested = hasFlag(flags, FLAG_1);
            test = hasFlag(flags, FLAG_3);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                webPhoto = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                receipt_msg_id = stream.readInt32(exception);
            }
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            start_param = stream.readString(exception);
            if (hasFlag(flags, FLAG_4)) {
                extended_media.clear();
                extended_media.add(MessageExtendedMedia.TLdeserialize(stream, stream.readInt32(exception), exception));
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, shipping_address_requested);
            flags = setFlag(flags, FLAG_3, test);
            stream.writeInt32(flags);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_0)) {
                webPhoto.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(receipt_msg_id);
            }
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(start_param);
            if (hasFlag(flags, FLAG_4)) {
                extended_media.get(0).serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaInvoice_layer145 extends TL_messageMediaInvoice {
        public static final int constructor = 0x84551347;

        public WebDocument photo;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            shipping_address_requested = hasFlag(flags, FLAG_1);
            test = hasFlag(flags, FLAG_3);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                receipt_msg_id = stream.readInt32(exception);
            }
            currency = stream.readString(exception);
            total_amount = stream.readInt64(exception);
            start_param = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, shipping_address_requested);
            flags = setFlag(flags, FLAG_3, test);
            stream.writeInt32(flags);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(receipt_msg_id);
            }
            stream.writeString(currency);
            stream.writeInt64(total_amount);
            stream.writeString(start_param);
        }
    }

    public static class TL_messageMediaUnsupported extends MessageMedia {
        public static final int constructor = 0x9f84f49e;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messageMediaEmpty extends MessageMedia {
        public static final int constructor = 0x3ded6320;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messageMediaVenue extends MessageMedia {
        public static final int constructor = 0x2ec0533f;

        public String icon; //custom
        public String emoji; //custom
        public long query_id; //custom
        public String result_id; //custom
        public TL_stories.TL_geoPointAddress geoAddress; //custom

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            title = stream.readString(exception);
            address = stream.readString(exception);
            provider = stream.readString(exception);
            venue_id = stream.readString(exception);
            venue_type = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeString(title);
            stream.writeString(address);
            stream.writeString(provider);
            stream.writeString(venue_id);
            stream.writeString(venue_type);
        }
    }

    public static class TL_messageMediaVenue_layer71 extends MessageMedia {
        public static final int constructor = 0x7912b71f;

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            title = stream.readString(exception);
            address = stream.readString(exception);
            provider = stream.readString(exception);
            venue_id = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeString(title);
            stream.writeString(address);
            stream.writeString(provider);
            stream.writeString(venue_id);
        }
    }

    public static class TL_messageMediaVideo_old extends TL_messageMediaVideo_layer45 {
        public static final int constructor = 0xa2d24290;

        public void readParams(InputSerializedData stream, boolean exception) {
            video_unused = Video.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            video_unused.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaDocument extends MessageMedia {
        public static final int constructor = 0x52d8ccd9;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            live_photo = hasFlag(flags, FLAG_11);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                video_timestamp = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            flags = setFlag(flags, FLAG_11, live_photo);
            flags = setFlag(flags, FLAG_12, webpage != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                Vector.serialize(stream, alt_documents);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_10)) {
                stream.writeInt32(video_timestamp);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer197_2 extends TL_messageMediaDocument {
        public static final int constructor = 0xdbbdf614;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                Vector.serialize(stream, alt_documents);
            }
            if (hasFlag(flags, FLAG_9)) {
                video_cover.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer197 extends TL_messageMediaDocument {
        public static final int constructor = 0xdd570bd5;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                Vector.serialize(stream, alt_documents);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer187 extends TL_messageMediaDocument {
        public static final int constructor = 0x4cf4d72d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            video = hasFlag(flags, FLAG_6);
            round = hasFlag(flags, FLAG_7);
            voice = hasFlag(flags, FLAG_8);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents.add(Document.TLdeserialize(stream, stream.readInt32(exception), exception));
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_6, video);
            flags = setFlag(flags, FLAG_7, round);
            flags = setFlag(flags, FLAG_8, voice);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                alt_documents.get(0).serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer159 extends TL_messageMediaDocument {
        public static final int constructor = 0x9cb070d7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            nopremium = hasFlag(flags, FLAG_3);
            spoiler = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                document = new TL_documentEmpty();
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, nopremium);
            flags = setFlag(flags, FLAG_4, spoiler);
            flags = setFlag(flags, FLAG_0, document != null);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_layer74 extends TL_messageMediaDocument {
        public static final int constructor = 0x7c4414d3;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                document = new TL_documentEmpty();
            }
            if (hasFlag(flags, FLAG_1)) {
                captionLegacy = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(captionLegacy);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaDocument_old extends TL_messageMediaDocument {
        public static final int constructor = 0x2fda2204;

        public void readParams(InputSerializedData stream, boolean exception) {
            document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            document.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaDocument_layer68 extends TL_messageMediaDocument {
        public static final int constructor = 0xf3e02ea8;

        public void readParams(InputSerializedData stream, boolean exception) {
            document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            captionLegacy = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            document.serializeToStream(stream);
            stream.writeString(captionLegacy);
        }
    }

    public static class TL_messageMediaPhoto extends MessageMedia {
        public static final int constructor = 0xe216eb63;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            spoiler = hasFlag(flags, FLAG_3);
            live_photo = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                photo = new TL_photoEmpty();
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, spoiler);
            flags = setFlag(flags, FLAG_4, live_photo);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
            if (hasFlag(flags, FLAG_4)) {
                document.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaPhoto_layer223 extends TL_messageMediaPhoto {
        public static final int constructor = 0x695150d7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            spoiler = hasFlag(flags, FLAG_3);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                photo = new TL_photoEmpty();
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, spoiler);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_messageMediaPoll extends MessageMedia {
        public static final int constructor = 0x773f4e66;

        public Poll poll;
        public PollResults results;
        public MessageMedia attached_media;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            poll = Poll.TLdeserialize(stream, exception);
            results = PollResults.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                attached_media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, attached_media != null);
            stream.writeInt32(flags);
            poll.serializeToStream(stream);
            results.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                attached_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaPoll_layer223 extends TL_messageMediaPoll {
        public static final int constructor = 0x4bd6e798;

        public void readParams(InputSerializedData stream, boolean exception) {
            poll = Poll.TLdeserialize(stream, exception);
            results = PollResults.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            poll.serializeToStream(stream);
            results.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaPhoto_layer74 extends TL_messageMediaPhoto {
        public static final int constructor = 0xb5223b0f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            } else {
                photo = new TL_photoEmpty();
            }
            if (hasFlag(flags, FLAG_1)) {
                captionLegacy = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                ttl_seconds = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(captionLegacy);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(ttl_seconds);
            }
        }
    }

    public static class TL_inputMediaInvoice extends InputMedia {
        public static final int constructor = 0x405fef0d;

        public int flags;
        public String title;
        public String description;
        public TL_inputWebDocument photo;
        public TL_invoice invoice;
        public byte[] payload;
        public String provider;
        public TL_dataJSON provider_data;
        public String start_param;
        public InputMedia extend_media;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            title = stream.readString(exception);
            description = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = TL_inputWebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            invoice = TL_invoice.TLdeserialize(stream, stream.readInt32(exception), exception);
            payload = stream.readByteArray(exception);
            if (hasFlag(flags, FLAG_3)) {
                provider = stream.readString(exception);
            }
            provider_data = TL_dataJSON.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_1)) {
                start_param = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                extend_media = InputMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(title);
            stream.writeString(description);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            invoice.serializeToStream(stream);
            stream.writeByteArray(payload);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(provider);
            }
            provider_data.serializeToStream(stream);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(start_param);
            }
            if (hasFlag(flags, FLAG_2)) {
                extend_media.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaGeoLive extends MessageMedia {
        public static final int constructor = 0xb940c666;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                heading = stream.readInt32(exception);
            }
            period = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_1)) {
                proximity_notification_radius = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            geo.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(heading);
            }
            stream.writeInt32(period);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(proximity_notification_radius);
            }
        }
    }

    public static class TL_messageMediaGeoLive_layer119 extends TL_messageMediaGeoLive {
        public static final int constructor = 0x7c3c2609;

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            period = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeInt32(period);
        }
    }

    public static class TL_messageMediaGame extends MessageMedia {
        public static final int constructor = 0xfdb19008;

        public void readParams(InputSerializedData stream, boolean exception) {
            game = TL_game.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            game.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaContact_layer81 extends TL_messageMediaContact {
        public static final int constructor = 0x5e7d2f39;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            user_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeInt32((int) user_id);
        }
    }

    public static class TL_messageMediaPhoto_layer68 extends TL_messageMediaPhoto {
        public static final int constructor = 0x3d8ce53d;

        public void readParams(InputSerializedData stream, boolean exception) {
            photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            captionLegacy = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            photo.serializeToStream(stream);
            stream.writeString(captionLegacy);
        }
    }

    public static class TL_messageMediaVideo_layer45 extends MessageMedia {
        public static final int constructor = 0x5bcf1675;

        public void readParams(InputSerializedData stream, boolean exception) {
            video_unused = Video.TLdeserialize(stream, stream.readInt32(exception), exception);
            captionLegacy = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            video_unused.serializeToStream(stream);
            stream.writeString(captionLegacy);
        }
    }

    public static class TL_messageMediaContact_layer131 extends TL_messageMediaContact {
        public static final int constructor = 0xcbf24940;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            vcard = stream.readString(exception);
            user_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeString(vcard);
            stream.writeInt32((int) user_id);
        }
    }

    public static class TL_messageMediaContact extends MessageMedia {
        public static final int constructor = 0x70322949;

        public void readParams(InputSerializedData stream, boolean exception) {
            phone_number = stream.readString(exception);
            first_name = stream.readString(exception);
            last_name = stream.readString(exception);
            vcard = stream.readString(exception);
            user_id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeString(vcard);
            stream.writeInt64(user_id);
        }
    }

    public static class TL_messageMediaVideoStream extends MessageMedia {
        public static final int constructor = 0xca5cab89;

        public int flags;
        public InputGroupCall call;
        public boolean rtmp_stream;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            rtmp_stream = hasFlag(flags, FLAG_0);
            call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, rtmp_stream);
            stream.writeInt32(flags);
            call.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaDice extends MessageMedia {
        public static final int constructor = 0x8cbec07;

        public int value;
        public String emoticon;
        public TL_messages_emojiGameOutcome game_outcome;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            value = stream.readInt32(exception);
            emoticon = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                game_outcome = TL_messages_emojiGameOutcome.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt32(value);
            stream.writeString(emoticon);
            if (hasFlag(flags, FLAG_0)) {
                game_outcome.serializeToStream(stream);
            }
        }
    }

    public static class TL_messageMediaDice_layer220 extends TL_messageMediaDice {
        public static final int constructor = 0x3f7ee58b;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readInt32(exception);
            emoticon = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(value);
            stream.writeString(emoticon);
        }
    }

    public static class TL_messageMediaDice_layer111 extends TL_messageMediaDice {
        public static final int constructor = 0x638fe46b;

        public void readParams(InputSerializedData stream, boolean exception) {
            value = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(value);
        }
    }

    public static class TL_messageMediaGeo extends MessageMedia {
        public static final int constructor = 0x56e0d474;

        public void readParams(InputSerializedData stream, boolean exception) {
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaWebPage extends MessageMedia {
        public static final int constructor = 0xddf10c3b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            force_large_media = hasFlag(flags, FLAG_0);
            force_small_media = hasFlag(flags, FLAG_1);
            manual = hasFlag(flags, FLAG_3);
            safe = hasFlag(flags, FLAG_4);
            webpage = WebPage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, force_large_media);
            flags = setFlag(flags, FLAG_1, force_small_media);
            flags = setFlag(flags, FLAG_3, manual);
            flags = setFlag(flags, FLAG_4, safe);
            stream.writeInt32(flags);
            webpage.serializeToStream(stream);
        }
    }

    public static class TL_messageMediaWebPage_layer165 extends TL_messageMediaWebPage {
        public static final int constructor = 0xa32dd600;

        public void readParams(InputSerializedData stream, boolean exception) {
            webpage = WebPage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            webpage.serializeToStream(stream);
        }
    }

    public static abstract class LangPackString extends TLObject {
        public int flags;
        public String key;
        public String zero_value;
        public String one_value;
        public String two_value;
        public String few_value;
        public String many_value;
        public String other_value;
        public String value;

        public static LangPackString TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            LangPackString result = null;
            switch (constructor) {
                case 0x6c47ac9f:
                    result = new TL_langPackStringPluralized();
                    break;
                case 0xcad181f6:
                    result = new TL_langPackString();
                    break;
                case 0x2979eeb2:
                    result = new TL_langPackStringDeleted();
                    break;
            }
            return TLdeserialize(LangPackString.class, result, stream, constructor, exception);
        }
    }

    public static class TL_langPackStringPluralized extends LangPackString {
        public static final int constructor = 0x6c47ac9f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            key = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                zero_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                one_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                two_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                few_value = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                many_value = stream.readString(exception);
            }
            other_value = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(key);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeString(zero_value);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(one_value);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(two_value);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(few_value);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeString(many_value);
            }
            stream.writeString(other_value);
        }
    }

    public static class TL_langPackString extends LangPackString {
        public static final int constructor = 0xcad181f6;

        public void readParams(InputSerializedData stream, boolean exception) {
            key = stream.readString(exception);
            value = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(key);
            stream.writeString(value);
        }
    }

    public static class TL_langPackStringDeleted extends LangPackString {
        public static final int constructor = 0x2979eeb2;

        public void readParams(InputSerializedData stream, boolean exception) {
            key = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(key);
        }
    }

    public static abstract class auth_SentCode extends TLObject {

        public int flags;
        public auth_SentCodeType type;
        public String phone_code_hash;
        public auth_CodeType next_type;
        public int timeout;
        public auth_Authorization authorization;

        public static auth_SentCode TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            auth_SentCode result = null;
            switch (constructor) {
                case TL_auth_sentCodeSuccess.constructor:
                    result = new TL_auth_sentCodeSuccess();
                    break;
                case TL_auth_sentCode.constructor:
                    result = new TL_auth_sentCode();
                    break;
                case TL_auth_sentCodePaymentRequired.constructor:
                    result = new TL_auth_sentCodePaymentRequired();
                    break;
            }
            return TLdeserialize(auth_SentCode.class, result, stream, constructor, exception);
        }
    }

    public static class TL_auth_sentCodeSuccess extends auth_SentCode {
        public static final int constructor = 0x2390fe44;

        public void readParams(InputSerializedData stream, boolean exception) {
            authorization = auth_Authorization.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            authorization.serializeToStream(stream);
        }
    }

    public static class TL_auth_sentCode extends auth_SentCode {
        public static final int constructor = 0x5e002502;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            type = auth_SentCodeType.TLdeserialize(stream, stream.readInt32(exception), exception);
            phone_code_hash = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                next_type = auth_CodeType.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                timeout = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            type.serializeToStream(stream);
            stream.writeString(phone_code_hash);
            if (hasFlag(flags, FLAG_1)) {
                next_type.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(timeout);
            }
        }
    }

    public static class TL_auth_sentCodePaymentRequired extends auth_SentCode {
        public static final int constructor = 0xf8827ebf;

        public String store_product;
        public String phone_code_hash;
        public String support_email_address;
        public String support_email_subject;
        public int premium_days;
        public String currency;
        public long amount;

        public void readParams(InputSerializedData stream, boolean exception) {
            store_product = stream.readString(exception);
            phone_code_hash = stream.readString(exception);
            support_email_address = stream.readString(exception);
            support_email_subject = stream.readString(exception);
            premium_days = stream.readInt32(exception);
            currency = stream.readString(exception);
            amount = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(store_product);
            stream.writeString(phone_code_hash);
            stream.writeString(support_email_address);
            stream.writeString(support_email_subject);
            stream.writeInt32(premium_days);
            stream.writeString(currency);
            stream.writeInt64(amount);
        }
    }

    public static abstract class BotInlineResult extends TLObject {

        public int flags;
        public String id;
        public String type;
        public Photo photo;
        public Document document;
        public String title;
        public String description;
        public String url;
        public WebDocument thumb;
        public WebDocument content;
        public BotInlineMessage send_message;
        public long query_id;

        public static BotInlineResult TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            BotInlineResult result = null;
            switch (constructor) {
                case 0x11965f3a:
                    result = new TL_botInlineResult();
                    break;
                case 0x17db940b:
                    result = new TL_botInlineMediaResult();
                    break;
            }
            return TLdeserialize(BotInlineResult.class, result, stream, constructor, exception);
        }
    }

    public static class TL_botInlineResult extends BotInlineResult {
        public static final int constructor = 0x11965f3a;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readString(exception);
            type = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                description = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                url = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                thumb = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                content = WebDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            send_message = BotInlineMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(id);
            stream.writeString(type);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(description);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(url);
            }
            if (hasFlag(flags, FLAG_4)) {
                thumb.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                content.serializeToStream(stream);
            }
            send_message.serializeToStream(stream);
        }
    }

    public static class TL_botInlineMediaResult extends BotInlineResult {
        public static final int constructor = 0x17db940b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readString(exception);
            type = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                title = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                description = stream.readString(exception);
            }
            send_message = BotInlineMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(id);
            stream.writeString(type);
            if (hasFlag(flags, FLAG_0)) {
                photo.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                document.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeString(title);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(description);
            }
            send_message.serializeToStream(stream);
        }
    }

    public static class TL_notificationSoundDefault extends NotificationSound {
        public static final int constructor = 0x97e8bebe;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notificationSoundNone extends NotificationSound {
        public static final int constructor = 0x6f0c34df;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_notificationSoundRingtone extends NotificationSound {
        public static final int constructor = 0xff6c8049;

        public long id;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_notificationSoundLocal extends NotificationSound {
        public static final int constructor = 0x830b9ae4;

        public String title;
        public String data;

        public void readParams(InputSerializedData stream, boolean exception) {
            title = stream.readString(exception);
            data = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(title);
            stream.writeString(data);
        }
    }

    public static abstract class NotificationSound extends TLObject {

        public static NotificationSound TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            NotificationSound result = null;
            switch (constructor) {
                case 0x97e8bebe:
                    result = new TL_notificationSoundDefault();
                    break;
                case 0x6f0c34df:
                    result = new TL_notificationSoundNone();
                    break;
                case 0xff6c8049:
                    result = new TL_notificationSoundRingtone();
                    break;
                case 0x830b9ae4:
                    result = new TL_notificationSoundLocal();
                    break;
            }
            return TLdeserialize(NotificationSound.class, result, stream, constructor, exception);
        }
    }

    public static abstract class PeerNotifySettings extends TLObject {
        public int flags;
        public int mute_until;
        public String sound;
        public boolean show_previews;
        public int events_mask;
        public boolean silent;
        public NotificationSound ios_sound;
        public NotificationSound android_sound;
        public NotificationSound other_sound;
        public boolean stories_muted;
        public boolean stories_hide_sender;
        public NotificationSound stories_ios_sound;
        public NotificationSound stories_android_sound;
        public NotificationSound stories_other_sound;

        public static PeerNotifySettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            PeerNotifySettings result = null;
            switch (constructor) {
                case 0x99622c0c:
                    result = new TL_peerNotifySettings();
                    break;
                case 0xa83b0426:
                    result = new TL_peerNotifySettings_layer156();
                    break;
                case 0x9acda4c0:
                    result = new TL_peerNotifySettings_layer77();
                    break;
                case 0xaf509d20:
                    result = new TL_peerNotifySettings_layer139();
                    break;
                case 0x8d5e11ee:
                    result = new TL_peerNotifySettings_layer47();
                    break;
                case 0x70a68512:
                    result = new TL_peerNotifySettingsEmpty_layer77();
                    break;
            }
            return TLdeserialize(PeerNotifySettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_peerNotifySettings_layer77 extends TL_peerNotifySettings {
        public static final int constructor = 0x9acda4c0;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            show_previews = hasFlag(flags, FLAG_0);
            silent = hasFlag(flags, FLAG_1);
            mute_until = stream.readInt32(exception);
            sound = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, show_previews);
            flags = setFlag(flags, FLAG_1, silent);
            stream.writeInt32(flags);
            stream.writeInt32(mute_until);
            stream.writeString(sound);
        }
    }

    public static class TL_peerNotifySettings extends PeerNotifySettings {
        public static final int constructor = 0x99622c0c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                show_previews = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                silent = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                mute_until = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                stories_muted = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_7)) {
                stories_hide_sender = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stories_ios_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                stories_android_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_10)) {
                stories_other_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeBool(show_previews);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeBool(silent);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(mute_until);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeBool(stories_muted);
            }
            if (hasFlag(flags, FLAG_7)) {
                stream.writeBool(stories_hide_sender);
            }
            if (hasFlag(flags, FLAG_8)) {
                stories_ios_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stories_android_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_10)) {
                stories_other_sound.serializeToStream(stream);
            }
        }
    }

    public static class TL_peerNotifySettings_layer156 extends TL_peerNotifySettings {
        public static final int constructor = 0xa83b0426;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                show_previews = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                silent = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                mute_until = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound = NotificationSound.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeBool(show_previews);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeBool(silent);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(mute_until);
            }
            if (hasFlag(flags, FLAG_3)) {
                ios_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_4)) {
                android_sound.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_5)) {
                other_sound.serializeToStream(stream);
            }
        }
    }

    public static class TL_peerNotifySettings_layer139 extends TL_peerNotifySettings {
        public static final int constructor = 0xaf509d20;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                show_previews = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                silent = stream.readBool(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                mute_until = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                sound = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeBool(show_previews);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeBool(silent);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(mute_until);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(sound);
            }
        }
    }

    public static class TL_peerNotifySettings_layer47 extends TL_peerNotifySettings {
        public static final int constructor = 0x8d5e11ee;

        public void readParams(InputSerializedData stream, boolean exception) {
            mute_until = stream.readInt32(exception);
            sound = stream.readString(exception);
            show_previews = stream.readBool(exception);
            events_mask = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(mute_until);
            stream.writeString(sound);
            stream.writeBool(show_previews);
            stream.writeInt32(events_mask);
        }
    }

    public static class TL_peerNotifySettingsEmpty_layer77 extends PeerNotifySettings {
        public static final int constructor = 0x70a68512;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class contacts_Blocked extends TLObject {

        public ArrayList<TL_peerBlocked> blocked = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int count;

        public static contacts_Blocked TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            contacts_Blocked result = null;
            switch (constructor) {
                case 0xade1591:
                    result = new TL_contacts_blocked();
                    break;
                case 0xe1664194:
                    result = new TL_contacts_blockedSlice();
                    break;
            }
            return TLdeserialize(contacts_Blocked.class, result, stream, constructor, exception);
        }
    }

    public static class TL_contacts_blocked extends contacts_Blocked {
        public static final int constructor = 0xade1591;

        public void readParams(InputSerializedData stream, boolean exception) {
            blocked = Vector.deserialize(stream, TL_peerBlocked::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, blocked);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_contacts_blockedSlice extends contacts_Blocked {
        public static final int constructor = 0xe1664194;

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            blocked = Vector.deserialize(stream, TL_peerBlocked::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, blocked);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_inputSecureValue extends TLObject {
        public static final int constructor = 0xdb21d0a7;

        public int flags;
        public SecureValueType type;
        public TL_secureData data;
        public InputSecureFile front_side;
        public InputSecureFile reverse_side;
        public InputSecureFile selfie;
        public ArrayList<InputSecureFile> translation = new ArrayList<>();
        public ArrayList<InputSecureFile> files = new ArrayList<>();
        public SecurePlainData plain_data;

        public static TL_inputSecureValue TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_inputSecureValue result = TL_inputSecureValue.constructor != constructor ? null : new TL_inputSecureValue();
            return TLdeserialize(TL_inputSecureValue.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            type = SecureValueType.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_0)) {
                data = TL_secureData.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                front_side = InputSecureFile.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                reverse_side = InputSecureFile.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                selfie = InputSecureFile.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                translation = Vector.deserialize(stream, InputSecureFile::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                files = Vector.deserialize(stream, InputSecureFile::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                plain_data = SecurePlainData.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            type.serializeToStream(stream);
            if (hasFlag(flags, FLAG_0)) {
                data.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_1)) {
                front_side.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_2)) {
                reverse_side.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                selfie.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_6)) {
                Vector.serialize(stream, translation);
            }
            if (hasFlag(flags, FLAG_4)) {
                Vector.serialize(stream, files);
            }
            if (hasFlag(flags, FLAG_5)) {
                plain_data.serializeToStream(stream);
            }
        }
    }

    public static abstract class help_AppConfig extends TLObject {

        public static help_AppConfig TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_AppConfig result = null;
            switch (constructor) {
                case 0xdd18782e:
                    result = new TL_help_appConfig();
                    break;
                case 0x7cde641d:
                    result = new TL_help_appConfigNotModified();
                    break;
            }
            return TLdeserialize(help_AppConfig.class, result, stream, constructor, exception);
        }
    }

    public static class TL_help_appConfig extends help_AppConfig {
        public static final int constructor = 0xdd18782e;

        public int hash;
        public JSONValue config;

        public void readParams(InputSerializedData stream, boolean exception) {
            hash = stream.readInt32(exception);
            config = JSONValue.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(hash);
            config.serializeToStream(stream);
        }
    }

    public static class TL_help_appConfigNotModified extends help_AppConfig {
        public static final int constructor = 0x7cde641d;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class messages_DhConfig extends TLObject {
        public byte[] random;
        public int g;
        public byte[] p;
        public int version;

        public static messages_DhConfig TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_DhConfig result = null;
            switch (constructor) {
                case 0xc0e24635:
                    result = new TL_messages_dhConfigNotModified();
                    break;
                case 0x2c221edd:
                    result = new TL_messages_dhConfig();
                    break;
            }
            return TLdeserialize(messages_DhConfig.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_dhConfigNotModified extends messages_DhConfig {
        public static final int constructor = 0xc0e24635;

        public void readParams(InputSerializedData stream, boolean exception) {
            random = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(random);
        }
    }

    public static class TL_messages_dhConfig extends messages_DhConfig {
        public static final int constructor = 0x2c221edd;

        public void readParams(InputSerializedData stream, boolean exception) {
            g = stream.readInt32(exception);
            p = stream.readByteArray(exception);
            version = stream.readInt32(exception);
            random = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(g);
            stream.writeByteArray(p);
            stream.writeInt32(version);
            stream.writeByteArray(random);
        }
    }
    
    public static class DisallowedGiftsSettings extends TLObject {
        public static final int constructor = 0x71f276c4;
        
        public int flags;
        public boolean disallow_unlimited_stargifts;
        public boolean disallow_limited_stargifts;
        public boolean disallow_unique_stargifts;
        public boolean disallow_premium_gifts;
        public boolean disallow_stargifts_from_channels;

        public static DisallowedGiftsSettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final DisallowedGiftsSettings result = DisallowedGiftsSettings.constructor != constructor ? null : new DisallowedGiftsSettings();
            return TLdeserialize(DisallowedGiftsSettings.class, result, stream, constructor, exception);
        }

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            disallow_unlimited_stargifts = hasFlag(flags, FLAG_0);
            disallow_limited_stargifts = hasFlag(flags, FLAG_1);
            disallow_unique_stargifts = hasFlag(flags, FLAG_2);
            disallow_premium_gifts = hasFlag(flags, FLAG_3);
            disallow_stargifts_from_channels = hasFlag(flags, FLAG_4);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, disallow_unlimited_stargifts);
            flags = setFlag(flags, FLAG_1, disallow_limited_stargifts);
            flags = setFlag(flags, FLAG_2, disallow_unique_stargifts);
            flags = setFlag(flags, FLAG_3, disallow_premium_gifts);
            flags = setFlag(flags, FLAG_4, disallow_stargifts_from_channels);
            stream.writeInt32(flags);
        }
    }

    public static class GlobalPrivacySettings extends TLObject {

        public int flags;
        public boolean archive_and_mute_new_noncontact_peers;
        public boolean keep_archived_unmuted;
        public boolean keep_archived_folders;
        public boolean hide_read_marks;
        public boolean new_noncontact_peers_require_premium;
        public boolean display_gifts_button;
        public long noncontact_peers_paid_stars;
        public DisallowedGiftsSettings disallowed_stargifts;

        public static GlobalPrivacySettings TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            GlobalPrivacySettings result = null;
            switch (constructor) {
                case TL_globalPrivacySettings.constructor:
                    result = new TL_globalPrivacySettings();
                    break;
                case TL_globalPrivacySettings_layer200.constructor:
                    result = new TL_globalPrivacySettings_layer200();
                    break;
            }
            return TLdeserialize(GlobalPrivacySettings.class, result, stream, constructor, exception);
        }
    }

    public static class TL_globalPrivacySettings extends GlobalPrivacySettings {
        public static final int constructor = 0xfe41b34f;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            archive_and_mute_new_noncontact_peers = hasFlag(flags, FLAG_0);
            keep_archived_unmuted = hasFlag(flags, FLAG_1);
            keep_archived_folders = hasFlag(flags, FLAG_2);
            hide_read_marks = hasFlag(flags, FLAG_3);
            new_noncontact_peers_require_premium = hasFlag(flags, FLAG_4);
            display_gifts_button = hasFlag(flags, FLAG_7);
            if (hasFlag(flags, FLAG_5)) {
                noncontact_peers_paid_stars = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                disallowed_stargifts = DisallowedGiftsSettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, archive_and_mute_new_noncontact_peers);
            flags = setFlag(flags, FLAG_1, keep_archived_unmuted);
            flags = setFlag(flags, FLAG_2, keep_archived_folders);
            flags = setFlag(flags, FLAG_3, hide_read_marks);
            flags = setFlag(flags, FLAG_4, new_noncontact_peers_require_premium);
            flags = setFlag(flags, FLAG_7, display_gifts_button);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt64(noncontact_peers_paid_stars);
            }
            if (hasFlag(flags, FLAG_6)) {
                disallowed_stargifts.serializeToStream(stream);
            }
        }
    }

    public static class TL_globalPrivacySettings_layer200 extends TL_globalPrivacySettings {
        public static final int constructor = 0xc9d8df1c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            archive_and_mute_new_noncontact_peers = hasFlag(flags, FLAG_0);
            keep_archived_unmuted = hasFlag(flags, FLAG_1);
            keep_archived_folders = hasFlag(flags, FLAG_2);
            hide_read_marks = hasFlag(flags, FLAG_3);
            new_noncontact_peers_require_premium = hasFlag(flags, FLAG_4);
            if (hasFlag(flags, FLAG_5)) {
                noncontact_peers_paid_stars = stream.readInt64(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, archive_and_mute_new_noncontact_peers);
            flags = setFlag(flags, FLAG_1, keep_archived_unmuted);
            flags = setFlag(flags, FLAG_2, keep_archived_folders);
            flags = setFlag(flags, FLAG_3, hide_read_marks);
            flags = setFlag(flags, FLAG_4, new_noncontact_peers_require_premium);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt64(noncontact_peers_paid_stars);
            }
        }
    }

    public static class TL_help_premiumPromo_layer144 extends TL_help_premiumPromo {
        public static final int constructor = 0x8a4f3c29;

        public void readParams(InputSerializedData stream, boolean exception) {
            status_text = stream.readString(exception);
            status_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            video_sections = Vector.deserializeString(stream, exception);
            videos = Vector.deserialize(stream, Document::TLdeserialize, exception);
            currency = stream.readString(exception);
            monthly_amount = stream.readInt64(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(status_text);
            Vector.serialize(stream, status_entities);
            Vector.serializeString(stream, video_sections);
            Vector.serialize(stream, videos);
            stream.writeString(currency);
            stream.writeInt64(monthly_amount);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_help_premiumPromo_layer140 extends TL_help_premiumPromo {
        public static final int constructor = 0xe0360f1b;

        public void readParams(InputSerializedData stream, boolean exception) {
            status_text = stream.readString(exception);
            status_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            video_sections = Vector.deserializeString(stream, exception);
            videos = Vector.deserialize(stream, Document::TLdeserialize, exception);
            currency = stream.readString(exception);
            monthly_amount = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(status_text);
            Vector.serialize(stream, status_entities);
            Vector.serializeString(stream, video_sections);
            Vector.serialize(stream, videos);
            stream.writeString(currency);
            stream.writeInt64(monthly_amount);
        }
    }

    public static class TL_help_premiumPromo extends TLObject {
        public static final int constructor = 0x5334759c;

        public String status_text;
        public ArrayList<MessageEntity> status_entities = new ArrayList<>();
        public ArrayList<String> video_sections = new ArrayList<>();
        public ArrayList<Document> videos = new ArrayList<>();
        public ArrayList<TL_premiumSubscriptionOption> period_options = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public String currency;
        public long monthly_amount;

        public static TL_help_premiumPromo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_help_premiumPromo result = null;
            switch (constructor) {
                case 0x5334759c:
                    result = new TL_help_premiumPromo();
                    break;
                case 0x8a4f3c29:
                    result = new TL_help_premiumPromo_layer144();
                    break;
                case 0xe0360f1b:
                    result = new TL_help_premiumPromo_layer140();
                    break;
            }
            result = TLdeserialize(TL_help_premiumPromo.class, result, stream, constructor, exception);
            if (result != null && result.currency != null) {
                TL_help_premiumPromo finalResult = result;
                result.period_options.add(new TL_premiumSubscriptionOption() {{
                    months = 1;
                    currency = finalResult.currency;
                    amount = finalResult.monthly_amount;
                    store_product = "telegram_premium";
                }});
            }
            return result;
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            status_text = stream.readString(exception);
            status_entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            video_sections = Vector.deserializeString(stream, exception);
            videos = Vector.deserialize(stream, Document::TLdeserialize, exception);
            period_options = Vector.deserialize(stream, TL_premiumSubscriptionOption::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(status_text);
            Vector.serialize(stream, status_entities);
            Vector.serializeString(stream, video_sections);
            Vector.serialize(stream, videos);
            Vector.serialize(stream, period_options);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class help_UserInfo extends TLObject {

        public static help_UserInfo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            help_UserInfo result = null;
            switch (constructor) {
                case 0xf3ae2eed:
                    result = new TL_help_userInfoEmpty();
                    break;
                case 0x1eb3758:
                    result = new TL_help_userInfo();
                    break;
            }
            return TLdeserialize(help_UserInfo.class, result, stream, constructor, exception);
        }
    }

    public static class TL_help_userInfoEmpty extends help_UserInfo {
        public static final int constructor = 0xf3ae2eed;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_help_userInfo extends help_UserInfo {
        public static final int constructor = 0x1eb3758;

        public String message;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
        public String author;
        public int date;

        public void readParams(InputSerializedData stream, boolean exception) {
            message = stream.readString(exception);
            entities = Vector.deserialize(stream, MessageEntity::TLdeserialize, exception);
            author = stream.readString(exception);
            date = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(message);
            Vector.serialize(stream, entities);
            stream.writeString(author);
            stream.writeInt32(date);
        }
    }

    public static class TL_secureValueHash extends TLObject {
        public static final int constructor = 0xed1ecdb0;

        public SecureValueType type;
        public byte[] hash;

        public static TL_secureValueHash TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_secureValueHash result = TL_secureValueHash.constructor != constructor ? null : new TL_secureValueHash();
            return TLdeserialize(TL_secureValueHash.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            type = SecureValueType.TLdeserialize(stream, stream.readInt32(exception), exception);
            hash = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            type.serializeToStream(stream);
            stream.writeByteArray(hash);
        }
    }

    public static abstract class messages_StickerSet extends TLObject {

        public StickerSet set;
        public ArrayList<TL_stickerPack> packs = new ArrayList<>();
        public ArrayList<TL_stickerKeyword> keywords = new ArrayList<>();
        public ArrayList<Document> documents = new ArrayList<>();

        public static TL_messages_stickerSet TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            TL_messages_stickerSet result = null;
            switch (constructor) {
                case 0x6e153f16:
                    result = new TL_messages_stickerSet();
                    break;
                case 0xb60a24a6:
                    result = new TL_messages_stickerSet_layer146();
                    break;
                case 0xd3f924eb:
                    result = new TL_messages_stickerSetNotModified();
                    break;
            }
            return TLdeserialize(TL_messages_stickerSet.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_stickerSet_layer146 extends TL_messages_stickerSet {
        public static final int constructor = 0xb60a24a6;

        public void readParams(InputSerializedData stream, boolean exception) {
            set = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            packs = Vector.deserialize(stream, TL_stickerPack::TLdeserialize, exception);
            documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            set.serializeToStream(stream);
            Vector.serialize(stream, packs);
            Vector.serialize(stream, documents);
        }
    }

    public static class TL_messages_stickerSet extends messages_StickerSet {
        public static final int constructor = 0x6e153f16;

        public void readParams(InputSerializedData stream, boolean exception) {
            set = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            packs = Vector.deserialize(stream, TL_stickerPack::TLdeserialize, exception);
            keywords = Vector.deserialize(stream, TL_stickerKeyword::TLdeserialize, exception);
            documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            set.serializeToStream(stream);
            Vector.serialize(stream, packs);
            Vector.serialize(stream, keywords);
            Vector.serialize(stream, documents);
        }
    }

    public static class TL_messages_stickerSetNotModified extends TL_messages_stickerSet {
        public static final int constructor = 0xd3f924eb;

        public void readParams(InputSerializedData stream, boolean exception) {

        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class InputGeoPoint extends TLObject {

        public int flags;
        public double lat;
        public double _long;
        public int accuracy_radius;

        public static InputGeoPoint TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputGeoPoint result = null;
            switch (constructor) {
                case 0x48222faf:
                    result = new TL_inputGeoPoint();
                    break;
                case 0xe4c123d6:
                    result = new TL_inputGeoPointEmpty();
                    break;
            }
            return TLdeserialize(InputGeoPoint.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputGeoPoint extends InputGeoPoint {
        public static final int constructor = 0x48222faf;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            lat = stream.readDouble(exception);
            _long = stream.readDouble(exception);
            if (hasFlag(flags, FLAG_0)) {
                accuracy_radius = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeDouble(lat);
            stream.writeDouble(_long);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(accuracy_radius);
            }
        }
    }

    public static class TL_inputGeoPointEmpty extends InputGeoPoint {
        public static final int constructor = 0xe4c123d6;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_help_inviteText extends TLObject {
        public static final int constructor = 0x18cb9f78;

        public String message;

        public static TL_help_inviteText TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_help_inviteText result = TL_help_inviteText.constructor != constructor ? null : new TL_help_inviteText();
            return TLdeserialize(TL_help_inviteText.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            message = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(message);
        }
    }

    public static abstract class Audio extends TLObject {
        public long id;
        public long access_hash;
        public int date;
        public int duration;
        public String mime_type;
        public int size;
        public int dc_id;
        public long user_id;
        public byte[] key;
        public byte[] iv;

        public static Audio TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            Audio result = null;
            switch (constructor) {
                case 0x586988d8:
                    result = new TL_audioEmpty_layer45();
                    break;
                case 0xf9e35055:
                    result = new TL_audio_layer45();
                    break;
                case 0x427425e7:
                    result = new TL_audio_old();
                    break;
                case 0x555555F6:
                    result = new TL_audioEncrypted();
                    break;
                case 0xc7ac6496:
                    result = new TL_audio_old2();
                    break;
            }
            return TLdeserialize(Audio.class, result, stream, constructor, exception);
        }
    }

    public static class TL_audioEmpty_layer45 extends Audio {
        public static final int constructor = 0x586988d8;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_audio_layer45 extends Audio {
        public static final int constructor = 0xf9e35055;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeString(mime_type);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_audio_old extends TL_audio_layer45 {
        public static final int constructor = 0x427425e7;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) user_id);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_audioEncrypted extends TL_audio_layer45 {
        public static final int constructor = 0x555555F6;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
            key = stream.readByteArray(exception);
            iv = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) user_id);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
            stream.writeByteArray(key);
            stream.writeByteArray(iv);
        }
    }

    public static class TL_audio_old2 extends TL_audio_layer45 {
        public static final int constructor = 0xc7ac6496;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            duration = stream.readInt32(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32((int) user_id);
            stream.writeInt32(date);
            stream.writeInt32(duration);
            stream.writeString(mime_type);
            stream.writeInt32(size);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_help_country extends TLObject {
        public static final int constructor = 0xc3878e23;

        public int flags;
        public boolean hidden;
        public String iso2;
        public String default_name;
        public String name;
        public ArrayList<TL_help_countryCode> country_codes = new ArrayList<>();

        public static TL_help_country TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_help_country result = TL_help_country.constructor != constructor ? null : new TL_help_country();
            return TLdeserialize(TL_help_country.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            hidden = hasFlag(flags, FLAG_0);
            iso2 = stream.readString(exception);
            default_name = stream.readString(exception);
            if (hasFlag(flags, FLAG_1)) {
                name = stream.readString(exception);
            }
            country_codes = Vector.deserialize(stream, TL_help_countryCode::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, hidden);
            stream.writeInt32(flags);
            stream.writeString(iso2);
            stream.writeString(default_name);
            if (hasFlag(flags, FLAG_1)) {
                stream.writeString(name);
            }
            Vector.serialize(stream, country_codes);
        }
    }

    public static abstract class SecurePasswordKdfAlgo extends TLObject {

        public static SecurePasswordKdfAlgo TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            SecurePasswordKdfAlgo result = null;
            switch (constructor) {
                case 0xbbf2dda0:
                    result = new TL_securePasswordKdfAlgoPBKDF2HMACSHA512iter100000();
                    break;
                case 0x86471d92:
                    result = new TL_securePasswordKdfAlgoSHA512();
                    break;
                case 0x4a8537:
                    result = new TL_securePasswordKdfAlgoUnknown();
                    break;
            }
            return TLdeserialize(SecurePasswordKdfAlgo.class, result, stream, constructor, exception);
        }
    }

    public static class TL_securePasswordKdfAlgoPBKDF2HMACSHA512iter100000 extends SecurePasswordKdfAlgo {
        public static final int constructor = 0xbbf2dda0;

        public byte[] salt;

        public void readParams(InputSerializedData stream, boolean exception) {
            salt = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(salt);
        }
    }

    public static class TL_securePasswordKdfAlgoSHA512 extends SecurePasswordKdfAlgo {
        public static final int constructor = 0x86471d92;

        public byte[] salt;

        public void readParams(InputSerializedData stream, boolean exception) {
            salt = stream.readByteArray(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeByteArray(salt);
        }
    }

    public static class TL_securePasswordKdfAlgoUnknown extends SecurePasswordKdfAlgo {
        public static final int constructor = 0x4a8537;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_historyImport extends TLObject {
        public static final int constructor = 0x1662af0b;

        public long id;

        public static TL_messages_historyImport TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messages_historyImport result = TL_messages_historyImport.constructor != constructor ? null : new TL_messages_historyImport();
            return TLdeserialize(TL_messages_historyImport.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static abstract class InputGame extends TLObject {
        public InputUser bot_id;
        public String short_name;
        public long id;
        public long access_hash;

        public static InputGame TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputGame result = null;
            switch (constructor) {
                case 0xc331e80a:
                    result = new TL_inputGameShortName();
                    break;
                case 0x32c3e77:
                    result = new TL_inputGameID();
                    break;
            }
            return TLdeserialize(InputGame.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputGameShortName extends InputGame {
        public static final int constructor = 0xc331e80a;

        public void readParams(InputSerializedData stream, boolean exception) {
            bot_id = InputUser.TLdeserialize(stream, stream.readInt32(exception), exception);
            short_name = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            bot_id.serializeToStream(stream);
            stream.writeString(short_name);
        }
    }

    public static class TL_inputGameID extends InputGame {
        public static final int constructor = 0x32c3e77;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static abstract class MessageReplies extends TLObject {

        public int flags;
        public boolean comments;
        public int replies;
        public int replies_pts;
        public ArrayList<Peer> recent_repliers = new ArrayList<>();
        public long channel_id;
        public int max_id;
        public int read_max_id;

        public static MessageReplies TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            MessageReplies result = null;
            switch (constructor) {
                case 0x4128faac:
                    result = new TL_messageReplies_layer131();
                    break;
                case 0x83d60fc2:
                    result = new TL_messageReplies();
                    break;
            }
            return TLdeserialize(MessageReplies.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messageReplies_layer131 extends TL_messageReplies {
        public static final int constructor = 0x4128faac;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            comments = hasFlag(flags, FLAG_0);
            replies = stream.readInt32(exception);
            replies_pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_1)) {
                recent_repliers = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                channel_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                read_max_id = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, comments);
            stream.writeInt32(flags);
            stream.writeInt32(replies);
            stream.writeInt32(replies_pts);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, recent_repliers);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32((int) channel_id);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(max_id);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(read_max_id);
            }
        }
    }

    public static class TL_messageReplies extends MessageReplies {
        public static final int constructor = 0x83d60fc2;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            comments = hasFlag(flags, FLAG_0);
            replies = stream.readInt32(exception);
            replies_pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_1)) {
                recent_repliers = Vector.deserialize(stream, Peer::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_0)) {
                channel_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                read_max_id = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, comments);
            stream.writeInt32(flags);
            stream.writeInt32(replies);
            stream.writeInt32(replies_pts);
            if (hasFlag(flags, FLAG_1)) {
                Vector.serialize(stream, recent_repliers);
            }
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt64(channel_id);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(max_id);
            }
            if (hasFlag(flags, FLAG_3)) {
                stream.writeInt32(read_max_id);
            }
        }
    }

    public static abstract class messages_SponsoredMessages extends TLObject {
        public int flags;
        public int posts_between;
        public int start_delay;
        public int between_delay;
        public ArrayList<TL_sponsoredMessage> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static messages_SponsoredMessages TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_SponsoredMessages result = null;
            switch (constructor) {
                case TL_messages_sponsoredMessagesEmpty.constructor:
                    result = new TL_messages_sponsoredMessagesEmpty();
                    break;
                case TL_messages_sponsoredMessages.constructor:
                    result = new TL_messages_sponsoredMessages();
                    break;
            }
            return TLdeserialize(messages_SponsoredMessages.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_sponsoredMessagesEmpty extends messages_SponsoredMessages {
        public static final int constructor = 0x1839490f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_messages_sponsoredMessages extends messages_SponsoredMessages {
        public static final int constructor = 0xffda656d;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                posts_between = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                start_delay = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                between_delay = stream.readInt32(exception);
            }
            messages = Vector.deserialize(stream, TL_sponsoredMessage::TLdeserialize, exception);
            chats = Vector.deserialize(stream, Chat::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(posts_between);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(start_delay);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(between_delay);
            }
            Vector.serialize(stream, messages);
            Vector.serialize(stream, chats);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messageViews extends TLObject {
        public static final int constructor = 0x455b853d;

        public int flags;
        public int views;
        public int forwards;
        public MessageReplies replies;

        public static TL_messageViews TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final TL_messageViews result = TL_messageViews.constructor != constructor ? null : new TL_messageViews();
            return TLdeserialize(TL_messageViews.class, result, stream, constructor, exception);
        }

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                views = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                forwards = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                replies = MessageReplies.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(views);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(forwards);
            }
            if (hasFlag(flags, FLAG_2)) {
                replies.serializeToStream(stream);
            }
        }
    }

    public static abstract class ReplyMarkup extends TLObject {
        public int flags;
        public boolean resize;
        public boolean single_use;
        public boolean is_persistent;
        public boolean selective;
        public boolean force_reply;
        public String placeholder;

        public static ReplyMarkup TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(ReplyMarkup.class, fromConstructor(constructor), stream, constructor, exception);
        }

        private static ReplyMarkup fromConstructor(int constructor) {
            switch (constructor) {
                case TL_replyKeyboardMarkup.constructor:
                    return new TL_replyKeyboardMarkup();
                case TL_replyKeyboardHide.constructor:
                    return new TL_replyKeyboardHide();
                case TL_replyKeyboardForceReply.constructor:
                    return new TL_replyKeyboardForceReply();
                case TL_replyKeyboardMarkup_layer129.constructor:
                    return new TL_replyKeyboardMarkup_layer129();
                case TL_replyKeyboardForceReply_layer129.constructor:
                    return new TL_replyKeyboardForceReply_layer129();
                case TL_replyInlineMarkup.constructor:
                    return new TL_replyInlineMarkup();
                case TL_replyInlineMarkup_layer228_old.constructor:
                    return new TL_replyInlineMarkup_layer228_old();
                case TL_replyInlineMarkup_layer228.constructor:
                    return new TL_replyInlineMarkup_layer228();
                default:
                    return null;
            }
        }
    }

    public static class TL_replyKeyboardMarkup extends ReplyMarkup {
        public static final int constructor = 0x85dd99d1;

        public ArrayList<TL_keyboard.KeyboardButtonRow> rows = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            resize = hasFlag(flags, FLAG_0);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
            is_persistent = hasFlag(flags, FLAG_4);
            force_reply = hasFlag(flags, FLAG_5);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardButtonRow::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_3)) {
                placeholder = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, resize);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            flags = setFlag(flags, FLAG_4, is_persistent);
            flags = setFlag(flags, FLAG_5, force_reply);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(placeholder);
            }
        }
    }

    public static class TL_replyKeyboardHide extends ReplyMarkup {
        public static final int constructor = 0xa03e5b85;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            selective = hasFlag(flags, FLAG_2);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
        }
    }

    public static class TL_replyKeyboardForceReply extends ReplyMarkup {
        public static final int constructor = 0x86b40b08;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
            if (hasFlag(flags, FLAG_3)) {
                placeholder = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_3)) {
                stream.writeString(placeholder);
            }
        }
    }

    public static class TL_replyKeyboardMarkup_layer129 extends TL_replyKeyboardMarkup {
        public static final int constructor = 0x3502758c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            resize = hasFlag(flags, FLAG_0);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, resize);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
        }
    }

    public static class TL_replyKeyboardForceReply_layer129 extends TL_replyKeyboardForceReply {
        public static final int constructor = 0xf4108aa0;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            single_use = hasFlag(flags, FLAG_1);
            selective = hasFlag(flags, FLAG_2);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_1, single_use);
            flags = setFlag(flags, FLAG_2, selective);
            stream.writeInt32(flags);
        }
    }

    public static class TL_replyInlineMarkup extends ReplyMarkup {
        public static final int constructor = 0xB2B15770;

        public ArrayList<TL_keyboard.KeyboardInlineButtonRow> rows = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            force_reply = hasFlag(flags, FLAG_5);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardInlineButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_5, force_reply);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
        }
    }

    public static class TL_replyInlineMarkup_layer228_old extends TL_replyInlineMarkup {
        public static final int constructor = 0x58F7FCB6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            force_reply = hasFlag(flags, FLAG_5);
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardInlineButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_5, force_reply);
            stream.writeInt32(flags);
            Vector.serialize(stream, rows);
        }
    }

    public static class TL_replyInlineMarkup_layer228 extends TL_replyInlineMarkup {
        public static final int constructor = 0x48a30254;

        public void readParams(InputSerializedData stream, boolean exception) {
            rows = Vector.deserialize(stream, TL_keyboard.KeyboardInlineButtonRow::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, rows);
        }
    }

    public static class WebPageAttribute extends TLObject {
        public int flags;

        private static WebPageAttribute fromConstructor(int constructor) {
            switch (constructor) {
                case TL_webPageAttributeTheme.constructor:              return new TL_webPageAttributeTheme();
                case TL_webPageAttributeStory_layer162.constructor:     return new TL_webPageAttributeStory_layer162();
                case TL_webPageAttributeStory.constructor:              return new TL_webPageAttributeStory();
                case TL_webPageAttributeStickerSet.constructor:         return new TL_webPageAttributeStickerSet();
                case TL_webPageAttributeUniqueStarGift.constructor:     return new TL_webPageAttributeUniqueStarGift();
                case TL_webPageAttributeStarGiftCollection.constructor: return new TL_webPageAttributeStarGiftCollection();
                case TL_webPageAttributeStarGiftAuction.constructor:    return new TL_webPageAttributeStarGiftAuction();
                case TL_webPageAttributeStarGiftAuction_layer219.constructor: return new TL_webPageAttributeStarGiftAuction_layer219();
                case TL_webPageAttributeAiComposeTone.constructor:      return new TL_webPageAttributeAiComposeTone();
            }
            return null;
        }

        public static WebPageAttribute TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return TLdeserialize(WebPageAttribute.class, fromConstructor(constructor), stream, constructor, exception);
        }
    }

    public static class TL_webPageAttributeStarGiftAuction extends WebPageAttribute {
        public final static int constructor = 0x01C641C2;

        public TL_stars.StarGift gift;
        public int end_date;

        @Deprecated
        public int center_color;

        @Deprecated
        public int edge_color;

        @Deprecated
        public int text_color;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            gift = TL_stars.StarGift.TLdeserialize(stream, stream.readInt32(exception), exception);
            end_date = stream.readInt32(exception);

            if (gift != null && gift.background != null) {
                center_color = gift.background.center_color;
                edge_color = gift.background.edge_color;
                text_color = gift.background.text_color;
            }
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            gift.serializeToStream(stream);
            stream.writeInt32(end_date);
        }
    }

    public static class TL_webPageAttributeAiComposeTone extends WebPageAttribute {
        public final static int constructor = 0x7781fe18;

        public long emoji_id;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            emoji_id = stream.readInt64(exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(emoji_id);
        }
    }

    public static class TL_webPageAttributeStarGiftAuction_layer219 extends TL_webPageAttributeStarGiftAuction {
        public final static int constructor = 0x34986ab;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            gift = TL_stars.StarGift.TLdeserialize(stream, stream.readInt32(exception), exception);
            end_date = stream.readInt32(exception);
            center_color = stream.readInt32(exception);
            edge_color = stream.readInt32(exception);
            text_color = stream.readInt32(exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            gift.serializeToStream(stream);
            stream.writeInt32(end_date);
            stream.writeInt32(center_color);
            stream.writeInt32(edge_color);
            stream.writeInt32(text_color);
        }
    }

    public static class TL_webPageAttributeStory extends WebPageAttribute {
        public final static int constructor = 0x2e94c3e7;

        public Peer peer;
        public int id;
        public TL_stories.StoryItem storyItem;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            id = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                storyItem = TL_stories.StoryItem.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            peer.serializeToStream(stream);
            stream.writeInt32(id);
            if (hasFlag(flags, FLAG_0)) {
                storyItem.serializeToStream(stream);
            }
        }
    }

    public static class TL_webPageAttributeStickerSet extends WebPageAttribute {
        public final static int constructor = 0x50cc03d3;

        public Peer peer;
        public boolean emojis;
        public boolean text_color;
        public ArrayList<TLRPC.Document> stickers = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            emojis = hasFlag(flags, FLAG_0);
            text_color = hasFlag(flags, FLAG_1);
            stickers = Vector.deserialize(stream, Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, emojis);
            flags = setFlag(flags, FLAG_1, text_color);
            stream.writeInt32(flags);
            Vector.serialize(stream, stickers);
        }
    }

    public static class TL_webPageAttributeStarGiftCollection extends WebPageAttribute {
        public final static int constructor = 0x31cad303;

        public ArrayList<TLRPC.Document> icons = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            icons = Vector.deserialize(stream, TLRPC.Document::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, icons);
        }
    }

    public static class TL_webPageAttributeUniqueStarGift extends WebPageAttribute {
        public final static int constructor = 0xcf6f6db8;

        public TL_stars.StarGift gift;

        public void readParams(InputSerializedData stream, boolean exception) {
            gift = TL_stars.StarGift.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            gift.serializeToStream(stream);
        }
    }

    public static class TL_webPageAttributeStory_layer162 extends TL_webPageAttributeStory {
        public static final int constructor = 0x939a4671;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            long user_id = stream.readInt64(exception);
            peer = new TL_peerUser();
            peer.user_id = user_id;
            id = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                storyItem = TL_stories.StoryItem.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            if (storyItem != null) {
                flags |= 1;
            } else {
                flags &= ~1;
            }
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(peer.user_id);
            stream.writeInt32(id);
            if (hasFlag(flags, FLAG_0)) {
                storyItem.serializeToStream(stream);
            }
        }
    }

    public static class TL_webPageAttributeTheme extends WebPageAttribute {
        public static final int constructor = 0x54b56617;

        public ArrayList<Document> documents = new ArrayList<>();
        public ThemeSettings settings;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_0)) {
                documents = Vector.deserialize(stream, Document::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                settings = ThemeSettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            if (hasFlag(flags, FLAG_0)) {
                Vector.serialize(stream, documents);
            }
            if (hasFlag(flags, FLAG_1)) {
                settings.serializeToStream(stream);
            }
        }
    }

    public static abstract class contacts_Contacts extends TLObject {
        public ArrayList<TL_contact> contacts = new ArrayList<>();
        public int saved_count;
        public ArrayList<User> users = new ArrayList<>();

        public static contacts_Contacts TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            contacts_Contacts result = null;
            switch (constructor) {
                case 0xb74ba9d2:
                    result = new TL_contacts_contactsNotModified();
                    break;
                case 0xeae87e42:
                    result = new TL_contacts_contacts();
                    break;
            }
            return TLdeserialize(contacts_Contacts.class, result, stream, constructor, exception);
        }
    }

    public static class TL_contacts_contactsNotModified extends contacts_Contacts {
        public static final int constructor = 0xb74ba9d2;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_contacts_contacts extends contacts_Contacts {
        public static final int constructor = 0xeae87e42;

        public void readParams(InputSerializedData stream, boolean exception) {
            contacts = Vector.deserialize(stream, TL_contact::TLdeserialize, exception);
            saved_count = stream.readInt32(exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, contacts);
            stream.writeInt32(saved_count);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class SecureRequiredType extends TLObject {

        public static SecureRequiredType TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            SecureRequiredType result = null;
            switch (constructor) {
                case 0x829d99da:
                    result = new TL_secureRequiredType();
                    break;
                case 0x27477b4:
                    result = new TL_secureRequiredTypeOneOf();
                    break;
            }
            return TLdeserialize(SecureRequiredType.class, result, stream, constructor, exception);
        }
    }

    public static class TL_secureRequiredType extends SecureRequiredType {
        public static final int constructor = 0x829d99da;

        public int flags;
        public boolean native_names;
        public boolean selfie_required;
        public boolean translation_required;
        public SecureValueType type;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            native_names = hasFlag(flags, FLAG_0);
            selfie_required = hasFlag(flags, FLAG_1);
            translation_required = hasFlag(flags, FLAG_2);
            type = SecureValueType.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_0, native_names);
            flags = setFlag(flags, FLAG_1, selfie_required);
            flags = setFlag(flags, FLAG_2, translation_required);
            stream.writeInt32(flags);
            type.serializeToStream(stream);
        }
    }

    public static class TL_secureRequiredTypeOneOf extends SecureRequiredType {
        public static final int constructor = 0x27477b4;

        public ArrayList<SecureRequiredType> types = new ArrayList<>();

        public void readParams(InputSerializedData stream, boolean exception) {
            types = Vector.deserialize(stream, SecureRequiredType::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, types);
        }
    }

    public static abstract class InputPrivacyKey extends TLObject {

        public static InputPrivacyKey TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputPrivacyKey result = null;
            switch (constructor) {
                case TL_inputPrivacyKeyStatusTimestamp.constructor:
                    result = new TL_inputPrivacyKeyStatusTimestamp();
                    break;
                case TL_inputPrivacyKeyChatInvite.constructor:
                    result = new TL_inputPrivacyKeyChatInvite();
                    break;
                case TL_inputPrivacyKeyPhoneCall.constructor:
                    result = new TL_inputPrivacyKeyPhoneCall();
                    break;
                case TL_inputPrivacyKeyForwards.constructor:
                    result = new TL_inputPrivacyKeyForwards();
                    break;
                case TL_inputPrivacyKeyProfilePhoto.constructor:
                    result = new TL_inputPrivacyKeyProfilePhoto();
                    break;
                case TL_inputPrivacyKeyPhoneNumber.constructor:
                    result = new TL_inputPrivacyKeyPhoneNumber();
                    break;
                case TL_inputPrivacyKeyAddedByPhone.constructor:
                    result = new TL_inputPrivacyKeyAddedByPhone();
                    break;
                case TL_inputPrivacyKeyVoiceMessages.constructor:
                    result = new TL_inputPrivacyKeyVoiceMessages();
                    break;
                case TL_inputPrivacyKeyPhoneP2P.constructor:
                    result = new TL_inputPrivacyKeyPhoneP2P();
                    break;
                case TL_inputPrivacyKeyAbout.constructor:
                    result = new TL_inputPrivacyKeyAbout();
                    break;
                case TL_inputPrivacyKeyBirthday.constructor:
                    result = new TL_inputPrivacyKeyBirthday();
                    break;
                case TL_inputPrivacyKeyStarGiftsAutoSave.constructor:
                    result = new TL_inputPrivacyKeyStarGiftsAutoSave();
                    break;
                case TL_inputPrivacyKeyNoPaidMessages.constructor:
                    result = new TL_inputPrivacyKeyNoPaidMessages();
                    break;
                case TL_inputPrivacyKeySavedMusic.constructor:
                    result = new TL_inputPrivacyKeySavedMusic();
                    break;
            }
            return TLdeserialize(InputPrivacyKey.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputPrivacyKeyStatusTimestamp extends InputPrivacyKey {
        public static final int constructor = 0x4f96cb18;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyChatInvite extends InputPrivacyKey {
        public static final int constructor = 0xbdfb0426;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyPhoneCall extends InputPrivacyKey {
        public static final int constructor = 0xfabadc5f;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyForwards extends InputPrivacyKey {
        public static final int constructor = 0xa4dd4c08;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyPhoneNumber extends InputPrivacyKey {
        public static final int constructor = 0x352dafa;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyAddedByPhone extends InputPrivacyKey {
        public static final int constructor = 0xd1219bdd;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyVoiceMessages extends InputPrivacyKey {
        public static final int constructor = 0xaee69d68;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyAbout extends InputPrivacyKey {
        public static final int constructor = 0x3823cc40;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyBirthday extends InputPrivacyKey {
        public static final int constructor = 0xd65a11cc;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyStarGiftsAutoSave extends InputPrivacyKey {
        public static final int constructor = 0xe1732341;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyNoPaidMessages extends InputPrivacyKey {
        public static final int constructor = 0xbdc597b4;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeySavedMusic extends InputPrivacyKey {
        public static final int constructor = 0x4dbe9226;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyProfilePhoto extends InputPrivacyKey {
        public static final int constructor = 0x5719bacc;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputPrivacyKeyPhoneP2P extends InputPrivacyKey {
        public static final int constructor = 0xdb9e70d2;

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static abstract class messages_ExportedChatInvite extends TLObject {

        public ExportedChatInvite invite;
        public ArrayList<User> users = new ArrayList<>();

        public static messages_ExportedChatInvite TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            messages_ExportedChatInvite result = null;
            switch (constructor) {
                case 0x222600ef:
                    result = new TL_messages_exportedChatInviteReplaced();
                    break;
                case 0x1871be50:
                    result = new TL_messages_exportedChatInvite();
                    break;
            }
            return TLdeserialize(messages_ExportedChatInvite.class, result, stream, constructor, exception);
        }
    }

    public static class TL_messages_exportedChatInviteReplaced extends messages_ExportedChatInvite {
        public static final int constructor = 0x222600ef;

        public ExportedChatInvite new_invite;

        public void readParams(InputSerializedData stream, boolean exception) {
            invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            new_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            invite.serializeToStream(stream);
            new_invite.serializeToStream(stream);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_messages_exportedChatInvite extends messages_ExportedChatInvite {
        public static final int constructor = 0x1871be50;

        public void readParams(InputSerializedData stream, boolean exception) {
            invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            invite.serializeToStream(stream);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class InputTheme extends TLObject {

        public static InputTheme TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            InputTheme result = null;
            switch (constructor) {
                case 0xf5890df1:
                    result = new TL_inputThemeSlug();
                    break;
                case 0x3c5693e9:
                    result = new TL_inputTheme();
                    break;
            }
            return TLdeserialize(InputTheme.class, result, stream, constructor, exception);
        }
    }

    public static class TL_inputThemeSlug extends InputTheme {
        public static final int constructor = 0xf5890df1;

        public String slug;

        public void readParams(InputSerializedData stream, boolean exception) {
            slug = stream.readString(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(slug);
        }
    }

    public static class TL_inputTheme extends InputTheme {
        public static final int constructor = 0x3c5693e9;

        public long id;
        public long access_hash;

        public void readParams(InputSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static abstract class photos_Photos extends TLObject {
        public ArrayList<Photo> photos = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int count;

        public static photos_Photos TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            photos_Photos result = null;
            switch (constructor) {
                case 0x8dca6aa5:
                    result = new TL_photos_photos();
                    break;
                case 0x15051f54:
                    result = new TL_photos_photosSlice();
                    break;
            }
            return TLdeserialize(photos_Photos.class, result, stream, constructor, exception);
        }
    }

    public static class TL_photos_photos extends photos_Photos {
        public static final int constructor = 0x8dca6aa5;

        public void readParams(InputSerializedData stream, boolean exception) {
            photos = Vector.deserialize(stream, Photo::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            Vector.serialize(stream, photos);
            Vector.serialize(stream, users);
        }
    }

    public static class TL_photos_photosSlice extends photos_Photos {
        public static final int constructor = 0x15051f54;

        public void readParams(InputSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            photos = Vector.deserialize(stream, Photo::TLdeserialize, exception);
            users = Vector.deserialize(stream, User::TLdeserialize, exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            Vector.serialize(stream, photos);
            Vector.serialize(stream, users);
        }
    }

    public static abstract class ChatFull extends TLObject {
        public long id;
        public ChatParticipants participants;
        public Photo chat_photo;
        public PeerNotifySettings notify_settings;
        public TL_chatInviteExported exported_invite;
        public ArrayList<TL_bots.BotInfo> bot_info = new ArrayList<>();
        public int flags;
        public boolean can_view_participants;
        public boolean can_set_username;
        public boolean has_scheduled;
        public String about;
        public int participants_count;
        public int admins_count;
        public int read_inbox_max_id;
        public int read_outbox_max_id;
        public int unread_count;
        public long migrated_from_chat_id;
        public int migrated_from_max_id;
        public int pinned_msg_id;
        public int kicked_count;
        public int unread_important_count;
        public int folder_id;
        public boolean can_set_stickers;
        public boolean hidden_prehistory;
        public boolean can_view_stats;
        public boolean can_set_location;
        public boolean blocked;
        public int banned_count;
        public int online_count;
        public StickerSet stickerset;
        public int available_min_id;
        public int call_msg_id;
        public long linked_chat_id;
        public ChannelLocation location;
        public int slowmode_seconds;
        public int slowmode_next_send_date;
        public int stats_dc;
        public int pts;
        public InputGroupCall call;
        public int ttl_period;
        public ArrayList<String> pending_suggestions = new ArrayList<>();
        public Peer groupcall_default_join_as;
        public ArrayList<Long> recent_requesters = new ArrayList<>();
        public String theme_emoticon;
        public int requests_pending;
        public Peer default_send_as;
        public ArrayList<String> available_reactions_legacy = new ArrayList<>();
        public int flags2;
        public boolean can_delete_channel;
        public boolean antispam;
        public boolean participants_hidden;
        public boolean translations_disabled;
        public boolean stories_pinned_available;
        public boolean view_forum_as_messages;
        public boolean restricted_sponsored;
        public ChatReactions available_reactions;
        public int reactions_limit;
        public TL_stories.PeerStories stories;
        public WallPaper wallpaper;
        public int boosts_applied;
        public int boosts_unrestrict;
        public StickerSet emojiset;
        public boolean can_view_revenue;
        public boolean can_view_stars_revenue;
        public boolean paid_media_allowed;
        public boolean paid_reactions_available;
        public boolean stargifts_available;
        public boolean paid_messages_available;
        public TL_bots.botVerification bot_verification;
        public int stargifts_count;
        public long send_paid_messages_stars;
        public ProfileTab main_tab;
        public long guard_bot_id;
        public boolean has_welcome_messages;
        public ArrayList<TL_communities.CommunityPeer> linked_peers = new ArrayList<>();
        public long inviterId; //custom
        public int invitesCount; //custom

        private static ChatFull fromConstructor(int constructor) {
            switch (constructor) {
                case TL_chatFull.constructor:
                    return new TL_chatFull();
                case TL_communityFull.constructor:
                    return new TL_communityFull();
                case TL_channelFull.constructor:
                    return new TL_channelFull();
                case TL_channelFull_layer225.constructor:
                    return new TL_channelFull_layer225();
                case TL_channelFull_layer212.constructor:
                    return new TL_channelFull_layer212();
                case TL_channelFull_layer204.constructor:
                    return new TL_channelFull_layer204();
                case TL_channelFull_layer197.constructor:
                    return new TL_channelFull_layer197();
                case TL_channelFull_layer195.constructor:
                    return new TL_channelFull_layer195();
                case TL_chatFull_layer177.constructor:
                    return new TL_chatFull_layer177();
                case TL_channelFull_layer177.constructor:
                    return new TL_channelFull_layer177();
                case TL_channelFull_layer176.constructor:
                    return new TL_channelFull_layer176();
                case TL_channelFull_layer167.constructor:
                    return new TL_channelFull_layer167();
                case TL_channelFull_layer173.constructor:
                    return new TL_channelFull_layer173();
                case TL_channelFull_layer162.constructor:
                    return new TL_channelFull_layer162();
                case TL_chatFull_layer144.constructor:
                    return new TL_chatFull_layer144();
                case TL_channelFull_layer144.constructor:
                    return new TL_channelFull_layer144();
                case TL_channelFull_layer139.constructor:
                    return new TL_channelFull_layer139();
                case TL_channelFull_layer135.constructor:
                    return new TL_channelFull_layer135();
                case TL_channelFull_layer134.constructor:
                    return new TL_channelFull_layer134();
                case TL_channelFull_layer98.constructor:
                    return new TL_channelFull_layer98();
                case TL_channelFull_layer99.constructor:
                    return new TL_channelFull_layer99();
                case TL_chatFull_layer87.constructor:
                    return new TL_chatFull_layer87();
                case TL_channelFull_layer122.constructor:
                    return new TL_channelFull_layer122();
                case TL_channelFull_layer121.constructor:
                    return new TL_channelFull_layer121();
                case TL_channelFull_layer110.constructor:
                    return new TL_channelFull_layer110();
                case TL_channelFull_layer103.constructor:
                    return new TL_channelFull_layer103();
                case TL_channelFull_layer101.constructor:
                    return new TL_channelFull_layer101();
                case TL_channelFull_layer71.constructor:
                    return new TL_channelFull_layer71();
                case TL_channelFull_layer72.constructor:
                    return new TL_channelFull_layer72();
                case TL_channelFull_layer52.constructor:
                    return new TL_channelFull_layer52();
                case TL_channelFull_layer67.constructor:
                    return new TL_channelFull_layer67();
                case TL_channelFull_layer48.constructor:
                    return new TL_channelFull_layer48();
                case TL_chatFull_layer122.constructor:
                    return new TL_chatFull_layer122();
                case TL_chatFull_layer123.constructor:
                    return new TL_chatFull_layer123();
                case TL_channelFull_layer123.constructor:
                    return new TL_channelFull_layer123();
                case TL_chatFull_layer124.constructor:
                    return new TL_chatFull_layer124();
                case TL_channelFull_layer124.constructor:
                    return new TL_channelFull_layer124();
                case TL_chatFull_layer131.constructor:
                    return new TL_chatFull_layer131();
                case TL_channelFull_layer131.constructor:
                    return new TL_channelFull_layer131();
                case TL_chatFull_layer132.constructor:
                    return new TL_chatFull_layer132();
                case TL_channelFull_layer132.constructor:
                    return new TL_channelFull_layer132();
                case TL_chatFull_layer133.constructor:
                    return new TL_chatFull_layer133();
                case TL_channelFull_layer133.constructor:
                    return new TL_channelFull_layer133();
                case TL_chatFull_layer121.constructor:
                    return new TL_chatFull_layer121();
                case TL_chatFull_layer98.constructor:
                    return new TL_chatFull_layer98();
                case TL_chatFull_layer92.constructor:
                    return new TL_chatFull_layer92();
                case TL_chatFull_layer135.constructor:
                    return new TL_chatFull_layer135();
                case TL_channelFull_old.constructor:
                    return new TL_channelFull_old();
                case TL_channelFull_layer70.constructor:
                    return new TL_channelFull_layer70();
                case TL_channelFull_layer89.constructor:
                    return new TL_channelFull_layer89();
                default:
                    return null;
            }
        }

        public static ChatFull TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            ChatFull result = TLdeserialize(ChatFull.class, fromConstructor(constructor), stream, constructor, exception);
            if (result != null && result.available_reactions == null) {
                if (!result.available_reactions_legacy.isEmpty()) {
                    TL_chatReactionsSome someReactions = new TL_chatReactionsSome();
                    for (int i = 0; i < result.available_reactions_legacy.size(); i++) {
                        TL_reactionEmoji reaction = new TL_reactionEmoji();
                        reaction.emoticon = result.available_reactions_legacy.get(i);
                        someReactions.reactions.add(reaction);
                    }
                    result.available_reactions = someReactions;
                } else {
                    result.available_reactions = new TL_chatReactionsNone();
                }
            }
            return result;
        }
    }

    public static class TL_channelFull_layer110 extends ChatFull {
        public static final int constructor = 0x2d895c74;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_view_stats = hasFlag(flags, FLAG_12);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            ExportedChatInvite invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (invite instanceof TL_chatInviteExported) {
                exported_invite = (TL_chatInviteExported) invite;
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_12, can_view_stats);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            exported_invite.serializeToStream(stream);
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            stream.writeInt32(pts);
        }
    }

    public static class TL_chatFull_layer124 extends TL_chatFull {
        public static final int constructor = 0xf06c4018;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
        }
    }

    public static class TL_channelFull_layer124 extends TL_channelFull {
        public static final int constructor = 0x2548c037;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
        }
    }

    public static class TL_chatFull_layer123 extends TL_chatFull {
        public static final int constructor = 0xf3474af6;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
        }
    }

    public static class TL_channelFull_layer123 extends TL_channelFull {
        public static final int constructor = 0x7a7de4f7;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
        }
    }

    public static class TL_chatFull_layer131 extends TL_chatFull {
        public static final int constructor = 0x8a1e2983;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
        }
    }

    public static class TL_chatFull_layer132 extends TL_chatFull {
        public static final int constructor = 0x49a0a5d9;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_channelFull_layer132 extends TL_channelFull {
        public static final int constructor = 0x2f532f3c;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt32(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt32((int) id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32((int) migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32((int) linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_chatFull_layer133 extends ChatFull {
        public static final int constructor = 0x4dbdc099;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_chatFull_layer135 extends ChatFull {
        public static final int constructor = 0x46a6ffb4;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_16)) {
                theme_emoticon = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                requests_pending = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                recent_requesters = Vector.deserializeLong(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_7, can_set_username);
            flags = setFlag(flags, FLAG_8, has_scheduled);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            participants.serializeToStream(stream);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo.serializeToStream(stream);
            }
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_3)) {
                Vector.serialize(stream, bot_info);
            }
            if (hasFlag(flags, FLAG_6)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_12)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_15)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_16)) {
                stream.writeString(theme_emoticon);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(requests_pending);
            }
            if (hasFlag(flags, FLAG_17)) {
                Vector.serializeLong(stream, recent_requesters);
            }
        }
    }

    public static class TL_channelFull_layer135 extends ChatFull {
        public static final int constructor = 0x56662e2e;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                requests_pending = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                recent_requesters = Vector.deserializeLong(stream, exception);
            }
            if (hasFlag(flags, FLAG_29)) {
                default_send_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
            if (hasFlag(flags, FLAG_28)) {
                stream.writeInt32(requests_pending);
            }
            if (hasFlag(flags, FLAG_28)) {
                Vector.serializeLong(stream, recent_requesters);
            }
            if (hasFlag(flags, FLAG_29)) {
                default_send_as.serializeToStream(stream);
            }
        }
    }

    public static class TL_channelFull_layer134 extends ChatFull {
        public static final int constructor = 0x59cff963;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                requests_pending = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_28)) {
                recent_requesters = Vector.deserializeLong(stream, exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
            if (hasFlag(flags, FLAG_28)) {
                stream.writeInt32(requests_pending);
            }
            if (hasFlag(flags, FLAG_28)) {
                Vector.serializeLong(stream, recent_requesters);
            }
        }
    }

    public static class TL_channelFull_layer133 extends ChatFull {
        public static final int constructor = 0xe9b27a17;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_view_participants = hasFlag(flags, FLAG_3);
            can_set_username = hasFlag(flags, FLAG_6);
            can_set_stickers = hasFlag(flags, FLAG_7);
            hidden_prehistory = hasFlag(flags, FLAG_10);
            can_set_location = hasFlag(flags, FLAG_16);
            has_scheduled = hasFlag(flags, FLAG_19);
            can_view_stats = hasFlag(flags, FLAG_20);
            blocked = hasFlag(flags, FLAG_22);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            if (hasFlag(flags, FLAG_0)) {
                participants_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_1)) {
                admins_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                kicked_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_2)) {
                banned_count = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_13)) {
                online_count = stream.readInt32(exception);
            }
            read_inbox_max_id = stream.readInt32(exception);
            read_outbox_max_id = stream.readInt32(exception);
            unread_count = stream.readInt32(exception);
            chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite = (TLRPC.TL_chatInviteExported) ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_4)) {
                migrated_from_max_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_5)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_9)) {
                available_min_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_14)) {
                linked_chat_id = stream.readInt64(exception);
            }
            if (hasFlag(flags, FLAG_15)) {
                location = ChannelLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_17)) {
                slowmode_seconds = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_18)) {
                slowmode_next_send_date = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                stats_dc = stream.readInt32(exception);
            }
            pts = stream.readInt32(exception);
            if (hasFlag(flags, FLAG_21)) {
                call = InputGroupCall.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_24)) {
                ttl_period = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_25)) {
                pending_suggestions = Vector.deserializeString(stream, exception);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_27)) {
                theme_emoticon = stream.readString(exception);
            }
        }

        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = setFlag(flags, FLAG_3, can_view_participants);
            flags = setFlag(flags, FLAG_6, can_set_username);
            flags = setFlag(flags, FLAG_7, can_set_stickers);
            flags = setFlag(flags, FLAG_10, hidden_prehistory);
            flags = setFlag(flags, FLAG_16, can_set_location);
            flags = setFlag(flags, FLAG_19, has_scheduled);
            flags = setFlag(flags, FLAG_20, can_view_stats);
            flags = setFlag(flags, FLAG_22, blocked);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(about);
            if (hasFlag(flags, FLAG_0)) {
                stream.writeInt32(participants_count);
            }
            if (hasFlag(flags, FLAG_1)) {
                stream.writeInt32(admins_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(kicked_count);
            }
            if (hasFlag(flags, FLAG_2)) {
                stream.writeInt32(banned_count);
            }
            if (hasFlag(flags, FLAG_13)) {
                stream.writeInt32(online_count);
            }
            stream.writeInt32(read_inbox_max_id);
            stream.writeInt32(read_outbox_max_id);
            stream.writeInt32(unread_count);
            chat_photo.serializeToStream(stream);
            notify_settings.serializeToStream(stream);
            if (hasFlag(flags, FLAG_23)) {
                exported_invite.serializeToStream(stream);
            }
            Vector.serialize(stream, bot_info);
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt64(migrated_from_chat_id);
            }
            if (hasFlag(flags, FLAG_4)) {
                stream.writeInt32(migrated_from_max_id);
            }
            if (hasFlag(flags, FLAG_5)) {
                stream.writeInt32(pinned_msg_id);
            }
            if (hasFlag(flags, FLAG_8)) {
                stickerset.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_9)) {
                stream.writeInt32(available_min_id);
            }
            if (hasFlag(flags, FLAG_11)) {
                stream.writeInt32(folder_id);
            }
            if (hasFlag(flags, FLAG_14)) {
                stream.writeInt64(linked_chat_id);
            }
            if (hasFlag(flags, FLAG_15)) {
                location.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_17)) {
                stream.writeInt32(slowmode_seconds);
            }
            if (hasFlag(flags, FLAG_18)) {
                stream.writeInt32(slowmode_next_send_date);
            }
            if (hasFlag(flags, FLAG_12)) {
                stream.writeInt32(stats_dc);
            }
            stream.writeInt32(pts);
            if (hasFlag(flags, FLAG_21)) {
                call.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_24)) {
                stream.writeInt32(ttl_period);
            }
            if (hasFlag(flags, FLAG_25)) {
                Vector.serializeString(stream, pending_suggestions);
            }
            if (hasFlag(flags, FLAG_26)) {
                groupcall_default_join_as.serializeToStream(stream);
            }
            if (hasFlag(flags, FLAG_27)) {
                stream.writeString(theme_emoticon);
            }
        }
    }

    public static class TL_chatFull extends ChatFull {
        public static final int constructor = 0x2633421b;

        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            can_set_username = hasFlag(flags, FLAG_7);
            has_scheduled = hasFlag(flags, FLAG_8);
            has_welcome_messages = hasFlag(flags, FLAG_21);
            id = stream.readInt64(exception);
            about = stream.readString(exception);
            participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_2)) {
                chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (hasFlag(flags, FLAG_13)) {
                exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if (hasFlag(flags, FLAG_3)) {
                bot_info = Vector.deserialize(stream, TL_bots.BotInfo::TLdeserialize, exception);
            }
            if (hasFlag(flags, FLAG_6)) {
                pinned_msg_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_11)) {
                folder_id = stream.readInt32(exception);
            }
            if (hasFlag(flags, FLAG_12)) {
                call = InputGroupCall.TLdeserialize(stream, stream.rxœì]sÛ6†ïû+´wîŒÆ•(É²›éE“n:ÉìfÖ™î%!‡EjI*»Óÿ¾ DÉ	ğKì4.Ûx88çye)¼ßÂtä\ÉÏs¹Jı(ü¾ß{üşÅw½ƒ×ŸG?ù‹ŞÕ{‘¼ÄÃÕB}Iú½×o~şÕ¿ÿ¾÷¿£·f¯4Ü•ŒıÈëıÔKÒXŠåµúR2ßÉäDkò!Ö«¹×“±R÷È]‘(ŞJ_¿{ãÉDù$ÿ¿òjëT¿Ê¹“ûæFß7ïåRºr¥ş<
ûç>ığáÔšjíÆò?k™¤‰˜ĞSVÎ02&Ãs¦nn_ÆÙˆü.çi_ŒÈ›HİønTNôãVë‡ø(ü@Ì©\óìê™'¯Ş‹ô_»Ÿ/?Iœ¡ÏrÜÀ_úi·±úó»ı·«õ,ğç½‘ïõö÷÷.ºß\ôêŸëtµNïw¿÷~©ÈËİøû©Üú¡&±úå:Ï‚'›ûÌ\—ié¾§ıŞ\„®ú/w­
ÅR¶h}Ûï©Ît“ù{é­éµh:¼éÁ¿ıÔ×AĞâ"ÎpëÀ'Ì#u©¥Lñ “Â%Êµ¹Hõ»nÆW~ñ†ß’¯b­ÓÂÛV"Vwä¯D˜&×åaÎGôE³‰©—sõ´¸«÷Q5½şñcª._|É†=U7q²“Ã‘ÖKùyÅ©ô\?ü¨z¬›«&›z“ù‚V^;fQªÜXDléwò´Zùa¨nw™<¸¥¹ÓpÍ64µˆOÆÍ¦•Ú·í’!0)ßÏc¨r‘hÄò]ÖO|¹8^òlnïå.-F6­Ÿ³£°¡iœ9|°;˜†P@×¿GÁAÅş¿ıºù' ’T¨Ğ›"IzïŞ¸Ù‚şZm~n ¾Èx8ªÅ3UÃvôŸ^_fá‡"èùaÚ;ˆÔ9ø<¿óFÃáèö…>ÉÂ˜·"Ëäê·Ğ{d‹gHÄX…ÚoÒM££bĞ¡ÚêÆbZhvmÚÜ7­òáGíí&Ç6ûy«ÓÀánŸ²o ‹±l× !;{m‚…³ÅÕ…€"?ìıcóÛû]”qh‰(÷şÿ&®ß¶áÉÅz_w‘Šöx¶wK-êÉõËH¹¿ˆ~üñÈûS=ÓïgGáÍÎ­úØgéœÁ¢9RÆ6+â¯Yñ*‹‹.Ÿ¸ ©CR‡¤Émr…¤I’$5HjX³ö•'5:äÆã}áU÷$ƒ7¼•ÒqnH2d É@’$I†*“$jì’dh%¹|ó/Z‡ò±hî¹r	ù‹\B+[äÈ%TZ'—p±\BÑra4o¢§%¢årúé—MÁBRáÕË—ÓŸ'ƒésK*œû¤Şuà‡Ô¼’¦(îğì·S_&×¯vã›…â‚Æ©«ï¼¥¯fã<Z‡QÛ*›úıƒ?Ï:äL6MT±@ş©£ÕKíC:c¬søœ4cË}p8‡-NÓ²¡Ã‰kqn°K6Ì?«0°µIˆÁXŞz#ñÜ6‰ì¨öÑ—ŸÜBŞV§wÊZßš%i61b“µR²Û÷<º«X¾÷Õ­_í†ƒ½ š‹¬LíŠ~6É®ïLı™ÍÓ­9EgÊ·F#£;@;m‡Ù“Lånbm9û‡­„©Ÿ¬ÄÒôşaÅ‰Ùİ™©iñ¶ÒX„I ¶‘ç'YìWêÇ0³)¡¶7?¸íƒGSûI¡ıfĞQ¼^ª3Å^Ÿaj]œ+±Zwb“•r?ŠÍ‹]¶Ÿ3±ü(ÃµÑåa±Ã'[œÔ6/ŞòJøê„+=_¸ê\}ªpy¬kú–×övéáÊ<~ği“¶Åg,÷{;BõíKO›Nˆcl\¼ósGÕí6Á£§íL¡á·ëmÎÄf=9Wßês_Q¨?yªÑìı®Î¢ÏîR|nY2Ú4V“·[ëu¸ißúÎ{¦|.eYç9–eÏ[h5u…>©¸ôb‘õÄ"–D7ÿ*ÖİfO_ãíç~3Ûú,ç™kÊú<Ü.—Ù£z¿ıA=—/cŞÕÓÔóW)µë§^~Ü?Û„7äÖNF¯¶§ƒ7ùo ”mÈ‰«øtyR-Şêhíµ:ã”¤Şåguf•¡çzb³8_†¾Øœ#]oŞİà*m—66mZú‡ä™Ğ!ÎåéÇ°„o“On²~PçŠÀÀBÑ¿Ò}Ùêù‘*şñ>;©âèŸîó“*FÃ&Uıæº››…í	çÅH¾}^ÁâTŸÚbŸÁRiØT‚ß¤¾”=ç?\gÃxŸùÒ?åŸÔJ´j=V®ş[}ÿ6ûş	¼Ó?’³(ÊV±Z¾´¹_8•ámn6;1osˆg°<Ô?]j½ıÃÂ°ß©ù²éGåÈÂß‡§»ã§úòûÁÿ<…Ï¦³Ô.¯i;cäTÇa›ü83ºÉ
[<Dì=ĞGªK¡B‹TÌ²=$~ ß‰ÙŒŒ!»ò°±çn²OVO]ÇŞ÷4Üä¨¯/åµ¸ÄÍIàæö¹Ë'´h=ôË¾6íœßfÛ4¿ëşù`Î _¨úµi¬&m^üë-<V/¾¯åõ5…¿VWö÷ÀVíÔ¿šB`«K¨­-¶ºÈ¤o,¶ºš^úa»Şöµ•Âvqú¥Ša»ŒûšÒ_»KLú†âc»ËÜô•Ävºëë*Ší¦ì oª,¶»úf¿V|R ùãS>+°X#¶‡5Šç”k•Ë‘_3ßÔÔĞa)Òfµ±lé°şXiIaŠµÓ¶\i¬mrX^<¾³¬ hVÜëF­·ÕD´+|•—
mÎb©­<²ºâ›ÅúšfU:YèQWT³+PĞ'4Ë_¡jf³0¦ÙzNÓ°4Çõ±s”À,«Iš
<Šå-«,³µãš–İ²UÉh^Èj¹Ÿ¬Jç©*ËŸ»y!%”¡ÀT#›ÑÔŸ.RR²ÜÇõOÎ½5–¡FsaŞÁÚtXÍj@–Å;–ìÖi´ñg£OÇ­´VY™±|g5ÅËÖš>Çå›£­ÇšÉYÊ"¶'d³*‡m«ƒ‚ãÊ…İâD)’3•+¬V$~øÁ|bJÅ,q£XE­“Ùk—¿±<†£a¹wkåˆí×Í?õ*¦íi9ÎÄ†œIå`æÜyÈ™3!gBÎ¤3È™."gB‘”ßŠ$I(’P$¡H²X1A‘T•¼B‘„"	ER[ƒ(’P$¡HB‘„"	EŠ$I/I(’ºxğäŠ$´A­Ñ¡BTÓ›hƒ/ójƒÏ6²d=ÈzõTúˆ¬YOcÈzõÔEÖcôYO÷>FÖƒ¬Y²*[ÈzXEÖsIYO7­ÍĞÙkm
Qèñ¦+nÓ±sçI7(nPÜ ¸ÑÎ7(nPÜ ¸y|•òö(nPÜgŠ­q7(nPÜ ¸AqƒâÅŠk†QÜt«)¡¸)ÚEqÓÒ;7ÛŠ7z´T´FÒ‚¤IKMo"i9¾Ì×)i1<š(RP¤ HA‘Ré#Š)MŒ¡HA‘RgEŠÑw)İûE
Š)(Rªl¡Hi`õ/¢Hé&õŒmüY•‰óËÍíàæ%"Dˆ<yhç"Dˆ<y<¾J©pDˆ<Œ³‘‡Ö8"Dˆ<y ò@äÈ‘‡5Ãˆ<º•iyí"òhé"í‘Çóy ±¨hÄ‰‹šŞDbq|$o¹GbÄ‰$H,X ±¨5ŠÄÂè;‹î}ŒÄ‰$U¶X4°j[bÑIë0¼›Úû³w‹Åhv;¹Eñ€âÅŠíœù†¨ò;Cu€ê ÕªTSæ¨ª²¨P :hkÕªT¨P :@u€êàñ…ê øøøøø¿ú‡ÿÀ=À=À=À}¥ ÷ ÷MŒÜÜ×¸7úpß½îîîî«lÜ|×ŸØCàg31o=xxxxíœ·€ÀƒÀƒÀƒÀƒÀƒÀƒÀÛšğ ğ ğ ğ ğ ğù¾¹¯ ğ ğ ğàäàäG•prprpòÆ— '?~“[7NNNNNNNŞ&•NŞÁ:898989898ù™Áî©ÅÏ6çƒÉXL»»»»µsæÂ`7pu~gÀÕÀÕÀÕÀÕÀÕÀÕÀÕm-WWWWWoÇ	¸¸ú««aœaœÏ¢‡q†q†q†q†q¶(ÃW›ƒ+nš’‡+†+Öô3\1\1\1\qQ¸b£ïpÅİû®®ø$ÒÒ·3é{cô½¹Ì¦ÓÑÒÒÒ÷/GúÍæw4444444ÛÖ"Ğ,Ğ,Ğ,Ğ,Ğìvœ€fffffffffŸ
š…3­6gÚ4‹g
gªég8S8S8S8Ó£p¦FßáL»÷1œ)œ)œ)œéæuqÎt´çL³vGÂtáÌæ³›€)€)€)€)€©µÜ€)€)€)€iıº`
`ÚÜ.€)€)€i ¦ ¦ ¦ö˜vË˜6wôÉS€ÆŠÖ    Õæ ›fM5ıĞĞĞĞXc Ñè;@c÷>hhüvÆnŞÅ¿ì>uFg:óÀñÀñÀñÀñÀñ¬evÀñÀñÀñÀñê×]p<p¼ævÁñÀñÀñjüÇÇÇ³g¯[¶¯wœğÔ¦Ş Ş Ş Ş*Fàààà­ÒG€7€·&Æ Ş ŞêŒ¼}xëŞÇ o oÏ	xëˆ 9ö´…3šL&ƒé9´ÿ  ÿÿì]iãÆı+Ú/Æ™DJ”('»Áz} ÀnbxÆşÖ½’(PÔŒ'°ÿ{ºyİÙjê=}°g¥&«ªêªW¯É(h  ]+$°dd H` XW‰ H` ÅëH`çD·ªájp«À­:·
l¦fq`3µØÀf›‰3Ï`3Í6ØL{„‚Í$ì;ØLòs6ØLı0ŒÆc/›$ÖÄ´&ÚÜ"p‹.Š[ÎN22pvÀÙgœpvÀÙg§«DpvÀÙgœpvâugœv8×|I,çEMšÓp5¨9—IÍµ¥Y¨-mq(P[@máÌ3¨- ¶€ÚjË¡ ¶ûj‹üƒÚòª©-{”XŒ(æ¹ŒfJx.ÚÈ¹ú<ğ\æ¹€{’ŒÜpOÀ=÷ÜpOÀ=é*ÜpOÀ=÷Ü“xÀ=÷ÜpOÀ=%”PB@	%„/”a „€²O((!Â¾ƒ"?Ç „€rJˆ¦îåJÆØtF‹ÙÄC@)|@Ù/ÄCNFyï}	)$¥‰|Â#J…‚0Â# Œ€0ÂHÄ Tá®FUxTáQ…GUxáÊ¢
¿Gªğ¨Â£
*ü+¨ÂËÕMu]]İ”,FÖÄr\ÔMQ7í£nÚ°Ä¨›¢nÚ8·¨›
òº×P7­—7G:+ÏôÍK%Rw¸vˆ¿ª°‡Tpã¯«îz°U£î}Pw½ºëábQwEİõbë®¨ç¡wŞõ<ÚŠ†s!·PÕ; —CU¯ùÒ«ªêUsœ7ïëİr©²ª7 Ë-áÜoMiÂóóOoËYÕj¾døçÛ¯/ z˜9&Ô[HBñ85ÄL+QIl”†J¢²J¢d•KáéÀÅL&SİB•U.T¹
T¹PåB•U®}iª\ü¡¡Ê…*W“\T¹PåB•k€:êH¨#D7EI‰ Ô‘Î©$YB‰¦£@”hP¢A‰¦•´‹+ÑÈUN†#u•m8Ó&ÏU<Mådo1C«V'dë-¨j$#CUUT5PÕ@UUT5PÕ8“ª°öWµë Ş-€z ßm# ß@¾|ùn-	È7oA• ¶
©î3ÓÔ‰¡M Ø^`à5€W ¯ ^¼xğ
àõ\€Wş~¢
xªy¨&`IÀ’€%K–,y^°d[Ó–ì)œÍÔ…£ÉØœM§À	Æ÷Nœ8!pBà„À	'äJ`À®¡ ».‚ Ø°`À€]ML?ğ™©ğ`´cN­©†GÊ?Kîüøğ3àgÀÏ€Ÿ?;{üğà)NCÀS€§ O<•Kºxª%&æ€9­ B¡$¤“¡nM´qÏhP·X§‚ ±„å§ÂW*Ã9¤HÿZ§§¼[Ç…Ejëû^Óö+²›
9ªmOts6È~tpy22Àå@®º t)È5ëÆîó“½¸÷ ¥8uÌ~åh9 o Ş€¡C†>ºIXk–daÃ>ï.Ã3S]<3Xclj€g Ï <xğàÀ3×Ï *T¨P	 @%Í¯*‘C0¾-A›.ÆÆÂY ÁE0 D$# @€ˆÒ@D ˆ  p €„	=z$ôHè¥úî§â4½Ê=ç:¦£'? E†\+Ö[ÛùÖy$înI\Á5æ‘Óã#ñëæĞûM—şlòH‘<‰ÔÄ¾ßT«]Ï&§È?NğÊ.AZÔ…¿DöÇÀßm>Ò/®/á™”­˜7å]à,R™cª²Î\® pÜğ¶›gêâkúˆõ¦±W9NLZ­úÈÆ¤š=uf®=BPŠ A)‚R¥ÙçÀ aÂ>„}—öuÆjR—~n–né#Ã=Ë§¨Kc¬X!°zåÕe‡9ˆUê+Ê>ˆUêÚ%V‘"TU‰ëêÖtvveÆ³}Z'ööôƒ½½A°·ŸÁ£IÚ±íbÛå ª;A33ÛXÓsÛ|Op‚Ga’‘á(ÂÈGFˆqGa.4€m§rÖä4øN‹ğá´N‹à´ˆ@`¡ğ)³©Mˆ1Ñ‘á!ÃKGv±ŞAÙ–l¾”\ç­˜ÿ¥«…Œ2.¾pd\d\È¸úÏ¸T$BY°qeD2Ô8iH†'C“ê[^y#áÈ5MÇãe#»Îd¨‚¡ô„D‰*áH„H„]F"„Š’ g«Gö©‘ÑXsİW•!›IFvÙJ;ÈhÑœoFó
’¤W› N‚aĞ=p÷—®º }aÙ#Í²Îò¹gÚ´#hÏ>Ú´#ºDt‰è2ÿœstÙ&ˆòXS÷™…`êßöo4B•!€X=±‡ÏZGºò·ê/iĞ´}ôŸÙ‹k™-7´ò–dÖ~f·_íB2§Ëå-k?G#õ‹9Ù;º¤Ô{±ÿŠ¥Ğyd;g·ÜßêÑs	]ÜµK‚6’“«’æ¸aÊ;LS<ZY¢6j´´Â²İ-Y4 ns[ÔŠ7ïJJòÁšèßÖä¹áoktŠp¬+#oêÓõ›¤³7Ù|”æ‚·ë
¶º“…ÿw¢•Å|K»ÖKpÛ£raü­$·põq<ÿ)‘‘Ra53>ş[ÙEÆ(x*åË0m”Yğ{Ê%‹^õSğÇ[–SGÀíÂWùp3Z¯’÷è)ĞŒE.£×3wª}ÚwFïÜM<cE#WjÈAËîËz•¾Á²éµYã ×Ú…óOşƒ·¾÷¿5/²„]Õ+ûŒµª²²ĞŠ…Hğì…Îã ä"¸ï›Ø…OôÙB›™ÃojØ'‡`Q7–Y7ª¡Wú±é0¿ÔJEšd6Ó&R?Ç(ñ½/'z4º†c)Ùw;Ç!ÛZ¼Ù ¹ŠĞqÓÊÂÆÿmJæ*ãÉT½ª\İs¹T‹ø¹ÍM=: znô’ÿüw2¡=ƒéIº„‚Q·*±
íğ‡ °^¸ßÎŸŒº¡]>¢h¸‡)Tfœ*5+v|År¹ÇI¹§VQ®L©¢1K¥Ÿ«P¡Ò- ®Ñ½?ĞÿĞÈãQš}•ı«g]*É¢]­wGUÂrj*TRéø.Q	OZ®nŒÜ©)OZË%Íh›í#{­—°³šŞÌ×»•ÍÁË·³6›9Ÿ÷4[yÛ-Ì›ä&ı£{ƒMã"a³İúËÚ^‹ºù'oş=ö¸ïÔnı]8ì~›ÄQy«÷ÅÈ)¹G\I}µ¨»tä[kíÚşïMhI|–$ÑÎâ] ÎâumqÍ²¬WfV,HÀbV«-%{\¤qE¥Ö%¸hÌ½¨`k‚ëş¸Ë\T}JÙÍZ"<|¡j¹u¶e¾­§«ŞhÇ‰ö0K¢YÍ„¸xê±©j=¼©C‡‹µ›ŠMt¸v|S1×7uépùô¦b(®İTìE)>Ê·*G¯n+%ûR
“ÕØÕ¨›×4²‹‚Õ(ÀÑ¾¥ËtÿHV¤‚–_ÓçvKQ†š¶¦iºaµÃ‘ì´‡[º²§öH#–ÙQä¿¼‡ÇPN¢3²4}<Ñ;JüÈÔAv”×XL§¤%@—É¼§ZA\9‘Û5µ‰Ùub¿£Ö­	ÌÔ´W°¢‹™±æFÒ=UJ­A eí4öÈ(”=µÊ‹zbJŸ:ˆ‹|ì&”Œ=uT3vê¯”<u—ç>ğ(±V€“éhh;NòÉ:
âI*"ì‹.ûW[d!¾·ZDálô#ô7ŒöÑ
Éƒ¼°¿•à†ÛuŞ;ÔîËiüÿQÃ½âÛî8ÔÅLËGğ~°‰úÎ…ÛÄ:ËÿQt˜+¹¨Ó¼]tœw}[çË>OT-U 6kån«¤6u>Ò‘ªcó‰İd¢[Ö=5>“Nt[
ïŞun¸ğd_4íÜY%	ÔézÇ6IøÒ_?ØxxEÔèG+*¾%U+ÉEöé<r)ÊrøÅÔÑlm´h—÷yiîÈr!™hêšf˜NKâK&P.˜™¦9qºîûÕ&ì’×$j®5›¤%\Iü!ğWŸ©–YDF®àVñ£Z´‘^Ü7ä{–İîĞ.ªè—LgZà™‰õŠ?”ì(óR¹yK\‰Ÿ{BâÕ|óA£NœIÏ¤„d{¨mÿ“±¸t—om/;õş^iK{™Œ{o»Â`eÕ!r¹Jt"õùc	«dÒ-¨>ÿLÿDÁ7ãŠŸÖ³=1é´«YwTêİ9ÙÑºOm¦lòäCpî³àÚX~'•Î6g%
íšúL7yôÇ£xù&‚ÆÅ{ùìÈö}=/¤T¢(º«iScÆyy0<_Ï'¸äÔªÛ¿çk°†LÃ›TüÉs‰G;÷ıÊÿÍûl_v›L³M”	tMÃk#şA„‰ë_{S1]aÛr¾<ìüÒÜñ—¾ £÷hdTZõê~0‹=pVq`µ©èè@3íºÙó …úåZCS_pêx±¾ÄòîHHïı¹%uX>ÒÄä÷şu1ïHêOó^ªôªù€®QëÓ9–t»t†òùëßR*8pn-p`1Ÿı.ag¿lê$ñ¼%ùäÇÇ©©8É	Î½Ÿ¹ßÖyììÛ-ísí×§ÿ'ƒÈ	Ìé€ƒpÖ;›W¾è|’Vãébû"Y>‰}"Û\,nx3TgÚ3„ÈaE¥¡Úyû/×wv+Æeß¨cŞeèıxdØöÌœ¶ÃN³ı#‰º5Cã&æHs£%„_•ªOå¤ºd4²‡³qG©’Ââı°£¬Ò¾,YI½’ñæxxV|.ZI¦¶Òˆ8û0Bo"àM¬÷ƒ¯¾Jnu›:ÎÁ»ø'Æ³¼¹§ü—Ğ[no½m\¤IîÀœó×ìoS{fb†ƒ?ş(Ztô—Aœ¿Ğ™t–'ïK¾#›€°j¨kÉ:[êˆH&Gîí“¿¤¿¦wáËá]ÎşXÆW&çÖy´‚áÛêa‘ôóç€,·D¶—…9–ê¨6éÔßÚ·#hµ¶¿¬­'Ë[Zt›å-ìŸ¶í_±É¨>óÊÅ‡´>Ô§»CÏ‘8[®N‡U
«PŒŸTFíÏ]`:v×Ğ˜Åf—˜Û&k9JşrÈC+Ï{[TÍÔ[°y•Í‡³ÈB•å¤Î¹=Ü¦¦îÙoåDª,ç»(×º€3sõ¶ûÓza~ ûºo™¬cy©eyÕ}Ø-Éi.ŞÏÀ	ÔWï"À5uõì&{qÎÒ[“*CÌƒƒ†?ÿ'Ÿıü@|!¢è…Ë:Î—üh¹.Í&„hä&ğÙÔK™ÉïOd½#<Jkéw.¦ù3Ù,_’ÚBÀş¯¢ˆnµŠ§«ŠL&ôû5ñËûaÿ÷H§gcĞ<xMDÏàHÚ,¼`ÎÙ›4E-è:77xr¬@üøâµ?&ö†7Ø´‰·~"T!WÄõ,a£…8,hH«–Û•µ\îi¹²Ö;Kü°“­µ¨wšéç†z#¿>äœ¦NN
ÿ²Àÿİ[Ñåœ¯OôŠPiwA½ƒŒğtû³ç<¦fĞ¿ç™R	öÔš±²¦|Ì]äÛÊv[ó¯pÖøÍZ%qŸÙZüÙP”{L§m¨¬F¡İ{G±¿?êúÑY>õyIß5%½(Ü¯s_>ìB_I'Ø¤¤']+ëEzÃÎ½¡fÒm.¾Şì²6ÿ  ÿÿì=i“ã6v¥³RN•Ê)J¤vË½ëéñlœò1åÍ~H¥T< nÆ’(SRÏLªœß€'> êè>Ø3C Ç»ñğBÌ˜ªsÒ9™9GòPØdu±¬¥^"ÁªÌF×$¤aÿ°ÃZr¨‡_cIÏáŸ(xÿ¬eÅX2s ¤ŞĞ)PC±3 =ÕÍ›GîÕiKÌı˜AyÅÊ{ZëÎÑ+ªi¿#¿µ’±°g6ïıÅØªÁÍK#G§;13Š¤|…¥$Õ§4Ÿ¤:•6•T'¹Äì´½…ÁP¦Ø—œ§c›ŠS²ôrŒêoV QÃwú[–£%´¦€àou®VÈ³2Ï÷‹ùa5¬¬ü]7+«}F†8„r[é›à–”«¢WÄmU2vûŞ:ò^Æ¡WÈ>ß8+ß¸	:#~0J‰»pÂµg%;.KU…û[‹œ…‹LQ—0)j77BYjš=±C­
4(Pdp“ï”Ø1ÒÁ™c”ó^Ñœ±y.â»N`Íƒ×ÌD±ÂmG!Ö«" æ¦§—†Šö**îE]]4{ ELu@‚\Ğ¦¯ÒËPî(”K~ëúÄûÛVÇ|Ã\A§.ny‘F!/‘²!ÿnç‰\2ãKd7`ÃñO¡õÂ!t×‹9”ë²ü¤"¼+èdœ_
Óé)Tcq"œÛ½¯“3Í-ÏY¬ÇÎr®|8Ø¿ˆ;–™ºNÁø›G@ë‡6ƒ¡uNëŸ„á«pÛ‘.Kú_¯Ofr×ˆÅm9^kD] D2¿
¶_ÅÖt­¸¶¡ÅüË‘òìÖğ¶‹tp’› à"([«Y9]úÑÒ¿ºÜ ôË3a9cÍö§[÷·õ*Ó­û›¿Qã4fëı’kœ¦Ş+6OéæuzñşöÆÏ4ĞõšØ³IƒŠ$z:“61ItŸOÚ4%Ñİ¤%ÑÇ›döz	À¤0-İ˜Ğ¢ßÁj”YşÂr‘K(tSãå„ØUs³&õ:9Ë•Ç<_Öùú8§o8'…±ç!2‚²*Y¼1ÔI–³¹ã/ƒé²­JVÏöŸãı³²U›½JÑï't8"~’‚#şÜú˜3Ä;Œ™ag¨Öj‚Uå·E¿<)*oğ”¦h¶+6f	TÉÑß¬üm^¹ñº¸79Š¦Å¾èÀukùÇÔáhÑ a_Tv´x|ê¸Çc–%ªH­‡Æ ™œÛ×-;.í&èÒ­IiÈéÏ„Bté”‚ÏV(ºNjB˜U‰úİ›Ñ™F÷ëÔV[y’°¦4GüeÍÇ|6Ğ–R{¹tmwÌÅ0“É}Há\)ä×S2t1¸™`únë¿9È ¨xH¶oÔb‡åQš>0ym©<™k/ìVÓĞ—èqí.´ïÃ*ŠáépÀ("BMÀv9–ëa<üìk­Ôc÷¥P,éyë‚ò§‘ODXÅ» ù´êk•œ=ÍN»¬a_©ó‡gÿxâÿŠ-¡şJŠiİg¤Ë£wœìYŠ¤wÀ§K¥ó›ÉLç"Z6½k×Y<]õM‰˜#\Ø.§¨åÄt®ğ±Ÿ/Hƒåêi&#3JÉo:Îš0&áƒ.¹PÇx„]	wi‹JÌ®á:µKJÒY’cÊ¨%-ŠĞˆôh26ÅòhÜ=œïEgH­ª3Ÿ†ÏdrTQìúòk ş1__ª³—¢OØĞk}/øùá]¼!³\gÿµ™öû*EsáçU'š°AU¢ÑDIhŒ ­B0ğ_‰úP ˜ »5Ç3Ü¸‰:tRñÒ§Ç‡Wl®’˜Ê·Z=Dˆå®Â¿Ûï¿!wXŸYe;€‡ (wŒù©ŸÁ\Øy‰8¨dğ<şòóú›º‹ğávq¼æ2ÏÂèš işÖø"ÃÖE¹í:™A
©ÌÿÒ°ŠªÏÂ7£pû
ÅF¸S¾¸Æ\ì1Ù|›€ŞBx7É.©ëRØ?ŸWèÓ>I(úÿ%IãÿÍ‹ëh`]3s¥wÊXà;
>Ñıwö¿Cûé˜÷YxQ|š1ñ›Ép)ş(¢,«k×É¿$+|ivxƒÿ)s	^¥ÒÓ&ôÎtrõb²•Š9sÃÅF« )ë¹B•òr&aò‚Ò~Ácüâ
Å§(>ì7şçÔ„¬ Ï±]ˆ¸K-*¾ğ~w–Óè¬pÒŒò>‹RèCAÛ Eà’Àù“EçŸ?ÆÑ.ÂœFñÓsÛ &¢S
—{.àûóh}­‚lÊ¢™­yLY†¡>ãY€%G²9øÇ®Â+"wÄ3
NGtç—”ª¾R ŠÈí¿|óMx:“-oİØn_Ğ{olÕ”#ŸJJS•0´]#o:óÀ»¶ıÈÏO~=å€ı…ƒ,WTñfiªXhy¾º– f`‚Õ)úaú3ùSÑ[÷f²áVàíg[–o¹'./8¬–ãºH^±¯ÅRİ‘cùsk!aP şGºyåQ¼•€º3Ë
}Ë’úsrü)‰âuÌ¯ªÜL1¬rä–:háÜ[D¾%G(ÄçêÍ=gé¸3Õ-.Ÿ5NWî{öEr{Ãœ{zÃWª#F°|dôÁR¢¨8çsÑqm‘±Œ–ÊóÏ²A®-¥•×‘}A&g³È¾‰¢ô]©~DÖñZ¾«“vºµ+Uó Â7wòÏCŞ¢«K‹Át:C§3ãd¼ iŞ¤æğËÄÒaº „Í‹™­¦?ú¢dnşé‡éÂH]X”úáy ¼ÜBá—ğòJË÷Û»ñŸpÙ%Ìœ)C»zÕõ^ãs.‰	Âìµa¸w„Ï°&¿Â3äK{ü:¯9gVY‘éíLÚ:Ëàç
^ÉGŞƒT ¥½ôNh,Úˆø—-ºâ‡
•r2RàP¹àıˆ®ü7åYqßV4Ô^¢È‰r`E•>q0Ê¨Ëì6}PŠÃàäÚ‚F… ¨T4jÀéäŠÁ¢_/R÷ËrÍğ`ÖÃ¬e¶Ê³+ÖıP¹uéTT•]¥kùÚ\Æ5`\Æ5 7)ã0®ãlyÃü2¡õ=y9ÁûjÖ=&yÙAØ'¿Z¶W¦ü‡g„™v¥T´.Xš¡k–ªï×ÕÔ¿ö£è»Íæ«êººe­¿ªşz»˜K»8Œ“Á8Œ“Á8Œ“áú’É
[AuWãÚPğ58z|eH™ñ5_ƒñ5Ã4¾ãk0¾†×ék0V°±‚l¬`c+ø¬àÛ£É&ÒcŠ–/Œ)ª?·´1-iiLËÆ´¼ÓÒ˜G\óÈX>Æò1–±|F´|$¬ƒì¥¼†×¹eZ€Û¶äx³º^õjÄã`aÖÅÈdÑ¸J ˜»9Ëãu™~bd¼GÓ+C)$Î¬Î•¡ÿ”Ù:F>'’¿¦“©Mâ¬ØÔ …•g)ve2«¡1ò#é¡n%¹­¤Sh*~#
U4Pm™xÆh¢®‘FË+Õ2à™ªS”*`—T3Â)“¥,íU ”Ú	hàQeJ.¸( u‹³z‰ÑG Ÿìu–î½”Ê—£æm¼Î3´^Hk³ô…Î==Wve¶±«ÃJseg®ìGf®ìòŸ¹²3Wvığ¾¬hĞÛP'$­p®i]c™ÛÄş…˜ÛDs›hno;²ÃPb
MTÕŞŞ!ÿxJtqø)‘)^_?Æ˜IõùPµ¥ÙĞÌ³—Ó…X’éjBkfBŠ®Q8C«e5o¢-˜öš»ÿ£&Âæ®³ÂW>^(8ÖP©d¹OÑ6>m[ß³"*ÏP½“¸®^É|©_ó‹yDÇ’?E÷$Í¤lAîñî‹’¼p×ëòs{ÉÉí=eZƒ¯W;ü"Ò5‰‹ïH¸Ò:&ñúÄå¡´G'§VA¸Æ›Šê\Ä§ÓIyŠš¬à54W‰’»äX{Ú6 ?*]r·ëäe¥<á8àáª¹ZÉş¦*öã’¸’õˆB|xY!ÅïÓ4‹,sÎ³ÕuL•ªL›&åúîtê‰V90óxo«a­}E¨i²;>Æ‘"èÅbaÛS´T_²¢âè-<ßö}ÁJF,à_–W¤¾nß²Çu§jà?¤şî°É¬5õS÷ËÈ{kµ)(Xò|gDŠÇMWîÌYÌQ$XØ©g»1Ígnˆ"AS…Ã#Ú`¡§×8a9Ù¨6	D»‹oñT…«ó‚y¶Å35ørñ –,Ê´®ñ¬V İñX”J$•õF.tœß0ÓÖéí®VÙÔ3ºŒfSÃ«¿™˜D²*•Òëm¨ö©ßkNvf(E”¢T'Yä²İ…!Š%
"puD©q*’DíÊ‰ã¾7Â•Z_Å´15Ætyc^5±‘ö–œ—˜(³E'I•¶”‘2FÊÈÉ5HÆÂÓI¥¥oˆÃÇm‡^Q‘;†QÃ«'C½4p6ôÏV¬‘Jè0ˆğ¬:Â:F›èUIµ²”’mTäêF[ÕÎöX¶——ĞÑ´:Êûã0N‹_³S ¿YÒ*áŠ›.cò“G>.lò@a'²`yîÚŸ†Àµ?"Rˆ4ª©fçBÄ4gœÌ‹±&J«Cµ¢Z¥¹S°·İ!ã²¯n{w¬…µdÔ%=Ê:“óB4ëõ~ãÇ»lköäO+pƒ6$çh”3œ(Ç"`Ğü÷¯iDû—ox÷×,üèîÏ@ 5 °”CFeÈÒA±ãsp¹WQ%és%Ë¿@Y¸‚bÅ/ª#txşéÁï°hFuá	r^qåšÔe§¿+jòã®˜ìjêÂÁØCŞ£•ş3‚_”Õ’£:ÈJ¬Œwš²à¥ºv[(]Fí“g£š3ç÷°D½Ğú9œ^x]K/$˜ÿpŸPŒI#[á‚ËøÍ(ÜBm…|Ûâ1ôïŸ“zğ7›·ñ!ôÓèWä’€IS˜Ô‡Íé‰«sS9æ€Ôq•°=àßıİÓiOkÛbA»]£©s‡|s:|Ö5=2–ÖÉı(Ò5½|4­$Iv;Lº&Y¨y'ŸRÿˆ’İ¥hf-ôm,0¸Ş s˜|G3ï"ÁŠrØŠ¼£gîúQ¸XÁ×•¬Oùú÷dí¯]ä…Àıæ-ìIÎnôïŠ7G=›™ınaWjş¦gĞt9-ÿFiä¢ú7i¹‚µµ;·ÑÿnæRpı“ù+)ÎoñÔ’§wñæˆ„ög6Àó>âñùvÉnÕÛè)MN{şç M° ò#IÇG|¬›S„VÛÓµƒEØVzùiø¿tŒ–åNZío1å´Ú‘­Ûİ1’‹ÆÇçïwxŒª´…Ã~çß´£m‚‘ ÙuÜCdtô¡ôşncÖ­öˆ¼)—»Í F‰wù¦ÜgñaÊÇVåjûyï^0Á´q"g;›$å€"Óìk ÒgìEÔ°oÑÚÇÃÊ+ÓÀ Š:?=Ò°yh˜@‘>z96“j}SrõLÉÕ1¥‡gÿ¸Át6lFå('¤éøZÃéŸâĞãl§×d}Û ÊUhM•æÑ`€
Úâb>sƒëËÄÜ”@œDEö‚½Eh*¼n{¡ĞNûÜuO\¨€qËb›—.7×SÆHÄçÌ4 õ²ŒªÔq­W©4Â·z¬¥mğK2]¡ìÅ„!™ÎŞ„¥¥{¼v«V¶ÙÌĞ»> †^I:Q:²MO®JîEM= UŸ‚k'•§óûƒrGÅ!lùÎë1j²BÀì¬š-¯Ù‰½FTQ¡$*´1{€!4‘[éw½ûÓŸîşLa¾ "}F î Z8şÔñ¯®« êÀ°äâgXòX,ù*ùeáÇÕå~™-föræ£k¿Ñ¤·@×Ú}ßqíÅœwÏ}1±PŞ´q››¾œãt±˜.ùUOà0ë‹;N‡Û!á6u˜¦K=ŞäÙÙÓw|¼>ì"Ø+?^?v-Æ¡g¬´l%c©4r~QúÆtR1:‰^Ö¤Áïdô›IÁö$úÌ&÷“èçL2&(³.kÒd†2}íIƒ'ÊtMZ¬Ñ8m¿4›½³Sƒ9ĞÚz=¼*1­ë`>·gÑàŒÇÓàŒ»¤¢ÑÄ $3šXo/£‰[ûâpF¿’×¯ø(*…*œ™G^ÈıÊèWF¿2ÑxŒÆc45ÛVGHÁìÕÿåcBŞ†¤Rêç;IêfîÒñÀªå#)?Ş`ĞG¬çìºR¥ÁS<KÒ44>l!“H@4¥úu&Wkø-?m,D’ŞĞ§}’b~ıK¤ÉoH$J/¹9V°¶¦KÀ­ZÛ)¥^·Qoòğ„â”¼×ã 8ç³Ğ™&?¨õAıE‰†}´‡O_JS.°BÔL¸B"Å+îÕ9ŠUŠ‘0ó¤ûMr|H¶[	oG^Öœ:Ã|(ŞgŒyag»ŞjRışmQêohRNãQ,šó:M¶õ~~Åië0äÂ Æk¼FÛ‚¬
B¬{@îÚãæ®Ûv[ xãV=X à³½ækà6~·wtL†² 7Ô&ç\‡=Tú´4¿/ÚèOí…¼üZzsûíâ­}mn«ŠªıV!K1[ŠºoF-ê1Õ«ml{PìkCj3•Qºb´_6‚Ş¡íú¡ıdöKÁ¾³"#ø³ú99ÆëÏÄ±&’Î¥ˆíyv‰ÁL@»ØàéìÂÇU>ây€rI@«lÿ.…$fwTé.éøD‡¬*ë/ëMåÏ °Dê»$=m?$û8 µD*aï§]|Ê}ÉÃ˜rf”?è²•†æ&UI1{+şÄ¡‚Á_Î‘ç\ıÃƒ˜Ã¡‡ï@àá<˜†í¨.Zîˆvd1O@àùT0uEœ_Z¬“u‰Ä‹ÊÒAI“1Ávß¼±¾{ûÀÁLz»Úd
YúŠ_¾Äox-˜Q­oôÈÄ¡FÔ°–³Àq,®l²Ğ:™¥nèŞ’¸¨Uû0…»\®Vh0ŸÉ`Çd¿ÚV$eáMÊò«q+eQI[¯WİÂ$I~LÂ¼¦Š€¹IvOPbËìßı0D‡\î®‡—¢"Í,Ï}|>mƒÕï	ˆ­°;›ÂK²9mÑ
˜!é¸Á+İ@³¾bí˜{EÕŞ¯1-ë64M)$±‚=µ<k½Fbië2¾õş99&ôlTRêa^íGSwî9ß&!>Ğİq8ğ™kÏ]´\Ê,Ó§¦¥¯çölÍçĞ¿ß…éçıEÃ¡/#ÏYÏ¢@ú#ÆùßPúˆ5ªÁ/=iû`ÿN¢¡H²é‚*Ö‘ïÈ’€;|Ã k!Èödö¸ª
¥ Z d‹‰ŒšæM§ñEÁ7P°®‘µ@ƒX8\×6%ödº5Ea³§@åÔJDŞ¤;=ßšÎh¼¼	µ¹m™«Õ®
ûUû¨‚ítÒŠğ¥Ä4oşÔˆ´b|©¦µ1¾*?µø7Óï÷dŞã›¸Ò—ÿx=‚×şcZÏåîˆRô¥‰«'4 oªRPÀ “oT¹‰*¤ªûZI«´An]˜\õ4òuaì`Ì1­xQZ‡×a•ıñ@Ü$LÓÌ˜ÉRş3!Ëê©”±§V¯"½¦[q,Ö›3À½H/\eS^+ÊÆV±m¾i¤gÚÀ1Ş¢Õ® sÀİ`×ãK¡dæ×^-~?á=ı>Nşy,Ù“o™MÒJ-ù®Ê€l³e!Wd¹qbê‰+T€H ôò,DD]"¥®.ØÒ9C‘AXĞ³©@†óÎÿÛËVGSK¯éÈL«º¢‘aåÍ-å÷9çñP\µâ5VÛÜK]åŞö™oè0¯„¸ÂÛòkEöÒ[ol¡›²…2ûÿOÇƒF{j[v%€Ï¼\¹W¤¥çÕ¥
ITÏıİéÀ-cÈ™»”³¼sl@¤ß7ÒdŞ5ÒıDß36a]ç;Æ|‡¾Íq@§z\"JG.‚Q7‘F#?AAİ›U Û¥ø•g´Ù¯> t{øe÷‡èûˆ¼+ÁcL‚äCÕãa{(\/‚7àÙ„À„ÔnÂÑl6]®İõ@àßo÷p€;gâÜõbœ»Â
Sù¡ ÒŠ£oËÆC|æ<Í»ì¯«d½:äÿ0²º$ù œüØæXËk	Rn·m?0‡6FWLs7½‚Ò%CéAékBÍ[À‘ÓŞbEİBâ)9~mãİ¤eû¡¨L¤AËÀµ¡:¯µ*'RÁ§ÀßßùÙŒ`Õ›;Ú ¥½g;Î¢Æ÷ÌVì»›Ê¨úİ#‰*ÿ}ó¹Ns B²µƒ¡ÂÙö^“eÀUöó<“iğœvÇ4F‡é¢Ô‚6A³óèÆ@œ6+`9Ck=³%ñÉÏÉñ§$Š×1ó¥_·%à¹$|m)jŞÀÚc«ÇWûyûÙÔ“šç¯pƒ_ôµ?‚jo‰Ş}(ñ­G— ¦ñùş®šAÀæ/\F•9ô¤ºÅ½aqº˜¯T•å
ˆjzu_qO3#IˆÌPryÏ¢ó÷Lë8=W;Ë}ñ„u7 ÂüŞûòª`ÿœìÚ½ÉúŞ§	¹®érx“fxƒN‡lŸNpØü.«úb·>•Tp@›5÷c‘U—û}{:üÍª¯Y„6èˆÚGTE•&ü¾ø[¦İ­1WHÒÏw	iÚŞ‡Æ`şn…¢¸bp:Ä;|¬²Âœ~¼[ùû=·á&"t¸°üÃXvíÖÜ:'ï6x¶«'ÔÆê˜O{’Ëÿ=ô·Üx}›Ï+<‹Œ¢e»µÿ[ÍËûmãH¸IhµÆ<È’WM£ f:«ÓÎñã ±BlÛç8Šä]¥è÷6ÒW¼yZÊ7{ÔqvL£‚à©CÙoü='›®Ğî	ÓPÄ_
ºá1~á7@Ÿöø¯ñ1#E´kŸs-X-ğOÿWä’İ}…‚øŸH¾èCl×/iôxŸ±‘¹;u¶#VQğ£||«[¡9ä6F|¿Mş'.ø""^q¸cCû(Yù}ÅÔÅ4Ü5E!ŞØGÂƒ*ÔÚúŸ '­$>ü!+Ö“Õºèø¾Ïùÿ
nW¢\~øÙä9ooI«œÓä·ò+’ÔnzÀôµÚûqT{ğÎÓÇ¶&ÏæW‡V7#vëïğĞ«#yißÏ‹Ö$EzgÛ§ÆTÂâ{Zù)ç1&ÉßP´ª’M€O²–$ƒÕOùåmˆşr÷Í7á	Ÿı”³u‡FS"i7gÏF -)X¶Äq3ò)¹HŸ®|Qä{‘ĞòT¯:‹BY²ƒb-—êPpgQ(ó!Pæ‚P¼!P<a(Î( ”Å€ÓÇû ¹«WX±U Ñı%`Õ)»ïRÁ½Ø†Nf–-D±l>„.ç¢té¸ 8­ŒÊ<(C(Æ¥˜™5 ÊÌ„2²–iïZ±İ­J(e_Q³!@f}PŞæÖ½2‡¡ûÑı 4hĞïWlb•Sõ ¨î}Ş%)ŠŸvª¨î”Î‹î/±J°èşx8Å8Äñ  —!Ã”¤ê|©ü§¬VRùˆ3{Cşâ"ôƒÙYGï­ªİÖR©¾+_¶T¯Ò"ÕI1Eæ—TF³şrÁÁ¹ï¨ ’{•^oÌ±ÁÿíN¤[…ım+ÔëoZâ›âcÎ¨f	”Î‘@W¬Ã¦èB-&Ï´Öö:ğfªãKaZ†ÿş#Ë^Wiû*j=Ñ,Àñ_èŞ•á)ûšNäpè+"Ñ¹®eE‹¥M]e€÷àÍy0èÖt+¯®8³bVs–àZ IKı»LØ,ÕM4F¶ix@ìß~yAiGè²˜İ“®‰ H/Ï†ğç[‚ósìx•/¸‘ºx•tJ²s‚éÃäö*ßÕäÔæuH‘¡äõÆ{xğf×–g…4qÅj×rË”p^s‹iŞwâõ²™^Eô¯ùŒi$Ü‘vÓFl¯ßèW„Jñº,˜.e¯½Ûâ°‡"ª‰×Ãc77ŞqšÚì±5cx½ØÓ+B xÍÙs Q¼¶ìA0R¼nì9x)^[ö ˜ ^7öŠh&^sö€È^×%$el©ä‹T4Å®ø#C±T”¯K€Í@0^¯Öù·CÍx]Ytà„”q×áwÈíÄî‘Èí0*¾‡ÛBHN¼w;øn_Uñ@Ü^,–VñA\ä™ÒĞÈO.ˆ\Æ"E'†X’$X·PtÒˆ´Aj1ˆ3¢’G[ ”w+‰A›ó î’›ß‚£ëæÎŸšŸÃ«?/fU`0Ê¶ã}I+PWø…‰à4—ûÃÆ.Á+xÌ¢ˆ†ƒYÀ\˜ÆP©Ğâ3b­İ)(èpåî7IeKEl9b3¢;[˜?‘_3FºíÉÑ¸³íUüqúYé‡ç4ù˜énšhÉìékôë<-˜Ô³pn’Ä¨ô¾ ÁÔİ8¿ğ-˜°ï´İ/ZQôõ²
<L5¼Èüf`Ãº!¯|g>YL­×å/´A·ğåÿÿ×»ÿóº¶H—#ÕšN2×“LkRšŠ2½ğ6Q2g“Ò%ÓË!:)8óIË;%Ó}1¡T2=İIå«’éæM(—•DGû6n]„wõ°&ŒK¦3>¿Â%ÓŸqgÉtÁÈxµdzãó#Î-™.î„õqÉôÆçWxYdz-'ÇKé2¤vÑ€*?ix»¤úâÿÓ^/©¾]Ş/©ŞÎò‚É-|:á¹ÃäÆ)v°t‹ÉuMZî1¹
~T»Éäº»“.w™ÜXŞr›É±œ4İgrø8Ôn4ÖYÆ!–¶K¶ó²ü¥şîŒòâ=“	Râ2“pâ'Îâ«ìÉîÚÒ‰c´i‰õ]étOqS}´ıV:İM nÀ(>&º
·ÓX%M(Óí&êLè•ùÔ qÖ†‹Góa,%Íßß$¿ÒÁA‚*'ôdáÖ¿d]5¯F~ÒœñÈt¯Šuß(¬®1„æUr<:0#£}<Zİ8m] tìèõİ´`ò¼9Z6m°€gp¨Põ bW©1i¹®3óÆ~mS™}&lÈ„Qó7aC&lÈ„1LØ	ês|˜°!6dÂ†LØ	ª~&l¨ş™°!©Ó2aC&lHü9Â†LäH¹c"wLä‰Üéîk"wLä‰Ü1‘;&rÇDî˜È9 &rÇDî˜Èş&rGæh‘;²q4q4ö4°›WuĞ„Ñ”?FcÂhL	£ya4M1§3¼äÿ  ÿÿì]Y“Û6ş+Ú<e«R)¤$z7NÇvö°—gœ<lmMQ$¤áDÊåÉlöÇ/x‰Há¢GšùøØÑİ îş`íAy	ÊK:¹¡¼å%(/AyÉ(/9~Y0»é÷ihô¿2µåj#~¯ks(P£‚Ô¨&5*¨QA
jTP£‚•§Z£‚ÒQ>(ÍcÒ”fp3EiF¿¥u­ç,Òàdğã`8xÁÎ0 nu¨ÛĞ\·1²,uÆÂ˜:“Aá
7P¸Â"4DáÆYn  ƒŸ%
0P€1@
08ø£ Í¨ºÙ(Àx(ài‚ò”? üåİmQşğÊP¿ Êõb,P¿€ún¦¨_@ıãAıBKïP¿p¬~A´ÀÔPH0ŸLFÆÒ±QH€B  ˆÒPH€B‚\ŸPH€B‚.n($@!
	PH€B àk Éçi$H>| ùİmäÉO ù]l€äÉ’/ÈH>| ùİ½Ôä¢ês¨º±$õ,†@ÕªUª^DL@ÕU8ÎÏà8ÀñÀq€ãüoFŒÍ¦ Ç8^‰íçip< Ÿ>8l[”°m1À¶ms3¶l›ñ ÛnéİÓÆ¶Øöxd:†1™Û¶lØv-Û¶<À¶mÛ>"°í#bÛÎ`ÛÅl»ø°í“À¶0ó4Â„3æî¶@˜0aîä„Yc´„3f Ìçƒ0B¾Ó©È—,¦ÃñrlòäÈo¾<{Èè+?K ¯@_@_¾rğú
ôµM5›>}ôuÖ‰¾fã¶íŠAÓHó0PŒ1JßÈiê²xãÎmàëe¶s·
ƒ½ïŞ¤‹YÌiÒ¤Hï±@zôŠ´=q¤ «(€¬b, ²dåf
õi‚¬ìàå½41Xs·i˜´’ÅıÊŸäĞH{±0—£1.W	4h$ĞÈâÉÏh$ĞÈĞH ‘üFäBÓ á¼ÑH \<M€råÊ”«»-P. \@¹ØA!P..n@¹€r	r{”K2-Uh¾´fÎtŒkH
tö(P}r åğ³”(g (P@9§å oài¼xğ†SÇŠ(ä”ó’À‡’ ,å°@ŒÀ€ÜLpƒ‚Éyc¦šœ7]Ë²]BœGrÉy$ç‘Ìæg‰d6’Ù$³‘Ìæàÿ|“Ù}$‘Şåh…ô.Ò»Ï6½+•(E”ƒ2 È€"*Æô¬3 ¢9IC5'9Y.§ÄYàÚä$‘“|*9I$ùY"±ˆÄâ ‰E$9ø÷”XDÖY»c-‘µCÖ®Ü
97A>È¹‰±@Î97n¦:sn‚)°ÉHÃ§\­ÉÜ0çÎY0dÁë=&v¯òSÈO!?…üòSÇø#?ÅÕù)ä§S~J*WDßú–†J ctŒ2FÈ!c$Æô3FCCCÆhLF“¥áL1BÆè¹gŒÎág‰tÒ9¤sº6mªtÒ1HÇ ÓAvÙdGÎ6;Â˜¤o¾A¤+rE7è*r=’ÿûD"2Í‰Iœó!b¡¦d$'‰8_#Hé9Ğ úàDŞ—Z¿_Ñidöú±]Wî]\d¿Øó8w¡]AÌ²—%H¦)ŸÁÊ,•æŠÃLT¡œéL‡}µæ„‡T:æÙY˜^·÷q¯yÆà±«Úw:Mİ™ÂİË‰g9ñ*ó›Í6zh-JO~•Á‡Ã±9\œZŸ…ÒğÎÙGòyO¨r0‚¡ÒOc™y“¹Cz¿Wã„Í‘$r§æTıÅ¬ğ±èy¸5oƒx+ŸeUJ?ÉXs<2œ¹åÂªˆ[Øˆça#Ngñ+Ç43ÓYÎl¤E`"`"úÉœ¸1©!ª	Ëqƒ,aMÆÀşœ‰ı9€§”Ç­<•¯¸ÑZŒ-Û9ÏV~ÎkJìPÇÔÔp¦Ã]kfõkÅ™œéĞs¦/øYâà…fn8xƒxğâ±}#œ‚ànSÒ§ š‰Ã	|p8AŒ'<ÛÃ	Üq6#¹#WcXÃÙ16bìfŒM~ßRUğ¢d°ˆßÚ% F \}—Ÿ§#BD„x¬¥|„Xßª¸!pCàv’›èyÆq=ÎQ:=Î†Ù÷§NÁœmÄijíå-¯äqÈºç®í%gRƒõ‘8ÔåxO>…ë’­úeñB_ù£¡ëÀ_çÊsÿRÿ1Á>\7~º¤Mşî¡"g­“?7^KÈ'9"&yÏ¹‹MhtPCMbıŒšÆ0ëg¹cÕ…Õekv—Ã\–ù„d·_Ç©¿^×fíŞ‹œÛAe0ö&ÇŞ‘¤XÉµg#wü¢ñBülÈ}¼±„…ñ8[S˜üYĞ.Ş5ÊY:CÛœ¹0ËbZäS2rGW˜ñ'ÿÎî}9®daXÃùHœk¡É’ãl¡EÄ§6Ş(XV]¤DûĞ¯-„’ê~ŸXƒï2şßÖCe-°wô¿]Qmôv¦¼tÄİ¡|ôìQk&ä<dÆ‘7ÁwJ;o¾ó…ë£u¦Y/å¡°ºÔ!³_'¦t‹¢ï7¶®§X+İ®>$ÒU#Ü4ÿšÔ'ß…NH}N~n%læê™³|?¡9‹Ÿ’ƒKÛ½~Vk¾4}¬ı2Ö´odşØ‰)Q=q7"ë¥ "lí‡ñİÍÂöï.í0í#zÕ…	Äœ1>Å…¶‘­›QëEÚï¼]ôW*\.Ó/[âS|9ènè`ì2¯¾xûeìÎ·ÙÚÍ>cÙtdZY‚“¶7¾/èŸ~¨ŒïIP<x‘G6Lõø†¶´s—az]ú‰Ò-ş²Êt|„®©m\_¡;A[‘QíµÖÕFGĞllÈng¯ÈîæÖ[İ^9ÕFÃ²'‹¥kYÍ¥\±¦/»”7Ó´’ˆ·¼—É¶ lHXÿ*f„Å¸lD¿‹˜Fs^Âä|šæã -İ–àĞî/QæºÔA7Vº³°)­Æ"=Ş·“!‘Ê£S7ß÷ÉšN8ıÑÛÚtszë­£’›Ú‘UÏ¼‹Ï­‹¸zŸë¸«¶D7Ó!¾l¨Óè=u¨è…Ÿ?G’±‹©MæsÎ,0ƒıeZ	#ËŞ˜çÖÔ’fán<Ù¾ÓmË4¬¹)ÍüŸqNË•c>5§¶cÈOû±CçVrÔ‡îh>5å™¿
d'|dŒg¶IFò¬ã‘s—L–g&¯mi ­ƒhµ1½"†ä`¯Û­ŸD¦*3sM¿Ô¬4\¿ø—(ØŞlv«íÕÉ®r+#ûÜC±,»¬±ó,õl¾Ëªö“/âË†şsOµ{©ÈÅ$(URuì·½¬ÒÜèy­ñ/†SRF†âHÎaê´ô3ƒ™CÕ2ƒ_eà$G%õ¦z•ÜÓƒ^÷ª×©OÚËfî2&°×	Œıú~ÌRqœ¡YJ^F%† Õ½juµõ2ƒyDy¢z]KÑ½Os¾Égî92ÿ˜_ö£íº4@mo¯‰>l#âÖ˜úÙ?§o£še§;Ğ€w¿êDj-“ôSì·—öz=pìu³F7¯íÕƒU›ÔÀ§•Í­m™uÁU½õ"û¸3É¿µóIË
˜Ô’ 9jö­=ÌN%§ãÑÒeŸÄ#öÚÛ9vè~$öNc˜üÉÄİ‡6sjs;gÆ³|ãù^Äñ¸ÖRëøªSì^Ù›`ï7Y$¿EAd¯oZŞÈ¨Óq<‡ÜìÖû›ÊÊŞ¶iH2ï­ıûâ¹¤9½q»ñnÙé×~hşE¼°‚ËÎIß¹`wú7:Íì-5„÷ôOÛøOM} ôç-aü²xˆÈ¿ş=ØÚëÀnıbç„^²ëÜP	¼õkÇEğÅË¡÷…şKn¿ª¶c›ËÂ~[3Èû)‡/ ±”×Õ~µ"»è•F·tÌË˜\[ò0IíUq9&-Vö)Ã»`åùYß»ÀI•’ ,‚Ò¼©šlIjT¸Gár¿‹‚ë&!~îÔ _Rc‘Ã×ˆÔ„hÒ“E“ÜœÓê½ëà°}*	Ğ &4®oÊCÑâ^À«…}AJH‚÷ŞŠî®tğô©c“¤œDúáæÿ“÷…Ø÷Ôé³÷¾s«$@•”¤ÙÎ§E9Ğ–éc‚í´È’Ñ’•AïÀÔ‰rKõ!X¯/¶[õ\ø»{ÅU\'&$Eºü5IQ&&²vã0'[to©k¥º€käøuEË®"¾›ÄgQ¨Ù¹¾%›l7&j‹"¿<NHˆ¿£Qßµ}GÔœ-~İŒÃ½Ÿ÷›Åá{jÚÙ '´·ü#ğ|â¾zĞ!
ƒ·,K¯Q¹¤q›Új-‰7®¥'ğUÇà@Ij&Şyş¶iˆ‰)Ø+m.P]¿TŸ_V'(é©êˆA“_¦$àp_…íÒ¢8M§&ƒ ¨å?ä;²şLÕÜ“VªÒri‡[Šx†Ó%ªÄ¿ #à5.£ËÀU‹os"Â\ó[ÕI­Q“”c4·4ÊA©ÉÊ1é”c:ã–ã:ØzN¼]eMµi’—D B+ñÓ6N‘ºê›˜Ôî¬ÕŒB–”¯®ÍEçŸÿëw±›°Rc} "Òë,¾£Uû]"%)…ØBS@&g’_í5õá¯ˆ¯¶(ëÄ¸¥ø,~õÈ}Œß^%‡o•ÄhP“•C§Ò‚>Ÿ“I•_®´VòÊ[ùŸ¶j²”)‰Æ,z’ö%şhŞó³•¦ÅÈğû$ø¿{/¢Ñ¶íÜ*:“züö"Åß*÷ØiÀóÊô¸e¹ˆ¨İ¾'şşU]¬×Á½âÀ°ŠX÷+{C´Æ‚q«LG‰–<Ò¥Ç”µ“ËÌ)gITÄ|_¬DG8óu iÉ|%”¸ùK*›/S-?Ì '!‹&ø5/½j@Ù»(Ñ‘á™¥6,‚
ÒCÍÒCniŞl¶‘ZeJBO§–=9_­¶“çTÄñ¡«xÛß¯U]‰95ì¢‹ív­6ìÍSŒ4*”äøk_‰%’BYª!Ùx{5à²DG†·¾¤a ¼4:R‡u‚ÜÒdØqãJÁ<é7W’‡MRN"}‚ˆ®d¹÷]EãU£%QÉ©é¹Ú'_0ĞUÊI“öEŸ0)=¡ÕsÙ¡jLFEˆïµbIcFƒ_Cï¿D½³ş§oÇÒªMsFD˜knSó–ujÒr¨!*jÒr¨µj’rŒ,ãA©IËaj•Ã”–c¬U±°Ÿ|ntzg§FSI¦ñPÏÕhªÉ4R[h*Êdô “¡&ÓXÕ©Ñ””I£(Š£¢æ"·ĞğQ½ü ŞnïÎaQ¨Æxmdå%Ó+”š\ù&AdkIâs^ê)á*)¼°\›íšÄSs(k´„dHK°¯íİº%Zr±%_lµ	aR¶.ö¡sK_øe¹TuXÕäyMœµrY'e!ŒvE`"P>ÖSPâæÿ3¹Ïš| ºçù+%ÔøåŞá½º»ë`µR„[êÄ$¤ĞQóİ Æ-Ç{Û§v_ÙQ95MmPÕÎ`³Ùûb¶¿FK¢ºãWÏÖ&
ƒ$K"—,íı:êæĞ¸‚³ë63æÑâ>/eŞúWaŸßôW?à\>¹¬ùş¿Êt°®‡¨Ô-ÃÙ~E†™<oû¾{IüñìæŠs¾…uk8ãµ¬§**R?í¦M=,×9î”ñÕ¢„eÊÌNş×³¥L¨Pã§òÁ’´kº>CÒPŒòDmŠ1±¬éÌx}	Å8OÅ`!Dú¬†éºÎ2¾æ°Ö’J±K ŸH/:‰oqHÏJ’ğH/õoğT´%écÿÊ’fC´éÊÔZZ£érŞĞ•|N·qN*¹ŒÈ‹óS»h¸&mÊ¾øötôäĞE5åü®Ş¡ÿ…}ÿ¯øUEÍµi–1ZLÈxÈørÖõá=4Ô	WÉ7ÃUß6H\µHº¯};Züä—r	¹ãåû¸x?ú?b7Ag×SÕ¾
ş@½—øö8Î¹ÓÉ¬|*æ§bè¯Á\{d¾Ù)B®%İ¼éÌ—5CiòË¤‹9ÏT‚cÆ{Ùú¥#úÌ’éš‹á˜q­hr\â°'¶Åª½w•İ	R×ÑN$<Á»ù¥¸éIF²?[U½ĞO¨/Eóá^)}| ¢’ß<8ë§bÑ˜¶¬ùZ¢_Ú,^.dE«zÓ”£¬I•…•ĞªèO‡†¤ÿå6²lF›¡%£ùÜN·Z'†Ö'÷7NÊ²Yev_MM¥¬öCecm`aÚæ}1œwÈúœ æıñç=«ŠÕ6ÛöÜ8SÖç-á•ØÈ;+²ŠØV×DkÂãF¹Ê~§¡vY±n,tÖ³¨]²OY³oã*éŠZ{	ÓË×GkÓ‡¥M¦ÖÒœö¬YGÎéN»ğ   ÿÿ :c6xœì=ksÛ8’ß÷WhçSfËåáK9sñ^ì<6[ÉÄ{2¶¶T 	ÚÜP¢–¤ìènç¿À—ø ) eÒ¡§ÊÉ„ ºÑhôüñ—?Í²Ÿ?ş”şNşØl-ß³gQbô‡íƒ(šİ~X®`;øÊ½`}uâ«‚Îà·®hö±ü}öÅØÕá\oü™·gv°âpkÇA8{9“¾Y¶°-àüò§z×‡Àsf˜sB°Š^¼_o¶ñ=à{ÿ× hxÔ`u6³‚À‡`°²ácòc	ü{±À´Ã9ö&½õİ‹}—_*=¶#ÔãÄ¸;0ÊAP·2©ûä¹Ü7IïŸ¶qÛœêÈ½¾_Çªò¢DÉêå¶Ù,“ù×še3#Ï+™¾^Yú`CY•¦éà+vº®–	ÎAà°we>Z¢u|=/nÊf¹Ÿ#Ş«ÁÇYï×•Ûà]l7WÀ÷…	£¹¤¸¦â.zf%ãür–ŒQLâüöC‰7
j•Ø-]¨ı¨dvirŞ¨d¦ÍyZ6ğ‰ØzıŞ]ˆÓÛ0X	ã3TS3 98‘e#A¼D¼¬ñ®»<5³PŠ¦Úh2Ù<añÒg$gájJÏ\áúà.j¬pE²4·d¯âÊsg/îAô!ö"ÁîlööÃ«wKéÇ:îøÇÙ¢ıˆ)Ì0…?É²iÛdÆGHA6ê5áçôì Y‰p4[ãÆØ¢»½‡«ÌøSdµÕø+·æ1ÿÀÂĞUmŞ·,½÷¢ó8Áñå¬À÷<pß¬„`°~Ñ%e$31»¤óğP°¶aàâ5±ó9âšlr/^T;%cşx3rÔ¹iıÒüÃ"X1ZŞÁøMÂsĞù”8ğ“õ/d;pp›¡¹†dÈZ“Ûîº†0œmĞ¯_êŸ_…!Ø}ğ¢ø¿ĞÁ;^Ì=»†¥o/~l\ [¸Ÿa´A¨ÁN¦®M€‚ËCoÃõì·ƒL¨EÆW†¢Y€€×†,{*¼BÂz™5ğ9•?¨™·ë8Á„ªlË¶¦¹ÌLˆ[EwÈò—e¤¿ığššÉÎ0•Ã*I2ğçŸ+¬:R^l›.Ö±a˜×Ğ‡1üa!ÌVšêª’m÷¬u±7R$Å2›À1+X±„…]MÙ²•4ÛÃJ4zïfUİ¢äW<5+$£Ùø\;ú•ãİÎòÜ†®#I'ØÎ#Kõ,ÏM·]¼¼hÎç”-Ç’¹ï8gA#	ğ_ı©W¹Ùö¢ß¥™³ÊC&~î®ÕÄm]Óµ€¥Kät¤ãâ÷7_ßq .€æÚ–¬£g¶Ş$H¿œ%È?}•Lª}uc‡®#4ş-ø
×ÂV[[˜
úÏããßôÙŞ!1¾ôÂøŞ;aQlÃU€2ß£×ôñ–À¶ƒí:Fl»´rò¿”(ùßŸ`zìu'¼lA­‡İÕ=³“í·b†}l9$™Öğ×íÊ‚ágøï-b6q–¢€¤¶E[‡©muV+ßï_ƒ@ÓÙ–ÆÂÑûN`yDˆoÄ_ÎŠI<åSL®—İpl<[×ÜÔ4C!¸K>rf8ş½„«à_^i«5É—Úöƒ¶¾÷®‡vÈvØÄ~ ı4™­BvÚ=P…4]Å*´KDö#©D`)Op{ê-ŞlK:÷ä?Ë¼i?ş?†§š½
—	d¤2¤dMSÎÈ=);‰;÷-mÁĞÌÖÆ^&sÄ.ehªÜwìy’ô@…ËÆI\ 16qÅ"ú{à­¡s¹íÄ@Ë²j"tÀD}ĞW)dI]¸fßIg¡àÑœå‰	—ùúƒ·ş*l%UYQ4[íy!½$ùzÜ‡²û9ˆÍ¦:¸©çsØ?Ò|ìıhVXöğ{:ôqÇ™Ğ±9Ä¥£\ºC¿üÜ—#Zn`¡ª}_mx&w^Nw™¥šŸCÇ¥|é¡ hZÒSeîŒ@”7¹€üôb›sÀ±AãDOq7–ÒŸ¶m=LK¶që0Û7ï|që–éÊrßO|»k´2–ı¦×)äñïĞúâÁG<Ç¸?ŠK¹ÓcHÂ¥¯Ôø˜ÅRã8 ûæ |ú¶÷x&sãDİá©¹dì¡	n…g)¡Äep"_Ï°-ÂIl…†Ã2Ãç ŞÔÜâ<6Ócº~è²ÓşšcámM¦Ò·%?8c÷Óu	¨.!×/_Îà”í–n.­ ¾ÚÁ	Fµ'GÒ«Mó)´4—ÛÖù92qAg7év–P•¡‡|V—ë,¤ß-…İ4j*ÚujËy¦ë„éŞÎsÄæÎfÔè;%|ñ®³Úäïaàz>|VÙ´åiös	$XÇÀo¼»õoaLàª®2wú¨¶D9ˆøNOñÓ9Tm×ÖÚ§°™è°Ùíí|Zr'ÎÏD\ßGqìwùòGZ¾Ù*EhıÄmÚ²ª9ÂæÃ­‘ÍáÍDÊ$lã`‰Ì¹{K7VC³}SäûK3j€/ùgxœJ²A›­RòŠ¬˜‚ÈÕ ©ĞÒ)¥t8®¶áØ· 2p-·ïôu66·5øèè¼º£¾J¦‡Ag˜¶;‡}ßˆëH
Í
§†‹ĞÔÁ¥RÍ¹£ »÷T-¶¼!-ä9‰ƒCÛÛ~şFBcö9·uyNˆßíK •€ßî6ğb£ß]¥ ±|Ám^Ö/üò®µ*,ÃtÍëù‹É$ó=êÖ·ÎDÜ–×,G5`›v¦Cş×ağÍ[yñî3ö½À i@ÉÑMÂáGRõc¤;WÉGd´>a(—VBëÛ9H±ÃWÇ¡¸0t²Wƒ~ÆÍ	3µQ˜6­ !u¦œ:bò¿ÄÆsÍ9T]j„Xr$÷–…ÖT5$(Ë`ÑF†'sû<-‡X
!Q†ñÖ›‡‹Æ‹’zªı_{‘BçsòA¤@9ærË¸¿v½)g)Cˆzà‹
)Ãà	ZÙ(ğÎR/i“ë†‚İ1Ç4MeN8+Á“kWvàÍ"Œ¤+âÃNì™ÿûéöÉp…ñ©e û†nqiÉƒíWApÀ“î!w¨Û„ß„%ÆÍlÃù°Şç‰
“\ƒİ
®c¡[W4KÖ{÷ÄXEbˆ#˜lKoíÅ”†Û¾Ó6½M¾«_³öP¸¶wLò7bà/Á
×OêÏNôÖhl¸Œüí]7×5²4ÚZ‘zÉ°K4=Ï_âô—giV¹Œ¡¯zVc6QŠ'çÆƒ6g™Eß’/óHÎj¥…×Äi¼’ÈÌÎÌö‡Êò•#  Ëºê’ìÓI’>I:‰²g+ÊˆMêI¬h’nš¶Ów¥è“íİ§ŞF=0*3û\ñ+ßÓØsYwtÒe·|éAû!²Ş6B$Å…ÎÓâ/F7ÙÍ¸`¼uã+Óf3›ÍĞ`iâ”·Êd Ô}ŒOj%4ìÁ¤'×B‹‚`¥+ÔOpù©wõ‹%`ÓŒeNàÒ‹G+°”UÄ2{”ù—)È¹*LÖšÀu	1‹šˆêU1îá§Ş!4¬Å¼ÊoV›X\=mKĞ]Xãº ô¬à„sP°PŒÖK!¢ø÷!ÎØ’y$5¡i¾e3>(Ç“iÅ_‡påmÅ=a‹Ÿ¯•U‰Œˆ ã{Õ¿{ñı›uìÅŒf:C3Å¸¼öØ€vLh­‚u|{¤9©A|" Î‹d¤Ÿfªt.¹Giv;ÜmâàŠgşûî¯˜)qÌ9qÆC³—${fdÿ±b”£	óhK²Ê‚&kb[f;ñœu3´$‰ó‡qÛkE”Å6»€Öm]YhnßÇ C•¹…¥F,“ÒYÇ¿ !;	ØIÀÖ[õ$`»5ı$z ½²Ñ~­î8Ñkªƒ|ÇÁ¨ŸDïiDï$Õ•Tã6mûâbQ¶-) ^™Â­-0êYÜ¤@FnAmSü%¼	¬éº@’lÒSÚ¸u²šd»şãŸ³ØùhŞiBˆoR\³— Z)7@ºAÛ9Ztïm6øÌ=HÓ_W¥öÃ^İƒYŠvòÇĞÔÜ3Í0É»Úå1Br)TXbŠ¤ ‰†r¤É«Gœ]¦L\™tÊŞ"g;å<~_‰BåÆû=šmá£wè©¯åd„in;ŠE‹ë˜r7?B¦ìMd,°›†«Ê–Ú{™×I»f´´kş3.í:©·I½uÂyzõÆ«ÌŸ¸ÁÅ3¶¾ÀÄLKÒB×	u^që(ƒ—¨Î±¿“Q™ÌXâ,¯bŒ ò”é8Xh`Ç+~†› Dz)©°·m>Yÿ‚6Îoa¸J·êv1IC)¸¦(„ÑÖÇj~½õım½Ø¾ŸU(MŠƒâ»Ü–£¹:?7àŸ=øˆ÷ª‡gTFäŞ#¡êÃ°_"øÇBSşJ7§Ø2Ô€bÉœ|ŠïaÈÙ;£Ã'ä·à+älZ†éš*à|lv¡wwÏIğ¹áX6°Nè7´+¹ ËPQì…ÁËk_<´CÖ6'Íãj„*/Íï=ßye!s„¼ç¦c*
'øë \w!ØÜïxà‡„!úü×0—ä/’ÕE!úŞ÷á5ÜŞ‰À²<ŠU(„ñ6¬Ëû2”óD©œeèœr¿"óÉj8ıİe2u‹áByU4»Á”«†¡g“U‚(*äêitTÀêIrM9:"ªR%rÕ=:J`µ-Š¹1:"ä&„(BäÍè±7gD‘"·¯FGŠ’i%Š¹±7:Z”m#aŒa@K±Úb*Ã¥EÍš¦@ º:x‘‘…å¢%í{ï:7èóWüş*!6ÂJÍµ-`Ú’¶Ş®›·K…¢SLğcsÁ¡sãÈ”•¢3Ü:gØg'%J'øÂ…éjUö„f~Y!ñ_“°ĞìçÜj¤îÿİšn,¸İœ~C¬ÄS°®—øRê*ù]çºQı&µmj´–ÇdáŠ¦¾†I†!tê…ÁÛ¥FR2k‡\Éoµ8öÿaì/ÚÕ>×ù Öè÷Á¶ÙYHşh´ÎRÑ2Œñÿ5ÓÕÚÙöb*–Ü7Ø]Ì`~u(ÎÈ“ğ ~¿p¹&=‚™Ònüİ2–íNÚİáóè¾/mzÈ†&<°›1Bcåû”¦`ÂãŠ¤9ª± Œ’:5<²,‰_°p¡Êº&ëlDÜ;L
4CU”1ÚĞéìÎ(±kÈš+»Ç´)Û¶¦/4>Ğ|0Uİ’LÙÇLW›‹dÖwU¯ÁÌ¶­SÎ/ª·áñÉ²ıÜóÙn!^YRpÎÃ?ûëjô¹)‰ÂC=ˆêğ¹d¤439Šå8hUßÿ«·É¨›QD[Qé-"µØÇ¾rõ2Ä­ÚZFMm±ïSS‚ôÌ]™Öì¹9r€2“bˆìV6ŞY3 'V¥Ê+“ø¤¼MÁÔ<!ÿÌ<}nÜ<§çÿaX"M‹ÜƒZî~³hI¿Ÿ“·Ñàlø¶<_³¥À@_›söŸÿÌ.·ï|atşúÍåoï–×ŸßyuûfùåÍç›÷Ÿ~åÊW^çQŠ‡uDg+A4ªÈ,=^29‘ºtë¡‚¨Úò:Q=×C‘òBìCƒã,Ğuç`~–‰ˆ£ïœĞïäb/ù¤õ0!ßÉ"7!Xå})pç¹±En·&¼ı<ên_[Q°¡œ6‡f
LÚ½‡ût“ê>ê~j=:éÄ.@“N¤Õ‰tYYÉ«!BrLçÁ3í(6“)Ÿ˜ô
Cş“4¯LÛOÍ7ø…YŠvp<Ÿ¢]qg8Nˆ¶E×‡ß<ËofTqD‹·	ü,kó	Ï*–4-‹«¼YîB~+·ñ}Ÿù€Áô¡s"n¸˜mğtIVÃ
|[ÆŞ&»ÒÛéj}1‹¶ww	)K˜ò1b®¢å6lf«$wMË6H$„Øø>O-ß}æQ”ÀË È6í5:ç6Ç2ç@3*
YpÛCcLÙâ©³À¤8¯ßj>³•·¿ø"ÕšVpK§úË25qLYÍ£&œ)Ëy´‹ê–´Fä"Ü”ñË†oé§gw¸ß¢‘5š‰ø–öfı¦7Oy“Bäwx5=AíK´=Ùh¬(ñá72Ô6Ô$Öa{ûò8÷‰\ª¢Po=øNm6mSg>¿¢`ÒY"÷zÈg5ñÇĞW9«KA†ÎêY]2tÖÎ:d"[Ô6½ô³¦„dè¾8k
J†îf©ºÍÑõMga›V§3•¥G	Á¦çW‹%_}ùF‹"eÜ…øëËi'ÔuKE_šXÀ-7şÙİêÜöZ˜w2 +Ô›ÈÉ€|>ädÄMFÜdÄMF\¯F»M¥²©òØâdSM6ÕdSM6ÕdSM6ÕdSM6UíçD69uF´IU«ñ~í{kø?[î®!owbH‹iEêİçñ9	°st]U5Mêj±T.Ã 8h0Îz±ÎB×í¹Äÿêp‚6TÕ–\`sƒ¾şÈª–âHüp/ƒ˜ôÚš5êBµØáHR…9$C†I]¥·ÿé]ÖùS$9öXÏEVÛ7R!ŠHŸİßËwøĞk‚µìmÁÔÈåÍ©qıQ0-r8BZ$"@092±<BjBY0Erm1B’zB0Ir6|’ ¨¿Cëu`oñë@"’œM:šªæe¦¶æ¤¢‰7>d}Vr¤c´ûdØ|¯bÔÇÚÆğbò¿2WflPæ$PËuëYÊ/ÖûÒV]lÂfr'kÌ	óÓmªœÿ˜ TØ¯#>ÙàÜÑÕuÌÑt¼	9%…Ã#§›ìiÍ"³‹_Ÿ!H¶…{OqU#Ø´~[5ïäŸÀ:XïVÁ¶90VD³úÕV“2«VÛ"ÍjsìSÕ@	Š?ì«¿f³?ÁQíÏù,Hueà;Ç"‚Çë¢~¬ªGïtEŞ;5`7 4¨Àœ·9_Owôl(OW;ÊÓÆbÏS3²]ËDKşnä2U:~j­uèŒ'Ø°ñ¬ê‘ıÇ:Å’s0p1 ßíRQ«‡ìBË;Â£½ü²º’ähZß%5‡ +úÛÍÓşíF†´·ÖQ›©v.UV°hmˆ¯!°[­+P^<ûfƒõ2‚pé{—ÃŸ%ˆ–1	ÔŞùÎq¿ÂD¸ÈÌºë­¢åx7äãá¡läH-døÆÌÒf§t¬ ä}Üa¿N=ßmÀvüg,,ª:»…‹ÑøÄdâP	#gmã!’åÚóã1*†â;)³ŒP4‡òåÒà*Kxº»ÀÍ¥;‘#RˆIÉn` U5]ipiHZSÚeÙMée4$9e>ã^ŒwÄº*¢ÿèL<òE&‚8\ü§¬G— ÒÈÙrUİrÃL+q 7p£q3›Ufj¶Ä¸osY­QÈlÅ½ƒWÙZ /¶Â,3éQiê­Š¯ÍäØš«X¦¶0'‰=IìI(Rôü^„"ŠÀtr–ºw"ÎÃmKw]Ciyhì”ç(+Óv.{1¡˜VÇ™PR‹
‘ış—ÙO?Íìm«¶A“ƒ7 –ÁëYÁë}iÁ›0‡yÎ¬•rşB}òé‰Œ,¶íñ–ë#˜™9¯àŸQÅ1édj¾>´qF6‚Îİ“5èû\ xËó¦¶¯h^v­Ôä«oÌ‹YfıPVåK$a›üë@³O1ØVøzT¥`„¡üÄÇs=È¾}`Üãâp¤Á…b*6Ö¾§ˆŠX‰bÃup»Íc¦57a/ÔB€Iÿ~ˆ®*sÓlOZecd¶ûºm].S»gX Ó^}jŞ49èB¡á™2²T°­à"+ŠÄªešBJîÇ­¥ë„«†ÏI|ƒÌ²gPıÄâT{ÒzuxÃô	ğ"×(ÆØyô©=ç„3Æ˜‡–¶ÆOyÚã_l÷81­í’qxû1\ßäPÎàö>ß»¯|?xDvÛ ?Pğxm+5Í×ªÕ}šMw¶›¶?/m£œ¼íÎp‚\Ú±Äsùi`^{Ï±ä)&\_éçşn‚º­Y‚µB‹1Ş“§¶şè6•ğ­AŸÜ÷Ås`@aÙ'Ñ+Âuäßmã²-ip‹Ô`µ_&I~Ò‡mˆg­—÷®ïƒ8¸ÁW­âûíÊ"j·!òHü×æŒn]ÌØ`CD?‹úÎ¾BÂİœô“÷Ğê¥ëÕ§i—B–Ë¡éj@£¬­€+è8k›ó‰
w¡ÆÂ¡¬$‘BËŞ‚ã,]‘ş° |³ÎŞ/ã„$DOÓe$(geÃpjRæûÀ>h¶,és,Ä\mâÇRÄQ“ĞkÈtÏíeÓ Â•<w²×³]Àö–[IE°tË‹ùÁHÚgj†¥ëuŞDá›¹‚yè™h4lY7—k±ÄT‡İCMJlyĞ&{6Û9G²’Ze¼w°!ásÂSü÷A®9Øêñ`Ö˜rUºåb/5:Øe]®ïŸƒ¬;…àâ«+0‰»ïPÜ‰`7İ½¬+Ì}‘6^îv<¹w/sÅ™„ŞdŠ”øç+¬Ù¾ÜÅ0I—jü¥Ç³—ÆBÏŒó³]ñ½Y©õ{ÎBlJÕÎ{àuô…ê€,4é€I\LâyÏ]-ø‚8Ğ,R æÁîI J N1‹I?1üİ‡BÊ‡uGÇ~óÓÂÁˆëo/–—Hz×`‡kÄ^…ĞA L~Â©úˆCĞn¿ùô+ÖC Ñ İ:¤”,g"^m–„üc6Û”‡ß¤óZÆÁW¸nÍ³h#A¿Oªa
ËÅP5yá,ÊóuŒç³&ÀVC¢L$hı.î|ˆş;½ ZŠM™9Ñ‚CrsFô3DH=?õÑ½ÅÛx‘ÃªÎ˜nhiŠXQæ)bÙ‘^ùÈ¥ˆ(ãğ©UÆTEL£ƒ¦ )WÓ‰ÆH9‚UaĞÏæÒªg­¨¢çÉ`•)öÌ‰hîr½qbCó€ƒ[6xÆzT’M±Ó#ÙÏ¦<cFß6AC'»µñÁ[qqî K®Öúöˆ µÙ²÷ñÊïºíHÂù$WI€Ë÷	ßY.?ºÓŞ€$Bæ5H¼òLû³u‡nb<áÃ­ğ,÷nò†Ôğ}´,èƒ·ë˜öÚÂáwè‹¹
l¦jIùÚoÀ6‚Mg3ÛŞ³Mà­›Œ+EZ'wlC¾ÃŸ/fQò?icºj#{°u¼`™öí4í>‰¸i_:­­XDOë ´¨‹aŠ!f·(ejJÇ(çq¶È|™»+;Ğl.„–÷	Gs½3]k. Úsf¨µl½¶Váƒ£ª‰ryé:V§´FÑÛ`ƒ/ú^Şá&š‹×ZÏ^k²×`‰«ŞWG¾ºë5ô9Š µÄYŸ©6Ú5~É‹ŠãÛ%µDLV:Œ”.ƒ8J_Ÿ<­ıPbP{„ t~Cåxš•Š~XzÉ`âöú£Eå{Ğ›°ÏƒAÛk… 6*Ã‰£Ú«ÍFÙĞ8âz·…QŒÙCjÅh‚Ÿ`¨Bêõè¢kV{Í\Ó…á•¢½U_¿ú›º-*U %d]vL½õg¨”Øëp‘´€,ŒÁ¿ÇM4ÒÁräµÁ¿Xßj©ˆä	ÍĞ¨ŒŒeÃH 5€¡IºÚD85;L 5\B[r¥‘Q#5şDr…¥ë²5o{Øh¨t¨Zš"­ua-`[†¡’#3nEîÇ„UŸ¶=…¸ñwƒÀ‚ğñ€ˆØËÈ¾‡ÎÖ'œ*äMİ Ü®ğºxvk›oƒ–šJ UtGªƒ”<ı]4êz¼h„_ì!¥åÕ§7÷pCà7š&%¢Šv!X;Á
9ûé§ìÁˆZ‡lŞ>:Ù"¤İ0X5ÏÚ~„²v+ü÷¶c›„vË-kÇÁM6è›uìÅ»‹¬N0ˆ=H>“v\7‚M`¸A8Á1ïŠHà4ïqøş2È*M—¯"qxDÚp—İ²m	ì~’×Ü÷àzxÑ~ƒ'B8âEÅê@Ç>õX-«¸Ğ„áWŒ( Q¤FµòôEU1˜hô¤…@ô$–g4iĞ“u]zh0áèÍ—ŠHÑpâQŠ Ø ]S¾æ™ÔAD,‡ZmËaPš¶iX®İ{ê&Ö¬ù~¾¾:G“Àÿ†½éúªá?o»×q‡/×Ïã½Ş•R)ÕŞ³¿Î¤ÙÏb¾ÿ”Q@ç5ßéİìŒçÀ5Õ¾ß}Ìør"/$µ®á¾ô&€úà ùÒ\¼~­ëãü©ßh«;£-Ù@õJî%×´¥Gı=ĞÄjik¶¡U8’-å:¶§ik.2_âOXòÛhu§»‡MO‡Ş¼=ìa#Äêş÷é‘4:LÜû=–‰çzÛŞ¨DMÄ3—N»j0eêÑ\t€,ÅD¿¥†S¿….“·[9ö"~dòê—ã9= Uˆ@K"Ö\ãHdTÎ*‹¡¿zVÖ\Í³”ºÈÙò‚ü9µYFPÏª+‡“M/6Ç¬iÊ•!­é'FÃ‰…vPU‰G›![ÓD•MV¦`öúG †iM,®ª‘Z£IÆ²©*šÊÊC¤~(CÚ«„’âÉŸîğ¡Šàj[ü‡ÛŸÒMy1×¥¶3æÉ¡šÜ¢É-šÜ¢ıÏä‘Ñ[4¹'ôîÉätÁœ|ƒÉ78DÅ‘úG™ëùõÊcÌuàZ¶d:“¹>™ë“¹Ş@r2×'sıæúd9O–ód97YÎÃ³œ±ge]~†Ğ²Õ÷»«“=;Ù³Uô&{–ÃÉ==;™–“i9™–ÁM¦%­iyœÁ7_*ÂM>Õ™Û²nöıëM¾ÉŒcAofÜd$MFRïFÒdñtÁ<­Å3™ '2AÄŸ¡êÎ|±Ğ¿gä“b¤FDúyRGÔU¯iÌTŠh!O#
³×¢åš~nÓùÿkuBÄ‘®Y›Ë†A€íe—nZ¬¼Íİ{›jø)Ù3³üÿ³¼ø–B?-3  ÆI^[8„DùÍ…mY^^80íû1z&¯0°	Æ—£’Í=Ğı~CuĞC:ÎQ9|âyÓòÖ1«ÔşæBmù’îÿŠ‚u*·¿ EHs[r l9„ª™Ìş
w¡ßËK1xÀ¿»Dpå“ˆÜ:Ğ²ˆ­}c©µ®´"´q˜"-4“PK–õ(˜á¹¼ Hˆ6¤Ù‹z^Wç‰’ßo1Ã	ØÖª¥;ò\!lÍßr(.ú•Àì°ÉpqŞ‹™”è¥)²¸ÿŠÁ\$Ã3[m5jœDbÔ`–Fõ‹¼¨ö¤uxÃ”÷ "å,%Ò1Ïy®ÃÖÁÌImŞäœØ1ÕxO- 
ÚÓú«­ÖIBeÚÆ		%[Õ!Ù,]‘UÛ!¼Ûîdf_b/ö›Uz+¨,èCç:ôlx1Ûà?˜åWmÎ'‘_5˜eùUıÄ"¿ª=iåWŞ0åëÛ¿˜u˜z¼Óí­•nÂ‡Æ“"´IH+iRÚÑ‰šZÁğ„)^öG-(äMR
› 2’6B#Z"7ğ¾Ñ «íz>\†Ğ…!\w<úYE«O‘P…$¨œ³ôm¡¸ ÒUXõÊH¼YmâOİWé›,×š/æPÅVr­Pµ×"®MÒUX/––]uæk8ĞÒÿd›}ÎK'~T¾»âlIÂ°t«J–*cCRéU¥EMJTêh»Ÿx•<ŒÜç¾Ã¤¿ sš ºNxÊ«û©€‹®·ª¨ÄÊ,Á+[˜ûf±.÷½h-Ë2œaZ•=.˜áû¨\L—Ë¼ö6„IDòv·!É[vT½gŸ{ªK˜¡f[P•¡¢Ó™LQWƒZp=ğ)}3M¨’¸ _£¥ÛaüŞyx9I'@r¤…¦Y\h¼w’Œ¤İ>ğ®­êæ\£4”kà‹=A¿ôšáB:è†)«[r¸ _‚õ×´ £ı\†U, ßìß¬€Ç9oÓQ€!C•÷`HÅ½†1B€“û!h«|ÜÂb~îgÕ¶$é¨]È¹ø–a¨šapşŒxø¯îBxûéPSæ¶Æ…ÀëĞ{€áÏ†ëˆëYmé›¥* ŸèI^Oå•¼@3\…÷Î…!â~µ§pœkz®W×™¬Ì
 ®r9Ë3İ:tšB·
¤J®÷GF•²ªHÜü5J–‡@bäÆĞÈˆQ1„’#·ÎFFÄ.I†ÌHjf¢H	šY¯##ÑjH–Ü¤Yru+¹Q?2JÔlz‘"$s5FFŠ!™ß32j$@*äî×ÈÈPwÀ„šæ©c8tŠàxñ¥Ø_¡#äĞÅpC’	û¡8ti{e_qè¼RRÂ´ş   ÿÿì=ksã8%÷e«§.;ë‡üº­Km:ı˜¾éÉv23¶¶\²D%º¶-¯-§'[uÿıHŠ’(>d‚¢¹ÇùĞˆ@AÚx>Â!¬<)zRv3~Âcêî½K;‰<œÒ­à›š¸rÙ£ÚİÜ"¼XQøCL‹?:	õóFÁx**Ë’Ö›T~gÆ~?’ıZ]á=/ş®_áÚe¹k±ók_×¢t0Lµ‚†ªˆô¬"H'X^Ø—VrÛ¬l“1æ`K)›¤ù. €OÿèÔuhŸ
««\cJî²Pd
{í¯´Vñ
ÍSlõ©ñî°H_ÊelY§Énsu‘>îWèë_ã±şOäŸs 2t|ÈÙaaW³%P1Œñ\õÚŒ}ŠŸjÎæ:Åì[ìSL¸ŸÿÓŒlqbæQü;
ÿzñ—¿{¬ØVšöØ."–{úhÖòğ‘6×í@G	u:[øƒYôÍ® B†Ÿåéèíî½F³ÑĞC^`ƒt6°¼äœ†ƒE0±Á9µÄ9Ê~c:rœo×Áöyƒwæy²´fÀç(/ğBÃİzËáê£édè/O	ò,9;CQ0šFËH­:GÓ`:.“ÔLt€3XˆeÌÌÎ"/²™ÊDˆĞ…‰Ì«ÎÆaÛä‡<gğó²Ê„"¤Å÷Â&‚)z‡ó1¡ÙåPÅL~ûnÆ ×¾GgvxÖvFŸ)´¨Îµï; šü@œä§0î@/ŠˆÍÁ’Ùlßûaøª°›¹FÄaÀ´G“¬æ]”dÍÜË¨ã‡¶«©<:b#&q…<„÷
+‚ï¨$ª…+·W½ïx 9’ö™X)N@§•h•9Rˆ¹iø-(Ä³J3£è¬ûTm]ê¾oG‘É­zGSu•Ã +K¿}Çy¥U‘Å© ¤"ÿ¨ŠUJGtØŸÎ/!%ÇÛ‘‚-dø[ÒÅ¥œÈyÔíâ'K5\hŞ™s§\×RŸê¹Ì¨”oÍ˜`©‹«˜#ò`müİÅŸ¯.t
½w‰§ìòÿ•·n'O²pƒdJvq¥&JóÙYq ­AúåoÏ¥ÉUì;°©5Ú3‘w¸’´˜ø%V‹¯—¾çÊå’_÷ÂSÚåa>ï…ç½ğ¼á$vŞ[ŞûCG;bˆsŞÏ;¢FK› Eq Ê§óöQßö¼}¼ØöñâêÜİõJ›v¾^9_¯8½^é˜Â<ß|íætc\É=Ûà¾"yíŒë¸ü[]ë»˜¨<V¸3E~ÎñK'’I~ÎùçØ'¨Å-Â=â}ş
Á‘Ÿ?…8ëÔ³Nı&İgu×ªƒAxMrÄ|M¶áat½|PÖóÕ¨(±g›ÏKE\Î^™†ŞÈ_ôf†Ïõ6U2~YY'_mÓêú³ş ô“[˜ï~¸ŒÆÙŸ·¯|ónğÃ§ëüÿQ€ÅhÛï‘Ÿìû§$¼M¾ºMÂ*ÌG«IXÕL/UøšË@çs5•‡<Ë¥Wæ{\¿ó—iÿ¯5ÒG‚çA×eÓ²½Cé…+è8 @YCNõÜRRJ9kÜrppw|0‚µ±Ú©İnã'?xş¼_Bj=H][¯ÉÅ#s´!æµ’hš)îMŒçe™|½®Ö*TĞÒ@³Ù7ë@’r›;‡$Rx®ˆ¼&o’uŠ…Í‘xn‰|ôS¬Q±LÇíX®ëéwÈ\¤SşºYG×ÑµËtí|ù3â^8u İ.ªe²Cï¶1ÙLœ-(¦Sbo·hïW®èdàœ’ø:q§˜,×rêˆ>\õ'9¢ı”+ Z‘·i,Nrãñb<f]?ÉÕ›.92ëõû=o¬È P&/£hæ«¢¨¹Ayj§§/ƒbê•'áÓtZó¼Şøsº€z3Ô›t>ßµ™é’1S¯7öfâÖ¥\&“õÃÕE€©x‰%”ã•—¡«¿SX,‚é¸\é×6ÖÌÂz=]–¯Çpê-†#UÙòS`†óİ·?ìÁxR«5Î»ï©í¾º£KÉA3/êESÅÄy¿9ıF:Ÿ»”Aä†È;½SïpÉÉ$ƒY¤‹í8CÈÙŞ%7F¾7xg‡K†MfıÎ3dåï¾Ü$É–Ój]ø(‡‹Å@]ëBÎ¤&øotñ»îƒœF}øw’¬jË_”Ã:NÁ‹_¥ÄEñkPQ‹¢—qO7W¬!Ğ¿W¿¡s®mıjM$Ç¸ÃKr[MæFÚ‚ä÷ÃMê¢OYÂ'¨.É‹©<%)Ú“Ğ…Nñ¦³™7UU_µE328ªê9¥µú)£–”Zú5!¥(Õj£V„˜ãµæ°¶çÁÓ—Ğó.«¿°ÆÊÿ”SqÜÂ?%^eÉŸâ³U±Ÿ¢7¸Ì‡·›zü"\Ş'óšƒ·°4ŒIègh²`ŒáøŒİäö€—[j Ğïÿëğöağ¦›B”+#ésT$ò«mcñ [ÛXtœÀ§›5nVkæ›=Íf÷?<ŞÉV®%ÌúáÃäĞNfHqBZoMY¨ººM;B[İQà(;C‰ßŠßBö¢“©úç°tSëã†¨pİ;ôšW/TNôi5©×–ÎDaúRš²Â>Ró
cß`Ñ3ƒmCÊÏ×ûÕmuF]owim!5Œ¾¾ÁSàoÕŠ@o!§é’TñMğ8¤ït¨ïQr›Ği”Ì7ä_êv¤Í\PcïÒ†ù[¡+Ìà8ø´}Ó8U `ı0Ä«ZH>Û„$SÒNÁZï‘J—V¾+KÙ=âõ„›)¿mğòHµâ˜RX';¼İ†sšëIÛ*J¶)Ä£`@Şd—&›’•SDŸ
éF¨¯Õç²@eÈ6ù=^Åéó|¤q>Ñ?ó-fá^ÀbxX —Š	Ì¿/ã'4ß·sjĞgu,£V<) ÚXÆ-Ì¬V5¨âu:›³²ñ¦¨é‹h›¬nJ…÷JØI%mi?M±Ëf“00ºWòA`< Uà—Œ­,Je‹.‡`†ï=Ö€ö¸HoÃqÅA<¤·»Ôÿ‚š!+@r©ô\d`òö÷¬<|sIÉ!™á§ù?ì‘jkÖÉ˜n³k5"ÒÛOö¼{06CX€1ÃüËfIŠ¿…T“Úc®€1ÃüaĞ&«„0]•BjèŠT Vcú•Xö˜hwãuÿo–= “”æûƒÉpÑj®r*` ˜b`r¡*pÌpÿ†·şCib GêÇ!ıË2:7­HƒÔfï“° ‘Ş"e„~õí|ò—MYswÑA¿‡Òa“÷®qU˜:iš÷uéß<0ÉÉœOâQ€·ñÛxÁ›õÕ‡Œ%pwe4õ“0j;_=ï:yàK
¨[ádõ¢ã]r­12y6n]òöpÛ‚¡‡›R.ZK÷±0Q#ñö§“ñĞ_(bwª§½·è_{´}îZîjæ@Á]T(âxê^®Œ·Y:"³kœaM½ê´É‰Êì,‡™åo÷êê!äN#È¤˜¡ÕdÉ.ı·îQª3ÕSÁ>ÑÛĞbÁ T–şË|yX]¡Æ¡iâ%kÁ7EĞDåÁ
’íPxe\œ8;”XÅ^C…¸ÑåoÕ§èf×	‡ÃÈFCÍ®C‘§ÕM¥ÔúÔÕÚša~Ü•o¶<&G8šx4G‹Hõ ’Yh•`Éºå¹ÏÑœ²-›ÁzZwº£“ÒĞŸ<UÊ*6·x™Ñ„‚ê4YÏı•2’‘å¨
–1É÷¹Cˆ×ûo[¬¼[•˜‚xEP	ÀÀÓ2=U=O^Rj>Œq(yWÓ´äÇ4ëQ¸Y³Àóú×².+Ší,¿÷úF¶´b€-ìlÂE››‰ŸLfã^/R< cšp¿]Ö‰YÌ/‡ Òƒ:ì:~ì®;5<v¿ôB®;•ö›J;–QgµnNmEÏíÁW-Iö_˜j­V#i¤Ogã±TÏü»õÀ°qàfìÓéğÚë÷%>(MÂ_i)"Sô@ßÉv‹‚tî¯w_¡ˆ»d¹'jE–ËOS?xDá|Eş[×2‡¦i)½Yz»Nãôùªì‡È/b]^·6U2;¸=—Qo$n6Lyvåg$˜ÒƒX›è•ªä›hvxôMTãc22]ø¼ğ~S”‰®eÉFØR]UG`~—M	âà®òã?²¼½ ÃKQ Õ0Ì¬¢šzÍÍ*/ò«\Ğ
­­u·V‹±k;ŸcÅzÕ>³’rkkÎ~Im“…¹} ØÑù„0èÁo²é¾Îf»áNGí‘kæ€cfÄ:Ç¡Ô‚\ºñ»ˆ¤Z%m'¿ÁWÑO*„¼‹¿DØpzÅ†™ä_ü—@·jö2œ´(MÖë½^üùÂ›šeÕq*«zR9u5:óRÅ/_=,ãIÙìä/±TæjĞ“Ÿ|úßş?åënÿw6oß½ò¦ÿÉ ªÂÇªB@g¬v’Dm K¨ M¬ş"º/G%l\‰iğüíÛCÚÏ¥ëĞŒtğHæçKÛv/mS™I–ëª©­Ó‹	¦+o!9ÚÃép2
W]v¦‹ágåk>M‡¡d8-Q~Æ#­¦ó‘O[%·Ì¯ZŸ‡ÕÂİêB‡¾Í8Âî(h«è²§írXídIç„ÍMb&û-i~7W5²jsBÍş4Ó÷ôÅ–=ô‡a?è·mÿ¶~“N~èË~éÈûƒúä9 `áı,1€E'PÑØ—Ö_ €8ÆÃÍØÜ¼L`fğ|€Í’iK¡ä*8"†<…tt7éGşÔS„}¾°¹–e“ĞX_=ô´©%`®G–’ãX‘ñY–cVµ)4¾½@ˆŞe.V†X·ÜLöZµ‚2k5¢VÊŸ€İ>áN‚ÙpØëšR­äú1=Ø™4<ĞA{=h‹Îş—8h«×MgÔ)Õ•T$G‚©|n¤_€”V©¥Œ–/ï™w<Ğpò2ïûZrØtyK^Vµ! «wÉ)E@¿‘fÚ	7L´‹4PœŒ
'tÔ:{JõÖAgÒAMã6BC­8dqñ Ò¥Ÿx™Ë'Äqôúm´XD¨¯!u~Ğî»€.oßJ <l•±äHÜÄƒşb:Unâ×—ğ©šutéo&6¹Úr·ò—Ë-*~çµ4VC×—4rCµóÁğB¢Eº¼¤{—2§aAb8ì&.ç{;¸Ÿæ)Á­u¯ßUüô‰ é3cy»Ó=4r8Sâ®Á\h»ñÛ‰ÿL\>][ÿ< “%F:ZS¶tÂg¼=Ñ-ğ5˜M?u¦‡—ê´9³	ò›«läO3UAú9ºqŠ‚Ñ(
Å!Z³'Åÿ8z6‚”T…`D´ğR»# È@İ?Û–3[j¢m‹=.:¾?÷£@qYÙæ6ÒªÔuPíwL»V°.7Šïíğúç¹'J—sû)–Zö6¼ÏÑ'úsîs?çøD­›åÀQ*c_“G€Íçu OAÛAƒÕİš]®x¸N\~o<ÔÕ)>¥÷íè°“T§¸ÖÍ«~İe×Pw(½!÷F(4*ş•÷¹ •Z…ÏrE%òñÉ4	E+ÃêiV½Lx›ÙÏelå“ÕJy:g€úF½ß‡^o‚Fı….>÷2+Å]AÈ'üû˜Q£JºO~x¼_äOsßGá¡ó;<b†˜Ç<e¡Ãx½°?z¨vˆ'“Eö(âŸ’FhÇ^¿çÂ-©òÕru…H2İ €dögİ¯ÜBÛÈ¾İç+©íC5-™[ìrC.ô¤A@Õi·‘q\ª¾®.eŠ¼ıˆ¿&[“]íğ“ì B‹Á,Ğø‡B6-ª:‹å—¹P¯.¾0ºŒ|Be][aTG)n+àä+ÜV?AÊÜV{šÖºñu³à-'	§[)òâg÷ÒıÔÁ•.9Ö8>XêÂğ'¬w•ÂŸ,•--4Bn@ğ†³ñƒ/ö›’a·¸±ñİ^>ûçİ¬:`ÊqÓÆ­×Ba±:5µrûùD%ÿDäÈZë9ëü&Ïui¶TuÄU¡V÷U0Øş^XIPH¥ßWBG¿|;IµÍ±Ü8›«ó¶pÌm!Ÿ¤ãm#m(–ÜrÂë0Öãúzí„$Úø½¹Súí:Ø>oR²ÄV§wzßúë0Y©àTü}åáœ|ıIX.;ò—ZXĞHŞæÑg0Ús»4œ6í2gêÁp2ô½±™ó	tà>iŠT“Ÿz¿)
ûÓ ßŸÚ¡vë6Üª×TÃÅbAH³×Âù´¶¬…‹µ	q‰°%¹m$+Ò®îjã¢;ñ³¬=xsÉøy°abM£’A”{eÚ¥0çŠâ,ÌfÂL~Ø›÷Ê†öÍØ1]^,9óİO™`—Wô
ÛIi<),#ú{?ğrT—q»c#“mVYk‰Tµi‰¸œY@Q´3Ãó¸J-bgşÌB4ñã™ŞoìNÇı ˜ú=+¤wD"~Âa‡{0ô†¡¹' ¾^Ç+¼"Ã·«äcKss<‰FÓ°ÿK+W‰İ_ôûüåš5]4;¾Óéb¸è†/øo·hïWïã(İYJÅ,ìE#d7-TŞ ÈÇî°NÚï%^à…^Ğ€Š÷hU]	*Ö_¶txA?š";­À³ã>ÙÄÁ‡À’ì›G½FKã¥¸¶âï“5ÑÆ”æ€Ü>…½­ÕÃgÍ®¾YàöZ¸É+•“¨õ%O¯CNôƒh<éûºÄ^]åDÍ’tÈo6ñ¦£‘.•d§™ÓWò-ñÄ¸"× nÄ…Ü^;L›Ø6'³A‘q±)=êÁ"ü`†}_«\ÌçKN•‘Šo6ZRùQ÷ÄË‡7y»[Ö0/"ç1íÖ%-“QyÈ«†›pãl&…+Â¡dä¾‘¶o®¿Ú)ï(å(šMdÅ±ãÒ†ÊœM'¦éÔ—wææ8E¶H~—la^—®ó%Èd×á*^LŞ>‘ğŸwñ2E[!ßïM|¤H7 ÏÆ”ë»ÿMb¹¶tşÿñ¤Å`_ãõS¬ŠÔ`Ÿ¾ğ~]÷õ‰š®µŸ7ÛÍzªBTû9^G‰öã¥)V¢zfnb<ÍòÕM±·„±üB³$l‰j{Ø&ûÍ<ğÇ«ó¡§n‡åLû1J¶ûUMßıbIjíèæ[ı¥îÍLİJ8Êš:ø×45í OkjÀ˜¾³©¥¤›nÀğ‰2Ì‚Hu’ék}º"Ó“`¤i)&Ê§ÚKÓVÌ‘O”•¦©˜ ?ÓlšÆc1è8ÓsšÖ1vÕ4óî¨i:“C©FÔÍ‡8y™‚Ôµ§h][qö2õ©k-Î`©Mu=ÄydÊU×\œK¢kumÅ©ÌT¯®µ8•¥&Öõç³PÌº³Ydô$ŠĞ£™é@—Á%›k@Ÿá%Ñ€Şe¦D ]F—T— zŒ/™Jô™\æšĞizÉ Ïì’êÈ\’Ú<Lß@ºaÈÔ¤–²l ]†—L	A:aA(u¤ã(S/Æ—T3AºL.™‚‚tÂQê)HÇÙe©®{‰¹_Ìã²~Áú,»28éQ1­"´‰·5V÷ây^ŠÅÒ¦ùË¡q€5&½adv¿/ğÿEËxm”SÂùèïÒßúa8#†İ²L,„j€ÙÆ©õF=Ï0 ¥Än'áS²N›2‚qG³	ªB³}H±˜gğ)úyíd}F>I.Ÿ›°"‡áŒ'$kLŞq¢uš2nãJ­Şjè‘8ˆbËäv¸Ó2ˆ&áØ[´QU¸‡gÙ¶è†µÙÎÜv|Ef÷˜zy^ú\)·e°²|Ÿv3g#¯ï÷ÃJu¢ã$Cûê¨Akà¬{é?àI ÃpyÄ¨‘b#â£o’›É"êÍ¢à(RÓ‰™…M@5§Ñeöt×¯fª›ñGÓÙl2™œÔ7¦ ªG½†ÊdÉBEMĞ"˜xƒ³Ò˜~kw–_~¤<Û~õm›Ú~ù	ÓÍ¬MıÙ$¬D=Uë©«VÉ	¡Ö¬…$Y,öÁ8òQÿ¬XùXeù
vó¢aeÙı´ƒ(«ñtÚ[Ì<EÕ–[“6H¨Lwvóè“4ìøOh25¢f®.ˆô&Q+#oÔ|9JÌ5m£l‰³Q0°Ñ`ïflrÁËÓDZL2kRÂ6N3—Ëk<"Ø§’^.ã·Aò£O¤M˜lÚ˜rĞV(ø‚Â°’H¶ÏV›d›Ñq¡ı¢I¢}í…;Dñv5O12#õ¤¥ö¸ªJK†RméZ[©00°:ÓSÕMÕÆËÊ)?²àÇa»hÙ?Ş†qJ	u°VãÅ(¨ho>gOùn´‚EÒ»pEìÊõ*4²Z¦ğê”hèæ¢Ÿ™¨œş)‘¤Å0$–[èä_I|®Az(º;UŒ”¥ÎÔ¬Q[›ëRÄå,êÈ¦Ñ&¾\UÉ Õn6KËDH‹qÂ‘a"µei$/ıQÏ³BIgôg:=nã„Ém5ø@3ƒÅR¾-æU—R=Ë>{u‘l²—}ÇO`N–<;ò¹<ê•’ge²¼…‹Ôˆ/­ô	#mo2GÿîB^BÉw(ñ¹"“E–eYN4ÆŞéHtUçİüV%˜‡[r!13lš-·9”ß|W<Mı#ˆšµü`RRlïæ1õV ğ†ıÂÅx2	{ı`¨¨IX)|" ¾ºÈtÖcP›d³_úÛûÿ¼|Mø1!Õê°I¶}.u¼ıÔçá®}åÙ®®èÑ®ˆñ“]=İ<äs^_hFñ€,øõxªÇ´6d,d4h¿¥í¥B>—¦í…9<Ğ­Âçê¼´yëbÉCö<şnÿğ€v©QmƒÍcây¡çË*ò‡’äOÎv‚…%ØÆ]©êAEÍªQó*VÙ¢^• LU«{7Õj&˜m¼ˆ8µ%Ky:™“Œ)6V%7h rˆi¨İë$½£>ÇÛO.Ã0ôÆƒ™â½[ôÙÕ§ú†¾Mç"ku
A&ü(Ê@FË+é+D	HM€k7?è3õBNf1+–§pë©iÅV·ˆÿö#B›Ã—(¿lè‰Úx%ßü|{ó=ëm“ÕM9ºWÂb‘X¸« a~É÷Å¿r×G²\fóÃï	é"È_ÖƒS9ùPò–2ùÁûª#r*0-h"ÃqÈÄÕÃ¢%îï?6%¢
B‰®jŠÀ€à|CsW0ÆeÜ¯Ã^…šš«ƒ¥0wE(„ª÷$IÇ¿\â+‘xã¯ÍÊÔQ¥
¡ê3ŞŞ!ã@!K¤Ø˜(LMEé*"€÷Ï¬ì›’¤ 	åR?Şûİï¡Ö‹äw¼RC†ÒÇôŞ¥‘U¡æ'ôµºNšR#„PóZü£¯Ÿ©mwGŠº6¤FÒ‘ô—)“Æ’ƒİ£TÒ¶;”.Xºâûº}LÖe£ da'|xãÈJPå2Ğ&îVk ĞR 	İèZ	 „š;gÚx^æ<pãEÁ‚Pğ.Y†X˜‘ƒ-˜ÔÜîT¶…®Æ½î‚Gî—\=ŞÆ„ˆ É‚S$‚é®ä	ñu‹î“Mc-&ƒ„êò<‰LseA‚â§‰ÂİÁ³Ğ¥lV‰•Ğ˜H«SÇM‚ÁÄ÷ãìÌQ‚„Pô:Io’ÕÊ_‡yÃñ%ã'ÊËæFXH³$i=»²ÃªĞ,d–;@:Ybã³–s;Iõ­pƒË.œ-PÛ“<S_îóÀ 3}§öOŒ?ä™4ÕX¸R²4ı'æ=ïıæ‹_<ß:˜dï²]IiXƒÕ|6ŞdˆPzœÆR»“ØÇä!^ß'_Ì’ÖÑPB²ğ:0XJHV–“3{É
{–üaĞ;#£€ha%Ü'	‰Êqd!0hP[ßÍÊ0[Ä£]u7¼c¿RP´ íSd&_ø.–ˆ$HWøë‡[?0J¥\«)É¼~òã¥¿X:»ÒÁm´·¬âÆS¥‚iÁ/"{ùHŒ$rpUäŸŞÆO~`”U¸Ö5ñ†<»n®İ(ø­À/kªñÉß6^G"<-[?reªó°€^·ë}úˆ5ã¿¨7êgpå>áAw?RûÒÅŞGàXÜ™8;@‰ğ@‚\ø¸°Í
@ğÕ*Wò²_©êr^µ@hûz™ä.®ğ(x”…Ûğ
«h|
ïâ‡µ¿ÄöÜ!C#Á¨@…Ğu¿õ×$Êt®÷aœ4%H Ÿ©LÒM÷z`F¦˜³\94Ï,î»ùËôñS‚Õ©H;I	‹J—¬>&øàF¯0Pp?;ç%Í2¦ÆY¤ÏvÇùyºİv(@ğJ~Gn1´Ñé Ïh‡š_UğÀ,NQNL˜¹m«ĞàÑâİº£°O	¬ı-ì»dûÕß6¿lTC…sÌq\ª(Ì›Dô#ÖJ¹%ŞÜ—$ ´ˆKo‚ (vˆ!8¯S¬?¡õ‹CšíÓ\*y`ğİŸ¿h¾B Áö"Ø¥†³ÄÖ!¶oÿ'‰×ŸÑ¿öh×\8 Ady8šKGzƒì6¬X†U+b¦\D©À@g‡GÔÜDØøû]	à¬Ş
ä[1³½àZÆ’:1×Ex–ñ4®_,X½Sp}éĞô¶A¸Oqa'¦¶¯qHß7(ò÷Ëô5ápø9~xtC’leb‹ °-AMŠ}šÂíÃPp/Ö;RHø>ÙÄ#O°	5®‰±8ïD
àzçøTêSúÍ_.7ş¦¹*¬ ƒÅM¯:ˆŸ’[?ßæO¡›O«á‚=*.ŸI á²Íp´Ôxˆzş¾Ç»ßg´YZÄ\ÔÁZ{E×Æ7Ó`p‚;BDxV³òìÈ— ZÅK9"F„Ÿ%‚#OÖŠK®ß´h7‘n·Œ“á‚O{d"<ØéÂßî^ûK48˜†Ïè	›rÈÍ³"ğUÉí~<âÏ!Ù†¤£Pí¤¸S>Ã®¢šd@>İíEö£·´Š>É@¡>èìÆİâ6¶˜İ[©G?^Ów	8˜àsna:;¿W!ZÓãîU ª‰ŞÇQz½§k‚¨'ZQ„Ù€¦üY£kº
¸ q„¿÷WèÃ:j|ËRfÃ¡!øÎß¤J ø€>ûëÆ!˜
 Óª¿Æ&Jˆ5kãói	t+ß$«M²C÷.¢uªĞ t»
ÂJ–tã
âóKønêõ6ùºs—KC†hG3×†$ôíü†\-l}‡™`Dğ†ÁÑC½İrÍ,LMy_¯ŒM˜…|ùûÁ/C«¨3Çu=©$PYƒFÎoÈ²øıBi¹h3&CQ¤¼srĞ„‘ÇÑÅ«¼[VòèâOº¸Şl–ìyÎÇÄ'K¾ø›"9Ì‘ì™¤›*¹`A Ş÷|<C÷³!ÕPnPı'ƒÑjÑŸ-
Pü„Âì]<~'å*‡So1™(ê£“ÖqøWÕ¯Ye<2-¹GÉĞªFÍgiU¶àuÀ¡L­J âZWJŠ{73¶Æ!¤Î¨0^—ò´fmc1ï»ÜÄ¾°†Â›ø.^š¬Şe²~P­Aú{?°ğÌıİ£r‘b—Êå+É—/èy×ËÚn¶øÿR›»,ó*eSw{}*fÅ¸Ú\æ
tÎ
éüşd1B#İÖ\--K”¨v{òS_ĞnıÉ0˜Úb}?ü²Y’½My)v˜€şt8	†cÃŠz2oW›TiéF=öaoØ3,](£¶¸Áv.ËX«[»<¬B3(¤¾Ãçbİrm(ÅV1ö´[§¼L»ulÃÀTÚ0pnœÅ‚[én%$WA’º}A¬aW;Q;Ä@¬08Êœƒ­4@ªéİŠ^¾ùhDï(³`ÇŒv–b¾#öRä-GPÁ‡ó–ÊBğ¼lgÅ'•¢ûõr™?448«°Cƒò8RVÔ+#¯H‘qh±¾{òB ñÕWX21_€0Ş$Á~…Ö˜€ıVøOÉŸ6Ï<J„ÎN=A¸Xh±0³ÁZü’;ÛMÇãŞ Úãı)I?%aÅ® *†£¤wetÅ’SN?|gÉçY–kêM ëvĞò®Cq@6¦!jj–úä$*Êå‰ĞJ[şğÃhqÇµ”å«ºëö\1Ü—ˆØ…?}1¼hä)PÙÅx´WO;l#×½WşóˆoÕl;Š__š÷ë+[@üúJ ¦~}önúõs«/9ËÄ¸àl.‘5 ‰èÃûC¥óaÚ˜rØ´1 b¬pxƒª¡Ï×A¥h½^G’E¦ËùaN„ê+š³³FG±ÂŞş:LVó84SqÔÁóşèãĞ\worğö¯:ä,:Uj¸xNÑ?şyñ0_(‡˜ÕóÛ¡Í×‰²Æ£û\€V×e³¥™6u±¥³£ˆßŸ‡>
Ì¡’l	²ÿk*Ì‘Ÿú“I4ìyÓéÔğd¢&#»İıH¤Üòjd4î{˜&D.9w˜Po13<ªé EâôGdyK†½
ÆH¸^$[{
Æ“şy£F‚ùn¹ß=²„IvTÁb0{ı&Tè3p!BxšÍC@t‹õD`ÛÚë-P
ø÷ÕvDŒúø§·è5£b‡Õ¿­~
fã~¿Ñ<°|Ö1õƒ~äMïÊ5ª:Ø"´Ş=&i³ùğ§ƒ(ÇõµºF»I½Y¶êF2ØóM³—Ã¼ù¦İ²ïˆ3(OôV ·åádj9+ÃıÄæfPËKO§;¥”|'“YµÖÜÏgnQ¶<Ÿ•3›|h§	qr`]œbñ$]!¸œ“)/c÷³Ûí-Ï6wÚ†8öİóL7şƒm›…cÔŸkÜP~êê ŠØÏ*Vla#`§Ù®ßÑÔ`İs%?]·,ï™/c”ÜŒßWÏÅŸŞ.êæÊJÈøh'j/w#´°:˜—£{zü<ÌÕæ¯ŸSD½âç½6k[2óêEödŞÃÔ‚Ê\`g+¾3V|æËs?Ó¹Ÿ±å©æï© AåõÕéŞù±l\Ø‘Øä.Ø6|7™‹¸«›˜ÙÄ:&6V;‘ïÆ•+yÌİ‹NîÙ?o0Ù`ÈåD{vmÒÑc êıì‰ƒ¿ÅÿıšlÃ»Ï·ª`Ôı[ª*at,1›N{Ñxy7Z!¦Á³Íp0‰¢Şt`‹ó¡…›"ŠömÊü¬>’&¾HóYîè"­á¿<]q#<Mûn»QE}±`ªkİ‡Omû|2º Ò5ôÿ©ÚUÆ"#cêÚ¨Õ§>P‚IT%->ü[œ>~X?ahNÂ£
P4Pl³Y‚R%µ4ıJFÆ<HöŠ@EÒb‹’/(œ-5Tê†y” n%f>~[Õ ¾­êo½­ÆİÍàí\d 
¥"S¾RÀNôÀOŞã3Ş<ìWÆ{°µ’K@äïÓÇùÿ  ÿÿì]é“Û¸•ÿW´ıe<UHİS›Nâ+ñîÌÄ5É|H¥X<@5cJT(Êvo*ÿû @‘8H€ÕT7U©‰mx8Şû½ÀCœl· €Óazü™k¯g‹PœÛ‰$qb¾ œ€¼OÚ¾S˜_Mše©~ÒAjMUì`é5´’3¡JÙvïßÁzyŞ£ÛÉ»şôggú­È+áF×‚©Ëp÷ØaB6í“şôˆnÒjæ¤43µ2~(0MÂ(ø½@¹[Á‘:Ÿ£ ğW°s@”DVJƒ€ÍE1Š¼Ôqç–=_yA/ÚÊà	8: àf÷§øúEà}2H	„®À«(Û¹‡7©ûÅõbPÊÿY
‚Ü¤÷š…‘%f,¦°Ü3×³ó1˜~8ø,œe/ÛŞ4V‹`­xÈ^BÜZ´#>-ËsâÙz–x—XÊb±ƒVãˆG½Yµ£½ÙLk¾Q<8ÍÑNâ–Ø}¹XNW«nlÖrĞk;°Â•jÎ1–¸Ù¸+É½†¬¤âZÒ\¡6(ˆôTÑ¶g
+‰wµ<ÕiZ™rÎV+ë4S§{ÎT zì‘VŸ×âç¼ 2~YoÏ+ßÊĞãHÊòÅW9ıTYIÕ
|—ñB‚‚•a( @?BÁ5CÌÓ±dNÉ4×õÑ/Kí–ÛÎ­q¢¥«û‹÷;wHöï-È>’*˜…ó¢/ª]¼Üx7ß
úñh2`ãù—û4Éİ 1úås¿}	^ QK5†Á´’R´ÚL/pï«|&õíÅ3d×­…a\§®ZÏ¸ş¼a÷ª€aP²Û0U7ø¹ Œ¾Ñ˜ÃĞ÷öeAÃ B#!#¨ôDcÄˆ.LŸÄa†§á·‹0üÈ¼İ˜wˆ<iRÑ°èÕ»êdcGRœMµÙF\Òîà+2\{i‡%ñü%yŞ€C
à_ù4ºe¦ÿœÄ§9éÿ;~¿-Cq^ú÷9V´ì’*å¡Âåşøß”¸œ®(³N5Ï=ŸøÄcƒnËpC]5û¶€ˆ~ƒ×@`›o÷YTòxŠóÑîƒ<oú/?üœÊW§(FìL×±â-ùö=³—¾|SØ ˜O9ıÔ|ÁOâØ=ø¤d†pµ…Ÿb°ßf|Núy¾úSÊŸ Ÿbw¿=ÁyîúWç©Ï-áîa…<İ:¬{’±åa$ô*{ÄƒfÉ1Cã„_ë8Âjé.ƒ™-İ'.¿É¹+wëÈ³¿¦ò'“§_—>×®¯ß4zâ3Ùí\qò¶â]+0_-|é	iëowu/=Ã™mì¹¤©ç¤¨yÀ~úÕó6¶çn¤ÙÜ¤Íşºÿ´O¾È_Ñ…S‚©½˜IÏ7È›®]¿µ½×ŞR"ŞgPxüš–Cw:_¬6vË?BDŒj^~Ù¾»YNCí¦?¨¿…™DiÓ;ğ]ùm ¦^ÿäBK*Ï¬:öt–k£Bß	ùQB¬†ÆÜŸƒÕ|¦?M¯İã}æ
³[RF–ÖÔSë>#öâÃ.EjÌåláMõ™æ/]¶×®=]¬¤'“ä³‘µ2N—›Y M0+gF¨?Õ>¼\9hyú×)É´ß–4Ó‚2áìõÔPŠö”ûòëHq´ï6	E+êsàî?½vÓ ÛÀI#ÊT¡3Óq¤¸ez%\éDUŸ81;³d÷v—ü3êD»Ô2íwIºs³›—İF^iI™ş›(ßï¡Ešu"~nF‹òÏà»~·q—ÚÑ¢'kìL:o†¥,<9X=Û«cŸ#7¬l£—³	~3Âó8!ñƒª¡Â¼Bù1·ç†u’5Ì|‘¦-Ï+ÆïUç´VM‹MÁ:ZííH±ÒlIKSã
uM…œQõúõjkåK¼ºb\BEX/4ŒåĞCk?uîĞÇ¬%ÕÚA•ø-íV¬Ééi4:tÆ¥go«½İ¸šL~‰QÒX“SªhI©±}óéO‘°[0F±h7›Má‰CÚX“7+ò÷Z+ÎZgQÍ¥S]³œÀ‰©®£óR©à©šs(›òÅ@/d½+.£9›][Ìê4b×n KüMå:±¢m^m¨¸¶$²÷I3üö©Õj«3¯[NÙõÓc¦+¯^³§ù–nJ˜ÚÓTY´RIÉ‘MYìU×Dÿİïà6¤*5y¯q4làFÔ¹›täo:gòı8§4®ÿ(Eü±·¬ù²ù5Ë§¥¦4œñpÿø×Ï M£ È…³²c;ñòÿgLş¹Bzù·,Ó1½­TËGu“VNƒ¨F>°†pF„]ƒsËkÎŒºà·`øR „+{(©`y¾N1Z¶NÎô‚hK>Ø››o; ×Ùp6^tGÕxûwíøõtğèYAÍ5£‡6à 9  ‡Œ îÚˆ#ŒĞ/|H9?†œR2‚ °c×.ÿ¸9L8:2TÂéŒ<Wx‘vNªÔbŒJ¸ÎYÎMiì8&ÕÑH°ŞªÑC’FPtîÚ‘íé€Ô³‚kÆ}0¡¥§™Í`ÀõGgŸ”ü ğ4 ?ªeèÕ#÷m„Á@½`Œ(p¥(@eƒzOÈÎ80âÀˆıâ 9Vjè¥>#8@:7âÀˆ#\Ä¨\|-'x’närî•K5ú¡D×›–Ğà&F^å;ø–NŠ&7Ö6¦è…í¶ê©¶—=…9å£¢³ò¤˜¾İSIkfV@ÈD©å;+şøİwßMŞgß'É'÷a’%“å3š”W}âß…­ÃO`òÏ#ú¢aÁ×ğ³ã$Ê^ö¡©ó® óá?Ÿ Ûï@q.ùÅÍOIö¦ÂÁÛ4MÒƒ#ÌŸÆ [jçNC,TEé’;-ÆĞ¦Š0b|“ÎÆ÷P e4¾ŸªñıªœMµ
}dkŞÛ'yaŒàÜ"#ˆŒ Ò/ˆüÅ°1A“8¿ŒÆÄˆ#L.€èîº¹x	I·fÆ£€=`D€zŞÓÃ	’Ë¡PİÛÃ¼©¡sB25@2É$ıÆ%Šä4æÎ	Y¾ïº®o&´Pô¯o³Bû¡"?»âCOnŞ.(¦«~^!±¬å•«W¹¼¸)–ãF¼4×Œ‚èW–ÎR5áøC7>²+Ó‚›;€o‘_Yz £ÔmöÄƒpLG{j0ö”&§IJ‡´‡°A{‘`íQôÅÇğÁp¸ôiÊkÖwúŠ‡¤ˆ4˜ÈÍK7œ›±ùIïF$‘`D‚³  4ĞÆ``ã-7`67cà®0bÀˆ»`	åYÖÜÌáúØq‚^5|‚g¯«U$ïó>€ƒc]5xF@$ªgå†LJ«?A/ô;K@µRuÜŠÇ¼ôúúŞİïAL_^æßzı·,‘9[³Ï×OYZE¶ójşnôÓ{`Ô½…¬=ÙÛM”LŞ¯vãíî=ˆrÑc&…cıÄ*^QÜxk;hGWƒ¤0S}u¥˜¹¥ÏÈæ}Ğz06ÿošM`Árì"ë+Yºš­wĞbÎ å+~ş’	ª²‰s@âLb}¸A ÿØ³éPtö›öÍÔ*ô#£¸šl‹Å|¨Üb'ã"cn¥ğóó*ğ5>Ô^e?BŸ@ÊÂÖiÄ±Åÿîú>ì¿àŸëö2ğ÷@Ş“‚¤`ï®ê]àfüüúÑ1ûo<Òp~ï&Gøß#ÁÙs»2ÀrÕÿ†Ş›Ï«ã§çõFpç|G6úÂfg>–EW&´¾‹ZúRy¾¼}êèœ‚1Í¼ñç«UHß3®jÈ"onVíT²€`n‡@›àbÑ’àl=[OWK‚IØ--ÙÌòl[ú@±€ZûÆöËÕBúf±dlíˆÓÕb:w¥—KWÎ²Z.]èY›•»Ô¡ØĞÆ^¯ƒÀ–>W-ÚZ‡M¬D,İ½Ú†.oûãí¶c1‚íœTY=*•R _I‹êT«jÖjÍWPùbİ&­”®Î,P­ù7€ì¥Èî+õ÷ßW˜q˜éÈ”WİÄ‰/¸QÃÑ1X¤Ä
5eÏ«[åÆ~¢Eg
‘õXñhÁÛcÂba¨’ï."¯Ï^âz”ˆK²7²éŒğ6±'ŸokfB?]q€ÎTO¡‡QĞtM–EI\Z –p1Uã&æ%¸ÆüĞR|©ƒ6ñ8×i¬	|¥>í`ı†‹¯$"¥€E¿“Ÿ&Ô„'†ŞE$ÈDĞHÔrx!Ü	ÿŠ²a1
üSFI‹¾~®ÏCe{­­HãÎ#¾\)¾¼€Óáß¢íwwÆĞ‰‚ÛÉÿ––ÃON^ˆ¦ÛúV´·Vİ°Tíg±ã)îgs'ÿ3"æsBL=”6]bÖZ~2ĞtïÌÈöÙº±zg?u¨¹L¸‚WúpÈ@ğúŞÍ~ÿ:#ü‹0w¿¸h›”€öÊZ»k{hR,ÃØ©cÃ˜Öøü§	jê^Ü`í5‡upS¸äÑÁİgš5·«cB>6¨¡‚#	Ú¤¬¥©£ûÇYH‹2bñ*¿(Ù[QÌ@¹6†jaànÜ•·º|({ÄŒF·sŸhúªÃšç#ÜòrxMZ8¥õÛr¹6GÖ«õÂ^=Â1˜QşGQ¨(kh³çÑF5»‹¥fpŠçŠ¦“¤§«Ÿ?®ß¤‡4Úó·Ÿ¯ã8ÅÓm¼¸#dV³Ô™ëmbË©Xóp„‚¡B¶©~#~ˆ›»6ü›úâ¹0ˆ4Õ‡]½×[êöóz6ıX]{¹~s£.|³½H
¶XÃ™®¦³ÀÌ¢ß“ÓgLA‘ác°¶4½‰¾›E\Ä¶¥òTm!QÖ,X=œéÀ™¯ı:Sd°ü•?Şñ¨è»øà ™r~üëáŒ¦ëêğMl^wf>c<·´Âi0÷‡è÷ØŠ·†Fƒâ­©9Æk.­ëQƒJZ¼’Íy×Ô Ì×¡læ}[Şã	ü<D]÷nÏ³>!Ò?Ü"˜3\–½Ø,f£áÒ'*OÄ¯S¬ÔÙ‘LÉGç]rÚó³AvÔÈm*o£ÏjrªÆ²ª‹¹·\(&+ú2ıù)É~L‚(Œ@ËŒ]k7œn‚ÀîØ³)®¤ÓßkÚ+•I.8WÎ!úøOXaè¹R¥óÓË¤PÆ”dU&¦<ç|<Ó~|)îPHD’²W­««@I7kîRpc»Š;¼†(5ù%8+šì
»’!]$Ğ5º¼hÍn ¦‚\¾µÙB)oìNÙÉ¥	AK}íS§åÃ++Àıüo/Ë#ÿ¯ßW&âXåM¾/RaçUXäz™BkïUL5ß6@¿|Y/Ò¹ëSø|[&¥¦ìmÓätxíÆñ‡³™ß]MN©şŒ>›ÌÀ÷,k-:=ÿ1OÆ{;H:òùìÇg §lA
5îœ¢’8¯Ò`/"ÙJ=)‹½JLPiO0Ôú6L4)ØN+çÎ™ÿxí™´P†CÇ2Ìb”|yhd
4áæÄ¨`Nû,}xÀ´ÌíéÌ_ Á‘¿Hšvı#}4÷ö(¨Ë~—¾›RF_µ³ŸÕİ,é^¶¸I»DqTËpÄ~Ô¶®*Ìğ4‡	)-?s¡éE™KÜË£E‚à:Ëx}qÒ‚û ?$UÛ‚'C,¯v§õdÑ™™BºÜW°‰"Yàš5+­œDCJ^$ºaí×¾ `OÆ !Ş<«}Wö‚Ut—FsĞ.Ñ+ÃõùŸu€ú\K¢Ët†	ÎhM[ä4rÛ+Ísş¸ÖİÍsOÑ(TO±“O~°(Œ¶&°Á÷-×[‚r»‘òW Bòµs°4^"/‘8NÜÀ	Åò‡~h|N>ç°s¢½—|•–óâ68;‘ıJ…	4Ô¤r¢Ãç%W,’½şƒ>€¯‡Š°´í31uT;»:_Í|;[¾pAÿ¯X
îP’«ÿQÍÀ&6<¬•}Íœ Ù¹ÑŞÙ»;q_|tÙePqv.?¡¨Äl]ìÊÖƒÓù%?J‹5¹´\²£=pN4¹ÎJjŠŸJ\6qa/>¥Ní@rÊšÊFAËæ}ğãä4•İ'Y>²ˆİ‡†’TjËNÇ{Ï}ıœËÅÑ.âßYÃÌDEM"9j¥ |5tñ% é»Â"T˜ø`_×ò„*	Ñ`‡ã’YKÏr"8‘@ˆÛsãS>7rB^E¥ „´=U¥²¿¶(T8Ñ§–Çv©¬{Ê"2Í…·QgÈMı{ÅH…²OŠ~ûP-í¶ªEs-Aàp„MPşµT2WÚœüÅÒÂsMÅ¾ '¨¿ŒFûvÚni2¸ğÂ jEËè|†|¡Ñ¥<÷œæ¢?¨#üŞhş‡#Šº%“-ï
¥ü9¶³ò¿Ôæ¢Åì½ÀÒÜz54v^pûÃ4µã"#IrÆ’™L’úsn†+”¤Ö’©U6§$UÖì1şŠq%©d±İÓ={EÌ0*…e¦¸í…ëäöšÖ—m¶š­ğ’¡§“&š±ì´¢c‹OgL¼!¨S[`êT—˜ZMˆ­É6M0Ff‹°¶§NB“´E¬¥ªué‘7`ÛUÇv¡–ĞWÍ]ªœÜ¢rÅ8Öª_±™uj
Mi­HºÔÂîï@ÙLo@bËk^`âk×ç-í&8‡@{Kv¿‘Í•pÕ„~†=µ@úFH]à¸ô@Û3¬Ğêƒ¼xAà\õAÜ–Ü´àİµÔy§N×Y_O§vÅ4o’ãÈ–KL›ó6;\´Ò",öuÍS·Bòœë)S¿ÚäOŠ\J´ä¶·dŠÇŞğ¨»†=»º°-Ìoå¬F3Ë[Ö¡Õ¨¼¾­øµ5-ØıªÛy\õşf`ìó6z,üÜÆ‰w«zùìÙ6ïéójco*ŞiciŞ%m^
Şm¬#q>›ë‰=NåzŒ›©ÚOÖ·l¬'t(Uk±^dc=ë¨Q;?ÍRRuËs¡jŠ;Ø\©â6:~*"$ñö:9tä(¹xµ*TĞG±_×<63§V‰÷àÔêqn[3ìUœµNî˜ q¡‡fĞ¸e&/E¡/fÒÛĞ8`f],¢ÈëÒ–#ÖÓRĞ™¬{ÕX¥âSus›³À{R]~0œÿÒ+5±ËÔ«W¤ú j'7G‚L%Ï§ÍqGæş5u-œ_’Ã€¶¹Ôï]óuûÜå©»g½šz«•í®Õ.8ÉHGÚİªö¶¿™mowsDßDG×‹Û^é€½\‚pÑ’x»+å
·º¹Eîõ677®‚ùyfû·Dä§ü(WñÇ
*§Ğí×ĞÜØ&é¦v×	ÿ5Ò¾òò‹ÜáHİš¿Ñ7¤˜ãteÀõÛ¹¢SŞÚ¥3SCM¡r{t¾jÚC{÷¯¥‰óâ(×@Ó¬ZÏaWÙ¥€hT†)H=ïBBª8†>!øÊÖñt8$©™ëı+é-BÁC¹i69Ü'{àìO;Op! æòÆËv´Ë—»hG)r—ìÈív¤Öåº‚Ö0 •Wk»­9¬€ÖßÔöÉc+	[RÁ¨xK-2F½Æš¼Y¶ÛÛF¹höò‹VLgú”Úú,H˜>Ÿ0Mv¯ÏW³¥P¥ÑgtrP<l–38Û4œ³rN!?ã¹–Ì{ÁÅ¦£ÂµEŞŠ˜ÎG¸Ê 1Ü„ˆ"‰"Ô·Èù´z·«]áÓ/å+§¯—ó0Ü„AßY»Mšé˜ÊF²şºb3·¸?X[Ö|Ş÷â"LÕ<]÷d¸ATö´g÷œLğ£8ñ’½Îw®T¢|8oÙéİõÀßKÉ'¥J²B¸OY!d,:h»öz¹˜Yj±²¨Ô‡wP/şH÷Èk2íâun¼•eı>t$Î.ğmÅĞh™p;z ¬}¬—úôğ+Kíˆ.¼Ílª†b%+l6Zfå^c MüZ~­ER¶…¢¡2Õ³¢9 ì~á6PÄÃä¶İq«yüñŒ¥ªµ[W¡ùSİëlƒâj,–¿8tœÜÈµmŞ[#ĞÛ¿Á«Éè×"÷có”*H3^ë2KÁ#ï»1U‘Ç 5	(¯ŸA`//ÎÕ;ò5i†2—iwCÓ@,~
õŒõ0
—«’ÑZ´àöo-È°@#pò³»ßIşè‚ÙÔ=Èù?  fe÷«•ÏA•^d#¨B±¼Tş ³T®§ºT¥5Ì ı§Kóå¾ƒ_ –jÏ†æÃl—úÄêñ(‰PÕçÌòãää¹×ó¿:Ÿ“¬.3Ö2btˆãß'‘/Oz•ÀŞq÷Ç/Mi¶ğ9õ€œZ–<ŞŸÂRmjñ>
€“‹ËÑ9í³(vğ°å“—2€œğÉ;úiä¡ÃñÉ>~$2$%ÿuŠşkÓ‹ú[”İ¿…½ÉĞÁü˜[~à[¤ö€b”?ái¸›éPßÆCsQ—ó(/ LGÆçÓÍ3iÂş:Ñ1±•:Qz‘‚ıôÇŸ v".Œ•°@PğÀä»ï&şé˜%;éª~÷å„îªS·‡¾çN¼mÏÛ4^TV¥2ëBe¦FÅZ­ÛS•©dÉüZk+*´²âX,«ÃX,.-Œ…VCö¼R,3¹ºÔn‹İàAvİ6tTàWÃJã”˜İ‡²vÇ#o±-¹\¾µß¼zwùÇïj"+úÉ¶±"IÔÄ>']±IdÉXS³j¡HjÙL­²½¢˜­Š³^$õV¬ÙXµe]É,Iõ;í¹#›CvâY³GV|d)¦+™AÈdÔá<Ñ5;çgSCùĞ³ì&Ë\x‘¥l{™Ïd ¾²s6çzÈ ¾™Ä…†rİ_wì–óŸÏQR£u[Kªö-‹™•g·Xæ5j,o+`«QsuËc®Fõõ-½•7·RÖY%´´9ëÔ‚‹Ërc(B”	‚B®jxRzüF<zx(¨BA!AŒNÈ×ğ:Cu®ßò/ ªšœ…XŞ	>´3?_­çöâÕÛÍh~ægßæçhF>93r´ßFûí	Ûo£ÖƒfÜš°ƒëÕ|eõ~hs´ƒ¢46OÊ°•ÑPy4Ce49›zÖ„µZ°&ÖK`­­åhMôaM´Ó¶
‡rÊ5^¢‚ZéF};êÛ+Õ·j:§\ŠÈÂY¾ò«¨“?Lnn&ßW¥hTEÕ?)¨"zlÉ€*rÃÕ|¹’ŞĞUQşUQOªhT£Z¬Z0é8X–´{LG´ÑºZØ9b§;[ä¢{•d?ça+…«…Í–¶nƒ”õ_"£DâH_ŞÃ8IÂ…àŠàk¾èØáøºÇ‡É½ç°ãŠŸï£ÀòïqÅ|˜wäfÆ×¿Ïß¡¨¡Ò˜‚¸f8¿ïo¬AFóxŸá_›3÷•–®Ï3ù"zÆRù~0[,gŠ	lŠ®xEWrı¼j™$h3_ùîz®˜$HJŞZïÓ4ÓSÛ
íP1i€¾ÙÜ=‚¥î5…OÍz–Í­ºyoa†QëÙÓ6¬v*ZVLul=ééH¡]]‚c-“§Ó#Ñó-Tòû{Êï¬j5F™¨çn/tˆ*C<¶×`À^ílåüJy¹»
Ì­÷Á£íS‘Ús„çôÌLİá–ÏÀYÒòúĞJõ™B’ˆJ/.™+¢BX2¢ü½Eæˆ:íÜ@¢Jy˜y$F­VKshZMÖÏY]?‰Ât–¸)ÏF£ßSz#eÔè}iô.B6$¢¶å6OİÔ:nf-êÉNİ¨şÎ¿Q¥Œ*e"kvÈ*åJ gŞ|—ÄHqLÙ	C/°}{#p!²m†¸+Y
ÙN_Äùd‰–Oæ›ÓÉTUu89ŠÃt6ûÌkZ°ÊµDäº%)-†Ûj»ğz'ëuşJ:Ú¾THzØ¼WˆXSúñ˜ø²DŠdÿ0:È¿|^
½İ&«D_ú–}?À™ù’¤‚ˆøû?ğ‚9™»•O€â;ã}‚KËØîŞÆ÷-{æ¯Ô¶·Õn´ÛQ[Î!˜­ßfHş¼4óÍî©1“Ûë~ÚA"¦ì·p¯ÈJÍ½‚"¤èZi:U-ïá‹Vüp¥Æ8)–Të¼‚@ƒÏRa5xpéyµY~ÉBDuÄŠ2ŸÙ›ª|—ĞÊÕ:/	]²n2œC’AI¦ 98IFĞ©&ÊÈ6P<KxİRO-="Ä,Q®4tœ€Œ¡wRñÇ„ÒeŠæ’”Ú8õEPŒ<¦ñş	ç~½™ú³`-HH…J¾ôµ¾.¯øÕér®¿€8àËh‡ø&´B¢34€yEKŸÛëC°,£n÷¾İÛDLøà#ğO)ø»ÑV!|@bkö)°,-sïpZÀ_¸‹PÍ'=»ñvçF-Ÿ£\Ëéf(¬-ÑÄ¦Y˜™Ø^]`vö
^cWW_yÑeä	D°g»Ó¸ÓN`”à´_TÌ•Ê‰tQ±?Ö÷û]xPW¼¨x ­® ÄŒ C™[Tî 4®¨µ°Wïú‚Í¹…XÅÿ„Š·É$ï‡ãÂ?Ë"¹¤È÷UlˆVŠ4l@	G~STH¹l‡Š
è¡¢úª¨˜ö0ÍÏÓÀ‰2–ÉÍª
wéÄ ¹ÊWô*_iÛì{'¦2Æ.;]š°vÉ?£Üıöäšy®Ï›…Şb&J7D4T‰9~´ÔsªİºØTI–Q¦òE^*Uq…¡6L@)ñšÍ€b­¼Ä_îÁN(1r®Îp•>¹¹ bÎ\‡–eÍÕ3L»x¼+®‚ÍÔS}ŠÓe;Šóõ,°WS_ƒâ›Ä?íÀ>û)É~L‚(Œ@Ğ±ît
–Ğ¼×èDËKœë©í­ÑÒÕ¹6ªàhÓæ{õ°«(ßÓÌE_ÃÑ	Ú–T}|nƒùø„n¾Ö*[Sa’:ş½++¾¸tüıñi«¬€Ğ/‹²Xo{Jïe@`’ 1”Ë2X]NÔ‘şá÷Õ&{‹'ü>:¾¤Ä^ºAğ‚ş¥ÕĞÄu"(Ÿnü Êu&xjÊ@’j?és{qj‹ÛBÒ/±'Y°gËIEBß\
KºY¾È%*![½˜~k–¤jªªÕHJşßF¥{Öµ--5‰ÄÛ¯õ§5›^].dCZ‚ŠA‡^¦ q€Ë¾a¶å>R]Rğ$Wâœ¾¥çwe oNóRl$paş@+šö*—Œ6Ğh1¿Û@%a¨¹ÏDñ‹
‘ò+µ®-…]£2×Ã-·Ñh¦§h4I¯†ur1Äâ)ÜÎ%öªl²"6g"BBƒÏË•ú@•:.fÔw±•ö|NúrÔ|h(]õ2]šP/t#hÔ/£~1«_FğÁ»òëŞ} iİ³	p¥{Şp½W«Ÿµ8ĞëGçõ½»ßƒøM† {_åÄRs¬8:¾Có$w2¾úw”ª$9ÕbÉ”;w-RK)…aKyºñ¸ï&	\æÔ!³ Ùœ³ìn‚bÚÔ5³„¿¬‘l'ş?•™©Ÿ	OËy¡Ïƒ5rªÆNÚÌ€e¹aè©Ì òÙ½İ²‡vGCìér¾\ÍAÇ´< 3÷|v%şK’ü@¥`ô¤Štõ{=ºR¿ÄôÉYS_-PšÍM ZÑæ>dzkåä"Èÿ$#ædšMXƒè‚0—ø«}ö³¼E2÷â R©êE (¤Õ@¡c`ÕlíËCyGµ»ê² âBêÙ‰=VÓ"2Ü”Û{Fy¯¤ĞOºwUæe¦i
—ªVÂ¨Z¸M²Å&­¸¥FäuÃí…ñ3wØPèÿÁdà­XÁóQÀóÀ˜ó¢jN
XºÚ2q,q„ßáRÄ™RÚædÅø §¬?jB0çlç?»{ŠIÑÿébÄƒé3#¦h.ÿã<˜Û (æôEé|ñƒÕ*Ø,ºP7ôÎv¿Ù0°•$¡iâ>ê*ğÂõr5ksˆ8~²ÛÁ AÖTzĞØİ;)&f%×ñ¡hÉ®æ!ÉSÊÇ?Ö&‰sŸ?Ü'Y‚²4d|Š .
‹ß}¸› ò–¬ÎAaï”eÉ?G++B–Ô‰ö!ßRÆ‚q‹‹‹¡\Ê¯“.¨şË}'#ù@å'şW²ßE{'ˆ‡Ø}p‚Sê"’â‚îWAA	ç‰Øö2ÉXª•ÄÌG­¤L]å„ÍaŞ]Ö6ŞK0 xòû
²]ö=è$tÓ%@ğèy'ŸÀŒ‘İ±©XÂ‰	şÿ   ÿÿì]m“ã¶‘ş+“/)_jËæ»¨ä²wãİu’‹ílíØñ‡«+H‚3¼Õˆ
IÍz¯Êÿı ¾	" ‰ÒrªâÌA Ùh<ı‚Fƒğ*À:KRv	¡ˆ¨PÏŸïfÒ³DK„CT‘°wâè©I%Bc–5˜­2Bû(‘Íİ!˜†ä|Ÿ:hG#j×té²EÑ‘"PYöAQŸ··[â¾"õ‚HÚõŠĞ“£˜GÑªñŒ¤4äÒj(Íjõ0I0]íF3¨uµYšC@­x¤Ô¼TÏ(¬Ó “’ĞE ©B¼¤ŒÔP• Ù_¤4Ø¤ #åE
VNÚİìáJ”ùG‰³*ÜiÇ6"Ût)I¹ÇJskO8İ‹ºR'ä_Ä­:¡`ÈÅ"Nq·È~d]¯SZæé†	Ûm•ôˆøG—¶hJµªtm
Ğ:Qæ:6˜o¨bQİ(P¹Š¯?«“e`ßñâ5t,¡PlÎX…qoÒ4‡ôUİüÇ‡mhalq–eæ"ê>ÊÿšD”¤D«µï;k…U¿ÃHm$›K|´F¶…e¤]|¢/®Èg+rjèùdiHí¼®ñšsOI­jqi.0ôÅW±=Ìéç±sM*Féÿ´Ë~¥3=ü Æ¶2µs­û¹s­½¡»Š°t¹ ¤h·±R¾îù¸B-Q’ Ãß Ú¨ªU%Ÿr¤Gqe}ˆ‹ÀX¤LÎo…ğ	n÷H C¸+~€?g[%µâ¡a›FD¹Õğ¨\?‡|÷†ftZˆíõ÷™uW¿?,éß÷Š8õ½—y=yÊ¨ótßkÙ0—Q\2û`;MÅ¥İº6%-ªE@z—86ùZs7H¨,QÎ&ln©Ï´ÌDÎZmRâ°SFf2^ÅJ9•Oã˜Ô>!saWŒã ì>¾Yô=Ü!LWaĞÄ®ç–˜AˆÔ¸ê®s¶QÑ%÷,&EwPÒ è<1':¯ò½gkJk„î˜½ôZ§\¸vß;w¦ré™tW€°Àon_`„“p”äĞÆÆj"Šç_€¯›KAçÅY0€>4‰Ô"x@í€£Ï°lÕéd*3É?ƒX˜å“àh÷3r²ğ©ŠN½OóDU‚Ã*ö?ğ(ft™?Ò‚zø>ÄH}Æq)•²Éö(ßr¦ö(#ŸŞ°×o vÃ^ÿ}şöhcÏ§*Áñ‹°¼ˆ´¯ÄèZª•õÛVm†Ù2Ú¨â…ì¾ )Wy#XJ´®XV (KUëÃpé~ºÈ¿¾Û·4
ZAcŸ|Œ#‚„›‘¶"È3Ò/R4O<®êNÊØÈ8EF¯Â°¡µíWÕfZ)-‡&X*ù°E ²÷¨û4R9á:0\{ÍÀ¦U‚SV¬œ˜:ğ˜qùİ¤é~ôÙ-›.,§ÓNÖÎét#cîô(™'ÊˆÚ0G¡z«•E¡2½×TÜUÒ:òs¼iËDaTä¹¤£d÷¸­ÏáªÈ²C®M²Ñn—â<t¸¼¥iôH6ëhµäñi6NöøvpìJ"b÷¡vßåÅ¾ş˜ó<áİ¯æ°æQ.U†±§†‡‹«<Ïk\ğ<ïü0›¶'t¬Ó%šDC}bñè3'[I¼Jw™7J6Mh{qYƒŠó\·ô€ó‘ŞóAóqÌ0·„a¹}S”‰ñæ	ÉÚ/L˜ÎL)³Ï†è´·N¯Óönıè­˜x­äˆ]LS,y†›âó~ÈkH½/Ğ;Á—LÍ¯b&ùİ:×;9²sKñ:ôqıC¾9Ÿ$ü˜¾ÏÒ_eWy&ƒÈµºĞÀ7å†5CweÀÈVíÙ"b"µ êÏV».IY’ÈÁ©…FsŞ˜h’†±ĞBB£œ Ä€­Ú˜«x>ğäTbÖs˜›Ãyã§G^Ë‹~ä(Ÿ&‰ƒ^3Ù‹n`Õòv=A;ŞåÅ}Òù—"C~±*RP¢_Vè\Vè²ğÎ¥;^Yy:ÏiÉIşuz6¡97ÍÑ¨}Ó1şàsNß—q:zTæˆeXS©i;RBVyÜf*I¢¥Œ9|¥SÑĞê.~K»R:Â)®Œ|àDşŠr`a¼šğ;mÔğüuŸd”zİììê:6²Ï=4B<ßÊª¿F#í"ôË&?<>Â| áh´§(ÉŸ“<‡‘p_hêj2Ú7ïÂC^¤Ï›>}¤œáÚ;­æˆğNS)^ŒƒUİ&jé^ì†bMiÛœeÛè$ÕkEC1Mú¥¢Äâ›0&}QöMººÓv¿†•uÊX™Ò²xB/‰È\ÉÕGoéëå€ô"%ÒHm†ö[ÕH4l—Ùä­2Ù€ŠBØ`ŒBßİ¾Uª³„)P2â·t: ‚‡<›ÏËGé-*ê}¬ oAÓb›µ7`á;¢)¿$ÅÓ»&-ƒ^Ú‹ÖAU˜%{jÏä¦ãÓ¨Œ9Ë6zXr'½¯g6Ó{/óî§SF¾¥şŸÿxY–DP«5z¢¨ùM™¦E´TjvBJõ&'ä§K†ÎsK›=^ &æ@&Û‰ÆwôÛ8
¹AÔ‰H8ı¡½^­mV½¸[uù¾À³·øçæİÊ/Ş½¸ü	ç¡i¿BçC¨O+BMË®ıØáÊ¥ÇeÛøªVøDØK¯BÅ"|³%â#z˜ìWÚ ;¾]f~LÂÈ×èAsêÉ¶cµ­oñ£ÉãS‘WVÁÚºÊ?n²“¿ÒOv/ˆ1ë Üp¨ßï£„rBİ ‡Û˜ùpãş›%Qåò)è<9~ú}ôœì:_ğßÆ>¼ê¢ Ş?yÕdõ)‹Y>cv™Ÿÿ’€MÅ}:0–²¦%?­º9 ë»©ä„(±=Ã¦äpÊ’?-Ë
MßçLş{”T³ØÈ†dN¦š‘,Õè¦-™ê~dºÀ‘şê1¹¦Âôå¨pœU'PC…,/ -hÆbÅù™T¶ä¶å¯'-''1b@ÔÅ7ÈY=)&¬vb-‰¬‚Ñ.%ïpB;\[–ˆR£(ã’
¶(#FédMš¦!Dõ]uä¡Îä¤ÈŠ¡g˜‘´Õ$yJ!` ±ëSº"<“oÅÀ_s”¤ANC31V…tà~”¥tit¨LEKì¤CÉeæ?=±‹ä:WRí»0£8Rb‹J¯¶µo®­Èã< Ô'âràÈ[IÄaÇ†!Íˆï‘+({<ÊˆA(w'S`IŠ¢àAkšİ0Ñm´Ö´9§`"Ö¦á®b(=?¤ÌÀdaôÃĞ…ŞÈ’ñ÷*#NGjhß3>Ç}\´pÅGGšß¾uÇÕ}¨N|Ç¨ÚÒ©¦øÃû7_—ÛÒ0Ã$İ	-£äu°Eä„ƒhQ ±
ŒÈ€Sä8ruƒÛ.Ö«êË~WÅ¡Tì¾ 	!%h´KJÁ#AÉèâ ËŠŞıÌÇI%n!¹«M0qœ
{½òLÖ^NqHÆpêÒĞñ¥ À”å‰ü5õËÓ0BcRnÛÖQd}6«mfâ>Qå„
{ájmÒûµ³òïÍû¹aıq¿3•IBú»¹—ĞŒŒÅ’%¹¡.è(P·€¹ WtjåS¥šÏ$º-;ù$Æ;¬ŒKxÇP'öŒU g—	º`èXÙ²°¶€“Jèµã%¡ÃvÄ`v÷3Şt,k—ŞN~íN^G¶­Öiö5oÀ¾ıõ0Å…¾€ØÖ©%Š¶Îw™âŸCÂÉ¼áêL^'³Xå‰"±0á;¾â°äDñ‰‘ËêóıJÑR»~›¬Ç¹­ßæt§½†Ï0šv/Ü>‹îòä4e§„ÊñÔ„NÏu÷¥Åúšµ ğ–ùª\ú¡u’ç;dı6FÓQàe<v•*á’áÀ*…M­Nxë~gÜ¿]ë.F*¬ğ™1N} éXw{ZO'Æœ.;‚,ymAÙúG!Êbé:ó^ŠÈí>¶Â(ƒ'Bvı È¹#R£¥8$zQì¦C9d¹@âmAâõ¡ÔíaÍt$0­‘i$hÎ\?L÷€ÈaÃ‚ÂëâX˜ıVÈ%+şíN(¬zå0tQ|
¢>ıt•ÚûÒ"Í«‘ägšS”B!>óÍ¡«kÙ«»]´1R`Ø¦&Û 9zı¶Áù¼„ÅY43ô®ÊèV~„fç\š
5‹˜¿àÔ26b]2êÊ¥ ÿ\q~œo£$õæôT¹Z¤9ñ~Â¥İJ¸	‰œ¨e§ç< ¤dîpSFíäx ¡cäî+çNƒ<-r5,ÊUÂ•Dh .Ä57Gğ
²ª”›‘[„üYU
×%«ê‹ËªâAÌ©§ŠFğu0ÃêHß’gE+›«+`Ğ”÷›æ¸$¾/)îSã’IëİRÏºVNS’úK\9³0 –eÊ»Loå<J¯f£oª)-¹D–È ã%‘Ób¸šd¹.Ø»Èò"Ë:eù¤ŒºQn*½ÏÍàrÍw©\ úÔJÌŸ	Y¡Üµ¤Òì¯ø^Ä¯Ÿaö‰q¿¢9#¤‹5i30 òèO+:Ú’:N+©ë½¦Úû•ƒŞÙ¿±Èşuàl¿h°ÿêË
Ì
œsÁè4•€v÷%Nioë¼‚6²;Û÷=—óª¤OÉÛ4<<C¹Û¹Ğè`½¶L—óâÊğïŸÒ"U{¯…İZ/¶`j+”4ˆ)WsİWöe÷í ‰î¤•RÓŒy+z#‰TÙƒ,YW-õdÈD5ä¢p‘ºÚªíş|ÓÏ´Ì9în®æÿ°ëßT|Ÿeàó÷I^ü{¿ç×w{r˜
Œ/¼&Á·×¶ª_WW©¾‰ıæ×Õíô7¢>È#Zzp`eêz#†‚w„å1—SØ±a®ìŠİQKÿÇo¸$q"t+‡şšˆsÜPEıÚvyŠ‰D ¾–Í ]Â†P°ï3ş	1Å_ÓP½Ï‡?şñdBÙ¡¶†{çï¯Á©ş0 qõwiİCk‹æ°ÓªşÒşw’óÇûN9¼K^+\`œèZkö1ÖÚY¦–ßXølÑïaö ‹"Ù=R-š‰P†zš¹ˆ`{È˜‘·VAï1î5 áÇÇ‰`´	ÓmÚï7Ê!úêhÃÕ¶xJ2Î¦qzÈŠ'¾¶èp—'ÅgêÓ,Å¼¦|b••p1Büœ!]ıYÑi“ôGSdˆ Åø©ÛwªöÄ/Õe÷%yí/«CËWGœå+&Î[©#NîÂQZ‡
hRkßõ„Z«QÇMrG¦×JfÚ´bÇf·Ÿss¥ÂFc«ë¥€uÁ[~¯„5®ÍØ©õÜ€—6G¶$ğÜ
¼a½ª§XjcêÕ€]P8ËıÁÚyŸ”dÈÖªÑÃ5|/Œğ¸x8t±¢Û³W‚]âßÛ˜ÇVŞV2„º7CÌ/ŒÏ*¾¬hµJ Åş¾&üöÊ‚qé¾pvAşy"¿KQ?ºGoYÔğx+êxM”éJ"-zvz–ı1éH¢,(óD‡/Áœ„ä Ôğ Ås==I¤Âs5£ĞÂù_˜Å€Y˜Å`R´Ø‹=Ás&öWVEº+@Xä›ó†#pT£¶i;Ñš’5zÌáÃ%™_ß=ŞT»v¢éÕërï¦2ßMgd°ªÏe»şÕÄôÇl÷i{È=^|ùÏÿ(SîşØæô¼ÙİÚ¥îØRÆ“Ş±Õj#ˆã@–>î¬2õ=.yolc€™pvœ[Ş7›ëJëäuá™•Hü¦gumÓİcIøçøwvF*Ï¯mˆÄÀˆ–Ci~A<¢%[•TW§ÃhGj¾õ¥s©¸×¥ ½8KŸß»?‘fI0Ì’|–“õéİ;Ø[pb©`ag¸ïÒ,H¢ë’ò$•<&ôHË)bÑ£Œ©Ñ[Ú•1Adô©ƒ
ñ9İF“YŒúâ®ŠÁO:£QÁ 0Üw/³R¸0ÕˆìwjĞ[Ë$8‡kÃ­ÙÕœ«”¾â‹]ÍV×í øª²BÈ|"'|1~«êxw=-J9Ï[?nÚ’âZI2{ñ±ã1Ô}\Rb=Ç!ÙÀíDÔÆÚµÃ¢µÌ9™ş5‹ÄO½0¸xcªäú+Ïl8»KÒY¢>pïıÚ qızA_år±Ã \ÛÚÓm%¤zIÄc÷C±i²ò3pLw~¦IÈYÌ™³™3¬ÖÍTÌxiœ¸èÚáÈ
_÷d‚kåzDŒg^Á3T²×ÅkÏµ)1‹‘¨7%à]Å•ÃæùY3O½Õ)ßü)ÍŠÍ}«E‘[æC„?a–ì©çˆ«‚;{üßŞ³¶@STÿ2´ßXò÷,»ŒåHäŞ"şƒÈ"nÏ»Xõ=ÏİCaçFÌ²#Säµ£°
¥—,ô!ÖBï•ÂŞPZú©d¯SÒ,*D‰êªd•ìHáì=¿1+5!Dt m-GÁo[Jãx3BiB¦çrŠFp”z±Mß·0Ì>ï\ç-ÏÁ#d–¬l¿÷¨£C6X³â9y†›âó¾¯¯J}˜£ê=	>ğ¿ÿçî#ì—É¨%/½'QŠşŞmAç¸~´Á#²èDS¸ƒ›İá9€ıÒu›8ÉòaÍŒØÉhP~m]M•º]<¡±7Ÿõ-†ã&w*÷"$8ğõh~åÛ¯¿ tÓ¡~|È¶T
ét3mœ8ÙByDÒÎ}Ë«™Ê,}I"ö4¾ÀİÒ¦AÚr+(ú—Â¡<úÚÓiZÑGTVCÎ]A¬CÎš¯˜ûC”ˆ”]%F·]Ç7€ËYA:ú_`ú>MdkÎº¾Ğ÷sÖœ¥ğ¦Êf“ß_Çnè€	Ã¿{ŞŸå÷@­W¾5aôiW †>€*h*±P³1Æ)®ášfäO ä¼G¿K–R4càG«)ã‹V?&F_¯ŒĞ8aô"—=0Ö®c;S èT
¤j1¿:!„Œí©\˜D‚£(v=Îšt"°Â”C°v#Š¦à²C³ØNC×r€cºS!æÂ”*€… Òğ•ëO™ŒR9N’ÛZ­}à‡S¡Aœú[TëDk.6—[ˆn2‰‡ÛHsè·ñÉD¢E­Ÿ&]Áî›È(È¥;mş-òçJ¿„Æzyc~áf2Æãíp†Æ1óš…¸ÎÕ.yQ±tûYùêil{Í«g:µˆß–á¦€–aî7æ$ 5™èƒÇ•_©B<jïF=¶Ö—îË"‰8‘TƒGB¯µ%¡·¯•š“PÖ_Fòy¼õ‘¹ãm[rÂlÿ
$iÙ/]kå’_»ü3-=Ì‘Ş]4ª˜Ò„"ú\©CÈeLU3X”cˆ<u XÄ¬ªãÃ²÷¶µÛ{øç$Ğ=¬Ó‹‘ó‰bsÂÓ#ÿJ)½Zü8ÛqìÄqšµHŠô´ø‡™kt”¡ñª¥A%üµÁF2çh0Å!±	.xÓ.0şY ñ ñL>ş™0Ö{ÊÍÂf[F3Ê²íU®ŒšLD¾ŠI<½RPÕ6;[‹£¿"Ô¿PÏ³Tu‹"’QDU»ñÆû8—†Ò«oÊ½=åPÕlƒ/PÅHê=ÛFÕŠ(r‚¢»h‰5:ãMĞ‚¾¬©:	WIo$J² ŞÉöY-	×º˜fïñbPeŠP“ÿ· :óï2äˆ2Ê®ËÒ’G.œŒ«>‡ Î¾êÜ*ü#~²´>M$vª´>b$ôRsîè*½²)¹bİfBÇ.ëùoØLÊxËf&T¬ÇnzºúxwBß_še|ı«­sg9OOÑÍoÏ¶±¡Z†wÎn¯w^İÇÌô®Ù{ªåIA¡â6Z·yç?Š‰×F¶³x<¥üğà¦Ğ
TÍ¡nØªğ“Ì•6G©9œ³8JKœ¼ùYÜ«^—˜÷<1ò$Ş(4VÊÄ¡±918Ã#cËŠV¶˜µ “§=G½JúôæTêb,šyFëøÒšù´h§ÚÔ»çô“¿ÃÏŸÒŒz9N^ÓYçd eeq,Û‹bÏâ¬L	ŞÂ-D+wÖ?r;XÇœõpÈaÕë'™ªõ4?…s­Ì¬¸†j¦°¯ƒ*Û#îúOİÇÇÂ[UÃ×íëœı)Õq5•Bn!Io?ÎT¿ÜP0wH¯É­ù0Wé~[Ã)è{®ì¤|SJâ_²ô°§Ş°ÆMêë:¡: 2HôâÀ‘ÏY*«¥iù1-~H£$NdáÑ÷Í8XóV
¢‘ &i×
—#\m…“*	Şl=ås?«Lc‹j^4²G¯-M­3xTÇ¡_ß=Vœ_“P7w‡–Ğ­Šã‡]ã6Çû;ÌX}ÅÙ<²‡Go<Ó¬ª]¢O«„œVs²áBÚîÕÚ-÷jEJª1(ËNyîÿ£ØÓå»2êé¤ƒ¿dh
¦Ñô1™˜÷|NÏSh©»ĞàeÔs¥ÛÇ8]„„è‰k‚Xƒ º‰§Æœ ƒÈ6ş-úcé*õ5É2­¾E›®F,}#²¼0Ô]QiË[ËëÕHåÚÂØvnC(oNDCtªbsMœs‰Íİ¨] 6×¹ÊÇ½Jš; °Òı+Ö•óµ{AŸÌ¼‚z£@-ÎØ}$e1K^pædıiİo‰³ôùÍ‘ÿ_uVPoòÄ<¤Îİh÷øš·‰w¥—}\Ô^¶¯v¨-ËR1xÛ™¦mª£u&@ÅÄqFRÅp^«b³‡ß ¼Á@4mäºñqU±½Û,%Ó'¡Û]—ªëÙé+1ê@QƒİŸëÈD“I°~ë\‹HE|Õ*¡¿E¯HİÑKîï¾*’üë¶rìŸë×hÈ_¶l/¤ƒEÿ.«Ww¿û	iĞŸ‹d›äÕ"åÀ!+»ÃQoŒ·×úUš¯ÛEC?š44ï»¦q#gˆÿ»ÜS@û$üÓ^:Hínèûj WL =œ…ÉÛö»ü]¡üæw¿ÿı­EBşğ‹şRh}Ô¼¥³ÿüÖûëoŒ(q«ê©&¸Uµl(SÔËŠ<èØî¢^bE•Ëyo-<¡toC0s2ŞÉl£~Õä¡ÖG>‰Ú•GÖ}íâgØñÖ}KÉ¹ì ,yÈ«ÂC[b7 ÖàÅïÏç2ÌY~Y­FåïM®e‡ã7ÒN|)ş}©ÓÙ³FÌ°ŠA†¬ŒµmÇ®îüãY.ÙK¯¢KH¹„òX8’úÑÊYÛ”r‹q³7£®ô€0ÖMe¢íÀ´]Ûg]9q!™¼i‘˜W…öªí¿[­¿{ûn±W{u±W/b¯ò˜ª2¶‚aEaèÎØ˜åB½ôÚ¹+µQúêÔÉ;ó;ß[}«»¤şYÔÉôçúÙswOL‹ù/!ÔÃ$¸ .-“<2Ã3£à$[láÂ²¦%•CtÊBcµ‚¡aÒk”¢ÛKÕh&uŠèxéçr4öU~3×£şÖszz[¬ú¶ÎŒa7û=Ùì’g@ëØÜ"ò´,¿),¶0y{ÍÒÃ.âi¢ÿ`¢}8µ}š'¸‡¿ípv×Ÿî¾ù&<äEúÌÚI¦Ê–Îíäv?²?l›XO{JîÆc'ÿÙî£Ö;ñ½—»{ïÔ=mú¨Ò)÷ó2šõŠŞ¡ë¦Óæõêe´¶zì¬gPÄpÏF%=z¯Y²u8Ã.V×EMÃtáF××ô°JÓ@] Ó4Ë°2©†US5õÛ	0×Nİ¥-¡ë¼Á'·¬WLM6çÃ»Æ1Ù„òxË"vñ†»œb@¸Çë!ï›İ%ÎûŞ´5Ë;Š¢E(>íş	÷‡â)Í’ÿªLwàÅ~ì¸”ôŒ29zĞº|¤-5»¶º£ô$L›<ÈÒOˆC¬Çû-(â4ë›üMŞ÷&ÄŞ%d'†o>E=@²g{¥ÕÌ6r{“p·7*iâvŠX¸İwyÜş˜ó´oEK,Wb-†-E]Èş¬å_èfQ½D.ÑmèMkçóû{!êªÅÆıÊ¥~f¼2jTIÔ¸%PÉÑx»ZxÆ6Ã7o¤„¯1ÕÄ¢ìyl¥GîCæSI52„‡~—l!M2€¼û¦Nï¥¬DmÛN`Ä_uä”ŒŸ÷ÛD²¥\Û[A7€RC«-kĞa¯ÖÚ¶‚×jq{¬™S†=Æ8·YÑ±Ö6ÍsänÂ'~Ì)QÔº|hyÇÕØ«äD1«KÊQO$#„UËÑÊ×®µºçß¸®@İ•31ô$ÿZÔò«uÅìi+ZáJn ’%—'®h˜Õj“¸²äúd™yE(íÓ¼¸¯¯KRà™›Ğ€”¤ê×1í°Øl“4YØO´±XmÂ¤øÌ~à˜u˜vEöy“ä)³sÌ”M˜F”epô©IÎÅ&$=iâï"N4ñ¯ÿ|2Ò<]gR¾·WB'ô"–Dá±­RfÅüïF¯Ò_}8ƒh¦T¨uw£ÒOOŸ\×
‘ÓÄáé6s#åEÖ‡UËÃ/ß§ï^ğ]PÌÜŠt¢Ñ†UæfF¸tŒépº™!“¢·I‚,*K½Û­œç˜¡ô"g]\65˜sH€’ğ#Ì µ²Ã5”•{£ˆ@©ñÚr,'â¬¿Î&‰H ûáPˆ8ú1V»qh€Éòc€š~‹ŸG’Ç§B¤1Éè­ÓÔLØOOğY’3±¿^­V±Êiú‚IjVÆ:°ÃJÍ»()ê;RäkËYÆ€_÷iV@äÑà”cL–=p@Ú¾yù9Ç7hÊŠô‚•8ãtc¤|Ÿ†ÔM">RÛ	"Äjh)oÒ‘½öÁ[y+s²B#–ÑOéãã23ÓäÚk-îÉ2\ò°M?ı€ì!åJ­şÎC‘"j—o{ûjèï Ûë84İÉË˜æÿJ“İ·Ÿ+|\¦LÛ™¼ O±ê|I?JRäÇÆjí9öTŠŠtÿ>•5ë\àqµ|©î8‘4Cl¢%i*Z‘Éãôš56\ÅïL6{	aşgº=Èª+=ÛŸ,3o¾K3¡Z×±áÅĞ2&¯q¬ìJ÷I()½‘Ù®ÍySÆÀì$»	T@/òíh5[zz™Ğr™¾m8F4YN:,9I±®İXÑ‚~ŸÁ¿&9ÒxŸÿšD”¤	Äg:Îd_§§£>À`.iÀÂ5ô­Ğœ¼ª~Şãl$Ô;é}f/ò€5öª9kCÈQ–-õ¸c›¡íD*íŠ)…ëš¾,E&ş}$¥Æ²-¤/ÉşàC35JhÀX‘vß=Í!©Ä]#\PQß'»0Â'T%±9X­Õt×°ZZ•Kòzc­C•ÙÏ»gé ”cAÃY“µye}NmxQøÖä5U‡p²¹dL®¨ŠiÔšó§Ÿ¾—£&,®<Oìş˜"ôÈ"Ùø áÄ&ûî§ñYŒñWÆ*²¦OT™8ÁĞôüµ1™+Õ*š@ˆçÄønÊPQhbW${ é¯Ø¡ sf2´T¢ò&İÒK$â8n:öd»"ä[~|,\”ÛšöRŞC˜•Ÿ¬~¥íZéYŠ“™t~AgMò2Éö`5|AÛµŞ­8PrmÛp¸sMA×6‚TÏ´Ò`¡üSíá¼+·×U~­åñ€:­­:•´İêä;uáêçšØŞt¯6É›ç 5ß›?™,ÄÊÈ8fXp—¬S/úÉseÉqôàn#¥é`xDÔéÀª_:W
Ï¡ÍiBÓÍ9Ñ#4M†Chˆá÷|ÙäÕ¿sœİ?ÒáÀIs­òÖ!®="—FáñöÓ/Ó7Ö¥å¼ÃTŞsÊ§ì9×Bé¤6éY-ù(iÖËjYVËU¬–NÖİü   ÿÿ  eŠ xœì][“Û¸±~Ï¯PòòV©](QÚ=v¯›=59vy¼É£
$Á1×”¨PÔŒœıï ï$@!J3­‡±L¡Ñ Ñht ¿ExçFo¾ İû¯œ­·»	îŞİã]ôÊ¼`7úÏFÉg´|Ï"‘\o‡ü‘·‹Fv°;DáÑ‚pôb4ùæ®gÆÌpf?ı¡Jús÷oïD!©ÃÛ#B¾Ï¿×)îÏ…9„mÏ~ÙíÑ-=ä{ÿÆÎ[!")°¬ ğ1Úğ7ï©ì?„gu¸¾à
óüóƒiõÏÒšãŸS9~ÙEóÙ³œÅ¸Àî§ŒÛïü†d5nY•Ï>#QƒªÒ'B<„^„c)
_à]iéó:Ó¤ş¢¸¡ËÃlûèp}¾ÙØBy‹]tô£×ôwç“w÷%:NÑ X3Ç]¸öÕÕ$–¯,Â>Ä÷‹=Ù„ìÑO2d;üP¥Ò«”51I[9‚õ¥šôSk£^–ƒÏ†ZËL
nGéŸMtÆÜáÏ_ğë™E.^®'Ó•S×kÒ oww×=ò¸6a’´[’úçcD$/¨],Æ³&õV£ZC‹e1ó¶·—ÍÜ›ÖÄŒîu©âjmš¦ËñÎ×ı§õÓ;Ç‹şt§©‹ÌÉÚš“E½‹R¶LQ¶ñj6-D5$+£Î&¬Hú^½´1Ú»Oe•R 9Qã¿íƒ0Â¤;ïIs¨şëQ|¼`ËZ¬…Î^"@"NÜq{&òó*´ÛRıS"æDšöŒ+Í£ôÛ›ì·¾'Í€"\Ä´ŠÛ¯2«ä)úpú~%|vH—ß·Dríù
ü¾ËQË‹óûbE¼	lÄ´Le_Zæqt(á’qoPÈjÑA4³"DßæZ3›¡u?ïJ‹,MĞÇ<øø%ˆ=“À˜–3Cn]_c¦¬wöôkM÷ã´3’ßõk<cDäf¬ûÖs-•_„v³–©h·4Ao»ûÏÁİY=Šî,–æÒœN„æ¾”`½VL´Ó°~;_¡"anŸ5¤2µ8¿Ë˜4j¹NéŸ@É¬9FA¢İÁ×è-Ù‹©éšfÁªV‘êì¹| ›ıšĞ]¼“Í„ìßmõáÖş8šöo‹ùz²6MN@ÄK-'ßWö#9äÎ­j¤6nb¢KR©¸¬Ô¶-.ª5Zÿ?·{ı=ŞèÊ­]{ºàÄhi×GwÂDk% 'ˆÖ¥Jvï!Få{İ«:“ZEc‹²º/èğTñŒÕ3½¿yõófR!É¢o7Ê–u#êİ1.uZë,a´Ü.<wŒû¾¾jšLÆt‚¦sƒc–›§ŒÖÙğ„ãÄçÒ°Û(Ø|_V­Ü‰¹^sqÒğ<¹ÀG›¥(ÛöûX×²¾@‹‰k#,Ñe¨Ø¹,Q²uñîv(:†Xto‰0Ù½Lëú»×Áw¯…íÃ?ÿ¨+9Ç¦»\&à‚S¦‚dïƒğ¸ÕdTl{9_q¼°)ƒÛ
úìIa=AˆÉÒÅ³	'Áô-æÌb2ıZ‹1JÑ¶'…ô‡Æ#Ò‚œß¹}†ÖÚ¼UòÒ=§Ô^#mğ	g5wL1~jPtîÅ&Ô*l=3gºšOŒ‰Ãq®n›–!ş›w õ~ÿ›ç8XSo-Ü…½^¸°eSuçİ²ÅŸOø_G|Ğ„ÅF®µœçà]=ş"ÊêøÁîn„öû0¸ÇÎÆú~®È‘^Xu¡AµôÑÒ¸HíTCAWj&m*4¹7uşuï ôèI;]Ç	ğjfO9‰_WH<^³(Í-"ŠM×³.%šñBVé@ÿF¬ïf{$F³âäŒ®uÌ[ cÔ	yÀó©=7ñàm¹Š[>â]Ì9w"1òû•59S‹ÅtµB3ÎBsçp.ÀVİ^Â9œÛˆè|¶Æi
ŸÏg“µip3¥É¹ÛäfM£,ˆPeÆÅ~ÑÆËpİx»¯ñfG“õšØ&²9+!Ûß5˜.öûĞ†K¼+£1õB·rõÍ™,•Õ`´ˆºi½af®ÜÙj	›¸«ßÄ¼Tš)ü„v_5Ù«Õdi˜“ÜcéHZºñ‘£Å¦SH„kòÄâß³ÿıá‡¡ç`­*˜®bŞ²Æ(;sJD¿ËvÅ€f2é=I/6_Î	¬”ìe'r<Y>ş„{¬	¡eaO°ËßãF9óæCÙ…‚CÉÎEĞx »&C›ş‹>Çüâ&ıŸZæzbò®%€t×%¥»~İÑ¢.h…aÌÖ6ç¬2 c¬/ÆÓkİ²3<1LË‚-ËµoY’‹9½È×f}×ZÍ8ñ8¥]µğİ6øÍ»%DGMŞĞ£µ‹-ø§ÈºA!‹ÅÑÊ‚ ºöX­ïW°È*}×óuí–d®mŞuÙùô3¨ôUï´ínÖ;6æš†ŞX3ÇâàDakÁ¼
„µ·Óxgá¬œùbÎÙæˆ/¹Ø“:Ê·xîèÿ
jÕ™Êè=ô»”xS®xz•¥zöÖËr¯é8¼²¶ —ÑRgÖÇF$9‡ğùó¦¥¯)â]Z—^?Y½ô*Şüo@lÔ
MÛ Ûš!Û\.!-pŠºhJ¼!"¬Ó­Y™Ó™53>‡KòxOõé|nê×¨*O—«õ„/ U¹.U)¿ŒAÓºâN×š#º¼
Côı†øUÿ‘_–ıbEE^>û¡æúÔ+(.úó9GÿÀ´´1	”§ÕÉ8K*Zw¡E"=©:]	†7OUOjÁY3Ã„
À¥Â.}¬{¯‘ıõ.;‡%X4åå…ksÎuw d\‡²'oÿÀ8Ôhæz‰Íl”3nPÃ¼Ğ º˜±×•\íÁĞ:~Ñ©ÕDããŒªnÅÇ†iZ<œ(>(ş ŠÿOäû{´Çº\Ş©e-œç=·ÿd(NÊ¸AãóBƒh|Æ^—Æ÷Î 4¾ıİ5»È»İ#M7s.w¾t\N¨¢æz£æÈ"5 ;J4õİ?±õ,ëµ·:~¾ù`ı†IÙÿÔÔÆ%/OÒ¦Ñ©¨†Äpq†øpô)Œdw¤ÑKİõàEö—Q©C9YR0QËµ;›Í¦öúÇZúÉÙà:…¼‚,?ãàc@š“Êô¬2léÇ"ÿZÿ)áoÏækg¹ZªóïÀ·œUqt¹cWáğœ)Î8‘fœeiù3'şÛd•šº4SK®¨Û t°E×$ìGw8Øìé7şNÙ6>6_Ğá7"õÀ}Ê/ûï Ør8ØÈ×m	³vf÷,$=Ğë}hyo©,T¢lõš
Ó.W)ÏFâZÂwÙp›[>`­¿‡ÖíuĞñh-ÄáÓ¢Ë¤¤ö[xBãúVDë4'"(Éè0c‡VûbÙ¤u¤ÕU]Ş—¢s¦<…;
Û]¨"•NÏ©È§7‡É]a{¶2,9‡eOD¸Å¾›ˆÑÑGBÆrJvËò,ÕÙI¸F…îÔêUú,S¬âpvÈ.'ãÆÏ±áo{4F³!J¸\ËZZ/›4 Ã€ö;˜éŒàÇKG”!×©Ì†™ó›ã¶;[{‰
‡4ûô2U‹~Ò_‹:ÒëèÕqU—_.íEGÇ(x<ìü€¸ñı«Ş’¨ªéBsÉƒÛ‰O¤:HšBoI©_å“¿NÔ¡îrˆ©Ô…wâW¢£ãåew¤iÂ¢û/ÁÓŞŸºî”cïvßÈhn¶èßMŒel.C34â"”S\ËqÏ„'…,¸1œ+¹ÙÎsKDßüëˆxCo4¹Ç›`Cæ!„Xï5S	_¤=:]£XáDœ3—BPàyQKÿø¢¤´eÖèÇÔáÑWı®»!äİÙõĞj×•OÈ¤³Sò]¹œ¹* œV·3µ™+ œU.gHçuÒÂÌVé–ò|W‰˜”¬€
¡À8(ÅHÚl†Je­¦äZVì–—§3@j:æM…
fcÎ|P Ÿ¹Ó¢Ó!ªz©ò´iİ¾—çJkñÒi•E0+Ú#UmS¡µ†VıWôÍ’ûP²è±êème½xgh=3]lsp9˜´	ßáğåˆ¾Ïá W¦’¾ÙL^5J
Ö~ÉîDPŠ½AÅğ.z$ü"*>	¿Y¯DÄÿ2ı’Tëêq¢˜­ óTóêÔ)mı?–zI\_ªõQÅ•ªoèÕ­ŠC/ö'ëõ–òYaÖÉ²…Y€|øäİ}‰ÈÊ…¾ãp=ëÃ.ÓÆâÁö-ªÍ°Fd&¹x;¢lÒ	KaÇ“(å°3Oíå,´Û°–ˆßÔRhO_ïÑÆ9ÎÑ¡Ù¢©bÃ¿u>7Üã|ãİÀ¿hÀÅÅTŒ¸¸YCŞ$ÇesåMfa~Iî3K³Mr‡Yš{’›ËÊL”ÜWfóRPŞ¨”/ÎRÉ¢Ú…9+ 1«;ìlÖ‚Â|uñä‚Vº–}\AÉÔ¶r%]SÛÄ•TNmÿVÑ<bcœ+ Ùb\ÒCJs\RGÊõ¸ •*£BÆ³ ÊûZùÜqé…b÷ƒxÅ×ÑGôòI~>Gßå{$Ù/3±³BO&‡ğ÷¯ıµ!Úî°û±ïQÔPÆÇ®8®NìÄæ‹¥ƒ<loğ÷§]ş‘†âø?&ŸßÀtØØÁ‘cu¢Èd¹;Pa€Ä­Ùâ;DÏ´íù1ı6Ìó7ÜAÎ8¾GŒ´Âs½†ñ  Ñ3»iÈéå{uJ!0É¹i± ¸#6ªäŠá{œw1äÄü—˜JbÜÉLìI°¥¹Ì.ˆ6x»¾‹¹è«¸;O4ê:p“ëKÄè9~±˜ï£ı;dEÚ8d%8Ó=†|J†˜hó'ŒÁîe6êäÑ&dÏ¤‚/Í03Å›°ô°¡Š×ô¯S©Ãbå*‰
B”˜‹Ê9–9IQ8ØEÄYŞ4S2÷7Ö½¿£!EgÃ±E¥·IÒ (Y'š…ñû~Å3‰X“ÂŞ-YlÓ87“¢ÇJ_IÑn&6ûä’Nñ†ğd¥Ü|òl·±yHŠÌĞà¡ÁXm6½fûu‘Åš7ê!¶‰¹¥\e­ß¢o¼÷Åä‡Òlú·á÷¤C6ür¥‹}é÷Íı‡»*øø×ñ†,«{´vè±MÒ¦añ`‹EŒ\¼6ÄÀÈgsÆ¥z¹Ù#ÏÉÈÇ‰	°Ò>{Ù&^ÇÚÁv{ÜyÑwV[Òƒmrù[fì~q~ıå/ö‘ŒÏ–ëåä¥¢bI>]Ã^f«™ZX™y{:ƒÜM~ó¾}<"Ïpã>K—ôù#b‚¿¢ó–ºû=v>9n-©öQ¹ÒXÖ79ÓÒv®c#dŞj©ª\*M­í2°•ùÚ÷>­Øú²%nQŠ‰P¬{Ê±¡Z=<@dwô:6qáé|Úk¥†6~;ò¬×ÒÂ—öMRvbtìÛŒ^‚WGÒ­˜ÍÌ“ZAèy¥FX™IJ(U{6ŒÙd5È÷ÜtyZÏM—ò¼&óÓxMæòz½^Ÿ¦×ëµ¯Å‰¼ò¼V'òZ)ğZ6«½¯ÓôĞ+ğ:±—ò}hÖ…f[ÆëMç…ª­ö.ËC§Õ®D/ÍËXÄÊXÉp
|§+B*¯S]İ¡”\‚SÅ¥èl	jõ´i/(«y¢üŸ°';±)’«ôä©=(×s]H•Õ‹ÓÊ¯¨µdtSJ(­'.|JëŞ‰6p©àë'®çFëìÍG´›-,W £]•Aº%'µ‚Ç%‰“6×U;¤ÈA@D˜&'Êr\v‹Cz`ş¤ }à™ãÈmu›Nïà½ˆOQò6òòÓONÒÉ·Å¾,I^éÎŒäù½‡ŠYûóXDË"xuÚÒãZÇC5Bò¬‘êÑtaXå˜>n¤½óÜ*}ÔL“ÄìÂê³F*oç{;\!‹ŠèğÖÂKKØ6òÛ¾_„†¾,#\h@OT²‚i*ZÁ€4ÍCÉ¹Ô¼ğ2ıä–âS%;"1©‹é6§šÑLgœÑE!º(¶p>—G5¬=\½rõ¶‚b
9E´u4SÈıAD_Ä5…•G’úÖŸJªwX*¢,!Âê3ymÿ²Ä˜Õ²7PÀŠ”ìrÜšØ¶mpŞåØ+\ÏsT°zÊ¢tQ…£pJihY½,Íü@ú¨ı*Ö3í'wË§Sd•ŒmªÊÕA§ÌùtfÙç¤îÀĞµ$	ÿŒ1L’¸OŠV’†{fà'I¦îÉÃ P£9(ªÏãøu•ÒÑPÅÓ‡	ëZLAœ4VX5(i¬·jRª¾j0Î‚÷tâï4#Èt¸ûu-U5²üB‰>v0ÆÅ»7»acj[îlÁ¹ªW#\¨§v‚JUp’,ù#Ô
x2‹E¾(âQ=JPF3Š¨ª
rt¢ˆBbk8‹>ô:6æiÕ­m¬Ç¯‹\r_Y !¦×@&øÄ^?jpE,¢7³#TÅô ’®¸’rA ¹ÈÅ`Şã|}êcômYÍÇEã«BiŒ+6X…˜œ›â>|1b“½F?cÏ¯îm·t=3T0XÚ^œçY{8Ì-M-G÷…uàCöíCBd¼?ğşÀû“ zºŞÔÀ•»dW®`;5l95c=w8WèëÊe'%]¨ìL· üªZ>='sS	¥`ZÇWo¨{\BMåOÅfDz0®c™#kI¦5
d«q®<*ë#½®"U¢s„šŞ›P-Û°Î(*L½)¹
õjÈDûÓ²µS7p³ÕÚAæœóº¿ÇmàÔ¶z`¯ƒ½Òè¹>	ë³4{6>«ÅÜ4W†Ægpã–àr,ÂMOÁq&f†5µÌÅD7HãJ×Ù­U‚òÕh²ªIï¼¯Şm¨jtò³Õ»óû³D}YíÌ´kL,¥pLzŠ‹r|DkœÀHıºÉQê¥K^n.8lŒs+¤5Ç™MÒ0XJ¥ãIÏç–êªÖyúQ³MœJSsÕnºGÏ5„ÂMI±}ú“†Ì¢z®àä0Š³Z¬æ3ÎKä<·ò$6Wà¤Ÿš„xk¬ßÏ7Wî–+xÚOÏsŞz;QÑjçĞ{gEe«‚¤×ĞŠú½:FÉEÆ¢âU©«÷‹èªM8 ‡Şx+Ï.À,+íˆóªâ¯LÑT•¯xƒªˆ¦:ìM™»yuÌÙ3•y_¾ˆµÊg&Øi×¯oQÖ4¾~›«ˆ´ª6õË]…âV¹V.{ÒUõH|ù«°Šªf	³¯EU³òËa…$U%QËØ
#Ós>Æ ›ëQöZa§¯¶ÓWóµ¹wi7¼ş©v·ô»¥ÈnÁ•t0p]£¬
l”ÙäÊ|Àµ…uj&'–¢åi…ë˜ûš©\üÉQ¾é;¿ÂMàgºTNşĞ±ÄG/ò«ÅŒ?JWœ,!ßN¯V'n]@Ä)~°Ûİ5ÌF²Oâª=ÿÊxğİî5ôİ1{Á´\m¯Cş´á\—¯ùL mõ+ø»1:vÕ{¢Âüi¦	I?ÿR…‚ÈH3*cEF8Ï¨MÇi˜F…Š´¨­Q!î0)á¨,Çy$G…Îl8·ÚHH”º×Q!]KáÒ9QîĞÚ<äS¬:3ùãJ¬F‰zV¥Şf’°†ù˜ñQkÂlÌ	ı¨U±WC@jôËqC(H­*SŒ¥n¡\¡!9]©Gßªy“ZjE18¤’‰ù](Ë#Lªªp„o¢®Gvtdz>>$W¹¦Kœùåºš´\;)°Ñø6sñĞ®è©C%"=s’ˆ$ôÌ±=2Ğ3CÁ>¿®lçßïæ¾ÆC°İïuG__“¸{ü~·ñ5¢}¯{÷:[În¾×»cqßÌ'{kT?xè©=ŸYÓ©nTOæèğ¢\€ ¼ àEaØxÀ ^Ğ / x‘~.Ä À)Ö ¼ àÅĞğ€OxØÀ> öA†°€}£ì` û ØM¡À> ö°ñ°]¸öá$ìƒ!…4ôvõû·ó7o_½¾¸÷—>¶<ÖĞ¹ıË•¹¤JSæÔ™²\¼{ÿn½0.m¦ä•Á>
x²˜ûh Æ‘ø¼é©€µúM:Z¿’ÅèğüÍ‡››Woß½İÜşúñã‡OŸGş³ ]pÚñ—o@˜¾]C^úŠ2¾šEz­hŠ)zŞ!PlŒË:öÇ£İÑ¯ºĞU¬Æ‚Áç×ÕGL«y¶ğÉ„”Îìyºo¬IËÓe?Èe/W‹ùââŞ[Èe@.rË€\.ÒryÈå‹Ú!rË•Ïà{îF™¹\“K¹¼Ãôò³
¿:ÙóØñÿ?:ú¬ßHóÇ]ÑJ˜±í{ ÿøgÀ?şğÏÒ€ü³9àŸÿ,GøgÀ?¬ü3àŸ{s şY›à“B[+‘ˆ8?úëh2ú‘6è=2 àhU† p4Ÿ£:Øa2ïì`«ÙÔ01€ ì ` ;Ô÷Ú v¸°€ D  ]{ä‹IÈˆ@Š5€ D #€ D .À @òøÇ—!‡<>äñå¨ŸNòàÂJ!ypÈƒC¼'ÈƒC\§¶<¸rVzº^÷“•Æ“Éz½²LÈJCV²Ò•®ï!+İV²Ë]†ì²®çÅdj!»,Å²Ë]–‘²Ë]V@{vr»Û…Ü®9äv!·+G}=¹]ÈÍ
+…Ü,äf!7¹Ùœ 7¹Ù*ÏIÒE_÷”†¹²H’B’’¤$­oÁ I
IRH’B’’¤Ê²B’TŠ5$I!I*#$I!IÊ ²”¥„,¥9d)!K)GYJÈRB–²”mœ!K	YJÈR?K©1\õ”1DÈqí•	CHBÂ†õİ$	CÈúAÖ²~õ»@™!ë'½÷ƒ¬dı ëI7HºAÒMŠ’nt“£>SÒ2fÂJ!c3È˜AÆ¬'È˜ÉfÌÔsWK³ŸÜÕ
¯LÛvV¼‚ä$¯ yUßU@ò
’W¼‚ä$¯JªÉ+H^AòŠ+$B ‰rH„@"D!ù!D$B.)Ò!‚¿ì'‚?]¯¦™"øÁ‡şy#øÿ  ÿÿì]{sã6’ÿ*ÊVİÖ¤ÎqHŠq²™«73—­ÌİÔ<²\]©ø m®%K'Ê¸jóİ 	$Š€@‹²[µ—KL4İ¿~°	|ˆàC"øÁ/~ÓŒ†CiˆàO)‚Ï××4‚_.rKù¼%Lµ2StS÷
ø*‚Ü^R,;ŞMí¥~è[ç‚u…AtK Û]¼,*ÀäÌ=ä3XŸ
òÏ8ü8ä3 Ÿ1ìiÈgÔ?Èg@>òÏ82ŸqøäS É o<W)æ7şg<û·™÷-ãèDş]•Ø‰Qç`w©M¿|;q|+!õ©H½@ê¥‹Ü!õ©H½@êR/Ñ‚Ô¤^N’zùş{ö¿û²)œ;‡eJšğ‘š7×”rÍ¡ {Áº%½€ìÅáÇ!{Ù‹aOCö¢şAö²½€ì…8{ÑD¦G'$ØÇ§!ÄH»ø§D"ÂÔÔÅin9¾íÚ#'"˜qè3Õ½3<!V]
üÕ¹ÔÂ@kó•˜íÅŸoî×!$L a	“ê	“©&L ë1¹¬ÇÏ@&2!	y¡™˜CÀæC‡€9Ì‡==$`Ñná í†h7D»û£İòÑW[W}Ë5ƒÈ5¡¢šÕ„¨fÑÕ„˜áäb†„ø ÄÇBâp‡ò8ÄáS\‚É@€\“
p9íßnvaQaBãMÄQˆ4¹ofài’s9åÜ¿S£ºÚu€Ò“Şù2ä8o|2²!/ŞÜZ„£·Ÿ{Çî¬¥ß[(u Z×7\ÓÆ‚cwüù6n…!ÊvdS"XM@õ®gí˜$·{‚Ûrº ‚‘Ü)N*‰Å7Kó˜âHñK.Ú,hı˜¥~5\„½ùJÛA”iŒ(“’/FàÇëñã»7•jM‡æb×÷§?i©?Ø¬b=ĞÏõÈwc;„ ıNıĞï[¼“é~‰wËŠhVm (‡ÑAÀ¬Qa   @ ÔÖãb°ğbpDcû•"ÁÈü0:A$X[ì¯Ú«±tôq…²¾Ä%Ìı?aî>ƒ 3PZm¼ú!=˜¤‘>×ùfqÌÆ³Ï”wò Šø?›ïšğñOˆÎÎ ôx2¥Ç&áş[Ã ÌÊ|[?(ó-†|¡(ó1Óû”ùN¿Ì2hıƒN8ÄÕ·>¨˜…ŠY}³L¤Ä°õDJì8™‹ùØ-2!R‘’§‰”@ŒbOPŠ×Gb#€Ä Fğ”1ğ û}V4g}=5¨}ãƒ¯ü|}åõ¨(œgßâøÎ{œiË²"s±p§æL÷¸Çm/|ìoF?ı÷ñÃÕeùÀ/X.Ú_-$¿$jEò{ƒEÅã2|³î¨!ÕÕ¾ÿÈì8é:!b‘gêrv×ˆ¥üŠ;Ñ	•8 yƒjƒd,1+U©JÅ@qù?DJí<M/¶Úz¦éLMÍ@ÌŞ=-~ğb‡€—À3±¯1p{˜vn~²¯xsÖŸ†tfAÙ …B$1/ˆyM%æA¡şA!(t¬A{úàĞH”§PÖàñ"3
RÚvhFà ƒ8H=	¤ö,48HÏÎKyšFàÙ€gÓ?ğlÀ³áP–ö4¸9`Oc±ğ=; W\¸àjô0\p5Ú³€\Ì¤¼ğXÀc™ÇË‹ÍÅìî‘J3¾Ø7£8vÎüİÒ±ºğÆ(oXˆ]—¶+]OÛùú{^ïÈÖ-såò{U`ÈqŞ2{vPëÉÊÌ9Ä	“kñ¨bôŠBxÖ^Œ‰úïğŸ("6ª}JŠ˜mWÁ>ÙìÖ?®ïòEW÷˜P×ˆ•“ëN«© úl[Ëj0v]jšß¯ˆ2½»_­Z»ü5İG7³Æ±ælö]1ó„	ÍÁ/™'_w#¿šzşÆgŒö[#ôâ[oûĞÑa¿£ÍÍÎú/sá¸('rQ±¶ÁV~ëØòÆ]M%sİP H†gQ86@¢Â/v´ÙŞğGÈ1üÀ©G[S>¾sÇëÍÃÓÿ˜#ÃD.ñãØP•²÷iOlãîÚQrD¶ríÜQ`T6RÌ†`µB±ğ`İ¤=7l’Û`%¼ad·â õæŸ©ø2á6g«ÍNxÍßä#-	ßî»ã­6X_§İUäg<~¡ª'[t1»ÙìöKâ£wî(7¶ÆÜ+\ŠäBµ/Kb¤;·üu·M³ı_r<ş	Ëÿ›Ùşæ~f¥Ê­ïxÃ*Z†D~û2–®Ô×KóÏç[9Ä&º_£»=o ºG×i"Şã$
-)#ôcšP†Œ&Ûiüx=Mo˜‘Ìª¶÷’¡è8'0ç¾4ÅÂ¿wÕ¨ºo„Q(¿Î‚ª§F¡ĞvK‘ªi™jdmYs/X¨’UäqÏyh›jd=GjìÅ‰ezâbí¹Y+MÅ‘,Y½¨²V	£ÂÉæ¹g#lõ•–ÊgRŸ?~²(ÕFù,±ç/‡?¶|(]10ğÀ¹­æ²$WRüé¦j`j¡bŠŠ†)FÎ³àŞv;ÿ?¬¢©ÑôÀª¶­¹ ï§wÊiD!%ƒRjdµ4u>Î³Q^}<ä§Ê*‡¡§oş+<D„-Û·³ïŞÌ*g£Ë6.føiŒÍby÷±Òz&\¸,Gì¸I…´Á0¢ü<¿J½®K:£Â•í‰~eé@*i¹LÕÕOÍ/
•-WèQhn¹zZË°‚$b8Š\²äƒŸË–,‘Sîİ1›ê~pYÆô@T3øHØ¼Pßu]—íŒÂ}:üTÓ1Š´»ÑÕ*#K¹2’YkÉ‘¤H=gm õáEkAÑ€T™ U@ª€T_ Rà'º€ ?vP9àç»ÚpÍîá>€rÌOÊá>á#,ºMh„O©Ú=mª\€jÕ>/½rº„Š)‰Üp*=LîY–L¬ òMQ¤Å›OÎª+_q\ÜÏëíÛ¶Môğm›JTíòí_ßª½©xÊ ¹¥ÏQ¢}`MÁšNÅš‰yWöõõë†pP,*XTÑÃgoQÏ!d{R“mj3Ù´öL6˜ì©˜ìgè B „ aR>÷ 
£'Ğ·æ¦†ª3 °æí·)&„+•…±Ö†ÔÁQ|Fjú–c|Ûê‘a		®ÑòmºBŸ·hH‰r¤Î£c¾‰ß!¦ï}üÀwç¡á}­¸˜G‚çñåîönóU¦kC6œG(0-OöıÖV#iÓvíÁ/P×$ÿÂ­ÍÀNÜydÈ“ü€‰Å(@fÄ‰<Éwi¢¸Hd"ÇXÄ
«ŒI:Ö"0\opŸV|ß¿÷P‚$Oğï[¤¸•vhø(öıæAµ‘C¹‰c)l%é¶¬ôvh«½‘{tt[e:úWvSE+€İO‚€dÙ€u­NP¥N, º_'¨:'`c¤õ$Fñœ8€m£NP#}N,À¶Z«” áœX€±ƒNPsN, hF'
TuNÀØJ'(È;'”XO+Jà9Q6´ÂÁışfyµ‰ecÍçÆ4)i‹xV07Å@—$ŸDTNâÓ:St¼l3Šçh®@ó*XÉx$QËr£%¡Ñ·XDnÔ)Ç®Ä.èi6(¿O³Åê¤]û¾Òæ¾ÅçŸ¼E&·É<À†êşµ…µ:ÖÍ£¤`âÊ33QÕÆg "}(OğYq :ÆÚØ@uÊY±¡Ö)Úø@5ÜYñÑpÚQêÛ©óa²Ã»l‰!Ğ]¶
öèca8ÀG–óyÎ­„Sg^÷kÆS ]ş‘îo~¾Û§ûeošæ¨ÕÛY0‹¾¥Œ‰ÅŠÕ÷Q¯Ósû…RâÄşçäÀnöšÚfÑmÛÌ5¹½sP6À\×UkPñ¯§2œ#EƒjÄO±VĞ‹>ú,ÇÓèæ}ñg'Ú¶Œ9ÖjI=¯Ş“>\2Ğw®Ë:ÑénÌ¡ÿŒ³·wÒÙ‘ÔÏ{s>g}ê›örÎ|+fQ®?{›®öh7à¤§¿•±Î±¡ğ\¶(y¹‚İ¤O¥8ÙmÖWõà]P”í]şÑ,Á²ÛôZ‹ëˆ‰Ü—ˆRÂÆ&Åwi2äkD9ÃJ-Â…çÉ÷Ğ}Ÿ¥‘Êù8’´«ñe&P&9m3P¢şe·Ò@"I÷oe!ÄéP*|ÿ-ÑFïóÁ”gA—¡Mš£JÎë¶¼“çGJ>ÿªƒù@²Úèñ=æÖÙ:6¡Lr7÷wño›4BfQ&«7w{´èÆr(ÉèbÊêu)Ep—ÇR5©‚r0ÉYü¼Şî5L GéhÚ‚z0Ù}Ø¬tG2rŒ’ ãòşQ;©7¹ONó1Z…ã[¨SŞoO’há:‹ÉW#ˆ£>NÌ=Çmòñé^èª‘e (µ™2;t³Âw?pÌÉGìù¸^<”q|–º‚¢hBâ81g{>r{ª‹üäŠ|‡ùZúøûòæáä³¿|ßOgXnlL¾ğSèˆêc…††a;¢7q§ÌŠÚ#ÖÇÈLÈõEå˜2;j×\£‘¼Èô‚sT¤š9á‰Íı©‡öò2^¢Q®‡‹ÉS‰"(Wr'ÿFUo(G;Fäû;¹–E–p@S‡Sg]ô(Ö!ıæ½¢„ä±6*ÔCVâºçè¹ÖA?}ìÛç•(‰AjŒúVùÆä1WU¡RşKóxS€˜ØØ¢Ú­É©.jÚ¢¢in-{‡R	;€tåP“ºŞ‚¡ÉHa†¢ûºÚ¡˜8?Á*ûù.Ú=n÷(Ö"ŠóÄ@Í‰ª…{ô?ÿ;‹ñ*»Å8Å5ÒEtOï_Ÿ„÷,ìIÄ¼‡>+ëâÛd^<ÊP©ï›Ç4kåˆè4AÛOX6òúJ‰¾N‡Ÿ)DMæ©S£>öŞz¦„_ƒn<Ğ©¾±àÌ1¥{o¿Æÿ‰ÙÉ/Şk3ñpõ^ºŞnv{R¿'¸!P¼ÜÜï;w|@eaÛ2í>ş)oòT\&=:7©‘6‡Üô³ÌÛMÖ¥[M®åoglvü©3Ïg°ÅÿÍ%ÄÜ³Î®yi%^+s“pÅÅ=ÜuÓeeÁrÿ¸åä›•…µŒ©›;Ä´½Dy7£ïV®[ó(ú!ºµWçÎ<pîQ´MS‘8Šæó…ål%"n©O‚8Iì`àÛ|âmÎpCj1ú'¤i"%S]ÿøUC)¶´r|"w`->qßÕûöm[Œú®è ³OÛ÷¨dJí3µ0™\áÒ UññÃÕ%æ±vW…åIBùÔå=Şâ£:RËvD-ÎaqAŒñ¡i‹;_³¨Ñœ¯Ë5y÷áÛ¶öîCwNl9á:4­Z|4Jg/™é‹âŸ2j³<FWÚÈ[X³p:]Yº.‚¤‚×–nêÈˆZE·{E«êbâ1èü®Á0‘sÄ³’nş<šKmöÑœsi2®ã|‘l8ßôFO&'gWøÁú·BğErÖ³>SüCs;ò	“ÀhVQıÛàòODuP\ã©‘K_ÒÖ»¨,…ÄS¦yQ[Œ'ÀIxhıôß&°Ó/w¸’ññp˜VHÔGˆ¯ò9‹f¬Àxzş4ß2é¨ó±4¶Şå	ÔpwëZšY«î0óà'yä.OK¬Õ¨âQš(Nây^‰^9§ 0~ñŒ/G0>€Ë~p	 @âK ‰“€7¦îüÍOŞèÊ}ÉøÙSmÉ¡óÅ0:L0`®—˜Š<uşÏ>B€Ür³4#7Z^ÈÛA^r+€Üzfwr %· ¤ HMH©CO7Â¡5ü€p p™¿¾—€7À°ò÷,ŞÓX<uƒà»ºk1Ê—†À €Aà¯ï%„>šà€æ<;Ù7ØIğõz†­äËØ¿n¢€œ×¡İâˆ×q`µ¹»Æçxu¿F¼ËäÁ&´>K›Á´®•­bÈç½—;” º‹º
Ê»nÑ£èRú l_Ğ`Â˜­„ô}û:r|7´İa/h'Ì¾ÜAº
Â÷Ûäwàİğyìú†§@ºÀÅ¾›&Ø,¡KÖ÷ÔÈ†‘—Dn4ğUx–îçÍOèoh»CQÀiç3ŒºSüìáÔ«î?tzßÂgÅyÔ7ğ2[)¬Æ¹’wVèÙY©”c5¹¶5Q)ƒ´x½ÄT&†Æğ,+FÄm”;‡”irÄ;ª¯X?™§XT|åR‰,~¹Ù–#×ŞCÎÙ3–j,µš4ii—A’SiâÚsíO7µsĞ¾­æd“ßG&¢Óu€(Â|Pdbz¬èèÖ%ÿƒvLå=Ê6
M@	­„ñ¶º/}vşZA­ª÷ó'1{—&/´ÕÚ·<ÅmVœÏå×4Ûÿ…~}éÍìšZøÚõå7Ä½kÎœÆŒ4qÈi‹7¡…a9Q`I5Ì–Émöï7qš¤ª…V`Èu$d& 7
ÒeõSt#äó³’sÎşËºÑgÓƒ?ëœw-| ò6²áí¶µî7n¥ú‘‰^ò„T_½~İÙéõ¸çÜ‹Îia].¶»TÂ%c‘[ïj€(rL³¢rëZÜ­Êá¦A¾rÿÚµ;ä¯˜(£Aş_Ö•IòØõ.ˆS¼ñŸ7Û«ÍŠ¨ú—Ÿ6ûıfÿñ‡Ù÷ßG÷şO‘Ñªy3¦­âjUãbVüOByJ®"·ø[rÃ›‹ Äå‰©ÿ’á!nÑn™¡âz×^ÏM›y–iÇ¶9Ì:æëÿìoÈ$ÔÌ±ç…Ièš³1[ºâòmK1ƒLYó]£+¢Â/…&èÁÂ¾Šv›k|1K^±ad!™	`…µİ¢X}s‘xs;	$ˆ^ÑMAòÈ{Nä-;°-yÅq5b–‰Ì ˜Él­Sµñù%¡ï¨Ô@¶úv	«+m¤Œ`ÉÔ9”¾)ÔÙìÏ.‡º¤–qöcq‰§ÈÈ ß|ÆæøË>]e—iVœÚr„üµ?2â+ª»	cö¯±Ú»ú[SçæÖ[Ôlaæ¨Ê%khÌOD“C÷’¨|GGáÓá=ÎÄV‘ZŸİ_÷¯ÁŒÿ˜¡–K2ñ6•çÎ¤4}Ó0ŒÙ¿+®‚·óªi¥´K~-¿–«;Bz”|üÑ«4rœV‘óĞ:Æ¨Û5 RğOKƒ8²_&İ³ æ²qÇ¯¤=ş?Ç˜¦KV®™óÒ5?v—3AÒ‘ïMlæw£JD½„tS æ7jl4–•	¼wÃ}Qz<7~\?WMº<|“QÙ}~y$Q?Ä.¯8ÿ·,Gc³ïfíÎéÓ¬>ªÊ‚,5íÎàëÁ;WØıÊÙ«zœûÏ°JÅVérÂÁÕ|pÏ1·;­CÔ½ƒğUõä4?'{X*â%S:1Ó¢Á{Å‰ytt]}Êö•q˜)íùµÆ“êÓ—XvON¥ö49ZÔ!NíÜ€oß;ÌÁ<½YfÔ¸-ãÎ/G>Ÿ©Ò¨e$:Ä•&-ºòZ×AáÅ¡k´{S…”I=Wi‡hÈT—¦èF«|äçß‹VûW7Áş—»L}Èk´åXø FÕst¤Qk-ø5Õp—ÿ~qwÅo*w*³…£cVÕ`Š/“šÓpĞâHòó¹y×“¹çúÒä‰¢N®âë».²ì$òpŠ—n*&Ş#N8°vDH^uõ(6/=Yòrİñ÷Mz÷ıß=ÊöšYÇwÄZV*¢Ê“£WåÁJ`YQûpMîÎ{0üĞ¾BUè=lnQ·W½¾E»up‡î8ªò]ZŞ!s*¬İl•Şİv®å•gA¼NïD½HgîÌ”İ~)¼Œ~ß¦;$¾~OÊe—«tvV]ç^)—ËYi>­û0‹vi.%ËbB–ìÓıªKËY[vIÿå3ä‡]‘Gd¶Å;#ÕP”tqzC?—³Ë˜ŠHÁÒ™‹K'OÛßª”ÚÿScíBƒJ†÷;ÑY‰<ä¶£QXÀ¥À8•{™Zh]İ²D½Xè™Òß{Šß·‡9¦OÕb‹9ùOÕa+'©Ÿ˜Ç%Vé§öPĞŠ£óôç3×¡#´-|“£vóêÂÃ*{œŞf§vk+ÅÍù@›qAÕ´ÄCÎE­­%s/ZJû ÛÌk…ÆqÒ‰V?øÕä‰Jô9Ü-«ÒÙÕr—£¨5êâ.F;kTÀ:µn—B¥‡µªÚîşs”¯FıÊ‹[•;²R•ï<§æ‡U±6‹Íq×|²Ğ¦?µÆ° œ8$	ÀyòØ€ ? ~Ïøi‚aGÁ¡ù\;¢Y —…‡ ÚôRhs–}åŸÒöƒıû-a¿%Í^#Ç¯İæÑÒƒlŞ$tÁqÃÔÏï²Ü0†ÆèY`	’€1 c´?DHã g4Ú%uÚ-öÙøIY›S+‘ÃGvxtÎ5òêßĞ«‚‚¶->‹İj¼bv3òQeÑµuì`IAÑmv¿¶_­g9fuuME_EuJÇü)½–¯ZfŸV¬¢®†8‚ú±¤?a’´›åóhŒ£·zö—šM#VÌ²ûY¸ZìÔc`'F¶ßçù   ÿÿìYsã6Ç¿ŠÖOÚ*—K‡ue+Ù$3“lR•£fSÉ#‹’ ›‰$jHÉ.?ìw_€7ÁnŠà!Ù™?|Hºq5Aô¯¶Í£ÙÊ]`LægÊ>¼IëÖC…éOP%çÍ¸vú¼Q—ÊV:×t´^Í—÷Äâvxâhw"qívı–*­ »-íÚ]K°
!bÔ-Ú13“Ñf3šm>#3£®ìÜö©hŸ˜»²ÕfØm<÷txgo·?…;g¥Îî°jĞ«Ï~ôİı·'g»Ş]\‘jo¼Ûè;Ù)e½ß½OŒCpî\yÿæ¥™
ş9ì77ºr7”a´Rå?œãã‡ıÑ9:"ÙÄ¼0¦Ó½_g¹²s_ªVÂÒ½ÏEÿ¤åU#Cş*,ğ1Å“Ed{öuC¼®§»zŠO'{ë÷s³"­Êh8Ä{aö?Õ£J«Ïõ¸™`´£bæÑğçîT=õ£âÒ‘;%Æ×ÿä7xøşàv¦7Uy¸}}Ü9i³TjššÉÜóõ/OÂóœµàÌLPĞÜé-Ãßº¶ÑÇ¹A,;@±EÿÅ'MÍMÒŸeêäï²tá°îßD£D&‹ş¢M*]jª‰+ŒˆDÁü Š¢¯fG:æÓ‹B˜µ™¿6xmdÁÀ\ŞFÒ[µ7ã¡:†³,%o]õbÖ5J¬îT)n
¢ÒĞ°È4™½|Õq]™úBÕu—ë‰,HÍlø\ø2û>­vÉÍşûïpÿæ/bË©§Õ-'XZ”Yû%¹Ó	dÔ3ªÏğ2ı¢ä \gzs§qDU êF»‹=#®–Ò•’¿ñü„GY|a~rÁùIœL5LIŒ†ÜíáĞèßÄ#áæÜ hubÙ1¢ÛêóF,“‚(AÁ•MDŠ%Ó@bÃ±öõÏ²+“‡hâ.ÿ,¼áËAz|t|u~@ğuÔ‹d#Šâp–wÈÇ¨´3û•p7ÑÊ1À¨ŞW&IOÕo¿$G)»8 âç£nfTrÙ§î‚^¥7øI¬[Ùòwîşè¹Û­l”¸!>¨4ıø?•ŒèYaœ©°SĞĞ hœqºıRQ¿g§¦ÂúF&Ù“é7ÌÔõtPnT_E“Ò;íh=ì|8ÒdºÙcbùÀ)E¸¤ÔçŸ˜Ïù]Ä'òóÓ^4kåÔFü2_î‹Ì!ó"³ÈÜ7&³Ç\ÂªSGMÚëœ7Ì?™İnŠãÈŞfr{¶¾Å#£ÈÒõÇğOî!8ŸÎŞ“­ÖZ®õBş-«ı]Ğ0¶ãwµ	ìÀ£ë‹½åzrR¸!îááMüF(*#©Üñ½g?ïu}G)XÈ-Ö\¯yGh˜Ê¬^¾ô]š¼¼¤ö\÷^6_s¯u.yMz.²¹¿o¨K’O»ôùêêĞ‹î‘+ßjL%Æëáj9ïz«acÌVÍûÂaÍ’¶d/Î˜‹ú`f\Õ2‡¸~ÛÜñã?³(’Ãóªn|m·JMS›pƒG=öK’Åş¼†ãpúİ·£Éà}×§úÔÀİ1ø{©¡pÑÊ"ÿM|^ZÜhäiòL/-Ëå"ç¿r[;iv±Ì†‹IÅ,•ì‚JO„ŸMçblÛ¦'Â5éèhøb+\æŒøÒª.’Ntsó÷Š7sj|±’:ª™¸—ÒkMê´ó¤Ô¸ èW‰…¼Æ.ëäyõg|eTAßœË,¼×ä û¤vj=­ÔF¦ß©µ¶Æ«Á7#v}`¹uW¥Ûâ>º{a­ìí63°*İ}ğœ'jù4Y™°÷ÖÁÙ[•hè-uïÎŞ«x±sÿt‚ÅÃ¿Ù¯l(ËWÇzœ¶%e{rÖÂ­\6ÙùV"µ2×[:ëµ(.…Ä	d{îımpro­_åÎ+ãËnäÈ|e•ìÅº‚>QËY»+N»ñÜ{ÿ³,çÁ>ÏrC÷M™îrTeo´Ô*Øéà‰sâ3VÆÂuİsíîd}¸Ô\ìË«Dµú“#¥Obâ³”•{oÖƒ³9úÖòt<+TñÍ{W6İ³í­}UuçtÈÜ}²îÎİ’	6ÎJË«“´/–çøÅ-¡Õï|bé.rxÛK÷TÜ³Y…Q…mã[*Æ'zãé§ƒ£AT@ ïÊ§XxŒw“çªƒ…¹{dáå\ÃÙ¼üWR?_VúW¡üO©*Ç®÷­{üa¿qƒìÈ?H3uûÿÀí½rw;w‘ù±ƒ…¸mã*Ÿ-—Éñ(+AÚj·øµ*aR6¶PjÿòßÅNôêgá{.$*jÔhpXQ§²È@¨(TîJùÑyx<úAİ”£XzÁÇUS.=×^Ë©õ±<unrøïåúåNN¢ÂaFOPÈµ‘–gi¯şbºÖòë_•aJMU°ÈÈİíşMêv{ôåË]|g/íö®÷×Ü“'+%úÄz–Yê3²ûF·Å§à¥éâ#Ù*JşŞBõ§`Mòyˆ¾`Ÿ‚t~ß<Û/…¼lù¡a>?(_tšƒ£ş=—Ôñkû¥ÿAÄäÈş(Gö–…û¸8Ñ^ø¡§Ç¿ê¶öKÀ9Ş©{?ŠMp^’½’zb#»uğ?k¬äß¥}&<jq9|2Ğ¥õåMŠvÖéü Ø¹p÷{9Ønİg±VËOìÍ:ùÜJ
I‚`ûåmøÇGûl’¯ş¶¼àÃDò	xº«È¤ë¸—>uà×ğ9ò›½ìílilö’PJaI¤o?)Û/»áŠÒ¿€jÈ'cyUs…EOCèCÃO·“ùµš2½K_úÚ{^áıÁÌctŠÄTqĞ„ğTäXÒQËzŞá`4×’d` mXéÄiCò”NZÓ²Ê6j*<H†“6h*m` m°h(m°0‘Ö´lƒ²Ë&30‘ÖpÈL¤Uòñ–I#»Œ´ù¼¡´ùÜ@ÚlÚPÚlj"­i»ÍLÚmÚĞ–È¤Mš€‰É˜4m·‰I»MÚ™´û†O™´qÓv›´Û¸©-›Ø’ñ°©4òT8FÚ ©´´EC3¹0±’MçwÃâüôx±ø~~½:™@wéõ$p±äØã¦Ïà³Sóz@é×ïÅÁò}O¬™eœ.©ÕhYî¾Osíj(Ç«Ë3AÒH»ßÏßäóÏ»XVFÌ±u‡CÒÀæ~±ÁtµÓû®÷ 16¢óªûR&í}IÚøÍ³Ú.‡šW„I5ÓRåüLšáHKDº?¸Äc=1ãaÒôÚ%#\b]8ç+áÒë'q0®.¹^Û”'…K;/P=¤c…K¾(ú\5?“r¬Wxè4¢ÎuG.jÄôFİoÃ¥ÓWâéã²êÅ¤\Alb½_®!.u¡W2®"6½n48×›®¾Ùn)fÛ Ş¨šgx)^éü2çhi˜c÷[¤K”wŠ)½Ôïn¯+UÖ.ë»¢r´vyN÷êi®Æ¨åı‘—èJô–š±c3Œ¦Êú</ß`Sº7e½«õáï¢¶½’‡ôøL¹ì•Lï(›:‡;9¡E¯_¦nåË÷˜!ÓeOuûÏJ§O›¡ıİá Ó<ÜW¨1úìŞÓşJôÑİ>™ÎJ5_ü´£GH4Aï}É0 T4xÑ‘S*]F"\A{ÚÚÈ†èa¦WĞ—°[çƒq…’0³Šß`K‘!?®PÚ€äùVõ 6¹‚Ò´]I8–‚ºÑWĞ”yQ“÷<2‹´›µ5Ï‰ËÀë–#w:ËÌ"5î'lmº†1f¬›FeŞ,4@é:3ç10”I_­ª=¤)ˆJJg˜«kÔw´bÊoÅóœ"±u}ùÁLà`—Q½D¶¹If ô„/¦ÛÔjDÂ¼]c‚ÄX¾ªSƒ å±Mô£mœ‚ø˜¯ #3IÈá„õ,ÙµÃ˜7}ÂPä1HtKûÆ²˜ÜR.2ƒf·º§Ì ñP¶zÎcf’v|K;ÎLòÅ~şí¤É&ã|o&ÙÈb.8“<¦·¬'Î$›Ù-ç3Ée~KúåL²XÜrî9ƒ\ÆÁ©]º—®V¸|ê²ËÊ¥C à¡3Ê`q[ğÔ¥*ëÁ»ìÌò’}‰rİ™e¢ª$;—*^|&ó[v‚S'?5`Ó©B­äX%=“f¹ÜßrJ³|dKqÊj½\ç´Û¨ó	êÙ-RîËJÊª{S4ò6VÒš±eU¼‚õDj¾¦­Qî°k·VèÕ›â È9ãÊû$1ÍÓ=pÍœlÄİnmzÖŠbR_[ëî´v[•q6†ò˜uïk¹´¦®­–ËUËÒJ=N-É2vµ,·¢»§e©†N›–¥8ZZ–\ÅAÒ²Èr÷FËÂ*=ãäğYëx…g#ã¨h×¡KÕ¼]: ÚİÕümK­ô”—İ€ó	t¾ìßvÏ/Ü·-Ñhé½ªp¢•¢îÏ-§w²bŞöÃæìxÛùí¶%qS}¤åWªK£ÃŸÁ¯3¡ I\xv3Ï$N$Uªò~ºƒÑr5¤¶æGXˆa!AXHÔ,ŸwX¢:Õ¨Du½	QÖÕ‘¹Õ¨Du ªQˆê@T¢:Õ¨Du ª£Çº–Õ¨WÕè
DW º¢$DW ºÑ¯'º‚éœŠ@P+A¥i E (¢º\E¤#(¢¶0E (Ai ˆäªQ#Na8i)Na5™Í–êXwÄ)è)§€8Ä)DÍrí8D
 R ‘ˆ z"*,È"R s!R ‘ˆ@¤ ")€HD
 R ‘ˆ@¤@‘Ñ…HŒ»†t•€Â…
>(|PøLw ÷î	î½4-¸wpïàŞÁ½W—î½ Ü{maàŞÁ½ƒ{OîıoÊ½×¡ÑÛÚ5¼YÏ‡b44ºÖ¾ ÑA£ƒF=½@£ƒF?+’[1uü»"’în>ìdç–OÛ~™Á}ú¼DoĞç ÏAŸƒ>}Ş}úô9èsĞç¥ú‚>}úô9SúÀe.ôD´@!ó|á²…Ö7ÄÖ“¼@®ƒ\¹räzr\¹r½krùñ¤ØúyÑÀÖ­·A[§$[/Klİ\(°u`ëÀÖ_¶>j	[Ÿ‰éx%†`ëÀÖ­kílØ:°u`ëÀÖÓØ:°õ³"­[¶^yùØ:°uR_`ëÀÖË•¶lØ:°u`ëÆú~6Ø:¸kp×à®Á]WÈÜõçÆ]3	jÔ4+ÔtiZPÓ ¦AMƒš5jÔt©¶ ¦AMƒš.o¥ºÔt¬xĞV<Z¬Å|°ÀŠ+ÖÚX1°b`ÅÀŠ§°b`ÅgE+V¬¸òú(°b`Å¤¾ÀŠ—+¬X1°b`ÅÀŠ	}ÁÓ‚§O¶B.ài_;Oş5½À¿‚ÿ
şõ¼Hğ¯ùük}¹à_ÒÁ¿‚ÿ
şÕ@rük‚t°h‰ ],Äl>¸Ÿ€ A
‚Tk_¤ HA‚ Aš^ HA	‚)ÒÊ+Œ HA’ú‚ AZ®4R¤ HA^„ ‘	"D&ˆÌ
¹€ÈìÈL™^€)S¦Ly^$`Êü˜²˜R—˜0%`JÀ”½üŠQø3øUmlksÌõh4¾ö h#ĞF Zûmìm:t"èD¢7N¬°®:t"èDĞ‰ A'‚N:±B@'i:‘Ö ”(?P~ ü*äÊ¦ü è¥ = z ô è	@/Ğk Ó ĞÓ¥Ğ @ïUzæ˜ÜpÑÖ€÷ëÅl²\®€É“&§µ/09`rÀä€É“ë¬Á€ÉU‘L08Àäşf˜ÜpÁ´t0í²âMo~êíåRN!áİj?œ_U¾ø"W°|`ùÀòiú‚åËW®4X>°|`ù°_H:t é@Ò¤ë¤I’$]¹t—#é˜ÄhÁ°8ÁÏ­,‚İ»ÇJ»v¯˜ì¹Ğ®Ø½:İ¤­ctg‹õh<›¢DˆNk_@t€è Ñ¢D×Yƒ¢«"ap Ñ¢Dˆİy}Ñ¢DˆîŒÜWÑH{s@Ú+„É*ƒp%y H @€4óÖ  @€4 iÁ @€4 i-£a÷-¡aÃÍd.ÆÓĞ0 a@Ã´ö4hĞ°£aÿ  ÿÿì]m“£¸ş+“o—ª©;clƒ/¹Ir»{y©½Û­}I>¤R.b×·Ûç—LU~|$@€ÛÍ‡½9#ÑBê–ºŸ~$€Ô0 †5¨al¤Ô0 †5¨a@kl)PÃ€du]$«ººpê¬€d$+á’¬€dU1Q YÉª"HV@²Ò’$«Št YÉªÉªßÉ÷-ñÂp>÷¼ñøNÀw¾Si|o€ïœ#àç8GmÎ‘âœ#àç8GÚøp€s$m/p€sTßhàYæ÷¸?ÀıîÆS†Äı¾M~ßø6À·¾M³HàÛ/àÛô
™ ßø6&"o›oÓ‚ãÍ,Q`Æã8v±?
P`€SßË¡À “˜,Àd&‹D›€É¢% “˜,Àd&‹6²L`²HÛLøĞ€¦ 4…K¦) Ë ¿€e ,` Ë Y$°Š°zge0l–ÏkÓ5ÓÚSKií`8ãpBZÒÚÖHZ’Ô¤†$5$©!Iİ$’Ô’	’Ô¤†$õ€“ÔªƒT¤ê U§ñ”şRuƒS5rpƒƒäà 9¸ì/£]‡³±¥ôÌ$ÆNú3HÏ@zæÓ3é€Ld: Ó™&‰éL8é€Ld: Ó™íÊé¸½Lä²ò€ü äšEB~ xA~@éã¦ÿ&ÿÑËLç–òs—8æÁÔ…ü äzÉ @ = ô Ğ@ß$ z èÚ ı5ô€{î}“¸7@ÍÙP3@Í 5ÔÜ, æâuQPsø×ÖGib?rÇG ÿüğ/À¿ ÿüğ/À¿ ÿü{İğ/ ¬ °À
 + ¬ °À
 k½H X‹WG€µä9²y†“À‰C7È Oä	¨åpaAÀİ wÜp7ÀİŒÚ¸[zî¸›1îv“˜Y_  IµuM4	Ğ¤Ñ¤‰k	MòCoŒ‘ï št½h A€5u A€hPÖæ³£A ´ Ğr: ~B	 $€XİğçÚ:ï-ŒİÙl:¼ğ
^ğ
À+Øxà"o¯€bhˆ¡Óbhˆ¡!†Vİ&’œXŠ$£ÙÜ‹ãÑ"Éë$©z²÷v¹„BN~AÈ	!g£È“†œôAĞ7´ .Ä]Cˆş úƒè¯¯è¯E(6r,…bŞd:ÎÃB±S…bW­–ë/Œ¶YPHæúS¦!A„’¼½$œ#H¸Hg™Î©à(ƒ£,—Ğì(›{–sßÖî6Œ&È‡İmàX‚c	å:–àæ›nŞm»ymH¶=Ç£9ò<pÀ€d1h—Hàé]´§W×ä«!Y€;{‹Ô†‹t§b@¯+u©ëwƒÂØÖd8Š/tÆ!@„ DÂÅDà®_š»®6¸Ú7éj×¸À( DáùÂ·1Ø½àş¾	~ÅävÅ÷ıËn‡_/÷‡?¦uî¬îwkü$ÜIùÕÚdD£3­I7•<Ü%«U“:è´…Ò{ü›ô÷dD*w¸GNŒWyï¯éú£šÑ-kºê(í‰y®«RhÛ–ÕZ«ÍúSÒ™ÉÍtÎ¹c«ô‘ÛCõeÙï©òVîşœ>í§§èoä5ˆ+?E‹x·y”·í+yãÄ˜«ÍcOz‡·«gö¬ı{qØÔh«õŠtöáùávúa©©…¬îÏtÒÑ¨I•›é¶ü•èËÊŞ'¹IÕSv“éÜ‚˜áN>ºyh[™ñrk-:u¡d)HÕˆ-¹ŒŞWÔ£ÛdJëêÓò~¾+ø*’™+D{œğáŒ'£ï+è•‹!£GƒïT|y_¿òZ_ª·˜(×qƒĞ‹}Qï?ov¦í¤ÎGÎ;#ÇXê{¢¼${~4‰ph.¹¸I4‹0FScqtæîö¢ãi0š„®‘½Ø<K2·‰]ÇC±‡D~Øl^“YÀ@bÑØáÃq·.Ù7³Èo“û‰¾ÏL¼`ŞrÚLúo- Vòø,`qsïËš¯$¥o¡ı]$î”Ô=‘ü¤ı<îªÔ<Z†öó¢LÓ]ÉzÓ‚Zõç7_ñn·ŒğyÃ]åg(ØÈk§ƒª[8±Æ(2ªÆ¸ÕB¤ó[Ù ¸Y0G¾$‘NáKy,],™ùÕš´èekBÏ©Ï­ ›HÌ9¯T©92GÀõáİíÁ¨û·mğ6ÓùÇšàq‘P1Îp–¼<Ï£“ñÔ)BxD”÷Š/¾_äGAXÍZWˆÜZ|¬°®]cÅ¡NÑĞC0«¥*ØP©QV6/Õ”c3.+x«±rn˜˜rµ0ŸX>‘Z¥#ı›O=ÌvßInÔJ'‰[»MCí/Ó/·-8V<ê¿\ÇÊÌ¹$Cşa»Ä1Ì×Ñ„VPi©¦ô¬^~ÃÊÿ¥½2Ø€½{Ø‹Ò’?lN_&“ï¹­:í]]oDpÕE€³-hGÕ/×‘¸p„†åóL"z–åP@z‘ ê€KzBP‡MeåØüà¿ ü)^×äâgD n>#"@v{Ùí”M)ñËI‰§#Ö[òœp,X:çÿüaPj ÏìıZ­Ş"2‰Ê¸½åH¨‹
Öb=#6$í&MTŞ_î_âWjŞìx·VŞĞîKåfÒ`†Ä=Y•æs¥@ê¿ÜíWÇO•{/7á‘Æ¬wû£R"ë<¾(£å«·«ŠÈ_¤\Ûï¾ûÃæ±\úÇåámép/·8úğùø(Ë¾Ü¡§dcÚãÿ—W¨t>ğ}ÒDs)Öˆ¢hâz¡‹#=Æİo@;z_<šÄs‡†ÂØ‘~“vB}OFÁX“@™	ıeóİ³Áöªú-ùŒ#4qœY+Ùv	™îôJi|ªL€¹Îš/\9{vüÌ0cLÍÖšû€³¹[d3¹¢t¤£óº&`(Lô&İC'#¼¯¤_#Î·#VØ›ZYúÁˆŒÈ>¦Ø°	’é·A%ç>Wsƒjî=×vƒJ“ûDémÀA¤o£hÆq¨)(”½Ÿ†¦ÛÍûÊz,ßğÔašçnLóêiş
fc˜é.n
2šJ³tvHË´9‡›yñCKØšÚ±Kû³Zz]œ…A.ÕÌŞlc<–½ìUìµ<p—²şŞ¸IoÑ3]µß£¯8z±ÃÍ	¢ÕşÚE2°¹4ÒÖFáØCN\µn†µª:8,+¬Ü>ßĞô>áÒômpõå¾;ìw?úïO	
{÷=ÇñjSÆô¤P]SKZãw'¸Dk´kiîbÍÔar$/ièOïÉÿ}Á»·(übÃ°q0Ï£‰Ò°ùw+æëA³{Yà 8öCmÿâÄŞE¢}¿›Ø³PM×~’†i¯Â~Íá9!9†7àBÌXñYÓrj¾ğjY/´J¿zÜüº|On¥G@•{ˆÚÁ‘Lÿ+¥‰‰ìÓ¾D9–ÄTpşÔWÛÃ³h™zy³òÚäêŠéÔ†îâ_lV+¢Ë`…;µDxNËF-©½²Ô²š‡uï3†;Í½N=–=¥{ƒ>R›µÓªâ£:ä…³¥Pœ$^‰zßr­¢–˜ˆ%)Ö!ŞÄµo"%‘òE\Yën³Š’_È›|Sÿø´9Õî’	¡ıÎ«gÔ¼Â·|=HÙì¼yâÏ5•Óàõ?Ü95…¬aÙ³ÙŠP® *ÑÇj)O ÜI:NjyÊÍ2q0wVÇv„$Ñä Ø^²×·ôæØ‹ãÑÌGÕ7WÓÁ>VAÉ†…ÍBÓS¡.#)êÂ-C»UƒÄ´3@B¿vêºªü¤3;ÁT²µTÌáNV2Ïƒ™3ï[Ó[(îÀtI©%†cXZyíÌu1rGÈ%Ÿt«Îh¥5ÁĞ¹5€^F“Ñ¥¨Œü¹å©ÂP±ßß’Vy?™ÜÀxó¦(O˜­ÓÉ".¿)¥D'OfŒ¦E“Ö‡äŞ-Hc%ÄoZ GŸpÍm.G]â@Æ¡æö)ÌÎœtS;Smiáf°5½Œ)80Ú&-(ŠIWæúc´ıYT+“Š¹¶ÙØ¾!ßdZ0K3¾"›¡(&áUK”¼±DÍn,œ«scÑ‚7–ÎwpÎwÀfm‘8Q Éw[c¯p}™ªËL5[Z‡»çíGtS­llLì'˜bÉ‰EIiÅwªï‚åí>IÎU"WL½Vo›d`«µu±2¹ÃÌÇª·©™ZÂ‡7[4‹QÇ{ÖO»&ƒC ùÊNæ•=§T±Ok*‰²¶ËÓŸO]¹A.kE»Nˆ&~<m%±uwôß 
#„CÍïdb?&‰qdúÂû.‹CÚëæË¥\ÃKJe¾hqí©.;É£ÓÇö~H¦Æ\àµìJYF¶6bHŒÈp{zfNnÒöúO0&P"4ááÓÏ ÖÜN>‘[¹m¿™å±|@ìŸôö{ÒÓ¬`‚µ.ÑîËq;´¨ÊÌ»§ÆMŸvÓÎ°SaGúù¼“Ñ‹šR˜—‰2õrÀ^Uï(I«åMìi0ÜG ÖwËG¨©ÇË® ÅÆ¡“˜şU´W%ìg;Æ#âÃ^†6P;Èñ<ÉæF–>JNğÛ=ËÓ6Ÿ—ûEJï±V&·ÕAş.'A
rq"@ıj‚d•tá AÊ@Q€t¤Í¶R¤`²çŠq‰ÙjÖ¬¯šû´Ëåbèt²İ:ûÅî×¡ŸŒ·‚~Œ'®ÄtÕ®&×¯äİ´1‹0ß=T)7ışíH^5>­I?­±­K|ëSîæåJ6q	·[ìİjnÙ*Èæcì¯KµIs­!’Ÿi^a0êydÃÖWéœÀÍÄª%Ãf•¸li\Tç&6O<ğ¿ÿ“ø×‹ñ¯CõÆU¡M½C¾–á^'Œ¼ iê·ÌZĞxuƒÀæd*²°õD@«
ek°¥ƒv›UkûŞ\su®DËã–ŠSK±ædöI¶ÌŞ½VÎ ›’+—Í_¼Ø=†zÇNÅßó?Şa²~NÜÓ-È6üm×	¢É|,1@µ¿>ç(9Í6ßÍ¾ÖğãoóUë´õòÍÔÊ‡Qìú!;Ü¤&=gü!;¼¹&ƒÖ”î´‰câ—Ôõ£s’¸ ¾	bˆP[Ò$Z¨}nàĞĞš+‰!Œ¿‘#ZHó·PDë:Ûiú¶Oû7sÕK5‚†é0²'V?p¢<[?Ó+İ
²ÃøùoOfc#'Œl7NÛãv³£4¨Íú@ƒ/ç9.vÃ©$š„cìƒÇòX-\-U>[©Í§!³•„˜lÅ{F4¶bUm[Yâ0—6ÖF{¸\(}Më“ŞŒîÊ_ÕĞí¥³Ï›õj¹ÆÒÓBLí5M<–Ù+-½IÕÙ£Ø“Ø¢(P´CáwªéÚ_AÒ0mÜ%&sÒ²ìZ1;3ú¦[DŞ§g/i®Ã¨zBŸ
®’iôéG®N5?>‘5gWhNK¦ãÅş\“…©üËæğó&ZÆK\95¯¦ ¡¢ã{Eï˜©¨JZp	Ø¨+2)0Å£Ëî¶ä_S Ÿşp·ç¯aö 2#àO¬àßcÓ8mÎêlj
ûñ^ª‰ûŠ«
=XópŞãFŸ}“?“¼èÅœà–Š}TFgÉ@éæ}ßP^ì±òá:LÂô×Ã,Á&è¡çP˜„ö/ÑóşÃ‡×6üßÀF(;#"¨Îù-µç$şoI¦èo™xÁÅšºpYŞ0}a:ˆ—ëÓÖwó‚»ø¿§õ|íû¼±ãMæh<6ô<÷|NA¾;B3z¸¥üİÓº¸’Î¬®`Ö.>ÆC_¼*bµ¸¦µóóu}ôA¹Ù=8Ã›ÿİY‰‡jÊ¹ÂzßªÍoÉ?Ò4øãş“é¯O|Fë5^Õİï¶+>o|ÿD.lóã¬^„Fç&¤*L?#²ŠÛ-_È÷‚r4?Ğš	¤IÍŸv›G–‹m';Š°¦“6²[nQöæög®¡@šf'â¹N;Éc/ƒ 6}Õ©¡´“é>ö"¬‰º·ãÛz±8>6¢ÉvÑÈŸ˜ìú·³&»S4Ÿ†©ÙÊµĞ8¡áØo÷¾‡wÎ§±;»c;/@xÜJÁ;6B›˜Š{şúra(±3q«~ñhè¾¶j8­t_{v‡©g•q@UÅâ>‡Ô93¡_fóÏíHÓÎÓİ¡P}nÚ=-rıÆºiE!¹«Ô³B¶ t\ş‰D”ã‰ª*Qp&Ås²%Úì&`ÎmÏ*bxhÕÀÆš”ı†tŞïùktEêËX±mô?p,è¾9óÎ_ı^ ¤,-\3Ği¸öcéÒ‰á„Kl+YK§Üã´WcA+ıÀa€›òÛÍ'ösÛŞI\wé¤oìªYQKÉ5¨H‡Ì‚SÖè]'[?ançóÓnc>­·VƒJÖé¸r™«GPatğØÿ:U¯ô]pĞæ¤›
sğı¦TØ04º!ı5JK¼„Ïxµ]¼%ÑMo/6ëxùÉ€É&«İgÖ_&ÏZş?ˆƒy<Ñ=D;iÊ¶Ğ”Î|64šûÑÅ­[`7&éí^jõ]š©¥L	Z„«l´‡¶K:ÅrOp­““´¥Ô6Ò®ˆ¼ş?Ş¿ù…¡¸Äô”ëOBwÿ,¶¦ƒòÖ¥LnŞök9Õ¼ZVÂk+õ„­sĞ£ğMÒ6¶8~à!Gv(“úL®1Ëí×™òæ#–h±Y¯ª‡ºò"‡p»©/Fkµôıûäå*hÃÕ_1\n(ŠÈd*?ë†šuUdz®Ú‡»úCb²‘9ÉÆ‰Lš¸e‚ÿh²Y‚×Ñİ&‘Ëæ	óóÚ‰&+{•½èL¯5Ï‡Ì´\Q~\4¢µ¢¤[n5³ EñI¹¸‘ïŸÛ‰ÑY+Ô~ŒÄ¨NôVœ’˜ É¹côÎ0u‡`î=4¨áÜêhPo|Ÿk¥A5÷*§A…É}¦£Ë§ŞA8:_qÌ5·ñqT[;*¤ğÌ\S5­ÑÂôßºE>9áıã;+{ÑÔ›z®ì«âlQ<î$&Ì—µ¬-'YÖ2iâ²Æ4YÖxİe-—1ÌeŒ’ö\|î‰Nb—¤ù¦.fŸµc¸İkŠñYñzì:/ÉP1Wo‡ÖÑæqAÿOî'&p£Üõ\/öø·Åºú›ä šãAuûeémù.ªZ‡SÚC§ñ>¥¢®¨¬„‘_*{€¶“*—>LÓõÍÔ»ITÑÈçâjR)W\“ZL…I•²v_#•8^H2€Í®µÆ’ùPÉGÃ
ñ×İæ¸}V+¢şä×å­¥çí™Ã
G¢%Ê»+Kt ÚI«xq\Ó§TÊı  ÿÿ Ã,ö·xœì][sÛ8–~ß_¡İ‡-w•*-‰¢.™éì$NÒ­tÇc»{¦¶T JìP¤BRN<[óß /âå€$(È"ù!qB¸œï\qp`xC;øs„«?=Û%ø/ÿ6ˆv{Ã±Í·y ~`{U-¶¶+~¶	^«Go/îÂsö[ÂZ!\õ±€8–ø#6&h27„øƒı£ôÄvÃF! 3´ÈJø<ğö¾	?ŠæTztú¶» ÃÛ‡¥‡GùÈÈjƒ\¼òQH—šİ\­}o¿»FsƒüĞ6írÃ?ØDË ÷ÊÎ'qCÚ™W^{>¦²ñ*‘¬B/DÜÊAAx·#èó½Mg>øñG“ÂËÛ–[‡èÚnwîqUË„¶üáÙ¦LÓ·ÄA	â7Ò1óö¿ï•kF¾òšã-ÃEÚZü};°§¦5Ãk|ÿ¸£4oøå[†•_(TªÛÇ ãĞxçâå‘°¶uõ/qÈ³ïp1ùö—ªf”B7™o¿÷]sá’Ço\ÓF>*¶-6Ø'ÍÁÏ Ô)`Ú©o#Çş'¹úàîöá]òO¶~ˆ¾í´ò.MÏ¥ÿÜ›¡çSl‘o&Ù±1ÿ0ø¿´kövHç¸wÂÁOwïd8†ı_íĞÜ®2½?É~Lë¾È¼û²ô&û9ôO¾Š¾rõÃ_ÀwºŸËª´büæOÆ35#K?'1Äåşå“pï»ºC”zaR^
†ñ°†)r8ĞşĞgÔß¿"æ!È?)šıXH\À¸ù?ª-ÛENœtGß&HÃæ™e†xğlLç…0í mƒJØ×¢ÜrĞ: F/¼`Ÿıà†Úä
ZöÃ•?}òï{úîÿÀpğşãëŸW£B[‡X¡ é¸ĞÔDîŠ™«½Ëz¼5)¼•±uoLo¤– ½^œ®í
Z. …‰­"Á+Ëâ`ò6’h©ŠËÊÖIÔ¶¸BYJôNqÎÌ¦¢m™}õ"Ïi	¬*ĞsûaºXi¶5¸‚Æ«ı ÉÕŒY'ÓI^¼D¶ŸŠAÎÁAFo?>Qoã1¼&Ì"Í÷Çv“ då–&:›Ùó^VnòşTi«DrÃUš‚ƒÍÚ`İ3¼ÀE_¡aÿ«²t‚÷ŞÿèÕ§}(RiÅÑÅÃøêÛ!‰#kåG’ê:–U×0à¯Œ‡\ÁI¼1õœÄËÓaVİI¼¨ZOâµÅ)?‰–Ãœ”YIºú](ó6]V¶¤2¯°5ÉhÆ¢‚-Š¿¨ÉeğÆ8­û"STG)¼ò'3*°ZË•ŞŒôŞQª­üÑˆ¢*õY¶X‘q§R‰æA[I­©Ö\MA¤Lí(îZy!‹Z¤BQDÊú`©™:c"gMŞ#ši3‹\ü±‹?vñÇ.şXeoìâ5óÅ-º¸E·èâÕwqq‹:æ5qT,Û!¿ `“ñI>³biKc¤éÀ¦ß>ö,‹²5¸İêØ[»üÄxÉ?ş—Y¡áÆkv
§Üofí-İRÌügv#rğï?åVè¿ø®ìàe²™¼SÜm7s}´Ş;<©o‘WÆˆãT—1oòíßPˆ¼ö}ô¾snÛ¥,–¢EªÕ3|e*Z¦Í–DrxK‚ ­I°bºğ-±GU®ö×´ùØÔôi™WùP?ÚAø×¨»WwmÈ¿ÊrCéí_£‘¿$S|ÿzƒÂW“ş)ûæï3¯{úg³7é2ïyzOÀBJÔÉäVI1*O"ÑÀ³Òj #é ÷›J=¸ïnJÀ¨ÿ l/ w,šÃË—¹™Š±Ò+>3Fã¯&,PñIÆ+¿—0FÅ÷5ş^Ä3?•ØH•‹{nÅ¯Sy&1„š6OĞÑ´=§{ÓÆœ¨ašX¢MQèíx–¬İC°‰Í‘n”%¬8{ôo2HR^Å¢9ç“Hã¤³¬ ÿOFæÆ¯4³iİ”¬'Š1CqÇ·4'ÛÆ.â)•üà
Eı ¢¬1kª…+ÌEÈ¯3½½[öë¾sP LJëxJ‘RêLQzğèÛX7ĞÌœšåÚ¦ãÀ°é—üTe ¾ÍÇd´œZZ»>ï(H»­Ñˆ¾œµëø7/üÕÃ¶e¬6­¸Hİ“¦g•ò	còª;Ó‰·®/Æ¹
ãüÜ*°—†sãK%›%òóÄlÆ5¾LLïÂ—Ï/¡¶ß+÷f´¾JNL‘.ñp÷‘×„€;ô¸%nxK¾ìI:_ËS—X–¦f-˜8kÍw KnF´I:p‘ø0ïnã¹Â‡d‹ìòyY6I/_cL-Ä`lì;ö¹BÑTÅ?ÀÅy’`Øs625	“@ï7™À}w3€"¸)Ú¨ûÔ«§Èƒ7ÅøOĞÛìsÓ	z$2Ø2FùqŸg’W¹-”»#‡Éì7cŠ1˜*"ĞÇ¦Bô]p@rJóL
qÄ[ÂÒ<·IüçŒ„h½b¿¬l^WaŸñèªôó§TNi'ÊÂi‹¥¦[æ¼ahËû¿æKñnëıi·Œ¨--OÇK¹nßmwác»ÇÆd²˜Ù[Î0ódcù"Ù·Õï)4RÜ~Ù#'¸úÛo”ˆío¥xõÂ)ÁÉ
ş`ğSäJÓŒg`ÑÏ’²@7v@¹ˆr kÏ*AeğŸÿõ\ÕHØ)]„BŸ?şXÓ+£V]§¬Í…ÏVöZ;SŠÑú™ÒFÂ™ŞS‘ø{h;Á‹˜‚WW¥wl?¼ [J;Ï 6™ÓF€­˜HF¨ÔM'ÓT8©Üø²/Ä3Á¹g64ïâæñ¤2íA½€Ñ&4óD;¥¬#ïø%2]™›ÈiãŞÙ%n–›×1k23	IÇ>/‹6Ñš²=É"ÉLœ‹CóÖ'FSdXtsŞÊ89±ZÊOã-‘¤=1#'İ4öE»ÄÆ©ïRRJRáÕ5	9]y#%I«óéba(q(.¤§İeLº[ìè§U;±^K 3{A$ m¤éŒ¾ş19Ì	"îèãÏÄWŒ1Æ‹éÈÒ/Àè-0^? ›û@ï,‹RE	,0A“Ò õßIX—à‚¬Ğ¸ñ=vªæfã…:É1#}º€vÚ;	‘‹äªî…¸ &Ög&àğ]pÑ\ùæŠ
s<6GxŞÀ»(<çq„® †¯K}P;+ÀWgÓ£@zë5Õ@„ø÷>r‡õU"nÈtnú\`©À©	¹±0£¡\Õœƒ's¤âüÈ{CÛõt5µ-*œ´ĞA´¢}¸¡âÏÅ×&*°‰fóùd:µ„Rï£®ÜıÖĞG;;Î¿ÎF¢±„Ï•IçpGBv"`+Ïé’3²…î+ªQš%g-š#ÒÖ4!r±eLRµà·×îï;Ğ_Œ!Ó‰XáWB?×†!†yÜÌ²ı \U%’±’şqƒó3 _é×ôÏ·ÿÕ¼øîX ×:%pıR×·M‰Ş–>¸Ÿ¬‚êõÅø)˜áĞì‚õ~cığB{ +åXŸ ¥ ·6¹÷D¸.´xÇRÅş ·l3ª Ç“ÇV™ÿº0ÃÓ'2˜eÎ“,ZŞ&ıÀÙ’eè¦¢“ã­)…•Tµ!ó‰ ışóytvk‚éûÈ&iÈjŸ…½µír9¦d·—,g£ñÍ#—Ï¾^wß­ßX+å0—ÓDJbqK#<F]ÀJ_fiH¾í<?OD%ñTİ°,K´}ƒM0•ï|:"Z‚Ÿ©YÅ×»:ìíIĞô9šc@€ïÓ ¡Ê¸º%û«áÇgk„Ï¦W•áÄC1EN‹ö"‡Û*÷Şg¢QÆœŒ:t¥ÃñïC™š–¯bòÑ/5¬cs&XVö™`òø0wñü:£gº²¶‘jÑ¼Ô‘©›8ËÁõtÜI wp|•Û`™lŠj1AË)ÂS@gvñ*<+Í¨ æ†˜Ÿo‰é=ÿñ†>ùêùJö°63,(u!Öo]‰â÷Õ—*¶UMÛº•@xoûÄ@¹Û*qšD[NfcôTáõ Y$|\¹$\ÅâHğ9=®èÀÈš.ècu[ÛV»}À¶úMŸ•Ä¿ ùt/í´U ¸¶4BôGÅøy&–¨Ë
d©Éë_ò¬íªÚõœï¢ÊŸü,e'İ¥Ø-¸ÿø¶1'ƒeÆ:ÄŞÂJ]Ò 6ï÷Ã&­ÄOŸ,}9 “fĞ™Ğ`´ìO¶}ø6V•ÜEû‘rPDùşD‰81§HÓ5Ò	#µ•H8¬M´,İ²$¾WAbãÙ’Œ'‚ˆoW¶Óé'sï+Ó—£º-6ùÒÕˆb\*ñ01£‘A€£#ÀæôÌ”Œ;•’¨«6Eyİî;šÄµBã‰µ&	‰J`-ghDF@}ì™¦¿G7¹<;ÄH›¡¬¼yäâ@Í©õÑX³ğ°;pˆü±WQ ¤³B§Ï‰"T?´ğmAc8ùYÍyÔ	YÌ5©Ú´	õ¶«€~Ç&ÁÊò½m©İáœX7\Ÿ>£©îä<)ZÅÆNàPí]e@5t}„µ	àa_€zªÏÿC*Q²sµD‹ÙÂZ ÖÚñXe_¨¿D÷üN—6^×mJD­ZŞÛÆùEj6k§/gP¢RÖA	Éš¹'Q¦ÔÊd}wÔòd~T_![´<iûLRPºômÃÓ£ïĞƒY¹˜/umnç§.z÷$Ğ¨ØÄ}t×£š=z}<50ÛÓ…wãJ+ËÜ¡ó¸Š×µ-NBxjvO4!ĞÕßõVÉ*ÚZßùö2WéúÃÖzº£Vxx×âĞ;Ü ¾^…]“J×êìpóÎí*şëA·z_bk’è®±´* rB/¢ø½;5'íM¡›#är.Os¤ò 5x¨BÁa-´1šŒ€B0]Û<ï9‡Àqæñ÷Ôó¹%;ÇV¿Pÿy9›˜­ŠVñhzRZ«®ÙÆfÎÛ£°™O¸µììˆoƒuGN|õ™uE°rT•xw<,ZâİÉ0KE}„˜#\/Ïy |«H‰å¬åT›&[Ş:?¾UÀ7Áwğ”¿l½Ò_“OU„s&“‰…æ 0€pNww›ÓåIÖ¦¯p©
éH‹“,h’{HUœ~YSÓ€êùÕ«mºæÎSÓŞv] â$ÿ€ç`âCç!ñS‚UÏ·›?q›¨¨%ğ8¶^xÒ©D¯Ò…ğ=Å{=‡K;÷¯u*{üÍ•	ó¢Ğ>jÃm€r ¶ñaáÍ Õ‘em©xù%²º”¨¤éD#3H­+Ö£-<®fÿ&Âƒ…ê÷Ü€'[ôMÔçÖv¡Gİ”&}WŸÇqŠˆcî¨T°*s-8Øê›qà”ñ£"é¦ÿ•„ÿµ„§W­¢ïŞ-¦ó×2fF„ˆ/ï±Íİjë!`;b\&Cí¦ñm|Õ«ø?¹•
´¡)†Şn•
“u}o;!4ÿK(~„ò!©…%ÕQY$3ï/Bó,æv¹:¿2T*°’c–ŸĞ†Eî¿ÑÌl«bÇıËQ§@ãu”ÚWu%8š]uµÂ×JÓ¥óÄ;Ñ™ÓˆàqYÙM$+Û	‰@m¤¦²ş¿SbCœóî‰¿>YT¶=Ø&‰‚ÜjÎé }láNdğ¹Bí£âhLÜh1?m%õdÈ|bÍ—`•EÆwè¿ï>ıÖ‘íÄ>ç¢¨ÛKLxÕ‰ÏãYöZÉÜÙlŒğH3Ş/›Ï??ù¾‚¢ÜöÙ·ß±ùm•¬ÍFT6 »ÊgÙÍâ³Sì#É¥¸Û'[{¿½¡Ö®§ä<Èbl,5ìPœ»ÌŸ;=YVÎ×RBKmFä>äÿ°jIµà+L¿df?ñz*ÕáÛJa1›Ë±1Ñ ¸(´ˆCz¢< C”-ÒğdÎÇW’$uvtGû{_ñÓqÈÎ†ª¶Ö]T ó¹«0JGËù|:éë™Î®pjLÆ‰R:ÉD›Ëû‘ç§sŸM•wXr$`;ØÚAp·_¯I ªô»¥°ÀÕÍö‚t4¼œg¯7Ö,B´T×ŞŞÙPf6”÷@y³\›6› sMŸ/æˆlÜ%Ç[Ü5|˜8ôQÚn›•;ï+ P¥Ô8E¼$İp¤ßÂ
nˆ6ša¤6È·IvsÏ/Ê²„,‹öMpßwfKg)mîH,J§W3c´°–“	€³ú´Ñ?÷A¸2é¯ep<xŸËRPÃĞ£fÙİDL»¾‚¿&-á 	¹c"N4} b¯µ}içÊİècöòá‘WvÒB°ğº*³F±©³tZf‹!Ÿ¸‚Œğ‹y¦ä*Cõ4­Hi9œ”é¶]nÎE!‡Ìr]â$õ1nüÄÆùÅÆXÑU7È0ŒåØäœ{`DhÑt¨˜FÏOÆ+İ†g)¿‘ mMÂŸ}o¿Ş{ş[;0÷A ,…Öæ@xàŒ‡(¾ûY*XŠ¨;S—Ğ’“W‰3ÒÃ êŒçD‡á{›((§Xçš­Ù°º€>ÇÓµn*>øª++Ààmº^Ó0¤¨8niEµ8êtTıÆlüb~Nî
»»½¡¶Oô{@ù})5É½Ø„P'ÁñGÏTv¯¾ 3ÍšˆjäŸ‰wã±¯‰·Ú±ßDÛ7cŸê¹.À·Ï2U»)YØï‰)×6ÊÅ/5l^cêê(9E¿Ğ<6E7ŠĞŸb¯¼	~lV÷ö‚UÕW‘Ä´Q—ìÂŒ„^mºµ×>Õg¤Jğ9YÌum\Ë™ÇçùÁÕs=Ş_-Nåşìx%íiÎæÎÆ“šj­*w¥öt°ò\G\“‹›Ñ5m¢ÎÀ&ve)‘¼màm·{×Ë¹~Â¸>ô¼y1“êc¼‚2â—sµ'?‡E>;W›a¹Ó´‘xo:<ğÈQpÛß›úA¢‹¨—)ØK:³vÁxÀÔ,îì‘S¿ÜVIÉ™æ'ZÛT¶/ì$åĞbUW•ÒFëêk.u˜ìõZOpD•µx¸’—›j…u´JSgkï„t*â6/²LÅJöŸÿÅ…Ìà%_	ñ'®
K73³3¯Õ0ìpp„n©ÛjÏ'Lƒbih7ÈGÛ r*õPJ4O$Øg#á ªûñ}V%•ÃØ)>0™ı‡Ü]	ı2FGe±œ¦Ä çıãË%1Ì¼Û6âV_,§!Râ¶A²FEY;:ğ˜Y^GÕF¢ºå°9=!W?£ø‰‚Üªc­È†•B"Ú«
€g8%™’-`bMÖ¬•Qâz«¯ÄØUĞl‡ áğÔ™Gægæ‹¸â8$ÏÑ[aYâÏ¸åù_‘Åcİó(Ï* «ñ™º0„¹M>x<ºÄéºc	!Çñ¾®vÈÆ+Ëñ<|¶w²B…?feÑï=VIÚy\…(” :¨Èƒj>r1\~ŒşWäŞïâ.¶ü¥ÔG :¹!xïq€"mAGDs|Û«,FÁ¾B©ı÷=¥,ŸçİÆóCs¾°ÿZEóâÿ„WpS~–¡1å&¿Ü}|Bˆà/“:¬€’l°³[^$w;ßÚæ&afŸş~(Hs·![â#ç–˜Ä¦ø|ã…ğ_?ş80÷Aèm/áTHİ0/sFÔ§+ ‹„/9ê+Ÿp µ¨Ø1<ü8 \70}lT
Ñ…6öë±·”>ËsJœ;Aö»í.|,ú"ÉÈß°OÅõ>Ş¾{óûÏ«›Û¼¾·úãİíİ‡O¿Aãg?áÆ÷¾ò¾>8Y#ç*ò.Yô«ÿ8LjïÆ%&ş`yÅ–!ûQÓ:(‰÷ôa¬e$Ş™3ªGâ½ù0«d&7f´”Ì‹tz"İ%ó™Ù0§Ñd^]A='qd¥ürr±M9@m˜“‘OVÊOÎ^L4¸²p |†½¼í“jÿ£rs³ÖÁYKÿgñ›÷5njËç¬”V]Mà°pEW9s§İô)Ï‘}¤–lã9Ød[)îxÑda)ğ#ã­ñU‡¤}Àv½	Òás’QqO)Y9zškÀ
ş*U9J* i#2Ÿ·º:â™z¢JœKŞ*"ÜËÅûì©÷yq#/nä÷åF^ÜÁ&Ÿiï6öåúíˆñeQ^yìâµUõuñÚ.^[“>…×&åDÅšBœ«›KmöÉÇÚ|4}=¿>SõÕ©'JÑhz~Å=¼¾·[¡=ªØMâm¸Ø^™ˆ›&U;—µnZ£­½¤1¿œ#µƒ*œŞôÔ$R¹„o~¤Öı«ƒ#ÖèíÃØBOœ\‘õĞÌÅ|.Nåƒ‰·
mÊ!Ú–Qq
Ç-fğ'Lé ÖGa1ÌË8+]K}|2oN†<”ù»:)b®{5íıêRA<Úfjö­‰ÖrÇêøm/}XP/­ü¬Tñ4µTçùA™"J[‹\œXßëı-Õ¦mIšÇy™O(×çÅ’ìcÒ'“¥n— #OévQxPdÜíĞVIEwk¬/'Ø¸T?­å§²xö’jF ;†¶»TàXx‰fHT¬§SxÈ.Å.³Œ¤sË…Å§ªÊ$óåH#NĞÇ»±!í¤‘xÉ?şwàíàƒ¥S_¹Q?å‰¯\ÇÀi¯ìó'½²¯KòÊ÷|ü	¯¿}z ¾O0yUÇ ò§ âÑ1¨"ùWŞPôğpøVã9åpH<]¶­3ôÊÓËLâõ·Vsxî­SrX®£”£w–ã«š›AnY¡ƒÍ( Œ¡üÌg¯72S¾,½Í~cˆXSô%hËšıt%>—A{ñµ·İ7<nX‡ï(TD‚Rò‰ı«^ÜeÁr„‹ş¬Ò"¢§”ƒ­¼š´FdJFÆL¬&íĞ©:9şW±|¬»áë‰„;›Cé€‡WS3úË—9¨©ßÎªØ²Õ„ªÒÅğJ1RYx„‡*pÏ¬Ñ™p]PıŞ\4`7Jd">”ÓXçÊÕÇµ“y4<°ıÌšªxğh˜®H«@°*cH ¥T!M3§ÆSà³[ h¡*çdW¿MR€eÎÈZ6ˆU·ßÆ.ˆ¥ÂÓ»BrñynO·Iš3£y,„ıHí
55Å¦w¢:Ù€¯$®¤¬ö’,õÉ8wPÌ½hZ}çR_½V+7Û—¡`yOıdeÅ/‰1péx×Š_f‹­B_±QŞˆ:²fR´÷{_*BºÚ|¡Y@ùÊ¢½yGöü éyfPœÛ	ÊVpºÙx¡’‹Ï5c<›×Š™ÂÃ¤xi<ûó«'ƒ_oåû“cFR^Ú[EY Ét®kDS^^œÇ-¾â• rìy5 í>Pâa6gÃ#êÇÁ…„Šm!ÕRT‰Z4/t^kÓ
›«Mt›W©™,ìÏì‹ÓšX\GGÂqŠşMú²º‚ûÚ-ÆÀM^bägC51~_EÅk*·Wø™ĞIøÏÏÏ\Î7¬Ö™ÎÓ¹İÊÑõS\¨ÉÒ£Îû¿³ 
Ã×È¡Ì„”h”Bš53 ·."š9¼Â.±åéTRi~G@uşøi/Š ²=o†”	¶Ê~—¼ƒ5‹½ãkUÉnÏ€RR}F§ß#®g…”dbb<], »åÏÂÉÙ¸IÙA’omË">qM%ÄœèKm9Ó¥öÌÙ“](üÿUè…ÈI|V ğ@ã—°—½%‹}Xé>Â
nÛÀ´§­(›1\HŠÊF÷l|	eÓQ2\_O¤–¹´ñ\ÃĞ…Áõ¶åù¦à&¢º[ãçÍ-"/w&ê”Ğ©D¤¾òe»Î‰ßŠ•%ïz<ö²œ²œ(·iõâQß æÏß£ã$Ê6–d1iÀI\‰‡k®ˆiàèhüİˆv–8Š¯?S6Š©ÕŠ“„A…G©Şò<¤²HYÊ3‡°jÑ¤úSóÕ]„	)&äùaÍÒsìÑ¿ã‡ü\e¸´Üìñ]ú¸!ÙzÚi½ÅŸ>Ÿj'âS]1ŸJÙ¿²ŠÏGƒıpÔ*®[kĞ¿å¬u•g­V}Á‡éË¬xšƒË1gEk\R*I[ÌMknM*ÓßA—“àâpøıÇ·‡s)d<Â€¾tšS¤Vyê›­&½É º¦éf®×©;Ÿ˜v ® fbw•ëÎ‰xş¿ÿoäFáÇ®»ˆœ>lVÏQKRk6¦¹ÄKãa­ôk–NâlG¯Oâ'nˆ³ãÑ^©WIÙ‰©µ/f@RÄY¬?3*AÜCDËï7‚|„oM[†¦&ÀÇ³PĞMæöÜ‰øz·‹’hTQŸL°æ@°ä.J¤¼=ŸÆ|şéäûHåbÛh‰¯¢%nuBy6&šµĞÔ]Öšz›.áº·2nËÖ·
Ø¦9]X£‰5Ÿ›)¹Ãé¼¼?íõİqÃWÂşª;•~6Äô¹n”¸¨9_ò–bãÏ5»gÕYTœDÃÚr¢M¼ì³˜|’v:Ã>R^"®B©É‚»ê+Ë±‰µ	$@³DzË]Ü-ú&z­3.n¼®Ñ‚ö<ìçèDú&Ş$u:#‚ÄéÔ™Wsc×r¦°7Ùà®u ¿Å«¦ÎâçÙË$$–¿[HÁ'²ù½o7êLïÉÌ´K}›ŞÄ °!{ÏÄ!î:ì„}.Q²>}a¹mL‡Úv9š´¯¢ñeO‚ğkúÊê7Z³)Fdª5SîâûôâJëU'VãU#üüa_x´9Ï¶<ÔÀ ¤k}ĞÓd«§ª6ÌÑt$C$şgÈ°ÂNQiÀJû3y\Ñ!¬‰¿óm·öç³€n[E£Q«ÅÔk}æÓLj`*–´ÚRC	4zõ¾c´W.<öÉ»ëN¹>‡Wj6ó„P”õ}d:ÔTDÜ?îX”^IaçñTPÍ¥mŒ˜ë‚Í3ÉP:ç«ˆímO„Szÿ¡_	Ê¬©1[@±1ÊØ‡YÜ¡+‡¿GŒEßM¨Ğfü¾bü÷=Ù«ÙÏÔ‘n3`S:MGÎ>õ?çFƒüá BÕÈ”ÿU]p25FæÂy‚U"çüù¤ŠÌ–Õ]”^¥d‡ÃÄÖhavÂxNwq£éõ‘âÒ–EÉ¿öÜo«ÌĞ\³æóêlàšÚ¯@Ã!¦Âoà"éıº}„ûi\µ	h7!2Ã¤JC¹Æp¶ÅnÅlShX·ô†TF½ënøÂ&
€S0ixxå[}‘Û-Ìõ	k—úî+<k" zÊe3"/ˆ@qõå$û«)ù|xÎa±mÕÔèÈ2S†ƒbØÇÃ=÷	;”Ü	XDÌöûÊ
­šĞLY1ƒø»1­:~”RˆÚ÷ŞÑ¸ª>m•4i£6–sm:_@·¾ÔëÓó}VÌL¢Jİ±Úm«ŸÛ®c»âS+»ç’•‰§².Ë¹XñŠ\µ­XDSÜŠİN½«˜yt–¾fÎh'¾è7X³ÍR6PƒÖŸ­)<á»CpÊk	+O¨vK}÷UÖÔ¨İ<;È«^Ê#/M†YÆ‘;U›á&‰§Ã<‹I¼ªó|'³8te#n”y‰v˜ğ¨Ìk³aÊ¹2¯Í‡~V´ëÒòxĞj6m“«µRMt‹BEû7cD´¹† ‹+îêQï—…ñÍø—’ÈëŞ¯}%ËÜ4º®2f†öá†
b~¾¡ÿóÕó±
ˆáñÂ˜â1`ŞÄõ’2ıİİŞvñï]@_‘×ôÏ·ÿ´ì)®’UUŠ•8×,¡Ş-1½¢fË/–sÃ„¶dÎâ°ñéî
óì#$hË'©RhóÑrfÎ¥r[b÷İôpÙ3aC5Moï†/ÒÄô/6wÉW–÷ÀÿÑ =qÒÚçg„<ÊßÏÒô4•Gbøî×t´JÎ­#2ë ¶ù‘£àî·ÓkÃ–vùp3º2t‘úù$*]ã­ñwÇYª6mÒW}¼Œáâx1‘kGÏŒ­Ñt¤Cõ9áŒóc¿ÏŞÉ	‘,wıÔ7¶ëÍ/¢#©J.\™ê:&ËV™·Y#óq_ä±#ş¹Ä-‡´pÃyå‚E%€ª“Ñ°}²kŞ³…Ty¾«ºí‚.3«Æ"„Ñ/w{#0}›CñÆ·M^ß!ó«]ôŸ]à­wß¢Z:dô•ÓjâÀÉÅ ó(l¥Úd-R¼PWUõé+÷“aVıÀuó a]HÒ¬¸|"Ä•§1‘3÷ló³Z9¬„±ÔxŠåVWyÿ…€Éc´²½jåx¿ôğì¿=Ûm¥÷ÌÔG#}„­+1uÿ„»pP÷è¤÷Ñ'Ÿ‰G”$kš4"&tŸhDÚCgAô«hŸ·+Ò&w–'}á“,zûcG‰*:0ä8 æbY•¶=4ú½Õ^n™ó…E¦3ÀÇ@e‚–ÉÈA”?V7ô'”yuCù>ÌÏZ%ôk‹á½[…âö'P—3Â<a9Œ_@ö;®"ƒ&¹sŸ¬øO	4È[ìŞ÷ª2â6„ñŒbœÂãØ¤‹j™ï¶]€[ÏKåH”Ú?â°Ÿ dN±el@f(|„™|øÃ&_•Ñç‹)Öp	[®%ÎŞL@h»¦O¶¤&²ëÿÂ×¸¯, ƒëÆ§QŠƒàš>%ã1î»äògÛRiÓ-gP$¥"õŒ5¡ÒÉ÷ke¹xŒ÷Âj·ÒÕPm›Ì‘62h§è[ÏÜ3àBåë*î]6.à¥Ï&`»‚M\¼D8B¶|à	ÌÔHŒ¯/®p†ZOÉr¹”¿‚º­iÉÿL¼u°&ŞjçÙÀÆ\lc~Ù Ã5~˜HÿíÓñ}¡ÿYÒ3¸Ü‡ŞûÊå¶Ğ&Ô‰lY¹ıª[Ç_@›ÃŒõVs)C]²,‹ì¯$ü:2­ùlÚêLS`;U{çì.2vÁk$“şé¯°,ñg6”oW6j%QnÉÎy¼÷(ÇÑ¿Waù‚»—–?æô­pxµsCğŞìıFÏ(»Byú÷½m~æ³¸Ûx~hîC:ú_«hFAüŸğÀ‘ãx_©Ÿfã(C ª¿çîxÍÉˆ%$Şa§hR>‘xo>Ì2Ìœñ0e©e$Lz¼`nÑˆZ&^/ÄKm„IM³%rb¢]¾„wÉÅ(ÎÁ¾ ™¤øBÅFY!”–E¡§&P¿+=¼\fôpµ¾n“ca²˜u¢æîEr_$÷Er_$7ÔqËØÿ;l‡œ½Ul‡b4[`Mt|±şÒ‡óKX pŸ¬P_¥®Â›äÏéº•oI‹HİŞ\¿ˆõY›ğ˜1ÍÈl&Uÿ&ë­¾cGV±›ô@üNË»½6]Õx}àşÊúÀ½rôW~t,í“ëİûe	»¡>¾ö·M·ˆ³¨öŸs-èˆ
YzµíÁÎ8$æ —|x‘”¼µÍM‚"Ÿş¾J×¯ü^ROZ§+Óo_…DMÁÇHÖ§Èò‘LÚ>K¹ÏP{ğï²{Ç9¹ÑU# ÙP»×»G±Oª§ÌóVìîUØ™mo³¯ê6ÄÄ·óÅÂ¬İ*
NœÂÀ\Èv•¥*ÍrŸ™Ü/0³l|b«6cqD‘ÉdF²¦I›9²˜N $—g/³/7ù¹\¹U¼ÜVWZş¥O…—9¢éD3x§÷µ?€Î(šZ<¯oŸÕŞÕ¹¤uZO>™oÓ<ê¢Eû¯EÏob¯	“Ä×ÈqØÎËk7øJ|°D|ú†QlŞªî¯6˜h\>P¯ÖP©ë!¼m°®¸…“š'‚À T•¾ŠuzÚRóâq<S±¿.WÓ~
ï&BÕQ[}Ùnsd€T(0eŠ æû’½j¯J´¯‰¨/¬±]ƒV/0CÕzéi£<4‘á?Şû(jLD]‘Uho/¶Şi™“¶×7Ş÷>ºÄØ1fÌË@'‰2	lÍSŒ‘=Ï4î[Jqo­äl™ÎG†iš5{ŒÍà}²Ş_qù-pÏÎÏ{ÙõÚ«¯ì(4ƒ9
@ú   ÿÿì][“£8²ş+µ/½Ü±½§/Ó»½§g¦¢«gçÑ!@”Ù¶ÁƒqõtÄùñGq1H	Ù€?tU52ˆÔ—©ÌT^¸rzŞãĞ)Î_	€jwÜ İ¡«vl=r<“l{„YC­öOhÙRè?Ä‡ôîPüµŞ£?ŸÈ÷I!Ÿˆ^Y·…Oãî¯½ûKñŸ?„‡Ÿvûôû«¿qÜÔ¸/ƒá(éˆôÒØ÷ 
<İ‘éèáª 6,PÔÂ9«ÀLÁB?„¸€ª×q Ò!s)ôÆ)Ç	få¾”3çÓ›í6Ûù¥¨ĞÛ·(©İKh÷İ…º“¢5ü zeè“ò8r–ÒXÙŠ«Rj˜M3K¿Iƒ9®;eìÀd}t/ÿè0]UµU˜\i½¸7ø”×ÎYÛlîæ'}ØÅ#Œ‘,ù=¤ÈÄŠãÇPtJ•‡Ã4Å^Fëä„Ğ)ñæ
®s¸r_®‰$¯ßĞZ$'4—R•ÄÑVªP
ÀëRĞäf <Z±”,ÅN¾|¶Ç±«.Jï^§ˆWnk”•ª+®ue9š¾vàğ•}îâÿ†íË™nW&¡,†çºÓy<Ä¿Q0gkÊç=ÍZ’ÂQ"£ó~"-êëñÿ;øèÅ‰”˜Q6ô<¡18uÍ:Ï¨uÆö„âz(<VÕÍÂ­‚Y\xÀ´™ÎØW„?W_}>¾Ë 1…Œ‚"ñ;fPÀÊy¨´T†VMà;Eñ°]”¡ùâ¿gÆÈ7¬ÁÅı9n\ÅÕV!×ÿ­¶æR¼ Ót=½)KñÏv“ëzÊõ¯…O›LdË‰²5Í2h5D¥éIãcªNÅMI¾¹¢j<MD¬®|Ğ–nB•â¬
,X•‹Õ‹ì·Ü> âl’¹›zéZ+Xïëı“€¸ñÑEÙ¾|zßM÷wU“šw¸‰=ôüñÂ[ï@^½‹w»8Â=ç$…%+PVCµ³öI¦®íÀŸ“w–ôËÈ6W6Ì@:_¾T—õÍåqOÒài¯Ì`e¸¸‘¯z¿ÉÆËß
ÏÆGT‹0 ?¿C÷AR5 Ûw,G:³¡(+Ëi*Fh4ßrºÌ8õ±¬Ìö‡ªiüô´…$¡&Œ¤¤`¬tj¦Pvï>Œ"f C™õC¬Üñ±wÃ¿|%.ê~ç&Œ&Ù´ÚIg$U/ˆHFºîªÀt!ÌÒ=İÙ÷â­O×Ï$´e¯ÙoƒÆo{Å¼òF»¸—š2ªXõ¾ÙsÙŠIÃdğ„xºµå:†°‚&*T/o,Ã²šøšåî·1 ¾5ªãzšËß²s8>zˆË1›Õ\!Ããb¾ŒËWWô£Ã&N‰Ó0=€É$ETÅ´UÊÖ|‘~Eãƒræñ <xäMll;lh¸3˜€ä C#P Î\NoOŞ®°kCcˆY ’HŒêw ğL“r
Æš,¡ùW<ãªß•„ÏÃzş¡;"E,Â4‘s¶ª*+¸r¸¢2{„
¤ñ~Í(—†/Ÿn¾¿.ú…R.“Ê•3§$»0šÈŞ\®gÑk}®Œ ×“ƒ?â•’òçW¨;›ÎŞşbÿê
›Ü'<ôQ=‹rüI£•)5tšn`ğ·"î/5ÆgÔ“Ô†¬,ôÿĞ=¾/,[|®À²bÀŸâÜ~”‚÷•¢C”]²ªéûÕ4õø¹ïIÃ‚åzHì¡	¿‰?·i(Í™¦ºíø+¡(üë4‹â N¾ÄgOä˜ù+Öb@Âô°Î<ÚÌ/éÆÕÏ©S›Q†aô´Í}¯ïvxóçKjuæÓŒrlKcê¥½)=ùÂ¸¯±/ÏÑë±˜šç6×n>sÍ«ÌÃ¼šlé9½¥=ªä÷ê3¬\ÛšgWšHwßìšÖ"`/®Ü•¡›”ºj‹’²()wt%eQ?õcQ?õcQ?èôìV?ø«3I­¦º‚¦¡CIšZNûÊqÎ8-ÌWy,ŸÄn¾@°‚Bñ;´·c´#æÕınGğúÂe]ÆŞëŠ€¡ŞçkÎñí¾„‹oÁí;Š»á]é?q*§ÚŠ¥Ú¬B¡Îe•5÷0{}ïó3“‰&wÍœ™.Ğ(¬©ÅUí¿
ª%o·h]‘ZHI— K"’1} CVXWÒ™¹G^ÓÏÖÈt¯˜ò°`¶#ºé±å‚@é¯Y9µs æBÅìqÜ;¾HDTğ6 xû[G/(ÑµÃoŸ?IIsU5OR3¾[öAò•W‡}Ì“g÷ »IœÆ!Ú=çŠ³u/ÓU5Ì|_åk,Šs\Yg»¿qã£”h>,eåğçKÈ<‘) v®V¿Hek0Qïa Ú÷`Âçği#Y¦mY®ÁŸ	Fv½“‰¹Ùë$û«vÇÿùõ&	²]ğŠ<YKlí?ášÊÿ¿ãÄr‚ZM @Å¢¤¬±¶ÑÓÚ‹ıI¤P jÀ:Ş‡A y·Tº¢¤¸,œTT’SW±]}Ó©ŒI¼[#Ùt@ë°@jDHÑo[_Yøû„~ÄW¤äBÇ±t—"°*Ç]şÖ¯+$NÖ·'T«®€lA×é–ªk:ê‹¬Ög(Ô$Y»¾éªŠGë6åİpÆ¦é…¶¿GˆÛr¼‹Q*)5Yu]/Ğ™)ƒµ°"œHœ•á`eòI-lu¶>|·)®ˆd?oNöÕNšj«<]!(ß™ÂŒ®¤GPÔÑ%7fCr"3:‡ Y<ğ#<¤¿%Û7ÇtC1æóùqˆIo;†ç9´ A©g’‰İcšÆíê#»ü ¹Fk°ß¯cd¦†Q/ŸA>×ôÔŸ;×-îœ÷U¿?]Œ‹ôF§‡\óÈú¨MZ¢U,h¼SÊ<²Š>ö{ #¼®ı€“Å’<ÙË‰&kô@¡)¬İ8Ù«¯³P|JqÉ2Úi¸Şoâ®£ãÎ=«ê\[Öí@êmšû"èF8f:ŸŒl#ŒãûÆ}‹€EÀ}P­T§tÅÿö’°>ôp˜ SÄ
×>5İBfáù
ĞİBgÕH™¯3ºá¤\›ñ½Ş¦aš¢[Ş)e¬Œ x®ªSÚRM/´æ%œs»“HGQ	EJ*SµV–Â¸ÔĞû”·qßá-ÏJ|T”R;Ïw]Å uªì X½Æv
Ÿ°'­ÄDˆ/€M?ºwù¢Ã÷/„Dİ¶M…R¹ø–ø‚Â\.ƒ?n!Râ.@ÓPsUÊ¹ŞåH&ÕçO!#9QúÌ"É½ï3ø‡:…‘M]ÿó™…}÷cF¤÷zÕ@WyºY'J¯9aHİ“÷¸¿Àz·"=Ğ·ı‡,Ìöì¬;'‰y£dÆ4‰¼ÁOÍÅMuCú´ñ'ã'"Qj7­Ë”E^²öëÏx²
Æûºâ¯l_¨¨š>1¯áê¡i¼N²
…]Ã•ü-¨ğ>§Ç¢$\ÛÅ«Şcğ¥¸@c„,Wª‡•ï°›yf]@QÎ1föJexÓv]Ï¥e|±>O‡I?Ô'ó„ÏÀû~·ÏJ>şñp€İ²ôŸ˜r˜JO~£MòØ%-,ğ‡	fùãb¸Š)#Ã·¼ÒÓ“h4chşx©ÕMÛ6]JÙ8q1ÚOj —pø@¿/U2rÈ&Ne¢Hr“©N±
ÍÉR>¥Ğ‰¬ÃZAGéÈHÑ¦zØ"!ş¢}TN*›L=İàª)ÏaÇü&zË½úïaºù)JÃ4Äyøzİç1?üGfdg_zE$¡øcÎÌb®,ÔOİåÔ%9C~zû…r‚õ™=‰i¤bTŸ}NL9t_d‹I§İ6êúábŸCoÓ.	wN–ùµ¯ˆH4˜†¦(\>´ëH´ğù‡¢\îÉ;NI®Õ©?¶t«Ïe‘qÕg‘q“qe_<\§—Û£Ô¨hÅFˆH4C×lËcö´Mm™M¯¼¯(˜n&¸E¾oqéšÇ6¨k^)X
£ıR‚ÅõW£ Ù9sgOÂí"€4Ä§IHõ}y¯C‹:ÓSÈB\iŠkø”\ ibïœ­Õxƒ‘¬¬¥³èL:‹v(’yÃa>•`á¡…‡&´3sÇ
ıİÿ„ğ[¶H2b=E34•Áöz!âî(Æeâ4$Ù«ØQÃâ£™Â©áŒCğ­‘x–+à†âèª­h÷`¼¶ÌÕÂ6¡íSâu¯]8
­îõ’³24ƒ]Á¯D^_)á–êê!ÛP HaJxFcÂİ.ñ!]_eiæŠuùzRûC)y¢Rr’öyVwÇX²P=]Î¦‚7*+AÿÂ-p·Pîd®fÚ”2áÂü'[ÍÓ^×¾!~TyuM÷k–Ëp³^á;.ñØSdxÊ‚_Ë¦j`èB‚ ‡ô½ªÃPÚYëeX\µ}˜G “«ñĞ&ÂíğÖ
yRÒ,ìØ"XÈ©£+5]¨eÁ?GWÖè5Å!µvq±Ï™=ëhì$‰J´´Ğ±ÿI5ü“Q¹–y×2¦©XÎïãíñõn±ÁûMìÿx}w<ô®¶KI§£èº©áô9PÃ©C…ÒÂ©wâN
gÌGZJ¸Ô|ê2-2—C	%aº±ÿæÜŒ¾Bß…OGgüÍsû‚íy¿“K®áÚÜ›"…çI§JE%QÚ©æå¾u^,5*L³3ÚkâçÌÃˆ´ê}×B.¹%`½ïWH«3÷Ãb­×ıÆŞÏ;JOîR°iÓèÌN$gÒ”†ïô\yÈİzpãÜ~&ß³ô6aœoZ9¼À~ßñªûÎ ;ÀU×ó”ÿÁ{·PX“mAİV¸êíŠ¹íòöÅ¤Õ­L—…#q–ÏŸ«¡Ñ!˜òµc¥í(¶@vÄ!mGõîeç¸{+Ÿ¡£ĞGŸ>~,Cø¾RË0E+?]cPe·¼¯Í™³—­ôÕ	®\ğêäÙô2Wõ!bÅ­êwà/iuúü±ZêHëiºT8;£—Ö¡9+ıô„ ÿ¸Ë)q÷wêß¢È_E‰óK5OAzÓ{Ùâ¾a{6E|{`xÊ†3Ô9¸+ÏŸĞo)z¶ì%šãù†Fñ]Î§ÓT%¥¦ÙhŠkáÙÉX}É¥x«o Å „i±u†iÓ =aS€Òœ«Fˆ:<*úùâ]8æ±éÃ¯‰/Øié
)Iİ”`¯8{êDã½æŒŞs\Ñlƒ¿nı Aÿú*i\A_qÀáàN ›fåÿ&áæŠ/–UF*ÓğŸá~Ê)†­i¾ïë
E3V§ñ|Ê$¹Ê<\¾dJeãÒ4ùhî)–W©Ù?±†·ùä-R†ïÃƒw<à†îíZ8âÑÉ†å¬´€èÇ!DÆç¾Sã¿A§…ùñ†‚à¤4JZé*pş>6ÛéºØ<ÆGâœUó«ˆşÚz	û=¢}½Ùn)­E.“ĞªXH×Z«%¡õå$´vÛ,ËÕNo_|N,Òƒ~ús'_8Jêcô¦rÌ) ¹&ĞJ¦`÷QlŸã¯”SØPåÜŸaãt™\õğõ6Œ¾òÅˆŒ©¨ÁöBŞ¢°Ğï|\>3„Àh¨8é.„×eœInX\Ã÷5ã=¨ÒFŠGQ_V`òûn¿çŒ>>;—”j“i®¬,ª¤6 Ê7è‡Â›ë{@V\ºnn'H¥<Iÿ0lGâÓ¤#U1á!]Gú”’)¤aº¥Äq.ÜráO»ŞÆ×Íˆø3T{®AöB‘ùsjĞzŞ/Ãò”{„dô®˜‹yP	·üœ#ëBÆ€i9öÊõù›·ĞTúñÏKqJñè¿˜»ÌŞì–Í».øR@&YéÃYé¶Ü§D¢ÓMºa5>têôÀ™D-‚Ü:®xSI »L&É	tñÅğ}ƒ+é©¡‹SÔğbÄáè¼$ÌÖhëRR­
rín'Zµi“Q8Am>~4¯†¹òKGU‰1>÷x£¹ê{§7u:»{Õşàô¥±İ}5¾?@äªÛ„>ÄRñßq}Îa"ÅQÀ´U!Ç	Øï“øyÈ± ¦çDLˆ™Waê,ÅR]\*•®`ø¿Ùn 'ÉG±ÍÀ€\ù CY`b¦Íşl‰­‰?ÄÉ7ø”²æBµ4ÅV.³f‰TF¾o	•ûóZayÌîmä¯(2sNº¬ç:óÊÒKŞ/ÜWÇ†Âq¤ÿğ#â7RvÏ|?pø6×"¦ĞÖà0…ãÅøÄr’_¢Ã5	#ûòå“”ƒP[f@Ëë¯—×Ğ™®f.F‡šæË0Ì•e)E®¯@h*mPdÙ)Ø53nÎ¢¨1Ö2^ÂÁÃ&à;°İ³2ÂÜÏ…†"TÑ%?-ŸXhñ¼âcäÏ=»©ÓÁˆW€[éãÂ·ŞW‚¸Üa+%‘G ê”KÄ–³G­7LB:/jÄx ÉúsÅÅh­^2a¦Ò£#)¥x:ÿ¹svùC¸…¸$ £Ä0ôC°&õ‰'¶¹ÂŒGÂ‹4 «§ZIñ2%Ûøäl
í è†rj§Ù–¦8”ÖøÍ43Â•ìfV˜lëìÇ…ùkdÔ™ÂÉ¾œÍê%€º­–Ú-‘Ë¥mÍVS¾›‚D¾ôvİtƒßm¢|ìÎÙ2‘Ùºd¶ğ2[ß³”@¡è“SB)“+šÜ:ÀD}ÙÀ”zòa«š¢yã·JYM)Åÿö9À˜æZàOö
ı®û5gxNÓÆÜî³èÇı­|ô·ñ“Éå(ª‹øH4»=²ò×`¿_#j²šÛã‹°™;t)JV“Ê¢²*?©%U©¡œ˜×TL-‹i8F!ºØnÙQ‰·	ŸÛ†CuÖˆz!:¼Nå[¿Fï”×å‚¬®6ãƒxÎŠ\‡c°\t¾*dù’¦Jx>3ë× 4Ì’ÿ)ò’ï{ìÆ> º¦b®<Ã¡”×@1RËGfç6lâtj¹{j|¾8™Õ\ä.'ölô€h®U1C|VQ*¢ËêæJ1][h@¸=úp¢Š,'S;VıLQ;æÛ]Q/dÎa®°îûÍ…Éí¨zİöïdøÓ.şox=~q4×0C¨ËáÂ/¿ŒÈ/;|ÍkNÿ¡ûú2”Û3Å
¸ú¾WB’Ó6.×œ( Qö+}Pş2“rÒİ´qœîv}YÕ
*ÆR]=T¬·û¥Òë¼÷3âE9õ‘±/}£°k6>óu¾¨„À´z]ğ°Ö…Ù„>=„Mz{>Ñ¿Sâ÷{D#r²u<Mór°—G°F>ÚNI›…ÃšŞAš([„C§€ÿ9oí±ÔUèÖÈzˆÊÙfí${RW¶®¯VºP¼ã¾±	°ùÒ¾\y=?!ÕiÔ1–Ò“-ŸÎ“¯¨¯&^{imCê†²¦ÜÀªM9‚šfÏ‚7Ûíµı^n¿è‡"Ğ¬•NIpš&6n´/Œ„~±wÜ!=ñí÷¡I‰ÈÕm¨%¦‹ôˆ>l€fZ­-ßë€HÅTäÂ\§ß÷“På
²İª~ÇùÊtnm‡Së«9¨PôŠõŒ¨ø=DàŸáoÉVN))ßSU¥à’ ,L¦a)dHj4¸¨hJã=„ûOaôõcÄR€@ß[™gBm@:‰İ,#@ıío˜Æâ6!ğqù~‰p.0€îšçóJ2Ëº#‘›İfi÷"ãD¿Z„¾~®bDº¾xEp›cU"L=OUG§Á¨ø±wäñ/CBá±`•§óµ*F…HPk,öÂ¥U-Z¬^î/2µ3/Š ¹HÍ)ræá	<·)zö˜êÄüËßOhûìtàîÇŒì[¼jÀ©Tëo~n@÷d²÷w¸ì°Fnİt¡,o,Á@v‡³¯Ò9s²6*u–aL–Èüì¤Š{q[úäñ'c)âs¬İôc­WñˆÒs­˜‡
T´Îèr¸Q*qÓÖ­•i>K·ì'
h1½)ìß7Ø?‹µ‘aPÓqØãì¤8÷Ø:­$è6FØæís:0ÅŸËHÙ'˜RtVñ¤)İ_¹:­Ãå¥µÒ¥U÷¼·{ÄÚ^O1 Ê‘DõÑ"ÔjåAßñ)%·zA•2‹á6Eò8È~P-}îö´³<fzç9
]Ñ87Â"9Ä]òºÏ‹O2¸’êÕ(Çz­åaK  ×ò<VáÎ¡ª9“yÎ¿Éué3y!tI5>ï{’ı.Cå«Àr]Š)ÙHÂ%Ïœ²FÓì2[qÍL^¤Ÿ|8f•ñ3ù##³c¥[€Vá‘.è¦L¹"„CqÁÄC_ÎÊÂÊB‰£*ŠµR(Uw`¬›ÄÀ÷ÀİùtŸÀS÷ÌAœ¬óúBç†w¬ !Ò5•~¸ñ±}ß¼ÿ„ñCŒ_ñ	Æë=şyßO,©Zušn×K¹åëDò–hã‹æ-!Èñ5ı¾†Kï™÷9Xe¥ÒÚ‘R†e0ï™JBïºT2XÂÕàO„Ç„ÒİÜ5§yÅ«ƒ²bJQŒÛ`g ¥ˆaàØÀ·¸Ò0û,§]š#òwÊ_åsø´AÆ|Ş 4Éş`‰Å,Mo.’vC“œæGû&ü©/÷Ğ&9İ>ËcüÈcÄ/XRJ)òbY>ô„İv§ZÉÂãİ0¶Pnd=lâTJj jĞ\1«öä¸®P>¡=şwAØÕG˜ìò­:\oÒ¸Ú¯¬©
´\_£9á‘bG2£)ÀìŞR¤X±¢ ËSxe"L7UÃÕ}ÑøÀa7†°ÿ"ë±í–"Ç8Uğ¼ë'J9¼Ñ×Ô¯wLKsmW<z¡=ş GˆÏ2ı›­[ 8ÂYª‰ #ÌçK,ªã:°Dµ¤ÆŞ]_g;Úd+ß6ëÁôôñ¬_Öğ‡yl—-Ú°@b‰°ôUUÈêi°ˆ¬¹ˆ,ø'>? á{8¥SJ]K€)İvÙ9¥leq=İ$Ì‚~}2Í¦‘e†– §|™!‚i?WpvœŠ‘%å;Ë×ùèöMÅC$H‰ığ)ÊJ}H‰®1TÛ7GTèf¹C9İ5«)z1tŸÄ¸"umàø¼4sÉŞÁ<íÅáã£æŠ]š£x#Š²3=è?d#)AiÍúÊØ@
vş`¿UtëÍ¡(’ïëmì4kqÂ“9H×ôæz$Ééq1šäÙA^¼Û£0ı¾¦=%ÇU˜ßÍ98®+Œ£Zn>f«a€ã‹Úı	0øƒ@NÑÂÍáÜçMoñïRJ;;´<ßV/šF¢òY“ğ7û£ŒXv±+ŸQñ'¬ØÅv¢Or:géºï†M9¢+óÈªŞ_½Lñ?=Ã*#f¬‰9T7QrÁğ5¾Ÿåè5<¨×Âˆv­–õ3>Öá søl¹òâöÒ#½§ _äÍ	6åF÷Ñ‹·3l9äÏ>©ARËhMaËX€;¡‰KŠ!Aj}M`{À…ƒTE’ïª¶YS`÷u–Zş@áÁzG)ŒäÔÂ†À5}O§ÄšŞ^õâ%Ë¤H¬â¸ 8-…¿ÛU€í1´¡%ßãˆÑï¾Ãn:õĞfn—tXúõÕÇ×<¬äZŞì!>È»æf0Z^ĞO,œb‚'¦ÁrDD¦B=Oô^'egì{2ŠYÇ’\?oc4®d–	Ønãoë=ı5"O2	_ÀÍ”ÕèÇCƒãô	ö$Z4#˜Ç4(‹dØn|³†ç¡5D¬ŠséÎˆÊš&çÈÉ” 'hèÖ?Ç¾”0Pèû†Ñt	|ËD7õ'!f¾Ş/ÉRğó}Œ€—†eŸ”SU¡­«pAÂ§Tå»ÎÓ‹Å—£GH—K¿ÄÿIB­”*Üš£xÖŒŠ¼ÁU¡ı9„ß1Ùã¤Œº‘ÒÑr n”X8Òk$‘ï&bÏÍÙÛP[5)É+
´'4q!©Ù‡åAÛä?ê(Ùxš–J
¶Èwc,<Z»¸qCüÙjËÑ­1Ğ¨ßÈqÑ®lzPÍLÌY–è­=…óC·H“Yc/_»9g|ö8uÔ`ºãéÍ–7z¸ÒN×’/v¦¹ÀåÌ>lSœš‚Fµƒ,.fESk	¹˜…š}vÙxìQ­è°‰“t§CÒP´$»K7Ç{æ\Ñ7<=ü˜Âİëò¨ªWØA1©8H¿dÆWµ%–/v£üÎÛµÔÔµ”³\Å×³PQ…ğAî²ÿb±Ìè„‚7zNLïëÙ,øg’QJË­òs!-?$ñN’ÄV+K	LV¨`)yÈDa Y1ÎŞ@şŞŠ¤§-ñ!Äï-.kù†zÃ¥1
ßsOæ³`i,,Ño]¬‹(è€_ôàEO–#lËÔè²jM0¢ Z¨¢)E‹D»$
{Eqâ–UÔ"ç¬8È)ÊlSw4nïS?¬öQ÷³öìÒÚ'#'ç¹º÷ÑŸùÑÎç¸ ïŠŸÌççĞ-€ÉÑcO4)‰iê+èRò8;²SW!vpf^Ñ6lª½‹ãÍÿºö²ßY·ù
¿Ë¯/|:Ÿ^­Äe†‰ìYÃ×HuzøÊ²f±+@®ïOÕWõV@J¿ı÷qbU<oŠSù5ÃNwêòˆf¹¨³W+ÅĞƒuãkŞÖ²/j‚i¿LıÄ°Ë0a	Ãzà·õbÁNÀR[é±5¸‡âw™©g¾âšP(ÊrfTö
÷luíJúÌoMÓK< ™];©Ğ
@XšÃHrğâãÙ&²•Øx?Ãú¯ñiWÏrzUßxÚ»]¥g<íÁõ~ñ4¼Ö)ØÑ+òõ¾}â©O–Ö#^nÛt|Î™ã<¡tDo0D‘vô’û[¸úñÇ
ÍB$dci:ÒşªMDñ“#è}}Ä§µ¿Hª­Ù†«[:%Ò÷±ı0şæ2oÅ¸ßÁ;Ÿıø„ÄG*†ÏT(Á½S3À)”€~I‹Šˆe½ßwY‘‰¬›köû‡8©7±'uÆjEŠŒé+Õ°}Z+ÌïÇÑÕƒhXö“f\ùˆ¢ÿ~üõ—»tw½ŞÇÚª¿Şõê‚ÕzP;“¬˜/Ğ¥NQª•åwŸ
2ÂgèÁp/ÅÇ¤+ÛW=V©Ê¹ÄµŸf®x–[ÚŠ;çB×3z&NBúÿ8fÑÇ(Ò¡Èµ<;P5¡ÒÂğÌ[í!}÷&ê/¢¦ Xéı“©ÎÄ]ÕQÑ’^EŒcJrš•+…sèÊˆ¸ öİÔ´€‡=2‡Z€&@s§õb¢
'àÖ˜\gBÌ›p¿G¿¬ãnÌçâ¼CÖ0úh¸óªßá&á~v¹g|Æ,×ºÜRæÜP¥kµİ„\×Ô¤(vC—­ÑùÙçÕ€=”"}8T¬2H¥ÜÀíYœû½D³Ûö3Pi‰¡;a*ÿjkRWq®¶Z÷¬6/ò¸U›ßíëSm?sšÕ¬åÛ‰?•pÑÔÅ ™¦€Û*;+ı‚¾G˜¦è&R7ìÀ‡¦bp) e^Yî€_{ñ6ÆQÒQ¸Ãzdkü[p€ÙÄï\ôÛ:3Z©ğ<‘—İ: >¦nüçúì8J-¿Ó‰öJËĞù;ØnÀ¡ßĞo{ü[k`9¦\•r,ÔÍşëœà¡,ëUDå¹uáÓ¾Ì#~Úßî+€hÏ¦*-¾g:vA· oÊ§ß®X}¡ä(†fxfNtZáO«xŞ‹/ŸŒÂÂ<ë§tÑu¼–h¯}¬˜6ºŞWÊôôS#s%¥Ì°”^hª-!w™ÙJÚ¨ÿ  ÿÿì=k“Û6’ßïW(ş°kWfÇ$%QRrñ•×ö$©³³^7©«Û”Š"AÏ”¨ÔŒ§Êùï‡'Ÿ HBĞŒÆV2–D<F¿€nÅyàœ‰°’ÑS.gu—[ui‹ñ¤™âÚ«TãB”¥u­#ù„}QíUtd÷<ü­·ïcïö—~ùÇÔÛf$¯‚–ãUaL­€³­YİhJ‰œ¯L]Â¶ uk¼ß§»$ğ=ş÷</<	E}ïû4âtQz+X¥Gá4sP@Â&©;-b¤İ6¸ñM”ûW£ÚŒpø€ïaØÙA×²‹£0Ÿ‚M´ßTëïZ-ÉepTÔkAkME›=+ˆOíWB is¨åWI 4 ÙhQ3 —ûUæ§`}ÀV[ÕÙkàİx·:1KZÔè%ÊÒŠ¦KˆE[Zû˜ìö;]ĞáÆôƒ·„2¤íh…³hUódk£ÉZ{º€|¹Ï¯4q#ÖÔ Ğê
!×ƒ!%81ÈßNÏ`ó*d°PØW
=w6µëªø!"Â÷ûİ:õfx¿>
¶Àgc5“‘AJÛ'¤>vl›Óa¨dŸ±Ñ6•H¾¢ƒÔ]Ûg!İ/Ó'Oš¿OS°õÛ—k[ĞZÉBÈ|Oj6ÂŞ3è!o­Awr’[9‡§cHêÜoí´=®wé]ë;Gå8³`îL8ç¨î÷ôRÆù÷A3êCÜ¤Zç4˜ƒ™ås2¤uËoÙ¹VæTN»=äÀ‡Á\™†aÂÍÍQ$s•ıİÛ~zå¥:˜˜-‚ÙLrÊc¿YFØp«
*òÎ0`d$ØÛ®wÿ	‘Ç[øù=ü¬%?®·šúÓ9Ç` ¤:^ú	Ï¤¸êˆéà_GaFğ5…ÜÈ>€:HSZ¹8À`ls´÷6q4
TyQÑ(İQÏèİ»§+º±öñíëŞ„t6bË°Fúğè=ÍFZœaMá”Y.YÄé‹ùÄó¦râD}‹,K1õb=.M6Ëkf'’˜ôQğ>4]êäËÑÄ*“x Äİëº®dnYa0D–ã«P"CÌir?ÕiÖ1Ë®7]¸S‹“T=İ?¡pæÜğ!t&q€BÆAáøÌò]©Í'³Àò8>¦ö=¤[À‹	ßy²šÙ?^"ÌÈQÅ½"%‘äudBµ¤KôÅt>¹µÀ§8üÀ©‚£§0ìŠháùó·İ{q|;"×äroÒU’'ï½üê‚]Ç{ô+şEÅééØ“`b·¤êe”]¯Ñ¿GŞ‡Éowı£WĞƒ.lìS–÷Ù‰*İ }GøÿÔvÓF	_]^¯1:`,?­ü üd³KA–=Å½?;Qº£˜C(•”*±E3ŒSñnåÒ’Yl²pW–ÏÙ@¯	¼²×#‰6.s}®É¥ÖD;„/ğ¦×à„e\œİId·çjl÷’µÑ-¼ú}ã[ø}Ÿf„£5I²uöÊ@†FŠ’†Íön¨¤=DÉ"CšPÓjªXÒÂÃ}côd‰å¬kÉÒ1·ı™5æäîŞ‰Ü‘SK®¥œğCÉ%»ªŒçN˜T¥¿*k*ÂÊZ}ÙPµŸÓd>ƒf5é ç-D°(F…¦³0÷ÍB:öŒ›ˆRŠ_Rº8óùszßï;Dh¤^šs˜L#¬ V§›ÙĞK´±×VmöA”Œ<ôw¹ßBÆÇOÜ‘ˆejğ‚ )ÈË"SD›÷ Ke„­˜Ö»Aò>A½­Aû%úı
RìFäW(£ï!-³ÓŸÜæiÈÈ’åÑj!ÙQ’Ü‹‹ü(ü~ğ4’$d¢"Ò;B)E¯wirâ[L®ÁvxøÖJF¨®p®‹åì6‰.ÌÊ¾yq^\ÓO£&d@@ñQºVî„÷y´´ämvÂy÷ğœ¾k3ù¹¯Qr„5ïÎU†Ò«d–ì°¿L¥™àæÖÂ¯ÛQàÚ÷R]‰ÎJşVï‘ãú¬vğ_ùœ#›O¸HáêoÏ&Â24n£„¿ìó<†’2VÎe7¸jš|Fy©o—Û$ÂÈ÷p¦–.Ó½˜]lÊqÅe—D±€(8HÂ™;ñÇçètë-½ô~öryKD²<~ Î´W¹OÛËØK× ¶YqGÉlãÅqGIâ
£ÅÅó‹‹ğm
VqE]nV*ö_*ÍgÙ˜v¿ËD<±äTl½Áİt1ñ‚Ê/Ì¹µØËsÏ¿ÂŞ)(Hı=œğÂ5„Q&¸ÀM&Òpk2´¿‚ÛWåHŠÓaà×Ïki
ÿ³>;wìxc·_¨Ê¦Ã¿¶4¤˜$Tâh¬Ï¾»rçcËŞ=V—H¨ÑdªØùÜŸL=ÇŞ9šê£O¦S{<™ï˜¦¯#ã¶UºŞt2ÆÊı«u»ç“p² ÑšZ×cÈ× ¡+ĞÙ›Í.W
C›†şÊuæ
hÆ²S‘¨W+Ç
æsÅN)e¹JÁ{°÷ÙÂÀzôş®Ùõde°×B‚¨wªvO—ô! Ì¶³šõ¡ÿ_±æˆñ?³ÕĞ¿ZL,ßu¸9´øŞBåB­Û™?ö×Z(wK©Î^¨õï ßšÇª8WëÔsgâ,X
¶şÔ¥‡ãX“á3;Q½ïp,x
Ü¥èOµ;Wì?XÙËRèÿGNªÁ~}ÎüÉÄ¨HËú˜gµş§`8áXau½J¶P1§İÏ•x
OñâÜjB(4ÖÜk€©hKpIR‚¥¡@t!©Ú”ĞX>I$œ\£1ıàQ5wµÂY´ª`= jH3ê3¦öÎ´‚èÎt¬`/"šW/kFH“×
Ô‚¢Š;…Üge)èz&¯*ŠÇÁÜÓqóFÜ¹ªî3]ù!$©©ªÂy€¯¤©× \çÍÿkE'§&’¬Ã!*šÒ
šmÛº@ƒM)Ò¬`2S° …¦h pU\{t§…1QE'W„¶åWÊıë j 8ÿ´ QÜ.fK…ÜOLÍ@êq®Ä¹ˆÖç…¿²fV àYn 0UtÅø«Ğ™,&
«¾f¬ÚcEF7³Æ³˜¨ËH‹[rêä0¢¨44(XíkåWusªêÿT>h…”}–öñÙıå/´©óêöš·% ÆÑ¦öm0w,a«CÁqy[$!j¿z³õÓÛÊ™Ì=5PÎƒLì—¢!Ñ”ñ:ÿPv"´èÍ –¢kACÑu»?G ®åR3o¨¶~©·Æ‚ÛUã
£,SÀœ	øL•õ{ïFp).r‚Şm ÜÓY¶¼ò²+Ac•½[Eñ6‚æĞ«ŞÔ}¾‰6äŠlÅI )ëó¡)Şk¡¦ZwOpGÏ7»É“áU´˜¡ø>ìèUïÉ¯ö›UvîÏø5‡òEóë/…Ä‡ßuµT;]U6$8tÅÊ2}™çi´Úç€œ?óê_[«º^œ·Æë-œßFwÓYS´8«§Q3Ø§ø¨“±ôuÿµÍš'ó^ïƒ‚B¼ÖÚã/VétYªMJxÒ¹ZèÂ”
ëêÙÓûÖ5HêÂ¿:-a]…ö a]kÈëº°®#ç@a]kìpa]k®SX×JAXóÛ?‚°Æ=OÖk}Âº;_X³Ø[e‹OçŒ° .	½ş–Gî¸Â9FÆ¿[#8Lú×)æn¥?4¨Kü^*ıq	$®R(‰íZAË³¿ª&Û¸É,¶¦<>ÒJ`/«¶Ï¦B¥„*Ø~F4´ojçQF—Ò
¾¡.‚yFÒ$A
à³RÿÜ{q”Ó[¼±SHë­C`6cœ¸l‡<°Xô·ª¶á)¤9
ÑìÃS¯ÔÄ#Âiäpà¼Å²p%P¥^]¤Ôj›ˆ{ë0~»ƒˆühçms^´^rÎÁ“WÛdË;^Ô[&érÅï*BË0Ú®AºK#Îqtvàù*Âñ4×†HÚUûh^£Ø>¿Z"LX°ä!,Ec.ä°«R^$,£mw™dŸËÁYFå¶‰¼Ø&ß¥h§–íÄš!LrÂ‚ÙU’æ¸DØÒGaÒÑğÊKG†'|F¡çkĞ9!¨a’šc‰W„¼Ùp™	X6é°+´‡Z£%ü¤Î@PïI[@xo¶ê¹i ª@|(.¢V˜ÏæSgÖóT¸ wº;ªzŞ›º>èQáÀí
×™Ùsoîh¼2®eö¤çŠúà•'İ[Í€oy=·‰jp ¼
gÖ8ğzıÍ‹Ğ]yâÚn8*+íu”¡˜ÅbºÅú6°ı™?éyŠ„‚*¡­œébÚó-ûjıNæ¡,&*³^¬1ÅÛ¡L|•éÖ{÷OMp|áúÛT¶+Z8ÕÎ…úw-t¾§ÂÍQ^q\(N>*P¶ñ;(S3tånG²¼FğK|‹ÃRTD¨œ£àó.J©ªÒ|O‡ÿ’\ƒInÃl¢á•Ô*jƒ˜AüpÖc½qZ¯ÊKÔqD.O'/JÀN¡â(……(z'¨ÚU…ä·/F¢<ıBÛYD6q•âÁ?€]Œï%İïà‚€Ÿ—ü…‹/Ç*0ûÒÈÛ/)ü¨vJşå“Kå|b)¢£˜— H7“pâÊ‹Ğg´2ÿ
ûX2£1?t¿¼‹*\Q²a‡Y´â¤O(4Y4¤«ˆ“ş¹€9ÚòŒ¢íâ&ø	ÒZè7Áok²)ÙÁøı>‹¶ÈÊ¬ÌÔ-í”'Øš%+€Ç†BÒNÇı®Ód¿ƒìƒ)¿a=Rœ–Ø•,ˆP¥.+fÉö¾‹ş´Lño½–K+ É:ğÇ>ò?-	°mçïÅHaù·p¦·‰p‘”·²\ƒ4ïˆÌOÂ0†)|O¶ÿ Œ\*HeÛAùÂËU‚ò|B|¿ºş§úä£O\<@ÀÁ#E!„Ö-dC1­R¿K°é]â ü6	í×k¬Z¼‡D4ÊØ·¥tãvëeÍËjä‚¬+Œ; 	‡F¶ˆ0Xn•ı²Ê[¼*qââ½$+.”zÛ6z?¾ığşÕ9µk¤k¡ô`K´^¹«åØ½>ÿùWL€„µIHâ]ÙIx¿Æ’;—!9gäe[yˆ3WXş'/»zçíèEg´ş‹ÎL“Iú2’²¸äa—Ä1‰€/z—´€É"> z’Ü•ø^Ü9vÜ"ì>NÖ‡{ĞÄº’ÛÑ¦Ë›S)ù.ŠãH2æ»tÍ±uvå!—ôyWGÄŠµJ¬ÒSğÇÏÈ…¬>î*“A…ûH…+9Œpâ1»{,Qöñ
©e€2mØà[İ‹¤N}´ëüòi•zP¦^•Š(áRE”E¦ƒ\ÛµdSÕHøKÒß¼ì="cI=:I­Ñ`XÚøÎ
9,Fˆû˜tW‚L7‡fÚoÄß0ı»Ò|%­Û`ØeÍÊ¯ªÿ:€ô>²Z•ı,ÓˆPiı$§à/küU\{‚ÿ‰ô)¬a^RmŠ«buùËÓ4I_Æqr¡…òàûTúÜô©PØXÄ#Çµ|qòæˆªrêh‘ã%©u(M^öéÛ\»+°©¿Üúùï²5Y¦#&Y(KÔÿä}÷ˆıd>‚Íy²˜Uû*ë¥Ñ5RWê†°õªôÑ<mì´Ò2Ú°ƒ¹Î,¡;•êº.Az"“’8àú"™«wæ­ìÅÂĞ2j’aÌ¶Æ–k¡SĞ$7èHá'æâ5™¾ènTŒ n”†)Êğº°‚ ğíAxínÕ·+0™
w„­ò£Y«‹…ì©Ğ+luÂƒ)"=×õm¡CY@²&}Ë]-\Kè±•,¯ó1˜`*Âã47;¤–dV¡7na	pÁ¡f~óI¸XMÄÁ®ÂI³-ÙJ[Xë{óÁX±˜±;3{Èà/ˆ/‚l\Ê;'î\œl¥«iYËîÌ],VCÉWÊÂyàZ+aÌ£œ‹ËÈË+×[b£ÇÏ…0“‹„7ÊLßî á@·eœ!œ:À]Íz1FâDe­Ÿ·;’q‰éÜã…ßKhtuÄÏÓÁ"é}0áŞóø¢ŒïM×_=q·øQx}¢î8d k¹.Çé•ÜbÀHa‹Á3Ñ˜©÷©İ+Îx<bFØ§v¯tƒàáfè­~ìƒèÇšê†Ç’®Ü®u¿Xh†¶x<ºñ[<¹nz†-Ê4®	XÌÂ1GOîğLû„ÎšĞ™î[<í6;ˆÀ\íóå6_½²™ÂLYZ {áÙá£fÊu>gî†;ÈÊ¨·.QôDU†s0A3C{v¬Áº‰ ™ş=Ö¡Ì,°fs{â2c ıÂEZ Boçò¦Zg}Ëã_]9Êz6¹Üâ’¥›`gÏš¾Ìª“Rgş	z>L¦S	ÌƒÿsdUª§¨KäÓì«ˆC E—{q.e±w­é­Shmhë4TÜ‘Tn,Ki;ÃÉM/9æÏ­hÜ"ÄÃËŞÑ;Èİ•‰ú†„W5¢|«1ÃØûó¬_Yšº©ğâTôù	Á‚|ƒvş•GqöD[B}qç¥³û"/üèËnìé×u¬jll69´±jammih3ßî3nk˜€¨fhéé“¿ÙO8œåi²Ï!XìŒh•rÙò¬üDKÓğ"ôÊâı¹	­]üT6X-ÕX^°ĞSÊ¤ LÅ²‚Ÿ«§\~Ÿ¡ßˆÂ#âyEmÔs±0D<ÄA¬Å`»†{6z1r«Øô¯¼ôeşÔy†@øëò¯¢¶ĞCN¾P^ÉÇ¼àqÊzsÈ>¹OÎX·‚*íøc6Ú5›6ˆ¨ÖÀ üzZAÂÉï¾ı¦¸ˆdãE[|ÄbU(ij*w˜}Ô§¡eŠ ¨³Jõ`sô”ÇAĞ–8Z$D0Ğc ¸?ñŒÕ»¬-6tˆiIÎÌ ¶~xÂ"½Úê…s¢ÑÛ®„z0<ÂğCÙÓH¨ÕbeE6Va¹Ñ'A÷xUĞôåK7bØY´tıïïÜìâ®–ÿûü¯ÉİxCAĞU #Ãf5¶˜»iªúgìá¯íêƒ.Âä9òäÁªîoøÇo¿íkÉÁ2B·÷{Å?ôÀ1{®OÃi_ÖÉ!cDş7NIB ä¥ØĞÔ¯^¼Ecö€ÆĞÀDËÉAi›²]î İÂÏËnºo>yz;°F9z|ƒ„âçm >Ãÿ¼ÍÁ¤çøø%üò´xÁoøpBäYOZ¨>q5¤½÷üOçÈ+*y~Aª2Qg%è»üZ±øŒÈÇ«4¹ñV1Eëm’â¼:İ‹¬lFbËˆŠ8/[Ö€±ôµ»Tw‰ºÂ< ÎU~?‡Ä!ÌÖX}ĞRkh¼kµ°_d´àOMúÄP?.ı¤Ê‡-~ÛkŞEr»Mün»Ôªú7,Iê¢¿ŒŞ½¹¼|ùã›%¾köâ·×Ï
íœÈoŞ,ÔÎƒ·îŒã©[|oLšäÀÏA@ll|meE¸kè%Še#÷;¾MPœ
´__nƒ6ıdù›íu”&[’òCœ›£	v«Ägø¶İ‡G:Mg!väˆH!s´Q¨æÿUıñ;®®uèšÖe—‚¦fš‘‚R
Éul¬:PuäüÍŠ•Ö±}€~… Ñ6ñ×K @wAd˜¸0Z¨ßA@¾=ùòÃ—'ğßâæ~ô~ƒj1rhOÒÚíšÕË•©=ª6é[´æë“Éq®”Sù¬5±#ÁÌÊùã0Õs˜ºÉÇ,ãÀo_şÏ›üŠU3µÆ‰Ï ıÂéÀU{e`S¶¿0^8Q"µåÀy_YÄ¬ïp+%b„ßıÔÔ4~ëlôüG2HbüÒiğõCnÃŸÀíËü©'‘iuU»Ğõ¨pä6z˜AG³U’ãê¤ˆ0èÏsŸ´ ù>³ağƒ¼Ä6‹uÔr×h—+ÀƒÔµfwTM+”¸N¶[ÕŞÈ_ü/èŒn%¡A­@;¯m®ä0•í.ÄÃŠóÂß‹·jŞc=ì¢d¾r+Úncpé.—`s©ØüÈ²ÚP}Û[İ4ü¾£lmò‡Nn1§l“sø\²SÏGK–§µ÷t›|ºop{!õl„×§ÅeŒ%© ğŞsM7qã©i¡½¨³$
4gäicvE+OÎy{œtHb|U°&_<Õ!_¤¯V‹V:µÕDÒ™Àüy¨F;‹ü’v£d‘ˆEP~Ò*_¦eT™ÄšGP¿d±µ66UÄnB38÷†„¤æ€5¨ ©{tBDÈíËuİç§——Ë‹ÿx·üùõéØbe€¼Mçgt¨cŸ¡å0 Âä¬\ªMÏj‹ãX¬¸]™¦ÏšOë}Yr»DÒ\ªíÓä ¾Îœz®¤‘@ÆScæ'ÅŸIê'<ã&%¡œ2‚²M`ôh€s®’1PNTª¨ÈŠ?y«é’C„‡š¹ª”EV+­*q/m›c™?Kß¢.Ç‚M’Z«I”T²pñˆ„Õ>/D¦íJJ±’½œ~cª3”ç„Ú‡u†R×‚‹Çõ¿½«8fu’ƒ¨vnSä{Éó˜ñ<G™Å2 y}5ß«Ü>ğyOŞM|_]5‘¾Ğ)WbŸS«¿+H6)®„¸I"AØ{%Çàİ¯øÂJÅ_’Äç<°(Q‘ä‹ß}Wƒı`NÄ7KqŠEıLÈòÑÀR.¡G>iTRâ
§ã¯8}5^
áøŒêRC*MÎğÄİµç†¯T©\å¨¯©¡¦ñ÷-Éú¨."l”*F­¥İ™HÇvÚËrãì›ğO<+(zg¦[êí/Ò¨kK1&ëtŠ©öbÁ¥S6µ»(¤•NÄÛ+e”´§æ9¦Ã]
î\Ke1:5—ÂÃõÏÿPËœ)¨4nâÆ8-Tœ=¢!ËÁsƒû±ã1Gá¹w'…hœaƒs/7™šñvoÇCv<<Ü=Ç»rVÈªŒ!újj˜qtG‡qtGÇI;:ô»&3MîšîØ¸îçxš±÷OÌŞo$Hff?µÅ|GÀ	n¾c/»íuµ¬.Ìğ.Ó­VíH[ÆÇa|ÆÇÁoÖø8NßÇa<'}Êù+³æ±ÆÔ”úµ„¾µ‡&˜™ª†`¼	Æ›ğø¼	ZÍ|tıŸ]>dl|cãßØø<\ÖªÇÆ7·1¸Û1¸Ámncpƒ[<4ÁÌlp¸?®Œ	|ç&pıâS™Øå«Æ¾§\,ƒM0ÙÓ/K“÷*˜ÆV”²½ôsÒ¨X@â›öF_„‰gx¯Ş½yıóK­Ñ\Òm*0“Œü@td¡Ö+`^İpH4B5ûy~zë|½µ¸:…-bæZÖjœœ;ı¤ƒãáa‚ÍP>Ø},=oÔ¢©z¥ÒøTçUˆ‚üœ&FwÑVŒN§9„mÕ6ÕhŞm¯Aš/™ÔâÖ™ñ8œ3ˆ‚Â0¶­A;2º†J–»4ñáZ‚Ë*0Û å‹Ôo¢açEÁ2Û¯× Ë¡’cyÑe+-¬8…ó »'¾ãVoÎòİìâhO(ÚÏÁ7Œ1Xõdy¶ôĞå@çÉº‚P;0¨óÔÛ~¤Ş{ª,ËÊ»F$w¼¼±x«xøª;9” EŞfØår*ˆ“ãõmóWŞz™”ååKû!šÎ¸w­½·ãivÒL2³Év¢›l
½U4[Í:b¾EĞ^a[èÆ±»—t_Oæ6i§|ú_§É~U"­B•M=ŸÖ`ó8gpmòéO÷ ‹ñ•±Êı4Â@-a·¶O$âCYş.®™K8|*EÂeûHjÃŸ¾?ö‘ÿiI8{i5÷÷šC}©šÂNÃT@>£
!eúWÀGæÏÇ·Kôõúz‡”Ê ä“ œ	8	Ë ÄÑ5Ho—{(¤bİœË‘ŠylÑ×òQ_€îéáó¯ºv{É~x¿ßÃ$‰ve©ë- Í	í‹Ö‘nÙ~Õ°[lÂ,co»ŞUÇûö/÷‘µ,õP¸’¢ëóğG*3S§¼•rÛ"Rï(;ˆ=?kø‡‡T^œQ7ñ€JD~á-R;‡TrÏ*¾ã!gg5²ÒVéO®öàTˆ¸Uƒh¹‘µØ`àÌ°ÒÊüLì”ÔĞ‚ßPÎßâ#Õ9«xMUF„V^c*5‡Ãñ_ñ›“sì3„OÙäFçŒ±íX×ê;çlš„q'Ç"{xÁ5£şäòAÍS5æPî˜ôSwUÙ­¡r³Z_æ@éc<PzpuI‡ÏQ3z¨Éµ:
ÛÜ®tÅ;¨Y.ñ“p!µı:İvmê(y:}uí~Ş;º6¥—İQ¼rš¨Dîbk£QìtÓéWkc²íi;3M3j{³~¾«L¯7¬Í6¹²£¹ÀÔp«?!¡mkÊV0™:¾Ü©9^Yu½=îã•&ß‚¹øÑä4ªx jôü¹¿ÏòdÓªeNV™“U_ÁÒË9¢Ó9¢c6”Í†òĞå~û´ÚwÖÚXvs¹…ÉÅa¶Núwa67ëú?‚SÎšhrÊMÂÅj2ŒSî«uÊ›ñ±›ñ±(ãc3>6ãccŸŒóê±:¯ŒÊ8¡ŒÊ8¡Œê¸N(İ—É¸z<AÎ
¬Bo~9t%I@ëx}4éu¿‚´²Ê>•wº¨&æÕUÁ^½Âs!)d.d1s!‹ñ¶|=GLLúgE‹ö¨6¬¤~©è1÷”³tYªÛô™ê1}Bk6·'şü¤MŸ÷›ÈØD¼ZÆ&26Ñ›èQÜ£"#c‚Ä˜ Æ9dQaÌˆ~fäL×‘OÕNæƒ‚–P°+|Ë]-\kvÿvÅ×b#ï$®9%«¢Á#Ÿ¬ßk?éÔlI" RM}‰¿ë¥Qrò	ªÇs©—šA–Šæ[÷NS$Š?¶\kìœ@zã„3N8ã„3N8ã„3N8úÉèÆÙeœ]ÆÙEİûØc=*´7óVöbaÚ¨ĞF…×5*4¿¶Q¡¨
mT£ õ„Tİ
££Gaœº3w±XÂhÆ»Tà7‚ß~M‚ÿ+kì ”nZá,ÂyàZ«Õ‘eÜãcâD†Vªşâ¨ûÌQß
…-Ú/Üœ‘—½Ş„g»Íİ[Ez²ä»:âpA®™€3ÕŞ;*Jx8qçÖä´øÓ`ªN‡o®Ó/§ì“3|Z‘O_üÆµÛ†™tÆLøJÌ„CÄQ?‡Ô0gÔW(ûN_ÁÈO•TEX¹ŞÊ;-‘iDÅÁ*½‘F.V]™ÓûbÕğSkÚdÀOxlnİc“áqn¤İJ¡I@ ç˜åŠ×l½Í°ÉÒz¶fè©šb:õ¶[\uš8ø¼G˜Üì/qŠ@fß
Ôë=CCÉHÈM®r¡ãrëSÂAt2‰vÇ÷‘É˜¨œ$¦ë|Q=Fõ4ª§Q=êÙ¤Q=Ø¯‚ÑøàôÀcègüMæ0TUÑ.LîU£ÖÕ‹µî«TëŒ&a4‰×$–^ĞvxŞêG›¹zîÑvoœ”h;^ÊÀS¼†[·Ü»ï+·;íâw-ƒøxÃ7gÌkDwòRQ
¡ÆLƒÒ*'vİ*ÒJyO4…$Ëµh½ô…şè9‚PˆÿwÊ{3onYÀ:¹ã4Fès„>“\¶ôR°„ıdÑ*‚ÚFcĞ’tF–Õ»Áòxàhró?^uF:§ük‹‹ÕaªîĞ_ï~zşİÇĞ„†”—F‰b®£é=hMOV	ÖáË£-aèzï¬x ú©*·Ô‹,Ïã:)”0:òw¨cÍ±xIÈÕkÁ8pæÓ‰etl£c×JÛ¨±F5j¬Qcûu:=RYìÖ.x×K°[»²æÓ¹ë¥ò(•F/4záİé…Fßê¬ğøô-£+5‡£+‚öbë½Ëvì…g‡î©i/÷ÌõãRu¾¶›oÍİw©=²*ópOõ§>úõ½§{Æ‘ÎË¡¿ÏŸ³#pPEùúäŠXãdô‘4çè<Ş
¶
Á¢ÊÏkRºTtş±ú?à£8´&¿@:Fî÷Í7L'ÙE[¸¸„¯É
Â×Æ	Ë\Gàf&é~³ô2ÆàÛ="y„‰¤õA™';V•ûƒmWÉgÌçÚ…êÍRÇt££ò“ı6— Ì'ë.Yú;‹î’8^^'9•Eˆú%É£ğöäy´…\d‹¿.3úÛü.oÿş:õÂœ‘\€¾p«†I4n©¯NM¡¯·^ÇÉv•æÍf¿ò[Qhó"f-¸ï!Õûû,O6Üa3Âx¤˜fÙoÄÅmFÙ[¥dY4®‘¼§‹Œ®*Äò^•"ñiÃ,hÉÓ›(÷¯FO%EĞã{ı¼Rú»VYô¤ ß§[ÿNjñ¥z»ô\€3Vë ¨Ş·'{²8¤'X½»'2İİ4*ŒJõ»ö;¯CôçŒT`=Øà£ƒ	º ~ÅÖ„~ŠêMpëâŒUİÇ±Lk¬x]9•Ù­ÒïaÈR€ê]¼¦4d×Yk}UÎYÑu­[¾n\«ÀCÔå$ÿp/ÀÅ«ùâbvqF5¬cÌ¯Ãu`I.¨Ñ4¾ùr]PÙmÂğJÒm„VÔ„!˜jiƒ+×”Šá®"êêÕU…š‰BM…eHÅ…œõØµœ»ğSğ·r¡Ö¤î.fÓàv…õ/ØYU)»ûc.¾ËŠi€ú‘ñø¶yœ3ÊšÔŸU9ô€Šî™€Q+yĞÒTV¯pçÎ²-–Ü¯FwV)™ïÿ  ÿÿì=ksÛ8’ßïW¨òa+WåÛ%Q}øÖ“ÛÙJ2¹ØsóakKÅhóB‰Z’JÖî¿ ¾@¢A$Q6=UIÆÄ³_èn4º9°qUâÊöª‰YÙnuÙÚ,>õ­À"_ŠºåŞHXˆ$‹5’ëhZ5#Œ½Èr»ª®®¹´ÆK§wXƒê:¨®ÇT]½³üôNægĞ;½óõèƒşøJõGc¶Ò¢?ZKäºcÏ|eúã VA÷2T@şbuĞÿıoĞÿ„úß »ºÛ »5Î¢UwË¾°^¶kLg¯µá©R¸d’÷õlôÓ>ÉrHD5í·JÍÓÆ¹É(?|½Çk¥^mRàDç¡Ü/VDQÆ ­óJªÔtÀbºt‰º„œJÑ³¤Ü?!¢CÙ ñ©ÍY%Õi…½å˜"²LCXÔby3^-.;,Š^å&i¨tJëòåÊ‰Z+o‡+Ä¾`#í™ğûwOV‚éÓº¿³0™‹‚ğFrpÜ	o:`ûåüTc, ¯ÃıêÿV®Ÿ_cmùrk¦u.*Ëì’ÏaeÎ'Ëù±‚íÌXUKü„&¿zs÷şãû‡÷woúÃ<<‰W[FrÈË|»Sƒõí–¨UÇæÊ¶&îr|ì”oíîÅfñP
Ä)ù3cP†a½ı6K|µA›0z…¼Æ&Øï‚+$ıã?@D@ïTñoOÇ3k>1xüÓ‡Jô¨ƒ~Ûáup_?ãI¿£Ûçİî=›!6şgÙ¢Ùº™Cò+ŠwxGŠK¿Åí‡²DÀy¯(Ë‘;ío©†–%¶ŞRlÁÿË/ßQù.AáEˆà/ÜGŠßr2À½uıØ²ô7„ß,µykèúòbNĞ0´–3{LFµ-ŸG®½š¨Î¥ ^?¢äù†M<éš–éN-€'ïi±Ÿ–x„pË1åY¬ô>õ×
ğä°y	ŒwŸÕ2Ë0ĞÊ¢Š‡=KV7A “²ì¥5¶¨:ØOÊa¶ÿˆ§3I0‡ÿ­ÿ¨óüwÑÂ/¦®Öó¿øš„‰Ğ6üËÔAI¸<%iÉ vĞ*ŠÏ´ŠŒ×±ğÿÙ„Õu°ùd†æKc	°9å l¦¡cÑt&Aö³CÏ‹Ìô6c€;™Ó³p	ÇéÖ.•ŸsLt÷§èjmFQ×íXÂ¤úÎİê"ÕéÊôæ+(ôD8ìïÿÈO–oH ÿ^}fËÀv©ÄY—ô7Qd=¿-ñÓz6Ÿ6±¸¦ÿĞH +{²0gİN Y‹ısâ$ot~zÌªÈ?|¼“¦Ã+zm·úW¬Ïşá~4Z6¯àëP!Ià…´˜s+Ãu¦Şp(ŠÊ {I€!©FuzIÎYŒ'æ
"9JêZä@z¥<*ĞŞæ‹æÔÂÙHëoŒ£±ÌUºñ7˜Ïw|ƒNÒQ‘5>à™°„š’I6	©³#Û!œcæâIáÉÏ¡x$iÜYÍJbMaöÿ“Z¶£?äyŠª=ë1˜˜/M	”®H)ùÈYº®+	E©œTVí”;‹½
‚£½ªÔ¨3d’®ÊvrWÓqäôİIÛÆUQ\o”¡¦@œ5İE'…úà+>wwÕ’_f"Dúl©‰yMÀu W¹´Qˆ¶NôLkı=K´ò¿s¨rùÑ“?1
Êu:ùSª¦IñÊ†×oÿ]x^± >æIÅÎSœN<µ¨¤ZÄÖÑÜ2–®)Ê¹—ÏR9Í<à(Ël¼»oü§l6ÏX:®5›)O‡­Â¯Èõ#L½
3ƒi «(b€zÀ9›ş)Í•ò~IÆ’ó˜Té5_Ä	H6ŸJÕZ«•çX3O‰œtİ(!k‰fsÔeÆ¯™{í3B.rB¿¹ô$\·XœñÖp¬n·äÈ<rtİeëkƒ"4’T„ôZeB}Ô›†#SoE´óTœúË.ˆµùŠ=@uÄa¦…ƒ«ƒ{sÔK1[ÓÒ5qfn0™5©QªÂ7¥¡ª"‹ÉOÕ€= ·ÿ]µsÕÀÍ<úìûíûÅ¶¥=Æ%g}ª]üïµx0¸êcş{ŸÎ\]—¦i®fKˆ‘ò[‹¨¤%ƒ¸ÏxÍ>aŞb­ô)X…ŞÇFÖÖ¥õvÚİP.¦/®ÕÏ•É(`´ûpËVFƒc@KlNFÓû ˜¦w‘cA!)~|Í)Uæ³·Éé„– *÷¾RŸê@pÒpè“ïâ3ŸöÖø¦£"b>íƒÄÿD*Ši¹C·Òÿx¶¬¸o]Te’ë“Æğá
Çâ¥Bê–ô²¢"+t¾’ÔAˆ³™g-,°û{âÔ9œd#Ù“ìuœN÷YucÁ×w6]Y€Q:0öe2öÀ~#ıìGü"+r¾ÒÛ¨øKûé‹~Q½ÎÂ›x’A|*S~××,?“'„iö"ˆ53Z±˜qô/®U¾)¬÷Ø—wU™	aìRùZÿá+J„8!²„Ô-¿cJ^‡§Ê¼mìò€Ä¢}?¡íş6LòœÚc6ïJ¥E—ûµÕÔ±‹IS¸hu“D‹V§dƒE+_TbE+eCEk³õ3RT97†²CôAv²qı+Ló’ÅOa”¬·Vı¶ì¾9•d3h÷Wpò3îrNôã&GI'	ËëL)ÊÚcOKDÉzÎ)4ˆ±ùD§s––5q\@Œ]§á6l2?8Ãq]r¥Wı«kËÁÊ«¨k=ëàÉøšĞäf¹…¹o
* z"¹½—bm
èAt(ô2®*¤¦ĞsrR\ïÅVI‚'t…|»9P¸¹«±»t=à
­İEš‡°KÂFÊÅ^Ù§ğÇÚßfÀ Yé÷­mcl•·´Ì[¬]?Æ ÷7¡[m;RZâ¾¥ÄH²†K^]We™Ê$~®¹%q2œdÂ“ 9É;
úÍêıÄä(Á<›V-°©§pI˜ãŸÕµ`«áäîÑÉİR¥`4…fW<¿)t7¯šØ®/*ˆ»©+,UîÑ¡¾Ô^ÑÜ€JãLîƒ§Hì#:ôñK®mÉ=Eiô*å?Ío_rÛµÃ„…-İñÕMæüë>ól¬÷½F—[ú§"¹ƒ—'z¯u<&µ×¦ÒFëá.—–ét €øs˜|
]ßó•Ş\1“OÙtl8ã.“ë¥º*xúÊKCØZŒ;u§HX'Q~ÔA¡oÿ9]5&˜­}›i×DQWµõ~Å@¾íc’2÷ô¶Ù¡Š]‘mPA—Võs4ŒM`uÊ<¯+Ö²è’¡>F€,Û–B­k\údÿ~¤wÌ:b,ËõseóTKL±màoQ6Y*1uòE©Z
öw’ÛHÁÜìµ$ÜDå~Aö¢R4?o,ÈÕ(Hçc½—$zRxÉF)è“+_S‡Aîá‘Q—4/.R²pğjğ99‘•‘¹q7şö«ÿø„O%²âµE~±èod:¾±Ï-ãû&3€nà¼‘Fûğ+™kÇLH°:$)·êsi20¤£êÈ„X'§Éƒt1 ø‘H4èÁË!ƒèYÎmZ.şvøšŠ‘4Á‰ğª…ÏtV>’^{¬FÀG5È j,Ä\‘Ôu'Ó›’z€-Â¡pÙEhãásÔÃXı*H©¸3Ş`í%-^ĞQJÌgàê4Û¹Oø–›Ä<òXé² ‡	ÜYÎINŸÃ3€"1ÙXlqÜ¾³õµÉŒ›õiÀ#ƒM9–OK£icygåçÈ Ì¥órmMS‘¼¥Ìt5É[_µ»VıÒ."àíT™;‘8ât×ÔR¬i«:M¹ÕÂ±Ôu¥ù„‹í¹ìÍY S£ßı.ÿ•š(æéA]¢O®xD©2½ªòú³«Œ•Ô»šWœ(b [ûÔù¼÷'sÀ°[Ù«
™áiÖqÅ¤"#gº½„ÈNyéiëmÕgípV–Æ{:]ÙÈ›‹îKÎv`_ìÑ;‡ÃqØ2Î‡};{†3BdÏåN)gZh¼€ê-1àµµxä<æxÿøˆ—\8î˜kUº¦ûu01[=ğp‚OŠ*œ”b]ù<à;NİsÎ OMb×ğÒI\@Ö ne	ã¬ìI'55ÍÕ*ä•k4·±å#ËJ<cî9ÎÄÜX½*Ó\İzé¬Ù„¢Wë%xtĞÄ|å™+w¾êò)‹9‚ğpBRÚ w/Ğ}HA[’×H8ÔgÖx>T0ÕI*cvÉ¨š)i¿õòkhÉLæN&·t¾vj÷ğ–«4ÀvTŠ%ñ†ˆuˆg_P9+µÑsÃ·}4ETq0;I,7+EUÿ¨?Uï+9ÅÏÙÏ˜)e5=§AÉçw%EÊ>º+éSò±ÚÅqNŞ*¡¬U3€´
ßg¡Œuï™jÚv‰SÅg^%jµªå0¢yí'G}#v#¼«Ò”9~K¥–‚‘ó•‹5h L©Äù…›55ÔšßDƒù/óÏN¸ÙYø{Ã$“ËK¨®±Çü	Ã.¢÷<YG²Îb–úÛı/ŸGÉÂ„HÛñ°ÊÆÚVâ…?]äW´B,…ñßX3nØ)Ég¸¶€¸Å³¤ã¥¨l’[D–ĞUß‚ù@›ú,®r–Pè´l¢rÂP%ÿ™‚æ|°ë£*,SZ‘™ ¾Q‚ü%—kt¬°2¡›Xj.D:ÇrÓİ·âS ïTZÇ£^??YşVãÉè¬ĞØ@À·Ô~2ùP;ÎÉ5œ8':q†#@«„†åà©$´’$ÛE!qšh”böØ]Örª1sıAªÙ'tÁÚğKõµŠtØ³ÊicüÕëF(¹“›İNKÒû™ç:¦ÊRÙDÖn³}_®ãXøØtÍ—Ê¸ÔİYæÛ»LåÆ ÓxV™Ó¹±´N)›oîN¤—70À ™_
OpñxáJ½‚49»R^·KYyïovÒ(.gÆÔš¢E'Õz4ÓwÕ„q¿ã%µ´mÍ}z¨lí§~ª'¼9äéQñ±B¤ı—Ë(Ò.î®>tøuâëöĞ«í7–'³J@:ºë:9F¢hÆÒkÒ'É”íŸT»TÙ$åMP¤’ä7İ®ĞÌp\ªºÜ;ê¸d_˜~bx<1L&öÜ™@ùAŞÙóS’8iØ…’×9¢êù.¹•U©u5µ½ù|áÅ2«’!ªÔ’šßØék„ÅuÂâCj…Å]ë…Å=®–fåáÒ-ù÷@Ñ¢ .´&õŒYÇ†Öàöårâ-æ6 ßKVvÆÌÍr?ÜDEÀ#ÈÊÑüı—'İj„+<a,=aM3Ó0€<b§a^ÑºÈƒš-éç»Ñ&~5Eî»‹S>á`çq°Ö«ú3¦·âCÊ¼ıä7å§jŞ”rRd¨ëef®óvæñC	Z«®Ô Û*‹‰17V@IÌš¡ÚS”Wœ/Ô
•T•FRóê]„‘–û-s1÷¦–4ÓşÀéHÉÜ,i–‹>½EXÎ-4‹&mÃb„N"3?*å$”<dO¶s³* JI ƒÃŠ¼k‰@X‹ér®GßÄàw¡³ßukğ	éø:%€Ã<H:ç‘Í¢â )ÑY@ôU6(+±ÊÁã9UêÔYÕtÀ‚¨kTRèP³O‚Q®v¢X|$zcEèiKÑÕE3¯•†c#åëÂ±½)ÆØy´U„³V“±í.,¹r ~¹„ŸïºU[—Îx6^(OxO
p~Æ§·ö Õ£Ö¨€® .§êú@<Á=6ğ*„ş>-»†cÈµjÉ˜uÉvë™äÄ«l¬%›6aöÙˆ
vĞDK9_¶¸$K¬•ªŠJÚÅÊtª
ªnŸ›
S€t¹à¾o(n,nJ?‹›)ò«øà‰ŸÂ˜zœÈ§xåš|y
“p´#Š-(7û¼XÔfÀ<ê [k0]k¾˜Ù’‡xú0èàú±+Ós\Ã«LªWW8šÀÁªàªÎ’=GÖ¥ò9 <TeN‚}ËĞ{t=‹"Dı`$?Tî*õ`„±R?*¢q*°Ïç=È‚#¹2ªrê‚•e	7Ã*u½m“¶#hK©·½C²QÇ!’Úáı‡¿bµµ½ŒF¶9¶\Ïìòê$şwqÎìÙ:{zH©†kÌ¾=,Øúi+Û5xˆ­à½®uU.Blªõì8ÍÈH2Û=DT’I81şœœ®CâÜb¾åÕo\µWmŒ;/’/s¥îøŠ'ûZC5§aÏ¼1
v/ª	-ë4×kÀÄ•6ş»Òß]ú’š¹ŸÂêÕÅfú4R‹@©šÌÅdeº€‘;ƒğ$"_P{që=	oq³²ŒUÿ¨ÂUõ¾²,ÅÏÙO~"˜V²"/œÉ~Á¦Î“µİ¢ ¯6ñ·ĞßâİaFÕó Ó±WæˆO£¸—Î=ÊÖ Ôî{T/â×k%èb”d>qU€Æ²R^‘}BÉSèş)ğujsœÉÜ ê<´[äÇS6˜¡åMÙãŞŠÜuSm¬’ĞiX~ÇPÛ´B¢Î  ÑôÃ@Ôê†I³n•×ëídèç;ò£æV*àpœ xRSóg
¢/´Ãål!+¦İûíÂ(AnVwñÂŞ}œè‚B»‚#ğÚmùŞ (‹(ât>¦ÖL§íêÒ™NlÏ2$,n%Xãıò©Û¦gÌæîÔğºNİuŞ…íÙ.òœÎ[Î‹0w›Ş¯cÚy×ø<î8ñÂÛh5F]g¾·6Êè–¸Eæ©û¨7Ê.¸`6u9›sÕyål`è†DÎä—	¾Öº€äÒçò B$^Xd¢ğò@QˆBÍ´‘Iè
Ç*iè;*ÊS¥Û1Õ&q|s6{~ÎïÊ¡+€éè¹ËlchËõÙjãğ¦ Îå!±ÙT_"´ñ÷›ÿò½ä]è"Ö*Ò(ÄØ`sx(H€V=¸>dælÙïƒıã!Ó‘şÒs%V4@v¶w…x¿·‹ğŠC¦‡GTÙ=¡‡C@ÆPó×İcdFÑµ¡TWğYÛØC‘%äcÉ®aÆƒI°>–*¾¢Ø
´ "Iu~¼şå»	¢:¢"NnöÉ“&Y›¥
‘»(ÜıBkWZÁJ,?8XF	†U]Ù^ 0·¾«cAåhõu€FgÕQ"ıºD8iõI~¸«ë|r,l*9‡7ãWùÂŸHVb0È?o±§.êµ-ªˆ#NçHõñ–¡'.ô#tmmÂ=å¡G.Ş~ó“§÷ÛÄO|²è¨¾Å
0”Œn*!*Y'X-É2ÁEş”‡ çx¡²nUBKŒ«ôRËèŸ‘Iz“_'¢—™·§å"¦ Pµ˜ê·7“+†ÊúM¯Rò…g”»ñQç]á¾?´ÆV—L ƒÎs©PºÎ+Ç´ÍùÒ\)ç&ƒ‰’ºi=)B‹ÜbRçË¹‹
{ğã¦{íò$—±¥µ‘İt2w,ø
ï³x²#™â’c¡ÈúÑ¹³É“Šx²KçŸ*J-äÛ$É½Tä¡pm'‰@ÎÎF"Š©D$ŠÆL¯¡ŸŠp¨JXCFóV†=a¿AŸë[¸n½!ÿ×‡PÁ&’½ĞH«
Œu=·(^%Ä}èD¾nö®j)1_¡•5Ó«ä·ÆC	%—‚éRié:]YoÛ„58jÉÕêîx1”;r«}¢.ªé‰P«œ’Ã°{Éõm4S&Ğ[ßuŒ¶E€Ä.Xõ‘ºãÙ+we˜íÏywxr¬bt$õû†§>dî„ n¡åoãõvÏ<ä[í·‰*ˆ€[xÌô´„¹éÁçÂÜAÑåáp}å'Äü*ú©­+ûÛ3"–ô¶×©ZÅï¬üÔIÍÇñ‹
øJVRçbœØÚ5¥ÀP8î…ïÀºWN—Ê €ã¶^%Öò÷“cm¿ì#oo<Œ´X‘3ÏuLd‰RáÑy¾¤ÀsïÂáïôïA£:P£JÁ¨Ëb,¨$Ëğ9ûˆpG†=ÄbÌæhe.eº|FJA5h(Q¾¬µ¿õòğ%zäo¸ûìÂ¸ğ‡a~H ¢ğ„Nz_üün'áæı&ü_¿ íOlƒ-SEywş[no÷~àb3&‡)y>v•}Ãê^åïï
”’]L€¥ãMoÉ‘t¥gÆLçÏiÂÑßòŞæy‘ –{øøõË»ßóiG?ıäĞ‘«=†‡õ…'@ÛÇ„OñÕĞƒÙşåfKÕÚ,…N«úÄ€Şõ_~ù0ÿºH´Âo+,3²Ó¿¹ì´G±âáÍúÍU²ë¡&l·tsoR à® 4 )<p0õ2oĞànÅHıQ¸a€•ÊÑş%ä†ô3CÛåÖëRÁ|Ÿbóõ>U¨väa0>D£•$”3PŸ„ğj<»y·x·Ğ"„+»Äp™n-°ó±¥©ø¿!¢ˆôæé ©S¦iÆ®õ¼½õ„¾qm<aŸ¥~úò›XUñ[^?J iı—àÌV G26¯• Ãš ¡ı¬Îj%=ÌûjNr<+z<XêêÏ‰İâıÈé^-‚¯$µà½‚Ôb÷JfPè7»*yB¡›yÅ²†¯^%	7«íèukG·©lyû&§ä7B¢†û•´ü¦°á¾=¿ÓvÓ¬“oè»aÖ¬+Dâ—®2g1ÓB´g1—ô—=š¡ş„C#°g54 ‹uñ
²ãC¼‚ƒÂ4ÀR@½¿²&|ç{ŞÏ[<GÒ'5xa,Ã˜kQƒË-²:pÁíI€¸ı¥8	^ÕÑvÉb»“ĞùŠvåôÊøvæLKñÍì0½³AÂÀ]§‘/L.Ñ9²Íu	›}qÂ¬Ş,C£×*ö„KËC¦é £KK:OÉL/nï‡ÊØ; ~ù7çæÄ1S›„Mw8èu—$
_•€»dI¥•úˆæÒ7¿ºuo¸+Ë{æŒW£ÊRæ±Q|]»B’(v~ú Œ¬>òÃÇ;éŒ«¢BU­ju<Íõ²ÏÅ¶D÷•Bòª¥ô"¾"‹æ¸ )J@RÕ~ÇŒê­Î¤-ê|n¸³ñT²‚£Ã.â>TªÁÌLŠ,$Š”LDZ™ôs¸í8©9YM–¶#™ğµ2éMÀUi˜S"õg™GÍúÉa¬ î*AuP´2Êi’™ùø×D-Êf’’šZU)vîLŠ@áiùbk"°Ÿ¡juiXì¢Øì!„B¸L¡äÜŞÓä00Ëk@.yº<}±‚ ü±NCÒú–Aˆ]›D`Ã¹Y¦åªšİò½¬ÚkBD÷æ¥C³ÉÄš‰JY4¿#¥Èª†,Ÿ_¯½è€öƒrï<••(ÓÁˆˆ¾"kÒÚ°*}®\!Ïì«Œ4a5c¦@g‹è|L³˜N›mcçk»’ñ\hAVªÇšë-ÉJõüô<¨jy4Â³ H ÿêâ%Gtß5L$záÓ›à=©ëÌõ}3ÖT«Ñ¿Dã®Ö–/ÉM~´[€¬£õ!ÜiU l{iLL[`şÆçßæ34{~­¨CÀm)øUIP•¢ UNW³¹áÚ“¨^+Qİ!ÏÂšËƒõ¨WZ¹Şj:› æ4D>ÂëÓBPı¼V"šô!Œö|Äá/ÒÔ‚)Ï+»¼|KeúcÖo	¬ ƒÛ>n	—r¢4è¢š-¿œÌdâz4eæ/¯ëEÆlÁM ¾è¡2ÁÄ0'À=”&˜LU'˜Lå& @_%On‚##àPÌÖeHS]ÊO¦1^ÙG¶g.'ir‹ßQYÃhC—¹šŒİ¹+ºF:Û-ÈæYò¦„1’-°ó·[aãúãLy#?ª?	jî[òIaÜ­0 ˜“G­:1‘H ÅìXU¹”„»\£TY+i±ö·vø¯õÆú—Bfô¢s¸OºõŞoi¬FÑŞDÉÅ°ó …{A}¢>¤»Õ~&¾÷¼Q’`ÂŒ³>ÓßŞg¿Ô5ß¹åt[ù‹g…ö“«L,)ô!Õ.¨tR¾%…[µ%¿äŸ¤Öeâˆ
ªöé
átüáÍ‘ŠDj:ü¢1ÔºNöÈõ¨œÖ.¬”‘m\-²½jò²w§59 ë‚ĞÓµé5c16\H9=è5ñ=yÊ*‘@‚ü<ù®‹¶‚ÆóAeT¦Aeb~N©25Ñê¤U7²<¨;òw^M÷ÕA;§:×’Å$“Í
æW™ˆôÆAo¼|½±	õ2í8uï ?¯6Õq=oŒ ğåAA=‚J…ÒzãÇ±8iüâ¤JmV^P·2èÊƒ®Ì0èÊ´:èÊ¯\Wnê³¸ªgU³	§FV ÛŸ~yÖ74ún{4Â:
mù±å¡ä™k­¨Ñıß1fÕS:y0$z¦ÃE™…ÅğásáÃ»»›»¥ßlí¸y~Jeµğs*–…Ÿ3	,ü
[áçŠ\åZ	ÑùE¶èÚ‡û¬ ¬Üg€Xa{p†¹ÁïC‹[U˜lÆ2nSƒ*³6µ¬1hSÓ]ëïa‚DmÅé—ŒÛÁU¯Îé\VÿJ9–k’Msİ'V”·'…'FYå‰ZŸòõEÖ6T¼=Fá~‡Üì·qÃÌ´íóeä#+rş{¢g¦*€ˆ”õ~‰\bñ¨)U&V/÷Å–¸CÎ{Ûpgùnö[h©l_”z%A›ƒ7†şŞ˜Á3xcØ~çôÆÎçÁ¡38tÁ¡38t‡Î1:Ânõ³éu;‚
«´Yô<Ó/æÆÂvq…Äª\ÛÏko/Apé®tRÈö/MçŠ)vÚß‚ôÂ1²“øºxf©ØŸä»‘4cª=I*¦kšõG®'5Ë“Xª6=‹íÓ–¥gg+Ò3:£gú+×¡¯ÌıBJĞCÜ$0fë™}•u÷‚Ã’rT˜R*3ùa¸¯aìŒY¥GÍÙ²aHÂ¿ÒãåÌÚ0ájéñv‰4ªû¬Ô¯@*Ô¤ÚB§¸0OJJ¡²­sª“mOéI¶1%–Öİí’I=±qÄë$||½×Ñr¤{ŞÂ\L|nT ½K§eÓOw´%IşÅ·6‰egØPÖ\@û"~ÌĞĞ%ÿ I–÷6CS[3‚-EúdsyÜ[ßññÃÇøöùç;-y1½¹·rV@B,&/¦‘ÄáàQ©eí¯G¾+™ëŒù?X`^ÙV-£Ü< ¸Tò½eJ´Eê°¨/wÈˆÌ34Ï˜y»¹$„G¦:Eæ"‡ÊÖb2~’g6^Lç3 Ùw»‡\?!DØ€Øî@mèì2ßF“1Slõ$L1k´ä¿T1Tò>²ÆI9Ç1HrÒ4BRB‘¼g,°Ô¡8XÕæVjôd«×Ør˜vUCñ"iñ–{üù±tè¢»šm@4Ié¢)Á§ë¹G	/]Júp _j²bò£¬væ€×õj›%;È8‰Â_·v:ß0Ö0EëËg¹ÓÕÊAÀ!%E„dH›®°D÷zÍtè]‰-BÔÁñk&*õ$$œ™r ¿y•vJ*Ç¯SL_­˜W$ôêê~vÎæÛ¥h:Ìç“Ó¡24ÇŞdfŒĞ^)2äúÚ÷º=0Pâ™Ä XÛ«·¤NÇT"S—Şİ~Ç'ÎMh•šÖdfºĞ½#L­¹ˆÜ:H±›mâßï,-ÎëùÒ›¢?vs^ŸŸn^¯Â&ğO+*l¤SNa¬ F_ÂØ§®Rhi­ùJZÕ‘!7ñãPƒéüvAŠ†ÃÙ+ÂŸıµMâ¿¦è:Dš5G‹%T¿vi—Fkˆ4¦—ï|»'è	£âB[N¦æØ\ Ê¹Äã>äú–ğ«·‚Ø‰ô
ï9AÿÇ(²¶nöVëüôyÉ‚°ÅÑLñ¤æe.‘w°§ù#›z$ŞøîÊqQ â¯~ŒÁğüğğQK5;d›Æl&xáºÃè
=Fù¶)\08
-ÌÍ¥½0–@©Óãï×­ïññ/ƒXn£:9›Ú³¹=«1vyÍ/ê$÷Ñü´ìÅ4O
7Ô\gÙ«j`Ö~ŞY§X½”ĞJİb1Æ,s&_Ñ?÷(NK#¢JNú„’§ĞıS¦Œ_w‘éxK)pD¢fqn×>eæÈ¶v;lØÿS”²ÀŞ'I¸…¾‚!_QÙÓş%,˜”Á:3Jıy`ÏEœWT%`ë 7^!…´ÌpĞàº¯@FñYÁ²ÑË5îPu„ÛÄr’ØøáŞ¥ÿû~Óãñ–ss6 è,
ZºIä²Û¼DîTÂ¬¿9féxl. Õ;;2SğNnµ.Éõ¶™p¡pí¦qÜĞ›ûı#V>’/Qèùúò&aAŸØ†]î¨.šOMÀC¥Wm¤‹şóˆ.ş¥T–¡›ÒuƒWÁøçğCı°"7~ npmè¾ı°¸›› l‘,Bß×ôEºĞ÷ˆ•¹¼Åq‰¦XJÕŞH]¾¢Tùâ$ûœ›ˆ8_v¹ë¶–ÅVõZfÕh£µéûÉb5½½ÑEk‚XEğ#èæÌ±Úé²$Cµ_©·El§;Ü%~.údmñ¿‰oà]D^iºÚ¸È˜ÏÇ&š²–æ6´Ã¼âÑJüé$²‰¤ÎMfµ¶x•éú5¨`ÈıÆÉÍn…˜d´á¡…e˜Ğ»»5ú_ZgX(m+°¶Z'a¸Â"WO:656$sèë‰'äîAò¼(ş=ÉÆßlè;÷]ä;ÇV”p2I	\ ä«‘çazªĞWJ'çğ˜Â£* ”›Î¼Bi"u©sór’s©œo9Aª9íjt©ĞyrU'8Øå×4Æì*C¤ª·ğ .Lèê>t²·
³èæ‡£f"JŸ›$±œ§Oh»ÇÊÈM€IF£2‚haz+À;¨MÖ“­ş?   ÿÿ ¶¯ï4xœì][“Û6–~Ï¯P^¦œ-mGÔR²ñŒİvR²Ç]¶3ó°µ¥‚H°›±D*eOïNæ·/À;‰
 A‰RCcâsùÎÁÁÁ`ıv‡õÆµ_|×ìqà¢û¿ø“ÿ10Ú>{w‡ğcúïö+¢Á>úöİàÿ¾~ñ¿Ş|Ü¿ñÂÉø™å{äVèßı˜•ıã›øÏo
½ïC’ÿX´ß>½]mñ~îñ+t}ïşı€÷!¶ï0Vôˆc¾à†Ø³/Œ±Ü•ãzh3p½pPåà§ÁèŸ6ÍÉtfÿøMµê§·îno¢fwä¶@´ˆdì; íşÙ·xÃÁÚ÷7ydŞÑqW×s}Cß[‘J×–6/m^ëÇR%:.R>éÍ§·6ÎöõYÚ{MƒÃ¯q×<‹;øià6›êˆé/Àá!ğÊõş`†¸¿A¶µU"p=OB”pÙl€U¾aÇ•A©gş®X¤EOŒ™±ÀË5K°´t6É«__z|ëîÃÿ¢ÃyïiÑÃ_ß?û®¯œ@ûwL×áb:«~(1Lü—C™ÉlÙ¹FËÑ†4oPøWßõ°ıwİúÛíÁsÃGe4:}±xùóëÅˆ%¥ïİ“âI‡vJmÅ®‚›OA‚ëQg!¹í»?ô÷«Ãnã#ûÖ÷Bd…wï¸|G?4ïûõo„àl76¦Ö™,’œºß3â(İ¹ıáşIşwô3#’ø•lÈà@ş€¤]Tâg2ÉiÍç/®}æ»í“ÿ&WdşA¸
Ù9ü~şHv>)ˆ·şoîj‹‚Ï‡8’.H¨x¿#‹‰k‰¼²äTëíÂöGÿá ‡RÓ}ä‡ˆ€(óâğgò×gÑÿ?¿}ñËj2L)H¢Òt‘ÕQ¹Õ©”JN0ĞEYhÏaô„·(­Š¶ü‡P?ØOD®j;ƒ×ôUÄSÏÊ<Õ¨¯R%”ŸaDEkR—ÏD´¾¦ıPÈ	PÍë”9Q-óç½xØ„ƒßWˆı«Zƒ×kj¡=&ŞD6Ûø)@y7J™ƒÓ<«¬sú[“©}f?%MQÏ’ììo~øÎ·]ÇÅ¶D¿e‚Ëäfqw²õ¼‰v~˜a8 ñŸuz3ô”ï£¼FN7ŒÀS=ÔoI™çDZ‡-öÂ9ÚL ClôW-k'Ğ9e*¯ZE}t	X¥Y– )ñ˜H	¦¼Ì!–Ş¸¨¬äyxCq²B|ëoü@2¶MæÍ±ã«]ŒÔaüzz)ˆÌ„ù	5²>ßşÁ³¥	c§­¿F;°¿F°j‹›Ú~&{İ~Gké<7ÌÅ0pY!Patsò/¤¡İ-Úl†
6Ğ9–‰±pàoWÈŞºÈœ€İ¹
£jÀGZ‡2óH°LõnøğÚİĞÅûAâïy‡\;õEX|ÏN/YfM»Ä®LgŠ lµİ›B%œY­®`²ƒkŒ3;…d™hõÚæäOjÁÖiE\I9…6!UTŸP&“IÂIÑYÃ‰ªmÄ¥-Ëå2ˆúctÕš‘Y#•Ê–bTPBi¢Z—m‘L3İ8’X=ËÒBs5›Ÿ&ìWdOèaÂ?ˆøºC;¬5/Y7FÍk?dÍÛ´@€¿à€õ7G’±p}Î'ù•ümGÿÆÌÊ|Äaèz„n÷É_¸@cînÄ)IÈyˆc:i$QDO”éO¯3º;‰ó6!ÜSx¤„°œ€²â£®H@½ÜøÖç‚G£…hZN­ùÌ2çMDÓö‘^? `EJ.FÏ<çĞûÆİºaÄÉKRîeÉhXİÃFâ{¬Ï<Û^´Ç²Ş=Ûo7şÿLÆMé_¬ÑÜÍà„¿êFõk2•!ÓZG°¨ÿ·r¤-ò¼Û<~ÃÄ%ıĞ†V¡¿ÚîïyÎŒĞßñ>çx,kˆªU¨(Yi¢¦¿üGÖWdMLìÈ6{|ÔÁ©ÏDTÒÆÕ|Ç!R‚¥d(}ŞúOÀÉaËíŒÊ›Gş
ÚşŠĞè/Z?†ø¿ÿg°ó7›•Q0×ËSÚû.=< ¤Ø{êı âõ6oºÄ}"û…öĞ”«½U&Æ°ª„ï‰H`·ĞÕ;²é?ÓMñAEk•œt‚@(°Wq¿·O¾ï«¦Ç8Äw<(è:k«ÙF¦º1ŒÌ†cP¸£fë`ÌçÊÆ@ÚÃG*ÆÚt5 Õ[2F³Õš—[íÿõîoq€dÎ¼TÛ‚Æ`c6a}ÓŒ]Î¤ªS†JRµÁ±a,_.g¯aûªópQqô¹àƒYv?oãäå«éKóvÜñvIŸHT@©LU9GMleúhãA©ÂdÒo†4UA´ñ(å½¼*1‚o¹,“š.¿æ
AÉ8ºK >ºiÍèbCB=¡Àq’ŒÕq^J™sø'·kÔ/Œ	—åvR¹Ë—ÄŠŒÂ•	;°fC`÷¿Mî‰ûÁÌai­ò8ÃØR­òŒ´æeï©C«ù™‹W…”ë­,KV…B‘İ´¢˜ìRªİ7X²A$™Ë:…â¬ØQ.Á
ŒÛê|¥Î</Şæ…@fƒáùÒYcp7k<©ñ¤Æ“åÑi<Ù¼d`§q™Æe2=j\&ŒËÚa¥‘©+­Gæh²Xk¬¤±Rñ§±8º«ÇJ·hÜ¢qË¥à––p¢×Ëxlœ¹=ÓpBÃ‰ÂOÃ	ptmàÄÙ5µÖ°ZÃéQ©†m¥ìŒù\½²3'ØZŒ´®Óº®ø»r]§õØºÖ;'Ö;²ê ŠÌU±8[,ŒÉxÂËT¤Jô');qéE‘K	a•¹&Ë¶›Î¶EĞk9´›‡&b2“§(c¶9ãÅ¤cŠbr¡¦äEÿM%y%¤[P§L»©F¤G;›'Í™É$kÍÊ"Ë"Jl•$}KËşõ²)½º^…­YÎÊú‘ZÅ†Ë–W´ºÈp³ğâ;l»åÑ¿7	Z:¶mšé{y'¹`Ğ¥ü¿~h'ù¥é,IVPÚ»Â—´6_Xëy÷"^Ú^üâ¢ÍG2‹d^Jèò|$•–Dìd'ß„x§]J®åß|LÿùzÓ@Z€s!vş(AF–BÎl±£İœ›0b u$UÉk½ì {.MäP£ÜIM¾°ëãõhÿJ¨w'x»U¸tóuù–ÿŠOL.ÿúi`T&:À›=æVøÓOƒWkôUlUJÑ|g…¹¹µ_}Åë;EYM{†ñ€»ÿH:!í ü¡yvš÷í9ÍH<‘—¢ìò<â~Qh’]&¡ˆ×ê5Kû	|,Şá¦üVüß?GÜ7ø!ÕºU¦Á ûlŸô/ï¿à pmÜ©RJè…,]BA*erDdõ^pJÂÎïˆëÛ£t+ûTıJŸ#&[!*¸~ÛhÕEGKÚÌ0‹ß™úÅuÂ[ßÆU0Öâ¥4kº˜LMVì¤ÄLÕÎ½û£¯è‘ai¡ƒGÆën1'Y3)¶W G!v¶ö›Ã=ó­6esÿĞcºJø8ªòWºfœò3A}¢äKŞÉZÆ86z”Z«­ï…´Í ôİà
n¢ìæÏ¢–¾LF7#§
HH,ÊÿX‡ Àõ(Õı¡-9Ì"”3¸n˜ğñ‘<îBÿ¶é`ãê/:2÷p²¼Ï—áK²B¢æl˜‹ŒF@SÒìü•åîš'ôK½+m¦Ü°kÅ­i¡•øXhXEŞUÏZeeì’Äá¥Ğ$å48¢Û£Ér9Õø ?ø Ó÷Â«• Š¤â@ Á€\ˆy[Ãâ¦Å²807Ö–O Œ5èhŒz10¢}·màÇujö±ÖìÊ5»ªÈğ U}I ‰¶ê}nv¡Şí±åØëÑS8Ğê]L-i¥pMJA8BšF9á;ôHdî¼u[æ¬ôSğìü}Áã¬Íåh²‚Z]îƒ/•§«
1‘7UòÈìã§©©d¾GÛÆú‹ùvA§­¡	ÙÇ;ÈÅ3•$ê	nc62Bä	99ñİybâV8¤nÍ^{Ù\š‘o#’é0ë¨
,Ö*0æ£Ùtj—"ùª •ˆ¾·y\Ñ¿ÚÖ{+p×Q  §ğW×óÈ÷
ğê‹»w×6ğPHI€o("Ûv)ã¡MTIL'å_cJzNV‡HôôŸ»÷ÇB$:kbX‰AÖ!°ò
gó_~'»›øş»”jì—ºƒÈDĞ¬ ˆ†SsR©Ù½‚“ SÿtÀŠóÚp<„UªgG–âÛd|]®1ÔÂ——±™xÈ¹O†ºO€lÎ…K´0BlŞ·Èƒ9&æâ¢*w+dÎjŸN,ó®Bv€Ã¡2V4¡íŒ+nnÎŠÇmîæğ/ÃdR ìcˆ‚ı'wØubš/ÑÍ­5«Ä#¿§+µ›ÿò·Ãfƒ¨bÍÑ×~G¦µÚÅ“HPX·˜"[it­ˆŒ˜?³)Ê¹_Ä¬x÷vùÙ“÷{$¥r€å‚ß8âë#Â""«.…
9‘šK¼4øq<¨fRáHÈ‡"ä7rfó%NÅÆ¹qŞ¸ 9=vé1g†3Z8ıpŠÔè}í/Qã/š†o³>AOŠ´¤Ô®íz¹×K4¸ä¸Nh\çÖáã«Ú££=:üîA£
è›soş)<_Â‡®b2&e,F33¦<ö0ÍSÂC»ßÃ+ï°]óc/â2}+g,bK®hä“¢ƒªÓ$°\É‚
b¬âúJé¤Ê¢ËÕ-lFÏ”gÏua²¹­µW*K»/Z:Ûò£ƒ(ns_r'ì$BÁ6§v€L^\Û¹™œêB*uœÔW{ºXÉĞ.n¯co<²t®MÊ4`¾åb¹˜[# ‰—Ã5ÃP4·^yïc[ÓOü?2Y2ª”É€«ä9?µ¢y“VÕÙ¹)YVQFŠ×E¤´,ıG*HÕÏgÎhda >ú8Õ_	_aÉ’
M#GşJ¥tì‰™UíL¦Ø\Nƒ¥„qÄfU§d"eDœ{§µYAÄMd‹F›o)^ÌÇ3¼79z 6íÊC[V¦%\ÏÚH¡Ãî>@v]P,x¼áÔ{2[ÿ^Ğ¥ˆl¡íÊ²	Ş;ëòT"Ù}_át&|F4ÕÆÃ*í4ò ´H£N"¡•ßE¨ÈÓ_VS$V§öÂXØ3À¶8.V?c¼[ù{O[_Ù8Dî†-å'¸ğ&eè¶3İ@?Ğ]è›g&(Ó9~ª›úµ@Ä#²\È†Î¾xY»²[è¦|
·wˆrRÅ\hæÌÖ6:FŒ)˜Jînor(ú«ÎÒ9-5Ó_4«¨»Òl¯…adè8[.ˆ¾à¬Rj·ÛhÖcìÈh”ãNdZ7¾ ~Éøºc/³|Ş¹à×'ˆ}Å”Së—İÀÜ6çtH”rmÅ€t‰Fëéb½àÁ±ÅOçáéÒ½(ïQS3¬ŞÓ”í½¸2ƒcCZø±i:sH,áƒîÉkRÎ\ÁÁ_¢K½0A	a÷*ğwï;æUbª’xã‰½°_µ¤åÜ:“3ÔNLh§6Î›Û¼GeBìõòåèv>^Ë;îâ	;é·ıƒÿöª¬y…1_Y¢¿4rŸùc|BéR	zÈ¥³‘¦Fs"ıŸ ‘pÄ	2C¯«`³%„¯Sñ«g´¦*¦H²)ì”hL>İ?MšŞ]û=ù–kó¶o“b„&ê¼Ñå´ ;ÏÅŠ­äyxÃs˜TãçW÷ªÕ<ëaµİßóKïğÛÈÒrÊäwÒŞúôâZÒ¢ĞµÿÏñøÂã÷ÜªÙú¥hZ\9KéGPñä¥âNS¥Ìz”oD”HW¦™EK¹ñÊ„Ş Oğ’åŠ£WÌ$sñK{Å.·Ì8—[$£\Å:ƒ…ÊûròWßz9Z^13Á¡è^Y.XZÉ¶{XšÈ\¡"”DÈÑ—ä†À¥«’°¨”¯ŞC+É€¤£V@Ğ×Üí«»Ef·áu •¶º5Ç»×C¦ï@Ï’˜³¼œ/—Æhó¢hˆ’ÿÎ	QÔCjÍ¯5?·û§§ù[_¦nUå˜AhdNÖHê×YÖ ªÃ#a…>&ú¡+öûQ7-{³™óåj}*âi|8Ø¥óæíSg¶E·J}Ò¨
tq._ø¢*7N­~N¹wvŸƒFø­°¶?eËktUtŒo„bq‘½˜­GZù´ò=ò•W§Z§iVÓï¹uš –:Ö ßÄl«5f‹ñ|4ŸQ¯«5®Eök	Ü•¾>z!í¹vìÓSåè[,fãéˆÀRŸ¼~%ŸÉ6%¶É"ĞùÕæANVâuR%Z¡”ÈçæGµ“…ró¿–—ª}ò_ùªÈkf“õˆŞ	IkV›øäÛ>õÈBò—¿.-wëowLwúwÓ¿s<ÄıRÇtv4Ê7™í)"á›oåu«a§ò‚7È-Ş3¶â()º1mƒsá•ærjaèÖq-œ.Ç¸ï×¿‘14`ÛñÌÄËù8şáß‚pó7€/œ‘äÈ%JkJ¦ãÛd`ı¸÷Ğåc›Ü˜Érp´Ì«ur£²–ÂïHq»¢Ø—º´|Å­ûö§ÒNş9
ˆüIG^Ï*$àğTçÌï’â0æpSxa$TîÙÄ³TÆ|úëúöx‰#Zx¡d·‹\ÖüXç	$dÓ.€°WªÀ£ ÆÉS"ª"ãñ—	IØòúxŠ$´»ø®ì~uÃ;Hªí•¨Psf®ÑğñU(ü:ƒ˜ÓA$ô¼ß‘¨ÖN‰2HÌ§·¯ œËú!WáTäÅ±ôé^—"fë[Ÿ™dÊ-xgÆzjñÓ‚Æ7VÏOÌ*`;]™Õœ3Ãhåb¡m İnó¨’6œ9Ïpëmüº£7ŞMÚ§F÷É1ÇÏñ«79³B¿
5I=k˜3<ŸKe´*‡	r?s¯wÆ"
ÂÚ—õ¢–Ñûà]û<8o†½y»û¸±{ŸÈ
İ/¤zvï³_†P<5—†šÓ-Ra7ÁG›åMîÀpâEãdtÓ<>­@\'¹ö ÷X…p›‡ç¨G¾â¥”¨c¨½YN)“(»Ğ}™5”?|~±ƒä©[Z]0£m¶í
µ«Áè€şÚj¶¼J¶ìÙÌì]XÛåhØ²S¦iN-ö
r-º•‹naÇ„èîN¤ÊÉÆbQ¹=2ÌM!@ü·Gvd?]Ëİ‘.<Ö`ÈJÑÔt›Vñ1| VcŸĞÏ¿ù®GÖ1ôıFÁæ.cÙ[×[Ùîm6şWºø|K#±{Š%£€˜Ç¾©‚Ò:jfÕ9õ ÇÁÕ©Øn¦úK2œÎ!õ©F`4È’[CİØs…S"Z‰ª“!K»çR@¢Ç9 A+¤Yàd‡CÅ™-	Q²´Ûø£5išèÆDÕÁšğÂrt\¿Ë ¸Ç,FTHñ«Zƒ3A±GhÙ7Å€†˜è/GÖPÓZ5º!ı­ÉÒ|f?‰0³ØÔ4kVåQbHe†ƒI@êjIÿ)†4qxŒ	Óv…B¦ãÉr:q²^óS¯GQQIÁê·Ó”À›r¡Ç&-^º¢±Õôòç°ù	¤
Ãëåbâ,†„J5#ÛàoïW¿~|ıaõæ©ùŸÆÛßoö¡¿m`ş|qQ&˜Ba;¿å¥ÀLkòÒdFãMSñæãÛâšMôqq˜³àxÃ$HÑ†Í«(ËµO/Ü¼¤çöñé}a+åóˆcúBg”²"”ÍWAŸ5yÅŸ	Ä1¢y4wš€E^ï¥˜EŞÁ¼LØ"§áÈEî®$x±È§‚×{årÂæ¤mÕÙ²ÛÜb¬õö5ìKEVCcZÖ@%\£ÀÔg„·¿¼CáÃM@lûYÔÒ÷ƒÉèfä´²3Ú"@4‘”5÷¨H¾uÊÏšö¨ ½ˆë@)ÿ—ØúÏƒxcÿL¿#²1ù¿?ş=UÅV+ äCØêºM]Vás…¬Ìº"æ–ô¯9Ö)ó)vÊdlX;^ş-Ê‚¢—ËªQËR8N—¼€Åd*kŒg3[8cÀ·/öuBW 8•x±HÇ¶Õi'FZôq¶yÕven:NÅÉÍÈ]¾”xßF]½$—it	wäÌ,+^Ì%—};Í»@Ò UV*7Îòunšn§{E•T}¦–D]—a	dyrŸLÆk´ÄöI·´ÑxO8ï­4–4cx+»ƒø‰+¼vÒw‘•
!™,[§Ê;}n)wä`OMO4UånË€âR±y˜Ş-d[³“3ÇS£ß‘Ù]e­)¾(ŞVÄ¯G#kj¢q_ëñOeÓ¯Å¤•OI,R o¢©Afs÷˜s®ĞwU’­— :‘Æ¿åE•Š¶É×ºçnCí¹ÅÃõ–m§
ÇAó@hJy“¹ÛØüØ“ïf“–jéÓ¬‰Ö\¹Z3{¢W^Î-×ë	v¦Óş ú¾äêÁµm,ü*}`W4S«ï‘Y‡ÜòÕ Èê‹¾ªßñ¥¿Ş?‚ZX¸•”¼í¹p*šDEú*¥8¹(ÆŒğ	BÑW¯“S³Ã
V>—ÈD…TT†ô^¿ßNŒWş#rà›õtç¹_³MæÃÁİ}€ [¹Ù³ØTs9D0×uƒ¼UÒ·÷úo)ßµ6”|_íi.7¤(ñëñl1şëÛ•²ñ´ER‚ı$ÉĞêbf²áƒ­ÔÆåÄ nxÍ}Ê2®ç=p8Qñœ‘¦k1dAÖï¢€š×Í³>¼Ã–OãX„U¡H'ğ@Ê¿œâ3ÈØˆ¹™Sc^PÎÛ¼ë{’×ı–Ğ]œby½pfÉ^½ê„ª4Í«gjä•şÚÄé7ÂiâÂo_”±úØ¢Em¯]½SÁ¹Ã»øRHG^	úKU`KÓ0¤S»ÈêsAQ«§d&å4LîL$µä/AW™¼¢¶_]µÙ0ÃµæÃ"ä¨h%)@‰šKîÍğÚşÒ‹}9›ËÔ<"Sİ2°D¦úb˜0ì·ñõ®k2éÛ&3á9 Ä{z^‹,•@‡B\ÁNª„4: j·ª!ˆv/C
TêzÀy©•
òæg*_¹VW¼·°ªæL‰(ou‘OåsŠ±±ìâœâÕËÙr>›ô9…öKh¿„öKh¿„öKh¿Dô»N¿„vh7vh7tOÚM İOÚMĞÚ€‡ømmÀ;ã©Mi^ğİğÚ?2HmˆkC<ÿiC\¾÷qmój›÷Œ6¯6<µá©Ïk3<[›ƒFæàÔ4L¼@úŞ™6ÍAmÖ¤6ë´Y—ÿzkÖi#G9mŒm¥h+åiZ)­3Htr°c/œ©3B¦FòOÉkH~d’·‡ä/>u¼¨ßÓD|­ñÎ¬¼³˜ÍÌùDÃ0ëB°‚V G¨Ö0G5Œ|RVäÚIn®ı]àZù#n­ÓoO×‹ÅÌ\°ÙŒ¤óïYV{À-ÀdLë»â‘LØ–®’sÊÑ„«íYR”'ºµe›ÆÔX^4Ñõœº¥[ß#6ö,|‹6e"gì8xì˜¨I:è­»ß×äÛ£Ùh¿ğRİ‘9ğrºÙ‡ ÑÙ0ó—éáÏó>à`•=´íñWìíÁñ²Ê˜¦Ãd­%êL‡ƒ/d!|E²<ÙOA¬ ôÊ7}+¡„F(~?Œû„KL"¸³§Š*&!Aã(¦'µR8"NÙêSs	QÈ€{9ŠIéC½Ó&PàÔ¼)HÅ”ğ…-é'ßöoıínƒéÿ©{+À²Lkf- hKX²ÚøJc+@Dv2/Üf¸Ş‘&ú&Ç«²„”/¼¯–LE¢NaÎ‰cŠ»ÀwqÀ\.öÚµÑˆ^ìv„è?¡ıg…`b{m-€Ê9iÒ¾ß„xû|°!ÿ{™”›íÂ9i/YÂ™š.·à[­’„T4¨>¤g†ÊŞñ³Çp,ÆÚğëXdƒEe>ÓKMú%£nM»Ôcü«çş~PçM2_·†±0ù¹±eBIÍ^Ÿ^YRv²nî½WÓPãk–}(Ë"  M»H2|ÁŒïÑ. o…ÿ¹óƒp…8ìœ.k“íG­å&o/Í`ÿbq@€÷hƒWhË}˜Î&.0Ÿ´i	°Ï›±ø»•¸÷”XW6‘»Ùs¦UHFÆÏ„O;öê³6ó~¿,µú8–ª©v<V¦j¯Éœ|	’©î†»WG”r²hx|ÎØ¼ŒİIéBôRõñÃÄ^%DƒÓ’ĞQoÃ.e1¦>ÜgŞË¨tó²‚Òë¦5””B•ÌÍqúe‡³€VÔ“zÊ7àG—K*¯ƒ^9î.¾íàR]SÔ»L&É34ô<¶ Á5é¦ofnHÌ‡™p‡ó‡ÕU6‡±–¯¹VD|†Ñêûd˜"™jÓaÉT$±°L³i<M
—òï¹ÓÂÁà	`Ë"œê4D¥Œ¡ÔÃ$µ7|ìsÆkì(Fí*ˆ&$¯€•øî,C$*A»[50D%Ò€ç˜bàÅ	ÒÅ¾$¾g¼8Í›xå}€xnMŒÙxÜÿ ^í9³#D{5J”¥½Ú«¡½Ú«qî®Ñ_ w.hZ¢+m@kZĞªÚãÉõµË™9^Ì¦mÔ>E£VÛ©%bÑvª¶Sµz‰vª6µÉ(Z­ÎdÔæŸ6ÿ´ùwİæ_c£ìîXd´ø}±•]-0§csñjÊ¿Z€,ªşjâõmlm\( _0ÖhßÑLQ:"\²¿Q´¸ Õx2Eœİ´§k+¥£ÓU–SÒéÒw®G¢í-ÌvŸïY¡èÉ/wGi¦#hù§;"höŸÂöîH¾É¢ë«F¾J—\™Lœ¼xõÂœ¿äËÄxªH¼ëiÉ"õG6]†ÀI–MPŞ\‹ ‘Ï¢ó€¼{|ëo·Ï•1ÿìÕxôòÅk^æ.+í0ºš×/5Ïr|/…™\¦ÛCL”È­
ò‹ëÔñQ‹1íî¨e2uÌùØÑG-=<jÑ!¥­Ô!ú D„è¸5}ï>èJ
èCÑCE(~ÔŠã	Âó‘¸ŸH£ø4Š×p¼´'_#ïhS5¬Ö°ZVkT{E¨öÿ  ÿÿì]İ“Û8rÏ_¡ÜCjSå¸ô-qïn®fmïÆW»·>÷î!•R$8ÃµDj)Ê>§rùÛ€_ ñA 9”,=Œg, ¿n4ºİcÑjÙAhawÓfM´Ùé¼?mx~à¹ÛÙM›½i³7mö¦ÍŞ”­›²uS¶EÙ²¢,¨”ï67}­§›ë87eá¦,\‚²pÛËn{Ùóìe*²Ü{Q÷§İùèƒ¾9Ä¿†ˆÙÓó‰’à?»¿BÏ¤ q0õp8AtD¿ÊŸä“ø}³Qm6øwüÂèÑ_Îš’Wïáéˆf¥û@ã=6†¦ç$šüB((Ê^[ë’ù¨¸Ÿ&9òeQåz…ÔÂp5àù´¼‡8!“
³Óù
lf›å\§d"ÀcÄe®~vh¶©x^M§&À%k?>îáßBøùû89îOE9"Ğu6‹­ëÎVj–WG¤„QÜ=ôÇ‰a{²õ9%©Æ1‘îı;D¤oòåÑ‘ŸØ®ó*ŞÇ‰ĞÄ‚.[3Ôó	„w÷ñ1‰‘Š¶Ë6 Äòˆùı¢·¶Áê§Ş”ğ}¬(İñgÆ-fˆòßÎÉÇnä¡¢SÛVPÇ­]©FIàé¼'ëÎû}OŸÃÔ{šÔ0Ê=e ^Ç¢Ï—Ôß2Íñ§~®=úÇ"?.zÑìWÌĞ¯*Àt˜Õ‹á„B¼Zï¬ÌJØ•ÆÔşÉ“u|½«V‚ù<^L4föS¶ÁñR!Q#sWKw¼`üfÂôB£ÎäO²&ßNşc¦¦²Ì¸*GÜv6KŒä¸Ñ‡†ÇÏŒ¬è–U<s–¨“"ÉevXÎñ¦ë….‚å(İAğ5BçÁŒF«ÚÂ…_„ú­J^-î`Ÿ²Äw|ÕËe¬è¨ã¼µyÜ­­Ûå;mÆòZÙxi-Kn­){
¬­rÿ”Ér¡hG o!Ø®Áğ®13ù<z¾•qdëÎı÷Ç]¹š0U7÷°öy~dG³w$}©¾õÏLL†Ç¸z?ï’8÷ĞÂŒªìâ˜eéõ0ÇP¹D+Ÿı|æ€Ç$´Ôää®¾¿Ãä^®™ª(ãnZ¶>f*>°
Ízã¯gĞU‚ÎìašÂBí½¸;8¡É‘ö10º¨ã¹~(zìÜEƒ6×ÎXuÊk<X’Zãš¶xùgBtş)àCo]N•v±Ç¡€sV™Rìà¹X‘#0ü³k}Ş˜¹çF»=üÄ‰WÀ­ğ	ëH·LDó>5½l™D#—j ­(áîôŸ"ªâä[¾Š”=ßT¸Zplcİg\–,i]úfH`rÉŸøçŞR³Úæà1ê‚$İ5SÑ¦)Ol
‡¡Bòæ*/7rKÙØ
 kû2á»1ß­ø·;œrXcÙı…6¢Ñp6wœÔ%…F¬MóÁáL/§Ş/§¿ÄéO±!ô»Ú;è¾¬˜bNgÔ§áå4 Ù…&­à}2±¾¸`¬4’ÊvÑFzN3X‹+l¡?åX¿p[$-Ÿ$FB<*3~KNÎRƒ¼÷·ßÖ˜õRLl[L³–£½ëÜ#LMö@1’}°Ëy Ãr;ñ‡ m
õ«	ÒoE–.Vr;®=È ×€pÒ$ß s¡!×¸^£YÅb‹‹¥8GxÓØĞYGØ&ŠIrÄò6CIkáÇ0â•#ÁKJ®Òríwi|,Æà~O&Fnü4İğLe«øœ¶4ËßÛÃÙ÷dĞryö$hú— Í¯L|ü‡ĞîH/]ŸGz{ÇªWıSõ°áÁåG1¹Äa:…Fv.5âôxháq?ı+şvã¹koü‘ˆ™LQ¼tİS~JD©N}|›PËEáŒÎF&?,	lq5Ù¢ÁÛ–Xc½\N7lÆÏÔ&.àf©`á–.x¾Y8x<ü…?Œf ıpMa0 ½—gV×}®¡^ôçn"ª	êÖWúñàŒCà)K<\ÃĞF?søâÍy._ˆp4‚—ß–a5µ'jüÕúÍTªœÔYz¸›\®öCµ1iôù÷y”àhïPQöOíÔN	n7¦^_{\àÌÎ¼H_6'×ÉÃÔÖ£İsO½˜¸ïW²ß&^†€óùL*IMànâçCé…9æûö]9oÍçqŠ;0‘ê>ùË	GWÑÏqø=*úIÅ•œøCQVÒs¾Ê½$—t‰×F¹¿b!$ıá»P—Ğ“¯ºjób-•=x•T“%°!ŒÈ~`]"-—.p|ßá›şù¶Ì›¬êé*ƒæÉî&Ü®X¸ñÚ²ç•¯Kò‚ol	Bo:Ö„€ä‚pÌá!¬…ìı&6g³S±$bb½ğ%œÓÉÍ?]…«[wùm/ü:³ÀsÊÜ!¨»?ûp'paãâ 8Át‡ó¢É¾çxß—Ğ‹6\gxÕê|ç^è2ß‡‡ÕãHB¯ñFw\1ğñ§%¢¾/gq™Ê
sµš¹XÏJjÏNŠâ Ö(¹©Ñ’â sÓ7a y«õÒ$®‹oÿ’;‚V2O:[àN§ZâMM¦P­D"G,ÜTD#ğı]ÖFCáo$¡<øÊ‚(­áHÁ"'éW#û–bü¬6¥X7A#’bİdcÅN6$j¥è ËX®W¹™åî¥TtF°ëmg®ë]‹è„D>ä“¬dç…ÎgûõÈ™ş™´À8ñaÒÏYwënfëÕvcë N<v»¦³kÔğ}7!ïaj[½¡¾WÔ“Å4½ĞÎG–»«şû>Ïø ì ~±^làtÅÉN£¤óÔÛKÑz^¸{Ñül\;j4Y—æâÙà…õtîl|èéğB1…I8Å´@t
C¤aºWºÀzìC`¿X®®µ•ÚsfëÿYó˜·Æ^ğ3	DàÃTƒV4oépÅ¹ä¦`Í8œ‰í`ŒèF”‹kÔº¨w;Kg‹¦)†İó)ŒŒ=`-Å–·òf ĞRº€ñ»ıãİÿ&¼èpŸ 	ËòX1/5H+fÔ2¨°‘§ŠÓ¶–Êª™»ªÙX5o;H÷œU#½ê¡'ÎKŒ¡!
àõ—·)lıuò]ä2äRêg¹İuĞœ–95`àÿÄ‘•=7œùÖ	8µ¯rÙÅ±ÇçßDàÀwœS¯tˆ%^ùƒHºr4‰„+ÚH%[ÑHU¢U^Š$k^›’ğ=ş`h=PÁã:gò×•ZÑó6˜Ví×`JúèÈ’i¡ Û	›ñxâAl¤ z7)ûQ
Øä¤ ¨Ï3·Ş3‚Õ‡³œ¬Öy÷¼`¢îº¤«õic^=$kÌ±ïa"2×“TÕcàÓßL7Ó­§~4\b…2w³„şf6x”&-Îäé²Š–Ê!Î:i»Æ·	%ÅZw«™’>´íVº [:îb:_Nù§øñú8ÂğjDİCHş×sè}|û/Oq’zgÀˆ:èSiO	ù#Ôd;êRé“íí­n$ìÊ®*"Xª^õÁ«•˜ÁG_&Î—+gí/9Zócü)ï{ğ]¹øJ¬LùÄ‹·²ˆ·~˜Íœ©póWt¦ô‚Ì-ãÆDÖ–z-MTüV.·ç²³™Mç‚8‹Úº7¾g$çù¶œÍûYŒ=z×A,‚Ôx›`ÕJj¬š©Úé/Å2hÀŸôcZVBÃRWw!/<šmÅ›¿WDAÚWË(ã!4IS{|ä8µ{HC÷ß!ÃİM×ü3t_vòÏzì)ÿL¤CäŸ¼{ß¹†&ıíz83èÖâ"G ¥ŒIe>Şzë¯×ÇuYójTíİ„ü¾Kò!oi0ÌÔ€&å&ÕŠş·ì”İBh®­şåg¯x„Õ¡ZKM‘d+XnÁpU\J\zmS½VStÇ ğümğ¬§‹ÅÌ™r"9Ero×wN²*².ÚxOĞûØÅá!	÷šùÓÀõV¬Á7 Åz\%öÃN®2ÉiÄ›N-Ò²Şuyf$cêK‘ÁJ¾Œ%X:+#¾Äaúmğäl«Ä¼Ù~B»mîúòR°mÕêÖŠ¹@/\¶õ3]÷yñ„8ªV[{›ÕÔ‡/¡Âe¬~ø­úöGÄyw“D~|Ø‰#OşBn^'v»ÿ%æG-k6Æğ([—(1å1îa
ûá28›® 3D¬Yb£#ŒLåRw'œÚÔş7]nD‘•š1#7´*È¤‘¨)Kó}¸Oab+ÅGàÏ}©î,|ÊŒ€»‡#…Ïõñ›}“Ó^6áé„‹o£O¨+w×ó­çÌçK£„^	<„çÃîs|Şû;°ßÇŸw!™Zë#	üí&Ğßq²;øÇ<ìc¨‹7&€©AA‚˜cJ™ê-¥ÁLõ¦ªMÍ.%¨I¿|¥j‚’{SAà	qÜ¼ ,t’¼ùÓ–ìMH^~f/DT¶aI@¤ÎWÁt«Í^Èÿ…¸-ˆÎM°òa Ö¬|ÊUŸI–³ˆ µX‰:ßMò¿óéŠB$"‘û¾ÃHFîĞ2É{@.'yO(‹Kşp—"5s8!JÊuk‰T•Ä°°“Ç˜Ôq{¡9IU­)âX‹õ4e”÷¢î±ƒõzéÖ¬c$A­Zòtf«çD@`ù*›Ä$ŸŒP‡+æ8Ö“ÃÕ<ó11ù‘³Fµ`š°Dt~øù/VvK³ÜúSÎA#÷UâÁdûZ9™A¶²r´r÷¢şóec'“mXÅ3ª{T5F÷m©MÏÏäöØ³»óiâùk²8§O8ø.NÒŸ29ÿ*ö­œ¾=×A*dÀQ!s¦8>ÅÜEçƒËñúÔÚxhJ»Ì1Êovˆ8./Ö®38Ñ¤Úº\ªöĞ¢é€õ»$¾Né{ø	FgøØƒÈãkúöÏaúä'à3ØïJÓa£)±Ûxç„Tp³ÑùÀ'îq'òf1ÚÑy±y!á!|ó>wá ö®±¸‚!ôïŠˆz2¼Ì"ên·_`2Û®íÍ°ìÒîuÑêõz£Eô%‡
1e°»,‚`3ƒœ°Öç5æ±rDÑˆ×,ªæ8üaÄÎÃ!d`£Ñ]KvºÆ2µ¶gÖ¦õ‰Æ‚XâÁRÎPÛ¥˜aõùq»\lƒ™ÇÑöìÖÇ¼,.Á®D@¤ƒãa¦Ëƒy9Ş› À*ŸÒÂ[ÀùÆ¬üOÃ7À×÷Ä±Ìğ£Éq2í“ç²©ïpa93Hè°;¡†sÏ¢¤ˆÂÉ‡Üâ‚l’yóDsPÚJÑøNÇXÑxTÕfÁŒ8NÓ…±×±€¬@M™7ãZ•…øSàZ+Sƒn]š9t&WW£ÒéJ¯l&‡	ÍæıÜû†L	›³SK*˜Êƒx¡Õº6Í¸la@$ìŸ†•9ÙOá[ºïëòOÙÜÂ5×ÕœÃÉz—ó…îmö×±w> ]ånâç¿é¹•Åo:HŠfÔÒ„4(m)/‡¤×®IDš][œeOiCšË8LêÉ°é1¤éëÈşúà•V³¦Ì<çÖ‰ö½Üõ}w=[s%VµG¬ÃøSIjyEƒFÊ©0hI.¡ıºc8d³‰kñG„‘S\µyI@MtÀK_áëâ6NÔîÖqÏ\ñ•Ÿ¨#ıìâºèÌLÒˆ%_˜¯qÖm4ù¿‡éÓ›(SœGÿÿ@,­3P#È gßj8úÔ[ş¯Îy·|Hõ¤Kr%gÜ
DŠFx=<G Öá0³…mµ¿
L\	™¶ıjU”æÇ¬|ióe,ì3ÖÅÉ¾·)ÀW[Â'!šFıÄÆWbéÌ¶¼Üàº¼:Ë”NÔ´RÀ¹NhŸşÌÎrÊ‰ı1)ğyÃİ(p§‚¥GhWĞ¹ç{+È9fµ ‰sk:{!5ÔğXËO~|­µ5m²qÖºPÊnL!ıœr7úúîlç+tájéRëwh+æıòÿ¼?\×¬‹=?PdM‹_²ÖË,·SóİB·¬[^¯SLÅ­Ó"`OŠÕq³Ìƒœ9ãÒDök“"û´ê‘‘7î•œ-rñ kó¤Çc®¾Çd9¯ïõÎm,Wë,şe¸XíÅ¹50«º‹2ÀS}°ÕÇ:yHÑ«³ 0w"¸ÁbceÄ ¥„Yóéf±áDÔ‰÷şŒSÂƒéé‡Ÿ&9wø/¥'p1‡§øœœ¤Ï4÷£ù‡iìÇè_89Ÿü]Rq»œƒl²’ñéÍVÜLgÓ÷¢ºùÊæq%›°^¼F·åTCw&Ö£õ?xÆoBıÏâHïÖWî5aìK4 7qò¯Y¬…Uk²BÍlÅ5œ©^*Ì4ù: Ğ<s…ÔßË-ø—doc¯œùÀİN]ÎmÜYxNöÚ;O}–Ï·ÕçÑºÕšwŞ‘j½uÚ™óç…prÉ€Ñômğç½º÷ˆ›Ë.œ%ÜsqÑPş¬Ïòùø³>Vş¬5ïÌŸµŞ:ñgc^7ş>Âô¡c¦ïÌ‚ øZÇÌb™|°á9Å—)ç:KÓ›òüşù9êB=r-ú/^6'fXcfO*Ú±ªK¦'XW&çËÅÂ÷œ¹	G¨€¾ñu–1à•³å=JÜnï>Ç‰ÿğşİä˜ÿ>fş©k{×ÉG}sşt½’Kz,àÔ7£Z×*ı™¿§Ÿ4^>©k]Ê'=ˆùWO y´’‡‚éÜãŞëÍÏâk¹Ç$şú0Á9ìĞlZ.ËÖg>äy$’sôÈ¾18edj(ŠÑ.%édóŞ¥ä0A¦ÀÀÁä,2’Ä{œÓ‰ÊÅJ–¦÷g	<‚úo£}Á<¤• ™`µÙL·^%ªÖog˜|á…¦~§Ù\Şg|”áWò…[ÿ÷‡÷š_ğÈÂ]Š~U»ÇH^ xOp—†6O”IÑ^F0µ|Øô¹ü9Ğ¢KŞTG–É{ÒÎ¬+˜Ï¥H»ø:wÍËui0‡M÷Rc$·½8ì¦^«¹d1÷ãuWdfB¸ èœG¸ZçÖSSµ\ªw­Ùü®¯İh»Ú@×w9Å›	3cü Öe·J9mÎ³œgZäâÕ¤(F«câ7å)Uêi¾GGB¤P’TS\–A®qœ ç¦M3B}ì>ÕÓ„=C•ä ³ô{45„Zßü{µË.òÉç¥™eAoŠµdj³ÅçÿáG¤bœà›âù—~üá)‰?ÿœüWµï~ÇÇÈïøšó²l‚½ø;>eØ{øu<ˆğ¹çÀ¹Ã)VF]!n|ÃËOr*¦`XUï’b±(cx‹?:—Vñ‡"pYF²©Ù˜;e:*Be³õëvq„Z[±`¦Š›H$Ú—ëélê­ËŸ6@pcnAäTéexO•ï'˜>ÅşøP¾3qp:îÌ[¯9)Úœğÿ²_ç§•ßD_\V›`Öòn²Ñ#I#¯l?+ôZÎWûğªeş‚W B8'­=×]¯MàzÎW-^ÖqF<Î9ıÖŞD;°¹‰å­Â—°m)Wh°Ít[M6i"òj9ª^Ã}ˆ`ğÅJ¾°ö7‡ãm,Ç3G‹Ö¿}}I5«¯«lÁÆx!'ÊÿüßÉlòmşû¿MşoÖw¸„JØøÀğÂİäCíhşa“ÿ11Öí²^Rj¢ìŸ#&{è÷ÿR½Ñ…¥ y¬éq£èlïß½ziê‚ê¿J£¦‰¼B›é@×JDz@t‡&½i÷!KøLfsÉ˜ps3]×ìğU¶lw"Uìu¡6	«n™
á}1[¥³şÔNªeWãÅ£„×–N´Š¥SQk”(‘6ªÚ­[²¨¦¡¡ê~Iáı÷$‘x(bM7?Fç¯³£ µ¾b(€}ŞLğu†IdR²–Ã„şF+}	ı ræ’úh—Ã¡m,ÁVè;E¢¼‰ƒİğúü‘@rˆ,³…w­P½*Ê¢n0á¸— ~Ûl&JJõÊŞnq-L-,®7g¤Ì¼h5Rà¥§İ-o4.n‰:èSX‹Æ´W™ ¡&†OoÇô‹~®Yo†•]Z››İ*‚ëµŒè%¼EHÒ×» XÌİ¥;SÖä«ÕÜéb¾İrÊ×ê®Ôæ‘,ëÖ\yõÒ;\»]»Z‹ºi…ªx{yƒÉÔƒNsúHúÆ„TîïºÃQ¥q£Ê…Íû<Q–ìŠo·nê’D´koë-œ…ğ²Òoãteˆv «ñn´¸äàŸRô<(‚ûÓ.÷ğşœÆ)Ò­O{R±ËJ*œõ&ğ¦€“j#¿LL&0É'"öó)cÇˆ6{	³G‚®|5LÂŒ±ñ6÷£’(+ÀˆDYîF€|Š­Ê ç`
×bC™q×cÇ›íƒe×z‘îçÇGxJÑFŸ¬Ôï…[¸Ø@£Øàáâ$È.$OJ5ƒö1	=ö*ÆÉ{‚şyw¾,f4ı›eëã•'Ø†	¶ÖªfŸ­dkÍ”¬±¯Ö[ LGzÆĞ*Ãœ¤0¯’ÑkÁ5zŒµYÍ0'0ÒÜrÉÑ©|ÜğSí(Ñ’›¿(Á¢ñÔâEİô—]Qg–õ²k¼nLv"v3¾&¾îø‚<°’Şd;›n–«§ºMû–áùÚ¼ng÷j´È¨ÙÈè˜|Àw,Fª¸]ÃùAG¨d‹b"RŠ…¤¤„¢˜ [õPšjy i®êMªS²ƒì‰ıøm
Vâ\à€Õ<Ç1¨pÓ½$³{1íŠ¡DJlÑ@¬¿-Ô	Êî.Ekå¤(Rdaû-U9a*oËÖÅÃtÑÉx…Jù;@lğõÒq9˜KÌ.íÊCœ>Áä´ó@´Ç#š‘JK$İ¸ÀŸÀh´¤¼T9_İMö˜.Zùn
b#mÈP2iƒÈ¥n¡,m²î.EÚhŸ‘è)]â Qñ˜=€ˆÃŸÇ27cùußµÜâbVãñÙŞúE7)nüúñbjÙÚÑÒ¾Ê¦-ğò(+_8˜à
2ø;—­—š3êSV6†²D”Ö:ÖÍ©?o(Tïd·_`2Ÿ­»Î¦ìÈn Ğ‡ÉzŒJ˜¯ÿ·¾‚3ŸÏ\w‡é×Óœiö$»KÁ¸67»èøv=›¦¢¹ıÀ×/[#àFJt7n ª{Şl^/ZsH.ÒÄ"(cÿgS>ÕVÇ×h&¼zH7š R¼Fsˆ3Uà%NE‡H›ıß[ÿšÿnŠtüS†ö†E›’ÑT*•.‰ºpº˜Í—-± F†iÎğ’fš±‰Õ%ò¨¥‹vÜİŒÚ™©£±ìzx¹ä:WÊ#ŞSZN¤ì4dÓ4ŸõlºâC³ÁeZ¦™{\4{xH.#ì=}¿c+›ÚCÜ•‹³Oê[6ó·§]@~„N§`¿óA¸çŸªx aÄO²ù„é.÷lI8Dâ,vE¥Áû3” ü§9¤0¨ÑPÔh©ÄtµfÎMœ2µ.€f€µ‘A}®D½yGš3TãÈ-ĞÚšw¾š™µ¢0ÑÚ6B§µf{-W¿e›‡zÉMÃ–=\«­f>_­7³ÙÊd«yÈïiàÕ§&ÄÈÙ«Q‰ÔØ¨ó=d²¶æaï’8Ñ)¸êX¥éS! †±g§?–Ìé›ÆZê«^~ƒnÓ tÆOĞA—i:Oã{ôO'jºSã|
½NÔÀtÆßbÅü\¢i:OãÇ0úØiQH68¥+£X¾ê^I¨^½W‘S
jJ@ê«®³õüµCÅæŒ`ßÔ£‘|v¨±ô—®¿¤ÏóG"€íPc3÷ÖKgµº`j}À5€»X8ŞtzÁÔ Û‘j8Á|ãÏ×ğ‚©AvE;Ô€ËÍfêÌ/™ds¶C±^!Áá\05°’`IlÌ½i°v.Lˆ>€OĞ¯‹‹éz«’3~ş›^Â^jF}6©aì6Oe§šõ—ê:,·C}ºê«Ë<ìêÑÔœzÔ£¹d,aNÀ`Xl7[ FÛõŒdÍ,¬zkg5K0æÎğÁPÚ™zi‘ú5U™ÒÊÕ[É 	'V’n‰ºØÚl·ş&€’œ½U)NÁÕ×~KÌ<ƒ ÿWcı}ópˆç®É<Â^;4‰Ÿ@™¸$eY|7[—S¿'î8¡z]UR™uîª9B{AÍbµ]kšHì­‘'pÒE¢ˆ¥ôB»Šµ]Û¯7‹9pÄ»v{¼×9ÂSb¾&`*4Ş–_o ‚”Ä±ß@üìA4Ù‚š¹ı•ÕÍtñ9:ôÓÚ´²Îûá	4
ÑWôi¹ášÊ¡‹BÄ‡WU¿µU7ç;&á'|A„yÙæPWêX/Ê«ŞŒ²ß°ô(r~Íy&¦Ï_¢ğ·3Äµzï‚>«æÍŞ¹VŸº=íŸ"‹_EÉ8x“×?üæ¡^»“¹ØIÍj’’Ÿl%i÷²è=ÀV¥fOZëíEÃ3Õ»éoBŞÂoÊI©ıĞ,l:6IJ·}V¶<V–ZzÊäò .«¡µèwXYşÅrµoË.-oïıØäÉ`š¢÷<İeÈÆŠ;ù{|èÁoÓù|ı%ŸuøušÙ‰Èµx·ş¶-Éh4(šsÂ	ù 8–éªç×êã¦<4^»OW‘¡cëœÔÈCöPî3b…¢Ù\ESi>£Ş»ŠÎ"~ÆHq¡“»	péU2Ac±õw‚íb¾Ş.·‹qIêAäh‡E°GÏYøĞá%Ã¼)b£QÄÄ,o‘7påP¬‘ŸöçÇ‹ ès ¯½øø§\‡ŞÇw ôq%+Î‹5\9H®÷ñ)à.:\qİÌ¬ûp—ùäÍˆ·.ˆ“Ãh­| ‘s÷ £ôz‰ë1÷qØ‚^OÕÖåÊ¶úæòEÖ<_ÉåñWY‘›¿Ä©†v-©Ô²p‚õ"p‘;š—O1Šk¹¾F„Ûë²N«[yñŠØr¯2U»S–gü»0IŸ|ğÅoµX,6s}æ´²;·˜PñË8˜WÏ¾¶,êˆ,–Ç*Ë&H‘ü5ü zE/Ö±<HÂšÁøëåFìüË‹iŸ §*K‘ná#Ü¥qT¦\àµ¢¿ÌRú®ƒÍ/Ì<+yH’Vò”jamÉ°—’ÎcJ·\vm:9ÏôŸŸ¢ÛVÔÂ”lUÉš¤k}€ÛTé€ò¦Àä[\Ø[YNÕÜ@Z~0ûhı¥ûu°–²â—|áW³“yCyíeÎÖ²ıëĞË©Ø0
ú/Ú÷b²ä¿G	Ëúœœ¸Fúİj\Æ-²ä·F“å®ÖÓ¹$¸5³Ç#]bE8&ğÓˆ4æ{2¡s’@*…Éõ‘iÂIFx$;šR*C<Àq¾œv{(ÕŞyî ò¶47ù)¿¤yëŞP‘¿= ½¶->M:ë_¶"Ö²Šd³©ÖM/4°"¤×ª\'e»"™z¹f:ZS}ùtfYrë ¥—Y7=¥«u5­`G-}ÛQì¦"ç'˜>Åşj_ß™ØÜÜz€w2‚²>'Î‘ÿC?jT]qºŠ3¿NçÃ$xF,8–DD ×uıÅÒ¨äŒBFR*ß‰Óx·Ñ£øëHÑ(ÊT‡Ã¨zİ‘C*¿­ıêp+ÃåûT+©Êß¹(Fg«ĞÇ?eœNWğşşŒğ_¡Ğ:Üã ä!øY¶Çf]#>ŸNÙªBÚÁHÌÃ±ÚW[¯[;<ôCâ¤GJ$jA”¾ÑG–2{º!œ‚ÅÜŠ·–Íúºš¦h3HĞô• ™½Ğ¨¼6—„8§VÎdÈe)^\M‰ªü”áı=RÇ“ĞÊ½¥åtî.— ½ ¿:1|
ágQíİ4D'«0Ú‘6~şià¥á'¸SiúÃÇ§t‡v:Üê'é.Aïï09„{Nq>üÔ	Âµ‹!ÓZ@&1¥qœÆ–xMøOH]&üGTı%¢ûv–<£ÈÍ=Ïšrrvhí®÷Öö|6i}¬•eZ{²Ïs:»²ÅĞ±Mäë¢åáªÓZg4şjéôĞºp:	×P=!µÌmzXñt+±pb50`c¢~-§ë·‚kí*!Ô•‘ôº›²éÏ­£œïµäo§É¶x¥Ô+»E^iƒ”ÜûÆ°€‘=ú¾ëùÛ™# ©ì
w–šOú;ÿL˜u„Áá]’–0¯÷u—bŠOĞÏÅ¶¨İ¿µ¡Ñ;Sü ü¾|#K'ÏÊ˜iwäÿ•Úûaä­Åú6Ÿƒ¨Ûü¡%Ú6÷©²Í}BU×wÅª6ÏÜJ¡®kF¾Iµ„©IJ„Õ_m'E½ş«†Ò½ÿ‰éù'¾{zOÖ¥óÒ[®ÁíğÓKjĞ|ñT¤';ÕA%(;¼‚eR’¤ÌSºÒ”3ìÕHÔìÔlìK®Ìm1ºÖ) Gƒˆ½+—3w6: èy«-çpPĞî˜Äq€ß—Ñ®Jêâ€-õÇÓ¾¸jZî^Hö Åq¬|§4¾4æïş’,ü}ø*ûóCAo5_ÍÔeÍ×¤®Æ…QªñT&ªi•…ªÄ•FòªÙ$X~c¡¬ÑÍüAŠÁ“‹Ú´Fêb{Q¹Â9Ş¤”E_<&_¹º8²P¦î°òim"·è¤
×<}{²¼NÒí@…9V¸F'3Ù5y¹ÉêL7Ùy‰²SöĞò¿`85©+mK¾h3:sËÆùZ$6ğıwñ~>Ã„'¦©Ë£&ryæ¸ŞÒ]ûÚŞIÌ 5a@şŸu»‡åÙ«Ú:‘ÕvĞ­ÀÊmd„ûpSØ+È·]MW€ùbòüšo|§û¦€ÓÍª›”±ÉÆÿÕ¡›
W„8p !Â¹©KLİíÔ8æF³pÅ‹†´fø:fvMVƒ>/Z{Êpán‚íœ“?ÉJx--º/ÉŠÂ¹7À_Ÿ.zÁ:ÇÿŸã0Â!åßÅíÄr­Pì.ïŠ}vğ;øÌ»‡Ï¾|¿wñmwDgöOjEôèKôÂd7õ™‡^CoFF/:Òı¯gx¶0vÖÖÈ‡îßBø¹óĞy?¼±} ÔBŞ-SQï„-AÉş,¾õ7¹û7³Õü~ÍqYs¹±©óÇuéSˆw«„¾ùş~:s8Ù7¿^BgÌm•ÌÎö~q¿]r*v}½dÎ™U:¿^¿Y|·-X:ç>Šs²ïqğ ‘„UÍş  ÿÿ¢Õú2¨3Î'İpaij1xŸ!(.Ñãy¬"‰çĞ"²:îN–®®fæXêÜScàmY@WUb›:ØxNô
ö÷ƒş9Y×„WANb	èüQ¬+•Ğ½Hßy)tÛ©05 )”ÀõQÈ±DÑ¼š*3X*¡h²ûÈ›ÈÁ’EaÉ¬n(wzæ•MåQÜ=7´XGºVC÷€\L…î
ºŞQ…´¿®
İJÿlR{ØM!t¥º2»1xŒ¨öğ@¹f#I¯¥LLLÍÌŒÍ1ëØä,êbZ7K ¶ÙØŸÔp!vQáÀÖPQë\<	•ÚIÃÌĞÙÑÈÒKcÜLIÊ/Á¹³g±™r*ø¨iìgÍÑ4¹AOÊ–R˜—HÑó‹™@Á@ê•íŞ‡ø†ä6Tpnv¶\QË   ÿÿ Öøö